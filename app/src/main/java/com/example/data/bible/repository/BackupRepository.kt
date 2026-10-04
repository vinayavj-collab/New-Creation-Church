package com.example.data.bible.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.bible.local.*
import com.example.util.UserDeviceHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val appVersion: String = "1.3",
    val timestamp: Long = System.currentTimeMillis(),
    val bookmarkCount: Int = 0,
    val highlightCount: Int = 0,
    val noteCount: Int = 0,
    val studyNoteCount: Int = 0,
    val readingPlanProgressCount: Int = 0
) {
    val totalItems: Int
        get() = bookmarkCount + highlightCount + noteCount + studyNoteCount + readingPlanProgressCount
}

class BackupRepository(
    private val context: Context,
    private val bibleDao: BibleDao
) {
    private val prefs = context.getSharedPreferences("user_backup_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_SYNC_ID = "custom_backup_sync_id"
        private const val KEY_LAST_CLOUD_BACKUP_TIME = "last_cloud_backup_time"
        private const val KEY_LAST_CLOUD_BACKUP_COUNT = "last_cloud_backup_count"
        private const val KEY_LAST_CLOUD_RESTORE_TIME = "last_cloud_restore_time"
        private const val FIRESTORE_BACKUP_COLLECTION = "user_study_backups"
    }

    fun getSyncId(): String {
        val custom = prefs.getString(KEY_CUSTOM_SYNC_ID, null)
        if (!custom.isNullOrBlank()) return custom
        return UserDeviceHelper.getDeviceId(context)
    }

    fun setSyncId(syncId: String) {
        prefs.edit().putString(KEY_CUSTOM_SYNC_ID, syncId.trim()).apply()
    }

    fun resetSyncIdToDeviceDefault() {
        prefs.edit().remove(KEY_CUSTOM_SYNC_ID).apply()
    }

    fun getLastCloudBackupTime(): Long = prefs.getLong(KEY_LAST_CLOUD_BACKUP_TIME, 0L)
    fun getLastCloudBackupCount(): Int = prefs.getInt(KEY_LAST_CLOUD_BACKUP_COUNT, 0)
    fun getLastCloudRestoreTime(): Long = prefs.getLong(KEY_LAST_CLOUD_RESTORE_TIME, 0L)

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val bookmarks = try { bibleDao.getAllBookmarks().first() } catch (_: Exception) { emptyList() }
        val highlights = try { bibleDao.getAllHighlights().first() } catch (_: Exception) { emptyList() }
        val notes = try { bibleDao.getAllNotes().first() } catch (_: Exception) { emptyList() }
        val studyNotes = try { bibleDao.getAllStudyNotesByDate().first() } catch (_: Exception) { emptyList() }
        val readingPlanProgress = try { bibleDao.getAllProgress().first() } catch (_: Exception) { emptyList() }
        val readingPosition = try { bibleDao.getReadingPosition().first() } catch (_: Exception) { null }

        val root = JSONObject()
        root.put("version", "1.3")
        root.put("timestamp", System.currentTimeMillis())
        root.put("app", "Vinay Kumar AVJ")
        root.put("deviceId", UserDeviceHelper.getDeviceId(context))

        // Bookmarks
        val bmArray = JSONArray()
        bookmarks.forEach { bm ->
            val obj = JSONObject()
            obj.put("bookId", bm.bookId)
            obj.put("bookName", bm.bookName)
            obj.put("chapter", bm.chapter)
            obj.put("verse", bm.verse)
            obj.put("translationId", bm.translationId)
            obj.put("verseText", bm.verseText)
            obj.put("timestamp", bm.createdAt)
            bmArray.put(obj)
        }
        root.put("bookmarks", bmArray)

        // Highlights
        val hlArray = JSONArray()
        highlights.forEach { hl ->
            val obj = JSONObject()
            obj.put("bookId", hl.bookId)
            obj.put("chapter", hl.chapter)
            obj.put("verse", hl.verse)
            obj.put("colorHex", hl.colorHex)
            obj.put("timestamp", hl.createdAt)
            hlArray.put(obj)
        }
        root.put("highlights", hlArray)

        // Notes (Verse Notes)
        val noteArray = JSONArray()
        notes.forEach { n ->
            val obj = JSONObject()
            obj.put("bookId", n.bookId)
            obj.put("bookName", n.bookName)
            obj.put("chapter", n.chapter)
            obj.put("verse", n.verse)
            obj.put("noteText", n.noteText)
            obj.put("timestamp", n.updatedAt)
            noteArray.put(obj)
        }
        root.put("notes", noteArray)

        // Study Notes
        val snArray = JSONArray()
        studyNotes.forEach { sn ->
            val obj = JSONObject()
            obj.put("noteId", sn.noteId)
            obj.put("title", sn.title)
            obj.put("tags", sn.tags)
            obj.put("date", sn.date)
            obj.put("time", sn.time)
            obj.put("content", sn.content)
            snArray.put(obj)
        }
        root.put("studyNotes", snArray)

        // Reading Plan Progress
        val rpArray = JSONArray()
        readingPlanProgress.forEach { rp ->
            val obj = JSONObject()
            obj.put("planId", rp.planId)
            obj.put("dayNumber", rp.dayNumber)
            obj.put("isCompleted", rp.isCompleted)
            obj.put("completedTimestamp", rp.completedTimestamp)
            rpArray.put(obj)
        }
        root.put("readingPlanProgress", rpArray)

        // Reading Position
        if (readingPosition != null) {
            val posObj = JSONObject()
            posObj.put("bookId", readingPosition.bookId)
            posObj.put("bookName", readingPosition.bookName)
            posObj.put("chapter", readingPosition.chapter)
            posObj.put("verse", readingPosition.verse)
            posObj.put("translationId", readingPosition.translationId)
            posObj.put("timestamp", readingPosition.updatedAt)
            root.put("readingPosition", posObj)
        }

        root.toString(2)
    }

    suspend fun createShareableBackupFile(): File = withContext(Dispatchers.IO) {
        val json = createBackupJson()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "Vinay_Bible_Backup_$dateStr.json"
        val exportDir = File(context.cacheDir, "backups")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)
        file.writeText(json)
        file
    }

    fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun restoreFromFileUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return@withContext Result.failure(Exception("फ़ाइल लोड नहीं हो सकी"))
            restoreFromJson(json, mergeMode = true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromJson(jsonString: String, mergeMode: Boolean = true): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            var restoredCount = 0

            // Bookmarks
            if (root.has("bookmarks")) {
                val bmArray = root.getJSONArray("bookmarks")
                for (i in 0 until bmArray.length()) {
                    val obj = bmArray.getJSONObject(i)
                    val bm = BibleBookmarkEntity(
                        bookId = obj.getInt("bookId"),
                        bookName = obj.optString("bookName", "Bible"),
                        chapter = obj.getInt("chapter"),
                        verse = obj.getInt("verse"),
                        translationId = obj.optString("translationId", "HIOV"),
                        verseText = obj.optString("verseText", ""),
                        createdAt = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    bibleDao.insertBookmark(bm)
                    restoredCount++
                }
            }

            // Highlights
            if (root.has("highlights")) {
                val hlArray = root.getJSONArray("highlights")
                for (i in 0 until hlArray.length()) {
                    val obj = hlArray.getJSONObject(i)
                    val hl = BibleHighlightEntity(
                        bookId = obj.getInt("bookId"),
                        chapter = obj.getInt("chapter"),
                        verse = obj.getInt("verse"),
                        colorHex = obj.optString("colorHex", "#FEF08A"),
                        createdAt = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    bibleDao.setHighlight(hl)
                    restoredCount++
                }
            }

            // Notes
            if (root.has("notes")) {
                val noteArray = root.getJSONArray("notes")
                for (i in 0 until noteArray.length()) {
                    val obj = noteArray.getJSONObject(i)
                    val note = BibleNoteEntity(
                        bookId = obj.getInt("bookId"),
                        bookName = obj.optString("bookName", "Bible"),
                        chapter = obj.getInt("chapter"),
                        verse = obj.getInt("verse"),
                        noteText = obj.optString("noteText", ""),
                        updatedAt = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    bibleDao.saveNote(note)
                    restoredCount++
                }
            }

            // Study notes
            if (root.has("studyNotes")) {
                val snArray = root.getJSONArray("studyNotes")
                for (i in 0 until snArray.length()) {
                    val obj = snArray.getJSONObject(i)
                    val sn = StudyNoteEntity(
                        noteId = obj.optLong("noteId", 0L),
                        title = obj.optString("title", "Untitled Note"),
                        tags = obj.optString("tags", ""),
                        date = obj.optString("date", ""),
                        time = obj.optString("time", ""),
                        content = obj.optString("content", "")
                    )
                    bibleDao.insertStudyNote(sn)
                    restoredCount++
                }
            }

            // Reading Plan Progress
            if (root.has("readingPlanProgress")) {
                val rpArray = root.getJSONArray("readingPlanProgress")
                for (i in 0 until rpArray.length()) {
                    val obj = rpArray.getJSONObject(i)
                    val rp = ReadingPlanProgressEntity(
                        planId = obj.getString("planId"),
                        dayNumber = obj.getInt("dayNumber"),
                        isCompleted = obj.optBoolean("isCompleted", true),
                        completedTimestamp = obj.optLong("completedTimestamp", System.currentTimeMillis())
                    )
                    bibleDao.setPlanDayCompleted(rp)
                    restoredCount++
                }
            }

            // Reading Position
            if (root.has("readingPosition")) {
                val posObj = root.getJSONObject("readingPosition")
                val pos = ReadingPositionEntity(
                    id = 1,
                    bookId = posObj.optInt("bookId", 40),
                    bookName = posObj.optString("bookName", "Matthew"),
                    chapter = posObj.optInt("chapter", 1),
                    verse = posObj.optInt("verse", 1),
                    translationId = posObj.optString("translationId", "HIOV"),
                    updatedAt = posObj.optLong("timestamp", System.currentTimeMillis())
                )
                bibleDao.saveReadingPosition(pos)
                restoredCount++
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FIREBASE FIRESTORE CLOUD BACKUP & RESTORE ---

    suspend fun backupToFirebaseCloud(customSyncId: String? = null): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val syncId = customSyncId?.takeIf { it.isNotBlank() } ?: getSyncId()
            val jsonString = createBackupJson()
            val root = JSONObject(jsonString)

            val metadata = BackupMetadata(
                appVersion = root.optString("version", "1.3"),
                timestamp = root.optLong("timestamp", System.currentTimeMillis()),
                bookmarkCount = root.optJSONArray("bookmarks")?.length() ?: 0,
                highlightCount = root.optJSONArray("highlights")?.length() ?: 0,
                noteCount = root.optJSONArray("notes")?.length() ?: 0,
                studyNoteCount = root.optJSONArray("studyNotes")?.length() ?: 0,
                readingPlanProgressCount = root.optJSONArray("readingPlanProgress")?.length() ?: 0
            )

            val firestoreData = hashMapOf<String, Any>(
                "syncId" to syncId,
                "timestamp" to metadata.timestamp,
                "appVersion" to metadata.appVersion,
                "totalItems" to metadata.totalItems,
                "bookmarkCount" to metadata.bookmarkCount,
                "highlightCount" to metadata.highlightCount,
                "noteCount" to metadata.noteCount,
                "studyNoteCount" to metadata.studyNoteCount,
                "readingPlanProgressCount" to metadata.readingPlanProgressCount,
                "payloadJson" to jsonString,
                "updatedAtFormatted" to SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(metadata.timestamp))
            )

            FirebaseFirestore.getInstance()
                .collection(FIRESTORE_BACKUP_COLLECTION)
                .document(syncId)
                .set(firestoreData, SetOptions.merge())
                .await()

            prefs.edit()
                .putLong(KEY_LAST_CLOUD_BACKUP_TIME, metadata.timestamp)
                .putInt(KEY_LAST_CLOUD_BACKUP_COUNT, metadata.totalItems)
                .apply()

            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromFirebaseCloud(customSyncId: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val syncId = customSyncId?.takeIf { it.isNotBlank() } ?: getSyncId()
            val docSnapshot = FirebaseFirestore.getInstance()
                .collection(FIRESTORE_BACKUP_COLLECTION)
                .document(syncId)
                .get()
                .await()

            if (!docSnapshot.exists()) {
                return@withContext Result.failure(Exception("सिंक आईडी '$syncId' के लिए क्लाउड में कोई बैकअप नहीं मिला।"))
            }

            val payloadJson = docSnapshot.getString("payloadJson")
            if (payloadJson.isNullOrBlank()) {
                return@withContext Result.failure(Exception("क्लाउड बैकअप का डेटा रिक्त है।"))
            }

            val restoreResult = restoreFromJson(payloadJson, mergeMode = true)
            if (restoreResult.isSuccess) {
                prefs.edit()
                    .putLong(KEY_LAST_CLOUD_RESTORE_TIME, System.currentTimeMillis())
                    .apply()
            }
            restoreResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkCloudBackupAvailable(customSyncId: String? = null): Result<BackupMetadata?> = withContext(Dispatchers.IO) {
        try {
            val syncId = customSyncId?.takeIf { it.isNotBlank() } ?: getSyncId()
            val docSnapshot = FirebaseFirestore.getInstance()
                .collection(FIRESTORE_BACKUP_COLLECTION)
                .document(syncId)
                .get()
                .await()

            if (!docSnapshot.exists()) {
                return@withContext Result.success(null)
            }

            val meta = BackupMetadata(
                appVersion = docSnapshot.getString("appVersion") ?: "1.3",
                timestamp = docSnapshot.getLong("timestamp") ?: 0L,
                bookmarkCount = docSnapshot.getLong("bookmarkCount")?.toInt() ?: 0,
                highlightCount = docSnapshot.getLong("highlightCount")?.toInt() ?: 0,
                noteCount = docSnapshot.getLong("noteCount")?.toInt() ?: 0,
                studyNoteCount = docSnapshot.getLong("studyNoteCount")?.toInt() ?: 0,
                readingPlanProgressCount = docSnapshot.getLong("readingPlanProgressCount")?.toInt() ?: 0
            )
            Result.success(meta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
