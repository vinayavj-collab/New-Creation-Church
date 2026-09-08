package com.example.data.bible.local

import android.content.Context
import android.util.Log
import com.example.data.bible.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray

class BibleLocalDataSource(
    private val context: Context,
    private val bibleDao: BibleDao
) {
    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        try {
            val count = bibleDao.getVerseCount(BibleTranslation.HINDI_IRV.id)
            if (count > 0) return@withContext

            val jsonString = context.assets.open("bible/offline_verses.json")
                .bufferedReader()
                .use { it.readText() }

            val jsonArray = JSONArray(jsonString)
            val entities = mutableListOf<BibleVerseEntity>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                entities.add(
                    BibleVerseEntity(
                        translationId = obj.getString("t"),
                        bookId = obj.getInt("b"),
                        chapter = obj.getInt("c"),
                        verse = obj.getInt("v"),
                        text = obj.getString("text")
                    )
                )
            }

            if (entities.isNotEmpty()) {
                bibleDao.insertVerses(entities)
            }
        } catch (e: Exception) {
            Log.e("BibleLocalDataSource", "Error seeding offline verses", e)
        }
    }

    fun getVersesForChapter(translationId: String, bookId: Int, chapter: Int): Flow<List<BibleVerse>> {
        return bibleDao.getVersesForChapter(translationId, bookId, chapter).map { entities ->
            val book = BibleBookDefinitions.getBookById(bookId)
            val bookName = book?.nameHindi ?: "अध्याय $chapter"
            entities.map { entity ->
                BibleVerse(
                    bookId = entity.bookId,
                    bookName = bookName,
                    chapter = entity.chapter,
                    verseNumber = entity.verse,
                    text = entity.text,
                    translationId = entity.translationId
                )
            }
        }
    }

    suspend fun getVersesForChapterSync(translationId: String, bookId: Int, chapter: Int): List<BibleVerseEntity> {
        return bibleDao.getVersesForChapterSync(translationId, bookId, chapter)
    }

    suspend fun saveVerses(verses: List<BibleVerseEntity>) {
        bibleDao.insertVerses(verses)
    }

    fun searchVerses(translationId: String, query: String): Flow<List<BibleVerse>> {
        return bibleDao.searchVerses(translationId, query).map { entities ->
            entities.map { entity ->
                val book = BibleBookDefinitions.getBookById(entity.bookId)
                val bookName = if (translationId.startsWith("HIN")) {
                    book?.nameHindi ?: "पुस्तक ${entity.bookId}"
                } else {
                    book?.nameEnglish ?: "Book ${entity.bookId}"
                }
                BibleVerse(
                    bookId = entity.bookId,
                    bookName = bookName,
                    chapter = entity.chapter,
                    verseNumber = entity.verse,
                    text = entity.text,
                    translationId = entity.translationId
                )
            }
        }
    }

    // Bookmarks
    fun getAllBookmarks(): Flow<List<BibleBookmarkEntity>> = bibleDao.getAllBookmarks()

    fun isVerseBookmarked(bookId: Int, chapter: Int, verse: Int): Flow<Boolean> =
        bibleDao.isVerseBookmarked(bookId, chapter, verse)

    suspend fun addBookmark(bookId: Int, bookName: String, chapter: Int, verse: Int, translationId: String, text: String) {
        bibleDao.insertBookmark(
            BibleBookmarkEntity(
                bookId = bookId,
                bookName = bookName,
                chapter = chapter,
                verse = verse,
                translationId = translationId,
                verseText = text
            )
        )
    }

    suspend fun removeBookmark(bookId: Int, chapter: Int, verse: Int) {
        bibleDao.deleteBookmark(bookId, chapter, verse)
    }

    suspend fun removeBookmarkById(id: Long) {
        bibleDao.deleteBookmarkById(id)
    }

    // Highlights
    fun getHighlightsForChapter(bookId: Int, chapter: Int): Flow<List<BibleHighlightEntity>> =
        bibleDao.getHighlightsForChapter(bookId, chapter)

    fun getAllHighlights(): Flow<List<BibleHighlightEntity>> = bibleDao.getAllHighlights()

    suspend fun setHighlight(bookId: Int, chapter: Int, verse: Int, colorHex: String) {
        bibleDao.setHighlight(
            BibleHighlightEntity(
                bookId = bookId,
                chapter = chapter,
                verse = verse,
                colorHex = colorHex
            )
        )
    }

    suspend fun removeHighlight(bookId: Int, chapter: Int, verse: Int) {
        bibleDao.removeHighlight(bookId, chapter, verse)
    }

    // Notes
    fun getNotesForChapter(bookId: Int, chapter: Int): Flow<List<BibleNoteEntity>> =
        bibleDao.getNotesForChapter(bookId, chapter)

    fun getAllNotes(): Flow<List<BibleNoteEntity>> = bibleDao.getAllNotes()

    suspend fun saveNote(bookId: Int, bookName: String, chapter: Int, verse: Int, noteText: String) {
        bibleDao.saveNote(
            BibleNoteEntity(
                bookId = bookId,
                bookName = bookName,
                chapter = chapter,
                verse = verse,
                noteText = noteText
            )
        )
    }

    suspend fun deleteNote(bookId: Int, chapter: Int, verse: Int) {
        bibleDao.deleteNote(bookId, chapter, verse)
    }

    // Reading Position
    fun getReadingPosition(): Flow<ReadingPositionEntity?> = bibleDao.getReadingPosition()

    suspend fun saveReadingPosition(bookId: Int, bookName: String, chapter: Int, verse: Int, translationId: String) {
        bibleDao.saveReadingPosition(
            ReadingPositionEntity(
                bookId = bookId,
                bookName = bookName,
                chapter = chapter,
                verse = verse,
                translationId = translationId
            )
        )
    }
}
