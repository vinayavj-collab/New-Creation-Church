package com.example.data.model

import androidx.annotation.Keep

@Keep
data class HomeSectionItem(
    val id: String = "",                         // e.g. "HOME_DAILY_GREETING", "HOME_FEATURED_BANNERS_SLIDER", "CUSTOM_xxx"
    val title: String = "",                      // Friendly label in Hindi & English
    val subtitle: String = "",                   // Description
    val type: String = "NATIVE",                 // "NATIVE" or "CUSTOM"
    val isVisible: Boolean = true,               // Whether the section is active
    val isPermanent: Boolean = true,             // True = permanent, False = temporary (timed)
    val expiresAtTimestamp: Long = 0L,           // Timestamp in ms when temporary action occurs (e.g. now + N days)
    val temporaryAction: String = "HIDE_AFTER",  // "HIDE_AFTER" (temporarily visible, hides after expiry) or "SHOW_AFTER" (temporarily hidden, shows after expiry)
    val durationDays: Int = 0,                   // Display reference: e.g. 7 days
    // Custom section fields (for type == "CUSTOM")
    val customTitle: String = "",
    val customSubtitle: String = "",
    val customImageUrl: String = "",
    val customActionUrl: String = "",
    val customActionText: String = "",
    val customBgColorHex: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isCurrentlyVisible(): Boolean {
        val now = System.currentTimeMillis()
        if (!isPermanent && expiresAtTimestamp > 0L) {
            val isExpired = now >= expiresAtTimestamp
            return if (temporaryAction == "HIDE_AFTER") {
                // Temporarily shown: once expired, it is no longer visible
                if (isExpired) false else isVisible
            } else {
                // Temporarily hidden: once expired, it reverts back to visible!
                if (isExpired) true else isVisible
            }
        }
        return isVisible
    }

    // Friendly time status string
    fun getTimeStatusDescription(): String {
        if (isPermanent || expiresAtTimestamp <= 0L) return "स्थायी (Permanent)"
        val diffMs = expiresAtTimestamp - System.currentTimeMillis()
        if (diffMs <= 0L) {
            return if (temporaryAction == "HIDE_AFTER") "समाप्त (स्वतः छिपा)" else "समाप्त (पुनः सक्रिय)"
        }
        val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()
        val hours = ((diffMs / (1000 * 60 * 60)) % 24).toInt()
        val timeStr = if (days > 0) "$days दिन $hours घंटे शेष" else "$hours घंटे शेष"
        return if (temporaryAction == "HIDE_AFTER") "अस्थायी: $timeStr बाद स्वतः छिपेगा" else "अस्थायी: $timeStr बाद पुनः दिखेगा"
    }
}

@Keep
data class HomeSectionsConfig(
    val isCustomLayoutEnabled: Boolean = true,
    val sections: List<HomeSectionItem> = defaultHomeSections(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun isSectionVisible(sectionId: String): Boolean {
        val sec = sections.find { it.id == sectionId } ?: return true
        return sec.isCurrentlyVisible()
    }
}

fun defaultHomeSections(): List<HomeSectionItem> = listOf(
    HomeSectionItem(
        id = "HOME_DAILY_GREETING",
        title = "दैनिक अभिवादन व स्वागत कार्ड",
        subtitle = "Daily Greeting & Name Greeting Banner",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "HOME_LIVE_STREAM_BANNER",
        title = "लाइव स्ट्रीम बैनर",
        subtitle = "Live Worship & Prayer Broadcast Banner",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "HOME_SPECIAL_ANNOUNCEMENT",
        title = "विशेष घोषणा कार्ड",
        subtitle = "Special Important Announcement Card",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "HOME_ADMIN_NOTICE",
        title = "एडमिन सूचना / नोटिस",
        subtitle = "Admin Urgent Broadcast Notice",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "HOME_REMOTE_PROMO_BANNER",
        title = "प्रचार व प्रोमो बैनर",
        subtitle = "Special Remote Promotion Banner",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "HOME_FEATURED_BANNERS_SLIDER",
        title = "फीचर्ड बैनर स्लाइडर",
        subtitle = "Featured Carousel Banners",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "HOME_DAILY_AUDIO_DEVOTIONAL",
        title = "दैनिक ऑडियो वचन व मनन",
        subtitle = "Daily Audio Devotional & Reflection",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "HOME_QUICK_ACCESS_BAR",
        title = "⚡ क्विक एक्सेस बार",
        subtitle = "Quick Access Category Chips Bar",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "SEC_TODAYS_VERSE",
        title = "आज का वचन व प्रोग्रेस डैशबोर्ड (Verse & Reading Progress)",
        subtitle = "Today's Verse, Active Reading Plans & Progress Dashboard",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "HOME_DID_YOU_KNOW",
        title = "क्या आप जानते हैं? (Did You Know Bible Facts)",
        subtitle = "Daily Bible Fact Card with Source",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "HOME_DAILY_QUIZ",
        title = "आज का बाइबिल क्विज़ (Daily Bible Quiz)",
        subtitle = "Daily Multiple Choice Question with Answer & Explanation",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "HOME_DAILY_DEVOTIONAL",
        title = "दैनिक मनन (Daily Devotional)",
        subtitle = "Daily Bible Verse and Devotional Thought from Firestore & Offline",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "SEC_UPCOMING",
        title = "आगामी प्रार्थना / संगति कार्यक्रम",
        subtitle = "Upcoming Events & Meetings",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "SEC_FEATURED_FELLOWSHIP",
        title = "विशेष संगति / संगति कार्ड",
        subtitle = "Featured Fellowship Card",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "SEC_RECENT_FELLOWSHIP_HEADER",
        title = "हाल की संगतियां (Recent Fellowships)",
        subtitle = "Recent Fellowship Meetings List",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "SEC_LATEST_VIDEOS_HEADER",
        title = "नवीनतम वीडियो (Latest Videos)",
        subtitle = "Recently Uploaded Videos",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "SEC_PLAYLISTS_ROW",
        title = "वीडियो प्लेलिस्ट संग्रह (Playlists)",
        subtitle = "Featured Video Playlists Carousel",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "SEC_LATEST_EVENTS",
        title = "नवीनतम आयोजन (Latest Events)",
        subtitle = "Recent Events and Gatherings",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    ),
    HomeSectionItem(
        id = "SEC_PHOTOS",
        title = "तस्वीरें (Photo Gallery)",
        subtitle = "Ministry & Church Photo Highlights",
        type = "NATIVE",
        isVisible = false,
        isPermanent = false
    ),
    HomeSectionItem(
        id = "SEC_PERSONAL_VLOG_HEADER",
        title = "व्यक्तिगत व्लॉग (Personal Vlogs)",
        subtitle = "Vinay Kumar AVJ Personal Vlogs",
        type = "NATIVE",
        isVisible = true,
        isPermanent = true
    )
)
