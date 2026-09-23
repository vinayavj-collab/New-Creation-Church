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
import androidx.compose.material.icons.filled.PushPin
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
import com.example.data.remote.DailymotionFeedService
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
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()
    val isMasterOrAdmin = currentAdmin != null || com.example.util.ProfileManager.isVinayProfile()
    var showVideoBarManagerDialog by remember { mutableStateOf(false) }

    val videoQuickAccessConfig by viewModel.videoQuickAccessConfig.collectAsStateWithLifecycle()
    val visibleTabs = remember(videoQuickAccessConfig) {
        val list = videoQuickAccessConfig.items.filter { it.isVisible }
        if (list.isEmpty()) com.example.data.model.defaultVideoQuickAccessItems() else list
    }

    var selectedTabId by remember {
        mutableStateOf(
            when (settings.youtubeDefaultTab) {
                YouTubeDefaultTab.ALL -> "all"
                YouTubeDefaultTab.AVJ_WORSHIP -> "worship"
                YouTubeDefaultTab.VINAY_KUMAR_AVJ -> "vinay_kumar"
            }
        )
    }

    val currentTab = remember(visibleTabs, selectedTabId) {
        visibleTabs.firstOrNull { it.id == selectedTabId } ?: visibleTabs.firstOrNull() ?: com.example.data.model.VideoQuickAccessItem(id = "all", label = "ALL", filterType = "ALL")
    }

    var dynamicMixSeed by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Filtered & sorted videos: Dynamic non-definite order for ALL tab (sometimes latest, sometimes mixed random across sources)
    val displayVideos = remember(allVideos, currentTab, dynamicMixSeed) {
        val validVideos = allVideos
            .filter { !it.id.startsWith("local_vid") && !it.thumbnailUrl.contains("local_vid") }
        val sortedAll = validVideos.sortedByDescending { it.publishedTimestamp }

        val rawList = when (currentTab.filterType) {
            "ALL" -> {
                val worshipVideos = validVideos.filter { it.channelId == PredefinedPlaylists.channelWorship.id }
                val mainVideos = validVideos.filter { it.channelId == PredefinedPlaylists.channelMain.id }
                val otherSources = validVideos.filter {
                    it.channelId != PredefinedPlaylists.channelWorship.id &&
                    it.channelId != PredefinedPlaylists.channelMain.id
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
            "CHANNEL" -> {
                if (currentTab.filterValue.equals("dailymotion", ignoreCase = true) || currentTab.id == "dailymotion") {
                    sortedAll.filter { it.id.startsWith("dm_") || it.channelId == DailymotionFeedService.CHANNEL_MAIN_ID || it.channelId == DailymotionFeedService.CHANNEL_VLOG_ID }.ifEmpty { sortedAll }
                } else if (currentTab.filterValue == PredefinedPlaylists.channelWorship.id || currentTab.id == "worship") {
                    sortedAll.filter { it.channelId == PredefinedPlaylists.channelWorship.id }.ifEmpty { sortedAll }
                } else if (currentTab.filterValue == PredefinedPlaylists.channelMain.id || currentTab.id == "vinay_kumar") {
                    sortedAll.filter { it.channelId == PredefinedPlaylists.channelMain.id }.ifEmpty { sortedAll }
                } else {
                    sortedAll.filter { it.channelId == currentTab.filterValue || it.channelTitle.contains(currentTab.filterValue, ignoreCase = true) }.ifEmpty { sortedAll }
                }
            }
            "KEYWORD" -> {
                val kw = currentTab.filterValue.trim().lowercase()
                if (kw.isNotBlank()) {
                    sortedAll.filter { it.title.lowercase().contains(kw) || it.description.lowercase().contains(kw) }.ifEmpty { sortedAll }
                } else {
                    sortedAll
                }
            }
            else -> sortedAll
        }
        val pinned = rawList.filter { it.isPinned }
        val unpinned = rawList.filter { !it.isPinned }
        (pinned + unpinned).distinctBy { it.id }
    }

    val targetChannelIdForTab = when {
        currentTab.filterType == "ALL" -> null
        currentTab.id == "worship" || currentTab.filterValue == PredefinedPlaylists.channelWorship.id -> PredefinedPlaylists.channelWorship.id
        currentTab.id == "vinay_kumar" || currentTab.filterValue == PredefinedPlaylists.channelMain.id -> PredefinedPlaylists.channelMain.id
        currentTab.id == "dailymotion" || currentTab.filterValue.equals("dailymotion", ignoreCase = true) -> DailymotionFeedService.CHANNEL_MAIN_ID
        else -> currentTab.filterValue.ifBlank { null }
    }

    val allPlaylists by viewModel.youtubePlaylists.collectAsStateWithLifecycle()

    val rawPlaylists = remember(allPlaylists, currentTab) {
        when (currentTab.filterType) {
            "ALL" -> allPlaylists
            "CHANNEL" -> {
                if (currentTab.filterValue.equals("dailymotion", ignoreCase = true) || currentTab.id == "dailymotion") {
                    allPlaylists.filter { it.channelTitle.contains("Dailymotion", ignoreCase = true) }.ifEmpty { allPlaylists }
                } else if (currentTab.filterValue == PredefinedPlaylists.channelWorship.id || currentTab.id == "worship") {
                    allPlaylists.filter { it.channelTitle.contains("Worship", ignoreCase = true) }.ifEmpty { allPlaylists }
                } else if (currentTab.filterValue == PredefinedPlaylists.channelMain.id || currentTab.id == "vinay_kumar") {
                    allPlaylists.filter { it.channelTitle.contains("Vinay Kumar", ignoreCase = true) && !it.channelTitle.contains("Worship", ignoreCase = true) }.ifEmpty { allPlaylists }
                } else {
                    allPlaylists.filter { it.channelTitle.contains(currentTab.filterValue, ignoreCase = true) }.ifEmpty { allPlaylists }
                }
            }
            "KEYWORD" -> {
                val kw = currentTab.filterValue.trim().lowercase()
                if (kw.isNotBlank()) {
                    allPlaylists.filter { it.title.lowercase().contains(kw) || it.channelTitle.lowercase().contains(kw) }.ifEmpty { allPlaylists }
                } else {
                    allPlaylists
                }
            }
            else -> allPlaylists
        }
    }
    // Skip empty / invalid playlists
    val playlists = remember(rawPlaylists) {
        rawPlaylists.filter { it.title.isNotBlank() && it.id.isNotBlank() }
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

            // Segmented Tab Filter: Dynamic Quick Access Bar (Controlled by Master Admin)
            if (videoQuickAccessConfig.isBarVisible || isMasterOrAdmin) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visibleTabs.forEach { tabItem ->
                            val isSelected = (currentTab.id == tabItem.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (tabItem.filterType == "ALL" && isSelected) {
                                        dynamicMixSeed = System.currentTimeMillis()
                                    }
                                    selectedTabId = tabItem.id
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (tabItem.isPinned) {
                                            Icon(
                                                Icons.Default.PushPin,
                                                contentDescription = "Pinned",
                                                modifier = Modifier.size(12.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else GoldWarm
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = tabItem.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }

                        // Admin Bar Quick Access Manager button
                        if (isMasterOrAdmin) {
                            OutlinedButton(
                                onClick = { showVideoBarManagerDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = if (!videoQuickAccessConfig.isBarVisible) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                } else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Manage Bar", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (!videoQuickAccessConfig.isBarVisible) "बार छिपा है (Settings)" else "बार सेटिंग्स",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Channel Hero Banner
            item {
                val currentInfo = when {
                    currentTab.filterType == "ALL" -> null
                    currentTab.id == "worship" || currentTab.filterValue == PredefinedPlaylists.channelWorship.id -> PredefinedPlaylists.channelWorship
                    currentTab.id == "vinay_kumar" || currentTab.filterValue == PredefinedPlaylists.channelMain.id -> PredefinedPlaylists.channelMain
                    currentTab.id == "dailymotion" || currentTab.filterValue.equals("dailymotion", ignoreCase = true) -> YouTubeChannelInfo(
                        id = DailymotionFeedService.CHANNEL_MAIN_ID,
                        name = DailymotionFeedService.CHANNEL_MAIN_TITLE,
                        handle = "dailymotion",
                        channelUrl = "https://www.dailymotion.com/x27lzjr",
                        description = "Official Dailymotion channel featuring Vinay Kumar AVJ videos and vlogs.",
                        avatarUrl = ""
                    )
                    else -> null
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
                                    val initials = when {
                                        currentTab.id == "worship" || currentTab.filterValue == PredefinedPlaylists.channelWorship.id -> "AVJ"
                                        currentTab.id == "dailymotion" || currentTab.filterValue.equals("dailymotion", ignoreCase = true) -> "DM"
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
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
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

            // Section: Latest Videos (Newest -> Oldest)
            item {
                SectionHeader(
                    title = when (currentTab.filterType) {
                        "ALL" -> "ALL VIDEOS (NEWEST FIRST / सभी वीडियो समय अनुसार)"
                        else -> "${currentTab.label.uppercase()} VIDEOS"
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
                    key = { it.id },
                    contentType = { "youtube_video_item" }
                ) { video ->
                    YouTubeVideoCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Subtle loading spinner or end of source indicator at the bottom
                if (isLoadingMoreVideos) {
                    item(key = "loading_more_indicator", contentType = "status_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "और वीडियो लोड हो रहे हैं...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                        YouTubeDefaultTab.VINAY_KUMAR_AVJ to "Vinay Kumar AVJ (मुख्य चैनल / प्रचार)"
                    ).forEach { (tabOption, labelText) ->
                        val isSelected = settings.youtubeDefaultTab == tabOption
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateYouTubeDefaultTab(tabOption)
                                    selectedTabId = when (tabOption) {
                                        YouTubeDefaultTab.ALL -> "all"
                                        YouTubeDefaultTab.AVJ_WORSHIP -> "worship"
                                        YouTubeDefaultTab.VINAY_KUMAR_AVJ -> "vinay_kumar"
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
                                        selectedTabId = when (tabOption) {
                                            YouTubeDefaultTab.ALL -> "all"
                                            YouTubeDefaultTab.AVJ_WORSHIP -> "worship"
                                            YouTubeDefaultTab.VINAY_KUMAR_AVJ -> "vinay_kumar"
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

    if (showVideoBarManagerDialog) {
        com.example.ui.admin.VideoQuickAccessManagerDialog(
            viewModel = viewModel,
            onDismiss = { showVideoBarManagerDialog = false }
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
