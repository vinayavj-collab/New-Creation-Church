package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.MainActivity
import java.util.concurrent.TimeUnit

/**
 * WorkManager CoroutineWorker for triggering local notifications before Fellowship Events start.
 */
class EventReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "EventReminderWorker"
        const val CHANNEL_ID = "fellowship_event_reminders_channel"
        const val CHANNEL_NAME = "Fellowship Event Reminders"

        const val KEY_EVENT_ID = "event_id"
        const val KEY_TITLE = "event_title"
        const val KEY_DATE = "event_date"
        const val KEY_TIME = "event_time"
        const val KEY_LOCATION = "event_location"
        const val KEY_POST_URL = "post_url"
        const val KEY_MINUTES_BEFORE = "minutes_before"
    }

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.success()
        val title = inputData.getString(KEY_TITLE) ?: "Fellowship Event"
        val dateStr = inputData.getString(KEY_DATE) ?: ""
        val timeStr = inputData.getString(KEY_TIME) ?: ""
        val locationStr = inputData.getString(KEY_LOCATION) ?: ""
        val minutesBefore = inputData.getInt(KEY_MINUTES_BEFORE, 60)

        Log.d(TAG, "Triggering reminder notification for event: $title (ID: $eventId)")

        try {
            showNotification(
                eventId = eventId,
                title = title,
                dateStr = dateStr,
                timeStr = timeStr,
                locationStr = locationStr,
                minutesBefore = minutesBefore
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying event reminder notification", e)
        }

        return Result.success()
    }

    private fun showNotification(
        eventId: String,
        title: String,
        dateStr: String,
        timeStr: String,
        locationStr: String,
        minutesBefore: Int
    ) {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies users before scheduled fellowship services and events."
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_event_id", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            eventId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val timeLabel = when {
            minutesBefore >= 1440 -> "${minutesBefore / 1440} दिन में"
            minutesBefore >= 60 -> "${minutesBefore / 60} घंटे में"
            else -> "$minutesBefore मिनट में"
        }

        val subtitle = buildString {
            append("यह संगति $timeLabel ($timeStr) शुरू होने वाली है।")
            if (locationStr.isNotBlank()) {
                append("\n📍 $locationStr")
            }
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("📅 स्मरण: $title")
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify("event_reminder_$eventId".hashCode(), notification)
    }
}

/**
 * Helper object to schedule, cancel, and manage Fellowship Event Reminders via WorkManager.
 */
object EventReminderScheduler {

    fun scheduleWorkReminder(
        context: Context,
        eventId: String,
        title: String,
        dateStr: String,
        timeStr: String,
        locationStr: String,
        startTimestamp: Long,
        minutesBefore: Int = 60,
        postUrl: String = ""
    ): Boolean {
        if (eventId.isBlank() || startTimestamp <= 0L) return false

        val triggerTime = startTimestamp - (minutesBefore * 60 * 1000L)
        val now = System.currentTimeMillis()
        val delayMillis = triggerTime - now

        if (delayMillis < 0) {
            // If reminder time has passed but event hasn't started yet, schedule immediately (1 sec delay)
            if (startTimestamp > now) {
                return enqueueWork(context, eventId, title, dateStr, timeStr, locationStr, 1000L, minutesBefore, postUrl)
            }
            return false // Event already in past
        }

        return enqueueWork(context, eventId, title, dateStr, timeStr, locationStr, delayMillis, minutesBefore, postUrl)
    }

    private fun enqueueWork(
        context: Context,
        eventId: String,
        title: String,
        dateStr: String,
        timeStr: String,
        locationStr: String,
        delayMillis: Long,
        minutesBefore: Int,
        postUrl: String
    ): Boolean {
        try {
            val inputData = workDataOf(
                EventReminderWorker.KEY_EVENT_ID to eventId,
                EventReminderWorker.KEY_TITLE to title,
                EventReminderWorker.KEY_DATE to dateStr,
                EventReminderWorker.KEY_TIME to timeStr,
                EventReminderWorker.KEY_LOCATION to locationStr,
                EventReminderWorker.KEY_MINUTES_BEFORE to minutesBefore,
                EventReminderWorker.KEY_POST_URL to postUrl
            )

            val workRequest = OneTimeWorkRequestBuilder<EventReminderWorker>()
                .setInitialDelay(maxOf(delayMillis, 500L), TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("fellowship_event_reminder")
                .addTag("reminder_$eventId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "work_event_reminder_$eventId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            return true
        } catch (e: Exception) {
            Log.e("EventReminderScheduler", "Failed to enqueue WorkManager reminder", e)
            return false
        }
    }

    fun cancelWorkReminder(context: Context, eventId: String) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork("work_event_reminder_$eventId")
        } catch (e: Exception) {
            Log.e("EventReminderScheduler", "Failed to cancel reminder for $eventId", e)
        }
    }
}
