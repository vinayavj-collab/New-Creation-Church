package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.bible.model.VerseOfTheDay
import com.example.data.local.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BibleVerseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleWidgetUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleWidgetUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_AUTO_UPDATE_TICK,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                updateAllWidgets(context)
                scheduleWidgetUpdate(context)
            }
            ACTION_REFRESH_VERSE -> {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
                val lastDate = prefs.getString(KEY_LAST_WIDGET_DATE, "") ?: ""
                var currentOffset = prefs.getInt(KEY_VERSE_OFFSET, 0)
                if (lastDate != todayDate) {
                    currentOffset = 0
                }
                prefs.edit()
                    .putString(KEY_LAST_WIDGET_DATE, todayDate)
                    .putInt(KEY_VERSE_OFFSET, currentOffset + 1)
                    .apply()
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_VERSE = "com.example.widget.ACTION_REFRESH_VERSE"
        const val ACTION_AUTO_UPDATE_TICK = "com.example.widget.ACTION_AUTO_UPDATE_TICK"
        private const val PREFS_WIDGET = "bible_verse_widget_prefs"
        private const val KEY_VERSE_OFFSET = "key_verse_offset"
        private const val KEY_LAST_WIDGET_DATE = "key_last_widget_date"

        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val EXTRA_BOOK_ID = "navigate_bible_book_id"
        const val EXTRA_CHAPTER = "navigate_bible_chapter"
        const val EXTRA_VERSE = "navigate_bible_verse"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
            val lastDate = prefs.getString(KEY_LAST_WIDGET_DATE, "") ?: ""
            var manualOffset = prefs.getInt(KEY_VERSE_OFFSET, 0)
            if (lastDate != todayDate) {
                manualOffset = 0
                prefs.edit().putString(KEY_LAST_WIDGET_DATE, todayDate).putInt(KEY_VERSE_OFFSET, 0).apply()
            }
            val prefsManager = PreferencesManager(context)
            val intervalHours = prefsManager.settings.value.widgetAutoChangeIntervalHours
            val baseVerse = VerseOfTheDay.getTodayVerse()
            val dailyVerses = VerseOfTheDay.dailyVerses
            val totalVerses = dailyVerses.size
            val baseIndex = dailyVerses.indexOf(baseVerse).coerceAtLeast(0)
            val autoOffset = if (intervalHours > 0) {
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                currentHour / intervalHours
            } else 0

            val currentIndex = ((baseIndex + autoOffset + manualOffset) % totalVerses + totalVerses) % totalVerses
            val verse = dailyVerses[currentIndex]

            val views = RemoteViews(context.packageName, R.layout.widget_verse_of_the_day)
            views.setTextViewText(R.id.widget_verse_text, verse.textHindi)
            views.setTextViewText(R.id.widget_verse_reference, "— " + verse.referenceHindi)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NAVIGATE_TO, "bible_reader")
                putExtra("isReadingPlanMode", false)
                putExtra(EXTRA_BOOK_ID, verse.bookId)
                putExtra(EXTRA_CHAPTER, verse.chapter)
                putExtra(EXTRA_VERSE, verse.verseNumber)
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_open_hint, openPendingIntent)

            val refreshIntent = Intent(context, BibleVerseWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_VERSE
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId + 1000,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val thisWidget = ComponentName(context, BibleVerseWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun scheduleWidgetUpdate(context: Context, explicitIntervalHours: Int = -1) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intervalHours = if (explicitIntervalHours >= 0) {
                    explicitIntervalHours
                } else {
                    PreferencesManager(context).settings.value.widgetAutoChangeIntervalHours
                }

                val intent = Intent(context, BibleVerseWidgetProvider::class.java).apply {
                    action = ACTION_AUTO_UPDATE_TICK
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    9080,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val calendar = Calendar.getInstance()
                val triggerAtMillis: Long
                if (intervalHours > 0) {
                    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                    val nextIntervalHour = (currentHour / intervalHours + 1) * intervalHours
                    calendar.set(Calendar.HOUR_OF_DAY, nextIntervalHour % 24)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 5)
                    calendar.set(Calendar.MILLISECOND, 0)
                    if (nextIntervalHour >= 24 || calendar.timeInMillis <= System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    triggerAtMillis = calendar.timeInMillis
                } else {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 5)
                    calendar.set(Calendar.MILLISECOND, 0)
                    triggerAtMillis = calendar.timeInMillis
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
