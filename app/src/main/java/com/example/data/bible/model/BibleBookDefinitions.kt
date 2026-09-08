package com.example.data.bible.model

object BibleBookDefinitions {
    val books: List<BibleBook> = listOf(
        // Old Testament (39 books)
        BibleBook(1, "उत्पत्ति", "Genesis", "उत्प", "Gen", Testament.OLD, 50, "व्यवस्था"),
        BibleBook(2, "निर्गमन", "Exodus", "निर्ग", "Exo", Testament.OLD, 40, "व्यवस्था"),
        BibleBook(3, "लैव्यव्यवस्था", "Leviticus", "लैव्य", "Lev", Testament.OLD, 27, "व्यवस्था"),
        BibleBook(4, "गिनती", "Numbers", "गिन", "Num", Testament.OLD, 36, "व्यवस्था"),
        BibleBook(5, "व्यवस्थाविवरण", "Deuteronomy", "व्यव", "Deut", Testament.OLD, 34, "व्यवस्था"),
        BibleBook(6, "यहोशू", "Joshua", "यहो", "Josh", Testament.OLD, 24, "इतिहास"),
        BibleBook(7, "न्यायियों", "Judges", "न्याय", "Judg", Testament.OLD, 21, "इतिहास"),
        BibleBook(8, "रूत", "Ruth", "रूत", "Ruth", Testament.OLD, 4, "इतिहास"),
        BibleBook(9, "1 शमूएल", "1 Samuel", "1शमू", "1Sam", Testament.OLD, 31, "इतिहास"),
        BibleBook(10, "2 शमूएल", "2 Samuel", "2शमू", "2Sam", Testament.OLD, 24, "इतिहास"),
        BibleBook(11, "1 राजा", "1 Kings", "1राजा", "1Kin", Testament.OLD, 22, "इतिहास"),
        BibleBook(12, "2 राजा", "2 Kings", "2राजा", "2Kin", Testament.OLD, 25, "इतिहास"),
        BibleBook(13, "1 इतिहास", "1 Chronicles", "1इति", "1Chr", Testament.OLD, 29, "इतिहास"),
        BibleBook(14, "2 इतिहास", "2 Chronicles", "2इति", "2Chr", Testament.OLD, 36, "इतिहास"),
        BibleBook(15, "एज्रा", "Ezra", "एज्रा", "Ezra", Testament.OLD, 10, "इतिहास"),
        BibleBook(16, "नहेमायाह", "Nehemiah", "नहे", "Neh", Testament.OLD, 13, "इतिहास"),
        BibleBook(17, "एस्तेर", "Esther", "एस्त", "Esth", Testament.OLD, 10, "इतिहास"),
        BibleBook(18, "अय्यूब", "Job", "अय्यू", "Job", Testament.OLD, 42, "काव्य"),
        BibleBook(19, "भजन संहिता", "Psalms", "भजन", "Psa", Testament.OLD, 150, "काव्य"),
        BibleBook(20, "नीतिवचन", "Proverbs", "नीति", "Prov", Testament.OLD, 31, "काव्य"),
        BibleBook(21, "सभोपदेशक", "Ecclesiastes", "सभो", "Eccl", Testament.OLD, 12, "काव्य"),
        BibleBook(22, "श्रेष्ठगीत", "Song of Solomon", "श्रेष्ठ", "Song", Testament.OLD, 8, "काव्य"),
        BibleBook(23, "यशायाह", "Isaiah", "यशा", "Isa", Testament.OLD, 66, "भविष्यद्वाणी"),
        BibleBook(24, "यिर्मयाह", "Jeremiah", "यिर्म", "Jer", Testament.OLD, 52, "भविष्यद्वाणी"),
        BibleBook(25, "विलापगीत", "Lamentations", "विला", "Lam", Testament.OLD, 5, "भविष्यद्वाणी"),
        BibleBook(26, "यहेजकेल", "Ezekiel", "यहेज", "Ezek", Testament.OLD, 48, "भविष्यद्वाणी"),
        BibleBook(27, "दानिय्येल", "Daniel", "दानि", "Dan", Testament.OLD, 12, "भविष्यद्वाणी"),
        BibleBook(28, "होशे", "Hosea", "होशे", "Hos", Testament.OLD, 14, "भविष्यद्वाणी"),
        BibleBook(29, "योएल", "Joel", "योएल", "Joel", Testament.OLD, 3, "भविष्यद्वाणी"),
        BibleBook(30, "आमोस", "Amos", "आमो", "Amos", Testament.OLD, 9, "भविष्यद्वाणी"),
        BibleBook(31, "ओबद्याह", "Obadiah", "ओब", "Obad", Testament.OLD, 1, "भविष्यद्वाणी"),
        BibleBook(32, "योना", "Jonah", "योना", "Jonah", Testament.OLD, 4, "भविष्यद्वाणी"),
        BibleBook(33, "मीका", "Micah", "मीका", "Mic", Testament.OLD, 7, "भविष्यद्वाणी"),
        BibleBook(34, "नहूम", "Nahum", "नहूम", "Nah", Testament.OLD, 3, "भविष्यद्वाणी"),
        BibleBook(35, "हबक्कूक", "Habakkuk", "हब", "Hab", Testament.OLD, 3, "भविष्यद्वाणी"),
        BibleBook(36, "सपन्याह", "Zephaniah", "सपन", "Zeph", Testament.OLD, 3, "भविष्यद्वाणी"),
        BibleBook(37, "हाग्गै", "Haggai", "हाग्गै", "Hag", Testament.OLD, 2, "भविष्यद्वाणी"),
        BibleBook(38, "जकर्याह", "Zechariah", "जकर्", "Zech", Testament.OLD, 14, "भविष्यद्वाणी"),
        BibleBook(39, "मलाकी", "Malachi", "मला", "Mal", Testament.OLD, 4, "भविष्यद्वाणी"),

        // New Testament (27 books)
        BibleBook(40, "मत्ती", "Matthew", "मत्ती", "Matt", Testament.NEW, 28, "सुसमाचार"),
        BibleBook(41, "मरकुस", "Mark", "मर", "Mark", Testament.NEW, 16, "सुसमाचार"),
        BibleBook(42, "लूका", "Luke", "लूका", "Luke", Testament.NEW, 24, "सुसमाचार"),
        BibleBook(43, "यूहन्ना", "John", "यूह", "John", Testament.NEW, 21, "सुसमाचार"),
        BibleBook(44, "प्रेरितों के काम", "Acts", "प्रेरित", "Acts", Testament.NEW, 28, "इतिहास"),
        BibleBook(45, "रोमियों", "Romans", "रोमि", "Rom", Testament.NEW, 16, "पत्रियां"),
        BibleBook(46, "1 कुरिन्थियों", "1 Corinthians", "1कुरि", "1Cor", Testament.NEW, 16, "पत्रियां"),
        BibleBook(47, "2 कुरिन्थियों", "2 Corinthians", "2कुरि", "2Cor", Testament.NEW, 13, "पत्रियां"),
        BibleBook(48, "गलतियों", "Galatians", "गलति", "Gal", Testament.NEW, 6, "पत्रियां"),
        BibleBook(49, "इफिसियों", "Ephesians", "इफि", "Eph", Testament.NEW, 6, "पत्रियां"),
        BibleBook(50, "फिलिप्पियों", "Philippians", "फिलि", "Phil", Testament.NEW, 4, "पत्रियां"),
        BibleBook(51, "कुलुस्सियों", "Colossians", "कुलु", "Col", Testament.NEW, 4, "पत्रियां"),
        BibleBook(52, "1 थिस्सलुनीकियों", "1 Thessalonians", "1थिस्स", "1Thess", Testament.NEW, 5, "पत्रियां"),
        BibleBook(53, "2 थिस्सलुनीकियों", "2 Thessalonians", "2थिस्स", "2Thess", Testament.NEW, 3, "पत्रियां"),
        BibleBook(54, "1 तीमुथियुस", "1 Timothy", "1तीमु", "1Tim", Testament.NEW, 6, "पत्रियां"),
        BibleBook(55, "2 तीमुथियुस", "2 Timothy", "2तीमु", "2Tim", Testament.NEW, 4, "पत्रियां"),
        BibleBook(56, "तीतुस", "Titus", "तीतु", "Titus", Testament.NEW, 3, "पत्रियां"),
        BibleBook(57, "फिलेमोन", "Philemon", "फिले", "Phlm", Testament.NEW, 1, "पत्रियां"),
        BibleBook(58, "इब्रानियों", "Hebrews", "इब्रा", "Heb", Testament.NEW, 13, "पत्रियां"),
        BibleBook(59, "याकूब", "James", "याकू", "Jas", Testament.NEW, 5, "पत्रियां"),
        BibleBook(60, "1 पतरस", "1 Peter", "1पत", "1Pet", Testament.NEW, 5, "पत्रियां"),
        BibleBook(61, "2 पतरस", "2 Peter", "2पत", "2Pet", Testament.NEW, 3, "पत्रियां"),
        BibleBook(62, "1 यूहन्ना", "1 John", "1यूह", "1John", Testament.NEW, 5, "पत्रियां"),
        BibleBook(63, "2 यूहन्ना", "2 John", "2यूह", "2John", Testament.NEW, 1, "पत्रियां"),
        BibleBook(64, "3 यूहन्ना", "3 John", "3यूह", "3John", Testament.NEW, 1, "पत्रियां"),
        BibleBook(65, "यहूदा", "Jude", "यहू", "Jude", Testament.NEW, 1, "पत्रियां"),
        BibleBook(66, "प्रकाशितवाक्य", "Revelation", "प्रका", "Rev", Testament.NEW, 22, "भविष्यद्वाणी")
    )

    fun getBookById(id: Int): BibleBook? = books.find { it.id == id }

    val oldTestamentBooks: List<BibleBook> by lazy {
        books.filter { it.testament == Testament.OLD }
    }

    val newTestamentBooks: List<BibleBook> by lazy {
        books.filter { it.testament == Testament.NEW }
    }
}
