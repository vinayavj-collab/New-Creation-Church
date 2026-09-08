package com.example.data.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class HomeSectionType(val id: String, val defaultTitle: String) {
    FELLOWSHIP_EVENTS("fellowship", "Fellowship Events (Featured)"),
    UPCOMING_EVENTS("upcoming", "Upcoming Events"),
    LATEST_EVENTS("latest_events", "Latest Events"),
    LATEST_VIDEOS("latest_videos", "Latest YouTube Videos"),
    PLAYLISTS("playlists", "Featured Playlists"),
    PHOTOS("photos", "Photo Gallery"),
    TODAYS_VERSE("verse", "Today's Bible Verse"),
    PERSONAL_VLOG("personal_vlog", "Personal Vlog (Hidden by Default)")
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showFellowshipEvents: Boolean = true,
    val showPersonalVlog: Boolean = false, // CRITICAL: OFF by default
    val showYouTube: Boolean = true,
    val showShorts: Boolean = true,
    val notifyFellowshipEvents: Boolean = true,
    val notifyYouTube: Boolean = true,
    val notifyPersonalVlog: Boolean = false, // CRITICAL: OFF by default
    val notifyUpcomingReminders: Boolean = true,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val favoriteCategories: Set<String> = emptySet(),
    val homeSectionsOrder: List<HomeSectionType> = listOf(
        HomeSectionType.FELLOWSHIP_EVENTS,
        HomeSectionType.UPCOMING_EVENTS,
        HomeSectionType.LATEST_EVENTS,
        HomeSectionType.LATEST_VIDEOS,
        HomeSectionType.PLAYLISTS,
        HomeSectionType.PHOTOS,
        HomeSectionType.TODAYS_VERSE,
        HomeSectionType.PERSONAL_VLOG
    ),
    val enabledHomeSections: Set<HomeSectionType> = setOf(
        HomeSectionType.FELLOWSHIP_EVENTS,
        HomeSectionType.UPCOMING_EVENTS,
        HomeSectionType.LATEST_EVENTS,
        HomeSectionType.LATEST_VIDEOS,
        HomeSectionType.PLAYLISTS,
        HomeSectionType.PHOTOS,
        HomeSectionType.TODAYS_VERSE
    ),
    val lastReadPostId: String? = null
)
