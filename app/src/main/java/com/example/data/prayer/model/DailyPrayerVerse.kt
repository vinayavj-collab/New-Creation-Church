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
    val reflectionPromptEnglish: String,
    val topic: String = "",
    val sermonNotes: String = "",
    val announcements: String = "",
    val prayerPoints: List<String> = emptyList(),
    val customNamesList: List<String> = emptyList(),
    val isCustomized: Boolean = false
) {
    // Computed property to return effective topic
    val displayTopic: String
        get() = topic.ifBlank { themeTitleHindi }

    // Computed property returning combined prayer points and customized names
    val allIntercessionNames: List<String>
        get() {
            val list = mutableListOf<String>()
            list.addAll(prayerPoints)
            customNamesList.forEach { name ->
                if (name.isNotBlank() && !list.contains(name)) {
                    list.add(name)
                }
            }
            return list
        }
}

