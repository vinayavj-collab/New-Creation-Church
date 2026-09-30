package com.example.ui.bible

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.bible.model.BibleVerse
import com.example.data.bible.repository.BibleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BibleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BibleRepository(application)

    var currentBookId by mutableStateOf(40) // Default Matthew
        private set
    var currentChapter by mutableStateOf(1)
        private set
    var currentTranslationId by mutableStateOf("HIOV")
        private set
    var dualHindiId by mutableStateOf("HIOV")
        private set
    var dualEnglishId by mutableStateOf("NKJV")
        private set

    val verses: StateFlow<List<BibleVerse>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList()) // simplified or combined flow

    fun navigateToChapter(bookId: Int, chapter: Int) {
        currentBookId = bookId
        currentChapter = chapter
        loadVerses()
    }

    fun setTranslation(translationId: String) {
        currentTranslationId = translationId
        loadVerses()
    }

    private val _versesState = kotlinx.coroutines.flow.MutableStateFlow<List<BibleVerse>>(emptyList())
    val versesState: StateFlow<List<BibleVerse>> = _versesState

    init {
        loadVerses()
    }

    fun loadVerses() {
        viewModelScope.launch {
            repository.getChapterVerses(
                translationId = currentTranslationId,
                bookId = currentBookId,
                chapter = currentChapter,
                coroutineScope = viewModelScope,
                dualHindiId = dualHindiId,
                dualEnglishId = dualEnglishId
            ).collect { list ->
                _versesState.value = list
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
}
