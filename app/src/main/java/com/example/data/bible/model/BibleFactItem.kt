package com.example.data.bible.model

import androidx.annotation.Keep
import java.util.Calendar

@Keep
data class BibleFactItem(
    val id: String = "",
    val dayOfYear: Int = 1,
    val factHindi: String = "",
    val factEnglish: String = "",
    val source: String = "",
    val category: String = "बाइबल तथ्य (Bible Fact)"
) {
    companion object {
        fun getOfflineFacts(): List<BibleFactItem> {
            return listOf(
                BibleFactItem(
                    id = "fact_1",
                    dayOfYear = 1,
                    factHindi = "बाइबल इतिहास में दुनिया में सबसे अधिक बिकने, छपने और पढ़ी जाने वाली पुस्तक है, जिसकी अरबों प्रतियां वितरित हो चुकी हैं।",
                    factEnglish = "The Bible is the single best-selling and most widely distributed book in human history.",
                    source = "गिनीज बुक ऑफ़ वर्ल्ड रिकॉर्ड्स (Guinness World Records)",
                    category = "इतिहास व प्रकाशन",
                ),
                BibleFactItem(
                    id = "fact_2",
                    dayOfYear = 2,
                    factHindi = "पवित्र बाइबल मूल रूप से तीन अलग-अलग प्राचीन भाषाओं में लिखी गई थी: इब्रानी (Hebrew), अरामी (Aramaic), और यूनानी (Greek)।",
                    factEnglish = "The Bible was originally written in three ancient languages: Hebrew, Aramaic, and Greek.",
                    source = "बाइबल भाषा संस्थान (Biblical Languages Institute)",
                    category = "मूल भाषाएँ",
                ),
                BibleFactItem(
                    id = "fact_3",
                    dayOfYear = 3,
                    factHindi = "बाइबल लगभग 40 विभिन्न लेखकों (जैसे राजाओं, भविष्यद्वक्ताओं, मछुआरों, चिकित्सकों और विद्वानों) द्वारा लगभग 1,500 वर्षों के कालखंड में लिखी गई थी।",
                    factEnglish = "The Bible was written by approximately 40 different human authors over a span of about 1,500 years.",
                    source = "क्रिश्चियन थियोलॉजिकल स्टडी बाइबल (Christian Theological Study Bible)",
                    category = "लेखन इतिहास",
                ),
                BibleFactItem(
                    id = "fact_4",
                    dayOfYear = 4,
                    factHindi = "पुराने नियम में 39 पुस्तकें और नए नियम में 27 पुस्तकें हैं, जिससे कुल मिलाकर पवित्र बाइबल में 66 पुस्तकें हैं।",
                    factEnglish = "There are 39 books in the Old Testament and 27 books in the New Testament, totaling 66 books in the Bible.",
                    source = "ऐतिहासिक बाइबल कैनन (Historical Bible Canon)",
                    category = "संरचना",
                ),
                BibleFactItem(
                    id = "fact_5",
                    dayOfYear = 5,
                    factHindi = "बाइबल का सबसे लंबा अध्याय भजन संहिता (Psalms) 119 है, जिसमें 176 आयतें हैं जो हिब्रू वर्णमाला के अनुसार विभाजित हैं।",
                    factEnglish = "The longest chapter in the Bible is Psalm 119, containing 176 verses structured around the Hebrew alphabet.",
                    source = "पवित्र बाइबल निर्देशिका (Holy Bible Directory)",
                    category = "अध्याय और आयतें",
                ),
                BibleFactItem(
                    id = "fact_6",
                    dayOfYear = 6,
                    factHindi = "बाइबल का सबसे छोटा अध्याय भजन संहिता 117 है, और बाइबल का बिल्कुल मध्य भाग भजन संहिता 118:8 है।",
                    factEnglish = "Psalm 117 is the shortest chapter in the Bible, and Psalm 118:8 is traditionally considered the exact center verse.",
                    source = "बाइबल सांख्यिकी अध्ययन (Bible Statistics Study)",
                    category = "तथ्य व आंकड़े",
                ),
                BibleFactItem(
                    id = "fact_7",
                    dayOfYear = 7,
                    factHindi = "यशायाह (Isaiah) की पुस्तक को 'लघु बाइबल' भी कहा जाता है क्योंकि इसमें पुराने नियम की तरह ही 39+27 = 66 अध्याय हैं।",
                    factEnglish = "The Book of Isaiah is often called the 'Bible in miniature' because it has 66 chapters, mirroring the 66 books of the Bible.",
                    source = "बाइबल इंट्रोडक्शन एंड आउटलाइन (Bible Introduction & Outline)",
                    category = "भविष्यवाणी",
                ),
                BibleFactItem(
                    id = "fact_8",
                    dayOfYear = 8,
                    factHindi = "प्राचीन काल में बाइबल के ग्रंथ चर्मपत्रों (Parchments) और पपाइरस (Papyrus) के स्क्रॉल पर हाथ से सावधानीपूर्वक कॉपी किए जाते थे।",
                    factEnglish = "In ancient times, biblical texts were meticulously hand-copied onto animal skin parchments and papyrus scrolls.",
                    source = "कुमरान डेड सी स्क्रॉल इतिहास (Qumran Dead Sea Scroll History)",
                    category = "पुरातत्व",
                ),
                BibleFactItem(
                    id = "fact_9",
                    dayOfYear = 9,
                    factHindi = "इब्रानी भाषा में 'अलेलूहिया' (Hallelujah) शब्द का शाब्दिक अर्थ 'यहोवा की स्तुति हो' होता है, जो विश्व की कई भाषाओं में समान रूप से प्रयुक्त होता है।",
                    factEnglish = "The word 'Hallelujah' is a Hebrew term meaning 'Praise Yahweh (The Lord),' used globally in worship.",
                    source = "हिब्रू बाइबल लेक्सिकॉन (Hebrew Bible Lexicon)",
                    category = "धार्मिक शब्द",
                ),
                BibleFactItem(
                    id = "fact_10",
                    dayOfYear = 10,
                    factHindi = "प्रेरित पौलुस (Apostle Paul) ने नए नियम में कुल 13 पत्र (रोमियों से फिलेमोन तक) लिखे, जिन्होंने प्रारंभिक कलीसिया के विश्वास को दृढ़ किया।",
                    factEnglish = "Apostle Paul wrote 13 epistles in the New Testament, laying foundational theology for the early Christian church.",
                    source = "न्यू टेस्टामेंट हिस्टोरिकल सर्वे (New Testament Historical Survey)",
                    category = "नए नियम के पत्र",
                ),
            )
        }

        fun getFactForToday(): BibleFactItem {
            val list = getOfflineFacts()
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val index = (dayOfYear - 1) % list.size
            return list[index]
        }
    }
}
