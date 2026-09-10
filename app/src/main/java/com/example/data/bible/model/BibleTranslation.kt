package com.example.data.bible.model

enum class Testament(val displayNameHindi: String, val displayNameEnglish: String) {
    OLD("पुराना नियम", "Old Testament"),
    NEW("नया नियम", "New Testament")
}

data class BibleTranslation(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String,
    val language: String,
    val license: String,
    val attribution: String,
    val isPublicDomain: Boolean,
    val isOfflineAvailable: Boolean
) {
    companion object {
        val HINDI_IRV = BibleTranslation(
            id = "HIN_IRV",
            nameHindi = "हिन्दी (IRV सरल भाषा)",
            nameEnglish = "Hindi (Indian Revised Version)",
            language = "hi",
            license = "Creative Commons CC BY-SA 4.0",
            attribution = "Bridge Connectivity Solutions / Free Bibles India",
            isPublicDomain = false,
            isOfflineAvailable = true
        )

        val HINDI_BSI_OV = BibleTranslation(
            id = "HIN_BSI_OV",
            nameHindi = "हिन्दी (BSI पवित्र बाइबिल - OV)",
            nameEnglish = "Hindi BSI (Old Version)",
            language = "hi",
            license = "Public Domain (Historical Text)",
            attribution = "Bible Society of India / Public Domain",
            isPublicDomain = true,
            isOfflineAvailable = false
        )

        val HINDI_ERV = BibleTranslation(
            id = "HIN_ERV",
            nameHindi = "हिन्दी (ERV सरल हिन्दी बाइबिल)",
            nameEnglish = "Hindi ERV (Easy-to-Read Version)",
            language = "hi",
            license = "World Bible Translation Center",
            attribution = "WBTC India",
            isPublicDomain = false,
            isOfflineAvailable = false
        )

        val HINDI_ULB = BibleTranslation(
            id = "HIN_ULB",
            nameHindi = "हिन्दी (ULB मूलनिष्ठ अनुवाद)",
            nameEnglish = "Hindi ULB (Unlocked Literal Bible)",
            language = "hi",
            license = "Creative Commons CC BY-SA 4.0",
            attribution = "Door43 World Missions Community",
            isPublicDomain = false,
            isOfflineAvailable = false
        )

        val ENGLISH_KJV = BibleTranslation(
            id = "ENG_KJV",
            nameHindi = "अंग्रेज़ी (KJV)",
            nameEnglish = "English (King James Version)",
            language = "en",
            license = "Public Domain",
            attribution = "Public Domain English Bible",
            isPublicDomain = true,
            isOfflineAvailable = true
        )

        val ENGLISH_WEB = BibleTranslation(
            id = "ENG_WEB",
            nameHindi = "अंग्रेज़ी (WEB Modern)",
            nameEnglish = "English (World English Bible)",
            language = "en",
            license = "Public Domain",
            attribution = "World English Bible (ebible.org)",
            isPublicDomain = true,
            isOfflineAvailable = false
        )

        val PARALLEL_HI_EN = BibleTranslation(
            id = "PARALLEL_HI_EN",
            nameHindi = "एक साथ (हिन्दी + English)",
            nameEnglish = "Parallel (Hindi + English)",
            language = "hi-en",
            license = "Dual Translation",
            attribution = "Hindi IRV + English KJV Verse by Verse",
            isPublicDomain = false,
            isOfflineAvailable = true
        )

        val ALL = listOf(HINDI_IRV, HINDI_BSI_OV, HINDI_ERV, HINDI_ULB, ENGLISH_KJV, ENGLISH_WEB, PARALLEL_HI_EN)
    }
}
