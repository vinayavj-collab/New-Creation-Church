package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.bible.model.ReadingPlanHighlightStyle
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vinay_app_prefs", Context.MODE_PRIVATE)

    init {
        // Reset default to false for v37 if previously saved as default true
        if (!prefs.getBoolean("v37_defaults_migrated", false)) {
            prefs.edit()
                .putBoolean("welcome_speech_once_day", false)
                .putBoolean("verse_speech_once_day", false)
                .putBoolean("v37_defaults_migrated", true)
                .apply()
        }

        // Migration to turn off Photo Gallery & Albums by default
        if (!prefs.getBoolean("v39_photo_gallery_off_migrated", false)) {
            val currentEnabled = prefs.getStringSet("enabled_home_sections", null)
            if (currentEnabled != null) {
                val updated = currentEnabled.toMutableSet()
                updated.remove(HomeSectionType.PHOTOS.id)
                prefs.edit().putStringSet("enabled_home_sections", updated).apply()
            }
            if (prefs.getString("custom_fourth_tab", null) == CustomFourthTab.PHOTOS.name) {
                prefs.edit().putString("custom_fourth_tab", CustomFourthTab.SONG_BOOK.name).apply()
            }
            prefs.edit().putBoolean("v39_photo_gallery_off_migrated", true).apply()
        }
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _readingPlanHighlightStyle = MutableStateFlow(loadReadingPlanHighlightStyle())
    val readingPlanHighlightStyle: StateFlow<ReadingPlanHighlightStyle> = _readingPlanHighlightStyle.asStateFlow()

    fun getReadingPlanHighlightStyle(): ReadingPlanHighlightStyle = _readingPlanHighlightStyle.value

    private fun loadReadingPlanHighlightStyle(): ReadingPlanHighlightStyle {
        return ReadingPlanHighlightStyle(
            isVisible = prefs.getBoolean("reading_plan_highlight_visible", true),
            windowFillColorHex = prefs.getString("reading_plan_highlight_fill_color", "#FDE68A") ?: "#FDE68A",
            strokeColorHex = prefs.getString("reading_plan_highlight_stroke_color", "#D97706") ?: "#D97706",
            alpha = prefs.getFloat("reading_plan_highlight_alpha", 0.35f),
            borderThicknessDp = prefs.getFloat("reading_plan_highlight_border_thickness", 2.0f),
            cornerRadiusDp = prefs.getFloat("reading_plan_highlight_corner_radius", 8.0f)
        )
    }

    fun updateReadingPlanHighlightStyle(style: ReadingPlanHighlightStyle) {
        prefs.edit()
            .putBoolean("reading_plan_highlight_visible", style.isVisible)
            .putString("reading_plan_highlight_fill_color", style.windowFillColorHex)
            .putString("reading_plan_highlight_stroke_color", style.strokeColorHex)
            .putFloat("reading_plan_highlight_alpha", style.alpha)
            .putFloat("reading_plan_highlight_border_thickness", style.borderThicknessDp)
            .putFloat("reading_plan_highlight_corner_radius", style.cornerRadiusDp)
            .apply()
        _readingPlanHighlightStyle.value = style
    }

    private fun loadSettings(): UserSettings {
        val themeStr = prefs.getString("theme_mode", ThemeMode.DYNAMIC.name) ?: ThemeMode.DYNAMIC.name
        val themeMode = try {
            ThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            ThemeMode.DYNAMIC
        }

        val vlogModeStr = prefs.getString("personal_vlog_mode", PersonalVlogMode.HIDDEN.name) ?: PersonalVlogMode.HIDDEN.name
        val personalVlogMode = try {
            PersonalVlogMode.valueOf(vlogModeStr)
        } catch (e: Exception) {
            PersonalVlogMode.HIDDEN
        }

        val ytTabStr = prefs.getString("youtube_default_tab", YouTubeDefaultTab.ALL.name) ?: YouTubeDefaultTab.ALL.name
        val youtubeDefaultTab = try {
            YouTubeDefaultTab.valueOf(ytTabStr)
        } catch (e: Exception) {
            YouTubeDefaultTab.ALL
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
            HomeSectionType.DID_YOU_KNOW.id,
            HomeSectionType.DAILY_QUIZ.id,
            HomeSectionType.DAILY_DEVOTIONAL.id,
            HomeSectionType.UPCOMING_EVENTS.id,
            HomeSectionType.FELLOWSHIP_EVENTS.id,
            HomeSectionType.LATEST_VIDEOS.id,
            HomeSectionType.PLAYLISTS.id,
            HomeSectionType.LATEST_EVENTS.id
        )
        val enabledStrings = prefs.getStringSet("enabled_home_sections", defaultEnabled) ?: defaultEnabled
        var enabledHomeSections = enabledStrings.mapNotNull { id ->
            HomeSectionType.entries.find { it.id == id }
        }.toMutableSet()

        // Remove PHOTOS by default unless user has customized enabled_home_sections
        if (!prefs.contains("enabled_home_sections")) {
            enabledHomeSections.remove(HomeSectionType.PHOTOS)
        }

        // Auto-enable new features (DID_YOU_KNOW, DAILY_QUIZ, DAILY_DEVOTIONAL) if not explicitly set
        if (!prefs.contains("enabled_home_sections")) {
            enabledHomeSections.addAll(
                listOf(
                    HomeSectionType.DID_YOU_KNOW,
                    HomeSectionType.DAILY_QUIZ,
                    HomeSectionType.DAILY_DEVOTIONAL
                )
            )
        } else {
            // Also ensure these crucial spiritual feature sections are enabled
            enabledHomeSections.add(HomeSectionType.DID_YOU_KNOW)
            enabledHomeSections.add(HomeSectionType.DAILY_QUIZ)
            enabledHomeSections.add(HomeSectionType.DAILY_DEVOTIONAL)
        }

        // Order
        val orderStr = prefs.getString("home_sections_order", null)
        val homeSectionsOrder = if (orderStr.isNullOrBlank()) {
            listOf(
                HomeSectionType.TODAYS_VERSE,
                HomeSectionType.DID_YOU_KNOW,
                HomeSectionType.DAILY_QUIZ,
                HomeSectionType.DAILY_DEVOTIONAL,
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

        val fourthTabStr = prefs.getString("custom_fourth_tab", CustomFourthTab.SONG_BOOK.name) ?: CustomFourthTab.SONG_BOOK.name
        val customFourthTab = try {
            CustomFourthTab.valueOf(fourthTabStr)
        } catch (e: Exception) {
            CustomFourthTab.SONG_BOOK
        }

        val photoLayoutStr = prefs.getString("blogger_photo_layout", BloggerPhotoLayout.GRID_2.name) ?: BloggerPhotoLayout.GRID_2.name
        val bloggerPhotoLayout = try {
            BloggerPhotoLayout.valueOf(photoLayoutStr)
        } catch (e: Exception) {
            BloggerPhotoLayout.GRID_2
        }

        val showVlog = personalVlogMode != PersonalVlogMode.HIDDEN

        val navOrderStr = prefs.getString("nav_tabs_order", "HOME,BLOGS,YOUTUBE,FOURTH_TAB,MORE") ?: "HOME,BLOGS,YOUTUBE,FOURTH_TAB,MORE"
        val navTabsOrder = navOrderStr.split(",").filter { it.isNotBlank() }

        val activePlans = prefs.getStringSet("active_plan_ids", null) ?: setOf("gospels_30")
        val behindColor = prefs.getString("plan_behind_color", "#EF4444") ?: "#EF4444"
        val onTrackColor = prefs.getString("plan_ontrack_color", "#EAB308") ?: "#EAB308"
        val completedColor = prefs.getString("plan_completed_color", "#10B981") ?: "#10B981"

        val verseAlarmFreqStr = prefs.getString("verse_alarm_freq", VerseAlarmFrequency.DAILY.name) ?: VerseAlarmFrequency.DAILY.name
        val verseAlarmFreq = try { VerseAlarmFrequency.valueOf(verseAlarmFreqStr) } catch (e: Exception) { VerseAlarmFrequency.DAILY }

        val verseAlarmModeStr = prefs.getString("verse_alarm_mode", VerseAlarmMode.SPEECH_DIRECT.name) ?: VerseAlarmMode.SPEECH_DIRECT.name
        val verseAlarmMode = try { VerseAlarmMode.valueOf(verseAlarmModeStr) } catch (e: Exception) { VerseAlarmMode.SPEECH_DIRECT }

        val verseAlarmContentStr = prefs.getString("verse_alarm_content", VerseAlarmContent.GREETING_AND_VERSE.name) ?: VerseAlarmContent.GREETING_AND_VERSE.name
        val verseAlarmContent = try { VerseAlarmContent.valueOf(verseAlarmContentStr) } catch (e: Exception) { VerseAlarmContent.GREETING_AND_VERSE }

        val dailyPrayerSlotStr = prefs.getString("daily_prayer_reminder_slot", DailyPrayerSlot.MORNING.name) ?: DailyPrayerSlot.MORNING.name
        val dailyPrayerSlot = try { DailyPrayerSlot.valueOf(dailyPrayerSlotStr) } catch (e: Exception) { DailyPrayerSlot.MORNING }

        return UserSettings(
            themeMode = themeMode,
            showFellowshipEvents = prefs.getBoolean("show_fellowship_events", true),
            personalVlogMode = personalVlogMode,
            showPersonalVlog = showVlog,
            showYouTube = prefs.getBoolean("show_youtube", true),
            showShorts = prefs.getBoolean("show_shorts", true),
            youtubeDefaultTab = youtubeDefaultTab,
            bibleReadingStyle = bibleReadingStyle,
            dataSaverEnabled = prefs.getBoolean("data_saver_enabled", true),
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
            customFourthTab = customFourthTab,
            bloggerPhotoLayout = bloggerPhotoLayout,
            isDrawerEnabled = prefs.getBoolean("is_drawer_enabled", true),
            drawerPosition = prefs.getString("drawer_position", "left") ?: "left",
            lastReadPostId = prefs.getString("last_read_post_id", null),
            navTabsOrder = navTabsOrder,
            activePlanIds = activePlans,
            planBehindColorHex = behindColor,
            planOnTrackColorHex = onTrackColor,
            planCompletedColorHex = completedColor,
            userName = prefs.getString("user_name", "") ?: "",
            enableWelcomeSpeech = prefs.getBoolean("enable_welcome_speech", true),
            enableVerseSpeechOnLaunch = prefs.getBoolean("enable_verse_speech_launch", true),
            welcomeSpeechOncePerDay = prefs.getBoolean("welcome_speech_once_day", false),
            verseSpeechOncePerDay = prefs.getBoolean("verse_speech_once_day", false),
            welcomeDialogDismissed = prefs.getBoolean("welcome_dialog_dismissed", false),
            verseAlarmEnabled = prefs.getBoolean("verse_alarm_enabled", true),
            verseAlarmHour = prefs.getInt("verse_alarm_hour", 6),
            verseAlarmMinute = prefs.getInt("verse_alarm_minute", 0),
            verseAlarmFrequency = verseAlarmFreq,
            verseAlarmIntervalHours = prefs.getInt("verse_alarm_interval_hours", 4),
            verseAlarmMode = verseAlarmMode,
            verseAlarmContent = verseAlarmContent,
            syncGreetingVolumeWithAlarm = prefs.getBoolean("sync_greeting_volume_alarm", true),
            greetingSpeechVolume = prefs.getFloat("greeting_speech_volume", 1.0f),
            greetingSpeechPitch = prefs.getFloat("greeting_speech_pitch", 1.0f),
            greetingSpeechSpeed = prefs.getFloat("greeting_speech_speed", 1.0f),
            alarmVolume = prefs.getFloat("alarm_volume", 1.0f),
            readingPlanReminderEnabled = prefs.getBoolean("reading_plan_reminder_enabled", true),
            readingPlanReminderHour = prefs.getInt("reading_plan_reminder_hour", 5),
            readingPlanReminderMinute = prefs.getInt("reading_plan_reminder_minute", 0),
            readingPlanReminderEveningEnabled = prefs.getBoolean("reading_plan_reminder_evening_enabled", true),
            readingPlanReminderEveningHour = prefs.getInt("reading_plan_reminder_evening_hour", 21),
            readingPlanReminderEveningMinute = prefs.getInt("reading_plan_reminder_evening_minute", 0),
            dailyPrayerReminderEnabled = prefs.getBoolean("daily_prayer_reminder_enabled", true),
            dailyPrayerReminderHour = prefs.getInt("daily_prayer_reminder_hour", 4),
            dailyPrayerReminderMinute = prefs.getInt("daily_prayer_reminder_minute", 0),
            dailyPrayerReminderSlot = dailyPrayerSlot,
            morningPrayerHour = prefs.getInt("morning_prayer_hour", DailyPrayerSlot.MORNING.defaultHour),
            morningPrayerMinute = prefs.getInt("morning_prayer_minute", DailyPrayerSlot.MORNING.defaultMinute),
            afternoonPrayerHour = prefs.getInt("afternoon_prayer_hour", DailyPrayerSlot.AFTERNOON.defaultHour),
            afternoonPrayerMinute = prefs.getInt("afternoon_prayer_minute", DailyPrayerSlot.AFTERNOON.defaultMinute),
            eveningPrayerHour = prefs.getInt("evening_prayer_hour", DailyPrayerSlot.EVENING.defaultHour),
            eveningPrayerMinute = prefs.getInt("evening_prayer_minute", DailyPrayerSlot.EVENING.defaultMinute),
            nightPrayerHour = prefs.getInt("night_prayer_hour", DailyPrayerSlot.NIGHT.defaultHour),
            nightPrayerMinute = prefs.getInt("night_prayer_minute", DailyPrayerSlot.NIGHT.defaultMinute),
            widgetAutoChangeIntervalHours = prefs.getInt("widget_auto_change_interval_hours", 0),
            personalBlogPassword = prefs.getString("personal_blog_password", "1234") ?: "1234",
            inactiveAdminAutoDisableDays = prefs.getInt("inactive_admin_auto_disable_days", 90),
            isGlobalAdminEmergencyLock = prefs.getBoolean("is_global_admin_emergency_lock", false),
            hasSeenProfileAdminPrompt = prefs.getBoolean("has_seen_profile_admin_prompt", false),
            masterAdminPasswordEnabled = prefs.getBoolean("master_admin_password_enabled", true),
            masterAdminPin = prefs.getString("master_admin_pin", "9876") ?: "9876",
            masterAdminDualAuthEnabled = prefs.getBoolean("master_admin_dual_auth_enabled", false),
            masterAdminSecondaryPin = prefs.getString("master_admin_secondary_pin", "123456") ?: "123456",
            biometricTimeoutDays = prefs.getInt("biometric_timeout_days", 30),
            isBiometricEnabled = prefs.getBoolean("is_biometric_enabled", true),
            globalAuthBypass = prefs.getBoolean("global_auth_bypass", false),
            requireP2EveryLogin = prefs.getBoolean("require_p2_every_login", true),
            trustedDevices = prefs.getString("trusted_devices_list", "Android-Primary-Device,Mobile-Auth-Terminal-01")?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: listOf("Android-Primary-Device"),
            profileReminderIntervalDays = prefs.getInt("profile_reminder_interval_days", 7),
            notificationMethod = prefs.getString("notification_method", "Local Notification") ?: "Local Notification",
            isChatEnabled = prefs.getBoolean("is_chat_enabled", false),
            chatAllowOnlyVerified = prefs.getBoolean("chat_allow_only_verified", true),
            chatWhitelistedUserIds = prefs.getString("chat_whitelisted_user_ids", "")?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
            chatAllowedRoles = prefs.getString("chat_allowed_roles", "")?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
            delegatedGlobalEventCreators = prefs.getString("delegated_global_event_creators", "")?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    fun updateDelegatedGlobalEventCreators(creators: List<String>) {
        val str = creators.joinToString(",")
        prefs.edit().putString("delegated_global_event_creators", str).apply()
        _settings.value = _settings.value.copy(delegatedGlobalEventCreators = creators)
    }

    fun updateChatSettings(
        isChatEnabled: Boolean,
        chatAllowOnlyVerified: Boolean,
        chatWhitelistedUserIds: List<String>,
        chatAllowedRoles: List<String>
    ) {
        val whitelistedStr = chatWhitelistedUserIds.joinToString(",")
        val rolesStr = chatAllowedRoles.joinToString(",")
        prefs.edit()
            .putBoolean("is_chat_enabled", isChatEnabled)
            .putBoolean("chat_allow_only_verified", chatAllowOnlyVerified)
            .putString("chat_whitelisted_user_ids", whitelistedStr)
            .putString("chat_allowed_roles", rolesStr)
            .apply()
        _settings.value = _settings.value.copy(
            isChatEnabled = isChatEnabled,
            chatAllowOnlyVerified = chatAllowOnlyVerified,
            chatWhitelistedUserIds = chatWhitelistedUserIds,
            chatAllowedRoles = chatAllowedRoles
        )
    }

    fun updateMasterAdminSecurity(
        passwordEnabled: Boolean,
        pin: String,
        dualAuthEnabled: Boolean,
        secondaryPin: String,
        biometricTimeout: Int = _settings.value.biometricTimeoutDays,
        biometricEnabled: Boolean = _settings.value.isBiometricEnabled,
        authBypass: Boolean = _settings.value.globalAuthBypass,
        p2EveryLogin: Boolean = _settings.value.requireP2EveryLogin,
        trustedDevicesList: List<String> = _settings.value.trustedDevices,
        reminderInterval: Int = _settings.value.profileReminderIntervalDays,
        notifMethod: String = _settings.value.notificationMethod
    ) {
        val devicesString = trustedDevicesList.joinToString(",")
        prefs.edit()
            .putBoolean("master_admin_password_enabled", passwordEnabled)
            .putString("master_admin_pin", pin)
            .putBoolean("master_admin_dual_auth_enabled", dualAuthEnabled)
            .putString("master_admin_secondary_pin", secondaryPin)
            .putInt("biometric_timeout_days", biometricTimeout)
            .putBoolean("is_biometric_enabled", biometricEnabled)
            .putBoolean("global_auth_bypass", authBypass)
            .putBoolean("require_p2_every_login", p2EveryLogin)
            .putString("trusted_devices_list", devicesString)
            .putInt("profile_reminder_interval_days", reminderInterval)
            .putString("notification_method", notifMethod)
            .apply()
        _settings.value = _settings.value.copy(
            masterAdminPasswordEnabled = passwordEnabled,
            masterAdminPin = pin,
            masterAdminDualAuthEnabled = dualAuthEnabled,
            masterAdminSecondaryPin = secondaryPin,
            biometricTimeoutDays = biometricTimeout,
            isBiometricEnabled = biometricEnabled,
            globalAuthBypass = authBypass,
            requireP2EveryLogin = p2EveryLogin,
            trustedDevices = trustedDevicesList,
            profileReminderIntervalDays = reminderInterval,
            notificationMethod = notifMethod
        )
    }

    fun updatePersonalBlogPassword(password: String) {
        prefs.edit().putString("personal_blog_password", password).apply()
        _settings.value = _settings.value.copy(personalBlogPassword = password)
    }

    fun updateInactiveAdminAutoDisableDays(days: Int) {
        prefs.edit().putInt("inactive_admin_auto_disable_days", days).apply()
        _settings.value = _settings.value.copy(inactiveAdminAutoDisableDays = days)
    }

    fun enableAdminLockSystem() {
        prefs.edit()
            .putBoolean("master_admin_password_enabled", true)
            .putBoolean("global_auth_bypass", false)
            .apply()
        _settings.value = _settings.value.copy(
            masterAdminPasswordEnabled = true,
            globalAuthBypass = false
        )
    }

    fun updateGlobalAdminEmergencyLock(locked: Boolean) {
        prefs.edit().putBoolean("is_global_admin_emergency_lock", locked).apply()
        _settings.value = _settings.value.copy(isGlobalAdminEmergencyLock = locked)
    }

    fun markProfileAdminPromptSeen() {
        prefs.edit().putBoolean("has_seen_profile_admin_prompt", true).apply()
        _settings.value = _settings.value.copy(hasSeenProfileAdminPrompt = true)
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

    fun updateCustomFourthTab(tab: CustomFourthTab) {
        prefs.edit().putString("custom_fourth_tab", tab.name).apply()
        _settings.value = _settings.value.copy(customFourthTab = tab)
    }

    fun updateBloggerPhotoLayout(layout: BloggerPhotoLayout) {
        prefs.edit().putString("blogger_photo_layout", layout.name).apply()
        _settings.value = _settings.value.copy(bloggerPhotoLayout = layout)
    }

    fun updateIsDrawerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_drawer_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isDrawerEnabled = enabled)
    }

    fun updateDrawerPosition(position: String) {
        prefs.edit().putString("drawer_position", position).apply()
        _settings.value = _settings.value.copy(drawerPosition = position)
    }

    fun setLastReadPostId(postId: String) {
        prefs.edit().putString("last_read_post_id", postId).apply()
        _settings.value = _settings.value.copy(lastReadPostId = postId)
    }

    fun updateNavTabsOrder(order: List<String>) {
        val str = order.joinToString(",")
        prefs.edit().putString("nav_tabs_order", str).apply()
        _settings.value = _settings.value.copy(navTabsOrder = order)
    }

    fun updateActivePlanIds(ids: Set<String>) {
        prefs.edit().putStringSet("active_plan_ids", ids).apply()
        _settings.value = _settings.value.copy(activePlanIds = ids)
    }

    fun getManualPlansJson(): String = prefs.getString("manual_plans_json", "") ?: ""

    fun updateManualPlansJson(json: String) {
        prefs.edit().putString("manual_plans_json", json).apply()
    }

    fun updateReadingPlanColors(behindHex: String, onTrackHex: String, completedHex: String) {
        prefs.edit()
            .putString("plan_behind_color", behindHex)
            .putString("plan_ontrack_color", onTrackHex)
            .putString("plan_completed_color", completedHex)
            .apply()
        _settings.value = _settings.value.copy(
            planBehindColorHex = behindHex,
            planOnTrackColorHex = onTrackHex,
            planCompletedColorHex = completedHex
        )
    }

    fun updateUserName(name: String) {
        prefs.edit().putString("user_name", name.trim()).apply()
        _settings.value = _settings.value.copy(userName = name.trim())
    }

    fun updateEnableWelcomeSpeech(enabled: Boolean) {
        prefs.edit().putBoolean("enable_welcome_speech", enabled).apply()
        _settings.value = _settings.value.copy(enableWelcomeSpeech = enabled)
    }

    fun updateEnableVerseSpeechOnLaunch(enabled: Boolean) {
        prefs.edit().putBoolean("enable_verse_speech_launch", enabled).apply()
        _settings.value = _settings.value.copy(enableVerseSpeechOnLaunch = enabled)
    }

    fun updateWelcomeSpeechOncePerDay(oncePerDay: Boolean) {
        prefs.edit().putBoolean("welcome_speech_once_day", oncePerDay).apply()
        _settings.value = _settings.value.copy(welcomeSpeechOncePerDay = oncePerDay)
    }

    fun updateVerseSpeechOncePerDay(oncePerDay: Boolean) {
        prefs.edit().putBoolean("verse_speech_once_day", oncePerDay).apply()
        _settings.value = _settings.value.copy(verseSpeechOncePerDay = oncePerDay)
    }

    fun updateWelcomeDialogDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean("welcome_dialog_dismissed", dismissed).apply()
        _settings.value = _settings.value.copy(welcomeDialogDismissed = dismissed)
    }

    fun updateVerseAlarmEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("verse_alarm_enabled", enabled).apply()
        _settings.value = _settings.value.copy(verseAlarmEnabled = enabled)
    }

    fun isPrayerTimeCustomizedByUser(): Boolean {
        return prefs.getBoolean("user_customized_prayer_time", false)
    }

    fun isVerseAlarmTimeCustomizedByUser(): Boolean {
        return prefs.getBoolean("user_customized_verse_alarm_time", false)
    }

    fun isReadingReminderTimeCustomizedByUser(): Boolean {
        return prefs.getBoolean("user_customized_reading_reminder_time", false)
    }

    fun updateVerseAlarmTime(hour: Int, minute: Int, isManual: Boolean = true) {
        val editor = prefs.edit()
            .putInt("verse_alarm_hour", hour)
            .putInt("verse_alarm_minute", minute)
        if (isManual) {
            editor.putBoolean("user_customized_verse_alarm_time", true)
        }
        editor.apply()
        _settings.value = _settings.value.copy(
            verseAlarmHour = hour,
            verseAlarmMinute = minute
        )
    }

    fun updateVerseAlarmFrequency(frequency: VerseAlarmFrequency) {
        prefs.edit().putString("verse_alarm_freq", frequency.name).apply()
        _settings.value = _settings.value.copy(verseAlarmFrequency = frequency)
    }

    fun updateVerseAlarmIntervalHours(intervalHours: Int) {
        prefs.edit().putInt("verse_alarm_interval_hours", intervalHours).apply()
        _settings.value = _settings.value.copy(verseAlarmIntervalHours = intervalHours)
    }

    fun updateVerseAlarmMode(mode: VerseAlarmMode) {
        prefs.edit().putString("verse_alarm_mode", mode.name).apply()
        _settings.value = _settings.value.copy(verseAlarmMode = mode)
    }

    fun updateVerseAlarmContent(content: VerseAlarmContent) {
        prefs.edit().putString("verse_alarm_content", content.name).apply()
        _settings.value = _settings.value.copy(verseAlarmContent = content)
    }

    fun updateSyncGreetingVolumeWithAlarm(sync: Boolean) {
        prefs.edit().putBoolean("sync_greeting_volume_alarm", sync).apply()
        _settings.value = _settings.value.copy(syncGreetingVolumeWithAlarm = sync)
    }

    fun updateGreetingSpeechVolume(volume: Float) {
        val v = volume.coerceIn(0.1f, 1.0f)
        prefs.edit().putFloat("greeting_speech_volume", v).apply()
        _settings.value = _settings.value.copy(greetingSpeechVolume = v)
    }

    fun updateGreetingSpeechPitch(pitch: Float) {
        val p = pitch.coerceIn(0.5f, 1.8f)
        prefs.edit().putFloat("greeting_speech_pitch", p).apply()
        _settings.value = _settings.value.copy(greetingSpeechPitch = p)
    }

    fun updateGreetingSpeechSpeed(speed: Float) {
        val s = speed.coerceIn(0.5f, 1.8f)
        prefs.edit().putFloat("greeting_speech_speed", s).apply()
        _settings.value = _settings.value.copy(greetingSpeechSpeed = s)
    }

    fun updateAlarmVolume(volume: Float) {
        val v = volume.coerceIn(0.1f, 1.0f)
        prefs.edit().putFloat("alarm_volume", v).apply()
        _settings.value = _settings.value.copy(alarmVolume = v)
    }

    fun updateReadingPlanReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reading_plan_reminder_enabled", enabled).apply()
        _settings.value = _settings.value.copy(readingPlanReminderEnabled = enabled)
    }

    fun updateReadingPlanReminderTime(hour: Int, minute: Int, isManual: Boolean = true) {
        val editor = prefs.edit()
            .putInt("reading_plan_reminder_hour", hour)
            .putInt("reading_plan_reminder_minute", minute)
        if (isManual) {
            editor.putBoolean("user_customized_reading_reminder_time", true)
        }
        editor.apply()
        _settings.value = _settings.value.copy(
            readingPlanReminderHour = hour,
            readingPlanReminderMinute = minute
        )
    }

    fun updateReadingPlanReminderEveningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reading_plan_reminder_evening_enabled", enabled).apply()
        _settings.value = _settings.value.copy(readingPlanReminderEveningEnabled = enabled)
    }

    fun updateReadingPlanReminderEveningTime(hour: Int, minute: Int, isManual: Boolean = true) {
        val editor = prefs.edit()
            .putInt("reading_plan_reminder_evening_hour", hour)
            .putInt("reading_plan_reminder_evening_minute", minute)
        if (isManual) {
            editor.putBoolean("user_customized_reading_reminder_time", true)
        }
        editor.apply()
        _settings.value = _settings.value.copy(
            readingPlanReminderEveningHour = hour,
            readingPlanReminderEveningMinute = minute
        )
    }

    fun updateDailyPrayerReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("daily_prayer_reminder_enabled", enabled).apply()
        _settings.value = _settings.value.copy(dailyPrayerReminderEnabled = enabled)
    }

    fun updateDailyPrayerReminderTime(hour: Int, minute: Int, isManual: Boolean = true) {
        val editor = prefs.edit()
            .putInt("daily_prayer_reminder_hour", hour)
            .putInt("daily_prayer_reminder_minute", minute)
        if (isManual) {
            editor.putBoolean("user_customized_prayer_time", true)
        }
        editor.apply()
        _settings.value = _settings.value.copy(
            dailyPrayerReminderHour = hour,
            dailyPrayerReminderMinute = minute
        )
    }

    fun updateDailyPrayerReminderSlot(slot: DailyPrayerSlot) {
        val editor = prefs.edit().putString("daily_prayer_reminder_slot", slot.name)
        val (h, m) = when (slot) {
            DailyPrayerSlot.MORNING -> {
                val hour = prefs.getInt("morning_prayer_hour", DailyPrayerSlot.MORNING.defaultHour)
                val min = prefs.getInt("morning_prayer_minute", DailyPrayerSlot.MORNING.defaultMinute)
                hour to min
            }
            DailyPrayerSlot.AFTERNOON -> {
                val hour = prefs.getInt("afternoon_prayer_hour", DailyPrayerSlot.AFTERNOON.defaultHour)
                val min = prefs.getInt("afternoon_prayer_minute", DailyPrayerSlot.AFTERNOON.defaultMinute)
                hour to min
            }
            DailyPrayerSlot.EVENING -> {
                val hour = prefs.getInt("evening_prayer_hour", DailyPrayerSlot.EVENING.defaultHour)
                val min = prefs.getInt("evening_prayer_minute", DailyPrayerSlot.EVENING.defaultMinute)
                hour to min
            }
            DailyPrayerSlot.NIGHT -> {
                val hour = prefs.getInt("night_prayer_hour", DailyPrayerSlot.NIGHT.defaultHour)
                val min = prefs.getInt("night_prayer_minute", DailyPrayerSlot.NIGHT.defaultMinute)
                hour to min
            }
            DailyPrayerSlot.CUSTOM -> {
                _settings.value.dailyPrayerReminderHour to _settings.value.dailyPrayerReminderMinute
            }
        }
        editor.putInt("daily_prayer_reminder_hour", h)
        editor.putInt("daily_prayer_reminder_minute", m)
        editor.apply()
        _settings.value = _settings.value.copy(
            dailyPrayerReminderSlot = slot,
            dailyPrayerReminderHour = h,
            dailyPrayerReminderMinute = m
        )
    }

    fun updatePrayerSlotCustomTime(slot: DailyPrayerSlot, hour: Int, minute: Int) {
        val editor = prefs.edit()
        when (slot) {
            DailyPrayerSlot.MORNING -> {
                editor.putInt("morning_prayer_hour", hour).putInt("morning_prayer_minute", minute)
                _settings.value = _settings.value.copy(morningPrayerHour = hour, morningPrayerMinute = minute)
            }
            DailyPrayerSlot.AFTERNOON -> {
                editor.putInt("afternoon_prayer_hour", hour).putInt("afternoon_prayer_minute", minute)
                _settings.value = _settings.value.copy(afternoonPrayerHour = hour, afternoonPrayerMinute = minute)
            }
            DailyPrayerSlot.EVENING -> {
                editor.putInt("evening_prayer_hour", hour).putInt("evening_prayer_minute", minute)
                _settings.value = _settings.value.copy(eveningPrayerHour = hour, eveningPrayerMinute = minute)
            }
            DailyPrayerSlot.NIGHT -> {
                editor.putInt("night_prayer_hour", hour).putInt("night_prayer_minute", minute)
                _settings.value = _settings.value.copy(nightPrayerHour = hour, nightPrayerMinute = minute)
            }
            DailyPrayerSlot.CUSTOM -> {}
        }
        // If this slot is currently selected or if it's custom, also update active reminder time
        if (_settings.value.dailyPrayerReminderSlot == slot || slot == DailyPrayerSlot.CUSTOM) {
            editor.putInt("daily_prayer_reminder_hour", hour)
            editor.putInt("daily_prayer_reminder_minute", minute)
            _settings.value = _settings.value.copy(
                dailyPrayerReminderHour = hour,
                dailyPrayerReminderMinute = minute
            )
        }
        editor.apply()
    }

    fun updateWidgetAutoChangeIntervalHours(hours: Int) {
        val safeHours = if (hours < 0) 0 else hours
        prefs.edit().putInt("widget_auto_change_interval_hours", safeHours).apply()
        _settings.value = _settings.value.copy(widgetAutoChangeIntervalHours = safeHours)
    }

    fun isNotificationOnboardingCompleted(): Boolean {
        return prefs.getBoolean("notification_onboarding_completed", false)
    }

    fun setNotificationOnboardingCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean("notification_onboarding_completed", completed).apply()
    }

    fun isNotificationPromptShownForVersion(versionCode: Int): Boolean {
        return prefs.getBoolean("notification_prompt_shown_v$versionCode", false)
    }

    fun setNotificationPromptShownForVersion(versionCode: Int, shown: Boolean = true) {
        prefs.edit().putBoolean("notification_prompt_shown_v$versionCode", shown).apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

