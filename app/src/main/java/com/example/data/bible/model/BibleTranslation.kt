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

        val ENGLISH_ESV = BibleTranslation(
            id = "ESV",
            nameHindi = "अंग्रेज़ी (ESV)",
            nameEnglish = "English (English Standard Version)",
            language = "en",
            license = "Standard Version",
            attribution = "English Standard Version (ESV)",
            isPublicDomain = true,
            isOfflineAvailable = true
        )

        val PARALLEL_HI_EN = BibleTranslation(
            id = "PARALLEL_HI_EN",
            nameHindi = "द्विभाषी (हिन्दी HIOV + English ESV)",
            nameEnglish = "Bilingual (Hindi HIOV + English ESV)",
            language = "hi-en",
            license = "Dual Translation",
            attribution = "Hindi HIOV + English ESV Verse by Verse",
            isPublicDomain = false,
            isOfflineAvailable = true
        )

        // Compatibility aliases pointing directly to HIOV and ESV
        val HINDI_IRV = HIOV
        val HIND = HIOV
        val HBSI = HIOV
        val HIULB = HIOV
        val HINDI_BSI_OV = HIOV
        val HINDI_ULB = HIOV
        val ENGLISH_KJV = ENGLISH_ESV

        val HINDI_TRANSLATIONS = listOf(HIOV)
        val ENGLISH_TRANSLATIONS = listOf(ENGLISH_ESV)

        val ALL = listOf(HIOV, ENGLISH_ESV, PARALLEL_HI_EN)

        fun fromId(id: String): BibleTranslation {
            return ALL.firstOrNull { 
                it.id.equals(id, ignoreCase = true)
            } ?: when {
                id.startsWith("ENG", ignoreCase = true) || id.equals("ESV", ignoreCase = true) || id.equals("KJV", ignoreCase = true) -> ENGLISH_ESV
                id.equals("PARALLEL_HI_EN", ignoreCase = true) -> PARALLEL_HI_EN
                else -> HIOV
            }
        }
    }
}
