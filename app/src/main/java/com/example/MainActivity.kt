package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.AppProfile
import com.example.data.model.AppStrings
import com.example.data.model.BlogPost
import com.example.data.model.GalleryPhoto
import com.example.data.model.LocalAppLanguage
import com.example.data.model.LocalAppStrings
import com.example.data.model.LocalAppProfile
import com.example.util.ProfileManager
import com.example.data.model.ThemeMode
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.data.model.appStrings
import com.example.ui.bible.BibleHomeScreen
import com.example.ui.bible.BibleReaderScreen
import com.example.ui.bible.BibleSavedScreen
import com.example.ui.bible.BibleSearchScreen
import com.example.ui.bible.BibleViewModel
import com.example.ui.components.AppUpdateModalDialog
import com.example.ui.components.BibleMiniAudioPlayer
import com.example.ui.components.YouTubeMiniPlayer
import com.example.ui.components.UniversalVideoPlayer
import com.example.util.GlobalVideoPlayerState
import com.example.util.VideoPlaybackTracker
import com.example.util.VideoPlaybackForegroundService
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.BibleVerseWidgetProvider
import com.example.ui.viewmodel.MainViewModel
import com.example.util.RemoteConfigHelper
import com.example.util.RemoteConfigManager
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

enum class MainDestination(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    CHAT("Chat", Icons.Default.Chat),
    BLOGS("Blogs", Icons.Default.Article),
    YOUTUBE("YouTube", Icons.Default.PlayCircle),
    BIBLE("Bible", Icons.Default.MenuBook),
    READING("Reading", Icons.Default.AutoStories)
}

sealed interface AppRoute {
    data object Main : AppRoute
    data class PostDetail(
        val post: BlogPost,
        val previousRoute: AppRoute = AppRoute.Main,
        val originDestination: MainDestination = MainDestination.HOME
    ) : AppRoute
    data class YouTubePlayer(
        val video: YouTubeVideo,
        val previousRoute: AppRoute = AppRoute.Main
    ) : AppRoute
    data class PlaylistDetail(val playlist: YouTubePlaylist) : AppRoute
    data class PhotoViewer(
        val photos: List<GalleryPhoto>,
        val initialIndex: Int,
        val previousRoute: AppRoute = AppRoute.Main
    ) : AppRoute
    data object Gallery : AppRoute
    data object Search : AppRoute
    data object Categories : AppRoute
    data class Settings(val scrollToUpdate: Boolean = false) : AppRoute
    data object About : AppRoute
    data object BibleHome : AppRoute
    data class BibleReader(
        val bookId: Int,
        val chapter: Int,
        val targetVerse: Int? = null,
        val isReadingPlanMode: Boolean = false,
        val highlightStartVerse: Int? = null,
        val highlightEndVerse: Int? = null,
        val targetBookId: Int? = if (isReadingPlanMode) bookId else null,
        val targetStartChapter: Int? = if (isReadingPlanMode) chapter else null,
        val targetStartVerse: Int? = if (isReadingPlanMode) (highlightStartVerse ?: targetVerse) else null,
        val targetEndChapter: Int? = if (isReadingPlanMode) chapter else null,
        val targetEndVerse: Int? = if (isReadingPlanMode) highlightEndVerse else null
    ) : AppRoute
    data object BibleSearch : AppRoute
    data object BibleSaved : AppRoute
    data object BibleReadingPlan : AppRoute
    data object DedicatedNotes : AppRoute
    data class Lyrics(val initialSongId: Long? = null) : AppRoute
    data object SyncCenter : AppRoute
    data object BackupRestore : AppRoute
    data object UpcomingEvents : AppRoute
    data object EventCalendar : AppRoute
    data object SavedItems : AppRoute
    data object RecentlyViewed : AppRoute
    data object HomeScreenSettings : AppRoute
    data class DailyPrayer(val prayerId: Int? = null) : AppRoute
    data object NotificationHistory : AppRoute
    data object UserProfile : AppRoute
    data object AdminPanel : AppRoute
}

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application)
    }
    private val bibleViewModel: BibleViewModel by viewModels {
        BibleViewModel.Factory(application)
    }

    private val externalRouteState = mutableStateOf<AppRoute?>(null)
    private val isInPipState = mutableStateOf(false)
    private var isCurrentlyPlayingVideo = false
    private lateinit var remoteConfig: FirebaseRemoteConfig

    fun enterPipModeIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            try {
                val aspectRatio = android.util.Rational(16, 9)
                val builder = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)

                // Use current active video player bounds (portrait player or floating mini player)
                val activeBounds = GlobalVideoPlayerState.activePipBounds.value
                if (activeBounds != null && !activeBounds.isEmpty) {
                    builder.setSourceRectHint(activeBounds)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setAutoEnterEnabled(true)
                }

                enterPictureInPictureMode(builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalVideoPlayerState.minimizeToMiniPlayer()
            }
        } else {
            GlobalVideoPlayerState.minimizeToMiniPlayer()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val ytSettings = com.example.util.YouTubeSettingsManager.settings.value
        if (ytSettings.enableBackgroundPip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isPlaying = VideoPlaybackTracker.activeVideoId != null ||
                            GlobalVideoPlayerState.currentVideo.value != null
            if (isPlaying) {
                try {
                    val aspectRatio = android.util.Rational(16, 9)
                    val builder = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)

                    // Pass exact on-screen coordinates where video is currently playing
                    val activeBounds = GlobalVideoPlayerState.activePipBounds.value
                    if (activeBounds != null && !activeBounds.isEmpty) {
                        builder.setSourceRectHint(activeBounds)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        builder.setAutoEnterEnabled(true)
                    }

                    enterPictureInPictureMode(builder.build())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipState.value = isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initRemoteConfig()
        com.example.util.YouTubeSettingsManager.init(applicationContext)
        val initialProfile = ProfileManager.getActiveProfile(applicationContext)
        title = if (initialProfile == AppProfile.VINAY) initialProfile.displayNameEnglish else getString(R.string.app_name)

        // Read saved theme mode before setting content (Default is DYNAMIC)
        val prefs = getSharedPreferences("vinay_app_prefs", Context.MODE_PRIVATE)
        val savedTheme = prefs.getString("theme_mode", ThemeMode.DYNAMIC.name) ?: ThemeMode.DYNAMIC.name
        if (savedTheme == ThemeMode.DYNAMIC.name) {
            try {
                if (com.google.android.material.color.DynamicColors.isDynamicColorAvailable()) {
                    com.google.android.material.color.DynamicColors.applyIfAvailable(this)
                }
            } catch (e: Exception) {
                // Compose MyApplicationTheme handles dynamic colors safely
            }
        }

        enableEdgeToEdge()
        handleWidgetIntent(intent)
        BibleVerseWidgetProvider.updateAllWidgets(applicationContext)

        setContent {
            val currentProfile by ProfileManager.getProfileFlow(applicationContext).collectAsState()
            LaunchedEffect(currentProfile) {
                this@MainActivity.title = if (currentProfile == AppProfile.VINAY) {
                    currentProfile.displayNameEnglish
                } else {
                    getString(R.string.app_name)
                }
            }
            val settings by viewModel.settings.collectAsState()
            val isDarkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.DYNAMIC -> isSystemInDarkTheme()
            }
            val isDynamicTheme = settings.themeMode == ThemeMode.DYNAMIC

            val appStrings = remember(settings.appLanguage) {
                AppStrings.forLanguage(settings.appLanguage)
            }

            val remotePrimaryColor by viewModel.themePrimaryColor.collectAsState()
            val remoteSecondaryColor by viewModel.themeSecondaryColor.collectAsState()

            CompositionLocalProvider(
                LocalAppStrings provides appStrings,
                LocalAppLanguage provides settings.appLanguage,
                LocalAppProfile provides currentProfile
            ) {
                MyApplicationTheme(
                    darkTheme = isDarkTheme,
                    dynamicColor = isDynamicTheme,
                    customPrimaryHex = remotePrimaryColor,
                    customSecondaryHex = remoteSecondaryColor
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    var showNotificationOnboardingModal by remember { mutableStateOf(false) }

                    val context = LocalContext.current
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            if (settings.dailyPrayerReminderEnabled) {
                                com.example.util.DailyPrayerReminderScheduler.scheduleDailyReminder(
                                    context,
                                    settings.dailyPrayerReminderHour,
                                    settings.dailyPrayerReminderMinute,
                                    settings.dailyPrayerReminderEnabled
                                )
                            }
                            if (settings.verseAlarmEnabled) {
                                com.example.util.VerseAlarmScheduler.scheduleNextAlarm(context, settings)
                            }
                            if (settings.readingPlanReminderEnabled) {
                                com.example.util.ReadingPlanReminderScheduler.scheduleAllReminders(context, settings)
                            }
                        }
                    }

                    LaunchedEffect(showSplash) {
                        if (!showSplash && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                            val isPromptedForUpdate = viewModel.isNotificationPromptShownForVersion(61)
                            if (!hasPermission && (!viewModel.isNotificationOnboardingCompleted() || !isPromptedForUpdate)) {
                                showNotificationOnboardingModal = true
                            }
                        }
                    }

                    if (showSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        // Dynamically register PictureInPicture params with OS so gestures/Home press seamlessly
                        // shrink from the real on-screen player bounds (MiniPlayer or Portrait player)
                        val activePipBounds by GlobalVideoPlayerState.activePipBounds.collectAsState()
                        val activeVideo by GlobalVideoPlayerState.currentVideo.collectAsState()
                        val isMiniPlayerActive by GlobalVideoPlayerState.isMiniPlayerActive.collectAsState()
                        val ytSettings by com.example.util.YouTubeSettingsManager.settings.collectAsState()

                        LaunchedEffect(activePipBounds, activeVideo, isMiniPlayerActive, ytSettings.enableBackgroundPip) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ytSettings.enableBackgroundPip) {
                                val isVideoActive = activeVideo != null
                                if (isVideoActive) {
                                    try {
                                        val builder = android.app.PictureInPictureParams.Builder()
                                            .setAspectRatio(android.util.Rational(16, 9))
                                        
                                        if (activePipBounds != null && !activePipBounds!!.isEmpty) {
                                            builder.setSourceRectHint(activePipBounds)
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            builder.setAutoEnterEnabled(true)
                                        }
                                        setPictureInPictureParams(builder.build())
                                    } catch (e: Exception) {
                                        // Ignore any OS-level PIP param exceptions gracefully
                                    }
                                }
                            }
                        }

                        AppNavigationHost(
                            viewModel = viewModel,
                            bibleViewModel = bibleViewModel,
                            isInPictureInPictureMode = isInPipState.value,
                            onEnterPipClick = { enterPipModeIfSupported() },
                            onVideoPlayingStateChanged = { isPlaying -> isCurrentlyPlayingVideo = isPlaying },
                            externalRoute = externalRouteState.value,
                            onClearExternalRoute = { externalRouteState.value = null }
                        )

                        if (showNotificationOnboardingModal) {
                            com.example.ui.components.NotificationOnboardingModal(
                                onDismiss = {
                                    showNotificationOnboardingModal = false
                                    viewModel.setNotificationPromptShownForVersion(61, true)
                                    viewModel.setNotificationOnboardingCompleted(true)
                                },
                                onGrantPermission = {
                                    showNotificationOnboardingModal = false
                                    viewModel.setNotificationPromptShownForVersion(61, true)
                                    viewModel.setNotificationOnboardingCompleted(true)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent == null) return

        // Notification Center intent handling (FCM / Local Notification tap)
        val openTab = intent.getStringExtra("open_tab")
        if (openTab == "NOTIFICATIONS" || intent.getBooleanExtra("open_notifications", false)) {
            externalRouteState.value = AppRoute.NotificationHistory
            return
        }

        // Daily Prayer notification intent handling
        val openDailyPrayer = intent.getBooleanExtra("open_daily_prayer", false) ||
                intent.getBooleanExtra("EXTRA_OPEN_DAILY_PRAYER", false) ||
                intent.action == "com.example.ACTION_DAILY_PRAYER_REMINDER"
        if (openDailyPrayer) {
            val prayerId = intent.getIntExtra("daily_prayer_id", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("EXTRA_DAILY_PRAYER_ID", -1).takeIf { it != -1 }
            externalRouteState.value = AppRoute.DailyPrayer(prayerId)
            return
        }

        // Reading plan intent handling - ONLY if isReadingPlanMode is true or explicitly a plan intent
        val isReadingPlan = intent.getBooleanExtra("isReadingPlanMode", false)
        if (isReadingPlan) {
            val planBookId = intent.getIntExtra("TargetBook", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("EXTRA_PLAN_BOOK_ID", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("targetBookId", -1).takeIf { it != -1 }
            val planStartChap = intent.getIntExtra("TargetStartChapter", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("EXTRA_PLAN_START_CHAPTER", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("targetStartChapter", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("EXTRA_PLAN_CHAPTER", -1).takeIf { it != -1 }
            val planEndChap = intent.getIntExtra("TargetEndChapter", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("EXTRA_PLAN_END_CHAPTER", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("targetEndChapter", -1).takeIf { it != -1 }
                ?: planStartChap
            val planStartVerse = intent.getIntExtra("TargetStartVerse", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("EXTRA_PLAN_START_VERSE", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("targetStartVerse", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("startVerse", -1).takeIf { it != -1 }
            val planEndVerse = intent.getIntExtra("TargetEndVerse", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("EXTRA_PLAN_END_VERSE", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("targetEndVerse", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("endVerse", -1).takeIf { it != -1 }

            if (planBookId != null && planStartChap != null) {
                externalRouteState.value = AppRoute.BibleReader(
                    bookId = planBookId,
                    chapter = planStartChap,
                    targetVerse = planStartVerse,
                    isReadingPlanMode = true,
                    highlightStartVerse = planStartVerse,
                    highlightEndVerse = planEndVerse,
                    targetBookId = planBookId,
                    targetStartChapter = planStartChap,
                    targetStartVerse = planStartVerse,
                    targetEndChapter = planEndChap,
                    targetEndVerse = planEndVerse
                )
                return
            }
        }

        val navigateTo = intent.getStringExtra(BibleVerseWidgetProvider.EXTRA_NAVIGATE_TO)
        if (navigateTo == "bible_reader") {
            val bookId = intent.getIntExtra(BibleVerseWidgetProvider.EXTRA_BOOK_ID, 43)
            val chapter = intent.getIntExtra(BibleVerseWidgetProvider.EXTRA_CHAPTER, 3)
            val verse = intent.getIntExtra(BibleVerseWidgetProvider.EXTRA_VERSE, 16)
            externalRouteState.value = AppRoute.BibleReader(
                bookId = bookId,
                chapter = chapter,
                targetVerse = verse,
                isReadingPlanMode = false,
                highlightStartVerse = null,
                highlightEndVerse = null,
                targetBookId = null,
                targetStartChapter = null,
                targetStartVerse = null,
                targetEndChapter = null,
                targetEndVerse = null
            )
            return
        }
    }

    override fun onStart() {
        super.onStart()
        BibleVerseWidgetProvider.updateAllWidgets(applicationContext)
        com.example.util.WelcomeSpeechManager.getInstance(applicationContext).setAppInForeground(true)
    }

    override fun onResume() {
        super.onResume()
        com.example.util.WelcomeSpeechManager.getInstance(applicationContext).setAppInForeground(true)
        val prefs = getSharedPreferences("vinay_app_prefs", Context.MODE_PRIVATE)
        val lastActive = prefs.getLong("last_active_timestamp", System.currentTimeMillis())
        val timeoutDays = prefs.getInt("biometric_timeout_days", 30)
        val timeoutMillis = timeoutDays * 24L * 60L * 60L * 1000L
        val now = System.currentTimeMillis()

        if (now - lastActive > timeoutMillis) {
            prefs.edit().remove("current_admin_id").putLong("last_active_timestamp", now).apply()
            Toast.makeText(
                this,
                "सत्र समाप्त (Session Expired): निष्क्रियता के कारण सत्र समाप्त हो गया है। कृपया पुनः लॉगिन करें।",
                Toast.LENGTH_LONG
            ).show()
        } else {
            prefs.edit().putLong("last_active_timestamp", now).apply()
        }
    }

    override fun onPause() {
        super.onPause()
        com.example.util.WelcomeSpeechManager.getInstance(applicationContext).setAppInForeground(false)
        com.example.util.WelcomeSpeechManager.getInstance(applicationContext).stop()
        val prefs = getSharedPreferences("vinay_app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_active_timestamp", System.currentTimeMillis()).apply()
    }

    override fun onStop() {
        super.onStop()
        com.example.util.WelcomeSpeechManager.getInstance(applicationContext).setAppInForeground(false)
        com.example.util.WelcomeSpeechManager.getInstance(applicationContext).stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.util.WelcomeSpeechManager.getInstance(applicationContext).setAppInForeground(false)
        com.example.util.WelcomeSpeechManager.getInstance(applicationContext).stop()
    }

    private fun initRemoteConfig() {
        try {
            remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0L) // 0 for instant testing / development
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

            // Asynchronously fetch and activate remote config parameters
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val isUpdated = task.result
                        val searchEnabled = remoteConfig.getBoolean("is_search_enabled")
                        val noticeHeading = remoteConfig.getString("app_notice_heading")
                        Log.i(
                            "MainActivity",
                            "Remote Config fetchAndActivate succeeded (updated: $isUpdated). " +
                                "is_search_enabled: $searchEnabled, app_notice_heading: '$noticeHeading'"
                        )
                        RemoteConfigManager.onConfigUpdated(remoteConfig)
                        RemoteConfigHelper.onConfigUpdated(remoteConfig)
                    } else {
                        Log.w("MainActivity", "Remote Config fetchAndActivate failed: ${task.exception?.message}")
                    }
                }
        } catch (e: Exception) {
            Log.w("MainActivity", "Firebase Remote Config initialization error: ${e.message}")
        }
    }

    /**
     * Getter method to check whether search features/buttons are enabled via Remote Config.
     */
    fun isSearchEnabled(): Boolean {
        return try {
            if (::remoteConfig.isInitialized) {
                remoteConfig.getBoolean("is_search_enabled")
            } else {
                RemoteConfigManager.isSearchEnabled()
            }
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Getter method to read dynamic notice banner title from Remote Config.
     */
    fun getAppNoticeHeading(): String {
        return try {
            if (::remoteConfig.isInitialized) {
                val heading = remoteConfig.getString("app_notice_heading")
                if (heading.isNotBlank()) heading else "महत्वपूर्ण सूचना"
            } else {
                RemoteConfigManager.getAppNoticeHeading()
            }
        } catch (e: Exception) {
            "महत्वपूर्ण सूचना"
        }
    }
}

@Composable
fun AppNavigationHost(
    viewModel: MainViewModel,
    bibleViewModel: BibleViewModel,
    isInPictureInPictureMode: Boolean = false,
    onEnterPipClick: (() -> Unit)? = null,
    onVideoPlayingStateChanged: (Boolean) -> Unit = {},
    externalRoute: AppRoute? = null,
    onClearExternalRoute: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()
    val strings = appStrings()
    val context = LocalContext.current
    val activity = context as? Activity
    var currentDestination by remember { mutableStateOf(MainDestination.HOME) }
    var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.Main) }
    var readingPlanTabMode by remember { mutableStateOf(com.example.ui.bible.ReadingTabMode.PLANS) }
    val galleryPhotos by viewModel.galleryPhotos.collectAsState()

    // Global Video State handling across all screens and interfaces
    val activeVideo by GlobalVideoPlayerState.currentVideo.collectAsState()
    val isMiniPlayerActive by GlobalVideoPlayerState.isMiniPlayerActive.collectAsState()
    val isVideoPlaying by GlobalVideoPlayerState.isPlaying.collectAsState()

    // Manage background playback service notification
    LaunchedEffect(activeVideo, isMiniPlayerActive, isVideoPlaying) {
        val video = activeVideo
        if (video != null && isMiniPlayerActive) {
            VideoPlaybackForegroundService.startService(
                context = context,
                videoId = video.id,
                title = video.title,
                channel = video.channelTitle,
                isPlaying = isVideoPlaying
            )
        } else if (video == null || !isMiniPlayerActive) {
            VideoPlaybackForegroundService.stopService(context)
        }
    }

    // Notify activity of video playing state for PiP onUserLeaveHint and reset orientation when leaving video
    LaunchedEffect(currentRoute) {
        val isVideo = currentRoute is AppRoute.YouTubePlayer
        onVideoPlayingStateChanged(isVideo)
        if (!isVideo) {
            (context as? ComponentActivity)?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Handle deep link / widget launch route
    LaunchedEffect(externalRoute) {
        if (externalRoute != null) {
            currentRoute = externalRoute
            onClearExternalRoute()
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    LaunchedEffect(Unit) {
        if (drawerState.isOpen) {
            drawerState.snapTo(DrawerValue.Closed)
        }
    }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Handle back press
    BackHandler(enabled = drawerState.isOpen || currentRoute != AppRoute.Main || currentDestination != MainDestination.HOME) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            when (val route = currentRoute) {
                is AppRoute.PostDetail -> {
                    currentDestination = route.originDestination
                    currentRoute = route.previousRoute
                }
                is AppRoute.YouTubePlayer -> {
                    (context as? ComponentActivity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    GlobalVideoPlayerState.minimizeToMiniPlayer()
                    currentRoute = route.previousRoute
                }
                is AppRoute.PlaylistDetail -> {
                    currentRoute = AppRoute.Main
                }
                is AppRoute.BibleReader, AppRoute.BibleSearch, AppRoute.BibleSaved, AppRoute.BibleReadingPlan -> {
                    currentRoute = AppRoute.BibleHome
                }
                is AppRoute.BibleHome -> {
                    currentRoute = AppRoute.Main
                }
                is AppRoute.PhotoViewer -> {
                    currentRoute = route.previousRoute
                }
                is AppRoute.EventCalendar -> {
                    currentRoute = AppRoute.UpcomingEvents
                }
                is AppRoute.AdminPanel -> {
                    currentRoute = AppRoute.Main
                }
                AppRoute.Main -> {
                    if (currentDestination != MainDestination.HOME) {
                        currentDestination = MainDestination.HOME
                    }
                }
                else -> {
                    currentRoute = AppRoute.Main
                }
            }
        }
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val currentAdmin by viewModel.currentAdmin.collectAsState()
    val allAdmins by viewModel.allAdmins.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    var showUpdateModalFromSidebar by remember { mutableStateOf(false) }

    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showAdminInvitationDialog by remember { mutableStateOf(false) }

    if (showFeedbackDialog) {
        com.example.ui.components.FeedbackDialog(
            onDismissRequest = { showFeedbackDialog = false }
        )
    }

    if (showAdminInvitationDialog) {
        com.example.ui.components.AdminInvitationAccessDialog(
            viewModel = viewModel,
            onDismiss = { showAdminInvitationDialog = false },
            onOpenNormalProfile = {
                showAdminInvitationDialog = false
                currentRoute = AppRoute.UserProfile
            },
            onOpenAdminPanel = {
                showAdminInvitationDialog = false
                currentRoute = AppRoute.AdminPanel
            }
        )
    }

    if (showUpdateModalFromSidebar) {
        AppUpdateModalDialog(
            viewModel = viewModel,
            onDismissRequest = { showUpdateModalFromSidebar = false },
            onOpenSettings = {
                currentRoute = AppRoute.Settings(scrollToUpdate = true)
            }
        )
    }

    val isBibleReaderScreen = currentRoute is AppRoute.BibleReader
    val isSongBookScreen = currentRoute is AppRoute.Lyrics

    val isRightDrawer = settings.drawerPosition == "right"
    CompositionLocalProvider(
        LocalLayoutDirection provides (if (isRightDrawer) LayoutDirection.Rtl else LayoutDirection.Ltr)
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = settings.isDrawerEnabled && !isBibleReaderScreen && !isSongBookScreen,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    if (settings.isDrawerEnabled) {
                        com.example.ui.components.SidebarContent(
                            currentRouteName = when (currentRoute) {
                                is AppRoute.Main -> currentDestination.name
                                is AppRoute.DedicatedNotes -> "NOTES"
                                is AppRoute.Lyrics -> "LYRICS"
                                is AppRoute.Gallery -> "PHOTOS"
                                is AppRoute.UpcomingEvents -> "EVENTS"
                                is AppRoute.EventCalendar -> "CALENDAR"
                                is AppRoute.SavedItems -> "SAVED"
                                is AppRoute.RecentlyViewed -> "RECENT"
                                is AppRoute.Search -> "SEARCH"
                                is AppRoute.Categories -> "CATEGORIES"
                                is AppRoute.SyncCenter -> "SYNC"
                                is AppRoute.BackupRestore -> "BACKUP"
                                is AppRoute.HomeScreenSettings -> "CUSTOMIZE_HOME"
                                is AppRoute.DailyPrayer -> "DAILY_PRAYER"
                                is AppRoute.NotificationHistory -> "NOTIFICATIONS"
                                is AppRoute.UserProfile -> "USER_PROFILE"
                                is AppRoute.Settings -> "SETTINGS"
                                is AppRoute.About -> "ABOUT"
                                else -> ""
                            },
                            settings = settings,
                            drawerPosition = settings.drawerPosition,
                            userProfile = userProfile,
                            currentAdmin = currentAdmin,
                            allAdmins = allAdmins,
                            isUpdateAvailable = updateState.isUpdateAvailable,
                            onToggleSidebarPosition = {
                                val nextPos = if (settings.drawerPosition == "right") "left" else "right"
                                viewModel.updateDrawerPosition(nextPos)
                            },
                            onNavigate = { routeKey ->
                                when (routeKey) {
                                    "HOME" -> { currentRoute = AppRoute.Main; currentDestination = MainDestination.HOME }
                                    "USER_PROFILE" -> {
                                        if (currentAdmin != null) {
                                            currentRoute = AppRoute.AdminPanel
                                        } else {
                                            currentRoute = AppRoute.UserProfile
                                        }
                                    }
                                    "ADMIN_PANEL" -> { currentRoute = AppRoute.AdminPanel }
                                    "DAILY_PRAYER" -> { currentRoute = AppRoute.DailyPrayer() }
                                    "BIBLE" -> { currentDestination = MainDestination.HOME; currentRoute = AppRoute.BibleHome }
                                    "READING" -> { currentRoute = AppRoute.Main; currentDestination = MainDestination.READING }
                                    "NOTES" -> { currentRoute = AppRoute.DedicatedNotes }
                                    "LYRICS" -> { currentRoute = AppRoute.Lyrics() }
                                    "PHOTOS" -> { currentRoute = AppRoute.Gallery }
                                    "BLOGS" -> { currentRoute = AppRoute.Main; currentDestination = MainDestination.BLOGS }
                                    "YOUTUBE" -> { currentRoute = AppRoute.Main; currentDestination = MainDestination.YOUTUBE }
                                    "EVENTS" -> { currentRoute = AppRoute.UpcomingEvents }
                                    "CALENDAR" -> { currentRoute = AppRoute.EventCalendar }
                                    "SAVED" -> { currentRoute = AppRoute.SavedItems }
                                    "RECENT" -> { currentRoute = AppRoute.RecentlyViewed }
                                    "SEARCH" -> { currentRoute = AppRoute.Search }
                                    "CATEGORIES" -> { currentRoute = AppRoute.Categories }
                                    "SYNC" -> { currentRoute = AppRoute.SyncCenter }
                                    "BACKUP" -> { currentRoute = AppRoute.BackupRestore }
                                    "CUSTOMIZE_HOME" -> { currentRoute = AppRoute.HomeScreenSettings }
                                    "NOTIFICATIONS" -> { currentRoute = AppRoute.NotificationHistory }
                                    "SETTINGS" -> { currentRoute = AppRoute.Settings() }
                                    "FEEDBACK" -> { showFeedbackDialog = true }
                                    "ABOUT" -> { currentRoute = AppRoute.About }
                                    "SHARE" -> {
                                        try {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Check out New Creation Church & Fellowship App!\nhttps://vinaykumaravj.blogspot.com/"
                                                )
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share App"))
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                    "URL_CHURCH" -> {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@newcreationchurchministry51015"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                    "URL_WORSHIP" -> {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@vinaykumaravjworship"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                    "URL_MAIN" -> {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@vinaykumaravj"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                    "URL_BLOG" -> {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vinaykumaravj.blogspot.com/"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                }
                            },
                            onCloseSidebar = {
                                coroutineScope.launch {
                                    if (drawerState.isOpen) {
                                        drawerState.close()
                                    }
                                }
                            },
                            onToggleTheme = {
                                val nextTheme = when (settings.themeMode) {
                                    ThemeMode.LIGHT -> ThemeMode.DARK
                                    ThemeMode.DARK -> ThemeMode.DYNAMIC
                                    ThemeMode.DYNAMIC -> ThemeMode.SYSTEM
                                    ThemeMode.SYSTEM -> ThemeMode.LIGHT
                                }
                                viewModel.updateThemeMode(nextTheme)
                            },
                            onCheckUpdate = {
                                viewModel.checkForAppUpdates(force = true)
                                showUpdateModalFromSidebar = true
                            },
                            onOpenFeedback = {
                                showFeedbackDialog = true
                            }
                        )
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val route = currentRoute) {
                        is AppRoute.Main -> {
                            val isPersonalVlogAllowed by viewModel.isPersonalVlogAllowed.collectAsState()
                            var blogsInitialTab by remember { mutableStateOf(com.example.ui.screens.BlogTab.FELLOWSHIP) }
                            val userProfile by viewModel.userProfile.collectAsState()
                            val currentAdmin by viewModel.currentAdmin.collectAsState()
                            val isMasterOrAdmin = currentAdmin != null || com.example.util.ProfileManager.isVinayProfile()
                            val userId = userProfile?.deviceId.takeIf { !it.isNullOrBlank() } ?: userProfile?.phoneNumber.takeIf { !it.isNullOrBlank() } ?: "local_user"
                            val isChatAllowed = settings.isChatEnabled && (
                                isMasterOrAdmin ||
                                settings.chatWhitelistedUserIds.contains(userId) ||
                                ((!settings.chatAllowOnlyVerified || userProfile?.isVerifiedVishwasi == true) &&
                                 (settings.chatAllowedRoles.isEmpty() || settings.chatAllowedRoles.contains(userProfile?.role)))
                            )
                            val navigationConfig by viewModel.adminNavigationConfig.collectAsState()
                            val mainTabs = remember(navigationConfig, settings.isChatEnabled, isChatAllowed) {
                                val activeConfig = if (navigationConfig.isNotEmpty()) {
                                    navigationConfig.filter { it.isVisible }.sortedBy { it.order }
                                } else {
                                    com.example.data.model.getDefaultNavigationTabs().filter { it.isVisible }
                                }
                                activeConfig.mapNotNull { config ->
                                    when (config.id.uppercase()) {
                                        "HOME" -> MainDestination.HOME
                                        "CHAT" -> if (isChatAllowed) MainDestination.CHAT else null
                                        "BLOGS" -> MainDestination.BLOGS
                                        "YOUTUBE" -> MainDestination.YOUTUBE
                                        "BIBLE" -> MainDestination.BIBLE
                                        "READING" -> MainDestination.READING
                                        else -> MainDestination.HOME
                                    }
                                }.distinct()
                            }
                            val mainPagerState = rememberPagerState(
                                initialPage = mainTabs.indexOf(currentDestination).coerceAtLeast(0)
                            ) { mainTabs.size }

                            // Bi-directional synchronization: swiping pager updates selected bottom tab
                            LaunchedEffect(mainPagerState.currentPage) {
                                val targetDest = mainTabs.getOrNull(mainPagerState.currentPage)
                                if (targetDest != null && targetDest != currentDestination) {
                                    currentDestination = targetDest
                                }
                            }

                            // Bi-directional synchronization: tapping bottom tab smoothly scrolls pager
                            LaunchedEffect(currentDestination) {
                                val targetIndex = mainTabs.indexOf(currentDestination)
                                if (targetIndex >= 0 && targetIndex != mainPagerState.currentPage) {
                                    mainPagerState.animateScrollToPage(targetIndex)
                                }
                            }

                            val context = LocalContext.current

                            Box(modifier = Modifier.fillMaxSize()) {
                                Scaffold(
                                    bottomBar = {
                                        Column {
                                            BibleMiniAudioPlayer(
                                                audioManager = bibleViewModel.audioManager,
                                                onOpenReader = { bId, chap, vNum ->
                                                    currentRoute = AppRoute.BibleReader(bId, chap, vNum, isReadingPlanMode = false)
                                                }
                                            )
                                            NavigationBar(
                                                tonalElevation = 6.dp,
                                                modifier = Modifier.testTag("bottom_nav_bar")
                                            ) {
                                    mainTabs.forEach { dest ->
                                        val selected = currentDestination == dest
                                        val labelText = when (dest) {
                                            MainDestination.HOME -> strings.navHome
                                            MainDestination.CHAT -> "चैट"
                                            MainDestination.BLOGS -> strings.navBlogs
                                            MainDestination.YOUTUBE -> strings.navYouTube
                                            MainDestination.BIBLE -> "Bible"
                                            MainDestination.READING -> "Reading"
                                        }
                                        val iconVector = when (dest) {
                                            MainDestination.HOME -> Icons.Default.Home
                                            MainDestination.CHAT -> Icons.Default.Chat
                                            MainDestination.BLOGS -> Icons.Default.Article
                                            MainDestination.YOUTUBE -> Icons.Default.PlayCircle
                                            MainDestination.BIBLE -> Icons.Default.MenuBook
                                            MainDestination.READING -> Icons.Default.AutoStories
                                        }
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                currentDestination = dest
                                                coroutineScope.launch {
                                                    mainPagerState.animateScrollToPage(mainTabs.indexOf(dest))
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = iconVector,
                                                    contentDescription = labelText
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = labelText,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                            HorizontalPager(
                                state = mainPagerState,
                                userScrollEnabled = (currentRoute == AppRoute.Main),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) { page ->
                                when (mainTabs[page]) {
                                    MainDestination.CHAT -> {
                                        ChatScreen(viewModel = viewModel)
                                    }
                                    MainDestination.HOME -> {
                                        HomeScreen(
                                            viewModel = viewModel,
                                            onPostClick = { currentRoute = AppRoute.PostDetail(it, previousRoute = AppRoute.Main, originDestination = MainDestination.HOME) },
                                            onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it, previousRoute = AppRoute.Main) },
                                            onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) },
                                            onUpcomingEventsClick = { currentRoute = AppRoute.UpcomingEvents },
                                            onSavedClick = { currentRoute = AppRoute.SavedItems },
                                            onRecentlyViewedClick = { currentRoute = AppRoute.RecentlyViewed },
                                            onCustomizeHomeClick = { currentRoute = AppRoute.HomeScreenSettings },
                                            onViewAllPosts = {
                                                blogsInitialTab = com.example.ui.screens.BlogTab.FELLOWSHIP
                                                currentDestination = MainDestination.BLOGS
                                            },
                                            onViewAllPersonalPosts = {
                                                blogsInitialTab = com.example.ui.screens.BlogTab.PERSONAL
                                                currentDestination = MainDestination.BLOGS
                                            },
                                            onViewAllVideos = { currentDestination = MainDestination.YOUTUBE },
                                            onViewGallery = { currentRoute = AppRoute.Gallery },
                                            onSongBookClick = { currentRoute = AppRoute.Lyrics() },
                                            onDailyPrayerClick = { currentRoute = AppRoute.DailyPrayer() },
                                            onReadVerse = { bId, chap, verseNum ->
                                                currentRoute = AppRoute.BibleReader(bId, chap, verseNum, isReadingPlanMode = false)
                                            },
                                            onOpenDrawer = {
                                                coroutineScope.launch {
                                                    if (drawerState.isClosed) {
                                                        drawerState.open()
                                                    }
                                                }
                                            },
                                            onReadingPlanClick = { planItem ->
                                                if (planItem != null && planItem.targetBookId != null && planItem.targetChapter != null) {
                                                    val sChapter = planItem.targetStartChapter
                                                    val eChapter = planItem.targetEndChapter
                                                    val sVerse = planItem.targetStartVerse ?: 1
                                                    val eV = planItem.targetEndVerse
                                                    val readingIntent = Intent(context, MainActivity::class.java).apply {
                                                        putExtra("isReadingPlanMode", true)
                                                        putExtra("TargetBook", planItem.targetBookId)
                                                        putExtra("TargetStartChapter", sChapter)
                                                        putExtra("TargetStartVerse", sVerse)
                                                        putExtra("TargetEndChapter", eChapter)
                                                        if (eV != null) {
                                                            putExtra("TargetEndVerse", eV)
                                                        }
                                                    }
                                                    activity?.intent = readingIntent
                                                    currentRoute = AppRoute.BibleReader(
                                                        bookId = planItem.targetBookId,
                                                        chapter = sChapter,
                                                        targetVerse = sVerse,
                                                        isReadingPlanMode = true,
                                                        highlightStartVerse = sVerse,
                                                        highlightEndVerse = eV,
                                                        targetBookId = planItem.targetBookId,
                                                        targetStartChapter = sChapter,
                                                        targetStartVerse = sVerse,
                                                        targetEndChapter = eChapter,
                                                        targetEndVerse = eV
                                                    )
                                                } else {
                                                    readingPlanTabMode = com.example.ui.bible.ReadingTabMode.PLANS
                                                    currentDestination = MainDestination.READING
                                                }
                                            },
                                            onNavigateToInsights = {
                                                readingPlanTabMode = com.example.ui.bible.ReadingTabMode.INSIGHTS
                                                currentDestination = MainDestination.READING
                                            },
                                            onNotificationClick = {
                                                currentRoute = AppRoute.NotificationHistory
                                            }
                                        )
                                    }
                                    MainDestination.BLOGS -> {
                                        BlogsScreen(
                                            viewModel = viewModel,
                                            onPostClick = { currentRoute = AppRoute.PostDetail(it, previousRoute = AppRoute.Main, originDestination = MainDestination.BLOGS) },
                                            onSearchClick = { currentRoute = AppRoute.Search },
                                            initialTab = blogsInitialTab
                                        )
                                    }
                                    MainDestination.YOUTUBE -> {
                                        YouTubeScreen(
                                            viewModel = viewModel,
                                            onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it, previousRoute = AppRoute.Main) },
                                            onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) }
                                        )
                                    }
                                    MainDestination.BIBLE -> {
                                        com.example.ui.bible.BibleHomeScreen(
                                            viewModel = bibleViewModel,
                                            onBackClick = { currentDestination = MainDestination.HOME },
                                            onOpenReader = { b, c, v ->
                                                currentDestination = MainDestination.HOME
                                                currentRoute = AppRoute.BibleReader(b, c, v, isReadingPlanMode = false)
                                            },
                                            onSearchClick = {
                                                currentDestination = MainDestination.HOME
                                                currentRoute = AppRoute.BibleSearch
                                            },
                                            onSavedClick = {
                                                currentDestination = MainDestination.HOME
                                                currentRoute = AppRoute.BibleSaved
                                            },
                                            onReadingPlanClick = {
                                                readingPlanTabMode = com.example.ui.bible.ReadingTabMode.PLANS
                                                currentDestination = MainDestination.READING
                                            }
                                        )
                                    }
                                    MainDestination.READING -> {
                                        com.example.ui.bible.BibleReadingPlanScreen(
                                            planRepository = viewModel.readingPlanRepository,
                                            onBackClick = { currentDestination = MainDestination.HOME },
                                            initialTabMode = readingPlanTabMode,
                                            onOpenBible = { bId, ch, sVerse, eVerse, sChap, eChap ->
                                                val sChapter = sChap ?: ch
                                                val eChapter = eChap ?: sChapter
                                                val readingIntent = Intent(context, MainActivity::class.java).apply {
                                                    putExtra("isReadingPlanMode", true)
                                                    putExtra("TargetBook", bId)
                                                    putExtra("TargetStartChapter", sChapter)
                                                    putExtra("TargetStartVerse", sVerse ?: 1)
                                                    putExtra("TargetEndChapter", eChapter)
                                                    if (eVerse != null) {
                                                        putExtra("TargetEndVerse", eVerse)
                                                    }
                                                }
                                                activity?.intent = readingIntent
                                                currentRoute = AppRoute.BibleReader(
                                                    bookId = bId,
                                                    chapter = ch,
                                                    targetVerse = sVerse,
                                                    isReadingPlanMode = true,
                                                    highlightStartVerse = sVerse,
                                                    highlightEndVerse = eVerse,
                                                    targetBookId = bId,
                                                    targetStartChapter = sChapter,
                                                    targetStartVerse = sVerse,
                                                    targetEndChapter = eChapter,
                                                    targetEndVerse = eVerse
                                                )
                                            },
                                             behindColorHex = settings.planBehindColorHex,
                                            onTrackColorHex = settings.planOnTrackColorHex,
                                            completedColorHex = settings.planCompletedColorHex,
                                            onUpdateColors = { b, t, c -> viewModel.updateReadingPlanColors(b, t, c) },
                                            bibleViewModel = bibleViewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

        is AppRoute.PostDetail -> {
            PostDetailScreen(
                post = route.post,
                viewModel = viewModel,
                onBack = { currentRoute = route.previousRoute },
                onImageClick = { imgUrl ->
                    val postPhotos = route.post.allImages.map { url ->
                        GalleryPhoto(
                            imageUrl = url,
                            postTitle = route.post.title,
                            postId = route.post.id,
                            source = route.post.source,
                            publishedDate = route.post.publishedDate
                        )
                    }
                    val targetList = if (postPhotos.isNotEmpty()) postPhotos else galleryPhotos
                    val idx = targetList.indexOfFirst { it.imageUrl == imgUrl }.coerceAtLeast(0)
                    currentRoute = AppRoute.PhotoViewer(
                        photos = targetList,
                        initialIndex = idx,
                        previousRoute = AppRoute.PostDetail(route.post, previousRoute = route.previousRoute)
                    )
                },
                onVideoClick = { vidId ->
                    val video = YouTubeVideo(
                        id = vidId,
                        title = route.post.title,
                        channelId = "",
                        channelTitle = "Vinay Kumar AVJ",
                        thumbnailUrl = "https://i.ytimg.com/vi/$vidId/hqdefault.jpg",
                        publishedAt = route.post.publishedDate,
                        publishedTimestamp = route.post.publishedTimestamp,
                        description = route.post.plainTextExcerpt
                    )
                    currentRoute = AppRoute.YouTubePlayer(video, previousRoute = route)
                },
                onPostClick = { currentRoute = AppRoute.PostDetail(it, previousRoute = route.previousRoute) },
                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) }
            )
        }

        is AppRoute.YouTubePlayer -> {
            LaunchedEffect(route.video) {
                GlobalVideoPlayerState.openVideo(route.video)
            }
            VideoPlayerScreen(
                video = route.video,
                viewModel = viewModel,
                isInPictureInPictureMode = isInPictureInPictureMode,
                onEnterPipClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                        onEnterPipClick?.invoke()
                    } else {
                        (context as? ComponentActivity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        GlobalVideoPlayerState.minimizeToMiniPlayer()
                        currentRoute = route.previousRoute
                    }
                },
                onBack = {
                    (context as? ComponentActivity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    GlobalVideoPlayerState.minimizeToMiniPlayer()
                    currentRoute = route.previousRoute
                },
                onRelatedVideoClick = { currentRoute = AppRoute.YouTubePlayer(it, previousRoute = route.previousRoute) }
            )
        }

        is AppRoute.PlaylistDetail -> {
            PlaylistDetailScreen(
                playlist = route.playlist,
                viewModel = viewModel,
                onBack = { currentRoute = AppRoute.Main },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it, previousRoute = route) }
            )
        }

        is AppRoute.Gallery -> {
            val galleryPhotos by viewModel.galleryPhotos.collectAsState()
            GalleryScreen(
                viewModel = viewModel,
                onPhotoClick = { idx ->
                    currentRoute = AppRoute.PhotoViewer(
                        photos = galleryPhotos,
                        initialIndex = idx,
                        previousRoute = AppRoute.Gallery
                    )
                },
                onAlbumClick = { albumPhotos, idx ->
                    currentRoute = AppRoute.PhotoViewer(
                        photos = albumPhotos,
                        initialIndex = idx,
                        previousRoute = AppRoute.Gallery
                    )
                }
            )
        }

        is AppRoute.PhotoViewer -> {
            PhotoViewerScreen(
                photos = route.photos,
                initialIndex = route.initialIndex,
                onBack = { currentRoute = route.previousRoute }
            )
        }

        is AppRoute.UpcomingEvents, is AppRoute.EventCalendar -> {
            EventsScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it, previousRoute = AppRoute.UpcomingEvents) },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.SavedItems -> {
            SavedScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it, previousRoute = AppRoute.SavedItems) },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it, previousRoute = AppRoute.SavedItems) },
                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) },
                onBibleClick = { bId, ch -> currentRoute = AppRoute.BibleReader(bId, ch, isReadingPlanMode = false) },
                onSongClick = { songId -> currentRoute = AppRoute.Lyrics(initialSongId = songId) },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.RecentlyViewed -> {
            RecentlyViewedScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it, previousRoute = AppRoute.RecentlyViewed) },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it, previousRoute = AppRoute.RecentlyViewed) },
                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) },
                onBibleClick = { bId, ch -> currentRoute = AppRoute.BibleReader(bId, ch, isReadingPlanMode = false) },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.HomeScreenSettings -> {
            HomeScreenSettingsScreen(
                viewModel = viewModel,
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.Search -> {
            SearchScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it, previousRoute = AppRoute.Search) },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it, previousRoute = AppRoute.Search) },
                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) },
                onBibleClick = { bId, ch -> currentRoute = AppRoute.BibleReader(bId, ch, isReadingPlanMode = false) },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.Categories -> {
            CategoriesScreen(
                viewModel = viewModel,
                onCategorySelected = { label ->
                    viewModel.setSearchQuery(label)
                    currentRoute = AppRoute.Search
                },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.Settings -> {
            SettingsScreen(
                viewModel = viewModel,
                onAboutClick = { currentRoute = AppRoute.About },
                onCustomizeHomeClick = { currentRoute = AppRoute.HomeScreenSettings },
                onSyncCenterClick = { currentRoute = AppRoute.SyncCenter },
                onBackupRestoreClick = { currentRoute = AppRoute.BackupRestore },
                onBack = { currentRoute = AppRoute.Main },
                scrollToUpdateSection = route.scrollToUpdate
            )
        }

        is AppRoute.About -> {
            val isPersonalVlogAllowed by viewModel.isPersonalVlogAllowed.collectAsState()
            AboutScreen(
                onBack = { currentRoute = AppRoute.Main },
                isPersonalVlogAllowed = isPersonalVlogAllowed
            )
        }

        is AppRoute.BibleReadingPlan -> {
            com.example.ui.bible.BibleReadingPlanScreen(
                planRepository = viewModel.readingPlanRepository,
                onBackClick = { currentRoute = AppRoute.Main },
                initialTabMode = readingPlanTabMode,
                onOpenBible = { bId, ch, sVerse, eVerse, sChap, eChap ->
                    val sChapter = sChap ?: ch
                    val eChapter = eChap ?: sChapter
                    val readingIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("isReadingPlanMode", true)
                        putExtra("TargetBook", bId)
                        putExtra("TargetStartChapter", sChapter)
                        putExtra("TargetStartVerse", sVerse ?: 1)
                        putExtra("TargetEndChapter", eChapter)
                        if (eVerse != null) {
                            putExtra("TargetEndVerse", eVerse)
                        }
                    }
                    activity?.intent = readingIntent
                    currentRoute = AppRoute.BibleReader(
                        bookId = bId,
                        chapter = ch,
                        targetVerse = sVerse,
                        isReadingPlanMode = true,
                        highlightStartVerse = sVerse,
                        highlightEndVerse = eVerse,
                        targetBookId = bId,
                        targetStartChapter = sChapter,
                        targetStartVerse = sVerse,
                        targetEndChapter = eChapter,
                        targetEndVerse = eVerse
                    )
                },
                behindColorHex = settings.planBehindColorHex,
                onTrackColorHex = settings.planOnTrackColorHex,
                completedColorHex = settings.planCompletedColorHex,
                onUpdateColors = { b, t, c -> viewModel.updateReadingPlanColors(b, t, c) },
                bibleViewModel = bibleViewModel
            )
        }

        is AppRoute.DedicatedNotes -> {
            StudyNotesScreen(
                studyNotesRepository = viewModel.studyNotesRepository,
                onBackClick = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.Lyrics -> {
            LyricsScreen(
                lyricsRepository = viewModel.lyricsRepository,
                bibleRepository = viewModel.bibleRepository,
                initialSongId = route.initialSongId,
                onBackClick = { currentRoute = AppRoute.Main },
                onOpenVerse = { bId, ch, v -> currentRoute = AppRoute.BibleReader(bId, ch, v) }
            )
        }

        is AppRoute.SyncCenter -> {
            SyncCenterScreen(
                syncRepository = viewModel.syncCenterRepository,
                onBackClick = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.BackupRestore -> {
            BackupRestoreScreen(
                backupRepository = viewModel.backupRepository,
                onBackClick = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.BibleHome -> {
            BibleHomeScreen(
                viewModel = bibleViewModel,
                onBackClick = {
                    currentDestination = MainDestination.HOME
                    currentRoute = AppRoute.Main
                },
                onOpenReader = { bId, chap, targetV ->
                    currentDestination = MainDestination.HOME
                    currentRoute = AppRoute.BibleReader(bId, chap, targetV, isReadingPlanMode = false)
                },
                onSearchClick = {
                    currentDestination = MainDestination.HOME
                    currentRoute = AppRoute.BibleSearch
                },
                onSavedClick = {
                    currentDestination = MainDestination.HOME
                    currentRoute = AppRoute.BibleSaved
                },
                onReadingPlanClick = {
                    currentDestination = MainDestination.HOME
                    currentRoute = AppRoute.BibleReadingPlan
                }
            )
        }

        is AppRoute.BibleReader -> {
            BibleReaderScreen(
                viewModel = bibleViewModel,
                bookId = route.bookId,
                chapter = route.chapter,
                targetVerse = route.targetVerse,
                isReadingPlanMode = route.isReadingPlanMode,
                highlightStartVerse = if (route.isReadingPlanMode) route.highlightStartVerse else null,
                highlightEndVerse = if (route.isReadingPlanMode) route.highlightEndVerse else null,
                targetBookId = if (route.isReadingPlanMode) route.targetBookId else null,
                targetStartChapter = if (route.isReadingPlanMode) route.targetStartChapter else null,
                targetStartVerse = if (route.isReadingPlanMode) route.targetStartVerse else null,
                targetEndChapter = if (route.isReadingPlanMode) route.targetEndChapter else null,
                targetEndVerse = if (route.isReadingPlanMode) route.targetEndVerse else null,
                onBackClick = { currentRoute = AppRoute.BibleHome },
                onSearchClick = { currentRoute = AppRoute.BibleSearch },
                onSavedClick = { currentRoute = AppRoute.BibleSaved },
                onReadingPlanClick = { currentRoute = AppRoute.BibleReadingPlan }
            )
        }

        is AppRoute.BibleSearch -> {
            BibleSearchScreen(
                viewModel = bibleViewModel,
                onBackClick = { currentRoute = AppRoute.BibleHome },
                onVerseClick = { bId, chap, verseNum ->
                    currentRoute = AppRoute.BibleReader(bId, chap, verseNum, isReadingPlanMode = false)
                }
            )
        }

        is AppRoute.BibleSaved -> {
            BibleSavedScreen(
                viewModel = bibleViewModel,
                onBackClick = { currentRoute = AppRoute.BibleHome },
                onVerseClick = { bId, chap, verseNum ->
                    currentRoute = AppRoute.BibleReader(bId, chap, verseNum, isReadingPlanMode = false)
                }
            )
        }

        is AppRoute.DailyPrayer -> {
            com.example.ui.prayer.DailyPrayerScreen(
                initialPrayerId = route.prayerId,
                onBackClick = { currentRoute = AppRoute.Main },
                onOpenBible = { bId, chap, v ->
                    currentRoute = AppRoute.BibleReader(
                        bookId = bId,
                        chapter = chap,
                        targetVerse = v,
                        isReadingPlanMode = false
                    )
                },
                viewModel = viewModel
            )
        }

        is AppRoute.NotificationHistory -> {
            com.example.ui.screens.NotificationHistoryScreen(
                viewModel = viewModel,
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.UserProfile -> {
            com.example.ui.screens.UserProfileScreen(
                viewModel = viewModel,
                onBack = { currentRoute = AppRoute.Main },
                onOpenAdminPanel = { currentRoute = AppRoute.AdminPanel },
                onOpenBible = { bId, chap, v ->
                    currentRoute = AppRoute.BibleReader(bId, chap, v, isReadingPlanMode = false)
                },
                onOpenPrayer = {
                    currentRoute = AppRoute.DailyPrayer()
                },
                onOpenNotes = {
                    currentRoute = AppRoute.DedicatedNotes
                }
            )
        }

        is AppRoute.AdminPanel -> {
            com.example.ui.admin.AdminPanelScreen(
                viewModel = viewModel,
                onNavigateBack = { currentRoute = AppRoute.Main }
            )
        }
    }

    if (currentRoute !is AppRoute.Main && currentRoute !is AppRoute.BibleReader) {
        BibleMiniAudioPlayer(
            audioManager = bibleViewModel.audioManager,
            onOpenReader = { bId, chap, vNum ->
                currentRoute = AppRoute.BibleReader(bId, chap, vNum, isReadingPlanMode = false)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        )
    }

    // Global Floating In-App PiP Video Player: persists across ALL screens and interfaces
    if (currentRoute !is AppRoute.YouTubePlayer) {
        val navBottomPadding = when (currentRoute) {
            is AppRoute.Main -> 90.dp
            is AppRoute.BibleReader -> 72.dp
            else -> 24.dp
        }
        YouTubeMiniPlayer(
            videoPlayerContent = {
                activeVideo?.let { vid ->
                    UniversalVideoPlayer(
                        videoUrlOrId = vid.id,
                        modifier = Modifier.fillMaxSize(),
                        autoplay = true
                    )
                }
            },
            onExpand = { video ->
                GlobalVideoPlayerState.expandToFullScreen()
                currentRoute = AppRoute.YouTubePlayer(video, previousRoute = currentRoute)
            },
            onClose = {
                GlobalVideoPlayerState.closePlayer()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = navBottomPadding, end = 12.dp)
        )
    }
}
}
}
}
}
