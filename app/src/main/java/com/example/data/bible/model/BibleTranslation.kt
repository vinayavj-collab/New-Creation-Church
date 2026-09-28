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
        val HIOV = BibleTranslation(
            id = "HIOV",
            nameHindi = "हिन्दी (HIOV ओल्ड वर्शन)",
            nameEnglish = "Hindi (HIOV Old Version)",
            language = "hi",
            license = "Public Domain",
            attribution = "Hindi Old Version (HIOV)",
            isPublicDomain = true,
            isOfflineAvailable = true
        )

        val ENGLISH_NKJV = BibleTranslation(
            id = "NKJV",
            nameHindi = "अंग्रेज़ी (NKJV)",
            nameEnglish = "English (New King James Version)",
            language = "en",
            license = "New King James Version",
            attribution = "New King James Version (NKJV)",
            isPublicDomain = false,
            isOfflineAvailable = true
        )

        val ENGLISH_ESV = ENGLISH_NKJV

        val PARALLEL_HI_EN = BibleTranslation(
            id = "PARALLEL_HI_EN",
            nameHindi = "द्विभाषी (हिन्दी HIOV + English NKJV)",
            nameEnglish = "Bilingual (Hindi HIOV + English NKJV)",
            language = "hi-en",
            license = "Dual Translation",
            attribution = "Hindi HIOV + English NKJV Verse by Verse",
            isPublicDomain = false,
            isOfflineAvailable = true
        )

        // Compatibility aliases pointing directly to HIOV and NKJV
        val HINDI_IRV = HIOV
        val HIND = HIOV
        val HBSI = HIOV
        val HIULB = HIOV
        val HINDI_BSI_OV = HIOV
        val HINDI_ULB = HIOV
        val ENGLISH_KJV = ENGLISH_NKJV

        val HINDI_TRANSLATIONS = listOf(HIOV)
        val ENGLISH_TRANSLATIONS = listOf(ENGLISH_NKJV)

        val ALL = listOf(HIOV, ENGLISH_NKJV, PARALLEL_HI_EN)

        fun fromId(id: String): BibleTranslation {
            return ALL.firstOrNull { 
                it.id.equals(id, ignoreCase = true)
            } ?: when {
                id.startsWith("ENG", ignoreCase = true) || id.equals("NKJV", ignoreCase = true) || id.equals("ESV", ignoreCase = true) || id.equals("KJV", ignoreCase = true) -> ENGLISH_NKJV
                id.equals("PARALLEL_HI_EN", ignoreCase = true) -> PARALLEL_HI_EN
                else -> HIOV
            }
        }
    }
}
