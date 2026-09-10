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

        val ALL = listOf(HINDI_IRV, ENGLISH_KJV, PARALLEL_HI_EN)
    }
}
