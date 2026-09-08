package com.example.data.bible.model

enum class BibleFontSize(val title: String, val sp: Float) {
    SMALL("Small (छोटा)", 15f),
    NORMAL("Normal (सामान्य)", 18f),
    LARGE("Large (बड़ा)", 21f),
    EXTRA_LARGE("Extra Large (बहुत बड़ा)", 25f)
}

enum class BibleLineSpacing(val title: String, val multiplier: Float) {
    COMPACT("Compact (सघन)", 1.25f),
    NORMAL("Normal (सामान्य)", 1.45f),
    COMFORTABLE("Comfortable (खुला)", 1.70f)
}

enum class BibleTheme(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light (उजला)"),
    DARK("Dark (गहरा)"),
    SEPIA("Sepia / Warm (सहज)")
}

data class BibleReadingSettings(
    val fontSize: BibleFontSize = BibleFontSize.NORMAL,
    val lineSpacing: BibleLineSpacing = BibleLineSpacing.NORMAL,
    val showVerseNumbers: Boolean = true,
    val theme: BibleTheme = BibleTheme.SYSTEM,
    val rememberLastReadingPosition: Boolean = true,
    val selectedTranslationId: String = BibleTranslation.HINDI_IRV.id
)
