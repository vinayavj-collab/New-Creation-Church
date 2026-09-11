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
        _currentChapter,
        _readingSettings
    ) { translation, book, chapter, settings ->
        val isDual = translation.id == BibleTranslation.PARALLEL_HI_EN.id || settings.isDualBibleEnabled
        val effTranslationId = if (isDual) BibleTranslation.PARALLEL_HI_EN.id else translation.id
        repository.getChapterVerses(
            translationId = effTranslationId,
            bookId = book.id,
            chapter = chapter,
            coroutineScope = viewModelScope,
            dualHindiId = settings.dualHindiVersionId,
            dualEnglishId = settings.dualEnglishVersionId
        )
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val chapterHeadings: StateFlow<List<BibleSectionHeading>> = combine(
        _selectedTranslation,
        _currentBook,
        _currentChapter
    ) { translation, book, chapter ->
        Triple(translation, book, chapter)
    }.flatMapLatest { (translation, book, chapter) ->
        repository.getChapterHeadings(translation.id, book.id, chapter)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val structuredBlocks: StateFlow<List<BibleContentBlock>> = combine(
        _selectedTranslation,
        _currentBook,
        _currentChapter,
        _readingSettings
    ) { translation, book, chapter, settings ->
        val isDual = translation.id == BibleTranslation.PARALLEL_HI_EN.id || settings.isDualBibleEnabled
        if (!isDual && translation.id.startsWith("HIN")) {
            repository.getStructuredChapter(book.id, chapter)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Verses partitioned into sections with their canonical section headings
    val chapterSections: StateFlow<List<ChapterSection>> = combine(
        verses,
        chapterHeadings
    ) { verseList, headingList ->
        if (verseList.isEmpty()) {
            emptyList()
        } else if (headingList.isEmpty()) {
            listOf(ChapterSection(heading = null, verses = verseList))
        } else {
            val sortedVerses = verseList.sortedBy { it.verseNumber }
            val sortedHeadings = headingList.sortedBy { it.beforeVerse }
            val result = mutableListOf<ChapterSection>()

            // Any verses before the first section heading
            val firstHeadingVerse = sortedHeadings.first().beforeVerse
            val preHeadingVerses = sortedVerses.filter { it.verseNumber < firstHeadingVerse }
            if (preHeadingVerses.isNotEmpty()) {
                result.add(ChapterSection(heading = null, verses = preHeadingVerses))
            }

            for (i in sortedHeadings.indices) {
                val currentHeading = sortedHeadings[i]
                val nextHeadingVerse = if (i + 1 < sortedHeadings.size) sortedHeadings[i + 1].beforeVerse else Int.MAX_VALUE
                val sectionVerses = sortedVerses.filter { it.verseNumber >= currentHeading.beforeVerse && it.verseNumber < nextHeadingVerse }
                result.add(ChapterSection(heading = currentHeading, verses = sectionVerses))
            }
            result
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Validation state: true if chapter has gaps or missing verses
    val isChapterIncomplete: StateFlow<Boolean> = verses.map { list ->
        if (list.isEmpty()) false
        else {
            val sorted = list.sortedBy { it.verseNumber }
            if (sorted.first().verseNumber != 1) true
            else {
                var gap = false
                for (i in 0 until sorted.size - 1) {
                    if (sorted[i + 1].verseNumber != sorted[i].verseNumber + 1) {
                        gap = true
                        break
                    }
                }
                gap
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

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

    // Bookmarks, Favorites, Highlights, Notes, Reading Position
    val bookmarks: StateFlow<List<BibleBookmarkEntity>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favorites: StateFlow<List<com.example.data.bible.local.BibleFavoriteVerseEntity>> = repository.getAllFavorites()
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

    fun toggleFavorite(verse: BibleVerse) {
        viewModelScope.launch {
            if (verse.isFavorite) {
                repository.removeFavorite(verse.bookId, verse.chapter, verse.verseNumber)
            } else {
                repository.toggleFavorite(
                    verse.bookId,
                    verse.chapter,
                    verse.verseNumber,
                    _selectedTranslation.value.id,
                    verse.text
                )
            }
        }
    }

    fun removeFavoriteById(id: Long) {
        viewModelScope.launch {
            repository.removeFavoriteById(id)
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

    fun toggleSubheadings(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showSubheadings = show)
    }

    fun toggleFavoritesHint(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showFavoritesHint = show)
    }

    fun toggleBookmarkHint(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showBookmarkHint = show)
    }

    fun toggleHighlights(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showHighlights = show)
    }

    fun toggleParagraphAndIndents(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showParagraphAndIndents = show)
    }

    fun toggleJesusWordsInRed(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showJesusWordsInRed = show)
    }

    fun updateJesusWordsColor(hex: String) {
        _readingSettings.value = _readingSettings.value.copy(jesusWordsColorHex = hex)
    }

    fun toggleJustifyBibleText(justify: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(justifyBibleText = justify)
    }

    fun toggleSuggestVerseSelection(suggest: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(suggestVerseSelection = suggest)
    }

    fun toggleNoteHint(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showNoteHint = show)
    }

    fun toggleAudioPlayer(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showAudioPlayer = show)
    }

    fun toggleNoteFormatHtml(isHtml: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(noteFormatHtml = isHtml)
    }

    fun setScreenTimeout(minutes: Int) {
        _readingSettings.value = _readingSettings.value.copy(screenTimeoutMinutes = minutes)
    }

    fun updateTheme(theme: BibleTheme) {
        _readingSettings.value = _readingSettings.value.copy(theme = theme)
    }

    fun updateFontStyle(fontStyle: BibleFontFamilyType) {
        _readingSettings.value = _readingSettings.value.copy(
            fontStyle = fontStyle,
            useSerifFont = fontStyle == BibleFontFamilyType.CLASSIC_SERIF || fontStyle == BibleFontFamilyType.TRADITIONAL_BOOK
        )
    }

    fun updateCustomTextColor(hex: String?) {
        _readingSettings.value = _readingSettings.value.copy(customTextColorHex = hex)
    }

    fun updateCustomHeadingColor(hex: String?) {
        _readingSettings.value = _readingSettings.value.copy(customHeadingColorHex = hex)
    }

    fun updateCustomSubHeadingColor(hex: String?) {
        _readingSettings.value = _readingSettings.value.copy(customSubHeadingColorHex = hex)
    }

    fun updateTranslationToggleBehavior(behavior: TranslationToggleBehavior) {
        _readingSettings.value = _readingSettings.value.copy(translationToggleBehavior = behavior)
    }

    fun configureDualBible(
        enabled: Boolean,
        hindiVersionId: String = _readingSettings.value.dualHindiVersionId,
        englishVersionId: String = _readingSettings.value.dualEnglishVersionId,
        viewMode: DualViewMode = _readingSettings.value.dualViewMode
    ) {
        _readingSettings.value = _readingSettings.value.copy(
            isDualBibleEnabled = enabled,
            dualHindiVersionId = hindiVersionId,
            dualEnglishVersionId = englishVersionId,
            dualViewMode = viewMode
        )
        if (enabled) {
            _selectedTranslation.value = BibleTranslation.PARALLEL_HI_EN
        }
    }

    fun toggleSerifFont(useSerif: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(
            useSerifFont = useSerif,
            fontStyle = if (useSerif) BibleFontFamilyType.CLASSIC_SERIF else BibleFontFamilyType.SYSTEM_DEFAULT
        )
    }

    fun toggleOriginalFormatMode(enabled: Boolean) {
        if (enabled) {
            _readingSettings.value = _readingSettings.value.copy(
                originalFormatMode = true,
                useSerifFont = true,
                justifyBibleText = true,
                showParagraphAndIndents = true
            )
        } else {
            _readingSettings.value = _readingSettings.value.copy(
                originalFormatMode = false
            )
        }
    }

    fun toggleRememberPosition(remember: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(rememberLastReadingPosition = remember)
    }

    val readingPlanRepository = com.example.data.bible.repository.ReadingPlanRepository(
        com.example.data.bible.local.BibleDatabase.getInstance(application).bibleDao()
    )

    fun toggleShowTodaysScriptureOnHome(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showTodaysScriptureOnHome = show)
    }

    fun toggleShowActivatedPlansOnHome(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showActivatedPlansOnHome = show)
    }

    fun updateVerseTapSelectionMode(mode: VerseTapSelectionMode) {
        _readingSettings.value = _readingSettings.value.copy(verseTapSelectionMode = mode)
    }

    fun activateReadingPlan(planId: String) {
        val currentSet = _readingSettings.value.activatedPlanIds
        _readingSettings.value = _readingSettings.value.copy(activatedPlanIds = currentSet + planId)
    }

    fun deactivateReadingPlan(planId: String) {
        val currentSet = _readingSettings.value.activatedPlanIds
        _readingSettings.value = _readingSettings.value.copy(activatedPlanIds = currentSet - planId)
    }

    fun resetReadingPlanProgress(planId: String) {
        viewModelScope.launch {
            readingPlanRepository.resetPlan(planId)
        }
    }

    fun addManualReadingPlan(
        titleHindi: String,
        titleEnglish: String,
        totalDays: Int,
        descriptionHindi: String = "",
        descriptionEnglish: String = ""
    ) {
        val newPlan = com.example.data.bible.model.ManualPlanData(
            id = "manual_" + System.currentTimeMillis(),
            titleHindi = if (titleHindi.isBlank()) "कस्टम प्लान" else titleHindi,
            titleEnglish = if (titleEnglish.isBlank()) "Custom Plan" else titleEnglish,
            descriptionHindi = descriptionHindi,
            descriptionEnglish = descriptionEnglish,
            totalDays = if (totalDays <= 0) 30 else totalDays
        )
        val currentList = com.example.data.bible.model.ManualPlanData.deserializeList(_readingSettings.value.manualPlansJson)
        val updatedList = currentList + newPlan
        val newJson = com.example.data.bible.model.ManualPlanData.serializeList(updatedList)
        val currentActivated = _readingSettings.value.activatedPlanIds
        _readingSettings.value = _readingSettings.value.copy(
            manualPlansJson = newJson,
            activatedPlanIds = currentActivated + newPlan.id
        )
    }

    fun deleteManualReadingPlan(planId: String) {
        val currentList = com.example.data.bible.model.ManualPlanData.deserializeList(_readingSettings.value.manualPlansJson)
        val updatedList = currentList.filter { it.id != planId }
        val newJson = com.example.data.bible.model.ManualPlanData.serializeList(updatedList)
        val currentActivated = _readingSettings.value.activatedPlanIds
        _readingSettings.value = _readingSettings.value.copy(
            manualPlansJson = newJson,
            activatedPlanIds = currentActivated - planId
        )
        resetReadingPlanProgress(planId)
    }

    fun getAllPlansList(): List<com.example.data.bible.model.ReadingPlanInfo> {
        val predefined = readingPlanRepository.getAllPlans()
        val manual = com.example.data.bible.model.ManualPlanData.deserializeList(_readingSettings.value.manualPlansJson).map { it.toReadingPlanInfo() }
        return predefined + manual
    }

    fun resetReadingSettings() {
        _readingSettings.value = BibleReadingSettings()
    }

    fun clearTargetVerse() {
        _targetVerse.value = null
    }

    val audioManager = com.example.util.BibleAudioManager(application)

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
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
