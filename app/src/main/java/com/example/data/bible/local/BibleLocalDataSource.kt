package com.example.data.bible.local

import android.content.Context
import android.util.Log
import com.example.data.bible.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
        private val STRONGS_PATTERN = Regex("<s.*?>.*?</s>", RegexOption.IGNORE_CASE)
        private val FOOTNOTE_PATTERN = Regex("<\b(?:f|fe|x|xref)\b.*?>.*?</(?:f|fe|x|xref)>", RegexOption.IGNORE_CASE)
        private val BRACKET_MARKER_PATTERN = Regex("\\[(?:xref|footnote|note|\\d+|[a-zA-Z])[^\\]]*\\]", RegexOption.IGNORE_CASE)

        /**
         * Robustly sanitizes and decodes Bible verse texts:
         * 1. Detects Base64 encoded Hindi or other strings (such as strings starting with 4KS...)
         * 2. Decodes Base64 to valid UTF-8 string
         * 3. Strips footnote superscripts (<sup>...</sup>), Strong's concordance tags (<S>...</S>), cross-references
         * 4. Strips bracketed technical markers ([xref-1], [a], [1], etc.)
         * 5. Strips remaining HTML tags and decodes HTML entities
         * 6. Normalizes whitespace for printed book typography
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

            // Strip footnote / cross-reference / Strong's markers
            text = text.replace(SUPERSCRIPT_PATTERN, "")
            text = text.replace(STRONGS_PATTERN, "")
            text = text.replace(FOOTNOTE_PATTERN, "")
            text = text.replace(BRACKET_MARKER_PATTERN, "")

            // Clean HTML tags and decode entities
            text = android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString()

            // Remove any residual brackets with single letters/numbers left over
            text = text.replace(Regex("\\[[0-9a-zA-Z]+\\]"), "")

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
                    val rawText = obj.optString("text", "")
                    val cleanText = decodeAndSanitizeVerseText(rawText)
                    entities.add(
                        BibleVerseEntity(
                            translationId = obj.optString("t", BibleTranslation.HINDI_IRV.id),
                            bookId = obj.optInt("b", 1),
                            chapter = obj.optInt("c", 1),
                            verse = obj.optInt("v", 1),
                            text = cleanText
                        )
                    )
                }

                if (entities.isNotEmpty()) {
                    bibleDao.insertVerses(entities)
                }
            }

            // Seed English Verses if needed
            val engCount = bibleDao.getVerseCount(BibleTranslation.ENGLISH_KJV.id)
            if (engCount < 400) {
                try {
                    val engJsonString = context.assets.open("bible/offline_english_verses.json")
                        .bufferedReader()
                        .use { it.readText() }

                    val engArray = JSONArray(engJsonString)
                    val engEntities = mutableListOf<BibleVerseEntity>()

                    for (i in 0 until engArray.length()) {
                        val obj = engArray.getJSONObject(i)
                        val rawText = obj.optString("text", "")
                        val cleanText = decodeAndSanitizeVerseText(rawText)
                        engEntities.add(
                            BibleVerseEntity(
                                translationId = BibleTranslation.ENGLISH_KJV.id,
                                bookId = obj.optInt("b", 1),
                                chapter = obj.optInt("c", 1),
                                verse = obj.optInt("v", 1),
                                text = cleanText
                            )
                        )
                    }

                    if (engEntities.isNotEmpty()) {
                        bibleDao.insertVerses(engEntities)
                    }
                } catch (e: Exception) {
                    Log.e("BibleLocalDataSource", "Error seeding offline English verses", e)
                }
            }

            // Seed Section Headings (ensure all 2,445+ verified headings across all 66 books are inserted)
            val headingCount = bibleDao.getHeadingCount(BibleTranslation.HINDI_IRV.id)
            if (headingCount < 2400) {
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
                                translationId = obj.optString("t", BibleTranslation.HINDI_IRV.id),
                                bookId = obj.optInt("b", 1),
                                chapter = obj.optInt("c", 1),
                                beforeVerse = obj.optInt("v", 1),
                                headingText = decodeAndSanitizeVerseText(obj.optString("h", ""))
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
        return bibleDao.getHeadingsForChapter(translationId, bookId, chapter).flatMapLatest { list ->
            if (list.isNotEmpty()) {
                flowOf(list.sortedBy { it.beforeVerse }.map { entity ->
                    BibleSectionHeading(
                        translationId = entity.translationId,
                        bookId = entity.bookId,
                        chapter = entity.chapter,
                        beforeVerse = entity.beforeVerse,
                        headingText = entity.headingText
                    )
                })
            } else {
                bibleDao.getHeadingsForChapter(BibleTranslation.HINDI_IRV.id, bookId, chapter).map { fallbackList ->
                    fallbackList.sortedBy { it.beforeVerse }.map { entity ->
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

    private var structuredJsonCache: org.json.JSONObject? = null

    suspend fun getStructuredChapter(bookId: Int, chapter: Int): List<BibleContentBlock> = withContext(Dispatchers.IO) {
        if (structuredJsonCache == null) {
            try {
                val jsonStr = context.assets.open("bible/offline_structured_bible.json")
                    .bufferedReader()
                    .use { it.readText() }
                structuredJsonCache = org.json.JSONObject(jsonStr)
            } catch (e: Exception) {
                Log.e("BibleLocalDataSource", "Error loading offline_structured_bible.json", e)
                structuredJsonCache = org.json.JSONObject()
            }
        }
        val key = "${bookId}_${chapter}"
        val blocksList = mutableListOf<BibleContentBlock>()
        val arr = structuredJsonCache?.optJSONArray(key)
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                when (obj.optString("type")) {
                    "SECTION_HEADING" -> {
                        blocksList.add(
                            BibleContentBlock.SectionHeading(
                                text = decodeAndSanitizeVerseText(obj.optString("text")),
                                beforeVerse = obj.optInt("beforeVerse", 1)
                            )
                        )
                    }
                    "TITLE" -> {
                        blocksList.add(
                            BibleContentBlock.Title(
                                text = decodeAndSanitizeVerseText(obj.optString("text")),
                                beforeVerse = obj.optInt("beforeVerse", 1)
                            )
                        )
                    }
                    "PROSE_PARAGRAPH" -> {
                        val versesArr = obj.optJSONArray("verses")
                        val verseItems = mutableListOf<VerseItem>()
                        if (versesArr != null) {
                            for (vIdx in 0 until versesArr.length()) {
                                val vObj = versesArr.getJSONObject(vIdx)
                                val fnsArr = vObj.optJSONArray("footnotes")
                                val fnList = mutableListOf<FootnoteItem>()
                                if (fnsArr != null) {
                                    for (fIdx in 0 until fnsArr.length()) {
                                        val fObj = fnsArr.getJSONObject(fIdx)
                                        fnList.add(
                                            FootnoteItem(
                                                ref = fObj.optString("ref"),
                                                target = fObj.optString("target"),
                                                text = fObj.optString("text")
                                            )
                                        )
                                    }
                                }
                                val vNum = if (vObj.has("verseNumber") && !vObj.isNull("verseNumber")) vObj.optInt("verseNumber") else null
                                verseItems.add(
                                    VerseItem(
                                        verseNumber = vNum,
                                        text = decodeAndSanitizeVerseText(vObj.optString("text")),
                                        footnotes = fnList
                                    )
                                )
                            }
                        }
                        if (verseItems.isNotEmpty()) {
                            blocksList.add(BibleContentBlock.ProseParagraph(verseItems))
                        }
                    }
                    "POETRY_BLOCK" -> {
                        val linesArr = obj.optJSONArray("lines")
                        val lineItems = mutableListOf<PoetryLineItem>()
                        if (linesArr != null) {
                            for (lIdx in 0 until linesArr.length()) {
                                val lObj = linesArr.getJSONObject(lIdx)
                                val fnsArr = lObj.optJSONArray("footnotes")
                                val fnList = mutableListOf<FootnoteItem>()
                                if (fnsArr != null) {
                                    for (fIdx in 0 until fnsArr.length()) {
                                        val fObj = fnsArr.getJSONObject(fIdx)
                                        fnList.add(
                                            FootnoteItem(
                                                ref = fObj.optString("ref"),
                                                target = fObj.optString("target"),
                                                text = fObj.optString("text")
                                            )
                                        )
                                    }
                                }
                                val vNum = if (lObj.has("verseNumber") && !lObj.isNull("verseNumber")) lObj.optInt("verseNumber") else null
                                lineItems.add(
                                    PoetryLineItem(
                                        indent = lObj.optInt("indent", 0),
                                        verseNumber = vNum,
                                        text = decodeAndSanitizeVerseText(lObj.optString("text")),
                                        footnotes = fnList
                                    )
                                )
                            }
                        }
                        if (lineItems.isNotEmpty()) {
                            blocksList.add(BibleContentBlock.PoetryBlock(lineItems))
                        }
                    }
                }
            }
        }
        blocksList
    }
}
