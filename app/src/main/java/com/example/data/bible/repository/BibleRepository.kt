package com.example.data.bible.repository

import android.content.Context
import com.example.data.bible.local.BibleDatabase
import com.example.data.bible.local.BibleBookmarkEntity
import com.example.data.bible.local.BibleFavoriteEntity
import com.example.data.bible.local.BibleHighlightEntity
import com.example.data.bible.local.BibleNoteEntity
import com.example.data.bible.local.BibleVerseEntity
import com.example.data.bible.model.BibleBookDefinitions
import com.example.data.bible.model.BibleTranslation
import com.example.data.bible.model.BibleVerse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BibleRepository(
    private val context: Context,
    private val localDataSource: com.example.data.bible.local.BibleLocalDataSource? = null,
    private val remoteDataSource: com.example.data.bible.remote.BibleRemoteDataSource? = null
) {
    private val dao = BibleDatabase.getDatabase(context).bibleDao()
    private val localDs = localDataSource ?: com.example.data.bible.local.BibleLocalDataSource(context, dao)

    suspend fun reseedBibleDatabase() {
        localDs.reseedBibleDatabase()
    }

    fun getAvailableTranslationIds(): List<String> = BibleTranslation.ALL.map { it.id }

    suspend fun getVersesForCompare(translationId: String, bookId: Int, chapter: Int, verse: Int): List<BibleVerse> = withContext(Dispatchers.IO) {
        val list = dao.getVersesForChapterSync(translationId, bookId, chapter)
        val matched = list.filter { it.verseNumber == verse }
        val book = BibleBookDefinitions.getBookById(bookId)
        val bName = book?.nameHindi ?: "पुस्तक"
        matched.map { entity ->
            BibleVerse(
                bookId = entity.bookId,
                chapter = entity.chapter,
                verseNumber = entity.verseNumber,
                text = entity.text,
                bookName = bName,
                translationId = translationId
            )
        }
    }

    fun getChapterVerses(
        translationId: String,
        bookId: Int,
        chapter: Int,
        coroutineScope: CoroutineScope,
        dualHindiId: String = "HIOV",
        dualEnglishId: String = "NKJV"
    ): Flow<List<BibleVerse>> {
        val bookmarksFlow = dao.getAllBookmarks()
        val favoritesFlow = dao.getAllFavorites()
        val highlightsFlow = dao.getHighlightsForChapter(bookId, chapter)
        val notesFlow = dao.getNotesForChapter(bookId, chapter)

        val isDual = translationId == BibleTranslation.PARALLEL_HI_EN.id
        val targetTransId = if (isDual) dualHindiId else translationId
        val secondaryTransId = if (isDual) dualEnglishId else null

        coroutineScope.launch(Dispatchers.IO) {
            val localVerses = dao.getVersesForChapterSync(targetTransId, bookId, chapter)
            if (localVerses.isEmpty()) {
                val mockVerses = (1..15).map { vNum ->
                    BibleVerseEntity(
                        translationId = targetTransId,
                        bookId = bookId,
                        chapter = chapter,
                        verseNumber = vNum,
                        text = "यह परमेश्वर का पवित्र वचन है - पुस्तक $bookId, अध्याय $chapter, वचन $vNum",
                        bookName = BibleBookDefinitions.getBookById(bookId)?.nameHindi ?: "अध्याय"
                    )
                }
                dao.insertVerses(mockVerses)
            }
            if (secondaryTransId != null) {
                val localSec = dao.getVersesForChapterSync(secondaryTransId, bookId, chapter)
                if (localSec.isEmpty()) {
                    val mockSec = (1..15).map { vNum ->
                        BibleVerseEntity(
                            translationId = secondaryTransId,
                            bookId = bookId,
                            chapter = chapter,
                            verseNumber = vNum,
                            text = "This is the holy word - Book $bookId, Chapter $chapter, Verse $vNum",
                            bookName = BibleBookDefinitions.getBookById(bookId)?.nameEnglish ?: "Chapter"
                        )
                    }
                    dao.insertVerses(mockSec)
                }
            }
        }

        val versesFlow = dao.getVersesForChapter(targetTransId, bookId, chapter)
        val secondaryVersesFlow = if (secondaryTransId != null) {
            dao.getVersesForChapter(secondaryTransId, bookId, chapter)
        } else {
            flowOf(emptyList())
        }

        return combine(versesFlow, secondaryVersesFlow, bookmarksFlow, favoritesFlow, highlightsFlow, notesFlow) { args ->
            @Suppress("UNCHECKED_CAST")
            val verses = args[0] as List<BibleVerseEntity>
            @Suppress("UNCHECKED_CAST")
            val secondaryVerses = args[1] as List<BibleVerseEntity>
            @Suppress("UNCHECKED_CAST")
            val bookmarks = args[2] as List<BibleBookmarkEntity>
            @Suppress("UNCHECKED_CAST")
            val favorites = args[3] as List<BibleFavoriteEntity>
            @Suppress("UNCHECKED_CAST")
            val highlights = args[4] as List<BibleHighlightEntity>
            @Suppress("UNCHECKED_CAST")
            val notes = args[5] as List<BibleNoteEntity>

            val secondaryMap = secondaryVerses.associate { it.verseNumber to it.text }
            val bookmarkedSet = bookmarks.filter { it.bookId == bookId && it.chapter == chapter }.map { it.verse }.toSet()
            val favoriteSet = favorites.filter { it.bookId == bookId && it.chapter == chapter }.map { it.verse }.toSet()
            val highlightMap = highlights.associate { it.verse to it.colorHex }
            val noteMap = notes.associate { it.verse to it.noteText }

            val book = BibleBookDefinitions.getBookById(bookId)
            val bookName = book?.nameHindi ?: "अध्याय $chapter"

            verses.sortedBy { it.verseNumber }.map { entity ->
                BibleVerse(
                    bookId = entity.bookId,
                    chapter = entity.chapter,
                    verseNumber = entity.verseNumber,
                    text = entity.text,
                    secondaryText = secondaryMap[entity.verseNumber],
                    bookName = bookName,
                    translationId = targetTransId,
                    isBookmarked = bookmarkedSet.contains(entity.verseNumber),
                    isFavorite = favoriteSet.contains(entity.verseNumber),
                    highlightColor = highlightMap[entity.verseNumber],
                    note = noteMap[entity.verseNumber]
                )
            }
        }
    }

    suspend fun toggleBookmark(bookId: Int, chapter: Int, verse: Int, isBookmarked: Boolean) {
        if (isBookmarked) {
            dao.insertBookmark(BibleBookmarkEntity(bookId = bookId, chapter = chapter, verse = verse))
        } else {
            dao.deleteBookmark(bookId, chapter, verse)
        }
    }

    suspend fun toggleFavorite(bookId: Int, chapter: Int, verse: Int, isFavorite: Boolean) {
        if (isFavorite) {
            dao.insertFavorite(BibleFavoriteEntity(bookId = bookId, chapter = chapter, verse = verse))
        } else {
            dao.deleteFavorite(bookId, chapter, verse)
        }
    }

    suspend fun saveNote(bookId: Int, chapter: Int, verse: Int, text: String) {
        if (text.isBlank()) {
            dao.deleteNote(bookId, chapter, verse)
        } else {
            dao.saveNote(BibleNoteEntity(bookId = bookId, chapter = chapter, verse = verse, noteText = text))
        }
    }
}
