package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.bible.model.VerseOfTheDay

class BibleVerseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_VERSE) {
            // Cycle to next verse in list
            val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
            val currentOffset = prefs.getInt(KEY_VERSE_OFFSET, 0)
            prefs.edit().putInt(KEY_VERSE_OFFSET, currentOffset + 1).apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BibleVerseWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_VERSE = "com.example.widget.ACTION_REFRESH_VERSE"
        private const val PREFS_WIDGET = "bible_verse_widget_prefs"
        private const val KEY_VERSE_OFFSET = "key_verse_offset"

        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val EXTRA_BOOK_ID = "navigate_bible_book_id"
        const val EXTRA_CHAPTER = "navigate_bible_chapter"
        const val EXTRA_VERSE = "navigate_bible_verse"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
            val offset = prefs.getInt(KEY_VERSE_OFFSET, 0)

            val totalVerses = VerseOfTheDay.dailyVerses.size
            val baseVerse = VerseOfTheDay.getTodayVerse()
            val baseIndex = VerseOfTheDay.dailyVerses.indexOf(baseVerse).let { if (it >= 0) it else 0 }
            val currentIndex = (baseIndex + offset) % totalVerses
            val verse = VerseOfTheDay.dailyVerses[currentIndex]

            val views = RemoteViews(context.packageName, R.layout.widget_verse_of_the_day)

            // Set Verse Text and Reference
            views.setTextViewText(R.id.widget_verse_text, verse.textHindi)
            views.setTextViewText(R.id.widget_verse_reference, "— ${verse.referenceHindi}")

            // Intent to open Bible Reader for this exact verse (Explicitly non-reading plan)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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

            // Intent to refresh verse
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
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BibleVerseWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
