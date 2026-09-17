package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.example.ui.components.*
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel

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
    val verseOfTheDayText by viewModel.verseOfTheDayText.collectAsStateWithLifecycle()
    val specialAnnouncementText by viewModel.specialAnnouncementText.collectAsStateWithLifecycle()

    // Welcome Customisation Dialog State
    var showWelcomeDialog by remember { mutableStateOf(false) }
    var showUpdateModalDialog by remember { mutableStateOf(false) }
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

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

    // Source posts from the given blog source(s)
    val sourcePosts = remember(allPosts, fellowshipPosts) {
        if (allPosts.isNotEmpty()) allPosts else fellowshipPosts
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

    // By default, select and show random videos from given source
    val randomVideos = remember(sourceVideos, shuffleVideoSeed) {
        if (sourceVideos.isEmpty()) {
            emptyList()
        } else {
            sourceVideos.shuffled(kotlin.random.Random(shuffleVideoSeed)).take(4)
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

            // Dynamic Greeting Banner (controlled by Remote Config daily_greeting_text)
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
                            val greetingFormatted = if (settings.userName.isNotBlank()) {
                                "${dailyGreetingText.ifBlank { "जय मसीह की" }}, ${settings.userName.trim()} जी!"
                            } else {
                                "${dailyGreetingText.ifBlank { "जय मसीह की" }}!"
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

            // Special Announcement Banner (controlled by Remote Config special_announcement_text - View.GONE if blank/null)
            if (!specialAnnouncementText.isNullOrBlank()) {
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
            if (activeAdminNotice != null && activeAdminNotice!!.isActive && activeAdminNotice!!.message.isNotBlank()) {
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

            // Update Notice Banner (if update is available)
            item(key = "HOME_UPDATE_NOTICE") {
                UpdateNoticeCard(viewModel = viewModel)
            }

            // Quick Access Bar (Upcoming, Saved, Recently Viewed, Customize)
            item(key = "HOME_QUICK_ACCESS_BAR") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (upcomingEvents.isNotEmpty()) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = onUpcomingEventsClick,
                                label = { Text("${strings.chipEvents} (${upcomingEvents.size})", fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                    if (onSongBookClick != null) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = onSongBookClick,
                                label = { Text("🎵 गीत पुस्तक", fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                    if (onDailyPrayerClick != null) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = onDailyPrayerClick,
                                label = { Text("🙏 दैनिक प्रार्थना", fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = onSavedClick,
                            label = { Text("${strings.chipSaved} (${savedItems.size})", fontWeight = FontWeight.SemiBold) }
                        )
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

            // Offline banner if disconnected
            if (isOffline) {
                item(key = "HOME_OFFLINE_BANNER") {
                    OfflineNoticeBanner(onRetry = { viewModel.refreshAll() })
                }
            }

            // Render Dynamic Customizable Sections according to user order & toggle settings
            settings.homeSectionsOrder.forEach { sectionType ->
                if (settings.enabledHomeSections.contains(sectionType)) {
                    when (sectionType) {
                        HomeSectionType.UPCOMING_EVENTS -> {
                            if (upcomingEvents.isNotEmpty()) {
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
                            if (featuredPost != null) {
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

                            if (displayMixedFeed.isNotEmpty()) {
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
                            if (randomVideos.isNotEmpty()) {
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
                            if (settings.showYouTube && featuredPlaylists.isNotEmpty()) {
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
                            if (recentFellowshipPosts.isNotEmpty()) {
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
                            if (previewPhotos.isNotEmpty()) {
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
                                                    text = strings.secTodayScripture,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = GoldWarm,
                                                        letterSpacing = 1.sp
                                                    )
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            val activeVerseText = when {
                                                verseOfTheDayText.isNotBlank() -> verseOfTheDayText
                                                dynamicTodayScripture.isNotBlank() -> dynamicTodayScripture
                                                else -> todaysVerse.textHindi
                                            }
                                            val hasCustomScriptureOverride = activeVerseText != todaysVerse.textHindi

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
                                                Text(
                                                    text = "${todaysVerse.bookNameEnglish} (${todaysVerse.bookNameHindi}) ${todaysVerse.chapter}:${todaysVerse.verseNumber}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = NavyPrimary
                                                    )
                                                )

                                                if (onReadVerse != null) {
                                                    TextButton(
                                                        onClick = {
                                                            onReadVerse(todaysVerse.bookId, todaysVerse.chapter, todaysVerse.verseNumber)
                                                        }
                                                    ) {
                                                        Text(strings.readChapter, fontSize = 12.sp)
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

                        HomeSectionType.PERSONAL_VLOG -> {
                            if (isPersonalVlogAllowed && personalVlogPosts.isNotEmpty()) {
                                item(key = "SEC_PERSONAL_VLOG_HEADER") {
                                    SectionHeader(
                                        title = strings.secPersonalVlog,
                                        actionTitle = strings.allVlogPosts,
                                        onAction = onViewAllPosts,
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
