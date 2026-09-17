package com.example.data.bible.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.bible.local.BibleBookmarkEntity
import com.example.data.bible.local.BibleFavoriteVerseEntity
import com.example.data.bible.local.BibleHighlightEntity
import com.example.data.bible.local.BibleLocalDataSource
import com.example.data.bible.local.BibleNoteEntity
import com.example.data.bible.local.BibleVerseEntity
import com.example.data.bible.local.ReadingPositionEntity
import com.example.data.bible.model.BibleBookDefinitions
import com.example.data.bible.model.BibleVerse
import com.example.data.bible.remote.BibleRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BibleRepository(
    private val context: Context,
    private val localDataSource: BibleLocalDataSource,
    private val remoteDataSource: BibleRemoteDataSource
) {
    suspend fun initialize() {
        com.example.data.bible.model.BibleVerseCounts.initialize(context)
        localDataSource.ensureSeeded()
    }

    fun isOnline(): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates whether a chapter's verses start at 1 and proceed sequentially without missing verses.
     */
    fun isVerseSequenceComplete(verses: List<BibleVerseEntity>): Boolean {
        if (verses.isEmpty()) return false
        val sorted = verses.sortedBy { it.verse }
        if (sorted.first().verse != 1) return false
        for (i in 0 until sorted.size - 1) {
            if (sorted[i + 1].verse != sorted[i].verse + 1) {
                return false // Discontinuity found!
            }
        }
        return true
    }

    /**
     * Offline-first chapter verses with bookmarks, favorites, highlights, and notes merged.
     * Automatically verifies sequence completeness and re-fetches if verses are missing.
     */
    fun getChapterVerses(
        translationId: String,
        bookId: Int,
        chapter: Int,
        coroutineScope: CoroutineScope,
        dualHindiId: String = com.example.data.bible.model.BibleTranslation.HIOV.id,
        dualEnglishId: String = com.example.data.bible.model.BibleTranslation.ENGLISH_ESV.id
    ): Flow<List<BibleVerse>> {
        val bookmarksFlow = localDataSource.getAllBookmarks()
        val favoritesFlow = localDataSource.getAllFavorites()
        val highlightsFlow = localDataSource.getHighlightsForChapter(bookId, chapter)
        val notesFlow = localDataSource.getNotesForChapter(bookId, chapter)

        if (translationId == com.example.data.bible.model.BibleTranslation.PARALLEL_HI_EN.id) {
            // Asynchronously fetch both chosen Hindi and English translations if not present locally
            coroutineScope.launch(Dispatchers.IO) {
                // Check Hindi
                val localHin = localDataSource.getVersesForChapterSync(dualHindiId, bookId, chapter)
                if (!isVerseSequenceComplete(localHin)) {
                    val remoteHin = remoteDataSource.fetchChapterVerses(dualHindiId, bookId, chapter)
                    if (!remoteHin.isNullOrEmpty()) {
                        localDataSource.replaceChapterVerses(dualHindiId, bookId, chapter, remoteHin)
                    }
                }
                // Check English
                val localEng = localDataSource.getVersesForChapterSync(dualEnglishId, bookId, chapter)
                if (!isVerseSequenceComplete(localEng)) {
                    val remoteEng = remoteDataSource.fetchChapterVerses(dualEnglishId, bookId, chapter)
                    if (!remoteEng.isNullOrEmpty()) {
                        localDataSource.replaceChapterVerses(dualEnglishId, bookId, chapter, remoteEng)
                    }
                }
            }

            val hinVersesFlow = localDataSource.getVersesForChapter(dualHindiId, bookId, chapter)
            val engVersesFlow = localDataSource.getVersesForChapter(dualEnglishId, bookId, chapter)

            return combine(hinVersesFlow, engVersesFlow, bookmarksFlow, favoritesFlow, highlightsFlow, notesFlow) { args: Array<Any> ->
                @Suppress("UNCHECKED_CAST")
                val hinVerses = args[0] as List<BibleVerse>
                @Suppress("UNCHECKED_CAST")
                val engVerses = args[1] as List<BibleVerse>
                @Suppress("UNCHECKED_CAST")
                val bookmarks = args[2] as List<BibleBookmarkEntity>
                @Suppress("UNCHECKED_CAST")
                val favorites = args[3] as List<BibleFavoriteVerseEntity>
                @Suppress("UNCHECKED_CAST")
                val highlights = args[4] as List<BibleHighlightEntity>
                @Suppress("UNCHECKED_CAST")
                val notes = args[5] as List<BibleNoteEntity>

                val bookmarkedSet = bookmarks.filter { it.bookId == bookId && it.chapter == chapter }
                    .map { it.verse }
                    .toSet()

                val favoriteSet = favorites.filter { it.bookId == bookId && it.chapter == chapter }
                    .map { it.verse }
                    .toSet()

                val highlightMap = highlights.associate { it.verse to it.colorHex }
                val noteMap = notes.associate { it.verse to it.noteText }

                val engMap = engVerses.associateBy { it.verseNumber }
                val book = BibleBookDefinitions.getBookById(bookId)
                val combinedBookName = "${book?.nameHindi ?: ""} (${book?.nameEnglish ?: ""})"

                // If Hindi verses are available, iterate them; if not yet, iterate English verses
                val baseList = if (hinVerses.isNotEmpty()) hinVerses else engVerses

                baseList.sortedBy { it.verseNumber }.map { base ->
                    val secondary = if (hinVerses.isNotEmpty()) engMap[base.verseNumber] else null
                    val primaryText = base.text
                    val secondaryText = secondary?.text

                    base.copy(
                        bookName = combinedBookName,
                        translationId = com.example.data.bible.model.BibleTranslation.PARALLEL_HI_EN.id,
                        text = primaryText,
                        secondaryText = secondaryText,
                        isBookmarked = bookmarkedSet.contains(base.verseNumber),
                        isFavorite = favoriteSet.contains(base.verseNumber),
                        highlightColor = highlightMap[base.verseNumber],
                        note = noteMap[base.verseNumber]
                    )
                }
            }
        }

        // Single Translation Mode (Hindi or English)
        coroutineScope.launch(Dispatchers.IO) {
            val localVerses = localDataSource.getVersesForChapterSync(translationId, bookId, chapter)
            val isComplete = isVerseSequenceComplete(localVerses)
            if (!isComplete) {
                val remoteVerses = remoteDataSource.fetchChapterVerses(translationId, bookId, chapter)
                if (!remoteVerses.isNullOrEmpty()) {
                    localDataSource.replaceChapterVerses(translationId, bookId, chapter, remoteVerses)
                } else {
                    // Fallback to local default translation if remote is unavailable
                    val fallbackId = if (translationId.startsWith("ENG") || translationId.equals("ESV", ignoreCase = true)) {
                        com.example.data.bible.model.BibleTranslation.ENGLISH_ESV.id
                    } else {
                        com.example.data.bible.model.BibleTranslation.HIOV.id
                    }
                    val fallbackLocal = localDataSource.getVersesForChapterSync(fallbackId, bookId, chapter)
                    if (fallbackLocal.isNotEmpty()) {
                        val mappedEntities = fallbackLocal.map { entity ->
                            entity.copy(translationId = translationId)
                        }
                        localDataSource.replaceChapterVerses(translationId, bookId, chapter, mappedEntities)
                    }
                }
            }
        }

        val versesFlow = localDataSource.getVersesForChapter(translationId, bookId, chapter)
        val commentariesFlow = localDataSource.getCommentariesForChapter(translationId, bookId, chapter)

        return combine(versesFlow, bookmarksFlow, favoritesFlow, highlightsFlow, notesFlow, commentariesFlow) { args: Array<Any> ->
            @Suppress("UNCHECKED_CAST")
            val verses = args[0] as List<BibleVerse>
            @Suppress("UNCHECKED_CAST")
            val bookmarks = args[1] as List<BibleBookmarkEntity>
            @Suppress("UNCHECKED_CAST")
            val favorites = args[2] as List<BibleFavoriteVerseEntity>
            @Suppress("UNCHECKED_CAST")
            val highlights = args[3] as List<BibleHighlightEntity>
            @Suppress("UNCHECKED_CAST")
            val notes = args[4] as List<BibleNoteEntity>
            @Suppress("UNCHECKED_CAST")
            val commentaries = args[5] as List<com.example.data.bible.local.BibleCommentaryEntity>

            val bookmarkedSet = bookmarks.filter { it.bookId == bookId && it.chapter == chapter }
                .map { it.verse }
                .toSet()

            val favoriteSet = favorites.filter { it.bookId == bookId && it.chapter == chapter }
                .map { it.verse }
                .toSet()

            val highlightMap = highlights.associate { it.verse to it.colorHex }
            val noteMap = notes.associate { it.verse to it.noteText }
            val commentaryMap = commentaries.groupBy { it.verseFrom }

            verses.sortedBy { it.verseNumber }.map { verse ->
                val comms = commentaryMap[verse.verseNumber]
                val commText = comms?.joinToString("\n\n") { it.text }
                verse.copy(
                    isBookmarked = bookmarkedSet.contains(verse.verseNumber),
                    isFavorite = favoriteSet.contains(verse.verseNumber),
                    highlightColor = highlightMap[verse.verseNumber],
                    note = noteMap[verse.verseNumber],
                    commentaryText = commText
                )
            }
        }
    }

    fun getChapterHeadings(
        translationId: String,
        bookId: Int,
        chapter: Int
    ): Flow<List<com.example.data.bible.model.BibleSectionHeading>> {
        return localDataSource.getHeadingsForChapter(translationId, bookId, chapter)
    }

    suspend fun refreshChapter(translationId: String, bookId: Int, chapter: Int): Boolean = withContext(Dispatchers.IO) {
        val remoteVerses = remoteDataSource.fetchChapterVerses(translationId, bookId, chapter)
        if (!remoteVerses.isNullOrEmpty()) {
            localDataSource.replaceChapterVerses(translationId, bookId, chapter, remoteVerses)
            true
        } else {
            false
        }
    }

    fun search(translationId: String, query: String): Flow<List<BibleVerse>> {
        return localDataSource.searchVerses(translationId, query)
    }

    // Bookmarks
    fun getAllBookmarks(): Flow<List<BibleBookmarkEntity>> = localDataSource.getAllBookmarks()

    suspend fun toggleBookmark(bookId: Int, chapter: Int, verse: Int, translationId: String, verseText: String) {
        val book = BibleBookDefinitions.getBookById(bookId)
        val bookName = book?.nameHindi ?: "अध्याय $chapter"
        val existing = localDataSource.getVersesForChapterSync(translationId, bookId, chapter)
        val text = existing.find { it.verse == verse }?.text ?: verseText

        localDataSource.addBookmark(bookId, bookName, chapter, verse, translationId, text)
    }

    suspend fun removeBookmark(bookId: Int, chapter: Int, verse: Int) {
        localDataSource.removeBookmark(bookId, chapter, verse)
    }

    suspend fun removeBookmarkById(id: Long) {
        localDataSource.removeBookmarkById(id)
    }

    // Favorites
    fun getAllFavorites(): Flow<List<BibleFavoriteVerseEntity>> = localDataSource.getAllFavorites()

    suspend fun toggleFavorite(bookId: Int, chapter: Int, verse: Int, translationId: String, verseText: String) {
        val book = BibleBookDefinitions.getBookById(bookId)
        val bookName = book?.nameHindi ?: "अध्याय $chapter"
        val existing = localDataSource.getVersesForChapterSync(translationId, bookId, chapter)
        val text = existing.find { it.verse == verse }?.text ?: verseText

        localDataSource.addFavorite(bookId, bookName, chapter, verse, translationId, text)
    }

    suspend fun removeFavorite(bookId: Int, chapter: Int, verse: Int) {
        localDataSource.removeFavorite(bookId, chapter, verse)
    }

    suspend fun removeFavoriteById(id: Long) {
        localDataSource.removeFavoriteById(id)
    }

    // Highlights
    fun getAllHighlights(): Flow<List<BibleHighlightEntity>> = localDataSource.getAllHighlights()

    suspend fun setHighlight(bookId: Int, chapter: Int, verse: Int, colorHex: String) {
        localDataSource.setHighlight(bookId, chapter, verse, colorHex)
    }

    suspend fun removeHighlight(bookId: Int, chapter: Int, verse: Int) {
        localDataSource.removeHighlight(bookId, chapter, verse)
    }

    // Notes
    fun getAllNotes(): Flow<List<BibleNoteEntity>> = localDataSource.getAllNotes()

    suspend fun saveNote(bookId: Int, chapter: Int, verse: Int, noteText: String) {
        val book = BibleBookDefinitions.getBookById(bookId)
        val bookName = book?.nameHindi ?: "अध्याय $chapter"
        localDataSource.saveNote(bookId, bookName, chapter, verse, noteText)
    }

    suspend fun deleteNote(bookId: Int, chapter: Int, verse: Int) {
        localDataSource.deleteNote(bookId, chapter, verse)
    }

    // Reading Position
    fun getReadingPosition(): Flow<ReadingPositionEntity?> = localDataSource.getReadingPosition()

    suspend fun saveReadingPosition(bookId: Int, chapter: Int, verse: Int, translationId: String) {
        val book = BibleBookDefinitions.getBookById(bookId)
        val bookName = book?.nameHindi ?: "अध्याय $chapter"
        localDataSource.saveReadingPosition(bookId, bookName, chapter, verse, translationId)
    }

    suspend fun getStructuredChapter(bookId: Int, chapter: Int): List<com.example.data.bible.model.BibleContentBlock> {
        return localDataSource.getStructuredChapter(bookId, chapter)
    }

    // Commentaries
    fun getCommentariesForChapter(translationId: String, bookId: Int, chapter: Int) =
        localDataSource.getCommentariesForChapter(translationId, bookId, chapter)

    fun getCommentariesForVerse(translationId: String, bookId: Int, chapter: Int, verse: Int) =
        localDataSource.getCommentariesForVerse(translationId, bookId, chapter, verse)
}
