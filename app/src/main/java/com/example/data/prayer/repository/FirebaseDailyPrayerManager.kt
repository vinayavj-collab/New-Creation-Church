package com.example.data.prayer.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.prayer.model.DailyPrayerVerse
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class FirebaseDailyPrayerManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: FirebaseDailyPrayerManager? = null

        fun getInstance(context: Context): FirebaseDailyPrayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseDailyPrayerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("daily_prayer_custom_prefs", Context.MODE_PRIVATE)
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val realtimeDb: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _firebasePrayers = MutableStateFlow<Map<Int, DailyPrayerVerse>>(emptyMap())
    val firebasePrayers: StateFlow<Map<Int, DailyPrayerVerse>> = _firebasePrayers.asStateFlow()

    private val _customizedPrayers = MutableStateFlow<Map<Int, DailyPrayerVerse>>(emptyMap())
    val customizedPrayers: StateFlow<Map<Int, DailyPrayerVerse>> = _customizedPrayers.asStateFlow()

    private var firestoreListener: ListenerRegistration? = null

    init {
        loadLocalCustomizations()
        listenToFirebasePrayers()
    }

    private fun loadLocalCustomizations() {
        try {
            val jsonStr = prefs.getString("custom_prayers_json", null) ?: return
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<Int, DailyPrayerVerse>()

            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val id = key.toIntOrNull() ?: continue
                val obj = json.getJSONObject(key)

                val basePrayer = DailyPrayerRepository.getPrayerById(id)
                val topic = obj.optString("topic", basePrayer.topic)
                val sermonNotes = obj.optString("sermonNotes", basePrayer.sermonNotes)
                val announcements = obj.optString("announcements", basePrayer.announcements)

                val prayerPointsArray = obj.optJSONArray("prayerPoints")
                val prayerPointsList = mutableListOf<String>()
                if (prayerPointsArray != null) {
                    for (i in 0 until prayerPointsArray.length()) {
                        prayerPointsList.add(prayerPointsArray.getString(i))
                    }
                }

                val customNamesArray = obj.optJSONArray("customNamesList")
                val customNamesList = mutableListOf<String>()
                if (customNamesArray != null) {
                    for (i in 0 until customNamesArray.length()) {
                        customNamesList.add(customNamesArray.getString(i))
                    }
                }

                map[id] = basePrayer.copy(
                    topic = topic,
                    sermonNotes = sermonNotes,
                    announcements = announcements,
                    prayerPoints = if (prayerPointsList.isNotEmpty()) prayerPointsList else basePrayer.prayerPoints,
                    customNamesList = customNamesList,
                    isCustomized = true
                )
            }
            _customizedPrayers.value = map
        } catch (e: Exception) {
            Log.e("FirebasePrayerManager", "Error loading local prayer customizations", e)
        }
    }

    private fun saveLocalCustomizations() {
        try {
            val json = JSONObject()
            _customizedPrayers.value.forEach { (id, prayer) ->
                val obj = JSONObject().apply {
                    put("id", id)
                    put("topic", prayer.topic)
                    put("sermonNotes", prayer.sermonNotes)
                    put("announcements", prayer.announcements)

                    val pArray = JSONArray()
                    prayer.prayerPoints.forEach { pArray.put(it) }
                    put("prayerPoints", pArray)

                    val nArray = JSONArray()
                    prayer.customNamesList.forEach { nArray.put(it) }
                    put("customNamesList", nArray)
                }
                json.put(id.toString(), obj)
            }
            prefs.edit().putString("custom_prayers_json", json.toString()).apply()
        } catch (e: Exception) {
            Log.e("FirebasePrayerManager", "Error saving prayer customizations", e)
        }
    }

    private fun listenToFirebasePrayers() {
        // 1. Listen via Firestore
        try {
            firestoreListener = firestore.collection("daily_prayers")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w("FirebasePrayerManager", "Firestore daily_prayers listen error", error)
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        val map = mutableMapOf<Int, DailyPrayerVerse>()
                        for (doc in snapshots.documents) {
                            try {
                                val id = doc.getLong("id")?.toInt() ?: doc.id.toIntOrNull() ?: 1
                                val base = DailyPrayerRepository.getPrayerById(id)

                                val topic = doc.getString("topic") ?: doc.getString("themeTitleHindi") ?: base.themeTitleHindi
                                val verseText = doc.getString("verse") ?: doc.getString("verseTextHindi") ?: base.verseTextHindi
                                val verseRef = doc.getString("verseReference") ?: doc.getString("verseReferenceHindi") ?: base.verseReferenceHindi
                                val sermonNotes = doc.getString("sermonNotes") ?: ""
                                val announcements = doc.getString("announcements") ?: ""
                                val prayerText = doc.getString("prayer") ?: doc.getString("prayerHindi") ?: base.prayerHindi
                                val declaration = doc.getString("declaration") ?: doc.getString("declarationHindi") ?: base.declarationHindi

                                @Suppress("UNCHECKED_CAST")
                                val points = (doc.get("prayerPoints") as? List<String>) ?: emptyList()

                                map[id] = base.copy(
                                    topic = topic,
                                    themeTitleHindi = topic,
                                    verseTextHindi = verseText,
                                    verseReferenceHindi = verseRef,
                                    prayerHindi = prayerText,
                                    declarationHindi = declaration,
                                    sermonNotes = sermonNotes,
                                    announcements = announcements,
                                    prayerPoints = points
                                )
                            } catch (e: Exception) {
                                Log.e("FirebasePrayerManager", "Error parsing Firestore prayer doc", e)
                            }
                        }
                        if (map.isNotEmpty()) {
                            _firebasePrayers.value = map
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("FirebasePrayerManager", "Firestore setup error", e)
        }

        // 2. Realtime Database fallback/sync
        try {
            realtimeDb.getReference("daily_prayers")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val map = mutableMapOf<Int, DailyPrayerVerse>()
                            for (child in snapshot.children) {
                                try {
                                    val id = child.child("id").getValue(Long::class.java)?.toInt()
                                        ?: child.key?.toIntOrNull() ?: 1
                                    val base = DailyPrayerRepository.getPrayerById(id)

                                    val topic = child.child("topic").getValue(String::class.java) ?: base.themeTitleHindi
                                    val verseText = child.child("verse").getValue(String::class.java) ?: base.verseTextHindi
                                    val verseRef = child.child("verseReference").getValue(String::class.java) ?: base.verseReferenceHindi
                                    val sermonNotes = child.child("sermonNotes").getValue(String::class.java) ?: ""
                                    val announcements = child.child("announcements").getValue(String::class.java) ?: ""
                                    val prayerText = child.child("prayer").getValue(String::class.java) ?: base.prayerHindi
                                    val declaration = child.child("declaration").getValue(String::class.java) ?: base.declarationHindi

                                    val pointsList = mutableListOf<String>()
                                    for (pChild in child.child("prayerPoints").children) {
                                        pChild.getValue(String::class.java)?.let { pointsList.add(it) }
                                    }

                                    map[id] = base.copy(
                                        topic = topic,
                                        themeTitleHindi = topic,
                                        verseTextHindi = verseText,
                                        verseReferenceHindi = verseRef,
                                        prayerHindi = prayerText,
                                        declarationHindi = declaration,
                                        sermonNotes = sermonNotes,
                                        announcements = announcements,
                                        prayerPoints = pointsList
                                    )
                                } catch (e: Exception) {
                                    Log.e("FirebasePrayerManager", "RealtimeDB parse error", e)
                                }
                            }
                            if (map.isNotEmpty() && _firebasePrayers.value.isEmpty()) {
                                _firebasePrayers.value = map
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w("FirebasePrayerManager", "RealtimeDB cancelled: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            Log.w("FirebasePrayerManager", "RealtimeDB setup error", e)
        }
    }

    fun getEffectivePrayer(id: Int): DailyPrayerVerse {
        val base = DailyPrayerRepository.getPrayerById(id)
        val fromFb = _firebasePrayers.value[id]
        val fromCustom = _customizedPrayers.value[id]

        val combined = fromCustom ?: fromFb ?: base

        // Ensure prayerPoints has default suggestions if empty
        val defaultPoints = if (combined.prayerPoints.isEmpty()) {
            listOf(
                "बीमारों और कमज़ोरों की चंगाई के लिए",
                "परिवारों में शांति, प्रेम व एकता के लिए",
                "कलीसिया व आत्मिक सेवकाई की उन्नति के लिए",
                "देश व समाज में शांति और सुरक्षा के लिए"
            )
        } else {
            combined.prayerPoints
        }

        return combined.copy(
            prayerPoints = defaultPoints
        )
    }

    fun getEffectiveTodayPrayer(): DailyPrayerVerse {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val dayIndex = ((dayOfYear - 1) % 31) + 1
        return getEffectivePrayer(dayIndex)
    }

    fun updateCustomPrayer(
        prayerId: Int,
        topic: String,
        sermonNotes: String,
        announcements: String,
        customNames: List<String>,
        syncToFirebase: Boolean = false
    ) {
        val current = getEffectivePrayer(prayerId)
        val updated = current.copy(
            topic = topic.trim(),
            themeTitleHindi = topic.trim().ifBlank { current.themeTitleHindi },
            sermonNotes = sermonNotes.trim(),
            announcements = announcements.trim(),
            customNamesList = customNames.filter { it.isNotBlank() },
            isCustomized = true
        )

        val newMap = _customizedPrayers.value.toMutableMap()
        newMap[prayerId] = updated
        _customizedPrayers.value = newMap
        saveLocalCustomizations()

        if (syncToFirebase) {
            scope.launch {
                try {
                    val data = hashMapOf(
                        "id" to prayerId,
                        "topic" to updated.displayTopic,
                        "verse" to updated.verseTextHindi,
                        "verseReference" to updated.verseReferenceHindi,
                        "sermonNotes" to updated.sermonNotes,
                        "announcements" to updated.announcements,
                        "prayer" to updated.prayerHindi,
                        "declaration" to updated.declarationHindi,
                        "prayerPoints" to updated.allIntercessionNames,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    firestore.collection("daily_prayers")
                        .document(prayerId.toString())
                        .set(data)
                        .addOnSuccessListener {
                            Log.d("FirebasePrayerManager", "Synced prayer $prayerId to Firestore")
                        }

                    realtimeDb.getReference("daily_prayers")
                        .child(prayerId.toString())
                        .setValue(data)
                } catch (e: Exception) {
                    Log.e("FirebasePrayerManager", "Failed to sync prayer to Firebase", e)
                }
            }
        }
    }

    fun resetPrayerCustomization(prayerId: Int) {
        val newMap = _customizedPrayers.value.toMutableMap()
        newMap.remove(prayerId)
        _customizedPrayers.value = newMap
        saveLocalCustomizations()
    }
}
