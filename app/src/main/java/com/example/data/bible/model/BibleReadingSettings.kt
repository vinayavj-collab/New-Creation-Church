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
    SEPIA("Sepia / Warm (सहज)"),
    AMOLED("OLED Black (शुद्ध काला)")
}

enum class ScreenTimeoutSetting(val titleHindi: String, val titleEnglish: String, val minutes: Int) {
    SYSTEM("सिस्टम डिफ़ॉल्ट", "System Default", 0),
    ONE_MIN("1 मिनट", "1 Minute", 1),
    TWO_MIN("2 मिनट", "2 Minutes", 2),
    FIVE_MIN("5 मिनट", "5 Minutes", 5),
    TEN_MIN("10 मिनट", "10 Minutes", 10),
    FIFTEEN_MIN("15 मिनट", "15 Minutes", 15),
    THIRTY_MIN("30 मिनट", "30 Minutes", 30),
    ALWAYS_ON("हमेशा चालू (Always On)", "Always On", -1)
}

data class BibleReadingSettings(
    val fontSize: BibleFontSize = BibleFontSize.NORMAL,
    val lineSpacing: BibleLineSpacing = BibleLineSpacing.NORMAL,
    val showVerseNumbers: Boolean = true,
    val showSubheadings: Boolean = true,
    val showFavoritesHint: Boolean = true,
    val showBookmarkHint: Boolean = true,
    val showHighlights: Boolean = true,
    val showParagraphAndIndents: Boolean = true,
    val showJesusWordsInRed: Boolean = true,
    val jesusWordsColorHex: String = "#DC2626", // Red, Crimson, Maroon, Amber, Gold
    val justifyBibleText: Boolean = false,
    val suggestVerseSelection: Boolean = true,
    val showNoteHint: Boolean = true,
    val showAudioPlayer: Boolean = true,
    val noteFormatHtml: Boolean = true,
    val screenTimeoutMinutes: Int = 0, // 0 = system, -1 = keep screen on, >0 = minutes
    val theme: BibleTheme = BibleTheme.SYSTEM,
    val rememberLastReadingPosition: Boolean = true,
    val selectedTranslationId: String = BibleTranslation.HINDI_IRV.id,
    val useSerifFont: Boolean = false,
    val originalFormatMode: Boolean = false
)
