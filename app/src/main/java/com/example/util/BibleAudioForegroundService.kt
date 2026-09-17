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
        const val ACTION_PLAY_PAUSE = "com.example.util.action.PLAY_PAUSE"
        const val ACTION_PREV_VERSE = "com.example.util.action.PREV_VERSE"
        const val ACTION_NEXT_VERSE = "com.example.util.action.NEXT_VERSE"

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
        val audioManager = BibleAudioManager.getInstance(this)

        when (action) {
            ACTION_STOP_SERVICE -> {
                audioManager.stop()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PLAY_PAUSE -> {
                audioManager.togglePlayPause()
                return START_STICKY
            }
            ACTION_PREV_VERSE -> {
                audioManager.skipPreviousVerse()
                return START_STICKY
            }
            ACTION_NEXT_VERSE -> {
                audioManager.skipNextVerse()
                return START_STICKY
            }
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: audioManager.currentBookName.value.ifBlank { "पवित्र बाइबिल वाचन" }
        val subtitle = intent?.getStringExtra(EXTRA_SUBTITLE) ?: "ऑडियो वाचन जारी है..."
        val isPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, audioManager.isPlaying.value) ?: audioManager.isPlaying.value

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

        val prevIntent = Intent(this, BibleAudioForegroundService::class.java).apply {
            action = ACTION_PREV_VERSE
        }
        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, BibleAudioForegroundService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, BibleAudioForegroundService::class.java).apply {
            action = ACTION_NEXT_VERSE
        }
        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BibleAudioForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = R.mipmap.ic_launcher
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseText = if (isPlaying) "रोकें (Pause)" else "चलाएं (Play)"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(smallIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "पिछला", prevPendingIntent)
            .addAction(playPauseIcon, playPauseText, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "अगला", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "बंद करें", stopPendingIntent)

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
