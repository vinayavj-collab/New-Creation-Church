package com.example.util

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.PreferencesManager
import com.example.data.model.VerseAlarmContent
import com.example.data.model.VerseAlarmFrequency
import com.example.data.model.VerseAlarmMode
import com.example.data.model.UserSettings
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyPrimary
import java.util.Calendar

object VerseAlarmScheduler {
    const val REQUEST_CODE = 9021

    fun scheduleNextAlarm(context: Context, settings: UserSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, VerseAlarmReceiver::class.java).apply {
            action = "com.example.ACTION_VERSE_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        if (!settings.verseAlarmEnabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val triggerTime: Long = if (settings.verseAlarmFrequency == VerseAlarmFrequency.INTERVAL_HOURS) {
            val intervalMs = settings.verseAlarmIntervalHours * 60 * 60 * 1000L
            System.currentTimeMillis() + intervalMs
        } else {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, settings.verseAlarmHour)
                set(Calendar.MINUTE, settings.verseAlarmMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            calendar.timeInMillis
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                try {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ignored: Exception) {}
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, VerseAlarmReceiver::class.java).apply {
            action = "com.example.ACTION_VERSE_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        alarmManager.cancel(pendingIntent)
    }

    fun triggerTestAlarm(context: Context) {
        val intent = Intent(context, VerseAlarmReceiver::class.java).apply {
            action = "com.example.ACTION_VERSE_ALARM"
            putExtra("is_test", true)
        }
        context.sendBroadcast(intent)
    }

    fun triggerTestNotification(context: Context) {
        VerseAlarmReceiver.createVerseNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "BIBLE")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9024,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val rawVerse = "विश्वास आशा की हुई वस्तुओं का निश्चय, और अनदेखी वस्तुओं का प्रमाण है। - इब्रानियों 11:1"
        val notif = NotificationCompat.Builder(context, VerseAlarmReceiver.CHANNEL_ID)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle("📖 आज का वचन (Daily Verse)")
            .setContentText(rawVerse)
            .setStyle(NotificationCompat.BigTextStyle().bigText(rawVerse))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "📖 बाइबल में पढ़ें", pendingIntent)
            .build()

        notificationManager.notify(9024, notif)
    }
}

class VerseAlarmReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "verse_channel_id"
        const val CHANNEL_NAME = "Daily Verse"

        fun createVerseNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "अलार्म एवं आज का वचन अनुस्मारक"
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isTest = intent.getBooleanExtra("is_test", false)
        val prefsManager = PreferencesManager(context)
        val settings = prefsManager.settings.value

        if (!settings.verseAlarmEnabled && !isTest) return

        // Reschedule next occurrence if not a manual test
        if (!isTest) {
            VerseAlarmScheduler.scheduleNextAlarm(context, settings)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createVerseNotificationChannel(context)

        // Today's synchronized scripture
        val todayVerse = com.example.data.bible.model.VerseOfTheDay.getTodayVerse()
        val rawVerse = "${todayVerse.textHindi} — ${todayVerse.referenceHindi}"
        val cleanVerse = ScriptureSpeechUtils.formatVerseForSpeech(todayVerse)

        // Effective speech volume
        val effectiveSpeechVolume = if (settings.syncGreetingVolumeWithAlarm) {
            settings.alarmVolume
        } else {
            settings.greetingSpeechVolume
        }

        when (settings.verseAlarmMode) {
            VerseAlarmMode.NOTIFICATION_ONLY -> {
                // Show notification only
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("open_tab", "BIBLE")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    9022,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )

                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(com.example.R.mipmap.ic_launcher)
                    .setContentTitle("📖 आज का वचन (Daily Verse)")
                    .setContentText(rawVerse)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(rawVerse))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .addAction(android.R.drawable.ic_menu_view, "📖 बाइबल में पढ़ें", pendingIntent)
                    .build()

                notificationManager.notify(9022, notif)
            }

            VerseAlarmMode.SPEECH_DIRECT -> {
                // Speak directly and show notification
                val speechQueue = mutableListOf<String>()
                if (settings.verseAlarmContent == VerseAlarmContent.GREETING_AND_VERSE) {
                    if (settings.userName.isNotBlank()) {
                        speechQueue.add("${settings.userName.trim()} जी, जय मसीह की")
                    } else {
                        speechQueue.add("जय मसीह की")
                    }
                }
                speechQueue.add("आज का वचन है। $cleanVerse")

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("open_tab", "BIBLE")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    9023,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )

                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(com.example.R.mipmap.ic_launcher)
                    .setContentTitle("📖 आज का वचन (Daily Verse)")
                    .setContentText(rawVerse)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(rawVerse))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                notificationManager.notify(9023, notif)

                // Speak
                WelcomeSpeechManager.getInstance(context).speakDirect(
                    messages = speechQueue,
                    speechPitch = settings.greetingSpeechPitch,
                    speechSpeed = settings.greetingSpeechSpeed,
                    speechVolume = effectiveSpeechVolume,
                    requireForeground = false
                )
            }

            VerseAlarmMode.MUSIC_THEN_SPEECH -> {
                // Launch full screen / alert alarm activity which plays music, then on dismiss speaks
                val alarmIntent = Intent(context, VerseAlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("verse_text", rawVerse)
                }
                context.startActivity(alarmIntent)
            }
        }
    }
}

class VerseAlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }

        val prefsManager = PreferencesManager(this)
        val settings = prefsManager.settings.value
        val verseText = intent.getStringExtra("verse_text") ?: "विश्वास आशा की हुई वस्तुओं का निश्चय है।"

        // Play alarm music / ringtone
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@VerseAlarmActivity, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(settings.alarmVolume, settings.alarmVolume)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // fallback
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VerseAlarmDialogContent(
                        userName = settings.userName,
                        verseText = verseText,
                        onStopAndListen = {
                            stopMusic()
                            val speechQueue = mutableListOf<String>()
                            if (settings.verseAlarmContent == VerseAlarmContent.GREETING_AND_VERSE) {
                                if (settings.userName.isNotBlank()) {
                                    speechQueue.add("${settings.userName.trim()} जी, जय मसीह की")
                                } else {
                                    speechQueue.add("जय मसीह की")
                                }
                            }
                            val cleanVerse = ScriptureSpeechUtils.formatScriptureTextForSpeech(verseText)
                            speechQueue.add("आज का वचन है। $cleanVerse")

                            val effectiveSpeechVolume = if (settings.syncGreetingVolumeWithAlarm) {
                                settings.alarmVolume
                            } else {
                                settings.greetingSpeechVolume
                            }

                            WelcomeSpeechManager.getInstance(applicationContext).speakDirect(
                                messages = speechQueue,
                                speechPitch = settings.greetingSpeechPitch,
                                speechSpeed = settings.greetingSpeechSpeed,
                                speechVolume = effectiveSpeechVolume,
                                requireForeground = false
                            )
                            finish()
                        },
                        onDismissOnly = {
                            stopMusic()
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun stopMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }
}

@Composable
fun VerseAlarmDialogContent(
    userName: String,
    verseText: String,
    onStopAndListen: () -> Unit,
    onDismissOnly: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GoldAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "⏰ आज का वचन अलार्म",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                if (userName.isNotBlank()) {
                    Text(
                        text = "जय मसीह की, $userName जी",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GoldAccent,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = verseText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onStopAndListen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "अलार्म रोकें और वचन सुनें",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismissOnly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("केवल अलार्म बंद करें")
                }
            }
        }
    }
}
