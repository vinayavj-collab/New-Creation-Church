package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vinay_app_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val themeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        val vlogModeStr = prefs.getString("personal_vlog_mode", PersonalVlogMode.HIDDEN.name) ?: PersonalVlogMode.HIDDEN.name
        val personalVlogMode = try {
            PersonalVlogMode.valueOf(vlogModeStr)
        } catch (e: Exception) {
            PersonalVlogMode.HIDDEN
        }

        val ytTabStr = prefs.getString("youtube_default_tab", YouTubeDefaultTab.AVJ_WORSHIP.name) ?: YouTubeDefaultTab.AVJ_WORSHIP.name
        val youtubeDefaultTab = try {
            YouTubeDefaultTab.valueOf(ytTabStr)
        } catch (e: Exception) {
            YouTubeDefaultTab.AVJ_WORSHIP
        }

        val bibleStyleStr = prefs.getString("bible_reading_style", BibleReadingStyle.PRINTED_BIBLE.name) ?: BibleReadingStyle.PRINTED_BIBLE.name
        val bibleReadingStyle = try {
            BibleReadingStyle.valueOf(bibleStyleStr)
        } catch (e: Exception) {
            BibleReadingStyle.PRINTED_BIBLE
        }

        val langStr = prefs.getString("app_language", AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
        val appLanguage = try {
            AppLanguage.valueOf(langStr)
        } catch (e: Exception) {
            AppLanguage.SYSTEM
        }

        val favCats = prefs.getStringSet("favorite_categories", emptySet()) ?: emptySet()

        // Home sections enabled (Today's Scripture at top)
        val defaultEnabled = setOf(
            HomeSectionType.TODAYS_VERSE.id,
            HomeSectionType.UPCOMING_EVENTS.id,
            HomeSectionType.FELLOWSHIP_EVENTS.id,
            HomeSectionType.LATEST_VIDEOS.id,
            HomeSectionType.PLAYLISTS.id,
            HomeSectionType.PHOTOS.id,
            HomeSectionType.LATEST_EVENTS.id
        )
        val enabledStrings = prefs.getStringSet("enabled_home_sections", defaultEnabled) ?: defaultEnabled
        val enabledHomeSections = enabledStrings.mapNotNull { id ->
            HomeSectionType.entries.find { it.id == id }
        }.toSet()

        // Order
        val orderStr = prefs.getString("home_sections_order", null)
        val homeSectionsOrder = if (orderStr.isNullOrBlank()) {
            listOf(
                HomeSectionType.TODAYS_VERSE,
                HomeSectionType.UPCOMING_EVENTS,
                HomeSectionType.FELLOWSHIP_EVENTS,
                HomeSectionType.LATEST_VIDEOS,
                HomeSectionType.PLAYLISTS,
                HomeSectionType.PHOTOS,
                HomeSectionType.LATEST_EVENTS,
                HomeSectionType.PERSONAL_VLOG
            )
        } else {
            val list = orderStr.split(",").mapNotNull { id ->
                HomeSectionType.entries.find { it.id == id }
            }.toMutableList()
            // Add any missing
            HomeSectionType.entries.forEach { if (!list.contains(it)) list.add(it) }
            list
        }

        val showVlog = personalVlogMode != PersonalVlogMode.HIDDEN

        return UserSettings(
            themeMode = themeMode,
            showFellowshipEvents = prefs.getBoolean("show_fellowship_events", true),
            personalVlogMode = personalVlogMode,
            showPersonalVlog = showVlog,
            showYouTube = prefs.getBoolean("show_youtube", true),
            showShorts = prefs.getBoolean("show_shorts", true),
            youtubeDefaultTab = youtubeDefaultTab,
            bibleReadingStyle = bibleReadingStyle,
            dataSaverEnabled = prefs.getBoolean("data_saver_enabled", false),
            notifyTodaysScripture = prefs.getBoolean("notify_todays_scripture", true),
            notifyReadingPlan = prefs.getBoolean("notify_reading_plan", true),
            notifyFellowshipEvents = prefs.getBoolean("notify_fellowship_events", true),
            notifyYouTube = prefs.getBoolean("notify_youtube", true),
            notifyPersonalVlog = prefs.getBoolean("notify_personal_vlog", false),
            notifyUpcomingReminders = prefs.getBoolean("notify_upcoming_reminders", true),
            appLanguage = appLanguage,
            favoriteCategories = favCats,
            homeSectionsOrder = homeSectionsOrder,
            enabledHomeSections = enabledHomeSections,
            lastReadPostId = prefs.getString("last_read_post_id", null)
        )
    }

    fun updateThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun updatePersonalVlogMode(mode: PersonalVlogMode) {
        prefs.edit().putString("personal_vlog_mode", mode.name).apply()
        val showVlog = mode != PersonalVlogMode.HIDDEN
        prefs.edit().putBoolean("show_personal_vlog", showVlog).apply()
        _settings.value = _settings.value.copy(
            personalVlogMode = mode,
            showPersonalVlog = showVlog
        )
    }

    fun updateYouTubeDefaultTab(tab: YouTubeDefaultTab) {
        prefs.edit().putString("youtube_default_tab", tab.name).apply()
        _settings.value = _settings.value.copy(youtubeDefaultTab = tab)
    }

    fun updateBibleReadingStyle(style: BibleReadingStyle) {
        prefs.edit().putString("bible_reading_style", style.name).apply()
        _settings.value = _settings.value.copy(bibleReadingStyle = style)
    }

    fun updateDataSaverEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("data_saver_enabled", enabled).apply()
        _settings.value = _settings.value.copy(dataSaverEnabled = enabled)
    }

    fun updateAppLanguage(lang: AppLanguage) {
        prefs.edit().putString("app_language", lang.name).apply()
        _settings.value = _settings.value.copy(appLanguage = lang)
    }

    fun updateFavoriteCategories(categories: Set<String>) {
        prefs.edit().putStringSet("favorite_categories", categories).apply()
        _settings.value = _settings.value.copy(favoriteCategories = categories)
    }

    fun updateShowFellowshipEvents(enabled: Boolean) {
        prefs.edit().putBoolean("show_fellowship_events", enabled).apply()
        _settings.value = _settings.value.copy(showFellowshipEvents = enabled)
    }

    fun updateShowPersonalVlog(enabled: Boolean) {
        val mode = if (enabled) PersonalVlogMode.SECONDARY else PersonalVlogMode.HIDDEN
        updatePersonalVlogMode(mode)
    }

    fun updateShowYouTube(enabled: Boolean) {
        prefs.edit().putBoolean("show_youtube", enabled).apply()
        _settings.value = _settings.value.copy(showYouTube = enabled)
    }

    fun updateShowShorts(enabled: Boolean) {
        prefs.edit().putBoolean("show_shorts", enabled).apply()
        _settings.value = _settings.value.copy(showShorts = enabled)
    }

    fun updateNotifyTodaysScripture(enabled: Boolean) {
        prefs.edit().putBoolean("notify_todays_scripture", enabled).apply()
        _settings.value = _settings.value.copy(notifyTodaysScripture = enabled)
    }

    fun updateNotifyReadingPlan(enabled: Boolean) {
        prefs.edit().putBoolean("notify_reading_plan", enabled).apply()
        _settings.value = _settings.value.copy(notifyReadingPlan = enabled)
    }

    fun updateNotifyFellowshipEvents(enabled: Boolean) {
        prefs.edit().putBoolean("notify_fellowship_events", enabled).apply()
        _settings.value = _settings.value.copy(notifyFellowshipEvents = enabled)
    }

    fun updateNotifyYouTube(enabled: Boolean) {
        prefs.edit().putBoolean("notify_youtube", enabled).apply()
        _settings.value = _settings.value.copy(notifyYouTube = enabled)
    }

    fun updateNotifyPersonalVlog(enabled: Boolean) {
        prefs.edit().putBoolean("notify_personal_vlog", enabled).apply()
        _settings.value = _settings.value.copy(notifyPersonalVlog = enabled)
    }

    fun updateNotifyUpcomingReminders(enabled: Boolean) {
        prefs.edit().putBoolean("notify_upcoming_reminders", enabled).apply()
        _settings.value = _settings.value.copy(notifyUpcomingReminders = enabled)
    }

    fun toggleHomeSection(section: HomeSectionType, enabled: Boolean) {
        val current = _settings.value.enabledHomeSections.toMutableSet()
        if (enabled) current.add(section) else current.remove(section)
        prefs.edit().putStringSet("enabled_home_sections", current.map { it.id }.toSet()).apply()
        _settings.value = _settings.value.copy(enabledHomeSections = current)
    }

    fun updateHomeSectionsOrder(order: List<HomeSectionType>) {
        val orderStr = order.joinToString(",") { it.id }
        prefs.edit().putString("home_sections_order", orderStr).apply()
        _settings.value = _settings.value.copy(homeSectionsOrder = order)
    }

    fun setLastReadPostId(postId: String) {
        prefs.edit().putString("last_read_post_id", postId).apply()
        _settings.value = _settings.value.copy(lastReadPostId = postId)
    }
}

