package com.example.ui.bible

import com.example.data.bible.model.BibleBook

/**
 * UI Presentation layer utility for Bible header displays.
 * Dynamically abbreviates long Bible book names strictly for header/toolbar display
 * to prevent horizontal truncation of chapter/verse numbers without altering
 * core database book names.
 */
object BibleHeaderFormatter {

    // Dynamic UI Presentation Mapping for long Hindi book names in headers/toolbars
    private val hindiHeaderAbbreviations: Map<String, String> = mapOf(
        "प्रेरितों के काम" to "प्रेरित",
        "भजन संहिता" to "भजन",
        "व्यवस्थाविवरण" to "व्यवस्था",
        "लैव्यव्यवस्था" to "लैव्य",
        "प्रकाशितवाक्य" to "प्रका.",
        "1 थिस्सलुनीकियों" to "1 थिस्स.",
        "2 थिस्सलुनीकियों" to "2 थिस्स.",
        "1 कुरिन्थियों" to "1 कुरि.",
        "2 कुरिन्थियों" to "2 कुरि.",
        "1 तीमुथियुस" to "1 तीमु.",
        "2 तीमुथियुस" to "2 तीमु.",
        "फिलिप्पियों" to "फिलि.",
        "कुलुस्सियों" to "कुलु.",
        "गलतियों" to "गलति.",
        "इफिसियों" to "इफि.",
        "इब्रानियों" to "इब्रा.",
        "सभोपदेशक" to "सभो.",
        "नीतिवचन" to "नीति",
        "श्रेष्ठगीत" to "श्रेष्ठ.",
        "विलापगीत" to "विलाप",
        "नहेमायाह" to "नहे.",
        "सपन्याह" to "सपन.",
        "हबक्कूक" to "हब.",
        "जकर्याह" to "जकर्.",
        "मलाकी" to "मला.",
        "1 इतिहास" to "1 इति.",
        "2 इतिहास" to "2 इति.",
        "1 शमूएल" to "1 शमू.",
        "2 शमूएल" to "2 शमू.",
        "1 राजा" to "1 राजा",
        "2 राजा" to "2 राजा",
        "1 पतरस" to "1 पत.",
        "2 पतरस" to "2 पत.",
        "1 यूहन्ना" to "1 यूह.",
        "2 यूहन्ना" to "2 यूह.",
        "3 यूहन्ना" to "3 यूह.",
        "न्यायियों" to "न्यायी"
    )

    private val englishHeaderAbbreviations: Map<String, String> = mapOf(
        "1 Thessalonians" to "1 Thess",
        "2 Thessalonians" to "2 Thess",
        "1 Corinthians" to "1 Cor",
        "2 Corinthians" to "2 Cor",
        "1 Chronicles" to "1 Chr",
        "2 Chronicles" to "2 Chr",
        "1 Timothy" to "1 Tim",
        "2 Timothy" to "2 Tim",
        "Deuteronomy" to "Deut",
        "Ecclesiastes" to "Eccl",
        "Lamentations" to "Lam",
        "Revelation" to "Rev",
        "Philippians" to "Phil",
        "Colossians" to "Col",
        "Galatians" to "Gal",
        "Ephesians" to "Eph",
        "Song of Solomon" to "Song",
        "1 Samuel" to "1 Sam",
        "2 Samuel" to "2 Sam",
        "1 Kings" to "1 Kings",
        "2 Kings" to "2 Kings",
        "1 Peter" to "1 Pet",
        "2 Peter" to "2 Pet",
        "1 John" to "1 John",
        "2 John" to "2 John",
        "3 John" to "3 John"
    )

    /**
     * Returns an abbreviated short book name for toolbar header presentation.
     */
    fun getShortBookName(fullName: String, isHindi: Boolean = true): String {
        return if (isHindi) {
            hindiHeaderAbbreviations[fullName] ?: fullName
        } else {
            englishHeaderAbbreviations[fullName] ?: fullName
        }
    }

    /**
     * Returns an abbreviated short book name from a [BibleBook] object.
     */
    fun getShortBookName(book: BibleBook, isHindi: Boolean = true): String {
        val fullName = if (isHindi) book.nameHindi else book.nameEnglish
        return getShortBookName(fullName, isHindi)
    }

    /**
     * Formats toolbar header strictly as "{ShortBookName} {Chapter}:{VerseRange}" (e.g. "प्रेरित 21:1-5").
     */
    fun formatHeaderTitle(
        book: BibleBook,
        chapter: Int,
        selectedVerses: Set<Int>,
        visibleVerse: Int?,
        isHindi: Boolean = true
    ): String {
        val shortName = getShortBookName(book, isHindi)
        return when {
            selectedVerses.size > 1 -> {
                val sorted = selectedVerses.sorted()
                val minV = sorted.first()
                val maxV = sorted.last()
                val isConsecutive = (maxV - minV == sorted.size - 1)
                val rangeStr = if (isConsecutive) "$minV-$maxV" else sorted.joinToString(",")
                "$shortName $chapter:$rangeStr"
            }
            selectedVerses.size == 1 -> {
                "$shortName $chapter:${selectedVerses.first()}"
            }
            else -> {
                val v = visibleVerse ?: 1
                "$shortName $chapter:$v"
            }
        }
    }
}
