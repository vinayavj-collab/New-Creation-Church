package com.example.ui.bible

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.bible.local.*
import com.example.data.bible.model.*
import com.example.data.bible.repository.BibleRepository
import com.example.data.bible.repository.ReadingPlanRepository
import com.example.util.BibleAudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BibleViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BibleDatabase.getDatabase(application)
    private val dao = database.bibleDao()
    private val localDataSource = BibleLocalDataSource(application, dao)
    private val repository = BibleRepository(application, localDataSource)
    val readingPlanRepository = ReadingPlanRepository(dao)
    val audioManager: BibleAudioManager = BibleAudioManager.getInstance(application)

    var currentBookId by mutableStateOf(40) // Default Matthew
        private set
    var targetVerseNumber by mutableStateOf<Int?>(null)
        private set
    var currentTranslationId by mutableStateOf("HIOV")
        private set
    var dualHindiId by mutableStateOf("HIOV")
        private set
    var dualEnglishId by mutableStateOf("NKJV")
        private set

    private val _currentBook = MutableStateFlow(BibleBookDefinitions.getBookById(40) ?: BibleBookDefinitions.BOOKS.first())
    val currentBook: StateFlow<BibleBook> = _currentBook.asStateFlow()

    private val _currentChapter = MutableStateFlow(1)
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()
    val currentChapterFlow: StateFlow<Int> = _currentChapter.asStateFlow()
    val currentChapterState: StateFlow<Int> = _currentChapter.asStateFlow()

    private val _selectedTranslation = MutableStateFlow(BibleTranslation.HIOV)
    val selectedTranslation: StateFlow<BibleTranslation> = _selectedTranslation.asStateFlow()

    private val _readingSettings = MutableStateFlow(BibleReadingSettings())
    val readingSettings: StateFlow<BibleReadingSettings> = _readingSettings.asStateFlow()

    private val _lastReadingPosition = MutableStateFlow<ReadingPositionEntity?>(null)
    val lastReadingPosition: StateFlow<ReadingPositionEntity?> = _lastReadingPosition.asStateFlow()

    var todayVerse by mutableStateOf<VerseOfTheDay?>(VerseOfTheDay.getTodayVerse())
        private set

    private val _versesState = MutableStateFlow<List<BibleVerse>>(emptyList())
    val versesState: StateFlow<List<BibleVerse>> = _versesState.asStateFlow()
    val verses: StateFlow<List<BibleVerse>> = _versesState

    private val _chapterSections = MutableStateFlow<List<ChapterSection>>(emptyList())
    val chapterSections: StateFlow<List<ChapterSection>> = _chapterSections.asStateFlow()

    private val _structuredBlocks = MutableStateFlow<List<BibleContentBlock>>(emptyList())
    val structuredBlocks: StateFlow<List<BibleContentBlock>> = _structuredBlocks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isChapterIncomplete = MutableStateFlow(false)
    val isChapterIncomplete: StateFlow<Boolean> = _isChapterIncomplete.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<BibleVerse>>(emptyList())
    val searchResults: StateFlow<List<BibleVerse>> = _searchResults.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BibleBookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BibleBookmarkEntity>> = _bookmarks.asStateFlow()

    private val _favorites = MutableStateFlow<List<BibleFavoriteVerseEntity>>(emptyList())
    val favorites: StateFlow<List<BibleFavoriteVerseEntity>> = _favorites.asStateFlow()

    private val _highlights = MutableStateFlow<List<BibleHighlightEntity>>(emptyList())
    val highlights: StateFlow<List<BibleHighlightEntity>> = _highlights.asStateFlow()

    private val _notes = MutableStateFlow<List<BibleNoteEntity>>(emptyList())
    val notes: StateFlow<List<BibleNoteEntity>> = _notes.asStateFlow()

    private val _planHighlightStyle = MutableStateFlow(ReadingPlanHighlightStyle())
    val planHighlightStyle: StateFlow<ReadingPlanHighlightStyle> = _planHighlightStyle.asStateFlow()

    init {
        viewModelScope.launch {
            localDataSource.ensureSeeded()
            loadVerses()
        }
        viewModelScope.launch {
            dao.getAllBookmarks().collect { _bookmarks.value = it }
        }
        viewModelScope.launch {
            dao.getAllFavorites().collect { _favorites.value = it }
        }
        viewModelScope.launch {
            dao.getAllHighlights().collect { _highlights.value = it }
        }
        viewModelScope.launch {
            dao.getAllNotes().collect { _notes.value = it }
        }
        viewModelScope.launch {
            dao.getReadingPosition().collect { _lastReadingPosition.value = it }
        }
    }

    fun getAllPlansList(): List<ReadingPlanInfo> = PredefinedReadingPlans.allPlans

    fun activateReadingPlan(planId: String) {
        val currentSet = _readingSettings.value.activatedPlanIds.toMutableSet()
        currentSet.add(planId)
        _readingSettings.value = _readingSettings.value.copy(activatedPlanIds = currentSet)
    }

    fun deactivateReadingPlan(planId: String) {
        val currentSet = _readingSettings.value.activatedPlanIds.toMutableSet()
        currentSet.remove(planId)
        _readingSettings.value = _readingSettings.value.copy(activatedPlanIds = currentSet)
    }

    fun resetReadingPlanProgress(planId: String) {
        viewModelScope.launch {
            readingPlanRepository.resetPlan(planId)
        }
    }

    fun deleteManualReadingPlan(planId: String) {
        val plans = ManualPlanData.deserializeList(_readingSettings.value.manualPlansJson).toMutableList()
        plans.removeAll { it.id == planId }
        val activeSet = _readingSettings.value.activatedPlanIds.toMutableSet()
        activeSet.remove(planId)
        _readingSettings.value = _readingSettings.value.copy(
            manualPlansJson = ManualPlanData.serializeList(plans),
            activatedPlanIds = activeSet
        )
    }

    fun addManualReadingPlan(
        titleHindi: String,
        titleEnglish: String,
        totalDays: Int,
        descriptionHindi: String = "",
        descriptionEnglish: String = ""
    ) {
        val id = "manual_${System.currentTimeMillis()}"
        val plan = ManualPlanData(
            id = id,
            titleHindi = titleHindi,
            titleEnglish = titleEnglish,
            totalDays = totalDays,
            descriptionHindi = descriptionHindi,
            descriptionEnglish = descriptionEnglish
        )
        val plans = ManualPlanData.deserializeList(_readingSettings.value.manualPlansJson).toMutableList()
        plans.add(plan)
        val activeSet = _readingSettings.value.activatedPlanIds.toMutableSet()
        activeSet.add(id)
        _readingSettings.value = _readingSettings.value.copy(
            manualPlansJson = ManualPlanData.serializeList(plans),
            activatedPlanIds = activeSet
        )
    }

    fun addManualReadingPlan(plan: ManualPlanData) {
        val plans = ManualPlanData.deserializeList(_readingSettings.value.manualPlansJson).toMutableList()
        plans.add(plan)
        val activeSet = _readingSettings.value.activatedPlanIds.toMutableSet()
        activeSet.add(plan.id)
        _readingSettings.value = _readingSettings.value.copy(
            manualPlansJson = ManualPlanData.serializeList(plans),
            activatedPlanIds = activeSet
        )
    }

    fun updatePlanHighlightStyle(style: ReadingPlanHighlightStyle) {
        _planHighlightStyle.value = style
    }

    fun removeBookmarkById(id: Long) {
        viewModelScope.launch {
            localDataSource.removeBookmarkById(id)
        }
    }

    fun removeFavoriteById(id: Long) {
        viewModelScope.launch {
            localDataSource.removeFavoriteById(id)
        }
    }

    fun deleteNote(bookId: Int, chapter: Int, verse: Int) {
        viewModelScope.launch {
            localDataSource.deleteNote(bookId, chapter, verse)
            loadVerses()
        }
    }

    fun openBook(bookId: Int, chapter: Int, targetVerse: Int? = null, isReadingPlan: Boolean = false) {
        currentBookId = bookId
        _currentChapter.value = chapter
        targetVerseNumber = targetVerse
        val book = BibleBookDefinitions.getBookById(bookId) ?: BibleBookDefinitions.BOOKS.first()
        _currentBook.value = book
        loadVerses()
        if (!isReadingPlan) {
            viewModelScope.launch {
                dao.saveReadingPosition(
                    ReadingPositionEntity(
                        bookId = bookId,
                        bookName = book.nameHindi,
                        chapter = chapter,
                        verse = targetVerse ?: 1,
                        translationId = currentTranslationId
                    )
                )
            }
        }
    }

    fun clearPlanHighlight() {
        _planHighlightStyle.value = _planHighlightStyle.value.copy(isVisible = false)
    }

    fun nextChapter() {
        val nextChap = _currentChapter.value + 1
        if (nextChap <= _currentBook.value.chapterCount) {
            openBook(_currentBook.value.id, nextChap)
        } else {
            val nextBook = BibleBookDefinitions.getBookById(_currentBook.value.id + 1)
            if (nextBook != null) {
                openBook(nextBook.id, 1)
            }
        }
    }

    fun previousChapter() {
        val prevChap = _currentChapter.value - 1
        if (prevChap >= 1) {
            openBook(_currentBook.value.id, prevChap)
        } else {
            val prevBook = BibleBookDefinitions.getBookById(_currentBook.value.id - 1)
            if (prevBook != null) {
                openBook(prevBook.id, prevBook.chapterCount)
            }
        }
    }

    fun refreshCurrentChapter() {
        loadVerses()
    }

    fun toggleChapterOutline(show: Boolean? = null) {
        val newShow = show ?: !_readingSettings.value.showChapterOutline
        _readingSettings.value = _readingSettings.value.copy(showChapterOutline = newShow)
    }

    fun updateFontStyle(font: BibleFontFamilyType) {
        _readingSettings.value = _readingSettings.value.copy(fontStyle = font)
    }

    fun updateVerseNumberSize(size: VerseNumberSize) {
        _readingSettings.value = _readingSettings.value.copy(verseNumberSize = size)
    }

    fun toggleSubheadings(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showSubheadings = show)
    }

    fun toggleHeadingVerseRanges(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showHeadingVerseRanges = show)
    }

    fun toggleParagraphAndIndents(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showParagraphAndIndents = show)
    }

    fun toggleOriginalFormatMode(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(originalFormatMode = show)
    }

    fun toggleJesusWordsInRed(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showJesusWordsInRed = show)
    }

    fun updateJesusWordsColor(colorHex: String) {
        _readingSettings.value = _readingSettings.value.copy(jesusWordsColorHex = colorHex)
    }

    fun updateCustomTextColor(colorHex: String?) {
        _readingSettings.value = _readingSettings.value.copy(customTextColorHex = colorHex)
    }

    fun updateCustomHeadingColor(colorHex: String?) {
        _readingSettings.value = _readingSettings.value.copy(customHeadingColorHex = colorHex)
    }

    fun updateCustomSubHeadingColor(colorHex: String?) {
        _readingSettings.value = _readingSettings.value.copy(customSubHeadingColorHex = colorHex)
    }

    fun configureDualBible(enabled: Boolean, dualHindi: String, dualEnglish: String, mode: DualViewMode = DualViewMode.INTERLEAVED) {
        dualHindiId = dualHindi
        dualEnglishId = dualEnglish
        _readingSettings.value = _readingSettings.value.copy(
            isDualBibleEnabled = enabled,
            dualHindiVersionId = dualHindi,
            dualEnglishVersionId = dualEnglish,
            dualViewMode = mode
        )
        loadVerses()
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

    fun toggleNoteHint(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showNoteHint = show)
    }

    fun toggleJustifyBibleText(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(justifyBibleText = show)
    }

    fun toggleSuggestVerseSelection(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(suggestVerseSelection = show)
    }

    fun updateCustomBgmFile(uri: String, fileName: String = "") {
        _readingSettings.value = _readingSettings.value.copy(customBgmUri = uri, customBgmFileName = fileName)
    }

    fun updateCustomBgmUrl(url: String) {
        _readingSettings.value = _readingSettings.value.copy(customBgmUri = url)
    }

    fun setScreenTimeout(timeoutMinutes: Int) {
        _readingSettings.value = _readingSettings.value.copy(screenTimeoutMinutes = timeoutMinutes)
    }

    fun toggleRememberPosition(remember: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(rememberLastReadingPosition = remember)
    }

    fun resetReadingSettings() {
        _readingSettings.value = BibleReadingSettings()
    }

    fun updateTranslationToggleBehavior(behavior: TranslationToggleBehavior) {
        _readingSettings.value = _readingSettings.value.copy(translationToggleBehavior = behavior)
    }

    fun navigateToChapter(bookId: Int, chapter: Int, targetVerse: Int? = null) {
        openBook(bookId, chapter, targetVerse, isReadingPlan = false)
    }

    fun selectTranslation(translation: BibleTranslation) {
        _selectedTranslation.value = translation
        currentTranslationId = translation.id
        loadVerses()
    }

    fun setTranslation(translationId: String) {
        currentTranslationId = translationId
        val found = BibleTranslation.ALL.find { it.id == translationId } ?: BibleTranslation.HIOV
        _selectedTranslation.value = found
        loadVerses()
    }

    fun loadVerses() {
        viewModelScope.launch {
            repository.getChapterVerses(
                translationId = currentTranslationId,
                bookId = currentBookId,
                chapter = _currentChapter.value,
                coroutineScope = viewModelScope,
                dualHindiId = dualHindiId,
                dualEnglishId = dualEnglishId
            ).collect { list ->
                _versesState.value = list
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            localDataSource.searchVerses(currentTranslationId, query).collect {
                _searchResults.value = it
            }
        }
    }

    fun toggleBookmark(verse: BibleVerse) {
        viewModelScope.launch {
            repository.toggleBookmark(verse.bookId, verse.chapter, verse.verseNumber, !verse.isBookmarked)
            loadVerses()
        }
    }

    fun toggleFavorite(verse: BibleVerse) {
        viewModelScope.launch {
            repository.toggleFavorite(verse.bookId, verse.chapter, verse.verseNumber, !verse.isFavorite)
            loadVerses()
        }
    }

    fun saveNote(verse: BibleVerse, noteText: String) {
        viewModelScope.launch {
            repository.saveNote(verse.bookId, verse.chapter, verse.verseNumber, noteText)
            loadVerses()
        }
    }

    fun setHighlight(verse: BibleVerse, colorHex: String) {
        viewModelScope.launch {
            localDataSource.setHighlight(verse.bookId, verse.chapter, verse.verseNumber, colorHex)
            loadVerses()
        }
    }

    fun removeHighlight(verse: BibleVerse) {
        viewModelScope.launch {
            localDataSource.removeHighlight(verse.bookId, verse.chapter, verse.verseNumber)
            loadVerses()
        }
    }

    // Settings Updates
    fun updateFontSize(size: BibleFontSize) {
        _readingSettings.value = _readingSettings.value.copy(fontSize = size)
    }

    fun updateLineSpacing(spacing: BibleLineSpacing) {
        _readingSettings.value = _readingSettings.value.copy(lineSpacing = spacing)
    }

    fun toggleVerseNumbers(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showVerseNumbers = show)
    }

    fun toggleAudioPlayer(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showAudioPlayer = show)
    }

    fun toggleTtsSmartStart(enabled: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(ttsSmartStartActiveVerse = enabled)
    }

    fun toggleTtsBackgroundPlay(enabled: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(ttsBackgroundPlay = enabled)
    }

    fun updateTtsSleepTimerMinutes(minutes: Int) {
        _readingSettings.value = _readingSettings.value.copy(ttsSleepTimerMinutes = minutes)
    }

    fun updateTtsSleepChapterCount(count: Int) {
        _readingSettings.value = _readingSettings.value.copy(ttsSleepChapterCount = count)
    }

    fun toggleDevotionalBgm(enabled: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(enableDevotionalBgm = enabled)
    }

    fun updateTtsVolume(volume: Float) {
        _readingSettings.value = _readingSettings.value.copy(ttsVolume = volume)
    }

    fun updateBgmVolume(volume: Float) {
        _readingSettings.value = _readingSettings.value.copy(bgmVolume = volume)
    }

    fun updateSelectedBgmTrack(trackId: String) {
        _readingSettings.value = _readingSettings.value.copy(selectedBgmTrackId = trackId)
    }

    fun updateTheme(theme: BibleTheme) {
        _readingSettings.value = _readingSettings.value.copy(theme = theme)
    }

    fun updateVerseTapSelectionMode(mode: VerseTapSelectionMode) {
        _readingSettings.value = _readingSettings.value.copy(verseTapSelectionMode = mode)
    }

    fun toggleShowTodaysScriptureOnHome(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showTodaysScriptureOnHome = show)
    }

    fun toggleShowActivatedPlansOnHome(show: Boolean) {
        _readingSettings.value = _readingSettings.value.copy(showActivatedPlansOnHome = show)
    }

    fun setExternalFolderPath(path: String) {
        _readingSettings.value = _readingSettings.value.copy(externalFolderPath = path)
    }

    fun getAvailableTranslationIds(): List<String> = repository.getAvailableTranslationIds()

    suspend fun getVersesForCompare(translationId: String, bookId: Int, chapter: Int, verse: Int): List<BibleVerse> =
        repository.getVersesForCompare(translationId, bookId, chapter, verse)

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BibleViewModel(app) as T
        }
    }
}
