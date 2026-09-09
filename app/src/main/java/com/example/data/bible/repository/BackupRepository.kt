package com.example.data.bible.repository

import android.content.Context
import com.example.data.bible.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val appVersion: String,
    val timestamp: Long,
    val bookmarkCount: Int,
    val highlightCount: Int,
    val noteCount: Int,
    val dedicatedNoteCount: Int,
    val customSongCount: Int
)

class BackupRepository(
    private val context: Context,
    private val bibleDao: BibleDao
) {

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val bookmarks = bibleDao.getAllBookmarks().first()
        val highlights = bibleDao.getAllHighlights().first()
        val notes = bibleDao.getAllNotes().first()
        val dedicatedNotes = bibleDao.getAllDedicatedNotes().first()
        val songs = bibleDao.getAllSongs().first().filter { it.isUserCreated || it.isFavorite }

        val root = JSONObject()
        root.put("version", "1.2")
        root.put("timestamp", System.currentTimeMillis())
        root.put("app", "Vinay Kumar AVJ")

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
            obj.put("timestamp", bm.timestamp)
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
            obj.put("timestamp", hl.timestamp)
            hlArray.put(obj)
        }
        root.put("highlights", hlArray)

        // Notes
        val noteArray = JSONArray()
        notes.forEach { n ->
            val obj = JSONObject()
            obj.put("bookId", n.bookId)
            obj.put("bookName", n.bookName)
            obj.put("chapter", n.chapter)
            obj.put("verse", n.verse)
            obj.put("noteText", n.noteText)
            obj.put("timestamp", n.timestamp)
            noteArray.put(obj)
        }
        root.put("notes", noteArray)

        // Dedicated Notes
        val dnArray = JSONArray()
        dedicatedNotes.forEach { dn ->
            val obj = JSONObject()
            obj.put("id", dn.id)
            obj.put("title", dn.title)
            obj.put("content", dn.content)
            obj.put("colorHex", dn.colorHex)
            obj.put("textColorHex", dn.textColorHex)
            obj.put("linkedReferences", dn.linkedReferences)
            obj.put("createdAt", dn.createdAt)
            obj.put("modifiedAt", dn.modifiedAt)
            dnArray.put(obj)
        }
        root.put("dedicatedNotes", dnArray)

        // Songs
        val songArray = JSONArray()
        songs.forEach { s ->
            val obj = JSONObject()
            obj.put("title", s.title)
            obj.put("content", s.content)
            obj.put("artist", s.artist)
            obj.put("category", s.category)
            obj.put("isFavorite", s.isFavorite)
            obj.put("isUserCreated", s.isUserCreated)
            obj.put("createdAt", s.createdAt)
            obj.put("modifiedAt", s.modifiedAt)
            songArray.put(obj)
        }
        root.put("songs", songArray)

        root.toString(2)
    }

    suspend fun saveBackupToLocalFile(): File = withContext(Dispatchers.IO) {
        val json = createBackupJson()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "vinay_backup_$dateStr.json"
        val file = File(context.filesDir, fileName)
        file.writeText(json)
        file
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
                        bookName = obj.getString("bookName"),
                        chapter = obj.getInt("chapter"),
                        verse = obj.getInt("verse"),
                        translationId = obj.optString("translationId", "hi_irv"),
                        verseText = obj.optString("verseText", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
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
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
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
                        noteText = obj.getString("noteText"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    bibleDao.saveNote(note)
                    restoredCount++
                }
            }

            // Dedicated notes
            if (root.has("dedicatedNotes")) {
                val dnArray = root.getJSONArray("dedicatedNotes")
                for (i in 0 until dnArray.length()) {
                    val obj = dnArray.getJSONObject(i)
                    val dn = DedicatedNoteEntity(
                        title = obj.optString("title", "Untitled Note"),
                        content = obj.optString("content", ""),
                        colorHex = obj.optString("colorHex", "#FFFBEB"),
                        textColorHex = obj.optString("textColorHex", "#1E293B"),
                        linkedReferences = obj.optString("linkedReferences", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        modifiedAt = obj.optLong("modifiedAt", System.currentTimeMillis())
                    )
                    bibleDao.insertDedicatedNote(dn)
                    restoredCount++
                }
            }

            // Songs
            if (root.has("songs")) {
                val songArray = root.getJSONArray("songs")
                for (i in 0 until songArray.length()) {
                    val obj = songArray.getJSONObject(i)
                    val song = ChristianSongEntity(
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        artist = obj.optString("artist", "Vinay Kumar AVJ"),
                        category = obj.optString("category", "Worship"),
                        isFavorite = obj.optBoolean("isFavorite", true),
                        isUserCreated = obj.optBoolean("isUserCreated", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        modifiedAt = obj.optLong("modifiedAt", System.currentTimeMillis())
                    )
                    bibleDao.insertSong(song)
                    restoredCount++
                }
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
