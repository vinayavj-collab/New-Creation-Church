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

@Keep
data class AudioMessageConfig(
    val isServiceActive: Boolean = true,
    val maxDurationSeconds: Int = 180,
    val dailyPublishTime: String = "06:00",
    val dailySlotsCount: Int = 1,
    val recordingQuality: String = "standard_64k",
    val requiresPrePublishApproval: Boolean = false,
    val fallbackAudioUrl: String = "",
    val retentionDays: Int = 30,
    val allowedRoleTiersForRecording: List<String> = listOf("master_admin", "bishop", "pastor"),
    val delegatedMediaManagerIds: List<String> = emptyList(),
    val storageCapMb: Int = 500, // 100 MB to 4096 MB (4 GB)
    val maxFileSizeMb: Int = 10, // Per-file maximum upload limit allowed by Master Admin
    val enableFifoAutoCleanup: Boolean = true // Auto cleanup non-pinned items when storage reaches >= 95%
)

@Keep
data class DailyDevotion(
    val devotionId: String = "",
    val scheduledDate: String = "",
    val slotIndex: Int = 1,
    val slotLabel: String = "सुबह का मनन",
    val title: String = "",
    val scriptureRef: String = "",
    val speakerId: String = "",
    val speakerName: String = "",
    val speakerRole: String = "",
    val audioUrl: String = "",
    val storagePath: String = "",
    val durationSeconds: Int = 0,
    val fileSizeBytes: Long = 0L,
    val bitrateKbps: Int = 64,
    val status: String = "scheduled", // "scheduled", "pending_approval", "published", "rejected"
    val isPinned: Boolean = false,
    val pinnedReason: String = "",
    val isOverwritten: Boolean = false,
    val overwrittenBy: String = "",
    val overwrittenAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val approvedBy: String = "",
    val approvedAt: Long? = null,
    val listensCount: Long = 0L
)

@Keep
data class MediaGovernanceConfig(
    val allowPastorsLongMessages: Boolean = true,
    val compressionBitrate: String = "48k", // "32k" | "48k" | "64k" | "128k"
    val audioCodec: String = "aac",
    val maxOriginalFileSizeMB: Int = 250,
    val updatedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Keep
data class SermonItem(
    val sermonId: String = "",
    val title: String = "",
    val preacherName: String = "Rev. Vinay Kumar",
    val scriptureReferences: List<String> = emptyList(),
    val audioUrl: String = "",
    val originalSizeBytes: Long = 0L,
    val compressedSizeBytes: Long = 0L,
    val compressionRatio: String = "0%",
    val bitrate: String = "48k",
    val durationMinutes: Int = 0,
    val branchId: String = "branch_ncc_main",
    val uploadedBy: String = "",
    val category: String = "संडे आराधना", // "संडे आराधना", "बाइबल स्टडी", "उपवास प्रार्थना"
    val createdAt: Long = System.currentTimeMillis()
)


