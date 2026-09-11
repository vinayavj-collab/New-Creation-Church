package com.example.ui.screens

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val fellowshipPosts by viewModel.fellowshipPosts.collectAsState()
    val personalVlogPosts by viewModel.personalVlogPosts.collectAsState()
    val youtubeVideos by viewModel.youtubeVideos.collectAsState()
    val latestVideos by viewModel.latestYouTubeVideos.collectAsState()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val savedItems by viewModel.savedItems.collectAsState()
    val galleryPhotos by viewModel.galleryPhotos.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val strings = appStrings()
    val allPlaylists by viewModel.youtubePlaylists.collectAsState()

    val featuredPlaylists = remember(allPlaylists) {
        allPlaylists.filter { it.title.isNotBlank() && it.id.isNotBlank() }.take(6)
    }
    val previewPhotos = galleryPhotos.take(6)

    // Today's verse
    val todaysVerse = remember { VerseOfTheDay.getTodayVerse() }

    // Welcome Customisation Dialog State
    var showWelcomeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Trigger welcome speech on app open
        viewModel.triggerWelcomeSpeechOnLaunch()

        // Show welcome dialog on first launch if name is blank and dialog not dismissed
        if (settings.userName.isBlank() && !settings.welcomeDialogDismissed) {
            showWelcomeDialog = true
        }
    }

    if (showWelcomeDialog) {
        WelcomeCustomizationDialog(
            viewModel = viewModel,
            onDismiss = { showWelcomeDialog = false }
        )
    }

    // Prioritize fellowship posts matching favorite categories if set
    val prioritizedFellowshipPosts = remember(fellowshipPosts, settings.favoriteCategories) {
        if (settings.favoriteCategories.isEmpty()) {
            fellowshipPosts
        } else {
            val favs = fellowshipPosts.filter { p -> p.labels.any { settings.favoriteCategories.contains(it) } }
            val others = fellowshipPosts.filter { p -> !p.labels.any { settings.favoriteCategories.contains(it) } }
            favs + others
        }
    }

    val featuredPost = prioritizedFellowshipPosts.firstOrNull()
    val recentFellowshipPosts = prioritizedFellowshipPosts.drop(1).take(5)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshAll() },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header
            item {
                AppHeader()
            }

            // Quick Access Bar (Upcoming, Saved, Recently Viewed, Customize)
            item {
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
                item {
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

                            if (recentFellowshipPosts.isNotEmpty()) {
                                item(key = "SEC_RECENT_FELLOWSHIP_HEADER") {
                                    SectionHeader(
                                        title = if (settings.favoriteCategories.isNotEmpty()) {
                                            strings.secFavoritesFirst
                                        } else {
                                            strings.secRecentFellowship
                                        },
                                        actionTitle = strings.viewAll,
                                        onAction = onViewAllPosts,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }

                                items(recentFellowshipPosts, key = { "FELL_${it.id}" }) { post ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                        BlogPostCard(
                                            post = post,
                                            onClick = { onPostClick(post) },
                                            dataSaverEnabled = settings.dataSaverEnabled
                                        )
                                    }
                                }
                            }
                        }

                        HomeSectionType.LATEST_VIDEOS -> {
                            // Section 20: Latest 2 videos across channels (AVJ Worship & Vinay Kumar AVJ)
                            if (latestVideos.isNotEmpty()) {
                                item(key = "SEC_LATEST_VIDEOS") {
                                    Column {
                                        SectionHeader(
                                            title = strings.secLatestVideos,
                                            actionTitle = strings.exploreYouTube,
                                            onAction = onViewAllVideos,
                                            modifier = Modifier.padding(top = 18.dp)
                                        )

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            latestVideos.take(2).forEach { video ->
                                                CompactVideoCard(
                                                    video = video,
                                                    onClick = { onVideoClick(video) }
                                                )
                                            }
                                        }
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
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
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

                                        Text(
                                            text = "\"${todaysVerse.textHindi}\"",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "\"${todaysVerse.textEnglish}\"",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )

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
                            }
                        }

                        HomeSectionType.PERSONAL_VLOG -> {
                            if (settings.showPersonalVlog && personalVlogPosts.isNotEmpty()) {
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        )
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
