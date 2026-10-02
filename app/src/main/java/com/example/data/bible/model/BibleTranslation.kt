package com.example.data.bible.model

data class BibleTranslation(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String
) {
    val language: String get() = if (id.startsWith("HIN") || id == "HIOV") "HINDI" else "ENGLISH"
    val shortName: String get() = id

    companion object {
        val HIOV = BibleTranslation("HIOV", "हिन्दी ओल्ड वर्शन (HIOV)", "Hindi Old Version")
        val HINDI_IRV = BibleTranslation("HIOV", "हिन्दी ओल्ड वर्शन (HIOV)", "Hindi Old Version")
        val ENGLISH_NKJV = BibleTranslation("NKJV", "अंग्रेज़ी NKJV", "New King James Version")
        val ENGLISH_KJ21 = BibleTranslation("KJ21", "21st Century King James", "21st Century King James")
        val KJV = BibleTranslation("KJV", "King James Version (KJV)", "King James Version")
        val PARALLEL_HI_EN = BibleTranslation("PARALLEL", "समानांतर (हिन्दी + अंग्रेज़ी)", "Parallel (Hindi + English)")

        val ALL = listOf(HIOV, ENGLISH_NKJV, ENGLISH_KJ21, KJV, PARALLEL_HI_EN)
    }
}
