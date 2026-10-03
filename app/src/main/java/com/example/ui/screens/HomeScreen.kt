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
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
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
    val allPosts by viewModel.allPosts.collectAsState()
    val randomizedPosts = remember(allPosts) { allPosts.shuffled() }
    val isPersonalVlogAllowed by viewModel.isPersonalVlogAllowed.collectAsState()
    val personalVlogPosts by viewModel.personalVlogPosts.collectAsState()
    val personalVideos by viewModel.personalVideos.collectAsState()
    val randomizedPersonalPosts = remember(personalVlogPosts) { personalVlogPosts.shuffled() }
    val randomizedPersonalVideos = remember(personalVideos) { personalVideos.shuffled() }
    val mixedRandomFeed by viewModel.mixedRandomFeed.collectAsState()
    val todayVerse = remember { VerseOfTheDay.getTodayVerse() }
    val readingInsights by viewModel.readingInsights.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(top = 0.dp),
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
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    onOpenPrayer = onOpenPrayer,
                    onOpenNotes = onOpenNotes
                )
            }

            // 4. Today's Bible Verse Card
            item {
                TodayVerseCard(
                    verse = todayVerse,
                    onOpenBible = onOpenBible,
                    viewModel = viewModel
                )
            }

            // 4b. Bible Reading Progress Summary Card
            item {
                ReadingProgressSummaryCard(
                    insights = readingInsights,
                    onClick = { onNavigateToInsights() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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

            // 8. Articles & Conferences Section (Randomized)
            if (randomizedPosts.isNotEmpty()) {
                item {
                    SectionHeader(
                        titleHindi = "लेख एवं सम्मेलन",
                        titleEnglish = "Articles & Conferences",
                        onViewAll = onViewAllPosts
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(randomizedPosts.take(10), key = { "blog_${it.id}" }) { post ->
                            BlogItemCard(post = post, onClick = { onPostClick(post) })
                        }
                    }
                }
            }

            // 8b. Dedicated Personal Blog Section (जब पर्सनल ब्लॉग ON हो)
            if (isPersonalVlogAllowed && randomizedPersonalPosts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    SectionHeader(
                        titleHindi = "पर्सनल ब्लॉग (Personal Blog)",
                        titleEnglish = "Personal Life, Stories & Reflections",
                        onViewAll = onViewAllPersonalPosts
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(randomizedPersonalPosts.take(10), key = { "personal_blog_${it.id}" }) { post ->
                            BlogItemCard(
                                post = post,
                                badgeText = "पर्सनल ब्लॉग",
                                onClick = { onPostClick(post) }
                            )
                        }
                    }
                }
            }

            // 9. Worship & YouTube Videos Section
            if (youtubeVideos.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        titleHindi = "आराधना एवं वीडियो (Worship Videos)",
                        titleEnglish = "Worship Services & Messages",
                        onViewAll = onViewAllVideos
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(youtubeVideos.take(10), key = { "video_${it.id}" }) { video ->
                            VideoItemCard(video = video, onClick = { onVideoClick(video) })
                        }
                    }
                }
            }

            // 9b. Dedicated Personal Videos Section (जब पर्सनल ब्लॉग ON हो)
            if (isPersonalVlogAllowed && randomizedPersonalVideos.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    SectionHeader(
                        titleHindi = "पर्सनल वीडियो (Personal Videos)",
                        titleEnglish = "Personal Vlogs & Life Videos",
                        onViewAll = onViewAllVideos
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(randomizedPersonalVideos.take(10), key = { "personal_vid_${it.id}" }) { video ->
                            VideoItemCard(
                                video = video,
                                badgeText = "पर्सनल व्लॉग",
                                onClick = { onVideoClick(video) }
                            )
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
    onOpenNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        // 4. नोट्स (Study Notes)
        item {
            QuickAccessChip(
                icon = Icons.Default.NoteAlt,
                title = "नोट्स",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onOpenNotes
            )
        }

        // 5. प्रार्थना (Daily Prayer)
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
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 1.dp,
        shadowElevation = 0.5.dp,
        modifier = modifier
            .height(28.dp)
            .testTag("quick_access_${title}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = contentColor
            )
        }
    }
}

@Composable
private fun TodayVerseCard(
    verse: VerseOfTheDay,
    onOpenBible: () -> Unit,
    viewModel: com.example.ui.viewmodel.MainViewModel? = null
) {
    val context = LocalContext.current
    var isApplyingWallpaper by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Row: Title on left, Copy & Share buttons on top right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Today Verse",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "आज का वचन (Verse of the Day)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Verse", "${verse.textHindi}\n— ${verse.referenceHindi}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "वचन कॉपी किया गया", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
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
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Verse Text - matching other cards font (bodyMedium) and dynamic sizing
                Text(
                    text = "“${verse.textHindi}”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Row: BOOK+CHAPTER+VERSE on bottom-left, Wallpaper Sync & Read in Bible on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "📖 ${verse.referenceHindi}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Wallpaper Sync Button
                        OutlinedButton(
                            onClick = {
                                isApplyingWallpaper = true
                                Toast.makeText(context, "⚡ 'आज का वचन' AI वॉलपेपर होम व लॉकस्क्रीन पर सिंक हो रहा है...", Toast.LENGTH_SHORT).show()
                                viewModel?.applyDailyWallpaperWithDetails(
                                    context = context,
                                    verseText = verse.textHindi,
                                    verseRef = verse.referenceHindi,
                                    forceNext = false,
                                    targetScreen = "both"
                                ) { success, _, ref ->
                                    isApplyingWallpaper = false
                                    if (success) {
                                        Toast.makeText(context, "✅ आज का वचन ($ref) होम स्क्रीन और लॉकस्क्रीन पर सेट हो गया!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "❌ वॉलपेपर सेट करने में विफल, कृपया पुनः प्रयास करें।", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isApplyingWallpaper,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            if (isApplyingWallpaper) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            } else {
                                Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(13.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("वॉलपेपर सिंक", style = MaterialTheme.typography.labelSmall)
                        }

                        FilledTonalButton(
                            onClick = onOpenBible,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("पढ़ें", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    titleHindi: String,
    titleEnglish: String,
    onViewAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        if (onViewAll != null) {
            TextButton(
                onClick = onViewAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("सभी देखें", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun VideoItemCard(
    video: YouTubeVideo,
    badgeText: String? = null,
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
                if (!badgeText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = badgeText, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
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

@Composable
private fun BlogItemCard(
    post: BlogPost,
    badgeText: String = "लेख / Blog",
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
                    model = post.featuredImageUrl ?: "",
                    contentDescription = post.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            if (badgeText.contains("पर्सनल")) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = badgeText, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                text = post.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}
