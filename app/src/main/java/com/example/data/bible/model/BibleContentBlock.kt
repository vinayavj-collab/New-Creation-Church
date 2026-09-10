package com.example.data.bible.model

data class FootnoteItem(
    val ref: String = "",
    val target: String = "",
    val text: String = ""
)

data class VerseItem(
    val verseNumber: Int? = null,
    val text: String,
    val footnotes: List<FootnoteItem> = emptyList()
)

data class PoetryLineItem(
    val indent: Int = 0,
    val verseNumber: Int? = null,
    val text: String,
    val footnotes: List<FootnoteItem> = emptyList()
)

sealed class BibleContentBlock {
    data class SectionHeading(
        val text: String,
        val beforeVerse: Int
    ) : BibleContentBlock()

    data class Title(
        val text: String,
        val beforeVerse: Int
    ) : BibleContentBlock()

    data class ProseParagraph(
        val verses: List<VerseItem>
    ) : BibleContentBlock()

    data class PoetryBlock(
        val lines: List<PoetryLineItem>
    ) : BibleContentBlock()
}
