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

enum class BibleTheme(val titleHindi: String, val titleEnglish: String) {
    SYSTEM("सिस्टम डिफ़ॉल्ट (Dynamic)", "System Dynamic"),
    LIGHT("उजला दिन (Day Light)", "Day Light"),
    PAPER("विंटेज पेपर (Vintage Paper)", "Vintage Paper"),
    WOOD("काष्ठ रंग (Warm Wood)", "Warm Wood"),
    EYE_PROTECTION("आँखों की सुरक्षा (Eye Care)", "Eye Care"),
    SEPIA("सेपिया क्लासिक (Classic Sepia)", "Classic Sepia"),
    NIGHT("रात्रि चारकोल (Night Charcoal)", "Night Charcoal"),
    DARK("डार्क स्लेट (Dark Slate)", "Dark Slate"),
    AMOLED("गहरा काला (OLED Pitch Black)", "OLED Black"),
    EMERALD("शांत हरा (Sage Emerald)", "Sage Emerald")
}

enum class BibleFontFamilyType(val titleHindi: String, val titleEnglish: String) {
    SYSTEM_DEFAULT("सिस्टम (System Sans)", "System Sans"),
    CLASSIC_SERIF("क्लासिक सेरिफ़ (Classic Serif)", "Classic Serif"),
    MODERN_POPPINS("मॉडर्न क्लीन (Modern Clean)", "Modern Clean"),
    ELEGANT_ROZHA("सुंदर राजसी (Elegant Rozha)", "Elegant Royal"),
    TRADITIONAL_BOOK("पारंपरिक पोथी (Traditional Book)", "Traditional Bookman"),
    MONOSPACE_STUDY("स्टडी टाइपराइटर (Monospace)", "Monospace Study")
}

enum class DualViewMode(val titleHindi: String, val titleEnglish: String) {
    INTERLEAVED("वचन-दर-वचन (Verse by Verse)", "Verse-by-Verse (Hindi + English)"),
    SIDE_BY_SIDE("समानांतर अगल-बगल (Side by Side)", "Side-by-Side Columns")
}

enum class ScreenTimeoutSetting(val titleHindi: String, val titleEnglish: String, val minutes: Int) {
    SYSTEM("सिस्टम डिफ़ॉल्ट (System Default)", "System Default", 0),
    TWO_MIN("2 मिनट (2 Min)", "2 Minutes", 2),
    FIVE_MIN("5 मिनट (5 Min)", "5 Minutes", 5),
    TEN_MIN("10 मिनट (10 Min)", "10 Minutes", 10),
    FIFTEEN_MIN("15 मिनट (15 Min)", "15 Minutes", 15),
    THIRTY_MIN("30 मिनट (30 Min)", "30 Minutes", 30),
    ALWAYS_ON("हमेशा स्क्रीन चालू रखें (Always On)", "Always On", -1)
}

enum class TranslationToggleBehavior(val titleHindi: String, val titleEnglish: String) {
    SINGLE_TAP_SHOW_ALL("सिंगल टैप पर सूची दिखाएं", "Single Tap Shows All Translations"),
    CYCLE_ON_TAP_LONG_PRESS_SHOW_ALL("सिंगल टैप पर बदलो, लॉन्ग टैप पर सूची", "Single Tap Cycles, Long Press Shows List")
}

enum class VerseTapSelectionMode(val titleHindi: String, val titleEnglish: String) {
    ENTIRE_VERSE("पूरा वचन टैप करें (Entire Verse)", "Tap Anywhere on Verse"),
    VERSE_NUMBER_ONLY("केवल वचन संख्या टैप करें (Verse Number Only)", "Tap Verse Number Only")
}

data class BibleReadingSettings(
    val fontSize: BibleFontSize = BibleFontSize.NORMAL,
    val lineSpacing: BibleLineSpacing = BibleLineSpacing.NORMAL,
    val fontStyle: BibleFontFamilyType = BibleFontFamilyType.SYSTEM_DEFAULT,
    val showVerseNumbers: Boolean = true,
    val showSubheadings: Boolean = true,
    val showFavoritesHint: Boolean = true,
    val showBookmarkHint: Boolean = true,
    val showHighlights: Boolean = true,
    val showParagraphAndIndents: Boolean = true,
    val showJesusWordsInRed: Boolean = false, // Default is now OFF as requested
    val jesusWordsColorHex: String = "#DC2626", // Red, Crimson, Maroon, Amber, Gold
    val customTextColorHex: String? = null, // Custom main text color
    val customHeadingColorHex: String? = null, // Custom chapter/section heading color
    val customSubHeadingColorHex: String? = null, // Custom subheading color
    val justifyBibleText: Boolean = false,
    val suggestVerseSelection: Boolean = true,
    val showNoteHint: Boolean = true,
    val showAudioPlayer: Boolean = true,
    val noteFormatHtml: Boolean = true,
    val screenTimeoutMinutes: Int = -1, // Default keep screen on while reading or custom timeout
    val theme: BibleTheme = BibleTheme.PAPER, // Default comforting reading paper
    val rememberLastReadingPosition: Boolean = true,
    val selectedTranslationId: String = BibleTranslation.HINDI_IRV.id,
    val useSerifFont: Boolean = false,
    val originalFormatMode: Boolean = false,
    val translationToggleBehavior: TranslationToggleBehavior = TranslationToggleBehavior.SINGLE_TAP_SHOW_ALL,
    val verseTapSelectionMode: VerseTapSelectionMode = VerseTapSelectionMode.ENTIRE_VERSE,
    // Dual / Bilingual Bible Options
    val isDualBibleEnabled: Boolean = false,
    val dualHindiVersionId: String = BibleTranslation.HINDI_IRV.id,
    val dualEnglishVersionId: String = BibleTranslation.ENGLISH_KJV.id,
    val dualViewMode: DualViewMode = DualViewMode.INTERLEAVED,
    // Version 21 & 22 Settings
    val showTodaysScriptureOnHome: Boolean = false, // By default hidden on Bible main navigation page
    val showActivatedPlansOnHome: Boolean = true, // Show activated plans on Bible main navigation page
    val activatedPlanIds: Set<String> = setOf("gospels_30"), // Set of activated reading plan IDs
    val manualPlansJson: String = "" // Serialized manual reading plans
)
