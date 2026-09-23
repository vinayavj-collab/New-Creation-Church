package com.example.data.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    DYNAMIC
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
    VINAY_KUMAR_AVJ("Vinay Kumar AVJ")
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
    DID_YOU_KNOW("did_you_know", "क्या आप जानते हैं? (Did You Know)"),
    DAILY_QUIZ("daily_quiz", "Daily Bible Quiz / आज का क्विज़"),
    DAILY_DEVOTIONAL("daily_devotional", "Daily Devotional / दैनिक मनन"),
    UPCOMING_EVENTS("upcoming", "Upcoming Events"),
    FELLOWSHIP_EVENTS("fellowship", "Fellowship Events (Featured)"),
    LATEST_VIDEOS("latest_videos", "Latest YouTube Videos"),
    PLAYLISTS("playlists", "Featured Playlists"),
    PHOTOS("photos", "Photo Gallery"),
    LATEST_EVENTS("latest_events", "Latest Events"),
    PERSONAL_VLOG("personal_vlog", "Personal Vlog")
}

enum class VerseAlarmFrequency(val titleHindi: String, val titleEnglish: String) {
    DAILY("प्रतिदिन एक बार (Daily Once)", "Daily Once"),
    INTERVAL_HOURS("निश्चित अंतराल पर (Fixed Interval)", "Fixed Interval")
}

enum class VerseAlarmMode(val titleHindi: String, val titleEnglish: String) {
    NOTIFICATION_ONLY("केवल नोटिफिकेशन (Notification Only)", "Notification Only"),
    SPEECH_DIRECT("अलार्म व तुरंत वचन वाचन (Alarm & Direct Speech)", "Alarm & Direct Speech"),
    MUSIC_THEN_SPEECH("पहले संगीत/अलार्म फिर वाचन (Music First, Speech on Stop)", "Music First, Speech on Stop")
}

enum class VerseAlarmContent(val titleHindi: String, val titleEnglish: String) {
    GREETING_AND_VERSE("अभिवादन + आज का वचन (Greeting + Verse)", "Greeting + Verse"),
    VERSE_ONLY("केवल आज का वचन (Only Verse)", "Only Verse")
}

enum class DailyPrayerSlot(val titleHindi: String, val titleEnglish: String, val defaultHour: Int, val defaultMinute: Int) {
    MORNING("सुबह की प्रार्थना (Morning)", "Morning Prayer", 4, 0),
    AFTERNOON("दोपहर की प्रार्थना (Afternoon)", "Afternoon Prayer", 12, 30),
    EVENING("संध्या / शाम की प्रार्थना (Evening)", "Evening Prayer", 18, 0),
    NIGHT("रात्रि की प्रार्थना (Night)", "Night Prayer", 21, 30),
    CUSTOM("कस्टम समय (Custom Time)", "Custom Time", 4, 0)
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.DYNAMIC,
    val showFellowshipEvents: Boolean = true,
    val personalVlogMode: PersonalVlogMode = PersonalVlogMode.HIDDEN, // CRITICAL: HIDDEN by default
    val showPersonalVlog: Boolean = false, // Backwards-compatible flag
    val showYouTube: Boolean = true,
    val showShorts: Boolean = true,
    val youtubeDefaultTab: YouTubeDefaultTab = YouTubeDefaultTab.ALL,
    val bibleReadingStyle: BibleReadingStyle = BibleReadingStyle.PRINTED_BIBLE,
    val dataSaverEnabled: Boolean = true,
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
        HomeSectionType.DID_YOU_KNOW,
        HomeSectionType.DAILY_QUIZ,
        HomeSectionType.DAILY_DEVOTIONAL,
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
        HomeSectionType.DID_YOU_KNOW,
        HomeSectionType.DAILY_QUIZ,
        HomeSectionType.DAILY_DEVOTIONAL,
        HomeSectionType.UPCOMING_EVENTS,
        HomeSectionType.FELLOWSHIP_EVENTS,
        HomeSectionType.LATEST_VIDEOS,
        HomeSectionType.PLAYLISTS,
        HomeSectionType.LATEST_EVENTS
    ),
    val customFourthTab: CustomFourthTab = CustomFourthTab.SONG_BOOK,
    val bloggerPhotoLayout: BloggerPhotoLayout = BloggerPhotoLayout.GRID_2,
    val isDrawerEnabled: Boolean = true,
    val drawerPosition: String = "left", // "left" or "right"
    val lastReadPostId: String? = null,
    val navTabsOrder: List<String> = listOf("HOME", "BLOGS", "YOUTUBE", "FOURTH_TAB", "MORE"),
    val activePlanIds: Set<String> = emptySet(),
    val planBehindColorHex: String = "#EF4444",   // Red for behind schedule
    val planOnTrackColorHex: String = "#EAB308",  // Yellow for on track
    val planCompletedColorHex: String = "#10B981", // Green for daily completed
    // Welcome Customization & TTS Settings
    val userName: String = "",
    val enableWelcomeSpeech: Boolean = true,
    val enableVerseSpeechOnLaunch: Boolean = true,
    val welcomeSpeechOncePerDay: Boolean = false,
    val verseSpeechOncePerDay: Boolean = false,
    val welcomeDialogDismissed: Boolean = false,
    // Verse of the Day Alarm & Voice Settings
    val verseAlarmEnabled: Boolean = true,
    val verseAlarmHour: Int = 6,
    val verseAlarmMinute: Int = 0,
    val verseAlarmFrequency: VerseAlarmFrequency = VerseAlarmFrequency.DAILY,
    val verseAlarmIntervalHours: Int = 4,
    val verseAlarmMode: VerseAlarmMode = VerseAlarmMode.SPEECH_DIRECT,
    val verseAlarmContent: VerseAlarmContent = VerseAlarmContent.GREETING_AND_VERSE,
    val syncGreetingVolumeWithAlarm: Boolean = true,
    val greetingSpeechVolume: Float = 1.0f,
    val greetingSpeechPitch: Float = 1.0f,
    val greetingSpeechSpeed: Float = 1.0f,
    val alarmVolume: Float = 1.0f,
    // Reading Plan Reminder Settings (Morning 5 am, Evening 9 pm)
    val readingPlanReminderEnabled: Boolean = true,
    val readingPlanReminderHour: Int = 5,
    val readingPlanReminderMinute: Int = 0,
    val readingPlanReminderEveningEnabled: Boolean = true,
    val readingPlanReminderEveningHour: Int = 21,
    val readingPlanReminderEveningMinute: Int = 0,
    // Daily Prayer & Motivational Verse Reminder Settings (4 am)
    val dailyPrayerReminderEnabled: Boolean = true,
    val dailyPrayerReminderHour: Int = 4,
    val dailyPrayerReminderMinute: Int = 0,
    val dailyPrayerReminderSlot: DailyPrayerSlot = DailyPrayerSlot.MORNING,
    val morningPrayerHour: Int = 4,
    val morningPrayerMinute: Int = 0,
    val afternoonPrayerHour: Int = 12,
    val afternoonPrayerMinute: Int = 30,
    val eveningPrayerHour: Int = 18,
    val eveningPrayerMinute: Int = 0,
    val nightPrayerHour: Int = 21,
    val nightPrayerMinute: Int = 30,
    // Widget Settings (0 = Daily Once / Only Today's Verse; > 0 = Auto change interval hours)
    val widgetAutoChangeIntervalHours: Int = 0,
    // Master Admin Control & Security Policy
    val personalBlogPassword: String = "1234",
    val inactiveAdminAutoDisableDays: Int = 90,
    val isGlobalAdminEmergencyLock: Boolean = false,
    val hasSeenProfileAdminPrompt: Boolean = false,
    val masterAdminPasswordEnabled: Boolean = true,
    val masterAdminPin: String = "9876",
    val masterAdminDualAuthEnabled: Boolean = false,
    val masterAdminSecondaryPin: String = "123456",
    val biometricTimeoutDays: Int = 30,
    val isBiometricEnabled: Boolean = true,
    val globalAuthBypass: Boolean = false,
    val requireP2EveryLogin: Boolean = true,
    val trustedDevices: List<String> = listOf("Android-Primary-Device", "Mobile-Auth-Terminal-01"),
    val profileReminderIntervalDays: Int = 7,
    val notificationMethod: String = "Local Notification",
    // Chat Configuration & Access Controls
    val isChatEnabled: Boolean = false,
    val chatAllowOnlyVerified: Boolean = true,
    val chatWhitelistedUserIds: List<String> = emptyList(),
    val chatAllowedRoles: List<String> = emptyList(),
    val delegatedGlobalEventCreators: List<String> = emptyList()
)

