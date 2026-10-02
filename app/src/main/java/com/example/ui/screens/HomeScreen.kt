package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.bible.model.ActiveReadingPlanItem
import com.example.data.bible.model.VerseOfTheDay
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit = {},
    onOpenBible: () -> Unit = {},
    onOpenLyrics: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onOpenPrayer: () -> Unit = {},
    onOpenPrayerRequests: () -> Unit = { onOpenPrayer() },
    onOpenEvents: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenUserProfile: () -> Unit = {},
    onPostClick: (BlogPost) -> Unit = {},
    onVideoClick: (YouTubeVideo) -> Unit = {},
    onPlaylistClick: (YouTubePlaylist) -> Unit = {},
    onUpcomingEventsClick: () -> Unit = onOpenEvents,
    onSavedClick: () -> Unit = {},
    onRecentlyViewedClick: () -> Unit = {},
    onCustomizeHomeClick: () -> Unit = {},
    onViewAllPosts: () -> Unit = {},
    onViewAllPersonalPosts: () -> Unit = {},
    onViewAllVideos: () -> Unit = {},
    onViewGallery: () -> Unit = {},
    onSongBookClick: () -> Unit = onOpenLyrics,
    onDailyPrayerClick: () -> Unit = onOpenPrayer,
    onReadVerse: (bookId: Int, chapter: Int, verse: Int?) -> Unit = { _, _, _ -> onOpenBible() },
    onReadingPlanClick: (ActiveReadingPlanItem?) -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNotificationClick: () -> Unit = onOpenNotifications,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProfile = LocalAppProfile.current
    val liveStreamInfo by viewModel.liveStreamInfo.collectAsState()
    val featuredBanners by viewModel.featuredBanners.collectAsState()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val youtubeVideos by viewModel.youtubeVideos.collectAsState()
    val todayVerse = remember { VerseOfTheDay.getTodayVerse() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = activeProfile.headerLogoRes),
                            contentDescription = activeProfile.displayNameEnglish,
                            modifier = Modifier
                                .fillMaxHeight()
                                .wrapContentWidth(),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Stream Banner
            item {
                LiveStreamBanner(liveStreamInfo = liveStreamInfo)
            }

            // 2. Featured Banners Carousel
            if (featuredBanners.isNotEmpty()) {
                item {
                    FeaturedBannerCarousel(banners = featuredBanners)
                }
            }

            // 3. Scrollable Quick Access Bar (निवेदन, इवेंट्स, गीत, प्रार्थना)
            item {
                QuickAccessBar(
                    onOpenPrayerRequests = onOpenPrayerRequests,
                    onOpenEvents = onOpenEvents,
                    onOpenLyrics = onOpenLyrics,
                    onOpenPrayer = onOpenPrayer
                )
            }

            // 4. Today's Bible Verse Card
            item {
                TodayVerseCard(
                    verse = todayVerse,
                    onOpenBible = onOpenBible
                )
            }

            // 5. Daily Bible Quiz Card
            item {
                DailyBibleQuizCard()
            }

            // 6. Did You Know? Bible Fact Card
            item {
                DidYouKnowFactCard()
            }

            // 7. Daily Devotional Card
            item {
                DailyDevotionalCard()
            }

            // 8. Featured Videos
            if (youtubeVideos.isNotEmpty()) {
                item {
                    SectionHeader(
                        titleHindi = "नवीनतम वीडियो एवं आराधना",
                        titleEnglish = "Latest Videos & Worship",
                        onViewAll = { }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(youtubeVideos.take(6), key = { it.id }) { video ->
                            VideoItemCard(video = video, onClick = { onVideoClick(video) })
                        }
                    }
                }
            }

            // 9. Upcoming Fellowship Events
            if (upcomingEvents.isNotEmpty()) {
                item {
                    SectionHeader(
                        titleHindi = "आगामी कार्यक्रम",
                        titleEnglish = "Upcoming Fellowship Events",
                        onViewAll = onOpenEvents
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        upcomingEvents.take(3).forEach { event ->
                            EventCard(event = event, onViewDetails = onOpenEvents)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessBar(
    onOpenPrayerRequests: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenPrayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. निवेदन (Prayer Request)
        item {
            QuickAccessChip(
                icon = Icons.Default.VolunteerActivism,
                title = "निवेदन",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onOpenPrayerRequests
            )
        }

        // 2. इवेंट्स / कैलेंडर (Events & Calendar)
        item {
            QuickAccessChip(
                icon = Icons.Default.Event,
                title = "इवेंट्स",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onOpenEvents
            )
        }

        // 3. गीत (Christian Song Book)
        item {
            QuickAccessChip(
                icon = Icons.Default.MusicNote,
                title = "गीत",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onOpenLyrics
            )
        }

        // 4. प्रार्थना (Daily Prayer)
        item {
            QuickAccessChip(
                icon = Icons.Default.Favorite,
                title = "प्रार्थना",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onOpenPrayer
            )
        }
    }
}

@Composable
private fun QuickAccessChip(
    icon: ImageVector,
    title: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = modifier
            .height(48.dp)
            .testTag("quick_access_${title}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

@Composable
private fun TodayVerseCard(
    verse: VerseOfTheDay,
    onOpenBible: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Title on left, Copy & Share buttons on top right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = "Today Verse",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "आज का वचन (Verse of the Day)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Verse", "${verse.textHindi}\n— ${verse.referenceHindi}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "वचन कॉपी किया गया", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "📖 आज का वचन:\n\n“${verse.textHindi}”\n\n— ${verse.referenceHindi}\n\nNew Creation Church App")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "वचन शेयर करें"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Verse Text
            Text(
                text = "“${verse.textHindi}”",
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Row: BOOK+CHAPTER+VERSE on bottom-left, Read in Bible button on bottom-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "📖 ${verse.referenceHindi}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                FilledTonalButton(
                    onClick = onOpenBible,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("बाइबल में पढ़ें", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    titleHindi: String,
    titleEnglish: String,
    onViewAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = titleHindi,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = titleEnglish,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun VideoItemCard(
    video: YouTubeVideo,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.width(220.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}
