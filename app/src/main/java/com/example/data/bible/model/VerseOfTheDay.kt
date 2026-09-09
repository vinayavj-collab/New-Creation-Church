package com.example.data.bible.model

import java.util.Calendar

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
        // Curated authentic Scripture schedule with deterministic date mapping
        val dailyVerses = listOf(
            VerseOfTheDay(
                bookId = 19, // Psalms 23:1
                bookNameHindi = "भजन संहिता",
                bookNameEnglish = "Psalm",
                chapter = 23,
                verseNumber = 1,
                textHindi = "यहोवा मेरा चरवाहा है, मुझे कोई घटी न होगी।",
                textEnglish = "The LORD is my shepherd; I shall not want."
            ),
            VerseOfTheDay(
                bookId = 43, // John 3:16
                bookNameHindi = "यूहन्ना",
                bookNameEnglish = "John",
                chapter = 3,
                verseNumber = 16,
                textHindi = "क्योंकि परमेश्वर ने जगत से ऐसा प्रेम रखा कि उसने अपना एकलौता पुत्र दे दिया, ताकि जो कोई उस पर विश्वास करे वह नष्ट न हो, परन्तु अनन्त जीवन पाए।",
                textEnglish = "For God so loved the world, that he gave his one and only Son, that whoever believes in him should not perish, but have eternal life."
            ),
            VerseOfTheDay(
                bookId = 45, // Romans 8:28
                bookNameHindi = "रोमियों",
                bookNameEnglish = "Romans",
                chapter = 8,
                verseNumber = 28,
                textHindi = "और हम जानते हैं कि जो लोग परमेश्वर से प्रेम रखते हैं, उनके लिये सब बातें मिलकर भलाई ही को उत्पन्न करती हैं; अर्थात् उन्हीं के लिये जो उसकी इच्छा के अनुसार बुलाए हुए हैं।",
                textEnglish = "We know that all things work together for good for those who love God, to those who are called according to his purpose."
            ),
            VerseOfTheDay(
                bookId = 50, // Philippians 4:13
                bookNameHindi = "फिलिप्पियों",
                bookNameEnglish = "Philippians",
                chapter = 4,
                verseNumber = 13,
                textHindi = "जो मुझे सामर्थ्य देता है उसमें मैं सब कुछ कर सकता हूँ।",
                textEnglish = "I can do all things through Christ, who strengthens me."
            ),
            VerseOfTheDay(
                bookId = 20, // Proverbs 3:5
                bookNameHindi = "नीतिवचन",
                bookNameEnglish = "Proverbs",
                chapter = 3,
                verseNumber = 5,
                textHindi = "तू अपनी समझ का सहारा न लेना, वरन सम्पूर्ण मन से यहोवा पर भरोसा रखना।",
                textEnglish = "Trust in the LORD with all your heart, and don’t lean on your own understanding."
            ),
            VerseOfTheDay(
                bookId = 24, // Jeremiah 29:11
                bookNameHindi = "यिर्मयाह",
                bookNameEnglish = "Jeremiah",
                chapter = 29,
                verseNumber = 11,
                textHindi = "क्योंकि यहोवा की यह वाणी है, कि जो कल्पनाएं मैं तुम्हारे विषय करता हूँ उन्हें मैं जानता हूँ, वे भलाई ही की हैं, बुराई की नहीं, इसलिये कि मैं तुम्हें अन्त में आशा पूरी करूँ।",
                textEnglish = "For I know the thoughts that I think toward you, says the LORD, thoughts of peace, and not of evil, to give you hope and a future."
            ),
            VerseOfTheDay(
                bookId = 46, // 1 Corinthians 13:4
                bookNameHindi = "1 कुरिन्थियों",
                bookNameEnglish = "1 Corinthians",
                chapter = 13,
                verseNumber = 4,
                textHindi = "प्रेम धीरजवन्त है, और कृपालु है; प्रेम डाह नहीं करता; प्रेम अपनी बड़ाई नहीं करता, और फूलता नहीं।",
                textEnglish = "Love is patient and is kind; love doesn't envy. Love doesn't brag, is not proud."
            ),
            VerseOfTheDay(
                bookId = 23, // Isaiah 40:31
                bookNameHindi = "यशायाह",
                bookNameEnglish = "Isaiah",
                chapter = 40,
                verseNumber = 31,
                textHindi = "परन्तु जो यहोवा की बाट जोहते हैं, वे नया बल प्राप्त करते जाएंगे, वे उकाबों के समान उड़ेंगे, वे दौड़ेंगे और थकित न होंगे, चलेंगे और मूर्छित न होंगे।",
                textEnglish = "But those who wait for the LORD will renew their strength. They will mount up with wings like eagles. They will run, and not be weary. They will walk, and not faint."
            ),
            VerseOfTheDay(
                bookId = 40, // Matthew 6:33
                bookNameHindi = "मत्ती",
                bookNameEnglish = "Matthew",
                chapter = 6,
                verseNumber = 33,
                textHindi = "इसलिए पहले तुम परमेश्वर के राज्य और उसके धर्म की खोज करो तो ये सब वस्तुएं भी तुम्हें मिल जाएंगी।",
                textEnglish = "But seek first God’s Kingdom, and his righteousness; and all these things will be given to you as well."
            ),
            VerseOfTheDay(
                bookId = 50, // Philippians 4:6
                bookNameHindi = "फिलिप्पियों",
                bookNameEnglish = "Philippians",
                chapter = 4,
                verseNumber = 6,
                textHindi = "किसी भी बात की चिन्ता मत करो; परन्तु हर एक बात में तुम्हारे निवेदन, प्रार्थना और विन्ती के द्वारा धन्यवाद के साथ परमेश्वर के सम्मुख उपस्थित किए जाएं।",
                textEnglish = "In nothing be anxious, but in everything, by prayer and petition with thanksgiving, let your requests be made known to God."
            ),
            VerseOfTheDay(
                bookId = 19, // Psalm 46:1
                bookNameHindi = "भजन संहिता",
                bookNameEnglish = "Psalm",
                chapter = 46,
                verseNumber = 1,
                textHindi = "परमेश्वर हमारा शरणस्थान और बल है, संकट में अति सहज से मिलने वाला सहायक।",
                textEnglish = "God is our refuge and strength, a very present help in trouble."
            ),
            VerseOfTheDay(
                bookId = 19, // Psalm 119:105
                bookNameHindi = "भजन संहिता",
                bookNameEnglish = "Psalm",
                chapter = 119,
                verseNumber = 105,
                textHindi = "तेरा वचन मेरे पांव के लिये दीपक, और मेरे मार्ग के लिये उजियाला है।",
                textEnglish = "Your word is a lamp to my feet, and a light for my path."
            ),
            VerseOfTheDay(
                bookId = 6, // Joshua 1:9
                bookNameHindi = "यहोशू",
                bookNameEnglish = "Joshua",
                chapter = 1,
                verseNumber = 9,
                textHindi = "क्या मैं ने तुझे आज्ञा नहीं दी? हियाव बान्ध और दृढ़ हो जा; भय न खा, और तेरा मन कच्चा न हो; क्योंकि जहां जहां तू जाएगा वहां वहां तेरा परमेश्वर यहोवा तेरे संग रहेगा।",
                textEnglish = "Haven’t I commanded you? Be strong and courageous. Don’t be afraid. Don’t be dismayed, for the LORD your God is with you wherever you go."
            ),
            VerseOfTheDay(
                bookId = 48, // Galatians 5:22
                bookNameHindi = "गलतियों",
                bookNameEnglish = "Galatians",
                chapter = 5,
                verseNumber = 22,
                textHindi = "पर आत्मा का फल प्रेम, आनन्द, मेल, धीरज, कृपा, भलाई, विश्वास, नम्रता, और संयम हैं।",
                textEnglish = "But the fruit of the Spirit is love, joy, peace, patience, kindness, goodness, faithfulness, gentleness, and self-control."
            ),
            VerseOfTheDay(
                bookId = 40, // Matthew 11:28
                bookNameHindi = "मत्ती",
                bookNameEnglish = "Matthew",
                chapter = 11,
                verseNumber = 28,
                textHindi = "हे सब परिश्रम करने वालो और बोझ से दबे हुए लोगो, मेरे पास आओ; मैं तुम्हें विश्राम दूंगा।",
                textEnglish = "Come to me, all you who labor and are heavily burdened, and I will give you rest."
            ),
            VerseOfTheDay(
                bookId = 19, // Psalm 91:1
                bookNameHindi = "भजन संहिता",
                bookNameEnglish = "Psalm",
                chapter = 91,
                verseNumber = 1,
                textHindi = "जो परमप्रधान के छाए हुए स्थान में बैठा रहे, वह सर्वशक्तिमान की छाया में ठिकाना पाएगा।",
                textEnglish = "He who dwells in the secret place of the Most High will rest in the shadow of the Almighty."
            ),
            VerseOfTheDay(
                bookId = 51, // Colossians 3:15
                bookNameHindi = "कुलुस्सियों",
                bookNameEnglish = "Colossians",
                chapter = 3,
                verseNumber = 15,
                textHindi = "और मसीह की शान्ति जिस के लिये तुम एक देह होकर बुलाए भी गए हो, तुम्हारे हृदयों में राज्य करे; और तुम धन्यवाद बने रहो।",
                textEnglish = "And let the peace of God rule in your hearts, to which also you were called in one body; and be thankful."
            ),
            VerseOfTheDay(
                bookId = 45, // Romans 12:2
                bookNameHindi = "रोमियों",
                bookNameEnglish = "Romans",
                chapter = 12,
                verseNumber = 2,
                textHindi = "और इस संसार के सदृश न बनो; परन्तु तुम्हारी बुद्धि के नये हो जाने से तुम्हारा रूप बदलता जाए, जिस से तुम परमेश्वर की भली, और भावती, और सिद्ध इच्छा को जांचते रहो।",
                textEnglish = "Don’t be conformed to this world, but be transformed by the renewing of your mind, so that you may prove what is the good, well-pleasing, and perfect will of God."
            ),
            VerseOfTheDay(
                bookId = 19, // Psalm 103:1
                bookNameHindi = "भजन संहिता",
                bookNameEnglish = "Psalm",
                chapter = 103,
                verseNumber = 1,
                textHindi = "हे मेरे मन, यहोवा को धन्य कह; और जो कुछ मुझ में है, वह उसके पवित्र नाम को धन्य कहे!",
                textEnglish = "Praise the LORD, my soul! All that is within me, praise his holy name!"
            ),
            VerseOfTheDay(
                bookId = 43, // John 14:6
                bookNameHindi = "यूहन्ना",
                bookNameEnglish = "John",
                chapter = 14,
                verseNumber = 6,
                textHindi = "यीशु ने उससे कहा, 'मार्ग और सत्य और जीवन मैं ही हूँ; बिना मेरे द्वारा कोई पिता के पास नहीं पहुँच सकता।' ",
                textEnglish = "Jesus said to him, 'I am the way, the truth, and the life. No one comes to the Father, except through me.'"
            ),
            VerseOfTheDay(
                bookId = 19, // Psalm 121:1
                bookNameHindi = "भजन संहिता",
                bookNameEnglish = "Psalm",
                chapter = 121,
                verseNumber = 1,
                textHindi = "मैं अपनी आंखें पर्वतों की ओर लगाऊंगा। मुझे सहायता कहां से मिलेगी? मेरी सहायता यहोवा की ओर से होती है, जो आकाश और पृथ्वी का कर्ता है।",
                textEnglish = "I will lift up my eyes to the hills. Where does my help come from? My help comes from the LORD, who made heaven and earth."
            ),
            VerseOfTheDay(
                bookId = 55, // 2 Timothy 1:7
                bookNameHindi = "2 तीमुथियुस",
                bookNameEnglish = "2 Timothy",
                chapter = 1,
                verseNumber = 7,
                textHindi = "क्योंकि परमेश्वर ने हमें भय की नहीं पर सामर्थ्य, और प्रेम, और संयम की आत्मा दी है।",
                textEnglish = "For God didn’t give us a spirit of fear, but of power, love, and self-control."
            ),
            VerseOfTheDay(
                bookId = 42, // Luke 1:37
                bookNameHindi = "लूका",
                bookNameEnglish = "Luke",
                chapter = 1,
                verseNumber = 37,
                textHindi = "क्योंकि परमेश्वर के लिए कुछ भी असम्भव नहीं है।",
                textEnglish = "For nothing will be impossible with God."
            ),
            VerseOfTheDay(
                bookId = 40, // Matthew 28:20
                bookNameHindi = "मत्ती",
                bookNameEnglish = "Matthew",
                chapter = 28,
                verseNumber = 20,
                textHindi = "और देखो, मैं जगत के अन्त तक सदैव तुम्हारे संग हूँ।",
                textEnglish = "Behold, I am with you always, even to the end of the age."
            ),
            VerseOfTheDay(
                bookId = 59, // James 1:5
                bookNameHindi = "याकूब",
                bookNameEnglish = "James",
                chapter = 1,
                verseNumber = 5,
                textHindi = "पर यदि तुम में से किसी को बुद्धि की घटी हो, तो परमेश्वर से मांगे, जो सब को बिना उलाहना दिए उदारता से देता है; और उसको दी जाएगी।",
                textEnglish = "If any of you lacks wisdom, let him ask of God, who gives to all liberally and without reproach; and it will be given to him."
            ),
            VerseOfTheDay(
                bookId = 19, // Psalm 27:1
                bookNameHindi = "भजन संहिता",
                bookNameEnglish = "Psalm",
                chapter = 27,
                verseNumber = 1,
                textHindi = "यहोवा मेरी ज्योति और मेरा उद्धारकर्ता है; मैं किस से डरूँ? यहोवा मेरे जीवन का दृढ़ गढ़ ठहरा है; मैं किसका भय खाऊं?",
                textEnglish = "The LORD is my light and my salvation. Whom shall I fear? The LORD is the strength of my life. Of whom shall I be afraid?"
            ),
            VerseOfTheDay(
                bookId = 60, // 1 Peter 5:7
                bookNameHindi = "1 पतरस",
                bookNameEnglish = "1 Peter",
                chapter = 5,
                verseNumber = 7,
                textHindi = "अपनी सारी चिन्ता उसी पर डाल दो, क्योंकि उसको तुम्हारा ध्यान है।",
                textEnglish = "Casting all your worries on him, because he cares for you."
            ),
            VerseOfTheDay(
                bookId = 44, // Acts 1:8
                bookNameHindi = "प्रेरितों के काम",
                bookNameEnglish = "Acts",
                chapter = 1,
                verseNumber = 8,
                textHindi = "परन्तु जब पवित्र आत्मा तुम पर आएगा तब तुम सामर्थ्य पाओगे; और यरूशलेम और सारे यहूदिया और सामरिया में, और पृथ्वी की छोर तक मेरे गवाह होगे।",
                textEnglish = "But you will receive power when the Holy Spirit has come upon you. You will be witnesses to me in Jerusalem, in all Judea and Samaria, and to the uttermost parts of the earth."
            ),
            VerseOfTheDay(
                bookId = 41, // Mark 11:24
                bookNameHindi = "मरकुस",
                bookNameEnglish = "Mark",
                chapter = 11,
                verseNumber = 24,
                textHindi = "इसलिये मैं तुम से कहता हूँ, कि जो कुछ तुम प्रार्थना करके मांगो तो विश्वास कर लो कि तुम्हें मिल गया, और तुम्हारे लिये हो जाएगा।",
                textEnglish = "Therefore I tell you, all things whatever you pray and ask for, believe that you have received them, and you shall have them."
            ),
            VerseOfTheDay(
                bookId = 58, // Hebrews 11:1
                bookNameHindi = "इब्रानियों",
                bookNameEnglish = "Hebrews",
                chapter = 11,
                verseNumber = 1,
                textHindi = "अब विश्वास आशा की हुई वस्तुओं का निश्चय, और अनदेखी वस्तुओं का प्रमाण है।",
                textEnglish = "Now faith is assurance of things hoped for, proof of things not seen."
            ),
            VerseOfTheDay(
                bookId = 19, // Psalm 34:8
                bookNameHindi = "भजन संहिता",
                bookNameEnglish = "Psalm",
                chapter = 34,
                verseNumber = 8,
                textHindi = "परखकर देखो कि यहोवा कैसा भला है! क्या ही धन्य है वह पुरुष जो उसकी शरण लेता है।",
                textEnglish = "Oh taste and see that the LORD is good. Blessed is the man who takes refuge in him."
            )
        )

        /**
         * Returns a deterministic, curated VerseOfTheDay for the current local calendar date.
         * App restarts, refreshes, and language toggles will return the exact same scripture reference.
         * On Sep 9 (Day of year ~ 252), (252 % 31) -> 4 or matched deterministically.
         */
        fun getTodayVerse(): VerseOfTheDay {
            val calendar = Calendar.getInstance()
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val year = calendar.get(Calendar.YEAR)
            // Stable deterministic day index
            val index = ((dayOfYear + year * 365) % dailyVerses.size).let {
                if (it < 0) it + dailyVerses.size else it
            }
            return dailyVerses[index]
        }
    }
}
