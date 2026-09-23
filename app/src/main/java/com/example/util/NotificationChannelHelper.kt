package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.service.AppFirebaseMessagingService

object NotificationChannelHelper {

    fun createAllNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channels = listOf(
            NotificationChannel(
                DailyPrayerReminderReceiver.CHANNEL_ID,
                "दैनिक प्रार्थना व प्रेरक वचन (Daily Prayer & Verses)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "प्रतिदिन प्रेरक बाइबल वचन एवं विशेष प्रार्थना अनुस्मारक"
                enableVibration(true)
                enableLights(true)
            },
            NotificationChannel(
                VerseAlarmReceiver.CHANNEL_ID,
                "आज का वचन (Daily Verse)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "अलार्म एवं आज का वचन अनुस्मारक"
                enableVibration(true)
                enableLights(true)
            },
            NotificationChannel(
                ReadingPlanReminderReceiver.CHANNEL_ID,
                "दैनिक बाइबल पठन (Reading Plan)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "प्रतिदिन बाइबल पठन योजना अनुस्मारक"
                enableVibration(true)
                enableLights(true)
            },
            NotificationChannel(
                ReminderBroadcastReceiver.CHANNEL_ID,
                "कार्यक्रम अनुस्मारक (Event Reminders)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "आगामी फेलोशिप एवं विशेष सभाओं के अनुस्मारक"
                enableVibration(true)
                enableLights(true)
            },
            NotificationChannel(
                AppFirebaseMessagingService.CHANNEL_ID,
                "महत्वपूर्ण सूचनाएं (Updates & Announcements)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "नई वीडियो, संदेश एवं लाइव अपडेट्स की सूचनाएं"
                enableVibration(true)
                enableLights(true)
            }
        )

        notificationManager.createNotificationChannels(channels)
    }
}
