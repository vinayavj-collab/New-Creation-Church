package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class BibleAudioForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "bible_audio_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "com.example.util.action.START_AUDIO_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.util.action.STOP_AUDIO_SERVICE"
        const val ACTION_UPDATE_NOTIFICATION = "com.example.util.action.UPDATE_AUDIO_NOTIFICATION"

        const val EXTRA_TITLE = "extra_audio_title"
        const val EXTRA_SUBTITLE = "extra_audio_subtitle"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        fun startService(context: Context, title: String, subtitle: String, isPlaying: Boolean) {
            val intent = Intent(context, BibleAudioForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SUBTITLE, subtitle)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateNotification(context: Context, title: String, subtitle: String, isPlaying: Boolean) {
            val intent = Intent(context, BibleAudioForegroundService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SUBTITLE, subtitle)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // If service not running, start foreground
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BibleAudioForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_SERVICE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Bible Audio Playback"
        val subtitle = intent?.getStringExtra(EXTRA_SUBTITLE) ?: "पवित्र बाइबिल वाचन जारी है..."
        val isPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, true) ?: true

        val notification = buildNotification(title, subtitle, isPlaying)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun buildNotification(title: String, subtitle: String, isPlaying: Boolean): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = R.mipmap.ic_launcher

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(smallIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bible Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active audio playback controls for Bible reading"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
