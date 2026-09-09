package com.example.data.bible.model

data class BibleSectionHeading(
    val translationId: String,
    val bookId: Int,
    val chapter: Int,
    val beforeVerse: Int,
    val headingText: String
)

data class ChapterSection(
    val heading: BibleSectionHeading?,
    val verses: List<BibleVerse>
)
