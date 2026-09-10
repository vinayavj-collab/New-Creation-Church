package com.example.util

import com.example.data.bible.model.BibleBook
import com.example.data.bible.model.BibleBookDefinitions
import java.util.regex.Pattern

data class DetectedVerseRef(
    val rawText: String,
    val book: BibleBook,
    val bookId: Int,
    val chapter: Int,
    val startVerse: Int,
    val endVerse: Int,
    val displayLabel: String,
    val startIndex: Int = -1,
    val endIndex: Int = -1
)

object VerseReferenceDetector {

    // Map all aliases (Hindi, English, abbreviations, lowercase) to book IDs
    private val bookAliasMap: Map<String, BibleBook> by lazy {
        val map = mutableMapOf<String, BibleBook>()

        BibleBookDefinitions.books.forEach { book ->
            // Hindi full and short
            map[book.nameHindi.lowercase().trim()] = book
            map[book.abbreviationHindi.lowercase().trim()] = book

            // English full and short
            map[book.nameEnglish.lowercase().trim()] = book
            map[book.abbreviationEnglish.lowercase().trim()] = book

            // Special common variations
            when (book.id) {
                1 -> listOf("gen", "genesis", "उत्प", "उत्पत्ति").forEach { map[it] = book }
                2 -> listOf("exo", "exodus", "निर्ग", "निर्गमन").forEach { map[it] = book }
                19 -> listOf("psalm", "psalms", "psa", "ps", "भजन", "भजन संहिता", "भजनसंहिता").forEach { map[it] = book }
                20 -> listOf("prov", "proverbs", "नीति", "नीतिवचन").forEach { map[it] = book }
                22 -> listOf("song", "songs", "song of solomon", "श्रेष्ठ", "श्रेष्ठगीत").forEach { map[it] = book }
                23 -> listOf("isa", "isaiah", "यशा", "यशायाह").forEach { map[it] = book }
                40 -> listOf("matt", "mat", "matthew", "मत्ती").forEach { map[it] = book }
                41 -> listOf("mark", "mrk", "मरकुस", "मर").forEach { map[it] = book }
                42 -> listOf("luke", "luk", "लूका").forEach { map[it] = book }
                43 -> listOf("john", "joh", "jn", "यूहन्ना", "यूह").forEach { map[it] = book }
                44 -> listOf("acts", "act", "प्रेरितों", "प्रेरितों के काम", "प्रेरित").forEach { map[it] = book }
                45 -> listOf("rom", "romans", "रोमियों", "रोमि").forEach { map[it] = book }
                46 -> listOf("1 cor", "1cor", "1 corinthians", "1कुरिन्थियों", "1 कुरिन्थियों", "1कुरि").forEach { map[it] = book }
                47 -> listOf("2 cor", "2cor", "2 corinthians", "2कुरिन्थियों", "2 कुरिन्थियों", "2कुरि").forEach { map[it] = book }
                48 -> listOf("gal", "galatians", "गलतियों", "गलति").forEach { map[it] = book }
                49 -> listOf("eph", "ephesians", "इफिसियों", "इफि").forEach { map[it] = book }
                50 -> listOf("phil", "php", "philippians", "फिलिप्पियों", "फिलि").forEach { map[it] = book }
                51 -> listOf("col", "colossians", "कुलुस्सियों", "कुलु").forEach { map[it] = book }
                52 -> listOf("1 thess", "1thess", "1 thessalonians", "1थिस्सलुनीकियों", "1 थिस्सलुनीकियों", "1थिस्स").forEach { map[it] = book }
                53 -> listOf("2 thess", "2thess", "2 thessalonians", "2थिस्सलुनीकियों", "2 थिस्सलुनीकियों", "2थिस्स").forEach { map[it] = book }
                54 -> listOf("1 tim", "1tim", "1 timothy", "1तीमुथियुस", "1 तीमुथियुस", "1तीमु").forEach { map[it] = book }
                55 -> listOf("2 tim", "2tim", "2 timothy", "2तीमुथियुस", "2 तीमुथियुस", "2तीमु").forEach { map[it] = book }
                58 -> listOf("heb", "hebrews", "इब्रानियों", "इब्रा").forEach { map[it] = book }
                59 -> listOf("jas", "james", "याकूब", "याकू").forEach { map[it] = book }
                60 -> listOf("1 pet", "1pet", "1 peter", "1पतरस", "1 पतरस", "1पत").forEach { map[it] = book }
                61 -> listOf("2 pet", "2pet", "2 peter", "2पतरस", "2 पतरस", "2पत").forEach { map[it] = book }
                62 -> listOf("1 john", "1john", "1 jn", "1यूहन्ना", "1 यूहन्ना", "1यूह").forEach { map[it] = book }
                63 -> listOf("2 john", "2john", "2 jn", "2यूहन्ना", "2 यूहन्ना", "2यूह").forEach { map[it] = book }
                64 -> listOf("3 john", "3john", "3 jn", "3यूहन्ना", "3 यूहन्ना", "3यूह").forEach { map[it] = book }
                65 -> listOf("jude", "jud", "यहूदा", "यहू").forEach { map[it] = book }
                66 -> listOf("rev", "revelation", "प्रकाशितवाक्य", "प्रका").forEach { map[it] = book }
            }
        }
        map
    }

    // Comprehensive Regex matching Book + Chapter : Verse(s) or Verse Range
    // E.g., "यूहन्ना 3:16", "John 3:16-18", "भजन 23:1-6", "1 कुरिन्थियों 13:4-8", "Romans 8:28", "भजन 23"
    private val versePattern = Pattern.compile(
        """(?i)\b(?:([1-3]\s*[a-zA-Z\u0900-\u097F]+|[a-zA-Z\u0900-\u097F]+(?:\s+के\s+काम|\s+संहिता)?)\s*(\d{1,3})(?:\s*[:.:,]?\s*(\d{1,3})(?:\s*[-–—toसे]\s*(\d{1,3}))?)?)""",
        Pattern.UNICODE_CHARACTER_CLASS
    )

    // Regex for inline standalone verse range like "verse 7-10", "वचन 7-10", "vs 7-10"
    private val standaloneVerseRangePattern = Pattern.compile(
        """(?i)\b(?:verse|vs|v\.|वचन|पद)\s*(\d{1,3})(?:\s*[-–—toसे]\s*(\d{1,3}))?""",
        Pattern.UNICODE_CHARACTER_CLASS
    )

    /**
     * Parses all Bible verse references found in a given text (sermon notes, lyrics, comments).
     */
    fun detectVerseReferences(text: String, defaultBookId: Int = 43, defaultChapter: Int = 1): List<DetectedVerseRef> {
        if (text.isBlank()) return emptyList()

        val results = mutableListOf<DetectedVerseRef>()
        val matcher = versePattern.matcher(text)

        var lastFoundBook: BibleBook? = null
        var lastFoundChapter = 1

        while (matcher.find()) {
            val rawBook = matcher.group(1)?.trim() ?: continue
            val rawChapter = matcher.group(2)?.trim()
            val rawStartVerse = matcher.group(3)?.trim()
            val rawEndVerse = matcher.group(4)?.trim()

            val normalizedBookKey = rawBook.lowercase().replace("\\s+".toRegex(), " ")
            val book = bookAliasMap[normalizedBookKey] ?: continue

            val chapter = rawChapter?.toIntOrNull() ?: 1
            if (chapter < 1 || chapter > book.chapterCount) continue

            val startVerse = rawStartVerse?.toIntOrNull() ?: 1
            val endVerse = rawEndVerse?.toIntOrNull() ?: startVerse

            lastFoundBook = book
            lastFoundChapter = chapter

            val displayLabel = when {
                rawStartVerse != null && rawEndVerse != null && startVerse != endVerse -> {
                    "${book.nameHindi} $chapter:$startVerse-$endVerse"
                }
                rawStartVerse != null -> {
                    "${book.nameHindi} $chapter:$startVerse"
                }
                else -> {
                    "${book.nameHindi} $chapter"
                }
            }

            results.add(
                DetectedVerseRef(
                    rawText = matcher.group(0) ?: displayLabel,
                    book = book,
                    bookId = book.id,
                    chapter = chapter,
                    startVerse = startVerse,
                    endVerse = if (endVerse >= startVerse) endVerse else startVerse,
                    displayLabel = displayLabel,
                    startIndex = matcher.start(),
                    endIndex = matcher.end()
                )
            )
        }

        // Also detect standalone verse ranges like "verse 7-10", "वचन 7-10" using the last contextual book or default
        val standaloneMatcher = standaloneVerseRangePattern.matcher(text)
        while (standaloneMatcher.find()) {
            val start = standaloneMatcher.start()
            // Make sure this isn't overlapping with an already matched range
            if (results.any { it.startIndex <= start && start < it.endIndex }) {
                continue
            }

            val startV = standaloneMatcher.group(1)?.toIntOrNull() ?: continue
            val endV = standaloneMatcher.group(2)?.toIntOrNull() ?: startV
            val book = lastFoundBook ?: BibleBookDefinitions.getBookById(defaultBookId) ?: BibleBookDefinitions.books.first()
            val chapter = lastFoundChapter

            val label = if (endV > startV) "${book.nameHindi} $chapter:$startV-$endV" else "${book.nameHindi} $chapter:$startV"

            results.add(
                DetectedVerseRef(
                    rawText = standaloneMatcher.group(0) ?: label,
                    book = book,
                    bookId = book.id,
                    chapter = chapter,
                    startVerse = startV,
                    endVerse = if (endV >= startV) endV else startV,
                    displayLabel = label,
                    startIndex = start,
                    endIndex = standaloneMatcher.end()
                )
            )
        }

        return results.distinctBy { "${it.bookId}_${it.chapter}_${it.startVerse}_${it.endVerse}" }
    }

    /**
     * Parses a single reference string (e.g. from a chip or manual input).
     */
    fun parseSingleReference(refString: String): DetectedVerseRef? {
        val list = detectVerseReferences(refString)
        return list.firstOrNull()
    }
}
