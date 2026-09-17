package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.PreferencesManager
import com.example.data.prayer.repository.DailyPrayerRepository
import java.util.Calendar

object DailyPrayerReminderScheduler {
    const val REQUEST_CODE = 9041

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int, enabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyPrayerReminderReceiver::class.java).apply {
            action = "com.example.ACTION_DAILY_PRAYER_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } catch (ignored: Exception) {}
        }
    }

    fun triggerTestNotification(context: Context) {
        val intent = Intent(context, DailyPrayerReminderReceiver::class.java).apply {
            action = "com.example.ACTION_DAILY_PRAYER_REMINDER"
            putExtra("is_test", true)
        }
        context.sendBroadcast(intent)
    }
}

class DailyPrayerReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "daily_prayer_notification_channel"
        const val NOTIFICATION_ID = 9042
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isTest = intent.getBooleanExtra("is_test", false)
        val prefsManager = PreferencesManager(context)
        val settings = prefsManager.settings.value

        if (!settings.dailyPrayerReminderEnabled && !isTest) return

        if (!isTest) {
            DailyPrayerReminderScheduler.scheduleDailyReminder(
                context,
                settings.dailyPrayerReminderHour,
                settings.dailyPrayerReminderMinute,
                settings.dailyPrayerReminderEnabled
            )
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "दैनिक प्रार्थना व प्रेरक वचन (Daily Prayer & Verses)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "प्रतिदिन प्रेरक बाइबल वचन एवं विशेष प्रार्थना अनुस्मारक"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val prayer = DailyPrayerRepository.getTodayPrayer()

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_daily_prayer", true)
            putExtra("EXTRA_OPEN_DAILY_PRAYER", true)
            putExtra("daily_prayer_id", prayer.id)
            putExtra("EXTRA_DAILY_PRAYER_ID", prayer.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val snippetPrayer = if (prayer.prayerHindi.length > 140) {
            prayer.prayerHindi.take(137) + "..."
        } else prayer.prayerHindi

        val bigText = buildString {
            append("📖 प्रेरक वचन (${prayer.verseReferenceHindi}):\n")
            append("\"${prayer.verseTextHindi}\"\n\n")
            append("🙏 दैनिक प्रार्थना:\n")
            append(snippetPrayer)
            append("\n\n👉 पूरी प्रार्थना, अंगीकार व मनन देखने के लिए टैप करें।")
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle("🙏 दैनिक प्रार्थना: ${prayer.themeTitleHindi}")
            .setContentText("📖 \"${prayer.verseTextHindi}\" — ${prayer.verseReferenceHindi}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setBigContentTitle("🙏 ${prayer.themeTitleHindi}")
                    .setSummaryText(prayer.verseReferenceHindi)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "🙏 प्रार्थना करें (Open)", pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notif)
    }
}
