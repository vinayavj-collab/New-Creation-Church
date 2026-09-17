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
    val studyNoteCount: Int,
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
        val studyNotes = try { bibleDao.getAllStudyNotesByDate().first() } catch (e: Exception) { emptyList() }
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

        // Songs
        val songArray = JSONArray()
        songs.forEach { s ->
            val obj = JSONObject()
            obj.put("songNumber", s.songNumber)
            obj.put("title", s.title)
            obj.put("content", s.content)
            obj.put("artist", s.artist)
            obj.put("category", s.category)
            obj.put("keyScale", s.keyScale)
            obj.put("colorHex", s.colorHex)
            obj.put("textColorHex", s.textColorHex)
            obj.put("linkedReferences", s.linkedReferences)
            obj.put("personalNotes", s.personalNotes)
            obj.put("blogPostId", s.blogPostId)
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
                        bookName = obj.optString("bookName", "Bible"),
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
                        noteText = obj.optString("noteText", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
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

            // Songs
            if (root.has("songs")) {
                val songArray = root.getJSONArray("songs")
                for (i in 0 until songArray.length()) {
                    val obj = songArray.getJSONObject(i)
                    val song = ChristianSongEntity(
                        songNumber = obj.optInt("songNumber", 0),
                        title = obj.optString("title", "Untitled"),
                        content = obj.optString("content", ""),
                        artist = obj.optString("artist", "Vinay Kumar AVJ"),
                        category = obj.optString("category", "Worship"),
                        keyScale = obj.optString("keyScale", "D"),
                        colorHex = obj.optString("colorHex", "#FFFBEB"),
                        textColorHex = obj.optString("textColorHex", "#1E293B"),
                        linkedReferences = obj.optString("linkedReferences", ""),
                        personalNotes = obj.optString("personalNotes", ""),
                        blogPostId = obj.optString("blogPostId", ""),
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
