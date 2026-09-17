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
 * Universal Remote Config Foundation & Vlog Master Override Helper.
 * Manages Firebase Remote Config parameter fetching, safe flag lookups with default TRUE fallback,
 * and dual-layer kill switch evaluation for Personal Vlog (Dailymotion ID x4sr8o4).
 */
object RemoteConfigHelper {
    private const val TAG = "RemoteConfigHelper"

    // Remote Config Keys
    const val KEY_IS_VLOG_SERVER_ENABLED = "is_vlog_server_enabled"
    const val KEY_IS_SEARCH_ENABLED = "is_search_enabled"
    const val KEY_APP_NOTICE_HEADING = "app_notice_heading"
    const val KEY_DAILY_GREETING_TEXT = "daily_greeting_text"
    const val KEY_VERSE_OF_THE_DAY_TEXT = "verse_of_the_day_text"
    const val KEY_SPECIAL_ANNOUNCEMENT_TEXT = "special_announcement_text"
    const val KEY_LATEST_VERSION_NAME = "latest_version_name"
    const val KEY_LATEST_VERSION_CODE = "latest_version_code"
    const val KEY_UPDATE_APK_URL = "update_apk_url"
    const val KEY_PERSONAL_VLOG_PASSWORD = "personal_vlog_password"
    const val KEY_PRIVATE_PROFILE_PASSWORD = "private_profile_password"
    const val KEY_PRIVATE_PROFILE_ENABLED = "private_profile_enabled"

    // Default Fallbacks (TRUE to ensure app functionality when offline or unconfigured)
    private const val DEFAULT_VLOG_SERVER_ENABLED = true
    private const val DEFAULT_SEARCH_ENABLED = true
    private const val DEFAULT_NOTICE_HEADING = "महत्वपूर्ण सूचना"
    private const val DEFAULT_DAILY_GREETING = "जय मसीह की"
    private const val DEFAULT_VERSE_OF_THE_DAY = "यहोवा मेरा चरवाहा है, मुझे कोई घटी न होगी।"
    private const val DEFAULT_SPECIAL_ANNOUNCEMENT = ""
    private const val DEFAULT_LATEST_VERSION_NAME = ""
    private const val DEFAULT_LATEST_VERSION_CODE = 0
    private const val DEFAULT_UPDATE_APK_URL = ""
    const val DEFAULT_PERSONAL_VLOG_PASSWORD = "9479"
    const val DEFAULT_PRIVATE_PROFILE_PASSWORD = "Vin@122333"
    private const val DEFAULT_PRIVATE_PROFILE_ENABLED = true

    private val _isVlogServerEnabled = MutableStateFlow(DEFAULT_VLOG_SERVER_ENABLED)
    val isVlogServerEnabled: StateFlow<Boolean> = _isVlogServerEnabled.asStateFlow()

    private val _personalVlogPassword = MutableStateFlow(DEFAULT_PERSONAL_VLOG_PASSWORD)
    val personalVlogPassword: StateFlow<String> = _personalVlogPassword.asStateFlow()

    private val _privateProfilePassword = MutableStateFlow(DEFAULT_PRIVATE_PROFILE_PASSWORD)
    val privateProfilePassword: StateFlow<String> = _privateProfilePassword.asStateFlow()

    private val _isPrivateProfileEnabled = MutableStateFlow(DEFAULT_PRIVATE_PROFILE_ENABLED)
    val isPrivateProfileEnabled: StateFlow<Boolean> = _isPrivateProfileEnabled.asStateFlow()

    private val _isSearchEnabled = MutableStateFlow(DEFAULT_SEARCH_ENABLED)
    val isSearchEnabled: StateFlow<Boolean> = _isSearchEnabled.asStateFlow()

    private val _appNoticeHeading = MutableStateFlow(DEFAULT_NOTICE_HEADING)
    val appNoticeHeading: StateFlow<String> = _appNoticeHeading.asStateFlow()

    private val _dailyGreetingText = MutableStateFlow(DEFAULT_DAILY_GREETING)
    val dailyGreetingText: StateFlow<String> = _dailyGreetingText.asStateFlow()

    private val _verseOfTheDayText = MutableStateFlow(DEFAULT_VERSE_OF_THE_DAY)
    val verseOfTheDayText: StateFlow<String> = _verseOfTheDayText.asStateFlow()

    private val _specialAnnouncementText = MutableStateFlow(DEFAULT_SPECIAL_ANNOUNCEMENT)
    val specialAnnouncementText: StateFlow<String> = _specialAnnouncementText.asStateFlow()

    private val _latestVersionName = MutableStateFlow(DEFAULT_LATEST_VERSION_NAME)
    val latestVersionName: StateFlow<String> = _latestVersionName.asStateFlow()

    private val _latestVersionCode = MutableStateFlow(DEFAULT_LATEST_VERSION_CODE)
    val latestVersionCode: StateFlow<Int> = _latestVersionCode.asStateFlow()

    private val _updateApkUrl = MutableStateFlow(DEFAULT_UPDATE_APK_URL)
    val updateApkUrl: StateFlow<String> = _updateApkUrl.asStateFlow()

    @Volatile
    private var remoteConfigInstance: FirebaseRemoteConfig? = null

    /**
     * Initializes Firebase Remote Config with a low fetch interval (0s / 60s)
     * for immediate live updates from Firebase Console.
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

            // Sync initial values from cache/defaults
            updateLocalState(remoteConfig)

            fetchAndActivate()
            return remoteConfig
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing RemoteConfigHelper: ${e.message}")
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
                        Log.i(
                            TAG,
                            "RemoteConfig fetchAndActivate success (updated=$isUpdated). " +
                                "is_vlog_server_enabled=${_isVlogServerEnabled.value}, " +
                                "is_search_enabled=${_isSearchEnabled.value}"
                        )
                        onComplete?.invoke(true)
                    } else {
                        Log.w(TAG, "RemoteConfig fetchAndActivate failed: ${task.exception?.message}")
                        onComplete?.invoke(false)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during fetchAndActivate: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * Universal Kill Switch: Safely fetch any boolean flag.
     * Defaults to [defaultValue] (default true) if offline or on exception.
     */
    fun getBoolean(key: String, defaultValue: Boolean = true): Boolean {
        return try {
            val config = remoteConfigInstance
            if (config != null) {
                config.getBoolean(key)
            } else {
                when (key) {
                    KEY_IS_VLOG_SERVER_ENABLED -> _isVlogServerEnabled.value
                    KEY_IS_SEARCH_ENABLED -> _isSearchEnabled.value
                    else -> defaultValue
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading boolean key '$key', falling back to $defaultValue: ${e.message}")
            defaultValue
        }
    }

    /**
     * Condition B (Server): Is Personal Vlog enabled remotely via Firebase Remote Config.
     * Defaulting to TRUE if offline/unavailable.
     */
    fun isVlogServerEnabled(): Boolean {
        return getBoolean(KEY_IS_VLOG_SERVER_ENABLED, DEFAULT_VLOG_SERVER_ENABLED)
    }

    fun getPersonalVlogPassword(): String {
        return try {
            val remotePass = remoteConfigInstance?.getString(KEY_PERSONAL_VLOG_PASSWORD)?.trim().orEmpty()
            if (remotePass.isNotBlank()) remotePass else _personalVlogPassword.value
        } catch (e: Exception) {
            _personalVlogPassword.value
        }
    }

    fun getPrivateProfilePassword(): String {
        return try {
            val remotePass = remoteConfigInstance?.getString(KEY_PRIVATE_PROFILE_PASSWORD)?.trim().orEmpty()
            if (remotePass.isNotBlank()) remotePass else _privateProfilePassword.value
        } catch (e: Exception) {
            _privateProfilePassword.value
        }
    }

    fun isPrivateProfileEnabled(): Boolean {
        return getBoolean(KEY_PRIVATE_PROFILE_ENABLED, DEFAULT_PRIVATE_PROFILE_ENABLED)
    }

    /**
     * Master Override Logic:
     * Evaluates TWO conditions for Personal Vlog (Dailymotion ID x4sr8o4) content:
     * - Condition A (Local): [isVlogLocallyEnabled] (Boolean from SharedPreferences / App Settings)
     * - Condition B (Server): [isVlogServerEnabled] (Boolean from Firebase Remote Config & Database)
     *
     * Returns TRUE ONLY IF both conditions are true (`Condition A && Condition B`).
     * Firebase acts as the Supreme Master: If Condition B is FALSE, vlog content is completely suppressed.
     */
    fun isPersonalVlogAllowed(isVlogLocallyEnabled: Boolean): Boolean {
        val conditionA = isVlogLocallyEnabled
        val conditionB = isVlogServerEnabled()
        val isAllowed = conditionA && conditionB
        return isAllowed
    }

    fun onConfigUpdated(remoteConfig: FirebaseRemoteConfig) {
        remoteConfigInstance = remoteConfig
        updateLocalState(remoteConfig)
    }

    private fun updateLocalState(config: FirebaseRemoteConfig) {
        try {
            _isVlogServerEnabled.value = config.getBoolean(KEY_IS_VLOG_SERVER_ENABLED)
            _isSearchEnabled.value = config.getBoolean(KEY_IS_SEARCH_ENABLED)
            _isPrivateProfileEnabled.value = config.getBoolean(KEY_PRIVATE_PROFILE_ENABLED)
            val vlogPass = config.getString(KEY_PERSONAL_VLOG_PASSWORD)
            if (vlogPass.isNotBlank()) {
                _personalVlogPassword.value = vlogPass
            }
            val profilePass = config.getString(KEY_PRIVATE_PROFILE_PASSWORD)
            if (profilePass.isNotBlank()) {
                _privateProfilePassword.value = profilePass
            }
            val heading = config.getString(KEY_APP_NOTICE_HEADING)
            if (heading.isNotBlank()) {
                _appNoticeHeading.value = heading
            }
            val greeting = config.getString(KEY_DAILY_GREETING_TEXT)
            if (greeting.isNotBlank()) {
                _dailyGreetingText.value = greeting
            }
            val verse = config.getString(KEY_VERSE_OF_THE_DAY_TEXT)
            if (verse.isNotBlank()) {
                _verseOfTheDayText.value = verse
            }
            val announcement = config.getString(KEY_SPECIAL_ANNOUNCEMENT_TEXT)
            _specialAnnouncementText.value = announcement
            val verName = config.getString(KEY_LATEST_VERSION_NAME)
            if (verName.isNotBlank()) {
                _latestVersionName.value = verName
            }
            val verCode = config.getLong(KEY_LATEST_VERSION_CODE).toInt()
            if (verCode > 0) {
                _latestVersionCode.value = verCode
            }
            val apkUrl = config.getString(KEY_UPDATE_APK_URL)
            if (apkUrl.isNotBlank()) {
                _updateApkUrl.value = apkUrl
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating local state from RemoteConfig: ${e.message}")
        }
    }
}
