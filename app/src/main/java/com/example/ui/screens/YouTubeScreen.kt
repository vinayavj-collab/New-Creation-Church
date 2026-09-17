package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PredefinedPlaylists
import com.example.data.model.YouTubeChannelInfo
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.data.model.YouTubeDefaultTab
import com.example.ui.components.PlaylistCard
import com.example.ui.components.RandomVideoCard
import com.example.ui.components.YouTubeVideoCard
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel

enum class YouTubeTabFilter {
    ALL,
    AVJ_WORSHIP,
    VINAY_KUMAR_AVJ,
    NEW_CREATION_CHURCH,
    DAILYMOTION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeScreen(
    viewModel: MainViewModel,
    onVideoClick: (YouTubeVideo) -> Unit,
    onPlaylistClick: (YouTubePlaylist) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val allVideos by viewModel.youtubeVideos.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isLoadingMoreVideos by viewModel.isLoadingMoreVideos.collectAsStateWithLifecycle()

    // Default tab comes from user settings (defaulting to ALL or user preference)
    var selectedTab by remember {
        mutableStateOf(
            when (settings.youtubeDefaultTab) {
                YouTubeDefaultTab.ALL -> YouTubeTabFilter.ALL
                YouTubeDefaultTab.AVJ_WORSHIP -> YouTubeTabFilter.AVJ_WORSHIP
                YouTubeDefaultTab.VINAY_KUMAR_AVJ -> YouTubeTabFilter.VINAY_KUMAR_AVJ
                YouTubeDefaultTab.NEW_CREATION_CHURCH -> YouTubeTabFilter.NEW_CREATION_CHURCH
            }
        )
    }

    var dynamicMixSeed by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Filtered & sorted videos: Dynamic non-definite order for ALL tab (sometimes latest, sometimes mixed random across sources)
    val displayVideos = remember(allVideos, selectedTab, dynamicMixSeed) {
        val validVideos = allVideos
            .filter { !it.id.startsWith("local_vid") && !it.thumbnailUrl.contains("local_vid") }
        val sortedAll = validVideos.sortedByDescending { it.publishedTimestamp }

        when (selectedTab) {
            YouTubeTabFilter.ALL -> {
                val worshipVideos = validVideos.filter { it.channelId == PredefinedPlaylists.channelWorship.id }
                val mainVideos = validVideos.filter { it.channelId == PredefinedPlaylists.channelMain.id }
                val churchVideos = validVideos.filter { it.channelId == PredefinedPlaylists.channelNewCreationChurch.id }
                val otherSources = validVideos.filter {
                    it.channelId != PredefinedPlaylists.channelWorship.id &&
                    it.channelId != PredefinedPlaylists.channelMain.id &&
                    it.channelId != PredefinedPlaylists.channelNewCreationChurch.id
                }

                // Dynamic generation: Seed-based alternation between Latest-On-Top and Multi-Source Randomized Interleaving
                if (dynamicMixSeed % 2 == 0L) {
                    // Strategy 1: Latest videos on top + Dynamic interleave for the rest
                    val topLatest = sortedAll.take(5)
                    val remainingPool = sortedAll.drop(5).shuffled()
                    (topLatest + remainingPool).distinctBy { it.id }
                } else {
                    // Strategy 2: Multi-source randomized interleaving (no fixed single channel sequence)
                    val shuffledSources = listOf(
                        worshipVideos.shuffled(),
                        mainVideos.shuffled(),
                        churchVideos.shuffled(),
                        otherSources.shuffled()
                    ).filter { it.isNotEmpty() }

                    val result = mutableListOf<YouTubeVideo>()
                    var index = 0
                    var hasMore = true
                    while (hasMore) {
                        hasMore = false
                        for (source in shuffledSources.shuffled()) {
                            if (index < source.size) {
                                result.add(source[index])
                                hasMore = true
                            }
                        }
                        index++
                    }
                    if (result.isEmpty()) sortedAll else result.distinctBy { it.id }
                }
            }
            YouTubeTabFilter.AVJ_WORSHIP -> sortedAll.filter { it.channelId == PredefinedPlaylists.channelWorship.id }.ifEmpty { sortedAll }
            YouTubeTabFilter.VINAY_KUMAR_AVJ -> sortedAll.filter { it.channelId == PredefinedPlaylists.channelMain.id }.ifEmpty { sortedAll }
            YouTubeTabFilter.NEW_CREATION_CHURCH -> sortedAll.filter { it.channelId == PredefinedPlaylists.channelNewCreationChurch.id }.ifEmpty { sortedAll }
            YouTubeTabFilter.DAILYMOTION -> sortedAll.filter {
                com.example.util.VideoUrlParser.isDailymotion(it.videoUrl) ||
                it.id.startsWith("dm_") ||
                it.videoUrl.contains("dailymotion", ignoreCase = true) ||
                it.videoUrl.contains("dai.ly", ignoreCase = true)
            }
        }
    }

    val targetChannelIdForTab = when (selectedTab) {
        YouTubeTabFilter.ALL -> null
        YouTubeTabFilter.AVJ_WORSHIP -> PredefinedPlaylists.channelWorship.id
        YouTubeTabFilter.VINAY_KUMAR_AVJ -> PredefinedPlaylists.channelMain.id
        YouTubeTabFilter.NEW_CREATION_CHURCH -> PredefinedPlaylists.channelNewCreationChurch.id
        YouTubeTabFilter.DAILYMOTION -> "DAILYMOTION"
    }

    val allPlaylists by viewModel.youtubePlaylists.collectAsStateWithLifecycle()

    val rawPlaylists = remember(allPlaylists, selectedTab) {
        when (selectedTab) {
            YouTubeTabFilter.ALL -> allPlaylists
            YouTubeTabFilter.AVJ_WORSHIP -> allPlaylists.filter { it.channelTitle.contains("Worship", ignoreCase = true) }.ifEmpty { allPlaylists }
            YouTubeTabFilter.VINAY_KUMAR_AVJ -> allPlaylists.filter { it.channelTitle.contains("Vinay Kumar", ignoreCase = true) && !it.channelTitle.contains("Worship", ignoreCase = true) }.ifEmpty { allPlaylists }
            YouTubeTabFilter.NEW_CREATION_CHURCH -> allPlaylists.filter { it.channelTitle.contains("Creation", ignoreCase = true) }.ifEmpty { allPlaylists }
            YouTubeTabFilter.DAILYMOTION -> emptyList()
        }
    }
    // Skip empty / invalid playlists
    val playlists = remember(rawPlaylists) {
        rawPlaylists.filter { it.title.isNotBlank() && it.id.isNotBlank() }
    }

    val dmVideos = remember(allVideos) {
        allVideos.filter {
            com.example.util.VideoUrlParser.isDailymotion(it.videoUrl) ||
            it.id.startsWith("dm_") ||
            it.videoUrl.contains("dailymotion", ignoreCase = true) ||
            it.videoUrl.contains("dai.ly", ignoreCase = true)
        }
    }

    val openChannelInYouTube = { channelUrl: String ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(channelUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    var showDefaultChannelDialog by remember { mutableStateOf(false) }

    // Random Video Suggestion
    var randomChannelFilter by remember { mutableStateOf("ALL") }
    var randomVideoSeed by remember { mutableStateOf(0) }
    val suggestedVideo = remember(allVideos, randomChannelFilter, randomVideoSeed) {
        val candidateList = when (randomChannelFilter) {
            "WORSHIP" -> allVideos.filter { it.channelId == PredefinedPlaylists.channelWorship.id }
            "MAIN" -> allVideos.filter { it.channelId == PredefinedPlaylists.channelMain.id }
            "CHURCH" -> allVideos.filter { it.channelId == PredefinedPlaylists.channelNewCreationChurch.id }
            else -> allVideos
        }.ifEmpty { allVideos }
        if (candidateList.isNotEmpty()) candidateList.random() else null
    }

    // Dynamic, smooth unlimited scrolling: auto-fetch more when reaching near the bottom
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 4
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoadingMoreVideos) {
            viewModel.loadMoreVideos(targetChannelIdForTab)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            dynamicMixSeed = System.currentTimeMillis()
            viewModel.refreshAll()
        },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "YouTube Channels & Media",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Worship Songs, Sermons, Gospel & Fellowship Videos",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = { showDefaultChannelDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("डिफ़ॉल्ट चैनल", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Segmented Tab Filter: ALL | AVJ WORSHIP | VINAY KUMAR AVJ | NEW CREATION CHURCH
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ALL Tab (First Tab)
                    FilterChip(
                        selected = selectedTab == YouTubeTabFilter.ALL,
                        onClick = {
                            if (selectedTab == YouTubeTabFilter.ALL) {
                                dynamicMixSeed = System.currentTimeMillis()
                            }
                            selectedTab = YouTubeTabFilter.ALL
                        },
                        label = {
                            Text(
                                text = "ALL",
                                fontWeight = if (selectedTab == YouTubeTabFilter.ALL) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (selectedTab == YouTubeTabFilter.ALL) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )

                    // AVJ Worship Tab
                    FilterChip(
                        selected = selectedTab == YouTubeTabFilter.AVJ_WORSHIP,
                        onClick = { selectedTab = YouTubeTabFilter.AVJ_WORSHIP },
                        label = {
                            Text(
                                text = "Worship",
                                fontWeight = if (selectedTab == YouTubeTabFilter.AVJ_WORSHIP) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (selectedTab == YouTubeTabFilter.AVJ_WORSHIP) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )

                    // Vinay Kumar AVJ Tab
                    FilterChip(
                        selected = selectedTab == YouTubeTabFilter.VINAY_KUMAR_AVJ,
                        onClick = { selectedTab = YouTubeTabFilter.VINAY_KUMAR_AVJ },
                        label = {
                            Text(
                                text = "Vinay Kumar AVJ",
                                fontWeight = if (selectedTab == YouTubeTabFilter.VINAY_KUMAR_AVJ) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (selectedTab == YouTubeTabFilter.VINAY_KUMAR_AVJ) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )

                    // New Creation Church Tab
                    FilterChip(
                        selected = selectedTab == YouTubeTabFilter.NEW_CREATION_CHURCH,
                        onClick = { selectedTab = YouTubeTabFilter.NEW_CREATION_CHURCH },
                        label = {
                            Text(
                                text = "New Creation Church",
                                fontWeight = if (selectedTab == YouTubeTabFilter.NEW_CREATION_CHURCH) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (selectedTab == YouTubeTabFilter.NEW_CREATION_CHURCH) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )

                    // Dailymotion Tab (डेली मोशन सेक्शन)
                    FilterChip(
                        selected = selectedTab == YouTubeTabFilter.DAILYMOTION,
                        onClick = { selectedTab = YouTubeTabFilter.DAILYMOTION },
                        label = {
                            Text(
                                text = "Dailymotion",
                                fontWeight = if (selectedTab == YouTubeTabFilter.DAILYMOTION) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (selectedTab == YouTubeTabFilter.DAILYMOTION) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Channel Hero Banner / Dailymotion Section Banner
            item {
                if (selectedTab == YouTubeTabFilter.DAILYMOTION) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0066DC)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "DM",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Dailymotion Media & Streams",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "डेली मोशन वीडियो एवं लाइव स्ट्रीम्स",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Dailymotion वीडियो लिंक सीधे ऐप में बिना किसी रुकावट के उच्च गुणवत्ता में देखे जा सकते हैं।",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                } else {
                    val currentInfo = when (selectedTab) {
                        YouTubeTabFilter.ALL -> null
                        YouTubeTabFilter.AVJ_WORSHIP -> PredefinedPlaylists.channelWorship
                        YouTubeTabFilter.VINAY_KUMAR_AVJ -> PredefinedPlaylists.channelMain
                        YouTubeTabFilter.NEW_CREATION_CHURCH -> PredefinedPlaylists.channelNewCreationChurch
                        YouTubeTabFilter.DAILYMOTION -> null
                    }

                    if (currentInfo != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(NavyPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initials = when (selectedTab) {
                                            YouTubeTabFilter.AVJ_WORSHIP -> "AVJ"
                                            YouTubeTabFilter.NEW_CREATION_CHURCH -> "NCC"
                                            else -> "VK"
                                        }
                                        Text(
                                            text = initials,
                                            color = GoldWarm,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentInfo.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = currentInfo.handle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = currentInfo.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { openChannelInYouTube(currentInfo.channelUrl) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFCC0000)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Subscriptions,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Subscribe", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { openChannelInYouTube(currentInfo.channelUrl) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Channel")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Random Video Suggestion Widget
            item {
                RandomVideoCard(
                    video = suggestedVideo,
                    onPlayClick = onVideoClick,
                    onShuffleClick = { randomVideoSeed++ },
                    onChannelSelect = { randomChannelFilter = it },
                    selectedChannelFilter = randomChannelFilter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Section: Playlists (Skipped if empty)
            if (playlists.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "FEATURED PLAYLISTS",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(playlists) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist) }
                            )
                        }
                    }
                }
            }

            // Section: Dailymotion Carousel (when on ALL tab and Dailymotion videos exist)
            if (selectedTab == YouTubeTabFilter.ALL && dmVideos.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DAILYMOTION STREAMS & VIDEOS (डेली मोशन)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0066DC)
                            )
                        )
                        TextButton(onClick = { selectedTab = YouTubeTabFilter.DAILYMOTION }) {
                            Text("See All", fontSize = 12.sp)
                        }
                    }
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dmVideos) { dmVid ->
                            Box(modifier = Modifier.width(280.dp)) {
                                YouTubeVideoCard(
                                    video = dmVid,
                                    onClick = { onVideoClick(dmVid) }
                                )
                            }
                        }
                    }
                }
            }

            // Section: Latest Videos (Newest -> Oldest)
            item {
                SectionHeader(
                    title = when (selectedTab) {
                        YouTubeTabFilter.ALL -> "ALL VIDEOS (NEWEST FIRST / सभी वीडियो समय अनुसार)"
                        YouTubeTabFilter.DAILYMOTION -> "DAILYMOTION VIDEOS & STREAMS (डेली मोशन वीडियो)"
                        else -> "LATEST VIDEOS"
                    },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (displayVideos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No videos available right now. Pull down to refresh.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = displayVideos,
                    key = { it.id }
                ) { video ->
                    YouTubeVideoCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Subtle loading spinner at the bottom while fetching next batch
                if (isLoadingMoreVideos) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Default Channel Picker Dialog
    if (showDefaultChannelDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultChannelDialog = false },
            icon = { Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("डिफ़ॉल्ट चैनल चुनें (Default Channel)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "जब भी आप YouTube टैब खोलेंगे, तो कौन सा चैनल सबसे पहले खुलेगा:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    listOf(
                        YouTubeDefaultTab.ALL to "All Channels (सभी वीडियो समय अनुसार)",
                        YouTubeDefaultTab.AVJ_WORSHIP to "AVJ Worship (आराधना गीत)",
                        YouTubeDefaultTab.VINAY_KUMAR_AVJ to "Vinay Kumar AVJ (मुख्य चैनल / प्रचार)",
                        YouTubeDefaultTab.NEW_CREATION_CHURCH to "New Creation Church (चर्च मिनिस्ट्री)"
                    ).forEach { (tabOption, labelText) ->
                        val isSelected = settings.youtubeDefaultTab == tabOption
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateYouTubeDefaultTab(tabOption)
                                    selectedTab = when (tabOption) {
                                        YouTubeDefaultTab.ALL -> YouTubeTabFilter.ALL
                                        YouTubeDefaultTab.AVJ_WORSHIP -> YouTubeTabFilter.AVJ_WORSHIP
                                        YouTubeDefaultTab.VINAY_KUMAR_AVJ -> YouTubeTabFilter.VINAY_KUMAR_AVJ
                                        YouTubeDefaultTab.NEW_CREATION_CHURCH -> YouTubeTabFilter.NEW_CREATION_CHURCH
                                    }
                                    showDefaultChannelDialog = false
                                    Toast.makeText(context, "डिफ़ॉल्ट चैनल सेट किया गया", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateYouTubeDefaultTab(tabOption)
                                        selectedTab = when (tabOption) {
                                            YouTubeDefaultTab.ALL -> YouTubeTabFilter.ALL
                                            YouTubeDefaultTab.AVJ_WORSHIP -> YouTubeTabFilter.AVJ_WORSHIP
                                            YouTubeDefaultTab.VINAY_KUMAR_AVJ -> YouTubeTabFilter.VINAY_KUMAR_AVJ
                                            YouTubeDefaultTab.NEW_CREATION_CHURCH -> YouTubeTabFilter.NEW_CREATION_CHURCH
                                        }
                                        showDefaultChannelDialog = false
                                        Toast.makeText(context, "डिफ़ॉल्ट चैनल सेट किया गया", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultChannelDialog = false }) {
                    Text("बंद करें")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}
