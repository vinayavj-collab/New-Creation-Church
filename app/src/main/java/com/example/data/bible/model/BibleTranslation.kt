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

        val ENGLISH_WEB = BibleTranslation(
            id = "ENG_WEB",
            nameHindi = "अंग्रेज़ी (WEB)",
            nameEnglish = "English (World English Bible)",
            language = "en",
            license = "Public Domain",
            attribution = "World English Bible - Free of copyright restrictions",
            isPublicDomain = true,
            isOfflineAvailable = true
        )

        val ALL = listOf(HINDI_IRV, ENGLISH_WEB)
    }
}
