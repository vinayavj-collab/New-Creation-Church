package com.example.util

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for Firebase Remote Config providing dynamic runtime parameters
 * such as search button visibility and dynamic notice headings.
 */
object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"

    const val KEY_IS_SEARCH_ENABLED = "is_search_enabled"
    const val KEY_IS_VLOG_SERVER_ENABLED = "is_vlog_server_enabled"
    const val KEY_APP_NOTICE_HEADING = "app_notice_heading"
    const val KEY_DAILY_GREETING_TEXT = RemoteConfigHelper.KEY_DAILY_GREETING_TEXT
    const val KEY_VERSE_OF_THE_DAY_TEXT = RemoteConfigHelper.KEY_VERSE_OF_THE_DAY_TEXT
    const val KEY_SPECIAL_ANNOUNCEMENT_TEXT = RemoteConfigHelper.KEY_SPECIAL_ANNOUNCEMENT_TEXT

    private const val DEFAULT_NOTICE_HEADING = "महत्वपूर्ण सूचना"

    val isVlogServerEnabled: StateFlow<Boolean> = RemoteConfigHelper.isVlogServerEnabled
    val dailyGreetingText: StateFlow<String> = RemoteConfigHelper.dailyGreetingText
    val verseOfTheDayText: StateFlow<String> = RemoteConfigHelper.verseOfTheDayText
    val specialAnnouncementText: StateFlow<String> = RemoteConfigHelper.specialAnnouncementText

    private val _isSearchEnabled = MutableStateFlow(true)
    val isSearchEnabled: StateFlow<Boolean> = _isSearchEnabled.asStateFlow()

    private val _appNoticeHeading = MutableStateFlow(DEFAULT_NOTICE_HEADING)
    val appNoticeHeading: StateFlow<String> = _appNoticeHeading.asStateFlow()

    @Volatile
    private var remoteConfigInstance: FirebaseRemoteConfig? = null

    fun getRemoteConfig(): FirebaseRemoteConfig? = remoteConfigInstance

    /**
     * Getter method to check if search features/buttons are enabled via Remote Config.
     */
    fun isSearchEnabled(): Boolean {
        return try {
            remoteConfigInstance?.getBoolean(KEY_IS_SEARCH_ENABLED) ?: _isSearchEnabled.value
        } catch (e: Exception) {
            _isSearchEnabled.value
        }
    }

    /**
     * Getter method to get the dynamic notice heading configured via Remote Config.
     */
    fun getAppNoticeHeading(): String {
        return try {
            val heading = remoteConfigInstance?.getString(KEY_APP_NOTICE_HEADING)
            if (!heading.isNullOrBlank()) heading else _appNoticeHeading.value
        } catch (e: Exception) {
            _appNoticeHeading.value
        }
    }

    fun getDailyGreetingText(): String {
        return try {
            val greeting = remoteConfigInstance?.getString(KEY_DAILY_GREETING_TEXT)
            if (!greeting.isNullOrBlank()) greeting else dailyGreetingText.value
        } catch (e: Exception) {
            dailyGreetingText.value
        }
    }

    fun getVerseOfTheDayText(): String {
        return try {
            val verse = remoteConfigInstance?.getString(KEY_VERSE_OF_THE_DAY_TEXT)
            if (!verse.isNullOrBlank()) verse else verseOfTheDayText.value
        } catch (e: Exception) {
            verseOfTheDayText.value
        }
    }

    fun getSpecialAnnouncementText(): String {
        return try {
            remoteConfigInstance?.getString(KEY_SPECIAL_ANNOUNCEMENT_TEXT) ?: specialAnnouncementText.value
        } catch (e: Exception) {
            specialAnnouncementText.value
        }
    }

    /**
     * Initializes FirebaseRemoteConfig with minimum fetch interval and defaults.
     */
    fun initialize(context: Context, minFetchIntervalSeconds: Long = 0L): FirebaseRemoteConfig? {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            remoteConfigInstance = remoteConfig

            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(minFetchIntervalSeconds)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

            // Populate initial values from cache/defaults
            updateLocalState(remoteConfig)

            fetchAndActivate()
            return remoteConfig
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing FirebaseRemoteConfig: ${e.message}")
            return null
        }
    }

    /**
     * Asynchronously fetches and activates Remote Config parameters.
     */
    fun fetchAndActivate(onComplete: ((Boolean) -> Unit)? = null) {
        val config = remoteConfigInstance ?: run {
            Log.w(TAG, "RemoteConfig not initialized yet")
            onComplete?.invoke(false)
            return
        }

        try {
            config.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val isUpdated = task.result
                        updateLocalState(config)
                        val searchEnabled = isSearchEnabled()
                        val noticeHeading = getAppNoticeHeading()
                        Log.i(
                            TAG,
                            "Remote Config fetchAndActivate succeeded (isUpdated=$isUpdated). " +
                                "is_search_enabled=$searchEnabled, app_notice_heading='$noticeHeading'"
                        )
                        onComplete?.invoke(true)
                    } else {
                        Log.w(TAG, "Remote Config fetchAndActivate failed: ${task.exception?.message}")
                        onComplete?.invoke(false)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during fetchAndActivate: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    fun onConfigUpdated(remoteConfig: FirebaseRemoteConfig) {
        remoteConfigInstance = remoteConfig
        RemoteConfigHelper.onConfigUpdated(remoteConfig)
        updateLocalState(remoteConfig)
    }

    private fun updateLocalState(config: FirebaseRemoteConfig) {
        try {
            _isSearchEnabled.value = config.getBoolean(KEY_IS_SEARCH_ENABLED)
            val heading = config.getString(KEY_APP_NOTICE_HEADING)
            if (heading.isNotBlank()) {
                _appNoticeHeading.value = heading
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating local state from RemoteConfig: ${e.message}")
        }
    }
}
