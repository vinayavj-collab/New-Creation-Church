package com.example.data.bible.model

enum class Testament(val hindiName: String, val englishName: String) {
    OLD("पुराना नियम", "Old Testament"),
    NEW("नया नियम", "New Testament")
}

data class BibleBook(
    val id: Int,
    val nameHindi: String,
    val nameEnglish: String,
    val abbreviationHindi: String,
    val abbreviationEnglish: String,
    val testament: Testament,
    val totalChapters: Int,
    val category: String = if (testament == Testament.OLD) "Old Testament" else "New Testament"
) {
    val chapterCount: Int get() = totalChapters

    constructor(
        id: Int,
        nameHindi: String,
        nameEnglish: String,
        testamentStr: String,
        totalChapters: Int
    ) : this(
        id = id,
        nameHindi = nameHindi,
        nameEnglish = nameEnglish,
        abbreviationHindi = nameHindi.take(3),
        abbreviationEnglish = nameEnglish.take(3),
        testament = if (testamentStr.uppercase().startsWith("OLD")) Testament.OLD else Testament.NEW,
        totalChapters = totalChapters
    )
}

object BibleBookDefinitions {
    val BOOKS = listOf(
        // Old Testament (1-39)
        BibleBook(1, "उत्पत्ति", "Genesis", "उत्प", "Gen", Testament.OLD, 50, "Law"),
        BibleBook(2, "निर्गमन", "Exodus", "निर्ग", "Exo", Testament.OLD, 40, "Law"),
        BibleBook(3, "लैव्यव्यवस्था", "Leviticus", "लैव्य", "Lev", Testament.OLD, 27, "Law"),
        BibleBook(4, "गिनती", "Numbers", "गिन", "Num", Testament.OLD, 36, "Law"),
        BibleBook(5, "व्यवस्थाविवरण", "Deuteronomy", "व्यव", "Deu", Testament.OLD, 34, "Law"),
        BibleBook(6, "यहोशू", "Joshua", "यहो", "Jos", Testament.OLD, 24, "History"),
        BibleBook(7, "न्यायियों", "Judges", "न्याय", "Jdg", Testament.OLD, 21, "History"),
        BibleBook(8, "रूत", "Ruth", "रूत", "Rut", Testament.OLD, 4, "History"),
        BibleBook(9, "1 शमूएल", "1 Samuel", "1शम", "1Sa", Testament.OLD, 31, "History"),
        BibleBook(10, "2 शमूएल", "2 Samuel", "2शम", "2Sa", Testament.OLD, 24, "History"),
        BibleBook(11, "1 राजा", "1 Kings", "1राज", "1Ki", Testament.OLD, 22, "History"),
        BibleBook(12, "2 राजा", "2 Kings", "2राज", "2Ki", Testament.OLD, 25, "History"),
        BibleBook(13, "1 इतिहास", "1 Chronicles", "1इति", "1Ch", Testament.OLD, 29, "History"),
        BibleBook(14, "2 इतिहास", "2 Chronicles", "2इति", "2Ch", Testament.OLD, 36, "History"),
        BibleBook(15, "एज्रा", "Ezra", "एज्रा", "Ezr", Testament.OLD, 10, "History"),
        BibleBook(16, "नहेमायाह", "Nehemiah", "नहे", "Neh", Testament.OLD, 13, "History"),
        BibleBook(17, "एस्तेर", "Esther", "एस्ते", "Est", Testament.OLD, 10, "History"),
        BibleBook(18, "अय्यूब", "Job", "अय्यू", "Job", Testament.OLD, 42, "Poetry"),
        BibleBook(19, "भजन संहिता", "Psalms", "भजन", "Psa", Testament.OLD, 150, "Poetry"),
        BibleBook(20, "नीतिवचन", "Proverbs", "नीति", "Pro", Testament.OLD, 31, "Wisdom"),
        BibleBook(21, "सभोपदेशक", "Ecclesiastes", "सभो", "Ecc", Testament.OLD, 12, "Wisdom"),
        BibleBook(22, "श्रेष्ठगीत", "Song of Solomon", "श्रेष्ठ", "Sng", Testament.OLD, 8, "Poetry"),
        BibleBook(23, "यशायाह", "Isaiah", "यशा", "Isa", Testament.OLD, 66, "Prophecy"),
        BibleBook(24, "यिर्मयाह", "Jeremiah", "यिर्म", "Jer", Testament.OLD, 52, "Prophecy"),
        BibleBook(25, "विलापगीत", "Lamentations", "विला", "Lam", Testament.OLD, 5, "Poetry"),
        BibleBook(26, "यहेजकेल", "Ezekiel", "यहेज", "Ezk", Testament.OLD, 48, "Prophecy"),
        BibleBook(27, "दानिय्येल", "Daniel", "दानि", "Dan", Testament.OLD, 12, "Prophecy"),
        BibleBook(28, "होशे", "Hosea", "होशे", "Hos", Testament.OLD, 14, "Minor Prophets"),
        BibleBook(29, "योएल", "Joel", "योएल", "Jol", Testament.OLD, 3, "Minor Prophets"),
        BibleBook(30, "आमोस", "Amos", "आमो", "Amo", Testament.OLD, 9, "Minor Prophets"),
        BibleBook(31, "ओबद्याह", "Obadiah", "ओब", "Oba", Testament.OLD, 1, "Minor Prophets"),
        BibleBook(32, "योना", "Jonah", "योना", "Jon", Testament.OLD, 4, "Minor Prophets"),
        BibleBook(33, "मीका", "Micah", "मीका", "Mic", Testament.OLD, 7, "Minor Prophets"),
        BibleBook(34, "नहूम", "Nahum", "नहूम", "Nah", Testament.OLD, 3, "Minor Prophets"),
        BibleBook(35, "हबक्कूक", "Habakkuk", "हब", "Hab", Testament.OLD, 3, "Minor Prophets"),
        BibleBook(36, "सपन्याह", "Zephaniah", "सपन", "Zep", Testament.OLD, 3, "Minor Prophets"),
        BibleBook(37, "हाग्गै", "Haggai", "हाग्गै", "Hag", Testament.OLD, 2, "Minor Prophets"),
        BibleBook(38, "जकर्याह", "Zechariah", "जक", "Zec", Testament.OLD, 14, "Minor Prophets"),
        BibleBook(39, "मलाकी", "Malachi", "मला", "Mal", Testament.OLD, 4, "Minor Prophets"),

        // New Testament (40-66)
        BibleBook(40, "मत्ती", "Matthew", "मत्ती", "Mat", Testament.NEW, 28, "Gospels"),
        BibleBook(41, "मार्क", "Mark", "मार्क", "Mrk", Testament.NEW, 16, "Gospels"),
        BibleBook(42, "लूका", "Luke", "लूका", "Luk", Testament.NEW, 24, "Gospels"),
        BibleBook(43, "यूहन्ना", "John", "यूह", "Jhn", Testament.NEW, 21, "Gospels"),
        BibleBook(44, "प्रेरितों के काम", "Acts", "प्रेरित", "Act", Testament.NEW, 28, "History"),
        BibleBook(45, "रोमियों", "Romans", "रोमि", "Rom", Testament.NEW, 16, "Pauline Epistles"),
        BibleBook(46, "1 कुरिन्थियों", "1 Corinthians", "1कुरि", "1Co", Testament.NEW, 16, "Pauline Epistles"),
        BibleBook(47, "2 कुरिन्थियों", "2 Corinthians", "2कुरि", "2Co", Testament.NEW, 13, "Pauline Epistles"),
        BibleBook(48, "गलातियों", "Galatians", "गला", "Gal", Testament.NEW, 6, "Pauline Epistles"),
        BibleBook(49, "इफिसियों", "Ephesians", "इफि", "Eph", Testament.NEW, 6, "Pauline Epistles"),
        BibleBook(50, "फिलिप्पियों", "Philippians", "फिलि", "Php", Testament.NEW, 4, "Pauline Epistles"),
        BibleBook(51, "कुलुस्सियों", "Colossians", "कुलु", "Col", Testament.NEW, 4, "Pauline Epistles"),
        BibleBook(52, "1 थिस्सलुनीकियों", "1 Thessalonians", "1थिस", "1Th", Testament.NEW, 5, "Pauline Epistles"),
        BibleBook(53, "2 थिस्सलुनीकियों", "2 Thessalonians", "2थिस", "2Th", Testament.NEW, 3, "Pauline Epistles"),
        BibleBook(54, "1 तीमुथियुस", "1 Timothy", "1तीम", "1Ti", Testament.NEW, 6, "Pauline Epistles"),
        BibleBook(55, "2 तीमुथियुस", "2 Timothy", "2तीम", "2Ti", Testament.NEW, 4, "Pauline Epistles"),
        BibleBook(56, "तीतुस", "Titus", "तीतु", "Tit", Testament.NEW, 3, "Pauline Epistles"),
        BibleBook(57, "फिलेमोन", "Philemon", "फिले", "Phm", Testament.NEW, 1, "Pauline Epistles"),
        BibleBook(58, "इब्रानियों", "Hebrews", "इब्रा", "Heb", Testament.NEW, 13, "General Epistles"),
        BibleBook(59, "याकूब", "James", "याकूब", "Jas", Testament.NEW, 5, "General Epistles"),
        BibleBook(60, "1 पतरस", "1 Peter", "1पत", "1Pe", Testament.NEW, 5, "General Epistles"),
        BibleBook(61, "2 पतरस", "2 Peter", "2पत", "2Pe", Testament.NEW, 3, "General Epistles"),
        BibleBook(62, "1 यूहन्ना", "1 John", "1यूह", "1Jn", Testament.NEW, 5, "General Epistles"),
        BibleBook(63, "2 यूहन्ना", "2 John", "2यूह", "2Jn", Testament.NEW, 1, "General Epistles"),
        BibleBook(64, "3 यूहन्ना", "3 John", "3यूह", "3Jn", Testament.NEW, 1, "General Epistles"),
        BibleBook(65, "यहूदा", "Jude", "यहूदा", "Jud", Testament.NEW, 1, "General Epistles"),
        BibleBook(66, "प्रकाशितवाक्य", "Revelation", "प्रका", "Rev", Testament.NEW, 22, "Apocalyptic")
    )

    val books: List<BibleBook> get() = BOOKS
    val ALL_BOOKS: List<BibleBook> get() = BOOKS
    val oldTestamentBooks: List<BibleBook> get() = BOOKS.filter { it.testament == Testament.OLD }
    val newTestamentBooks: List<BibleBook> get() = BOOKS.filter { it.testament == Testament.NEW }

    fun getBookById(id: Int): BibleBook? = BOOKS.find { it.id == id }
}
