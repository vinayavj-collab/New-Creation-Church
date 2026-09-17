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

    // Multi-Language Book Mapping HashMap: maps Hindi and English book names (and common abbreviations) to Book IDs (1 to 66)
    private val bookAliasMap: HashMap<String, Int> by lazy {
        val map = HashMap<String, Int>()

        BibleBookDefinitions.books.forEach { book ->
            map[book.nameHindi.lowercase().trim()] = book.id
            map[book.abbreviationHindi.lowercase().trim()] = book.id
            map[book.nameEnglish.lowercase().trim()] = book.id
            map[book.abbreviationEnglish.lowercase().trim()] = book.id

            // Special common variations & mappings
            when (book.id) {
                1 -> listOf("gen", "genesis", "उत्प", "उत्पत्ति").forEach { map[it] = book.id }
                2 -> listOf("exo", "exodus", "निर्ग", "निर्गमन").forEach { map[it] = book.id }
                3 -> listOf("lev", "leviticus", "लैव्य", "लैव्यव्यवस्था").forEach { map[it] = book.id }
                4 -> listOf("num", "numbers", "गिन", "गिनती").forEach { map[it] = book.id }
                5 -> listOf("deut", "deuteronomy", "व्यव", "व्यवस्थाविवरण").forEach { map[it] = book.id }
                6 -> listOf("josh", "joshua", "यहो", "यहोशू").forEach { map[it] = book.id }
                7 -> listOf("judg", "judges", "न्याय", "न्यायियों").forEach { map[it] = book.id }
                8 -> listOf("ruth", "रूत").forEach { map[it] = book.id }
                9 -> listOf("1 sam", "1sam", "1शमू", "1 शमूएल").forEach { map[it] = book.id }
                10 -> listOf("2 sam", "2sam", "2शमू", "2 शमूएल").forEach { map[it] = book.id }
                11 -> listOf("1 kin", "1kings", "1राजा", "1 राजा").forEach { map[it] = book.id }
                12 -> listOf("2 kin", "2kings", "2राजा", "2 राजा").forEach { map[it] = book.id }
                13 -> listOf("1 chr", "1chronicles", "1इति", "1 इतिहास").forEach { map[it] = book.id }
                14 -> listOf("2 chr", "2chronicles", "2इति", "2 इतिहास").forEach { map[it] = book.id }
                15 -> listOf("ezra", "एज्रा").forEach { map[it] = book.id }
                16 -> listOf("neh", "nehemiah", "नहे", "नहेमायाह").forEach { map[it] = book.id }
                17 -> listOf("esth", "esther", "एस्त", "एस्तेर").forEach { map[it] = book.id }
                18 -> listOf("job", "अय्यू", "अय्यूब").forEach { map[it] = book.id }
                19 -> listOf("psalm", "psalms", "psa", "ps", "भजन", "भजन संहिता", "भजनसंहिता").forEach { map[it] = book.id }
                20 -> listOf("prov", "proverbs", "नीति", "नीतिवचन").forEach { map[it] = book.id }
                21 -> listOf("eccl", "ecclesiastes", "सभो", "सभोपदेशक").forEach { map[it] = book.id }
                22 -> listOf("song", "songs", "song of solomon", "श्रेष्ठ", "श्रेष्ठगीत").forEach { map[it] = book.id }
                23 -> listOf("isa", "isaiah", "यशा", "यशायाह").forEach { map[it] = book.id }
                24 -> listOf("jer", "jeremiah", "यिर्म", "यिर्मयाह").forEach { map[it] = book.id }
                25 -> listOf("lam", "lamentations", "विला", "विलापगीत").forEach { map[it] = book.id }
                26 -> listOf("ezek", "ezekiel", "यहेज", "यहेजकेल").forEach { map[it] = book.id }
                27 -> listOf("dan", "daniel", "दानि", "दानिय्येल").forEach { map[it] = book.id }
                28 -> listOf("hos", "hosea", "होशे").forEach { map[it] = book.id }
                29 -> listOf("joel", "योएल").forEach { map[it] = book.id }
                30 -> listOf("amos", "आमो", "आमोस").forEach { map[it] = book.id }
                31 -> listOf("obad", "obadiah", "ओब", "ओबद्याह").forEach { map[it] = book.id }
                32 -> listOf("jonah", "योना").forEach { map[it] = book.id }
                33 -> listOf("mic", "micah", "मीका").forEach { map[it] = book.id }
                34 -> listOf("nah", "nahum", "नहूम").forEach { map[it] = book.id }
                35 -> listOf("hab", "habakkuk", "हब", "हबक्कूक").forEach { map[it] = book.id }
                36 -> listOf("zeph", "zephaniah", "सपन", "सपन्याह").forEach { map[it] = book.id }
                37 -> listOf("hag", "haggai", "हाग्गै").forEach { map[it] = book.id }
                38 -> listOf("zech", "zechariah", "जकर्", "जकर्याह").forEach { map[it] = book.id }
                39 -> listOf("mal", "malachi", "मला", "मलाकी").forEach { map[it] = book.id }
                40 -> listOf("matt", "mat", "matthew", "मत्ती").forEach { map[it] = book.id }
                41 -> listOf("mark", "mrk", "मरकुस", "मर").forEach { map[it] = book.id }
                42 -> listOf("luke", "luk", "लूका").forEach { map[it] = book.id }
                43 -> listOf("john", "joh", "jn", "यूहन्ना", "यूह").forEach { map[it] = book.id }
                44 -> listOf("acts", "act", "प्रेरितों", "प्रेरितों के काम", "प्रेरित").forEach { map[it] = book.id }
                45 -> listOf("rom", "romans", "रोमियों", "रोमि").forEach { map[it] = book.id }
                46 -> listOf("1 cor", "1cor", "1 corinthians", "1कुरिन्थियों", "1 कुरिन्थियों", "1कुरि").forEach { map[it] = book.id }
                47 -> listOf("2 cor", "2cor", "2 corinthians", "2कुरिन्थियों", "2 कुरिन्थियों", "2कुरि").forEach { map[it] = book.id }
                48 -> listOf("gal", "galatians", "गलतियों", "गलति").forEach { map[it] = book.id }
                49 -> listOf("eph", "ephesians", "इफिसियों", "इफि").forEach { map[it] = book.id }
                50 -> listOf("phil", "php", "philippians", "फिलिप्पियों", "फिलि").forEach { map[it] = book.id }
                51 -> listOf("col", "colossians", "कुलुस्सियों", "कुलु").forEach { map[it] = book.id }
                52 -> listOf("1 thess", "1thess", "1 thessalonians", "1थिस्सलुनीकियों", "1 थिस्सलुनीकियों", "1थिस्स").forEach { map[it] = book.id }
                53 -> listOf("2 thess", "2thess", "2 thessalonians", "2थिस्सलुनीकियों", "2 थिस्सलुनीकियों", "2थिस्स").forEach { map[it] = book.id }
                54 -> listOf("1 tim", "1tim", "1 timothy", "1तीमुथियुस", "1 तीमुथियुस", "1तीमु").forEach { map[it] = book.id }
                55 -> listOf("2 tim", "2tim", "2 timothy", "2तीमुथियुस", "2 तीमुथियुस", "2तीमु").forEach { map[it] = book.id }
                56 -> listOf("titus", "तीतुस", "तीतु").forEach { map[it] = book.id }
                57 -> listOf("phlm", "philemon", "फिलेमोन", "फिले").forEach { map[it] = book.id }
                58 -> listOf("heb", "hebrews", "इब्रानियों", "इब्रा").forEach { map[it] = book.id }
                59 -> listOf("jas", "james", "याकूब", "याकू").forEach { map[it] = book.id }
                60 -> listOf("1 pet", "1pet", "1 peter", "1पतरस", "1 पतरस", "1पत").forEach { map[it] = book.id }
                61 -> listOf("2 pet", "2pet", "2 peter", "2पतरस", "2 पतरस", "2पत").forEach { map[it] = book.id }
                62 -> listOf("1 john", "1john", "1 jn", "1यूहन्ना", "1 यूहन्ना", "1यूह").forEach { map[it] = book.id }
                63 -> listOf("2 john", "2john", "2 jn", "2यूहन्ना", "2 यूहन्ना", "2यूह").forEach { map[it] = book.id }
                64 -> listOf("3 john", "3john", "3 jn", "3यूहन्ना", "3 यूहन्ना", "3यूह").forEach { map[it] = book.id }
                65 -> listOf("jude", "jud", "यहूदा", "यहू").forEach { map[it] = book.id }
                66 -> listOf("rev", "revelation", "प्रकाशितवाक्य", "प्रका").forEach { map[it] = book.id }
            }
        }
        map
    }

    // Unicode Regex Pattern supporting English and Devanagari characters: [\p{IsDevanagari}a-zA-Z0-9\s:-]+
    private val versePattern = Pattern.compile(
        """(?i)\b(?:([1-3]\s*[\p{IsDevanagari}a-zA-Z]+|[\p{IsDevanagari}a-zA-Z]+(?:\s+के\s+काम|\s+संहिता)?)\s*(\d{1,3})(?:\s*[:.:,]?\s*(\d{1,3})(?:\s*[-–—toसे]\s*(\d{1,3}))?)?)""",
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
            val bookId = bookAliasMap[normalizedBookKey] ?: continue
            val book = BibleBookDefinitions.getBookById(bookId) ?: continue

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
