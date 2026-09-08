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

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Upcoming Fellowship Event"
        val subtitle = intent.getStringExtra("subtitle") ?: "An event is starting soon!"
        val postId = intent.getStringExtra("postId") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming Vinay Kumar AVJ Fellowship Events"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_post_id", postId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            postId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("📅 $title")
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(postId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "event_reminders_channel"
    }
}

object ReminderScheduler {
    enum class ReminderOffset(val label: String, val millisBefore: Long) {
        ONE_DAY("1 Day Before", 24 * 60 * 60 * 1000L),
        THREE_HOURS("3 Hours Before", 3 * 60 * 60 * 1000L),
        ONE_HOUR("1 Hour Before", 60 * 60 * 1000L)
    }

    fun scheduleReminder(
        context: Context,
        postId: String,
        eventTitle: String,
        eventDate: String,
        eventTimestamp: Long,
        offset: ReminderOffset
    ): Boolean {
        val triggerTime = eventTimestamp - offset.millisBefore
        if (triggerTime <= System.currentTimeMillis()) {
            return false // In the past
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("title", eventTitle)
            putExtra("subtitle", "$eventTitle is starting in ${offset.label.lowercase()} on $eventDate.")
            putExtra("postId", postId)
        }

        val requestCode = (postId + offset.name).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            return true
        } catch (e: SecurityException) {
            // In Android 12+, exact alarm permission might not be granted, fallback to inexact
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                return true
            } catch (ex: Exception) {
                return false
            }
        } catch (e: Exception) {
            return false
        }
    }
}
