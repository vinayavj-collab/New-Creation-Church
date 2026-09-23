package com.example.data.model

import androidx.annotation.Keep

@Keep
data class LiveStreamInfo(
    val isLive: Boolean = false,
    val title: String = "लाइव आराधना सेवा (Live Worship)",
    val subtitle: String = "अभी जुड़ें और प्रभु की महिमा करें",
    val url: String = "",
    val platform: String = "YOUTUBE",
    val scheduledTime: String = ""
)

@Keep
data class PrayerRequestItem(
    val id: String = "",
    val serialNumber: Int = 0,
    val name: String = "विश्वासी (Believer)",
    val city: String = "",
    val pastorName: String = "",
    val userRole: String = "विश्वासी",
    val isUrgent: Boolean = false,
    val requestText: String = "",
    val isPrivate: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val prayingCount: Int = 0,
    val senderDeviceId: String = "",
    val isAnswered: Boolean = false,
    val answeredTimestamp: Long = 0L,
    val testimonyText: String = "",
    val isVerifiedAdmin: Boolean = false,
    val adminDesignation: String = "",
    val adminName: String = "",
    val category: String = "अन्य",
    val tags: List<String> = emptyList(),
    val adminReplyText: String = "",
    val adminReplyAuthorName: String = "",
    val adminReplyAuthorDesignation: String = "",
    val adminReplyTimestamp: Long = 0L
) {
    fun getEffectiveCategory(): String {
        return if (category.isNotBlank()) category
        else if (tags.isNotEmpty()) tags.first()
        else "अन्य"
    }

    fun getEffectiveTags(): List<String> {
        return if (tags.isNotEmpty()) tags
        else if (category.isNotBlank()) listOf(category)
        else listOf("अन्य")
    }
}

object PrayerCategories {
    const val HEALING = "चंगाई"
    const val FAMILY = "पारिवारिक"
    const val LIVELIHOOD = "आजीविका"
    const val STUDENT = "छात्र जीवन"
    const val SPIRITUAL = "आत्मिक"
    const val MENTAL = "मानसिक"
    const val TRAVEL = "यात्रा"
    const val CHURCH = "कलीसिया"
    const val MINISTRY = "सेवकाई"
    const val OTHER = "अन्य"

    val ALL = listOf(
        HEALING,
        FAMILY,
        LIVELIHOOD,
        STUDENT,
        SPIRITUAL,
        MENTAL,
        TRAVEL,
        CHURCH,
        MINISTRY,
        OTHER
    )

    val ALL_CATEGORIES = ALL

    fun getCategoryIcon(cat: String): String = when (cat.trim()) {
        HEALING -> "🩺"
        FAMILY -> "👨‍👩‍👧‍👦"
        LIVELIHOOD -> "💼"
        STUDENT -> "🎓"
        SPIRITUAL -> "✝️"
        MENTAL -> "🧠"
        TRAVEL -> "🚗"
        CHURCH -> "⛪"
        MINISTRY -> "📖"
        else -> "🙏"
    }
}

@Keep
data class PrayerCategoryStatItem(
    val category: String = "",
    val icon: String = "",
    val totalCount: Int = 0,
    val answeredCount: Int = 0,
    val pendingCount: Int = 0,
    val percentage: Int = 0
)

@Keep
data class PrayerRequestsConfig(
    val enabled: Boolean = true,
    val showPastorField: Boolean = true,
    val allowTestimonies: Boolean = true,
    val allowPublicWall: Boolean = true,
    val maxDailyPrayTapsPerUser: Int = 1
)

@Keep
data class QuickAccessConfig(
    val prayerCountMode: String = "TOTAL", // "TOTAL", "TODAY", "TESTIMONY", "ACTIVE"
    val prayerChipLabel: String = "🙏 निवेदन",
    val showPrayerChip: Boolean = true,
    val showEventChip: Boolean = true,
    val showSongbookChip: Boolean = true,
    val showDailyPrayerChip: Boolean = true,
    val showSavedChip: Boolean = true
)

@Keep
data class FeaturedBannerItem(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val actionUrl: String = "",
    val badgeText: String = ""
)

@Keep
data class DailyAudioDevotional(
    val title: String = "आज का आत्मिक मनन",
    val speaker: String = "दैनिक मसीही सीख",
    val audioUrl: String = "",
    val date: String = "",
    val durationText: String = ""
)

@Keep
data class PrayerDeletionAuditLog(
    val id: String = "",
    val prayerRequestId: String = "",
    val serialNumber: Int = 0,
    val prayerName: String = "",
    val prayerCity: String = "",
    val prayerPastor: String = "",
    val prayerRole: String = "",
    val prayerText: String = "",
    val isAnswered: Boolean = false,
    val testimonyText: String = "",
    val originalTimestamp: Long = 0L,
    val deletedTimestamp: Long = System.currentTimeMillis(),
    val deletedDateFormatted: String = "",
    val deletedByRole: String = "", // "AUTHOR" or "ADMIN_PIN"
    val deletedByDeviceId: String = "",
    val adminPinUsed: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val action: String = "DELETE_PRAYER_REQUEST"
)

