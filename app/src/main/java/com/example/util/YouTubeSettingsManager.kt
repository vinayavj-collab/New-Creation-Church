package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object YouTubeSettingsManager {
    private const val PREFS_NAME = "youtube_settings_prefs"
    private const val KEY_HIDE_TITLE = "hide_title_share"
    private const val KEY_ENABLE_PIP = "enable_background_pip"
    private const val KEY_AUTOPLAY = "autoplay_next"
    private const val KEY_SHOW_CONTROLS = "show_controls"
    private const val KEY_QUALITY = "selected_quality"

    private val _settings = MutableStateFlow(YouTubePlayerSettings())
    val settings: StateFlow<YouTubePlayerSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hideTitle = prefs.getBoolean(KEY_HIDE_TITLE, true)
        val enablePip = prefs.getBoolean(KEY_ENABLE_PIP, true)
        val autoplay = prefs.getBoolean(KEY_AUTOPLAY, true)
        val showControls = prefs.getBoolean(KEY_SHOW_CONTROLS, true)
        val quality = prefs.getString(KEY_QUALITY, "auto") ?: "auto"
        _settings.value = YouTubePlayerSettings(hideTitle, enablePip, autoplay, showControls, quality)
    }

    fun updateSettings(context: Context, newSettings: YouTubePlayerSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_HIDE_TITLE, newSettings.hideTitleAndShare)
            .putBoolean(KEY_ENABLE_PIP, newSettings.enableBackgroundPip)
            .putBoolean(KEY_AUTOPLAY, newSettings.autoplayNext)
            .putBoolean(KEY_SHOW_CONTROLS, newSettings.showControls)
            .putString(KEY_QUALITY, newSettings.selectedQuality)
            .apply()
        _settings.value = newSettings
    }
}
