package com.example.data.prayer.model

data class DailyPrayerVerse(
    val id: Int,
    val dayNumber: Int,
    val themeTitleHindi: String,
    val themeTitleEnglish: String,
    val category: String,
    val verseBookId: Int,
    val verseChapter: Int,
    val verseNumber: Int,
    val verseReferenceHindi: String,
    val verseReferenceEnglish: String,
    val verseTextHindi: String,
    val verseTextEnglish: String,
    val prayerTitleHindi: String,
    val prayerTitleEnglish: String,
    val prayerHindi: String,
    val prayerEnglish: String,
    val declarationHindi: String,
    val declarationEnglish: String,
    val reflectionPromptHindi: String,
    val reflectionPromptEnglish: String
)
