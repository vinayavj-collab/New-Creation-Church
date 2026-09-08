package com.example.data.bible.model

data class BibleVerse(
    val bookId: Int,
    val bookName: String,
    val chapter: Int,
    val verseNumber: Int,
    val text: String,
    val translationId: String,
    val isBookmarked: Boolean = false,
    val highlightColor: String? = null,
    val note: String? = null
)
