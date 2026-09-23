package com.example.data.model

import androidx.annotation.Keep

@Keep
data class AdminNotice(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isActive: Boolean = false,
    val type: String = "info", // "info", "warning", "announcement", "urgent"
    val actionText: String? = null,
    val actionUrl: String? = null,
    val isDismissible: Boolean = true,
    val isPermanent: Boolean = true,
    val durationHours: Int = 0,
    val durationDays: Int = 0,
    val expiresAtTimestamp: Long = 0L
) {
    fun isExpired(): Boolean {
        if (isPermanent) return false
        if (expiresAtTimestamp <= 0L) return false
        return System.currentTimeMillis() > expiresAtTimestamp
    }

    fun getRemainingTimeFormatted(): String {
        if (isPermanent) return "♾️ स्थायी"
        val diff = expiresAtTimestamp - System.currentTimeMillis()
        if (diff <= 0L) return "❌ समाप्त"
        val hours = diff / (1000 * 60 * 60)
        val days = hours / 24
        val remainingHours = hours % 24
        return if (days > 0) "⏱️ $days दिन ${remainingHours} घंटे शेष" else "⏱️ $hours घंटे ${(diff / (1000 * 60)) % 60} मिनट शेष"
    }
}

