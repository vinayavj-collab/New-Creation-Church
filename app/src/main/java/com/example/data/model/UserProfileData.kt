package com.example.data.model

import androidx.annotation.Keep

@Keep
data class UserProfileData(
    val userId: String = "",
    val displayName: String = "",
    val fullName: String = "", // Unified Demographic Name
    val photoUriOrPath: String = "",
    val role: String = "विश्वासी (Believer)",
    val phoneNumber: String = "",
    val phone: String = "", // Indexed phone field
    val city: String = "",
    val location: String = "", // City / Village demographic
    val bio: String = "",
    val favoriteVerse: String = "",
    val joinDate: String = "",
    val deviceId: String = "",
    val storagePermissionMode: String = "AUTOMATIC", // "AUTOMATIC" (default) or "CUSTOM"
    val isVerifiedVishwasi: Boolean = false,
    val isVerified: Boolean = false, // Emerald #10B981 tick
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // Demographic Analytics Fields
    val gender: String = "Male", // "Male", "Female", "Other"
    val dateOfBirth: String = "", // e.g., "1998-05-15" (Used for Age Brackets)
    val serialNumber: String = "", // Monospace alphanumeric (e.g., "NCC1", "DIO2")
    val previousSerials: List<String> = emptyList(),
    val churchId: String = "",
    val churchName: String = "",
    val congregationName: String = "", // Dynamic String reference
    val isBaptized: Boolean = false,
    val baptismStatus: Boolean = false,
    val baptismDate: String = "",
    val faithStatus: String = "Regular Believer", // "New Believer", "Regular Believer", "Seeking Baptism"
    val interests: List<String> = emptyList(), // ["Worship & Choir", "Youth Ministry", "Prayer Intercession", "Media & Tech", "Sunday School", "Hospitality"]
    val ministryInterest: String = "",
    val organizationName: String = "",
    val yearsInFaith: Int = 0,
    val distanceToChurchKm: Double = 0.0,
    val maritalStatus: String = "",
    val cityPincode: String = "",
    
    // Administrative Extensions
    val title: String = "", // "Rev.", "Pastor", "Bishop", "Dr.", "Bro."
    val roleTier: String = "believer", // "master_admin", "bishop", "deputy_bishop", "pastor", "elder", "believer"
    val isAdmin: Boolean = false, // False for regular believer
    val reportsToSeniorId: String = "",
    val assignedAuthorityId: String = "", // Creator/Authority UID who issued serial
    val assignedAuthorityName: String = "", // Creator/Authority Name
    val accessiblePrefixes: List<String> = emptyList(), // ["NCC", "KHWR"]
    val dioceseRegion: String = "",
    val p1PasswordHash: String = "",
    val canGenerateP2: Boolean = false,
    val customPermissions: List<String> = emptyList(),
    val accountStatus: String = "active", // "active", "disabled"
    val status: String = "active", // "pending_activation", "active", "disabled"
    val isDeviceBlocked: Boolean = false
) {
    fun getAge(): Int {
        if (dateOfBirth.isBlank()) return 28 // default adult median
        return try {
            val parts = dateOfBirth.split("-", "/")
            val birthYear = parts.firstOrNull { it.length == 4 }?.toIntOrNull() ?: 1995
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            (currentYear - birthYear).coerceAtLeast(0)
        } catch (_: Exception) {
            28
        }
    }

    fun getAgeBracket(): String {
        val age = getAge()
        return when {
            age < 13 -> "Kids (0-12)"
            age in 13..25 -> "Youth (13-25)"
            age in 26..59 -> "Adults (26-59)"
            else -> "Seniors (60+)"
        }
    }
}

enum class UserActivityType(val titleHindi: String, val titleEnglish: String) {
    ALL("समस्त (All)", "All"),
    BIBLE_READ("बाइबिल पठन", "Bible Reading"),
    VIDEO_WATCH("वीडियो / संदेश", "Video & Sermons"),
    BLOG_READ("आलेख / इवेंट्स", "Articles & Events"),
    PRAYER_POSTED("प्रार्थना निवेदन", "Prayer Requests"),
    TESTIMONY_SHARED("गवाही (Testimony)", "Testimony"),
    STUDY_NOTE("स्टडी नोट्स", "Study Notes"),
    SAVED_BOOKMARK("बुकमार्क्स", "Saved Bookmarks")
}

data class UserActivityItem(
    val id: String,
    val type: UserActivityType,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val imageUrl: String? = null,
    val payloadJson: String? = null
)
