package com.example.data.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class PersonalVlogMode(val displayName: String) {
    HIDDEN("Hidden (Default)"),
    SECONDARY("Secondary (Available Separately)"),
    HOME_AND_SECONDARY("Home + Secondary"),
    PRIORITY_OVERRIDE("Priority / Override (When Newer)")
}

enum class BibleReadingStyle(val displayName: String) {
    PRINTED_BIBLE("Printed Bible Style (Continuous + Headings)"),
    PARAGRAPH("Paragraph Style")
}

enum class YouTubeDefaultTab(val displayName: String) {
    ALL("All Channels (सभी चैनल)"),
    AVJ_WORSHIP("Vinay Kumar AVJ Worship (Worship)"),
    VINAY_KUMAR_AVJ("Vinay Kumar AVJ"),
    NEW_CREATION_CHURCH("New Creation Church")
}

enum class CustomFourthTab(val titleHindi: String, val titleEnglish: String) {
    PHOTOS("फ़ोटो (Photos)", "Photos"),
    BIBLE("बाइबिल (Bible)", "Bible"),
    READING_PLAN("रीडिंग प्लान (Reading Plan)", "Reading Plan"),
    SONG_BOOK("मसीही गीत (Song Book)", "Song Book"),
    NOTES("माई स्टडी नोट्स (Notes)", "Notes")
}

enum class BloggerPhotoLayout(val columns: Int, val titleHindi: String, val titleEnglish: String) {
    SINGLE(1, "1 बड़ा फ़ोटो (1 Full)", "1 Full"),
    GRID_2(2, "2 ग्रिड (2 Grid)", "2 Columns"),
    GRID_3(3, "3 ग्रिड (3 Grid)", "3 Columns"),
    GRID_4(4, "4 ग्रिड (4 Grid)", "4 Columns")
}

enum class HomeSectionType(val id: String, val defaultTitle: String) {
    TODAYS_VERSE("verse", "Today's Bible Verse / आज का वचन"),
    UPCOMING_EVENTS("upcoming", "Upcoming Events"),
    FELLOWSHIP_EVENTS("fellowship", "Fellowship Events (Featured)"),
    LATEST_VIDEOS("latest_videos", "Latest YouTube Videos"),
    PLAYLISTS("playlists", "Featured Playlists"),
    PHOTOS("photos", "Photo Gallery"),
    LATEST_EVENTS("latest_events", "Latest Events"),
    PERSONAL_VLOG("personal_vlog", "Personal Vlog")
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showFellowshipEvents: Boolean = true,
    val personalVlogMode: PersonalVlogMode = PersonalVlogMode.HIDDEN, // CRITICAL: HIDDEN by default
    val showPersonalVlog: Boolean = false, // Backwards-compatible flag
    val showYouTube: Boolean = true,
    val showShorts: Boolean = true,
    val youtubeDefaultTab: YouTubeDefaultTab = YouTubeDefaultTab.AVJ_WORSHIP,
    val bibleReadingStyle: BibleReadingStyle = BibleReadingStyle.PRINTED_BIBLE,
    val dataSaverEnabled: Boolean = false,
    val notifyTodaysScripture: Boolean = true,
    val notifyReadingPlan: Boolean = true,
    val notifyFellowshipEvents: Boolean = true,
    val notifyYouTube: Boolean = true,
    val notifyPersonalVlog: Boolean = false, // CRITICAL: OFF by default
    val notifyUpcomingReminders: Boolean = true,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val favoriteCategories: Set<String> = emptySet(),
    val homeSectionsOrder: List<HomeSectionType> = listOf(
        HomeSectionType.TODAYS_VERSE,
        HomeSectionType.UPCOMING_EVENTS,
        HomeSectionType.FELLOWSHIP_EVENTS,
        HomeSectionType.LATEST_VIDEOS,
        HomeSectionType.PLAYLISTS,
        HomeSectionType.PHOTOS,
        HomeSectionType.LATEST_EVENTS,
        HomeSectionType.PERSONAL_VLOG
    ),
    val enabledHomeSections: Set<HomeSectionType> = setOf(
        HomeSectionType.TODAYS_VERSE,
        HomeSectionType.UPCOMING_EVENTS,
        HomeSectionType.FELLOWSHIP_EVENTS,
        HomeSectionType.LATEST_VIDEOS,
        HomeSectionType.PLAYLISTS,
        HomeSectionType.PHOTOS,
        HomeSectionType.LATEST_EVENTS
    ),
    val customFourthTab: CustomFourthTab = CustomFourthTab.PHOTOS,
    val bloggerPhotoLayout: BloggerPhotoLayout = BloggerPhotoLayout.GRID_2,
    val lastReadPostId: String? = null,
    val navTabsOrder: List<String> = listOf("HOME", "BLOGS", "YOUTUBE", "FOURTH_TAB", "MORE"),
    val activePlanIds: Set<String> = setOf("gospels_30"),
    val planBehindColorHex: String = "#EF4444",   // Red for behind schedule
    val planOnTrackColorHex: String = "#EAB308",  // Yellow for on track
    val planCompletedColorHex: String = "#10B981" // Green for daily completed
)

