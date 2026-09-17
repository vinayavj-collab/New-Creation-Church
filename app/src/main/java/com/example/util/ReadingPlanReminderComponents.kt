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
import java.util.Calendar

object ReadingPlanReminderScheduler {
    const val REQUEST_CODE = 9031

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int, enabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReadingPlanReminderReceiver::class.java).apply {
            action = "com.example.ACTION_READING_PLAN_REMINDER"
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
        val intent = Intent(context, ReadingPlanReminderReceiver::class.java).apply {
            action = "com.example.ACTION_READING_PLAN_REMINDER"
            putExtra("is_test", true)
        }
        context.sendBroadcast(intent)
    }
}

class ReadingPlanReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "reading_plan_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isTest = intent.getBooleanExtra("is_test", false)
        val prefsManager = PreferencesManager(context)
        val settings = prefsManager.settings.value

        if (!settings.readingPlanReminderEnabled && !isTest) return

        if (!isTest) {
            ReadingPlanReminderScheduler.scheduleDailyReminder(
                context,
                settings.readingPlanReminderHour,
                settings.readingPlanReminderMinute,
                settings.readingPlanReminderEnabled
            )
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "रीडिंग प्लान अनुस्मारक (Reading Plan)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "दैनिक बाइबल अध्ययन व रीडिंग प्लान अनुस्मारक"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "BIBLE")
            putExtra("open_reading_plan", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9032,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle("📖 दैनिक बाइबल रीडिंग प्लान")
            .setContentText("आज का बाइबल पाठ पूरा करने का समय हो गया है। आइए परमेश्वर के वचन का मनन करें!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("आज का बाइबल पाठ पूरा करने का समय हो गया है। आइए परमेश्वर के वचन का मनन करें!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "📖 प्लान खोलें", pendingIntent)
            .build()

        notificationManager.notify(9032, notif)
    }
}
