package com.example.data.bible.model

data class BibleVerse(
    val bookId: Int,
    val chapter: Int,
    val verseNumber: Int,
    val text: String,
    val secondaryText: String? = null,
    val bookName: String = "",
    val translationId: String = "HIOV",
    val isBookmarked: Boolean = false,
    val isFavorite: Boolean = false,
    val highlightColor: String? = null,
    val note: String? = null,
    val commentaryText: String? = null
)
