package com.example.data.bible.model

import androidx.annotation.Keep
import java.util.Calendar

@Keep
data class BibleQuizItem(
    val id: String = "",
    val dayOfYear: Int = 1,
    val questionHindi: String = "",
    val optionsHindi: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanationHindi: String = "",
    val reference: String = "",
    val category: String = "बाइबल क्विज़ (Bible Quiz)"
) {
    companion object {
        fun getOfflineQuizzes(): List<BibleQuizItem> {
            return listOf(
                BibleQuizItem(
                    id = "quiz_1",
                    dayOfYear = 1,
                    questionHindi = "बाइबल की सबसे पहली पुस्तक कौन सी है?",
                    optionsHindi = listOf("निर्गमन (Exodus)", "उत्पत्ति (Genesis)", "लैव्यवस्था (Leviticus)", "गिनती (Numbers)"),
                    correctIndex = 1,
                    explanationHindi = "उत्पत्ति (Genesis) बाइबल की पहली पुस्तक है, जिसमें सृष्टि की रचना और पूर्वजों का इतिहास दर्ज है।",
                    reference = "उत्पत्ति 1:1"
                ),
                BibleQuizItem(
                    id = "quiz_2",
                    dayOfYear = 2,
                    questionHindi = "प्रभु यीशु मसीह का जन्म किस नगर में हुआ था?",
                    optionsHindi = listOf("नासरत (Nazareth)", "यिरूशलेम (Jerusalem)", "बेतुलेहम (Bethlehem)", "अन्तियाकिया (Antioch)"),
                    correctIndex = 2,
                    explanationHindi = "मीका 5:2 की भविष्यवाणी के अनुसार प्रभु यीशु का जन्म यहूदा के बेतलेहेम में हुआ था।",
                    reference = "मीका 5:2 / मत्ती 2:1"
                ),
                BibleQuizItem(
                    id = "quiz_3",
                    dayOfYear = 3,
                    questionHindi = "पुराने नियम में कुल कितनी पुस्तकें हैं?",
                    optionsHindi = listOf("27", "39", "66", "12"),
                    correctIndex = 1,
                    explanationHindi = "पुराने नियम में 39 पुस्तकें हैं और नए नियम में 27 पुस्तकें हैं, कुल 66 पुस्तकें हैं।",
                    reference = "ऐतिहासिक बाइबल कैनन"
                ),
                BibleQuizItem(
                    id = "quiz_4",
                    dayOfYear = 4,
                    questionHindi = "किस भविष्यवक्ता को आग के रथ पर जीवित स्वर्ग उठा लिया गया था?",
                    optionsHindi = listOf("मूसा (Moses)", "एलिजा (Elijah - एलिय्याह)", "अहमिया (Nehemiah)", "यूहन्ना (John)"),
                    correctIndex = 1,
                    explanationHindi = "2 राजाओं 2:11 के अनुसार एलिय्याह (Elijah) को आंधी में स्वर्ग पर उठा लिया गया था।",
                    reference = "2 राजाओं 2:11"
                ),
                BibleQuizItem(
                    id = "quiz_5",
                    dayOfYear = 5,
                    questionHindi = "प्रेरितों के काम अध्याय 2 में पवित्र आत्मा किस पर्व के दिन चेलों पर उतरा था?",
                    optionsHindi = listOf("फसह का पर्व (Passover)", "पिन्तेकुस्त का पर्व (Pentecost)", "झोपड़ियों का पर्व (Tabernacles)", "महापाप प्रायश्चित दिन"),
                    correctIndex = 1,
                    explanationHindi = "पिन्तेकुस्त (Pentecost) के दिन पवित्र आत्मा की सामर्थ्य आग की जीभों के समान चेलों पर उतरी।",
                    reference = "प्रेरितों के काम 2:1-4"
                ),
                BibleQuizItem(
                    id = "quiz_6",
                    dayOfYear = 6,
                    questionHindi = "बाइबल में सबसे छोटा पद (Verse) कौन सा है?",
                    optionsHindi = listOf("आनंदित रहो", "यीशु रोया", "प्रार्थना करो", "धन्यवाद दो"),
                    correctIndex = 1,
                    explanationHindi = "यूहन्ना 11:35 'यीशु रोया' (Jesus wept) बाइबल का सबसे छोटा पद है।",
                    reference = "यूहन्ना 11:35"
                ),
                BibleQuizItem(
                    id = "quiz_7",
                    dayOfYear = 7,
                    questionHindi = "नूह ने जलप्रलय से बचने के लिए किस लकड़ी से जहाज (Ark) बनाया था?",
                    optionsHindi = listOf("देवदार (Cedar)", "गोफेर (Gopher)", "जैतून (Olive)", "शीशम (Rosewood)"),
                    correctIndex = 1,
                    explanationHindi = "परमेश्वर ने नूह को गोफेर लकड़ी से जहाज बनाने की आज्ञा दी थी।",
                    reference = "उत्पत्ति 6:14"
                ),
                BibleQuizItem(
                    id = "quiz_8",
                    dayOfYear = 8,
                    questionHindi = "दाऊद ने किस दैत्य (Giant) को गोफन और पत्थर से हराया था?",
                    optionsHindi = listOf("गोलियथ (Goliath)", "सूल (Saul)", "नूकैन", "अम्लेक"),
                    correctIndex = 0,
                    explanationHindi = "दाऊद ने विश्वास के साथ गोफन से एक चिकना पत्थर चलाकर पलिشتی योद्धा गोलियथ को मार गिराया था।",
                    reference = "1 शमूएल 17:50"
                ),
                BibleQuizItem(
                    id = "quiz_9",
                    dayOfYear = 9,
                    questionHindi = "नया नियम मूल रूप से किस भाषा में लिखा गया था?",
                    optionsHindi = listOf("इब्रानी (Hebrew)", "लैटिन (Latin)", "कोइने यूनानी (Koine Greek)", "अरामी (Aramaic)"),
                    correctIndex = 2,
                    explanationHindi = "नया नियम मूल रूप से प्राचीन ग्रीक (Koine Greek) भाषा में लिखा गया था।",
                    reference = "नया नियम मूल पांडुलिपियां"
                ),
                BibleQuizItem(
                    id = "quiz_10",
                    dayOfYear = 10,
                    questionHindi = "परमेश्वर ने मूसा को सीनै पर्वत पर कौन सी दो वस्तुएं दी थीं?",
                    optionsHindi = listOf("सोने का मुकुट", "दस आज्ञाओं की पत्थर की टिकिया (Tablets)", "मन्दिर की कुंजी", "भविष्यवाणी की पुस्तक"),
                    correctIndex = 1,
                    explanationHindi = "परमेश्वर ने मूसा को अपनी उंगली से लिखी हुई दस आज्ञाओं की दो पत्थर की टिकिया दी थीं।",
                    reference = "निर्गमन 31:18"
                )
            )
        }

        fun getQuizForToday(): BibleQuizItem {
            val list = getOfflineQuizzes()
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val index = (dayOfYear - 1) % list.size
            return list[index]
        }
    }
}
