package com.example.data.bible.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.bible.local.BibleBookmarkEntity
import com.example.data.bible.local.BibleHighlightEntity
import com.example.data.bible.local.BibleLocalDataSource
import com.example.data.bible.local.BibleNoteEntity
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
     * Offline-first chapter verses with bookmarks, highlights, and notes merged.
     */
    fun getChapterVerses(
        translationId: String,
        bookId: Int,
        chapter: Int,
        coroutineScope: CoroutineScope
    ): Flow<List<BibleVerse>> {
        // Trigger background fetch if local chapter is not present yet
        coroutineScope.launch(Dispatchers.IO) {
            val localVerses = localDataSource.getVersesForChapterSync(translationId, bookId, chapter)
            if (localVerses.isEmpty()) {
                val remoteVerses = remoteDataSource.fetchChapterVerses(translationId, bookId, chapter)
                if (!remoteVerses.isNullOrEmpty()) {
                    localDataSource.saveVerses(remoteVerses)
                }
            }
        }

        val versesFlow = localDataSource.getVersesForChapter(translationId, bookId, chapter)
        val bookmarksFlow = localDataSource.getAllBookmarks()
        val highlightsFlow = localDataSource.getHighlightsForChapter(bookId, chapter)
        val notesFlow = localDataSource.getNotesForChapter(bookId, chapter)

        return combine(versesFlow, bookmarksFlow, highlightsFlow, notesFlow) { verses, bookmarks, highlights, notes ->
            val bookmarkedSet = bookmarks.filter { it.bookId == bookId && it.chapter == chapter }
                .map { it.verse }
                .toSet()

            val highlightMap = highlights.associate { it.verse to it.colorHex }
            val noteMap = notes.associate { it.verse to it.noteText }

            verses.map { verse ->
                verse.copy(
                    isBookmarked = bookmarkedSet.contains(verse.verseNumber),
                    highlightColor = highlightMap[verse.verseNumber],
                    note = noteMap[verse.verseNumber]
                )
            }
        }
    }

    suspend fun refreshChapter(translationId: String, bookId: Int, chapter: Int) = withContext(Dispatchers.IO) {
        val remoteVerses = remoteDataSource.fetchChapterVerses(translationId, bookId, chapter)
        if (!remoteVerses.isNullOrEmpty()) {
            localDataSource.saveVerses(remoteVerses)
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
}
