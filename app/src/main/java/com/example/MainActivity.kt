package com.example

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
    BLOGS("Blogs", Icons.Default.Article),
    YOUTUBE("YouTube", Icons.Default.PlayCircle),
    BIBLE("Bible", Icons.Default.MenuBook),
    READING("Reading", Icons.Default.AutoStories)
}

sealed interface AppRoute {
    data object Main : AppRoute
    data class PostDetail(val post: BlogPost) : AppRoute
    data class YouTubePlayer(val video: YouTubeVideo) : AppRoute
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
}

class MainActivity : ComponentActivity() {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to enter PiP mode: ${e.message}")
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isCurrentlyPlayingVideo) {
            enterPipModeIfSupported()
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

            CompositionLocalProvider(
                LocalAppStrings provides appStrings,
                LocalAppLanguage provides settings.appLanguage,
                LocalAppProfile provides currentProfile
            ) {
                MyApplicationTheme(
                    darkTheme = isDarkTheme,
                    dynamicColor = isDynamicTheme
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        AppNavigationHost(
                            viewModel = viewModel,
                            bibleViewModel = bibleViewModel,
                            isInPictureInPictureMode = isInPipState.value,
                            onEnterPipClick = { enterPipModeIfSupported() },
                            onVideoPlayingStateChanged = { isPlaying -> isCurrentlyPlayingVideo = isPlaying },
                            externalRoute = externalRouteState.value,
                            onClearExternalRoute = { externalRouteState.value = null }
                        )
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
    }

    override fun onPause() {
        super.onPause()
        com.example.util.WelcomeSpeechManager.getInstance(applicationContext).setAppInForeground(false)
        com.example.util.WelcomeSpeechManager.getInstance(applicationContext).stop()
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

    // Notify activity of video playing state for PiP onUserLeaveHint
    LaunchedEffect(currentRoute) {
        onVideoPlayingStateChanged(currentRoute is AppRoute.YouTubePlayer)
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
            when (currentRoute) {
                is AppRoute.BibleReader, AppRoute.BibleSearch, AppRoute.BibleSaved, AppRoute.BibleReadingPlan -> {
                    currentDestination = MainDestination.HOME
                    currentRoute = AppRoute.BibleHome
                }
                is AppRoute.BibleHome -> {
                    currentDestination = MainDestination.HOME
                    currentRoute = AppRoute.Main
                }
                is AppRoute.PhotoViewer -> {
                    currentRoute = (currentRoute as AppRoute.PhotoViewer).previousRoute
                }
                is AppRoute.EventCalendar -> {
                    currentRoute = AppRoute.UpcomingEvents
                }
                AppRoute.Main -> {
                    if (currentDestination != MainDestination.HOME) {
                        currentDestination = MainDestination.HOME
                    }
                }
                else -> {
                    currentDestination = MainDestination.HOME
                    currentRoute = AppRoute.Main
                }
            }
        }
    }

    val updateState by viewModel.updateState.collectAsState()
    var showUpdateModalFromSidebar by remember { mutableStateOf(false) }

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
                                is AppRoute.Settings -> "SETTINGS"
                                is AppRoute.About -> "ABOUT"
                                else -> ""
                            },
                            settings = settings,
                            drawerPosition = settings.drawerPosition,
                            isUpdateAvailable = updateState.isUpdateAvailable,
                            onToggleSidebarPosition = {
                                val nextPos = if (settings.drawerPosition == "right") "left" else "right"
                                viewModel.updateDrawerPosition(nextPos)
                            },
                            onNavigate = { routeKey ->
                                when (routeKey) {
                                    "HOME" -> { currentRoute = AppRoute.Main; currentDestination = MainDestination.HOME }
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
                            }
                        )
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                when (val route = currentRoute) {
                    is AppRoute.Main -> {
                        val mainTabs = remember {
                            listOf(
                                MainDestination.HOME,
                                MainDestination.BLOGS,
                                MainDestination.YOUTUBE,
                                MainDestination.BIBLE,
                                MainDestination.READING
                            )
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

                        Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    tonalElevation = 6.dp,
                                    modifier = Modifier.testTag("bottom_nav_bar")
                                ) {
                                    mainTabs.forEach { dest ->
                                        val selected = currentDestination == dest
                                        val labelText = when (dest) {
                                            MainDestination.HOME -> strings.navHome
                                            MainDestination.BLOGS -> strings.navBlogs
                                            MainDestination.YOUTUBE -> strings.navYouTube
                                            MainDestination.BIBLE -> "Bible"
                                            MainDestination.READING -> "Reading"
                                        }
                                        val iconVector = when (dest) {
                                            MainDestination.HOME -> Icons.Default.Home
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
                                    MainDestination.HOME -> {
                                        HomeScreen(
                                            viewModel = viewModel,
                                            onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                                            onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) },
                                            onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) },
                                            onUpcomingEventsClick = { currentRoute = AppRoute.UpcomingEvents },
                                            onSavedClick = { currentRoute = AppRoute.SavedItems },
                                            onRecentlyViewedClick = { currentRoute = AppRoute.RecentlyViewed },
                                            onCustomizeHomeClick = { currentRoute = AppRoute.HomeScreenSettings },
                                            onViewAllPosts = { currentDestination = MainDestination.BLOGS },
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
                                            onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                                            onSearchClick = { currentRoute = AppRoute.Search }
                                        )
                                    }
                                    MainDestination.YOUTUBE -> {
                                        YouTubeScreen(
                                            viewModel = viewModel,
                                            onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) },
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

        is AppRoute.PostDetail -> {
            PostDetailScreen(
                post = route.post,
                viewModel = viewModel,
                onBack = { currentRoute = AppRoute.Main },
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
                        previousRoute = AppRoute.PostDetail(route.post)
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
                    currentRoute = AppRoute.YouTubePlayer(video)
                },
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) }
            )
        }

        is AppRoute.YouTubePlayer -> {
            VideoPlayerScreen(
                video = route.video,
                viewModel = viewModel,
                isInPictureInPictureMode = isInPictureInPictureMode,
                onEnterPipClick = onEnterPipClick,
                onBack = { currentRoute = AppRoute.Main },
                onRelatedVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) }
            )
        }

        is AppRoute.PlaylistDetail -> {
            PlaylistDetailScreen(
                playlist = route.playlist,
                viewModel = viewModel,
                onBack = { currentRoute = AppRoute.Main },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) }
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

        is AppRoute.UpcomingEvents -> {
            UpcomingEventsScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onOpenCalendarView = { currentRoute = AppRoute.EventCalendar },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.EventCalendar -> {
            EventCalendarScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onBack = { currentRoute = AppRoute.UpcomingEvents }
            )
        }

        is AppRoute.SavedItems -> {
            SavedScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) },
                onPlaylistClick = { currentRoute = AppRoute.PlaylistDetail(it) },
                onBibleClick = { bId, ch -> currentRoute = AppRoute.BibleReader(bId, ch, isReadingPlanMode = false) },
                onSongClick = { songId -> currentRoute = AppRoute.Lyrics(initialSongId = songId) },
                onBack = { currentRoute = AppRoute.Main }
            )
        }

        is AppRoute.RecentlyViewed -> {
            RecentlyViewedScreen(
                viewModel = viewModel,
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) },
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
                onPostClick = { currentRoute = AppRoute.PostDetail(it) },
                onVideoClick = { currentRoute = AppRoute.YouTubePlayer(it) },
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
            AboutScreen(
                onBack = { currentRoute = AppRoute.Main }
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
                }
            )
        }

        is AppRoute.NotificationHistory -> {
            com.example.ui.screens.NotificationHistoryScreen(
                viewModel = viewModel,
                onBack = { currentRoute = AppRoute.Main }
            )
        }
    }
}
}
}
}
