package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationEntity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AppFCMService"
        const val CHANNEL_ID = "fcm_notifications_channel"
        const val CHANNEL_NAME = "सूचना एवं घोषणाएं (Notices & Announcements)"

        fun initialize(context: Context? = null) {
            try {
                FirebaseMessaging.getInstance().isAutoInitEnabled = false
            } catch (e: Exception) {
                Log.w(TAG, "FCM init check: ${e.message}")
            }
        }

        fun subscribeToDefaultTopics(context: Context? = null) {
            try {
                if (context != null) {
                    val availability = GoogleApiAvailability.getInstance()
                    val resultCode = availability.isGooglePlayServicesAvailable(context)
                    if (resultCode != ConnectionResult.SUCCESS) {
                        Log.w(TAG, "Google Play Services not available (code: $resultCode), skipping FCM topic subscription")
                        return
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize FCM topic subscription: ${e.message}")
            }
        }

        private fun subscribeToTopicsInternal() {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("all")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to 'all' topic")
                        }
                    }
                FirebaseMessaging.getInstance().subscribeToTopic("notices")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to 'notices' topic")
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to subscribe to FCM topics: ${e.message}")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        subscribeToTopicsInternal()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received: ${remoteMessage.messageId}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "नई सूचना (New Notice)"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: ""

        val type = remoteMessage.data["type"] ?: "fcm"
        val linkUrl = remoteMessage.data["link_url"] ?: remoteMessage.data["url"]
        val timestamp = if (remoteMessage.sentTime > 0) remoteMessage.sentTime else System.currentTimeMillis()

        // 1. Intercept and permanently save incoming FCM message to local Room database
        val entity = NotificationEntity(
            remoteMessageId = remoteMessage.messageId,
            title = title,
            body = body,
            timestamp = timestamp,
            isRead = false,
            type = type,
            linkUrl = linkUrl
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getInstance(applicationContext).notificationDao().insertNotification(entity)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving FCM notification to Room database", e)
            }
        }

        // 2. Display push notification in system tray
        showSystemNotification(title, body)
    }

    private fun showSystemNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "प्रशासनिक एवं सामान्य सूचनाएं"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_tab", "HOME")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notifId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notifId, notificationBuilder.build())
    }
}
