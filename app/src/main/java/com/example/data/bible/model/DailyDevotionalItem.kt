package com.example.data.bible.model

import androidx.annotation.Keep
import java.util.Calendar

@Keep
data class DailyDevotionalItem(
    val id: String = "",
    val dayOfYear: Int = 1,
    val titleHindi: String = "",
    val verseReference: String = "",
    val verseTextHindi: String = "",
    val devotionalThoughtHindi: String = "",
    val author: String = "पास्टर / पास्टरल टीम",
    val category: String = "दैनिक मनन (Daily Devotional)"
) {
    companion object {
        fun getOfflineDevotionals(): List<DailyDevotionalItem> {
            return listOf(
                DailyDevotionalItem(
                    id = "dev_1",
                    dayOfYear = 1,
                    titleHindi = "परमेश्वर की अचूक शांति",
                    verseReference = "फिलिपियों 4:6-7",
                    verseTextHindi = "किसी भी बात की चिंता मत करो, पर हर एक बात में तुम्हारे निवेदन, प्रार्थना और धन्यवाद के साथ परमेश्वर के सामने प्रस्तुत किए जाएं। तब परमेश्वर की शांति, जो समझ से बिल्कुल परे है, तुम्हारे हृदय और तुम्हारे विचारों को मसीह यीशु में सुरक्षित रखेगी।",
                    devotionalThoughtHindi = "चिंता हमारे भविष्य की कठिनाइयों को दूर नहीं करती, बल्कि आज की शक्ति को छीन लेती है। जब हम अपनी हर चिंता को प्रार्थना में प्रभु को सौंप देते हैं, तो उसकी स्वर्गीय शांति हमारे मन को भर देती है।",
                    author = "संपादकीय टीम",
                    category = "दैनिक मनन",
                ),
                DailyDevotionalItem(
                    id = "dev_2",
                    dayOfYear = 2,
                    titleHindi = "विश्वास के साथ चलना",
                    verseReference = "2 कुरिन्थियों 5:7",
                    verseTextHindi = "क्योंकि हम रूप को देखकर नहीं, परंतु विश्वास से चलते हैं।",
                    devotionalThoughtHindi = "ईश्वर पर भरोसा रखना तब आसान होता है जब सब कुछ ठीक हो, लेकिन असली विश्वास तब चमकता है जब रास्ता धुंधला और अनिश्चित हो। आज अपनी आंखें परिस्थितियों पर नहीं, बल्कि सर्वशक्तिमान परमेश्वर पर टिकाए रखें।",
                    author = "संपादकीय टीम",
                    category = "दैनिक मनन",
                ),
                DailyDevotionalItem(
                    id = "dev_3",
                    dayOfYear = 3,
                    titleHindi = "नया दिन, नई करुणा",
                    verseReference = "विलापगीत 3:22-23",
                    verseTextHindi = "यह यहोवा की करुणा ही है कि हम विनाशी नहीं हुए, क्योंकि उसकी दया कभी समाप्त नहीं होती; वह हर भोर को नई होती है; तेरी सच्चाई महान है।",
                    devotionalThoughtHindi = "बीता हुआ कल चाहे कितना भी कठिन क्यों न रहा हो, आज की सुबह परमेश्वर की नई करुणा और क्षमा लेकर आती है। अपनी पुरानी विफलताओं को भूलकर आज की नई आशीषों को ग्रहण करें।",
                    author = "संपादकीय टीम",
                    category = "दैनिक मनन",
                ),
                DailyDevotionalItem(
                    id = "dev_4",
                    dayOfYear = 4,
                    titleHindi = "यहोवा हमारा बल है",
                    verseReference = "भजन संहिता 46:1",
                    verseTextHindi = "परमेश्वर हमारा शरणस्थान और बल है, संकट में अति प्रवीन सहायता है।",
                    devotionalThoughtHindi = "जब जीवन के तूफान हमें चारों ओर से घेर लें, तब यह न भूलें कि हमारा परमेश्वर संकट के समय सबसे तेज और मजबूत सहायता है। उस पर भरोसा रखें और भयभीत न हों।",
                    author = "संपादकीय टीम",
                    category = "दैनिक मनन",
                ),
                DailyDevotionalItem(
                    id = "dev_5",
                    dayOfYear = 5,
                    titleHindi = "प्रेम में बढ़ना",
                    verseReference = "1 कुरिन्थियों 16:14",
                    verseTextHindi = "तुम्हारे सब काम प्रेम के साथ किए जाएं।",
                    devotionalThoughtHindi = "मसीह का सबसे बड़ा आदेश एक-दूसरे से प्रेम रखना है। हमारे शब्द, हमारे कार्य और हमारे विचार प्रेम से परिपूर्ण होने चाहिए ताकि संसार जान सके कि हम उसके चेले हैं।",
                    author = "संपादकीय टीम",
                    category = "दैनिक मनन",
                ),
            )
        }

        fun getDevotionalForToday(): DailyDevotionalItem {
            val list = getOfflineDevotionals()
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val index = (dayOfYear - 1) % list.size
            return list[index]
        }
    }
}
