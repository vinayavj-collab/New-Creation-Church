package com.example.data.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    SYSTEM("system", "System Default", "सिस्टम डिफ़ॉल्ट"),
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिंदी")
}

data class AppStringsData(
    // Bottom navigation
    val navHome: String,
    val navBlogs: String,
    val navYouTube: String,
    val navPhotos: String,
    val navMore: String,

    // Home Screen
    val chipEvents: String,
    val chipSaved: String,
    val chipRecent: String,
    val chipCustomize: String,
    val secUpcomingEvents: String,
    val secFeaturedFellowship: String,
    val secRecentFellowship: String,
    val secFavoritesFirst: String,
    val secLatestVideos: String,
    val secFeaturedPlaylists: String,
    val secLatestEvents: String,
    val secPhotoMemories: String,
    val secTodayScripture: String,
    val secPersonalVlog: String,
    val viewAll: String,
    val exploreYouTube: String,
    val allPlaylists: String,
    val albums: String,
    val readChapter: String,
    val allVlogPosts: String,
    val offlineMode: String,
    val offlineDesc: String,
    val retry: String,

    // More Screen
    val moreTitle: String,
    val secEventsSaved: String,
    val upcomingEvents: String,
    val upcomingEventsSub: String,
    val eventCalendar: String,
    val eventCalendarSub: String,
    val savedForLater: String,
    val savedForLaterSub: String,
    val recentlyViewed: String,
    val recentlyViewedSub: String,
    val secBibleSearch: String,
    val holyBible: String,
    val holyBibleSub: String,
    val globalSearch: String,
    val globalSearchSub: String,
    val dynamicLabels: String,
    val dynamicLabelsSub: String,
    val secCustomizationApp: String,
    val customizeHome: String,
    val customizeHomeSub: String,
    val settings: String,
    val settingsSub: String,
    val aboutVinay: String,
    val aboutVinaySub: String,
    val shareApp: String,
    val shareAppSub: String,
    val secOfficialChannels: String,
    val worshipChannel: String,
    val worshipChannelSub: String,
    val mainChannel: String,
    val mainChannelSub: String,
    val fellowshipBlog: String,
    val fellowshipBlogSub: String,

    // Settings Screen
    val settingsTitle: String,
    val secLanguage: String,
    val appInterfaceLanguage: String,
    val appLanguageDesc: String,
    val langSystem: String,
    val langEnglish: String,
    val langHindi: String,
    val activeLanguage: String,
    val secAppearance: String,
    val appTheme: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val secFeedCustomization: String,
    val customizeHomeSections: String,
    val customizeHomeSectionsSub: String,
    val favoriteCategories: String,
    val favoriteCategoriesSub: String,
    val secContentSources: String,
    val fellowshipEvents: String,
    val fellowshipEventsSub: String,
    val youTubeVideos: String,
    val youTubeVideosSub: String,
    val personalVlog: String,
    val personalVlogSub: String,
    val secNotifications: String,
    val eventReminders: String,
    val eventRemindersSub: String,
    val fellowshipAnnounce: String,
    val fellowshipAnnounceSub: String,
    val newYouTubeVideos: String,
    val newYouTubeVideosSub: String,
    val personalVlogNotify: String,
    val personalVlogNotifySub: String,
    val secOtherStorage: String,
    val clearCache: String,
    val clearCacheSub: String,
    val cacheClearedToast: String,
    val langChangedToast: String,

    // Blogs Screen
    val blogsTitle: String,
    val searchBlogsPlaceholder: String,
    val filterAll: String,
    val filterFellowship: String,
    val filterVlog: String,
    val noArticlesFound: String,

    // YouTube Screen
    val youtubeTitle: String,
    val youtubeSubtitle: String,
    val tabVideos: String,
    val tabPlaylists: String,
    val recentUploads: String,

    // Photos Screen
    val photosTitle: String,
    val tabAllPhotos: String,
    val tabAlbums: String,
    val noPhotos: String,

    // Upcoming Events & Calendar
    val upcomingTitle: String,
    val calendarTitle: String,
    val noUpcoming: String,
    val pastEventsArchive: String,
    val addToCalendar: String,
    val openLocation: String,
    val remindMe: String,
    val viewDetails: String,
    val eventSchedule: String,

    // Saved & History
    val savedTitle: String,
    val recentHistoryTitle: String,
    val emptySaved: String,
    val emptyRecent: String,
    val clearHistory: String,

    // Search
    val searchPlaceholder: String,
    val recentSearches: String,
    val clear: String,
    val noSearchResults: String,

    // Common
    val share: String,
    val openInBrowser: String,
    val bookmarkedToast: String,
    val removedBookmarkToast: String
)

val EnglishAppStrings = AppStringsData(
    navHome = "Home",
    navBlogs = "Blogs",
    navYouTube = "YouTube",
    navPhotos = "Photos",
    navMore = "More",

    chipEvents = "📅 Events",
    chipSaved = "⭐ Saved",
    chipRecent = "🕘 Recent",
    chipCustomize = "⚙️ Customize",
    secUpcomingEvents = "📅 UPCOMING EVENTS",
    secFeaturedFellowship = "FEATURED POST",
    secRecentFellowship = "RANDOM POSTS & MESSAGES",
    secFavoritesFirst = "FELLOWSHIP EVENTS (FAVORITES FIRST)",
    secLatestVideos = "▶️ RANDOM VIDEOS & MESSAGES",
    secFeaturedPlaylists = "FEATURED PLAYLISTS",
    secLatestEvents = "LATEST FELLOWSHIP UPDATES",
    secPhotoMemories = "PHOTO MEMORIES & ALBUMS",
    secTodayScripture = "📖 TODAY'S SCRIPTURE",
    secPersonalVlog = "PERSONAL LIFE",
    viewAll = "View All",
    exploreYouTube = "Explore YouTube",
    allPlaylists = "All Playlists",
    albums = "Albums",
    readChapter = "Read Chapter",
    allVlogPosts = "View All",
    offlineMode = "Offline Mode",
    offlineDesc = "Showing cached content. Connect to internet to refresh.",
    retry = "Retry",

    moreTitle = "More & Navigation",
    secEventsSaved = "EVENTS & PERSONAL SAVED",
    upcomingEvents = "Event",
    upcomingEventsSub = "Fellowship events, calendar and meeting schedule",
    eventCalendar = "Event",
    eventCalendarSub = "View scheduled events by date and add to Google Calendar",
    savedForLater = "Saved for Later",
    savedForLaterSub = "Bookmarked posts, videos, playlists & bible verses",
    recentlyViewed = "Recently Viewed",
    recentlyViewedSub = "Quick history of opened articles, media & scriptures",
    secBibleSearch = "BIBLE & SEARCH",
    holyBible = "Bible (पवित्र बाइबल)",
    holyBibleSub = "Read the Word of God • Offline & Multilingual",
    globalSearch = "Global Search",
    globalSearchSub = "Search blogs, videos, playlists, and scripture",
    dynamicLabels = "Dynamic Labels & Categories",
    dynamicLabelsSub = "Browse all dynamic labels from blog archives",
    secCustomizationApp = "CUSTOMIZATION & APP",
    customizeHome = "Customize Home Screen",
    customizeHomeSub = "Reorder sections and toggle visibility",
    settings = "Settings",
    settingsSub = "Language, theme, notifications & content sources",
    aboutVinay = "About Vinay Kumar AVJ",
    aboutVinaySub = "Vision, channels and ministry information",
    shareApp = "Share App",
    shareAppSub = "Share Vinay Kumar AVJ Fellowship Hub",
    secOfficialChannels = "OFFICIAL CHANNELS",
    worshipChannel = "Vinay Kumar AVJ Worship",
    worshipChannelSub = "Official YouTube Worship Channel",
    mainChannel = "Vinay Kumar AVJ",
    mainChannelSub = "Official YouTube Ministry Channel",
    fellowshipBlog = "Fellowship Events Blog",
    fellowshipBlogSub = "Official fellowship news & announcements",

    settingsTitle = "Settings & Preferences",
    secLanguage = "LANGUAGE / भाषा",
    appInterfaceLanguage = "App Interface Language",
    appLanguageDesc = "Controls app navigation and labels. Blog articles remain in their original posted language.",
    langSystem = "System Default",
    langEnglish = "English",
    langHindi = "हिंदी (Hindi)",
    activeLanguage = "Current Active: English",
    secAppearance = "APPEARANCE",
    appTheme = "App Theme",
    themeSystem = "System",
    themeLight = "Light",
    themeDark = "Dark",
    secFeedCustomization = "FEED & HOME CUSTOMIZATION",
    customizeHomeSections = "Customize Home Screen Sections",
    customizeHomeSectionsSub = "Reorder sections or toggle Upcoming Events, Videos, Verse",
    favoriteCategories = "Favorite Categories",
    favoriteCategoriesSub = "Prioritize specific labels on your Home and Blogs feeds",
    secContentSources = "CONTENT SOURCES",
    fellowshipEvents = "Fellowship Events",
    fellowshipEventsSub = "Primary fellowship news, conventions, and reports (Recommended: ON)",
    youTubeVideos = "YouTube Videos & Playlists",
    youTubeVideosSub = "Vinay Kumar AVJ Worship & Vinay Kumar AVJ channels",
    personalVlog = "Personal Vlog",
    personalVlogSub = "Pastor Vinay's personal reflections (Default: OFF)",
    secNotifications = "NOTIFICATIONS",
    eventReminders = "Event Reminders",
    eventRemindersSub = "Alerts for scheduled fellowship event reminders",
    fellowshipAnnounce = "Fellowship Event Announcements",
    fellowshipAnnounceSub = "Notifications for newly published fellowship articles",
    newYouTubeVideos = "New YouTube Videos",
    newYouTubeVideosSub = "Alerts for new worship songs and sermons",
    personalVlogNotify = "Personal Vlog Notifications",
    personalVlogNotifySub = "Notifications for personal vlog posts (Default: OFF)",
    secOtherStorage = "OTHER & STORAGE",
    clearCache = "Clear Local Cache",
    clearCacheSub = "Remove saved posts and videos to re-download fresh content",
    cacheClearedToast = "Local cache cleared. Refreshing...",
    langChangedToast = "Language switched to English",

    blogsTitle = "Blogs",
    searchBlogsPlaceholder = "Search articles, events...",
    filterAll = "All",
    filterFellowship = "Fellowship",
    filterVlog = "Personal Life",
    noArticlesFound = "No articles found",

    youtubeTitle = "YouTube Ministry",
    youtubeSubtitle = "Worship Songs, Sermons, Gospel & Fellowship Videos",
    tabVideos = "Videos",
    tabPlaylists = "Playlists",
    recentUploads = "Recent Uploads",

    photosTitle = "Photo Memories",
    tabAllPhotos = "All Photos",
    tabAlbums = "Albums",
    noPhotos = "No photos available.",

    upcomingTitle = "Upcoming Fellowship Events",
    calendarTitle = "Fellowship Event Calendar",
    noUpcoming = "No upcoming events available at this moment.",
    pastEventsArchive = "Past Events Archive",
    addToCalendar = "Add to Calendar",
    openLocation = "Open Location",
    remindMe = "Remind Me",
    viewDetails = "View Details",
    eventSchedule = "EVENT SCHEDULE",

    savedTitle = "Saved for Later",
    recentHistoryTitle = "Recently Viewed",
    emptySaved = "You haven't saved anything yet.",
    emptyRecent = "No recently viewed items yet.",
    clearHistory = "Clear History",

    searchPlaceholder = "Search blogs, videos, scripture...",
    recentSearches = "Recent Searches",
    clear = "Clear",
    noSearchResults = "No results found",

    share = "Share",
    openInBrowser = "Open in Browser",
    bookmarkedToast = "Saved for later",
    removedBookmarkToast = "Removed from Saved"
)

val HindiAppStrings = AppStringsData(
    navHome = "होम",
    navBlogs = "ब्लॉग्स",
    navYouTube = "यूट्यूब",
    navPhotos = "फ़ोटो",
    navMore = "अधिक",

    chipEvents = "📅 कार्यक्रम",
    chipSaved = "⭐ सहेजे गए",
    chipRecent = "🕘 हालिया",
    chipCustomize = "⚙️ अनुकूलित करें",
    secUpcomingEvents = "📅 आगामी कार्यक्रम (Upcoming Events)",
    secFeaturedFellowship = "विशेष संदेश (Featured)",
    secRecentFellowship = "रैंडम पोस्ट्स व संदेश (Random Posts)",
    secFavoritesFirst = "संगति कार्यक्रम (पसंदीदा पहले)",
    secLatestVideos = "▶️ रैंडम वीडियो (Random Videos)",
    secFeaturedPlaylists = "विशेष प्लेलिस्ट्स (Playlists)",
    secLatestEvents = "नवीनतम संगति अपडेट्स",
    secPhotoMemories = "फ़ोटो स्मृतियाँ और एल्बम",
    secTodayScripture = "📖 आज का पवित्र वचन",
    secPersonalVlog = "पर्सनल लाइफ़ (Personal Life)",
    viewAll = "सभी देखें",
    exploreYouTube = "यूट्यूब देखें",
    allPlaylists = "सभी प्लेलिस्ट",
    albums = "एल्बम",
    readChapter = "अध्याय पढ़ें",
    allVlogPosts = "सभी देखें",
    offlineMode = "ऑफ़लाइन मोड",
    offlineDesc = "कैश की गई सामग्री दिखाई जा रही है। ताज़ा करने के लिए इंटरनेट से जुड़ें।",
    retry = "पुनः प्रयास करें",

    moreTitle = "अधिक व नेविगेशन",
    secEventsSaved = "कार्यक्रम और सहेजे गए",
    upcomingEvents = "Event",
    upcomingEventsSub = "कलीसिया कार्यक्रम, कैलेंडर व मीटिंग शेड्यूलिंग",
    eventCalendar = "Event",
    eventCalendarSub = "तारीख के अनुसार कार्यक्रम देखें और गूगल कैलेंडर में जोड़ें",
    savedForLater = "बाद के लिए सहेजे गए",
    savedForLaterSub = "सहेजे गए पोस्ट, वीडियो, प्लेलिस्ट और बाइबल वचन",
    recentlyViewed = "हाल ही में देखे गए",
    recentlyViewedSub = "खोले गए लेखों, मीडिया और वचनों का हालिया इतिहास",
    secBibleSearch = "पवित्र बाइबल और खोज",
    holyBible = "पवित्र बाइबल (Holy Bible)",
    holyBibleSub = "परमेश्वर का वचन पढ़ें • ऑफ़लाइन और बहुभाषी",
    globalSearch = "वैश्विक खोज (Global Search)",
    globalSearchSub = "ब्लॉग, वीडियो, प्लेलिस्ट और वचन खोजें",
    dynamicLabels = "श्रेणियां और लेबल्स (Categories)",
    dynamicLabelsSub = "ब्लॉग अभिलेखागार से सभी श्रेणियां देखें",
    secCustomizationApp = "अनुकूलन और ऐप",
    customizeHome = "होम स्क्रीन अनुकूलित करें",
    customizeHomeSub = "सेक्शंस का क्रम बदलें और चालू/बंद करें",
    settings = "सेटिंग्स और भाषा",
    settingsSub = "भाषा, थीम, सूचनाएं और सामग्री स्रोत",
    aboutVinay = "विनय कुमार एवीजे के बारे में",
    aboutVinaySub = "दृष्टिकोण, चैनल और सेवकाई की जानकारी",
    shareApp = "ऐप शेयर करें",
    shareAppSub = "विनय कुमार एवीजे फेलोशिप ऐप साझा करें",
    secOfficialChannels = "आधिकारिक चैनल",
    worshipChannel = "विनय कुमार एवीजे वर्शिप",
    worshipChannelSub = "आधिकारिक यूट्यूब आराधना चैनल",
    mainChannel = "विनय कुमार एवीजे",
    mainChannelSub = "आधिकारिक यूट्यूब सेवकाई चैनल",
    fellowshipBlog = "संगति कार्यक्रम ब्लॉग",
    fellowshipBlogSub = "आधिकारिक संगति समाचार और घोषणाएं",

    settingsTitle = "सेटिंग्स और प्राथमिकताएं",
    secLanguage = "भाषा (LANGUAGE)",
    appInterfaceLanguage = "ऐप इंटरफ़ेस भाषा",
    appLanguageDesc = "नेविगेशन और लेबल्स को नियंत्रित करता है। ब्लॉग लेख अपनी मूल भाषा में रहेंगे।",
    langSystem = "सिस्टम डिफ़ॉल्ट",
    langEnglish = "English",
    langHindi = "हिंदी (Hindi)",
    activeLanguage = "वर्तमान सक्रिय: हिंदी (Hindi)",
    secAppearance = "रूप-रंग (APPEARANCE)",
    appTheme = "ऐप थीम (Theme)",
    themeSystem = "सिस्टम",
    themeLight = "लाइट",
    themeDark = "डार्क",
    secFeedCustomization = "फ़ीड और होम अनुकूलन",
    customizeHomeSections = "होम स्क्रीन सेक्शंस अनुकूलित करें",
    customizeHomeSectionsSub = "क्रम बदलें या आगामी कार्यक्रम, वीडियो, वचन चालू/बंद करें",
    favoriteCategories = "पसंदीदा श्रेणियां",
    favoriteCategoriesSub = "होम और ब्लॉग्स फ़ीड पर पसंदीदा लेबल्स को प्राथमिकता दें",
    secContentSources = "सामग्री स्रोत (CONTENT SOURCES)",
    fellowshipEvents = "संगति कार्यक्रम",
    fellowshipEventsSub = "मुख्य संगति समाचार, सम्मेलन और रिपोर्ट (अनुशंसित: चालू)",
    youTubeVideos = "यूट्यूब वीडियो और प्लेलिस्ट",
    youTubeVideosSub = "विनय कुमार एवीजे वर्शिप और मुख्य चैनल",
    personalVlog = "व्यक्तिगत व्लॉग",
    personalVlogSub = "पास्टर विनय के व्यक्तिगत विचार (डिफ़ॉल्ट: बंद)",
    secNotifications = "सूचनाएं (NOTIFICATIONS)",
    eventReminders = "कार्यक्रम अनुस्मारक",
    eventRemindersSub = "निर्धारित संगति कार्यक्रम अनुस्मारक के लिए अलर्ट",
    fellowshipAnnounce = "संगति कार्यक्रम घोषणाएं",
    fellowshipAnnounceSub = "नए प्रकाशित संगति लेखों की सूचनाएं",
    newYouTubeVideos = "नए यूट्यूब वीडियो",
    newYouTubeVideosSub = "नए आराधना गीतों और संदेशों के लिए अलर्ट",
    personalVlogNotify = "व्यक्तिगत व्लॉग सूचनाएं",
    personalVlogNotifySub = "व्यक्तिगत व्लॉग पोस्ट की सूचनाएं (डिफ़ॉल्ट: बंद)",
    secOtherStorage = "अन्य और स्टोरेज",
    clearCache = "लोकल कैश साफ़ करें",
    clearCacheSub = "ताज़ा सामग्री फिर से लोड करने के लिए कैश साफ़ करें",
    cacheClearedToast = "लोकल कैश साफ़ हो गया। ताज़ा किया जा रहा है...",
    langChangedToast = "भाषा हिंदी में बदली गई",

    blogsTitle = "ब्लॉग्स",
    searchBlogsPlaceholder = "लेख, कार्यक्रम खोजें...",
    filterAll = "All",
    filterFellowship = "फेलोशिप",
    filterVlog = "पर्सनल लाइफ़",
    noArticlesFound = "कोई लेख नहीं मिला",

    youtubeTitle = "यूट्यूब मिनिस्ट्री",
    youtubeSubtitle = "आराधना गीत, उपदेश, सुसमाचार और संगति वीडियो",
    tabVideos = "वीडियो",
    tabPlaylists = "प्लेलिस्ट",
    recentUploads = "हालिया अपलोड",

    photosTitle = "फ़ोटो स्मृतियाँ",
    tabAllPhotos = "सभी फ़ोटो",
    tabAlbums = "एल्बम",
    noPhotos = "कोई फ़ोटो उपलब्ध नहीं है।",

    upcomingTitle = "आगामी संगति कार्यक्रम",
    calendarTitle = "संगति कार्यक्रम कैलेंडर",
    noUpcoming = "वर्तमान में कोई आगामी कार्यक्रम उपलब्ध नहीं है।",
    pastEventsArchive = "पिछले कार्यक्रम संग्रह",
    addToCalendar = "कैलेंडर में जोड़ें",
    openLocation = "नक्शा देखें",
    remindMe = "याद दिलाएं",
    viewDetails = "विवरण देखें",
    eventSchedule = "कार्यक्रम विवरण",

    savedTitle = "बाद के लिए सहेजे गए",
    recentHistoryTitle = "हाल ही में देखे गए",
    emptySaved = "आपने अभी तक कुछ भी सहेज कर नहीं रखा है।",
    emptyRecent = "हाल ही में देखा गया कोई आइटम नहीं है।",
    clearHistory = "इतिहास साफ़ करें",

    searchPlaceholder = "ब्लॉग, वीडियो, वचन खोजें...",
    recentSearches = "हालिया खोजें",
    clear = "साफ़ करें",
    noSearchResults = "कोई परिणाम नहीं मिला",

    share = "शेयर करें",
    openInBrowser = "ब्राउज़र में खोलें",
    bookmarkedToast = "बाद के लिए सहेजा गया",
    removedBookmarkToast = "सहेजे गए से हटाया गया"
)

val LocalAppStrings = staticCompositionLocalOf<AppStringsData> { EnglishAppStrings }
val LocalAppLanguage = staticCompositionLocalOf<AppLanguage> { AppLanguage.SYSTEM }

@Composable
fun appStrings(): AppStringsData = LocalAppStrings.current

object AppStrings {
    fun forLanguage(language: AppLanguage): AppStringsData {
        return when (language) {
            AppLanguage.HINDI -> HindiAppStrings
            AppLanguage.ENGLISH -> EnglishAppStrings
            AppLanguage.SYSTEM -> {
                val defaultLang = Locale.getDefault().language
                if (defaultLang.equals("hi", ignoreCase = true)) HindiAppStrings else EnglishAppStrings
            }
        }
    }

    // Backward compatibility helper
    fun get(key: String, language: AppLanguage): String {
        val s = forLanguage(language)
        return when (key) {
            "nav_home" -> s.navHome
            "nav_blogs" -> s.navBlogs
            "nav_youtube" -> s.navYouTube
            "nav_photos" -> s.navPhotos
            "nav_more" -> s.navMore
            "sec_fellowship" -> s.secFeaturedFellowship
            "sec_upcoming" -> s.secUpcomingEvents
            "sec_latest_events" -> s.secLatestEvents
            "sec_latest_videos" -> s.secLatestVideos
            "sec_playlists" -> s.secFeaturedPlaylists
            "sec_photos" -> s.secPhotoMemories
            "sec_today_verse" -> s.secTodayScripture
            "sec_personal_vlog" -> s.secPersonalVlog
            "view_all" -> s.viewAll
            "all_videos" -> s.exploreYouTube
            "view_details" -> s.viewDetails
            "add_to_calendar" -> s.addToCalendar
            "open_location" -> s.openLocation
            "remind_me" -> s.remindMe
            "saved" -> s.savedTitle
            "recently_viewed" -> s.recentHistoryTitle
            "clear_history" -> s.clearHistory
            "favorite_categories" -> s.favoriteCategories
            "home_screen_settings" -> s.customizeHome
            "app_language" -> s.appInterfaceLanguage
            "empty_saved" -> s.emptySaved
            "empty_upcoming" -> s.noUpcoming
            "empty_photos" -> s.noPhotos
            "no_internet" -> s.offlineMode
            "retry" -> s.retry
            else -> key
        }
    }
}
