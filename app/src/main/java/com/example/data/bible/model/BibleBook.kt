package com.example.data.bible.model

data class BibleBook(
    val id: Int,
    val nameHindi: String,
    val nameEnglish: String,
    val testament: String,
    val totalChapters: Int
)

object BibleBookDefinitions {
    val BOOKS = listOf(
        BibleBook(1, "उत्पत्ति", "Genesis", "OLD", 50),
        BibleBook(2, "निर्गमन", "Exodus", "OLD", 40),
        BibleBook(3, "लैव्यव्यवस्था", "Leviticus", "OLD", 27),
        BibleBook(4, "गिनती", "Numbers", "OLD", 36),
        BibleBook(5, "व्यवस्थाविवरण", "Deuteronomy", "OLD", 34),
        BibleBook(40, "मत्ती", "Matthew", "NEW", 28),
        BibleBook(41, "मार्क", "Mark", "NEW", 16),
        BibleBook(42, "लूका", "Luke", "NEW", 24),
        BibleBook(43, "यूहन्ना", "John", "NEW", 21),
        BibleBook(44, "प्रेरितों के काम", "Acts", "NEW", 28)
    )

    fun getBookById(id: Int): BibleBook? = BOOKS.find { it.id == id }
}
