package com.example.data.model

import androidx.annotation.Keep

/**
 * Global Enterprise Attendance Governance Policies (Master Admin Level)
 */
@Keep
data class AttendanceGlobalConfig(
    val isAttendanceServiceActive: Boolean = true,
    val permittedAttendanceModes: List<String> = listOf("usher_scans_member", "member_scans_venue_qr", "dual_mode"),
    val enforceGeoFencingGlobally: Boolean = true,
    val preventDeviceMultiCheckIn: Boolean = true,
    val maxAllowedRadiusMetersLimit: Int = 200,
    val dynamicQrRotationSeconds: Int = 30,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Branch Pastor & Admin Local Autonomy Settings
 */
@Keep
data class BranchAttendanceSettings(
    val branchId: String = "branch_ncc_01",
    val branchName: String = "न्यू क्रिएशन चर्च (मुख्य कलीसिया)",
    val churchPrefix: String = "NCC",
    val activeMode: String = "dual_mode", // "usher_scans_member", "member_scans_venue_qr", "dual_mode"
    val venueQrType: String = "static_wall_poster", // "static_wall_poster", "dynamic_rotating_screen"
    val latitude: Double = 21.1904,
    val longitude: Double = 81.2849,
    val allowedRadiusMeters: Int = 50, // 25, 50, 100, 200
    val delegatedUsherUserIds: List<String> = emptyList(),
    val delegatedUsherNames: List<String> = emptyList(),
    val secretHmacKey: String = "ncc_attendance_hmac_secret_2026",
    val isOfflineCheckInEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Branch-Level Recurring / Special Service Schedule with Time Window Guards
 */
@Keep
data class ServiceSchedule(
    val serviceId: String = "sun_morning_main",
    val serviceName: String = "रविवार सुबह की मुख्य आराधना",
    val dayOfWeek: Int = 0, // 0 = Sunday, 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday
    val serviceStartTime: String = "09:00", // 24-hr format "HH:mm"
    val serviceEndTime: String = "11:30",
    val checkInWindowOpenMinutesBefore: Int = 30, // Opens at 08:30 AM
    val checkInWindowCloseMinutesAfter: Int = 60,  // Late-arrival cut-off at 10:00 AM
    val isActive: Boolean = true
) {
    fun getDayNameHindi(): String {
        return when (dayOfWeek) {
            0 -> "रविवार (Sunday)"
            1 -> "सोमवार (Monday)"
            2 -> "मंगलवार (Tuesday)"
            3 -> "बुधवार (Wednesday)"
            4 -> "गुरुवार (Thursday)"
            5 -> "शुक्रवार (Friday)"
            6 -> "शनिवार (Saturday)"
            else -> "दैनिक"
        }
    }
}

/**
 * Result of Time Window Evaluation for Church Services
 */
@Keep
data class ActiveServiceWindowResult(
    val isWindowActive: Boolean = false,
    val activeService: ServiceSchedule? = null,
    val minutesUntilClose: Int = 0,
    val nextService: ServiceSchedule? = null,
    val nextServiceTimeFormatted: String = "",
    val message: String = ""
)

/**
 * Atomic Check-In Record with Composite Primary Key Rule:
 * {branchId}_{serviceId}_{serviceDate}_{memberIdOrSerial}
 */
@Keep
data class AttendanceRecord(
    val recordId: String = "",
    val branchId: String = "branch_ncc_01",
    val churchPrefix: String = "NCC",
    val serviceId: String = "sun_morning_main",
    val serviceName: String = "रविवार सुबह की मुख्य आराधना",
    val serviceDate: String = "", // e.g., "2026-09-27"
    val memberId: String = "",
    val memberSerial: String = "NCC01",
    val memberName: String = "",
    val gender: String = "Male", // "Male", "Female", "Child", "Other"
    val roleTier: String = "believer",
    val checkInMethod: String = "usher_scan", // "usher_scan", "venue_qr_static", "venue_qr_dynamic", "manual_usher_search"
    val verifiedByUsherId: String? = null,
    val verifiedByUsherName: String? = null,
    val isOfflineSynced: Boolean = false,
    val offlineScannedAt: Long? = null,
    val deviceId: String = "",
    val checkInTimestamp: Long = System.currentTimeMillis(),
    val geoLatitude: Double? = null,
    val geoLongitude: Double? = null,
    val distanceFromVenueMeters: Double? = null,
    val avatarUrl: String = "",
    val status: String = "CONFIRMED"
)

/**
 * Absentee Care (सुधि ट्रैकर) Analytics Model
 */
@Keep
data class AbsenteeCareMember(
    val memberId: String = "",
    val memberSerial: String = "",
    val memberName: String = "",
    val phone: String = "",
    val roleTier: String = "believer",
    val churchPrefix: String = "NCC",
    val lastAttendedDate: String = "",
    val consecutiveAbsenceCount: Int = 0, // 2 -> Follow-up Needed, 3+ -> Critical Alert
    val severityLevel: String = "FOLLOW_UP", // "FOLLOW_UP" (2 weeks), "CRITICAL" (3+ weeks)
    val lastFollowUpTimestamp: Long = 0L,
    val followUpNotes: String = "",
    val isCareContacted: Boolean = false
)

/**
 * Family Unit Model for 1-Tap Batch Family Pass Attendance
 */
@Keep
data class FamilyMemberItem(
    val userId: String = "",
    val memberSerial: String = "",
    val memberName: String = "",
    val relationship: String = "सदस्य", // "मुखिया/पिता", "माता/पत्नी", "बेटा", "बेटी", "सदस्य"
    val gender: String = "Male", // "Male", "Female", "Child", "Other"
    val roleTier: String = "believer",
    val avatarUrl: String = "",
    val dateOfBirth: String = "",
    val anniversaryDate: String = "",
    val isSelected: Boolean = true
)

@Keep
data class FamilyUnit(
    val familyId: String = "",
    val familyHeadUserId: String = "",
    val familyHeadSerial: String = "",
    val branchId: String = "branch_ncc_01",
    val familyName: String = "परिवार (Family)",
    val memberUids: List<String> = emptyList(),
    val members: List<FamilyMemberItem> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Pastoral Care Milestone Alert (Birthday & Wedding Anniversary Triggers)
 */
@Keep
data class PastoralMilestoneAlert(
    val memberId: String = "",
    val memberSerial: String = "",
    val memberName: String = "",
    val type: String = "BIRTHDAY", // "BIRTHDAY" | "ANNIVERSARY"
    val title: String = "जन्मदिन (Birthday)",
    val dateStr: String = "",
    val isToday: Boolean = true,
    val dayOffset: Int = 0, // 0 = today, -1 = yesterday, +1 = tomorrow
    val formattedMessage: String = ""
)
