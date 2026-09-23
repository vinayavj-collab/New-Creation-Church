package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.bible.local.BibleDatabase
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.PrayerRequestItem
import com.example.data.model.UserActivityItem
import com.example.data.model.UserActivityType
import com.example.data.model.UserProfileData
import com.example.util.UserDeviceHelper
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserProfileRepository(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val bibleDatabase: BibleDatabase,
    private val firebaseDataRepository: FirebaseDataRepository,
    private val preferencesManager: PreferencesManager
) {
    private val prefs = context.getSharedPreferences("user_profile_store", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfileData> = _userProfile.asStateFlow()

    init {
        // Initial setup of Join Date if empty
        if (_userProfile.value.joinDate.isBlank()) {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date())
            updateProfile(_userProfile.value.copy(joinDate = dateStr))
        }
    }

    private fun loadProfile(): UserProfileData {
        val deviceId = UserDeviceHelper.getDeviceId(context)
        val name = prefs.getString("display_name", "") ?: ""
        val photoUri = prefs.getString("photo_uri_path", "") ?: ""
        val role = prefs.getString("role", "विश्वासी (Believer)") ?: "विश्वासी (Believer)"
        val phone = prefs.getString("phone_number", "") ?: ""
        val city = prefs.getString("city", "") ?: ""
        val bio = prefs.getString("bio", "परमेश्वर का अनुग्रह मेरे लिए काफी है। ✝️") ?: "परमेश्वर का अनुग्रह मेरे लिए काफी है। ✝️"
        val favoriteVerse = prefs.getString("favorite_verse", "यूहन्ना 3:16") ?: "यूहन्ना 3:16"
        val joinDate = prefs.getString("join_date", "") ?: ""
        val storageMode = prefs.getString("storage_permission_mode", "AUTOMATIC") ?: "AUTOMATIC"
        val isVerifiedVishwasi = prefs.getBoolean("is_verified_vishwasi", false) || role.contains("Verified", ignoreCase = true)
        val lastUpdated = prefs.getLong("last_updated", System.currentTimeMillis())

        return UserProfileData(
            displayName = name,
            photoUriOrPath = photoUri,
            role = role,
            phoneNumber = phone,
            city = city,
            bio = bio,
            favoriteVerse = favoriteVerse,
            joinDate = joinDate,
            deviceId = deviceId,
            storagePermissionMode = storageMode,
            isVerifiedVishwasi = isVerifiedVishwasi,
            lastUpdated = lastUpdated
        )
    }

    fun updateProfile(profile: UserProfileData) {
        val updated = profile.copy(
            deviceId = UserDeviceHelper.getDeviceId(context),
            isVerifiedVishwasi = profile.isVerifiedVishwasi || profile.role.contains("Verified", ignoreCase = true),
            lastUpdated = System.currentTimeMillis()
        )
        prefs.edit()
            .putString("display_name", updated.displayName)
            .putString("photo_uri_path", updated.photoUriOrPath)
            .putString("role", updated.role)
            .putString("phone_number", updated.phoneNumber)
            .putString("city", updated.city)
            .putString("bio", updated.bio)
            .putString("favorite_verse", updated.favoriteVerse)
            .putString("join_date", updated.joinDate)
            .putString("storage_permission_mode", updated.storagePermissionMode)
            .putBoolean("is_verified_vishwasi", updated.isVerifiedVishwasi)
            .putLong("last_updated", updated.lastUpdated)
            .apply()

        // Sync with app-wide user name in settings
        if (updated.displayName.isNotBlank()) {
            preferencesManager.updateUserName(updated.displayName)
        }

        _userProfile.value = updated
        syncToCloud(updated)
    }

    suspend fun saveImageFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "profile_pictures")
            if (!dir.exists()) dir.mkdirs()

            val avatarFile = File(dir, "user_avatar_${System.currentTimeMillis()}.jpg")
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val out = FileOutputStream(avatarFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
                val path = avatarFile.absolutePath
                updateProfile(_userProfile.value.copy(photoUriOrPath = path))
                return@withContext path
            }
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Error saving profile avatar: ${e.message}")
        }
        return@withContext ""
    }

    suspend fun saveCroppedBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "profile_pictures")
            if (!dir.exists()) dir.mkdirs()

            val avatarFile = File(dir, "user_avatar_${System.currentTimeMillis()}.jpg")
            val out = FileOutputStream(avatarFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            out.flush()
            out.close()
            val path = avatarFile.absolutePath
            updateProfile(_userProfile.value.copy(photoUriOrPath = path))
            return@withContext path
        } catch (e: Exception) {
            Log.e("UserProfileRepo", "Error saving cropped profile avatar: ${e.message}")
            return@withContext ""
        }
    }

    private fun syncToCloud(profile: UserProfileData) {
        scope.launch {
            try {
                val deviceId = profile.deviceId.ifBlank { UserDeviceHelper.getDeviceId(context) }
                val map = hashMapOf<String, Any>(
                    "displayName" to profile.displayName,
                    "role" to profile.role,
                    "phoneNumber" to profile.phoneNumber,
                    "city" to profile.city,
                    "bio" to profile.bio,
                    "favoriteVerse" to profile.favoriteVerse,
                    "joinDate" to profile.joinDate,
                    "deviceId" to deviceId,
                    "lastUpdated" to profile.lastUpdated,
                    "churchName" to profile.churchName,
                    "organizationName" to profile.organizationName,
                    "yearsInFaith" to profile.yearsInFaith,
                    "distanceToChurchKm" to profile.distanceToChurchKm,
                    "baptismStatus" to profile.baptismStatus,
                    "ministryInterest" to profile.ministryInterest,
                    "dateOfBirth" to profile.dateOfBirth,
                    "gender" to profile.gender,
                    "maritalStatus" to profile.maritalStatus,
                    "cityPincode" to profile.cityPincode
                )

                // 1. Firestore cloud sync
                FirebaseFirestore.getInstance()
                    .collection("user_profiles")
                    .document(deviceId)
                    .set(map, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("UserProfileRepo", "Profile synced to Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.w("UserProfileRepo", "Firestore sync skipped: ${e.message}")
                    }

                // 2. Realtime Database mirror
                FirebaseDatabase.getInstance()
                    .getReference("user_profiles")
                    .child(deviceId)
                    .setValue(map)
            } catch (e: Exception) {
                Log.w("UserProfileRepo", "Cloud sync error: ${e.message}")
            }
        }
    }

    // Unified Personal Activity History Feed
    fun getActivityHistoryFlow(filterType: UserActivityType = UserActivityType.ALL): Flow<List<UserActivityItem>> {
        val recentFlow = appDatabase.recentlyViewedDao().getRecentItems()
        val savedFlow = appDatabase.savedItemDao().getAllSavedItems()
        val notesFlow = bibleDatabase.bibleDao().getAllStudyNotesByDate()
        val prayersFlow = firebaseDataRepository.prayerRequests

        return combine(recentFlow, savedFlow, notesFlow, prayersFlow) { recents, saveds, notes, prayers ->
            val activities = mutableListOf<UserActivityItem>()
            val myDeviceId = UserDeviceHelper.getDeviceId(context)

            // 1. Add Recent Views
            recents.forEach { r ->
                val actType = when (r.type.uppercase()) {
                    "BIBLE" -> UserActivityType.BIBLE_READ
                    "VIDEO", "PLAYLIST" -> UserActivityType.VIDEO_WATCH
                    "BLOG" -> UserActivityType.BLOG_READ
                    else -> UserActivityType.BLOG_READ
                }
                activities.add(
                    UserActivityItem(
                        id = "recent_${r.id}",
                        type = actType,
                        title = r.title,
                        subtitle = r.subtitle.ifBlank { "देखा गया" },
                        timestamp = r.viewedTimestamp,
                        imageUrl = r.imageUrl,
                        payloadJson = r.extraDataJson
                    )
                )
            }

            // 2. Add Personal Study Notes
            notes.forEach { n ->
                activities.add(
                    UserActivityItem(
                        id = "note_${n.noteId}",
                        type = UserActivityType.STUDY_NOTE,
                        title = n.title.ifBlank { "अध्ययन नोट्स" },
                        subtitle = if (n.content.length > 80) "${n.content.take(80)}..." else n.content,
                        timestamp = try {
                            val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                            sdf.parse("${n.date} ${n.time}")?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        },
                        payloadJson = "${n.noteId}"
                    )
                )
            }

            // 3. Add My Prayer Requests & Testimonies
            prayers.filter { p ->
                UserDeviceHelper.isMyRequest(context, p.id, p.senderDeviceId)
            }.forEach { p ->
                activities.add(
                    UserActivityItem(
                        id = "prayer_${p.id}",
                        type = if (p.isAnswered && p.testimonyText.isNotBlank()) UserActivityType.TESTIMONY_SHARED else UserActivityType.PRAYER_POSTED,
                        title = if (p.isAnswered) "उत्तरित प्रार्थना व गवाही: #${p.serialNumber}" else "प्रार्थना निवेदन: #${p.serialNumber} (${p.name})",
                        subtitle = if (p.isAnswered && p.testimonyText.isNotBlank()) p.testimonyText else p.requestText,
                        timestamp = p.timestamp,
                        payloadJson = p.id
                    )
                )
            }

            // 4. Add Saved items / bookmarks
            saveds.forEach { s ->
                activities.add(
                    UserActivityItem(
                        id = "saved_${s.id}",
                        type = UserActivityType.SAVED_BOOKMARK,
                        title = s.title,
                        subtitle = "बुकमार्क सहेजा गया (${s.subtitle})",
                        timestamp = s.savedTimestamp,
                        imageUrl = s.imageUrl,
                        payloadJson = s.extraDataJson
                    )
                )
            }

            // Sort chronologically and apply filter
            val sorted = activities.distinctBy { it.id }.sortedByDescending { it.timestamp }
            if (filterType == UserActivityType.ALL) {
                sorted
            } else {
                sorted.filter { it.type == filterType }
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            appDatabase.recentlyViewedDao().clearHistory()
        }
    }
}
