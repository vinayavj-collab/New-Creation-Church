package com.example.data.prayer.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.prayer.model.PersonalPrayerItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class PersonalPrayerJournalRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _prayers = MutableStateFlow<List<PersonalPrayerItem>>(emptyList())
    val prayers: StateFlow<List<PersonalPrayerItem>> = _prayers.asStateFlow()

    private val _streakCount = MutableStateFlow(0)
    val streakCount: StateFlow<Int> = _streakCount.asStateFlow()

    private val _isCompletedToday = MutableStateFlow(false)
    val isCompletedToday: StateFlow<Boolean> = _isCompletedToday.asStateFlow()

    init {
        loadPrayers()
        loadStreakData()
    }

    private fun loadPrayers() {
        val jsonString = prefs.getString(KEY_PRAYERS_JSON, "[]") ?: "[]"
        val list = mutableListOf<PersonalPrayerItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    PersonalPrayerItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        details = obj.optString("details", ""),
                        createdDate = obj.optString("createdDate", ""),
                        isAnswered = obj.optBoolean("isAnswered", false),
                        answeredDate = if (obj.has("answeredDate")) obj.optString("answeredDate") else null,
                        testimonyNotes = if (obj.has("testimonyNotes")) obj.optString("testimonyNotes") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _prayers.value = list
    }

    private fun savePrayers(list: List<PersonalPrayerItem>) {
        _prayers.value = list
        try {
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("details", item.details)
                    put("createdDate", item.createdDate)
                    put("isAnswered", item.isAnswered)
                    if (item.answeredDate != null) put("answeredDate", item.answeredDate)
                    if (item.testimonyNotes != null) put("testimonyNotes", item.testimonyNotes)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_PRAYERS_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addPrayer(title: String, details: String) {
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN")).format(Date())
        val newItem = PersonalPrayerItem(
            id = System.currentTimeMillis().toString(),
            title = title.trim(),
            details = details.trim(),
            createdDate = dateStr,
            isAnswered = false
        )
        val current = _prayers.value.toMutableList()
        current.add(0, newItem)
        savePrayers(current)
    }

    fun markPrayerAnswered(id: String, testimonyNotes: String = "") {
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN")).format(Date())
        val updated = _prayers.value.map { item ->
            if (item.id == id) {
                item.copy(
                    isAnswered = true,
                    answeredDate = dateStr,
                    testimonyNotes = testimonyNotes.ifBlank { "प्रभु ने अद्भुत रीति से प्रार्थना सुनी और उत्तर दिया! आमीन।" }
                )
            } else {
                item
            }
        }
        savePrayers(updated)
    }

    fun deletePrayer(id: String) {
        val updated = _prayers.value.filter { it.id != id }
        savePrayers(updated)
    }

    // --- Streak Logic ---
    private fun loadStreakData() {
        val savedStreak = prefs.getInt(KEY_STREAK_COUNT, 0)
        val lastDate = prefs.getString(KEY_LAST_PRAYER_DATE, "") ?: ""
        val todayStr = getTodayKey()
        val yesterdayStr = getYesterdayKey()

        val isToday = lastDate == todayStr
        _isCompletedToday.value = isToday

        if (isToday) {
            _streakCount.value = savedStreak.coerceAtLeast(1)
        } else if (lastDate == yesterdayStr) {
            _streakCount.value = savedStreak
        } else if (lastDate.isNotBlank()) {
            // Broken streak
            _streakCount.value = 0
        } else {
            _streakCount.value = 0
        }
    }

    /**
     * Records today's prayer completion.
     * Returns the updated streak count and whether it was newly recorded today.
     */
    fun recordDailyPrayerCompleted(): Pair<Int, Boolean> {
        val todayStr = getTodayKey()
        val yesterdayStr = getYesterdayKey()
        val lastDate = prefs.getString(KEY_LAST_PRAYER_DATE, "") ?: ""
        val currentStreak = prefs.getInt(KEY_STREAK_COUNT, 0)

        if (lastDate == todayStr) {
            _isCompletedToday.value = true
            return Pair(currentStreak.coerceAtLeast(1), false)
        }

        val newStreak = if (lastDate == yesterdayStr) {
            currentStreak + 1
        } else {
            1
        }

        prefs.edit()
            .putInt(KEY_STREAK_COUNT, newStreak)
            .putString(KEY_LAST_PRAYER_DATE, todayStr)
            .apply()

        _streakCount.value = newStreak
        _isCompletedToday.value = true
        return Pair(newStreak, true)
    }

    private fun getTodayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getYesterdayKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    companion object {
        private const val PREFS_NAME = "personal_prayer_journal_prefs"
        private const val KEY_PRAYERS_JSON = "personal_prayers_json"
        private const val KEY_STREAK_COUNT = "prayer_streak_count"
        private const val KEY_LAST_PRAYER_DATE = "last_prayer_completed_date"

        @Volatile
        private var INSTANCE: PersonalPrayerJournalRepository? = null

        fun getInstance(context: Context): PersonalPrayerJournalRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PersonalPrayerJournalRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
