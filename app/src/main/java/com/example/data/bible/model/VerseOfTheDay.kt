package com.example.data.bible.model

data class VerseOfTheDay(
    val bookId: Int,
    val bookNameHindi: String,
    val bookNameEnglish: String,
    val chapter: Int,
    val verseNumber: Int,
    val textHindi: String,
    val textEnglish: String
) {
    val referenceHindi: String get() = "$bookNameHindi $chapter:$verseNumber"
    val referenceEnglish: String get() = "$bookNameEnglish $chapter:$verseNumber"

    companion object {
        val dailyVerses = listOf(
            VerseOfTheDay(
                bookId = 43, // John
                bookNameHindi = "यूहन्ना",
                bookNameEnglish = "John",
                chapter = 3,
                verseNumber = 16,
                textHindi = "क्योंकि परमेश्वर ने जगत से ऐसा प्रेम रखा कि उसने अपना एकलौता पुत्र दे दिया, ताकि जो कोई उस पर विश्वास करे वह नष्ट न हो, परन्तु अनन्त जीवन पाए।",
                textEnglish = "For God so loved the world, that he gave his one and only Son, that whoever believes in him should not perish, but have eternal life."
            ),
            VerseOfTheDay(
                bookId = 19, // Psalms
                bookNameHindi = "भजन संहिता",
                bookNameEnglish = "Psalms",
                chapter = 23,
                verseNumber = 1,
                textHindi = "यहोवा मेरा चरवाहा है, मुझे कोई घटी न होगी।",
                textEnglish = "The LORD is my shepherd: I shall not want."
            ),
            VerseOfTheDay(
                bookId = 45, // Romans
                bookNameHindi = "रोमियों",
                bookNameEnglish = "Romans",
                chapter = 8,
                verseNumber = 28,
                textHindi = "और हम जानते हैं कि जो लोग परमेश्वर से प्रेम रखते हैं, उनके लिये सब बातें मिलकर भलाई ही को उत्पन्न करती हैं; अर्थात् उन्हीं के लिये जो उसकी इच्छा के अनुसार बुलाए हुए हैं।",
                textEnglish = "We know that all things work together for good for those who love God, to those who are called according to his purpose."
            ),
            VerseOfTheDay(
                bookId = 50, // Philippians
                bookNameHindi = "फिलिप्पियों",
                bookNameEnglish = "Philippians",
                chapter = 4,
                verseNumber = 13,
                textHindi = "जो मुझे सामर्थ्य देता है उसमें मैं सब कुछ कर सकता हूँ।",
                textEnglish = "I can do all things through Christ, who strengthens me."
            ),
            VerseOfTheDay(
                bookId = 20, // Proverbs
                bookNameHindi = "नीतिवचन",
                bookNameEnglish = "Proverbs",
                chapter = 3,
                verseNumber = 5,
                textHindi = "तू अपनी समझ का सहारा न लेना, वरन सम्पूर्ण मन से यहोवा पर भरोसा रखना।",
                textEnglish = "Trust in the LORD with all your heart, and don’t lean on your own understanding."
            ),
            VerseOfTheDay(
                bookId = 24, // Jeremiah
                bookNameHindi = "यिर्मयाह",
                bookNameEnglish = "Jeremiah",
                chapter = 29,
                verseNumber = 11,
                textHindi = "क्योंकि यहोवा की यह वाणी है, कि जो कल्पनाएं मैं तुम्हारे विषय करता हूँ उन्हें मैं जानता हूँ, वे भलाई ही की हैं, बुराई की नहीं, इसलिये कि मैं तुम्हें अन्त में आशा पूरी करूँ।",
                textEnglish = "For I know the thoughts that I think toward you, says the LORD, thoughts of peace, and not of evil, to give you hope and a future."
            ),
            VerseOfTheDay(
                bookId = 46, // 1 Corinthians
                bookNameHindi = "1 कुरिन्थियों",
                bookNameEnglish = "1 Corinthians",
                chapter = 13,
                verseNumber = 4,
                textHindi = "प्रेम धीरजवन्त है, और कृपालु है; प्रेम डाह नहीं करता; प्रेम अपनी बड़ाई नहीं करता, और फूलता नहीं।",
                textEnglish = "Love is patient and is kind; love doesn't envy. Love doesn't brag, is not proud."
            )
        )

        fun getTodayVerse(): VerseOfTheDay {
            val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
            return dailyVerses[dayOfYear % dailyVerses.size]
        }
    }
}
