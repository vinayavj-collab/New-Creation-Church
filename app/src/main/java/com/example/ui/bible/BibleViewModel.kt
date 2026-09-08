package com.example.ui.bible

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.bible.local.BibleBookmarkEntity
import com.example.data.bible.local.BibleDatabase
import com.example.data.bible.local.BibleHighlightEntity
import com.example.data.bible.local.BibleLocalDataSource
import com.example.data.bible.local.BibleNoteEntity
import com.example.data.bible.local.ReadingPositionEntity
import com.example.data.bible.model.*
import com.example.data.bible.remote.BibleRemoteDataSource
import com.example.data.bible.repository.BibleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BibleViewModel(
    application: Application,
    private val repository: BibleRepository
) : AndroidViewModel(application) {

    private val _selectedTranslation = MutableStateFlow(BibleTranslation.HINDI_IRV)
    val selectedTranslation: StateFlow<BibleTranslation> = _selectedTranslation.asStateFlow()

    private val _currentBook = MutableStateFlow(BibleBookDefinitions.getBookById(43) ?: BibleBookDefinitions.books.first()) // Default to John / यूहन्ना
    val currentBook: StateFlow<BibleBook> = _currentBook.asStateFlow()

    private val _currentChapter = MutableStateFlow(1)
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()

    private val _targetVerse = MutableStateFlow<Int?>(null)
    val targetVerse: StateFlow<Int?> = _targetVerse.asStateFlow()

    private val _readingSettings = MutableStateFlow(BibleReadingSettings())
    val readingSettings: StateFlow<BibleReadingSettings> = _readingSettings.asStateFlow()

    private val _isOnline = MutableStateFlow(repository.isOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val todayVerse: VerseOfTheDay = VerseOfTheDay.getTodayVerse()

    // Verses flow bound to current book, chapter, and translation
    val verses: StateFlow<List<BibleVerse>> = combine(
        _selectedTranslation,
        _currentBook,
        _currentChapter
    ) { translation, book, chapter ->
        Triple(translation, book, chapter)
    }.flatMapLatest { (translation, book, chapter) ->
        repository.getChapterVerses(translation.id, book.id, chapter, viewModelScope)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Search query & results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<BibleVerse>> = combine(
        _searchQuery,
        _selectedTranslation
    ) { query, translation ->
        Pair(query.trim(), translation.id)
    }.flatMapLatest { (query, translationId) ->
        if (query.length < 2) {
            flowOf(emptyList())
        } else {
            repository.search(translationId, query)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Bookmarks, Highlights, Notes, Reading Position
    val bookmarks: StateFlow<List<BibleBookmarkEntity>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val highlights: StateFlow<List<BibleHighlightEntity>> = repository.getAllHighlights()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val notes: StateFlow<List<BibleNoteEntity>> = repository.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val lastReadingPosition: StateFlow<ReadingPositionEntity?> = repository.getReadingPosition()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch {
            repository.initialize()
            _isOnline.value = repository.isOnline()
        }
    }

    fun selectTranslation(translation: BibleTranslation) {
        _selectedTranslation.value = translation
    }

    fun openBook(bookId: Int, chapter: Int = 1, targetVerse: Int? = null) {
        val book = BibleBookDefinitions.getBookById(bookId) ?: return
        _currentBook.value = book
        _currentChapter.value = chapter.coerceIn(1, book.chapterCount)
        _targetVerse.value = targetVerse

        if (_readingSettings.value.rememberLastReadingPosition) {
            viewModelScope.launch {
                repository.saveReadingPosition(
                    bookId = book.id,
                    chapter = _currentChapter.value,
                    verse = targetVerse ?: 1,
                    translationId = _selectedTranslation.value.id
                )
            }
        }
    }

    fun selectChapter(chapter: Int) {
        val book = _currentBook.value
        val validChapter = chapter.coerceIn(1, book.chapterCount)
        _currentChapter.value = validChapter
        _targetVerse.value = null

        if (_readingSettings.value.rememberLastReadingPosition) {
            viewModelScope.launch {
                repository.saveReadingPosition(
                    bookId = book.id,
                    chapter = validChapter,
                    verse = 1,
                    translationId = _selectedTranslation.value.id
                )
            }
        }
    }

    fun nextChapter() {
        val currentBookVal = _currentBook.value
        val currentChap = _currentChapter.value
        if (currentChap < currentBookVal.chapterCount) {
            selectChapter(currentChap + 1)
        } else {
            // Move to next book if available
            val nextBook = BibleBookDefinitions.getBookById(currentBookVal.id + 1)
            if (nextBook != null) {
                openBook(nextBook.id, 1)
            }
        }
    }

    fun previousChapter() {
        val currentBookVal = _currentBook.value
        val currentChap = _currentChapter.value
        if (currentChap > 1) {
            selectChapter(currentChap - 1)
        } else {
            // Move to previous book last chapter
            val prevBook = BibleBookDefinitions.getBookById(currentBookVal.id - 1)
            if (prevBook != null) {
                openBook(prevBook.id, prevBook.chapterCount)
            }
        }
    }

    fun refreshCurrentChapter() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshChapter(
                _selectedTranslation.value.id,
                _currentBook.value.id,
                _currentChapter.value
            )
            _isOnline.value = repository.isOnline()
            _isLoading.value = false
        }
    }

    fun toggleBookmark(verse: BibleVerse) {
        viewModelScope.launch {
            if (verse.isBookmarked) {
                repository.removeBookmark(verse.bookId, verse.chapter, verse.verseNumber)
            } else {
                repository.toggleBookmark(
                    verse.bookId,
                    verse.chapter,
                    verse.verseNumber,
                    _selectedTranslation.value.id,
                    verse.text
                )
            }
        }
    }

    fun removeBookmarkById(id: Long) {
        viewModelScope.launch {
            repository.removeBookmarkById(id)
        }
    }

    fun setHighlight(verse: BibleVerse, colorHex: String) {
        viewModelScope.launch {
            repository.setHighlight(verse.bookId, verse.chapter, verse.verseNumber, colorHex)
        }
    }

    fun removeHighlight(verse: BibleVerse) {
        viewModelScope.launch {
            repository.removeHighlight(verse.bookId, verse.chapter, verse.verseNumber)
        }
    }

    fun saveNote(verse: BibleVerse, noteText: String) {
        viewModelScope.launch {
            if (noteText.isBlank()) {
                repository.deleteNote(verse.bookId, verse.chapter, verse.verseNumber)
            } else {
                repository.saveNote(verse.bookId, verse.chapter, verse.verseNumber, noteText.trim())
            }
        }
    }

    fun deleteNote(bookId: Int, chapter: Int, verse: Int) {
        viewModelScope.launch {
            repository.deleteNote(bookId, chapter, verse)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFontSize(size: BibleFontSize) {
        _readingSettings.value = _readingSettings.value.copy(fontSize = size)
    }

    fun updateLineSpacing(spacing: BibleLineSpacing) {
        _readingSettings.value = _readingSettings.value.copy(lineSpacing = spacing)
    }

    fun toggleVerseNumbers(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showVerseNumbers = show)
    }

    fun updateTheme(theme: BibleTheme) {
        _readingSettings.value = _readingSettings.value.copy(theme = theme)
    }

    fun toggleRememberPosition(remember: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(rememberLastReadingPosition = remember)
    }

    fun clearTargetVerse() {
        _targetVerse.value = null
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = BibleDatabase.getInstance(application)
            val localDataSource = BibleLocalDataSource(application, database.bibleDao())
            val remoteDataSource = BibleRemoteDataSource()
            val repository = BibleRepository(application, localDataSource, remoteDataSource)
            return BibleViewModel(application, repository) as T
        }
    }
}
