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

/**
 * Foreground service for background video audio playback control notification.
 * Allows user to control play/pause or return directly to the app.
 */
class VideoPlaybackForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "video_playback_channel"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START_SERVICE = "com.example.util.action.START_VIDEO_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.util.action.STOP_VIDEO_SERVICE"
        const val ACTION_UPDATE_NOTIFICATION = "com.example.util.action.UPDATE_VIDEO_NOTIFICATION"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.util.action.VIDEO_TOGGLE_PLAY_PAUSE"

        const val EXTRA_VIDEO_ID = "extra_video_id"
        const val EXTRA_TITLE = "extra_video_title"
        const val EXTRA_CHANNEL = "extra_video_channel"
        const val EXTRA_IS_PLAYING = "extra_video_is_playing"

        fun startService(context: Context, videoId: String, title: String, channel: String, isPlaying: Boolean) {
            val intent = Intent(context, VideoPlaybackForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
                putExtra(EXTRA_VIDEO_ID, videoId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CHANNEL, channel)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun updateNotification(context: Context, videoId: String, title: String, channel: String, isPlaying: Boolean) {
            val intent = Intent(context, VideoPlaybackForegroundService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
                putExtra(EXTRA_VIDEO_ID, videoId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CHANNEL, channel)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VideoPlaybackForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var currentVideoId: String = ""
    private var currentTitle: String = "YouTube Video"
    private var currentChannel: String = "Now Playing"
    private var isPlaying: Boolean = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START_SERVICE -> {
                currentVideoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: ""
                currentTitle = intent.getStringExtra(EXTRA_TITLE) ?: "YouTube Video"
                currentChannel = intent.getStringExtra(EXTRA_CHANNEL) ?: "Now Playing"
                isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            ACTION_UPDATE_NOTIFICATION -> {
                val newId = intent.getStringExtra(EXTRA_VIDEO_ID)
                if (!newId.isNullOrBlank()) currentVideoId = newId
                val newTitle = intent.getStringExtra(EXTRA_TITLE)
                if (!newTitle.isNullOrBlank()) currentTitle = newTitle
                val newChannel = intent.getStringExtra(EXTRA_CHANNEL)
                if (!newChannel.isNullOrBlank()) currentChannel = newChannel
                isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, isPlaying)
                updateNotificationManager()
            }
            ACTION_TOGGLE_PLAY_PAUSE -> {
                isPlaying = !isPlaying
                if (currentVideoId.isNotBlank()) {
                    VideoPlaybackTracker.setPlaying(currentVideoId, isPlaying)
                    GlobalVideoPlayerState.togglePlayPause()
                }
                updateNotificationManager()
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun updateNotificationManager() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Play/Pause Toggle
        val playPauseIntent = Intent(this, VideoPlaybackForegroundService::class.java).apply {
            action = ACTION_TOGGLE_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            this,
            1,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Close / Stop
        val stopIntent = Intent(this, VideoPlaybackForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentChannel)
            .setSmallIcon(R.mipmap.ic_launcher_church)
            .setContentIntent(contentPendingIntent)
            .setOngoing(isPlaying)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", stopPendingIntent)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls background video playback"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
