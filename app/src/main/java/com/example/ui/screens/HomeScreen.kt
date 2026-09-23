package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.bible.model.VerseOfTheDay
import com.example.data.bible.model.ActiveReadingPlanItem
import androidx.compose.ui.platform.testTag
import com.example.data.model.*
import com.example.data.prayer.repository.FirebaseDailyPrayerManager
import com.example.ui.components.*
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.prayer.UrgentPrayerAlertWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPostClick: (BlogPost) -> Unit,
    onVideoClick: (YouTubeVideo) -> Unit,
    onPlaylistClick: (YouTubePlaylist) -> Unit,
    onUpcomingEventsClick: () -> Unit,
    onSavedClick: () -> Unit,
    onRecentlyViewedClick: () -> Unit,
    onCustomizeHomeClick: () -> Unit,
    onViewAllPosts: () -> Unit,
    onViewAllPersonalPosts: () -> Unit = {},
    onViewAllVideos: () -> Unit,
    onViewGallery: () -> Unit,
    onReadVerse: ((bookId: Int, chapter: Int, verse: Int) -> Unit)? = null,
    onSongBookClick: (() -> Unit)? = null,
    onDailyPrayerClick: (() -> Unit)? = null,
    onOpenDrawer: (() -> Unit)? = null,
    onReadingPlanClick: ((ActiveReadingPlanItem) -> Unit)? = null,
    onNavigateToInsights: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeReadingPlans by viewModel.activeReadingPlans.collectAsStateWithLifecycle()
    val readingInsights by viewModel.readingInsights.collectAsStateWithLifecycle()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val activeAdminNotice by viewModel.activeAdminNotice.collectAsStateWithLifecycle()
    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val fellowshipPosts by viewModel.fellowshipPosts.collectAsStateWithLifecycle()
    val personalVlogPosts by viewModel.personalVlogPosts.collectAsStateWithLifecycle()
    val isPersonalVlogAllowed by viewModel.isPersonalVlogAllowed.collectAsStateWithLifecycle()
    val youtubeVideos by viewModel.youtubeVideos.collectAsStateWithLifecycle()
    val latestVideos by viewModel.latestYouTubeVideos.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()
    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()
    val galleryPhotos by viewModel.galleryPhotos.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val strings = appStrings()
    val allPlaylists by viewModel.youtubePlaylists.collectAsStateWithLifecycle()
    val mixedRandomFeed by viewModel.mixedRandomFeed.collectAsStateWithLifecycle()

    var shufflePostSeed by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var shuffleVideoSeed by remember { mutableLongStateOf(System.currentTimeMillis() + 500L) }

    val featuredPlaylists = remember(allPlaylists) {
        allPlaylists.filter { it.title.isNotBlank() && it.id.isNotBlank() }.take(6)
    }
    val previewPhotos = galleryPhotos.take(6)

    // Today's verse & Remote Config strings
    val todaysVerse = remember { VerseOfTheDay.getTodayVerse() }
    val dynamicTodayScripture by viewModel.todayScripture.collectAsStateWithLifecycle()
    val dailyGreetingText by viewModel.dailyGreetingText.collectAsStateWithLifecycle()
    val dailyGreetingConfig by viewModel.dailyGreetingConfig.collectAsStateWithLifecycle()
    val verseOfTheDayText by viewModel.verseOfTheDayText.collectAsStateWithLifecycle()
    val specialAnnouncementText by viewModel.specialAnnouncementText.collectAsStateWithLifecycle()

    val dailyPrayerManager = remember { FirebaseDailyPrayerManager.getInstance(context) }
    val firebasePrayers by dailyPrayerManager.firebasePrayers.collectAsStateWithLifecycle()
    val isTodayPrayerFromFirebase = remember(firebasePrayers) {
        val todayPrayerId = dailyPrayerManager.getEffectiveTodayPrayer().id
        firebasePrayers.containsKey(todayPrayerId)
    }

    // New Firebase Features: Live Stream, Banners, Audio Devotional, Prayer Requests
    val liveStreamInfo by viewModel.liveStreamInfo.collectAsStateWithLifecycle()
    val featuredBanners by viewModel.featuredBanners.collectAsStateWithLifecycle()
    val dailyAudioDevotional by viewModel.dailyAudioDevotional.collectAsStateWithLifecycle()
    val prayerRequests by viewModel.prayerRequests.collectAsStateWithLifecycle()
    val quickAccessConfig by viewModel.quickAccessConfig.collectAsStateWithLifecycle()

    val prayerCountText = remember(prayerRequests, quickAccessConfig.prayerCountMode) {
        when (quickAccessConfig.prayerCountMode.uppercase()) {
            "TODAY" -> {
                val calToday = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                prayerRequests.count { it.timestamp >= calToday }.toString()
            }
            "TESTIMONY" -> {
                prayerRequests.count { it.isAnswered || it.testimonyText.isNotBlank() }.toString()
            }
            "ACTIVE" -> {
                prayerRequests.count { !it.isAnswered }.toString()
            }
            else -> { // TOTAL
                prayerRequests.size.toString()
            }
        }
    }

    // Remote Config Dynamic Controls & Feature Flags
    val isLiveStreamEnabled by viewModel.isLiveStreamEnabled.collectAsStateWithLifecycle()
    val isGalleryEnabled by viewModel.isGalleryEnabled.collectAsStateWithLifecycle()
    val isPrayerRequestEnabled by viewModel.isPrayerRequestEnabled.collectAsStateWithLifecycle()
    val promoBannerImageUrl by viewModel.promoBannerImageUrl.collectAsStateWithLifecycle()
    val promoBannerTitle by viewModel.promoBannerTitle.collectAsStateWithLifecycle()
    val promoBannerLinkUrl by viewModel.promoBannerLinkUrl.collectAsStateWithLifecycle()
    val isPromoBannerEnabled by viewModel.isPromoBannerEnabled.collectAsStateWithLifecycle()

    var showPrayerRequestsDialog by remember { mutableStateOf(false) }

    var dismissedPrayerNotificationId by rememberSaveable { mutableStateOf("") }
    val latestPrayerRequest = remember(prayerRequests) {
        prayerRequests.firstOrNull { !it.isPrivate && !it.isAnswered }
    }
    val isNewPrayerRequestAvailable = remember(latestPrayerRequest, dismissedPrayerNotificationId) {
        if (latestPrayerRequest == null) false
        else if (latestPrayerRequest.id == dismissedPrayerNotificationId) false
        else {
            val lastSeenTime = com.example.util.UserDeviceHelper.getLastSeenPrayerTime(context)
            val isMine = com.example.util.UserDeviceHelper.isMyRequest(context, latestPrayerRequest.id, latestPrayerRequest.senderDeviceId)
            !isMine && (latestPrayerRequest.timestamp > lastSeenTime)
        }
    }

    // Active Urgent Prayer Alert & Automatic Window Trigger
    val activeUrgentPrayer = remember(prayerRequests) {
        prayerRequests.firstOrNull { it.isUrgent && !it.isAnswered }
    }
    var urgentPrayerDismissedId by rememberSaveable { mutableStateOf("") }
    var showUrgentPrayerWindow by remember { mutableStateOf(false) }

    val shouldAutoOpenUrgentWindow = remember(activeUrgentPrayer, urgentPrayerDismissedId) {
        if (activeUrgentPrayer == null) false
        else if (activeUrgentPrayer.id == urgentPrayerDismissedId) false
        else !com.example.util.UserDeviceHelper.isUrgentPrayerDismissed(context, activeUrgentPrayer.id)
    }

    LaunchedEffect(shouldAutoOpenUrgentWindow, activeUrgentPrayer?.id) {
        if (shouldAutoOpenUrgentWindow && activeUrgentPrayer != null) {
            showUrgentPrayerWindow = true
        }
    }

    // Welcome Customisation Dialog State
    var showWelcomeDialog by remember { mutableStateOf(false) }
    var showUpdateModalDialog by remember { mutableStateOf(false) }
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    var isNotificationBannerDismissed by rememberSaveable { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            isNotificationBannerDismissed = true
        }
    }

    val homeSectionsConfig by viewModel.homeSectionsConfig.collectAsStateWithLifecycle()
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()
    var showHomeLayoutDialog by remember { mutableStateOf(false) }

    val isMasterAdmin = currentAdmin?.isMasterAdmin() == true ||
            currentAdmin?.rank == AdminHierarchy.RANK_VINAY_KUMAR ||
            (currentAdmin?.designation?.contains("Vinay", ignoreCase = true) == true) ||
            com.example.util.ProfileManager.isVinayProfile()

    LaunchedEffect(Unit) {
        // Automatic update check on app launch
        viewModel.checkForAppUpdates(force = true)

        // Trigger welcome speech on app open
        viewModel.triggerWelcomeSpeechOnLaunch()

        // Show welcome dialog on first launch if name is blank and dialog not dismissed
        if (settings.userName.isBlank() && !settings.welcomeDialogDismissed) {
            showWelcomeDialog = true
        }
    }

    LaunchedEffect(updateState.isUpdateAvailable) {
        if (updateState.isUpdateAvailable) {
            viewModel.triggerWelcomeSpeechOnLaunch()
        }
    }

    if (showWelcomeDialog) {
        WelcomeCustomizationDialog(
            viewModel = viewModel,
            onDismiss = { showWelcomeDialog = false }
        )
    }

    if (showUpdateModalDialog) {
        AppUpdateModalDialog(
            viewModel = viewModel,
            onDismissRequest = { showUpdateModalDialog = false }
        )
    }

    if (showPrayerRequestsDialog) {
        PrayerRequestsDialog(
            viewModel = viewModel,
            onDismiss = { showPrayerRequestsDialog = false }
        )
    }

    if (showUrgentPrayerWindow && activeUrgentPrayer != null) {
        UrgentPrayerAlertWindow(
            item = activeUrgentPrayer,
            viewModel = viewModel,
            onDismiss = {
                urgentPrayerDismissedId = activeUrgentPrayer.id
                com.example.util.UserDeviceHelper.dismissUrgentPrayer(context, activeUrgentPrayer.id)
                showUrgentPrayerWindow = false
            },
            onOpenPrayerList = {
                urgentPrayerDismissedId = activeUrgentPrayer.id
                com.example.util.UserDeviceHelper.dismissUrgentPrayer(context, activeUrgentPrayer.id)
                showUrgentPrayerWindow = false
                showPrayerRequestsDialog = true
            }
        )
    }

    if (showHomeLayoutDialog) {
        com.example.ui.admin.HomeLayoutManagerDialog(
            viewModel = viewModel,
            onDismiss = { showHomeLayoutDialog = false }
        )
    }

    val isPersonalVlogOn = isPersonalVlogAllowed && (settings.personalVlogMode != com.example.data.model.PersonalVlogMode.HIDDEN || settings.showPersonalVlog)

    // Source posts from the given blog source(s), including personal vlog posts when active
    val sourcePosts = remember(allPosts, fellowshipPosts, personalVlogPosts, isPersonalVlogOn) {
        val base = if (allPosts.isNotEmpty()) allPosts else fellowshipPosts
        if (isPersonalVlogOn && personalVlogPosts.isNotEmpty()) {
            (base + personalVlogPosts).distinctBy { it.id }
        } else {
            base
        }
    }

    // By default, select and show random posts from given source
    val randomFellowshipPosts = remember(sourcePosts, settings.favoriteCategories, shufflePostSeed) {
        if (sourcePosts.isEmpty()) {
            emptyList()
        } else {
            val random = kotlin.random.Random(shufflePostSeed)
            if (settings.favoriteCategories.isEmpty()) {
                sourcePosts.shuffled(random)
            } else {
                val favs = sourcePosts.filter { p -> p.labels.any { settings.favoriteCategories.contains(it) } }
                val others = sourcePosts.filter { p -> !p.labels.any { settings.favoriteCategories.contains(it) } }
                favs.shuffled(random) + others.shuffled(random)
            }
        }
    }

    val featuredPost = randomFellowshipPosts.firstOrNull()
    val recentFellowshipPosts = randomFellowshipPosts.drop(1).take(5)

    // Source videos from the given YouTube channels
    val sourceVideos = remember(youtubeVideos, latestVideos) {
        if (youtubeVideos.isNotEmpty()) youtubeVideos else latestVideos
    }

    // By default, select and show random videos from given source, keeping pinned video on top
    val randomVideos = remember(sourceVideos, shuffleVideoSeed) {
        if (sourceVideos.isEmpty()) {
            emptyList()
        } else {
            val pinned = sourceVideos.filter { it.isPinned }
            val unpinned = sourceVideos.filter { !it.isPinned }
            (pinned + unpinned.shuffled(kotlin.random.Random(shuffleVideoSeed))).distinctBy { it.id }.take(4)
        }
    }

    // Intentional Unified Mixed Feed (Blogs + Videos)
    val displayMixedFeed = remember(mixedRandomFeed, sourcePosts, sourceVideos, shufflePostSeed) {
        if (mixedRandomFeed.isNotEmpty()) {
            mixedRandomFeed
        } else {
            val merged = mutableListOf<MixedFeedItem>()
            sourcePosts.take(12).forEach { merged.add(MixedFeedItem.BlogPostItem(it)) }
            sourceVideos.take(12).forEach { merged.add(MixedFeedItem.VideoItem(it)) }
            merged.shuffled(kotlin.random.Random(shufflePostSeed))
        }
    }

    // Dynamic Customizable Sections according to Master Admin & User order
    val effectiveOrder = remember(homeSectionsConfig, settings.homeSectionsOrder) {
        if (homeSectionsConfig.sections.isNotEmpty()) {
            val orderMap = mutableMapOf<HomeSectionType, Int>()
            homeSectionsConfig.sections.forEachIndexed { index, item ->
                when (item.id) {
                    "SEC_UPCOMING" -> orderMap[HomeSectionType.UPCOMING_EVENTS] = index
                    "SEC_FEATURED_FELLOWSHIP", "SEC_RECENT_FELLOWSHIP_HEADER" -> {
                        if (!orderMap.containsKey(HomeSectionType.FELLOWSHIP_EVENTS)) {
                            orderMap[HomeSectionType.FELLOWSHIP_EVENTS] = index
                        }
                    }
                    "SEC_LATEST_VIDEOS_HEADER" -> orderMap[HomeSectionType.LATEST_VIDEOS] = index
                    "SEC_PLAYLISTS_ROW" -> orderMap[HomeSectionType.PLAYLISTS] = index
                    "SEC_LATEST_EVENTS" -> orderMap[HomeSectionType.LATEST_EVENTS] = index
                    "SEC_PHOTOS" -> orderMap[HomeSectionType.PHOTOS] = index
                    "SEC_TODAYS_VERSE" -> orderMap[HomeSectionType.TODAYS_VERSE] = index
                    "HOME_DID_YOU_KNOW" -> orderMap[HomeSectionType.DID_YOU_KNOW] = index
                    "HOME_DAILY_QUIZ" -> orderMap[HomeSectionType.DAILY_QUIZ] = index
                    "HOME_DAILY_DEVOTIONAL" -> orderMap[HomeSectionType.DAILY_DEVOTIONAL] = index
                    "SEC_PERSONAL_VLOG_HEADER" -> orderMap[HomeSectionType.PERSONAL_VLOG] = index
                }
            }
            settings.homeSectionsOrder.sortedBy { orderMap[it] ?: 999 }
        } else {
            settings.homeSectionsOrder
        }
    }

    // Dynamic Custom Sections added by Master Admin
    val customSections = remember(homeSectionsConfig) {
        homeSectionsConfig.sections.filter { it.type == "CUSTOM" && it.isCurrentlyVisible() }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            shufflePostSeed = System.currentTimeMillis()
            shuffleVideoSeed = System.currentTimeMillis() + 500L
            viewModel.refreshAll()
        },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header
            item(key = "HOME_APP_HEADER") {
                AppHeader(
                    isUpdateAvailable = updateState.isUpdateAvailable,
                    onUpdateClick = { showUpdateModalDialog = true },
                    onOpenDrawer = onOpenDrawer,
                    isDrawerEnabled = settings.isDrawerEnabled,
                    drawerPosition = settings.drawerPosition,
                    unreadNotificationCount = unreadNotificationCount,
                    onNotificationClick = onNotificationClick
                )
            }

            // Master Admin Home Customization Quick Bar
            if (isMasterAdmin) {
                item(key = "MASTER_ADMIN_HOME_LAYOUT_CHIP") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { showHomeLayoutDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("होम पेज कस्टमाइज़र (Master Admin)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("क्रम बदलें, जोड़ें/हटाएं, समय सीमा (टाइमर) या स्थायी", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // High Priority Urgent Prayer Card (Red Emergency Alert)
            if (activeUrgentPrayer != null) {
                item(key = "HOME_URGENT_PRAYER_ALERT") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { showUrgentPrayerWindow = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "🚨 तत्काल प्रार्थना अलर्ट",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            text = "#${activeUrgentPrayer.serialNumber}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${activeUrgentPrayer.name} (${activeUrgentPrayer.userRole}): ${activeUrgentPrayer.requestText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.95f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showUrgentPrayerWindow = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("तत्काल विंडो खोलें 🚨", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.incrementPrayingCountWithLimit(context, activeUrgentPrayer.id) { success, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("प्रार्थना की 🙏 (${activeUrgentPrayer.prayingCount})", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isNewPrayerRequestAvailable && latestPrayerRequest != null) {
                item(key = "HOME_NEW_PRAYER_REQUEST_ALERT") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolunteerActivism,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "🔔 नया प्रार्थना निवेदन प्राप्त हुआ",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "#${latestPrayerRequest.serialNumber}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${latestPrayerRequest.name}: ${latestPrayerRequest.requestText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            com.example.util.UserDeviceHelper.setLastSeenPrayerTime(context, latestPrayerRequest.timestamp)
                                            showPrayerRequestsDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("प्रार्थना करें 🙏", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            dismissedPrayerNotificationId = latestPrayerRequest.id
                                            com.example.util.UserDeviceHelper.setLastSeenPrayerTime(context, latestPrayerRequest.timestamp)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("हटाएं", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!hasNotificationPermission && !isNotificationBannerDismissed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item(key = "HOME_NOTIFICATION_REMINDER_BANNER") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (settings.appLanguage == com.example.data.model.AppLanguage.HINDI)
                                        "सूचनाएं (Notifications) चालू करें"
                                    else
                                        "Enable Notifications",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (settings.appLanguage == com.example.data.model.AppLanguage.HINDI)
                                        "दैनिक वचन, प्रार्थना समय और नए अपडेट पाने के लिए अनुमति दें।"
                                    else
                                        "Stay updated with daily verses, prayer times, and announcements.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        try {
                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } catch (e: Exception) {
                                            try {
                                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (e2: Exception) {
                                                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts("package", context.packageName, null)
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(fallback)
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (settings.appLanguage == com.example.data.model.AppLanguage.HINDI) "चालू करें" else "Enable",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { isNotificationBannerDismissed = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Master Admin Home Customization Quick-Access Bar
            if (isMasterAdmin) {
                item(key = "HOME_MASTER_ADMIN_BAR") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { showHomeLayoutDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DashboardCustomize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "👑 मास्टर एडमिन: होम लेआउट कस्टमाइज़ करें",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Dynamic Greeting Banner (controlled by Remote Config daily_greeting_text)
            if (homeSectionsConfig.isSectionVisible("HOME_DAILY_GREETING")) {
                item(key = "HOME_DAILY_GREETING") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WavingHand,
                            contentDescription = "Daily Greeting",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val activeGreeting = if (dailyGreetingConfig.isExpired()) "जय मसीह की" else dailyGreetingConfig.greetingText.ifBlank { dailyGreetingText.ifBlank { "जय मसीह की" } }
                            val rawGreeting = activeGreeting.ifBlank { "जय मसीह की" }
                            val cleanName = settings.userName.trim()
                            val nameTag = if (cleanName.isNotBlank()) "$cleanName जी" else ""
                            val greetingFormatted = when {
                                rawGreeting.contains("{name}") -> {
                                    if (nameTag.isNotBlank()) rawGreeting.replace("{name}", nameTag)
                                    else rawGreeting.replace("{name}", "").replace("  ", " ").trim()
                                }
                                nameTag.isNotBlank() -> {
                                    "$rawGreeting, $nameTag!"
                                }
                                else -> {
                                    "$rawGreeting!"
                                }
                            }
                            Text(
                                text = greetingFormatted,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "आज का दिन आपके लिए आशीषमय हो 🙏",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

            // Special Announcement Banner (controlled by Remote Config special_announcement_text - View.GONE if blank/null)
            if (homeSectionsConfig.isSectionVisible("HOME_SPECIAL_ANNOUNCEMENT") && !specialAnnouncementText.isNullOrBlank()) {
                item(key = "HOME_SPECIAL_ANNOUNCEMENT") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Special Announcement",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📢 विशेष घोषणा (Special Announcement)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = specialAnnouncementText!!,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Slim, compact scrolling marquee news ticker at the very top of Home
            if (homeSectionsConfig.isSectionVisible("HOME_ADMIN_NOTICE") && activeAdminNotice != null && activeAdminNotice!!.isActive && activeAdminNotice!!.message.isNotBlank()) {
                item(key = "HOME_ADMIN_NOTICE") {
                    AdminNoticeBanner(
                        notice = activeAdminNotice,
                        onDismiss = {
                            activeAdminNotice?.id?.let { id -> viewModel.dismissAdminNotice(id) }
                        },
                        onActionClick = { url ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Live Stream Banner (Pulsing Red Live Worship Indicator)
            if (homeSectionsConfig.isSectionVisible("HOME_LIVE_STREAM_BANNER") && isLiveStreamEnabled && liveStreamInfo.isLive && liveStreamInfo.url.isNotBlank()) {
                item(key = "HOME_LIVE_STREAM_BANNER") {
                    LiveStreamBanner(
                        liveStreamInfo = liveStreamInfo,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Dynamic Promotional Banner from Firebase Remote Config
            if (homeSectionsConfig.isSectionVisible("HOME_REMOTE_PROMO_BANNER") && isPromoBannerEnabled && promoBannerImageUrl.isNotBlank()) {
                item(key = "HOME_REMOTE_PROMO_BANNER") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable(enabled = promoBannerLinkUrl.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(promoBannerLinkUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(promoBannerImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = promoBannerTitle.ifBlank { "Promotional Banner" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp, max = 220.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (promoBannerTitle.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = promoBannerTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (promoBannerLinkUrl.isNotBlank()) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Open Link",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Featured Banners & Posters Slider from Firebase
            if (homeSectionsConfig.isSectionVisible("HOME_FEATURED_BANNERS_SLIDER") && featuredBanners.isNotEmpty()) {
                item(key = "HOME_FEATURED_BANNERS_SLIDER") {
                    FeaturedBannerCarousel(
                        banners = featuredBanners,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

            // Daily Audio Devotional Card
            if (homeSectionsConfig.isSectionVisible("HOME_DAILY_AUDIO_DEVOTIONAL") && dailyAudioDevotional != null && dailyAudioDevotional!!.audioUrl.isNotBlank()) {
                item(key = "HOME_DAILY_AUDIO_DEVOTIONAL") {
                    DailyAudioDevotionalCard(
                        devotional = dailyAudioDevotional!!,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Update Notice Banner (if update is available)
            item(key = "HOME_UPDATE_NOTICE") {
                UpdateNoticeCard(viewModel = viewModel)
            }

            // Quick Access Bar (Upcoming, Saved, Recently Viewed, Prayer Requests, Customize)
            if (homeSectionsConfig.isSectionVisible("HOME_QUICK_ACCESS_BAR")) {
                item(key = "HOME_QUICK_ACCESS_BAR") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPrayerRequestEnabled && quickAccessConfig.showPrayerChip) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { showPrayerRequestsDialog = true },
                                label = { Text("${quickAccessConfig.prayerChipLabel.ifBlank { "🙏 निवेदन" }} ($prayerCountText)", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    if (quickAccessConfig.showEventChip) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = onUpcomingEventsClick,
                                label = { Text("📅 Event (${upcomingEvents.size})", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }
                    if (onSongBookClick != null && quickAccessConfig.showSongbookChip) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = onSongBookClick,
                                label = { Text("🎵 गीत पुस्तक", fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                    if (onDailyPrayerClick != null && quickAccessConfig.showDailyPrayerChip) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = onDailyPrayerClick,
                                label = { Text(if (isTodayPrayerFromFirebase) "🙏 प्रार्थना 🔥" else "🙏 प्रार्थना", fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                    if (quickAccessConfig.showSavedChip) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = onSavedClick,
                                label = { Text("${strings.chipSaved} (${savedItems.size})", fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = onRecentlyViewedClick,
                            label = { Text(strings.chipRecent, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = onCustomizeHomeClick,
                            label = { Text(strings.chipCustomize, fontWeight = FontWeight.Normal) }
                        )
                    }
                }
            }
        }

            // Offline banner if disconnected
            if (isOffline) {
                item(key = "HOME_OFFLINE_BANNER") {
                    OfflineNoticeBanner(onRetry = { viewModel.refreshAll() })
                }
            }

            // Render Dynamic Customizable Sections according to Master Admin & User order
            effectiveOrder.forEach { sectionType ->
                if (settings.enabledHomeSections.contains(sectionType)) {
                    when (sectionType) {
                        HomeSectionType.UPCOMING_EVENTS -> {
                            if (homeSectionsConfig.isSectionVisible("SEC_UPCOMING") && upcomingEvents.isNotEmpty()) {
                                item(key = "SEC_UPCOMING") {
                                    Column {
                                        SectionHeader(
                                            title = strings.secUpcomingEvents,
                                            actionTitle = "${strings.viewAll} (${upcomingEvents.size})",
                                            onAction = onUpcomingEventsClick,
                                            modifier = Modifier.padding(top = 14.dp)
                                        )
                                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                            EventCard(
                                                event = upcomingEvents.first(),
                                                onViewDetails = { onPostClick(upcomingEvents.first().post) },
                                                onScheduleReminder = { offset ->
                                                    viewModel.scheduleEventReminder(context, upcomingEvents.first(), offset)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HomeSectionType.FELLOWSHIP_EVENTS -> {
                            if (homeSectionsConfig.isSectionVisible("SEC_FEATURED_FELLOWSHIP") && featuredPost != null) {
                                item(key = "SEC_FEATURED_FELLOWSHIP") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = strings.secFeaturedFellowship,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FeaturedPostCard(
                                            post = featuredPost,
                                            onClick = { onPostClick(featuredPost) },
                                            dataSaverEnabled = settings.dataSaverEnabled
                                        )
                                    }
                                }
                            }

                            if (homeSectionsConfig.isSectionVisible("SEC_RECENT_FELLOWSHIP_HEADER") && displayMixedFeed.isNotEmpty()) {
                                item(key = "SEC_RECENT_FELLOWSHIP_HEADER") {
                                    SectionHeader(
                                        title = if (settings.favoriteCategories.isNotEmpty()) {
                                            strings.secFavoritesFirst
                                        } else {
                                            "रैंडम ब्लॉग्स व वीडियो (Random Blog & Videos)"
                                        },
                                        actionTitle = strings.viewAll,
                                        onAction = onViewAllPosts,
                                        onShuffle = {
                                            shufflePostSeed = System.currentTimeMillis()
                                            viewModel.shuffleMixedFeed()
                                        },
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }

                                items(
                                    items = displayMixedFeed.take(8),
                                    key = { item ->
                                        when (item) {
                                            is MixedFeedItem.BlogPostItem -> "MIXED_BLOG_${item.post.id}"
                                            is MixedFeedItem.VideoItem -> "MIXED_VID_${item.video.id}"
                                        }
                                    }
                                ) { item ->
                                    when (item) {
                                        is MixedFeedItem.BlogPostItem -> {
                                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                                BlogPostCard(
                                                    post = item.post,
                                                    onClick = { onPostClick(item.post) },
                                                    dataSaverEnabled = settings.dataSaverEnabled
                                                )
                                            }
                                        }
                                        is MixedFeedItem.VideoItem -> {
                                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                                CompactVideoCard(
                                                    video = item.video,
                                                    onClick = { onVideoClick(item.video) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HomeSectionType.LATEST_VIDEOS -> {
                            // Random videos across channels (AVJ Worship & Vinay Kumar AVJ) by default
                            if (homeSectionsConfig.isSectionVisible("SEC_LATEST_VIDEOS_HEADER") && randomVideos.isNotEmpty()) {
                                item(key = "SEC_LATEST_VIDEOS_HEADER") {
                                    SectionHeader(
                                        title = strings.secLatestVideos,
                                        actionTitle = strings.exploreYouTube,
                                        onAction = onViewAllVideos,
                                        onShuffle = {
                                            shuffleVideoSeed = System.currentTimeMillis()
                                        },
                                        modifier = Modifier.padding(top = 18.dp)
                                    )
                                }

                                items(
                                    items = randomVideos,
                                    key = { "VID_${it.id}" }
                                ) { video ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                                        CompactVideoCard(
                                            video = video,
                                            onClick = { onVideoClick(video) }
                                        )
                                    }
                                }
                            }
                        }

                        HomeSectionType.PLAYLISTS -> {
                            if (homeSectionsConfig.isSectionVisible("SEC_PLAYLISTS_ROW") && settings.showYouTube && featuredPlaylists.isNotEmpty()) {
                                item(key = "SEC_PLAYLISTS_HEADER") {
                                    SectionHeader(
                                        title = strings.secFeaturedPlaylists,
                                        actionTitle = strings.allPlaylists,
                                        onAction = onViewAllVideos,
                                        modifier = Modifier.padding(top = 18.dp)
                                    )
                                }

                                item(key = "SEC_PLAYLISTS_ROW") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        items(featuredPlaylists, key = { "PL_${it.id}" }) { playlist ->
                                            PlaylistCard(
                                                playlist = playlist,
                                                onClick = { onPlaylistClick(playlist) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HomeSectionType.LATEST_EVENTS -> {
                            // Secondary / Latest events stream
                            if (homeSectionsConfig.isSectionVisible("SEC_LATEST_EVENTS") && recentFellowshipPosts.isNotEmpty()) {
                                item(key = "SEC_LATEST_EVENTS") {
                                    SectionHeader(
                                        title = strings.secLatestEvents,
                                        actionTitle = strings.viewAll,
                                        onAction = onViewAllPosts,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                                items(recentFellowshipPosts.take(2), key = { "LATEST_EV_${it.id}" }) { post ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                        BlogPostCard(
                                            post = post,
                                            onClick = { onPostClick(post) }
                                        )
                                    }
                                }
                            }
                        }

                        HomeSectionType.PHOTOS -> {
                            if (homeSectionsConfig.isSectionVisible("SEC_PHOTOS") && isGalleryEnabled && previewPhotos.isNotEmpty()) {
                                item(key = "SEC_PHOTOS") {
                                    Column {
                                        SectionHeader(
                                            title = strings.secPhotoMemories,
                                            actionTitle = strings.albums,
                                            onAction = onViewGallery,
                                            modifier = Modifier.padding(top = 18.dp)
                                        )

                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        ) {
                                            items(previewPhotos, key = { it.imageUrl }) { photo ->
                                                Card(
                                                    modifier = Modifier
                                                        .size(130.dp)
                                                        .clickable { onViewGallery() },
                                                    shape = RoundedCornerShape(14.dp),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                ) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(photo.imageUrl)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = photo.postTitle,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HomeSectionType.TODAYS_VERSE -> {
                            if (homeSectionsConfig.isSectionVisible("SEC_TODAYS_VERSE")) {
                                item(key = "SEC_TODAYS_VERSE") {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 16.dp,
                                                end = 16.dp,
                                                top = 14.dp,
                                                bottom = if (activeReadingPlans.isNotEmpty()) 4.dp else 14.dp
                                            ),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        val activeVerseText = when {
                                            verseOfTheDayText.isNotBlank() -> verseOfTheDayText
                                            dynamicTodayScripture.isNotBlank() -> dynamicTodayScripture
                                            else -> todaysVerse.textHindi
                                        }
                                        val hasCustomScriptureOverride = verseOfTheDayText.isNotBlank() || dynamicTodayScripture.isNotBlank()

                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Book,
                                                    contentDescription = null,
                                                    tint = GoldWarm,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (hasCustomScriptureOverride) "${strings.secTodayScripture} 🔥" else strings.secTodayScripture,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = GoldWarm,
                                                        letterSpacing = 1.sp
                                                    )
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "\"$activeVerseText\"",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )

                                            if (!hasCustomScriptureOverride) {
                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(
                                                    text = "\"${todaysVerse.textEnglish}\"",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Bookmark,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "${todaysVerse.bookNameHindi} (${todaysVerse.bookNameEnglish}) ${todaysVerse.chapter}:${todaysVerse.verseNumber}",
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                fontSize = 13.sp
                                                            )
                                                        )
                                                    }
                                                }

                                                if (onReadVerse != null) {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            onReadVerse(todaysVerse.bookId, todaysVerse.chapter, todaysVerse.verseNumber)
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = strings.readChapter,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Compact Motivational Active Reading Progress Bar (Smooth visual bridge)
                                    val primaryActivePlan = activeReadingPlans.firstOrNull()
                                    val readingProgressFraction = remember(primaryActivePlan, readingInsights) {
                                        if (primaryActivePlan != null && primaryActivePlan.totalDays > 0) {
                                            (primaryActivePlan.completedDays.toFloat() / primaryActivePlan.totalDays.toFloat()).coerceIn(0f, 1f)
                                        } else if (readingInsights.totalVersesRead > 0) {
                                            (readingInsights.totalVersesRead.toFloat() / 31102f).coerceIn(0.01f, 1f)
                                        } else {
                                            0f
                                        }
                                    }
                                    val readingProgressPercent = (readingProgressFraction * 100).toInt()
                                    val readingProgressTitle = remember(primaryActivePlan, readingInsights, readingProgressPercent) {
                                        if (primaryActivePlan != null) {
                                            "Active Reading: $readingProgressPercent% Completed"
                                        } else if (readingInsights.totalVersesRead > 0) {
                                            "Bible Reading: $readingProgressPercent% Completed (${readingInsights.totalVersesRead} Verses)"
                                        } else {
                                            "Active Reading: Start Today's Plan"
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (primaryActivePlan != null && onReadingPlanClick != null) {
                                                        onReadingPlanClick(primaryActivePlan)
                                                    } else {
                                                        onNavigateToInsights()
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AutoStories,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = readingProgressTitle,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "$readingProgressPercent%",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                LinearProgressIndicator(
                                                    progress = { readingProgressFraction },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(5.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    drawStopIndicator = {}
                                                )
                                            }
                                        }
                                    }

                                    // Minimalist progress tracker for active Bible Reading Plans directly below Verse of the Day
                                    if (activeReadingPlans.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                                .testTag("active_reading_plans_container"),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            activeReadingPlans.forEach { planItem ->
                                                CompactReadingPlanProgressRow(
                                                    plan = planItem,
                                                    onClick = onReadingPlanClick
                                                )
                                            }
                                        }
                                    }

                                    // Compact "Your Progress" (आपकी प्रगति) card placed below active Reading Plans
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        ReadingProgressSummaryCard(
                                            insights = readingInsights,
                                            onClick = onNavigateToInsights
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HomeSectionType.DID_YOU_KNOW -> {
                        if (homeSectionsConfig.isSectionVisible("HOME_DID_YOU_KNOW")) {
                            item(key = "HOME_DID_YOU_KNOW") {
                                DidYouKnowFactCard()
                            }
                        }
                    }

                    HomeSectionType.DAILY_QUIZ -> {
                        if (homeSectionsConfig.isSectionVisible("HOME_DAILY_QUIZ")) {
                            item(key = "HOME_DAILY_QUIZ") {
                                DailyBibleQuizCard()
                            }
                        }
                    }

                    HomeSectionType.DAILY_DEVOTIONAL -> {
                        if (homeSectionsConfig.isSectionVisible("HOME_DAILY_DEVOTIONAL")) {
                            item(key = "HOME_DAILY_DEVOTIONAL") {
                                DailyDevotionalCard()
                            }
                        }
                    }

                    HomeSectionType.PERSONAL_VLOG -> {
                        if (homeSectionsConfig.isSectionVisible("SEC_PERSONAL_VLOG_HEADER") && isPersonalVlogAllowed && personalVlogPosts.isNotEmpty()) {
                            item(key = "SEC_PERSONAL_VLOG_HEADER") {
                                SectionHeader(
                                    title = strings.secPersonalVlog,
                                    actionTitle = strings.allVlogPosts,
                                    onAction = onViewAllPersonalPosts,
                                    modifier = Modifier.padding(top = 22.dp)
                                )
                            }

                            items(personalVlogPosts.take(3), key = { "VLOG_${it.id}" }) { post ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    BlogPostCard(
                                        post = post,
                                        onClick = { onPostClick(post) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dynamic Custom Sections added by Master Admin
        customSections.forEach { customSec ->
                item(key = "CUSTOM_SEC_${customSec.id}") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable(enabled = customSec.customActionUrl.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(customSec.customActionUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (customSec.customImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(customSec.customImageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = customSec.customTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            Text(
                                text = customSec.customTitle.ifBlank { customSec.title },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (customSec.customSubtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = customSec.customSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (customSec.customActionUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(customSec.customActionUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = customSec.customActionText.ifBlank { "यहाँ क्लिक करें" },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactVideoCard(
    video: YouTubeVideo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 70.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (video.channelTitle.contains("Worship", ignoreCase = true)) {
                            NavyPrimary
                        } else {
                            Color(0xFFCC0000)
                        }
                    ) {
                        Text(
                            text = video.channelTitle,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (video.isRemote) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFF5722).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "🔥",
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = video.publishedAt,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
            if (onShuffle != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onShuffle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Randomize",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
        if (actionTitle != null && onAction != null) {
            TextButton(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = actionTitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactReadingPlanProgressRow(
    plan: ActiveReadingPlanItem,
    onClick: ((ActiveReadingPlanItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick(plan) }
                else Modifier
            )
            .padding(vertical = 1.dp)
            .testTag("active_plan_row_${plan.planId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = plan.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${plan.progressPercent}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = plan.tintColor
                )
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { plan.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .clip(RoundedCornerShape(1.5.dp)),
            color = plan.tintColor,
            trackColor = plan.tintColor.copy(alpha = 0.18f)
        )
    }
}
