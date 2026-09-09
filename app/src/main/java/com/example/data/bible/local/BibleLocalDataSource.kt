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
    companion object {
        private val BASE64_PATTERN = Regex("^[A-Za-z0-9+/=]{20,}$")
        private val SUPERSCRIPT_PATTERN = Regex("<sup.*?>.*?</sup>", RegexOption.IGNORE_CASE)

        /**
         * Robustly sanitizes and decodes Bible verse texts:
         * 1. Detects Base64 encoded Hindi or other strings (such as strings starting with 4KS...)
         * 2. Decodes Base64 to valid UTF-8 string
         * 3. Strips footnote superscripts (<sup>...</sup>)
         * 4. Strips HTML tags and unescapes HTML entities
         * 5. Normalizes whitespace
         */
        fun decodeAndSanitizeVerseText(raw: String): String {
            var text = raw.trim()
            if (text.isEmpty()) return ""

            // Check for Base64 encoded payload
            if (text.startsWith("4KS") || (text.length >= 24 && BASE64_PATTERN.matches(text))) {
                try {
                    val decodedBytes = java.util.Base64.getDecoder().decode(text)
                    val decodedStr = String(decodedBytes, Charsets.UTF_8)
                    if (decodedStr.isNotEmpty() && !decodedStr.contains("\uFFFD")) {
                        text = decodedStr
                    }
                } catch (e: Exception) {
                    // Fall back to original text if decoding fails
                }
            }

            // Strip footnote / cross-reference superscripts
            text = text.replace(SUPERSCRIPT_PATTERN, "")

            // Clean HTML tags and decode entities
            text = android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString()

            // Normalize whitespace
            return text.replace(Regex("\\s+"), " ").trim()
        }
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        try {
            val count = bibleDao.getVerseCount(BibleTranslation.HINDI_IRV.id)
            // If less than 400 verses exist, re-seed so all complete chapters are present
            if (count < 400) {
                val jsonString = context.assets.open("bible/offline_verses.json")
                    .bufferedReader()
                    .use { it.readText() }

                val jsonArray = JSONArray(jsonString)
                val entities = mutableListOf<BibleVerseEntity>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawText = obj.getString("text")
                    val cleanText = decodeAndSanitizeVerseText(rawText)
                    entities.add(
                        BibleVerseEntity(
                            translationId = obj.getString("t"),
                            bookId = obj.getInt("b"),
                            chapter = obj.getInt("c"),
                            verse = obj.getInt("v"),
                            text = cleanText
                        )
                    )
                }

                if (entities.isNotEmpty()) {
                    bibleDao.insertVerses(entities)
                }
            }

            // Seed Section Headings
            val headingCount = bibleDao.getHeadingCount(BibleTranslation.HINDI_IRV.id)
            if (headingCount < 30) {
                try {
                    val headingsJson = context.assets.open("bible/offline_headings.json")
                        .bufferedReader()
                        .use { it.readText() }

                    val hArray = JSONArray(headingsJson)
                    val headingEntities = mutableListOf<BibleHeadingEntity>()

                    for (i in 0 until hArray.length()) {
                        val obj = hArray.getJSONObject(i)
                        headingEntities.add(
                            BibleHeadingEntity(
                                translationId = obj.getString("t"),
                                bookId = obj.getInt("b"),
                                chapter = obj.getInt("c"),
                                beforeVerse = obj.getInt("v"),
                                headingText = decodeAndSanitizeVerseText(obj.getString("h"))
                            )
                        )
                    }

                    if (headingEntities.isNotEmpty()) {
                        bibleDao.insertHeadings(headingEntities)
                    }
                } catch (e: Exception) {
                    Log.e("BibleLocalDataSource", "Error seeding offline headings", e)
                }
            }
        } catch (e: Exception) {
            Log.e("BibleLocalDataSource", "Error seeding offline verses", e)
        }
    }

    fun getHeadingsForChapter(translationId: String, bookId: Int, chapter: Int): Flow<List<BibleSectionHeading>> {
        return bibleDao.getHeadingsForChapter(translationId, bookId, chapter).map { list ->
            list.sortedBy { it.beforeVerse }.map { entity ->
                BibleSectionHeading(
                    translationId = entity.translationId,
                    bookId = entity.bookId,
                    chapter = entity.chapter,
                    beforeVerse = entity.beforeVerse,
                    headingText = entity.headingText
                )
            }
        }
    }

    suspend fun getHeadingsForChapterSync(translationId: String, bookId: Int, chapter: Int): List<BibleSectionHeading> {
        return bibleDao.getHeadingsForChapterSync(translationId, bookId, chapter).sortedBy { it.beforeVerse }.map { entity ->
            BibleSectionHeading(
                translationId = entity.translationId,
                bookId = entity.bookId,
                chapter = entity.chapter,
                beforeVerse = entity.beforeVerse,
                headingText = entity.headingText
            )
        }
    }

    fun getVersesForChapter(translationId: String, bookId: Int, chapter: Int): Flow<List<BibleVerse>> {
        return bibleDao.getVersesForChapter(translationId, bookId, chapter).map { entities ->
            val book = BibleBookDefinitions.getBookById(bookId)
            val bookName = if (translationId.startsWith("HIN")) {
                book?.nameHindi ?: "अध्याय $chapter"
            } else {
                book?.nameEnglish ?: "Chapter $chapter"
            }
            entities.sortedBy { it.verse }.map { entity ->
                BibleVerse(
                    bookId = entity.bookId,
                    bookName = bookName,
                    chapter = entity.chapter,
                    verseNumber = entity.verse,
                    text = decodeAndSanitizeVerseText(entity.text),
                    translationId = entity.translationId
                )
            }
        }
    }

    suspend fun getVersesForChapterSync(translationId: String, bookId: Int, chapter: Int): List<BibleVerseEntity> {
        return bibleDao.getVersesForChapterSync(translationId, bookId, chapter).sortedBy { it.verse }
    }

    suspend fun saveVerses(verses: List<BibleVerseEntity>) {
        val sanitized = verses.map { it.copy(text = decodeAndSanitizeVerseText(it.text)) }
        bibleDao.insertVerses(sanitized)
    }

    suspend fun replaceChapterVerses(translationId: String, bookId: Int, chapter: Int, verses: List<BibleVerseEntity>) {
        val sanitized = verses.map { it.copy(text = decodeAndSanitizeVerseText(it.text)) }
        bibleDao.replaceChapterVerses(translationId, bookId, chapter, sanitized)
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
                    text = decodeAndSanitizeVerseText(entity.text),
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
