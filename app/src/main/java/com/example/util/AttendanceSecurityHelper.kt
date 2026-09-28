package com.example.util

import android.location.Location
import com.example.data.model.ActiveServiceWindowResult
import com.example.data.model.BranchAttendanceSettings
import com.example.data.model.ServiceSchedule
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.*

object AttendanceSecurityHelper {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates precise distance between two GPS coordinates using Haversine formula
     */
    fun calculateHaversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) + cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Validates if the member's GPS location falls within the allowed radius of the church venue
     */
    fun isWithinGeoFence(
        memberLat: Double?,
        memberLng: Double?,
        venueLat: Double,
        venueLng: Double,
        allowedRadiusMeters: Int
    ): Pair<Boolean, Double> {
        if (memberLat == null || memberLng == null) {
            return Pair(false, -1.0)
        }
        val distance = calculateHaversineDistance(memberLat, memberLng, venueLat, venueLng)
        val isInside = distance <= allowedRadiusMeters.toDouble()
        return Pair(isInside, distance)
    }

    /**
     * Generates a dynamic HMAC-SHA256 rotating QR payload refreshing every [rotationSeconds]
     */
    fun generateDynamicRotatingQrPayload(
        branchId: String,
        secretKey: String,
        rotationSeconds: Int = 30
    ): Pair<String, Long> {
        val currentTimeMs = System.currentTimeMillis()
        val currentPeriod = currentTimeMs / (rotationSeconds * 1000L)
        val expirationTimestamp = (currentPeriod + 1) * (rotationSeconds * 1000L)

        val message = "$branchId:$currentPeriod"
        val hmacToken = generateHmacSha256(message, secretKey)

        val json = JSONObject().apply {
            put("type", "ROTATING_ATTENDANCE")
            put("branchId", branchId)
            put("token", hmacToken)
            put("period", currentPeriod)
            put("exp", expirationTimestamp)
        }
        return Pair(json.toString(), expirationTimestamp)
    }

    /**
     * Verifies a dynamic rotating QR payload token within +/- [tolerancePeriods] (default 1 period tolerance for network latency)
     */
    fun verifyDynamicRotatingQrPayload(
        qrPayload: String,
        expectedBranchId: String,
        secretKey: String,
        rotationSeconds: Int = 30,
        tolerancePeriods: Int = 1
    ): Boolean {
        return try {
            val json = JSONObject(qrPayload)
            if (json.optString("type") != "ROTATING_ATTENDANCE") return false
            val branchId = json.optString("branchId")
            if (branchId != expectedBranchId) return false
            val token = json.optString("token")
            val tokenPeriod = json.optLong("period", -1L)
            if (tokenPeriod <= 0) return false

            val currentPeriod = System.currentTimeMillis() / (rotationSeconds * 1000L)
            val diff = abs(currentPeriod - tokenPeriod)
            if (diff > tolerancePeriods) return false

            val expectedMessage = "$branchId:$tokenPeriod"
            val expectedHmac = generateHmacSha256(expectedMessage, secretKey)
            token == expectedHmac
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generates a Static Wall QR payload
     */
    fun generateStaticWallQrPayload(
        branchId: String,
        branchName: String,
        secretKey: String
    ): String {
        val sig = generateHmacSha256("STATIC_VENUE:$branchId", secretKey)
        val json = JSONObject().apply {
            put("type", "CHURCH_VENUE_QR")
            put("branchId", branchId)
            put("branchName", branchName)
            put("sig", sig)
            put("version", "v1")
        }
        return json.toString()
    }

    /**
     * Validates a Static Wall QR payload
     */
    fun verifyStaticWallQrPayload(
        qrPayload: String,
        expectedBranchId: String,
        secretKey: String
    ): Boolean {
        return try {
            val json = JSONObject(qrPayload)
            if (json.optString("type") != "CHURCH_VENUE_QR") return false
            val branchId = json.optString("branchId")
            if (branchId != expectedBranchId) return false
            val sig = json.optString("sig")
            val expectedSig = generateHmacSha256("STATIC_VENUE:$branchId", secretKey)
            sig == expectedSig
        } catch (e: Exception) {
            // Also allow plain match if raw text
            qrPayload.contains(expectedBranchId)
        }
    }

    /**
     * Extracts serial number / member identity from scanned QR payload
     */
    fun extractMemberSerialFromScan(qrContent: String): String {
        val trimmed = qrContent.trim()
        if (trimmed.isEmpty()) return ""

        // Try JSON parsing
        try {
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val json = JSONObject(trimmed)
                val serial = json.optString("serialNumber").ifBlank {
                    json.optString("serial").ifBlank {
                        json.optString("id").ifBlank {
                            json.optString("memberSerial")
                        }
                    }
                }
                if (serial.isNotBlank()) return serial
            }
        } catch (_: Exception) {}

        // Match patterns like NCC01, NCC12, DIO99, BEL01
        val regex = Regex("""\b([A-Z]{2,6}\d{1,6})\b""")
        val match = regex.find(trimmed)
        if (match != null) {
            return match.groupValues[1]
        }

        return trimmed
    }

    /**
     * Evaluates current Time Window against branch service schedules
     */
    fun evaluateActiveServiceWindow(
        schedules: List<ServiceSchedule>,
        calendar: Calendar = Calendar.getInstance()
    ): ActiveServiceWindowResult {
        val activeSchedules = schedules.filter { it.isActive }
        if (activeSchedules.isEmpty()) {
            return ActiveServiceWindowResult(
                isWindowActive = false,
                message = "वर्तमान में कोई कलीसिया सभा निर्धारित नहीं है।"
            )
        }

        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY=1 -> 0
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentMinutesOfDay = currentHour * 60 + currentMinute

        // Check if today matches any service
        val todaySchedules = activeSchedules.filter { it.dayOfWeek == currentDayOfWeek }

        for (schedule in todaySchedules) {
            val startMinutes = parseTimeToMinutes(schedule.serviceStartTime)
            val windowOpenMinutes = startMinutes - schedule.checkInWindowOpenMinutesBefore
            val windowCloseMinutes = startMinutes + schedule.checkInWindowCloseMinutesAfter

            if (currentMinutesOfDay in windowOpenMinutes..windowCloseMinutes) {
                val remaining = windowCloseMinutes - currentMinutesOfDay
                return ActiveServiceWindowResult(
                    isWindowActive = true,
                    activeService = schedule,
                    minutesUntilClose = remaining,
                    message = "सक्रिय सभा: ${schedule.serviceName} (चेक-इन खुला है, $remaining मिनट शेष)"
                )
            }
        }

        // Find next upcoming service
        val nextService = findNextUpcomingService(activeSchedules, currentDayOfWeek, currentMinutesOfDay)
        val nextFormatted = if (nextService != null) {
            "${nextService.getDayNameHindi()} - ${nextService.serviceStartTime}"
        } else {
            "शीघ्र"
        }

        return ActiveServiceWindowResult(
            isWindowActive = false,
            nextService = nextService,
            nextServiceTimeFormatted = nextFormatted,
            message = "इस समय कोई सक्रिय आराधना निर्धारित नहीं है। अगली आराधना $nextFormatted बजे खुलेगी।"
        )
    }

    private fun findNextUpcomingService(
        schedules: List<ServiceSchedule>,
        currentDay: Int,
        currentMinutes: Int
    ): ServiceSchedule? {
        // Today later
        val todayLater = schedules
            .filter { it.dayOfWeek == currentDay && parseTimeToMinutes(it.serviceStartTime) > currentMinutes }
            .minByOrNull { parseTimeToMinutes(it.serviceStartTime) }
        if (todayLater != null) return todayLater

        // Next days
        for (i in 1..7) {
            val checkDay = (currentDay + i) % 7
            val dayServices = schedules.filter { it.dayOfWeek == checkDay }
            if (dayServices.isNotEmpty()) {
                return dayServices.minByOrNull { parseTimeToMinutes(it.serviceStartTime) }
            }
        }
        return schedules.firstOrNull()
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            val h = parts[0].trim().toInt()
            val m = if (parts.size > 1) parts[1].trim().toInt() else 0
            h * 60 + m
        } catch (e: Exception) {
            540 // 9:00 AM fallback
        }
    }

    private fun generateHmacSha256(data: String, key: String): String {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
            mac.init(secretKeySpec)
            val bytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            MessageDigest.getInstance("MD5").digest(data.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }

    // =========================================================================
    // 1-TAP FAMILY PASS (BATCH QR ATTENDANCE EXTENSION)
    // =========================================================================

    /**
     * Generates a 1-Tap Family Pass QR JSON payload
     */
    fun generateFamilyPassPayload(familyId: String, headSerial: String): String {
        val json = JSONObject().apply {
            put("type", "FAMILY_PASS")
            put("familyId", familyId)
            put("headSerial", headSerial)
            put("version", "v1")
        }
        return json.toString()
    }

    /**
     * Checks whether scanned content is a Family Pass QR
     */
    fun isFamilyPassPayload(qrContent: String): Boolean {
        val trimmed = qrContent.trim()
        if (trimmed.startsWith("{") && trimmed.contains("\"FAMILY_PASS\"")) return true
        if (trimmed.startsWith("FAM_") || trimmed.startsWith("FAMILY:")) return true
        if (trimmed.endsWith("-F") || trimmed.contains("-F-") || trimmed.contains("_FAM")) return true
        return false
    }

    /**
     * Extracts Family Pass (familyId, headSerial) from scanned QR payload
     */
    fun extractFamilyPassInfo(qrContent: String): Pair<String, String>? {
        val trimmed = qrContent.trim()
        if (!isFamilyPassPayload(trimmed)) return null
        try {
            if (trimmed.startsWith("{")) {
                val json = JSONObject(trimmed)
                val famId = json.optString("familyId")
                val headSerial = json.optString("headSerial").ifBlank { json.optString("serialNumber") }
                if (famId.isNotBlank() || headSerial.isNotBlank()) {
                    return Pair(famId, headSerial)
                }
            } else if (trimmed.startsWith("FAMILY:")) {
                val parts = trimmed.substring(7).split(":")
                return Pair(parts.getOrNull(0) ?: "", parts.getOrNull(1) ?: "")
            } else if (trimmed.endsWith("-F")) {
                val headSerial = trimmed.removeSuffix("-F")
                return Pair("fam_$headSerial", headSerial)
            }
        } catch (_: Exception) {}
        return null
    }

    // =========================================================================
    // PASTORAL CARE MILESTONE TRIGGERS (BIRTHDAY & ANNIVERSARY EVALUATION)
    // =========================================================================

    /**
     * Evaluates member's DOB and Anniversary against current date within a +/- 2 day window
     */
    fun evaluatePastoralMilestones(
        dobStr: String,
        anniversaryStr: String,
        memberName: String,
        memberSerial: String,
        memberId: String = "",
        todayCalendar: Calendar = Calendar.getInstance()
    ): List<com.example.data.model.PastoralMilestoneAlert> {
        val alerts = mutableListOf<com.example.data.model.PastoralMilestoneAlert>()

        val todayMonth = todayCalendar.get(Calendar.MONTH) + 1 // 1-12
        val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH) // 1-31

        // Birthday Check
        if (dobStr.isNotBlank()) {
            val (bMonth, bDay) = parseMonthDay(dobStr)
            if (bMonth > 0 && bDay > 0) {
                val diff = calculateDayDifference(todayMonth, todayDay, bMonth, bDay)
                if (abs(diff) <= 2) {
                    val isToday = diff == 0
                    val msg = when {
                        isToday -> "🎂 आज $memberName ($memberSerial) का जन्मदिन है!"
                        diff == 1 -> "🎂 कल $memberName ($memberSerial) का जन्मदिन था।"
                        diff == -1 -> "🎂 कल $memberName ($memberSerial) का जन्मदिन है!"
                        diff == 2 -> "🎂 2 दिन पूर्व जन्मदिन था"
                        else -> "🎂 2 दिन बाद जन्मदिन है"
                    }
                    alerts.add(
                        com.example.data.model.PastoralMilestoneAlert(
                            memberId = memberId,
                            memberSerial = memberSerial,
                            memberName = memberName,
                            type = "BIRTHDAY",
                            title = if (isToday) "आज जन्मदिन है! 🎂" else "जन्मदिन (Birthday)",
                            dateStr = dobStr,
                            isToday = isToday,
                            dayOffset = diff,
                            formattedMessage = msg
                        )
                    )
                }
            }
        }

        // Wedding Anniversary Check
        if (anniversaryStr.isNotBlank()) {
            val (aMonth, aDay) = parseMonthDay(anniversaryStr)
            if (aMonth > 0 && aDay > 0) {
                val diff = calculateDayDifference(todayMonth, todayDay, aMonth, aDay)
                if (abs(diff) <= 2) {
                    val isToday = diff == 0
                    val msg = when {
                        isToday -> "💍 आज $memberName ($memberSerial) की विवाह वर्षगांठ है!"
                        diff == 1 -> "💍 कल $memberName ($memberSerial) की विवाह वर्षगांठ थी।"
                        diff == -1 -> "💍 कल $memberName ($memberSerial) की विवाह वर्षगांठ है!"
                        else -> "💍 विवाह वर्षगांठ (Wedding Anniversary)"
                    }
                    alerts.add(
                        com.example.data.model.PastoralMilestoneAlert(
                            memberId = memberId,
                            memberSerial = memberSerial,
                            memberName = memberName,
                            type = "ANNIVERSARY",
                            title = if (isToday) "विवाह वर्षगांठ! 💍" else "विवाह वर्षगांठ",
                            dateStr = anniversaryStr,
                            isToday = isToday,
                            dayOffset = diff,
                            formattedMessage = msg
                        )
                    )
                }
            }
        }

        return alerts
    }

    private fun parseMonthDay(dateStr: String): Pair<Int, Int> {
        return try {
            val parts = dateStr.trim().split("-", "/", ".")
            if (parts.size >= 3) {
                // Check if YYYY-MM-DD or DD-MM-YYYY
                if (parts[0].length == 4) {
                    Pair(parts[1].toInt(), parts[2].toInt())
                } else {
                    Pair(parts[1].toInt(), parts[0].toInt())
                }
            } else if (parts.size == 2) {
                Pair(parts[0].toInt(), parts[1].toInt())
            } else {
                Pair(0, 0)
            }
        } catch (_: Exception) {
            Pair(0, 0)
        }
    }

    private fun calculateDayDifference(m1: Int, d1: Int, m2: Int, d2: Int): Int {
        if (m1 == m2) {
            return d1 - d2
        }
        // Rough day of year diff
        val days1 = m1 * 30 + d1
        val days2 = m2 * 30 + d2
        return days1 - days2
    }
}
