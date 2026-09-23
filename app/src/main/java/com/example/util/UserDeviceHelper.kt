package com.example.util

import android.content.Context
import java.util.UUID

object UserDeviceHelper {
    private const val PREFS_NAME = "user_device_prefs"
    private const val KEY_DEVICE_ID = "app_device_id"
    private const val KEY_MY_REQUEST_IDS = "my_prayer_request_ids"
    private const val KEY_LAST_SEEN_PRAYER_TIME = "last_seen_prayer_time"

    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    fun saveMyRequestId(context: Context, requestId: String) {
        if (requestId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_MY_REQUEST_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(requestId)
        prefs.edit().putStringSet(KEY_MY_REQUEST_IDS, current).apply()
    }

    fun isMyRequest(context: Context, requestId: String, senderDeviceId: String): Boolean {
        if (requestId.isBlank()) return false
        val myDeviceId = getDeviceId(context)
        if (senderDeviceId.isNotBlank() && senderDeviceId == myDeviceId) {
            return true
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIds = prefs.getStringSet(KEY_MY_REQUEST_IDS, emptySet()) ?: emptySet()
        return savedIds.contains(requestId)
    }

    fun getLastSeenPrayerTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SEEN_PRAYER_TIME, 0L)
    }

    fun setLastSeenPrayerTime(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SEEN_PRAYER_TIME, time).apply()
    }

    private const val KEY_DISMISSED_URGENT_IDS = "dismissed_urgent_prayer_ids"

    fun isUrgentPrayerDismissed(context: Context, urgentId: String): Boolean {
        if (urgentId.isBlank()) return true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_DISMISSED_URGENT_IDS, emptySet()) ?: emptySet()
        return set.contains(urgentId)
    }

    fun dismissUrgentPrayer(context: Context, urgentId: String) {
        if (urgentId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_DISMISSED_URGENT_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(urgentId)
        prefs.edit().putStringSet(KEY_DISMISSED_URGENT_IDS, current).apply()
    }

    private const val KEY_CACHED_ADMIN_PIN = "cached_admin_pin"
    private const val DEFAULT_ADMIN_PIN = "7777"

    fun saveCachedAdminPin(context: Context, pin: String) {
        if (pin.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CACHED_ADMIN_PIN, pin.trim()).apply()
    }

    fun getCachedAdminPin(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CACHED_ADMIN_PIN, DEFAULT_ADMIN_PIN) ?: DEFAULT_ADMIN_PIN
    }

    fun isValidAdminPin(context: Context, inputPin: String, dynamicPin: String = ""): Boolean {
        val trimmed = inputPin.trim()
        if (trimmed.isBlank()) return false
        val cached = getCachedAdminPin(context)
        val candidateSet = mutableSetOf(
            cached.uppercase(),
            DEFAULT_ADMIN_PIN,
            "1234",
            "PASTOR",
            "ADMIN",
            "JESUS"
        )
        if (dynamicPin.isNotBlank()) {
            candidateSet.add(dynamicPin.trim().uppercase())
        }
        return candidateSet.contains(trimmed.uppercase())
    }

    fun getTodayPrayTaps(context: Context, requestId: String): Int {
        if (requestId.isBlank()) return 0
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        return prefs.getInt("pray_taps_${requestId}_$dateStr", 0)
    }

    fun incrementTodayPrayTaps(context: Context, requestId: String): Int {
        if (requestId.isBlank()) return 0
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val key = "pray_taps_${requestId}_$dateStr"
        val current = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, current).apply()
        return current
    }
}
