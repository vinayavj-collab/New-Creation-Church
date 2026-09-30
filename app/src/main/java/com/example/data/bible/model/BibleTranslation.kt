package com.example.data.bible.model

data class BibleTranslation(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String
) {
    companion object {
        val HIOV = BibleTranslation("HIOV", "हिन्दी ओल्ड वर्शन (HIOV)", "Hindi Old Version")
        val ENGLISH_NKJV = BibleTranslation("NKJV", "अंग्रेज़ी NKJV", "New King James Version")
        val PARALLEL_HI_EN = BibleTranslation("PARALLEL", "समानांतर (हिन्दी + अंग्रेज़ी)", "Parallel (Hindi + English)")

        val ALL = listOf(HIOV, ENGLISH_NKJV, PARALLEL_HI_EN)
    }
}
