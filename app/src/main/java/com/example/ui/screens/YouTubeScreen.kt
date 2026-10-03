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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    val isPersonalVlogAllowed by viewModel.isPersonalVlogAllowed.collectAsStateWithLifecycle()
    val isMasterOrAdmin = currentAdmin != null || com.example.util.ProfileManager.isVinayProfile()
    var showVideoBarManagerDialog by remember { mutableStateOf(false) }

    val videoQuickAccessConfig by viewModel.videoQuickAccessConfig.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.youtubePlaylists.collectAsStateWithLifecycle()

    val visibleTabs = remember(videoQuickAccessConfig, isPersonalVlogAllowed) {
        val baseItems = if (videoQuickAccessConfig.items.isEmpty()) {
            com.example.data.model.defaultVideoQuickAccessItems()
        } else {
            val existingIds = videoQuickAccessConfig.items.map { it.id }.toSet()
            val mergedList = videoQuickAccessConfig.items.toMutableList()
            if (!existingIds.contains("dailymotion_main") && !existingIds.contains("dailymotion_vlog")) {
                val dmIdx = mergedList.indexOfFirst { it.id == "dailymotion" || it.filterValue == "dailymotion" }
                val mainDm = com.example.data.model.VideoQuickAccessItem(id = "dailymotion_main", label = "डेलीमोशन (मुख्य)", filterType = "CHANNEL", filterValue = "dailymotion_main", isVisible = true, isPinned = false, order = 4)
                val vlogDm = com.example.data.model.VideoQuickAccessItem(id = "dailymotion_vlog", label = "डेलीमोशन (पर्सनल)", filterType = "CHANNEL", filterValue = "dailymotion_vlog", isVisible = true, isPinned = false, order = 5)
                if (dmIdx >= 0) {
                    mergedList.removeAt(dmIdx)
                    mergedList.add(dmIdx, vlogDm)
                    mergedList.add(dmIdx, mainDm)
                } else {
                    mergedList.add(mainDm)
                    mergedList.add(vlogDm)
                }
            }
            if (!existingIds.contains("other_videos")) {
                mergedList.add(com.example.data.model.VideoQuickAccessItem(id = "other_videos", label = "अन्य", filterType = "OTHER", filterValue = "other_videos", isVisible = true, isPinned = false, order = 6))
            }
            mergedList
        }

        baseItems.filter { item ->
            val isPersonalVlog = item.id == "dailymotion_vlog" || item.filterValue == "dailymotion_vlog"
            item.isVisible && (!isPersonalVlog || isPersonalVlogAllowed)
        }
    }

    var selectedTabId by remember {
        mutableStateOf(
            when (settings.youtubeDefaultTab) {
                YouTubeDefaultTab.ALL -> "all"
                YouTubeDefaultTab.AVJ_WORSHIP -> "worship"
                YouTubeDefaultTab.VINAY_KUMAR_AVJ -> "vinay_kumar"
                YouTubeDefaultTab.NEW_CREATION_CHURCH -> "new_creation_church"
            }
        )
    }

    val currentTab = remember(visibleTabs, selectedTabId) {
        visibleTabs.firstOrNull { it.id == selectedTabId } ?: visibleTabs.firstOrNull() ?: com.example.data.model.VideoQuickAccessItem(id = "all", label = "ALL", filterType = "ALL")
    }

    val dmMainVideos = remember(allVideos) {
        allVideos.filter { candidate ->
            val isDm = candidate.id.startsWith("dm_") ||
                    candidate.channelId == DailymotionFeedService.CHANNEL_MAIN_ID
            val isVlog = candidate.id.startsWith("dm_${DailymotionFeedService.CHANNEL_VLOG_ID}_") ||
                    candidate.channelId == DailymotionFeedService.CHANNEL_VLOG_ID ||
                    candidate.channelTitle.contains("vlog", ignoreCase = true) ||
                    candidate.title.contains("vlog", ignoreCase = true)
            isDm && !isVlog
        }
    }

    val dmVlogVideos = remember(allVideos, isPersonalVlogAllowed) {
        if (!isPersonalVlogAllowed) emptyList()
        else {
            allVideos.filter { candidate ->
                candidate.id.startsWith("dm_${DailymotionFeedService.CHANNEL_VLOG_ID}_") ||
                        candidate.channelId == DailymotionFeedService.CHANNEL_VLOG_ID ||
                        candidate.channelTitle.contains("vlog", ignoreCase = true) ||
                        candidate.title.contains("vlog", ignoreCase = true)
            }
        }
    }

    val otherSectionVideos = remember(allVideos, allPlaylists) {
        val otherPlaylistVideoIds = allPlaylists
            .filter { it.displayTarget == "OTHER_ONLY" || it.displayTarget == "ALL" }
            .flatMap { it.videoIds + it.videoUrls.mapNotNull { url -> com.example.util.VideoUrlParser.parse(url).videoId.ifBlank { null } } }
            .toSet()

        allVideos.filter { candidate ->
            candidate.channelId == "other_videos" ||
            candidate.channelId == "custom_other" ||
            candidate.channelTitle.contains("अन्य", ignoreCase = true) ||
            candidate.channelTitle.contains("Other", ignoreCase = true) ||
            candidate.id.startsWith("custom_") ||
            otherPlaylistVideoIds.contains(candidate.id) ||
            otherPlaylistVideoIds.any { candidate.videoUrl.contains(it) }
        }
    }

    var dynamicMixSeed by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Filtered & sorted videos: Dynamic non-definite order for ALL tab (sometimes latest, sometimes mixed random across sources)
    val displayVideos = remember(allVideos, allPlaylists, currentTab, dynamicMixSeed, isPersonalVlogAllowed) {
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
                if (currentTab.filterValue.equals("dailymotion_main", ignoreCase = true) || currentTab.id == "dailymotion_main") {
                    sortedAll.filter { candidate ->
                        val isDm = candidate.id.startsWith("dm_") ||
                                candidate.channelId == DailymotionFeedService.CHANNEL_MAIN_ID
                        val isVlog = candidate.id.startsWith("dm_${DailymotionFeedService.CHANNEL_VLOG_ID}_") ||
                                candidate.channelId == DailymotionFeedService.CHANNEL_VLOG_ID ||
                                candidate.channelTitle.contains("vlog", ignoreCase = true) ||
                                candidate.title.contains("vlog", ignoreCase = true)
                        isDm && !isVlog
                    }.ifEmpty { sortedAll.filter { it.id.startsWith("dm_") } }
                } else if (currentTab.filterValue.equals("dailymotion_vlog", ignoreCase = true) || currentTab.id == "dailymotion_vlog") {
                    if (!isPersonalVlogAllowed) {
                        emptyList()
                    } else {
                        sortedAll.filter { candidate ->
                            candidate.id.startsWith("dm_${DailymotionFeedService.CHANNEL_VLOG_ID}_") ||
                                    candidate.channelId == DailymotionFeedService.CHANNEL_VLOG_ID ||
                                    candidate.channelTitle.contains("vlog", ignoreCase = true) ||
                                    candidate.title.contains("vlog", ignoreCase = true)
                        }
                    }
                } else if (currentTab.filterValue.equals("dailymotion", ignoreCase = true) || currentTab.id == "dailymotion") {
                    sortedAll.filter { candidate ->
                        val isVlog = candidate.id.startsWith("dm_${DailymotionFeedService.CHANNEL_VLOG_ID}_") ||
                                candidate.channelId == DailymotionFeedService.CHANNEL_VLOG_ID ||
                                candidate.channelTitle.contains("vlog", ignoreCase = true) ||
                                candidate.title.contains("vlog", ignoreCase = true)
                        val isDm = candidate.id.startsWith("dm_") ||
                                candidate.channelId == DailymotionFeedService.CHANNEL_MAIN_ID ||
                                candidate.channelId == DailymotionFeedService.CHANNEL_VLOG_ID
                        isDm && (!isVlog || isPersonalVlogAllowed)
                    }.ifEmpty { sortedAll }
                } else if (currentTab.filterValue == PredefinedPlaylists.channelWorship.id || currentTab.id == "worship") {
                    sortedAll.filter { it.channelId == PredefinedPlaylists.channelWorship.id }.ifEmpty { sortedAll }
                } else if (currentTab.filterValue == PredefinedPlaylists.channelMain.id || currentTab.id == "vinay_kumar") {
                    sortedAll.filter { it.channelId == PredefinedPlaylists.channelMain.id }.ifEmpty { sortedAll }
                } else if (currentTab.filterValue == PredefinedPlaylists.channelNewCreationChurch.id || currentTab.id == "new_creation_church") {
                    sortedAll.filter { it.channelId == PredefinedPlaylists.channelNewCreationChurch.id || it.channelTitle.contains("New Creation", ignoreCase = true) }.ifEmpty { sortedAll }
                } else {
                    sortedAll.filter { it.channelId == currentTab.filterValue || it.channelTitle.contains(currentTab.filterValue, ignoreCase = true) }.ifEmpty { sortedAll }
                }
            }
            "OTHER" -> {
                // Section "अन्य" (Custom Admin videos & target "OTHER_ONLY" or "ALL" playlists)
                val otherPlaylistVideoIds = allPlaylists
                    .filter { it.displayTarget == "OTHER_ONLY" || it.displayTarget == "ALL" }
                    .flatMap { it.videoIds + it.videoUrls.mapNotNull { url -> com.example.util.VideoUrlParser.parse(url).videoId.ifBlank { null } } }
                    .toSet()

                val otherVideos = sortedAll.filter { candidate ->
                    candidate.channelId == "other_videos" ||
                    candidate.channelId == "custom_other" ||
                    candidate.channelTitle.contains("अन्य", ignoreCase = true) ||
                    candidate.channelTitle.contains("Other", ignoreCase = true) ||
                    candidate.id.startsWith("custom_") ||
                    otherPlaylistVideoIds.contains(candidate.id) ||
                    otherPlaylistVideoIds.any { candidate.videoUrl.contains(it) }
                }

                if (otherVideos.isEmpty()) {
                    sortedAll.filter { it.isRemote || it.id.startsWith("custom_") }.ifEmpty { sortedAll }
                } else {
                    otherVideos
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
        currentTab.id == "new_creation_church" || currentTab.filterValue == PredefinedPlaylists.channelNewCreationChurch.id -> PredefinedPlaylists.channelNewCreationChurch.id
        currentTab.id == "dailymotion_main" || currentTab.filterValue.equals("dailymotion_main", ignoreCase = true) -> DailymotionFeedService.CHANNEL_MAIN_ID
        currentTab.id == "dailymotion_vlog" || currentTab.filterValue.equals("dailymotion_vlog", ignoreCase = true) -> DailymotionFeedService.CHANNEL_VLOG_ID
        currentTab.id == "dailymotion" || currentTab.filterValue.equals("dailymotion", ignoreCase = true) -> DailymotionFeedService.CHANNEL_MAIN_ID
        else -> currentTab.filterValue.ifBlank { null }
    }

    val rawPlaylists = remember(allPlaylists, currentTab) {
        when (currentTab.filterType) {
            "ALL" -> allPlaylists.filter { it.displayTarget != "OTHER_ONLY" }
            "OTHER" -> allPlaylists.filter { it.displayTarget == "OTHER_ONLY" || it.displayTarget == "ALL" }
            "CHANNEL" -> {
                val candidateList = allPlaylists.filter { it.displayTarget != "OTHER_ONLY" }
                if (currentTab.filterValue.equals("dailymotion_main", ignoreCase = true) || currentTab.id == "dailymotion_main" ||
                    currentTab.filterValue.equals("dailymotion", ignoreCase = true) || currentTab.id == "dailymotion") {
                    candidateList.filter { it.channelTitle.contains("Dailymotion", ignoreCase = true) }.ifEmpty { candidateList }
                } else if (currentTab.filterValue.equals("dailymotion_vlog", ignoreCase = true) || currentTab.id == "dailymotion_vlog") {
                    candidateList.filter { it.channelTitle.contains("vlog", ignoreCase = true) }.ifEmpty { candidateList }
                } else if (currentTab.filterValue == PredefinedPlaylists.channelWorship.id || currentTab.id == "worship") {
                    candidateList.filter { it.channelTitle.contains("Worship", ignoreCase = true) }.ifEmpty { candidateList }
                } else if (currentTab.filterValue == PredefinedPlaylists.channelMain.id || currentTab.id == "vinay_kumar") {
                    candidateList.filter { it.channelTitle.contains("Vinay Kumar", ignoreCase = true) && !it.channelTitle.contains("Worship", ignoreCase = true) }.ifEmpty { candidateList }
                } else if (currentTab.filterValue == PredefinedPlaylists.channelNewCreationChurch.id || currentTab.id == "new_creation_church") {
                    candidateList.filter { it.channelTitle.contains("New Creation", ignoreCase = true) }.ifEmpty { candidateList }
                } else {
                    candidateList.filter { it.channelTitle.contains(currentTab.filterValue, ignoreCase = true) }.ifEmpty { candidateList }
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
            // Header (Single line, no extra details text)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (settings.appLanguage == com.example.data.model.AppLanguage.HINDI) "यूट्यूब वीडियो व मीडिया" else "YouTube Videos & Media",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

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
                            .padding(horizontal = 16.dp, vertical = 2.dp),
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

            // Section: Random Video Suggestion Widget
            item {
                RandomVideoCard(
                    video = suggestedVideo,
                    onPlayClick = onVideoClick,
                    onShuffleClick = { randomVideoSeed++ },
                    onChannelSelect = { randomChannelFilter = it },
                    selectedChannelFilter = randomChannelFilter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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

            // Section: Dailymotion Main Channel Videos (डेलीमोशन मुख्य चैनल)
            if (currentTab.filterType == "ALL" && dmMainVideos.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "डेलीमोशन (मुख्य चैनल / Dailymotion Main)",
                        subtitle = "आराधना, प्रवचन व मुख्य वीडियो",
                        modifier = Modifier.padding(top = 14.dp),
                        onViewAll = { selectedTabId = "dailymotion_main" }
                    )
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dmMainVideos.take(10), key = { "dm_main_${it.id}" }) { video ->
                            HorizontalVideoCard(
                                video = video,
                                badgeText = "डेलीमोशन मुख्य",
                                onClick = { onVideoClick(video) }
                            )
                        }
                    }
                }
            }

            // Section: Dailymotion Personal Vlog (डेलीमोशन पर्सनल व्लॉग) - केवल जब पर्सनल ब्लॉग ON हो
            if (currentTab.filterType == "ALL" && isPersonalVlogAllowed && dmVlogVideos.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "डेलीमोशन (पर्सनल व्लॉग / Personal Vlogs)",
                        subtitle = "व्यक्तिगत जीवन, यात्रा व प्रेरणा",
                        modifier = Modifier.padding(top = 14.dp),
                        onViewAll = { selectedTabId = "dailymotion_vlog" }
                    )
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dmVlogVideos.take(10), key = { "dm_vlog_${it.id}" }) { video ->
                            HorizontalVideoCard(
                                video = video,
                                badgeText = "पर्सनल व्लॉग",
                                onClick = { onVideoClick(video) }
                            )
                        }
                    }
                }
            }

            // Section: Other Videos (अन्य वीडियो व कस्टम लिंक)
            if (currentTab.filterType == "ALL" && otherSectionVideos.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "अन्य वीडियो व कस्टम लिंक (Other Videos)",
                        subtitle = "विशेष व एडमिन द्वारा जोड़े गए वीडियो",
                        modifier = Modifier.padding(top = 14.dp),
                        onViewAll = { selectedTabId = "other_videos" }
                    )
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(otherSectionVideos.take(10), key = { "other_${it.id}" }) { video ->
                            HorizontalVideoCard(
                                video = video,
                                badgeText = "अन्य",
                                onClick = { onVideoClick(video) }
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
                        YouTubeDefaultTab.VINAY_KUMAR_AVJ to "Vinay Kumar AVJ (मुख्य चैनल / प्रचार)",
                        YouTubeDefaultTab.NEW_CREATION_CHURCH to "New Creation Church"
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
                                        YouTubeDefaultTab.NEW_CREATION_CHURCH -> "new_creation_church"
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
                                            YouTubeDefaultTab.NEW_CREATION_CHURCH -> "new_creation_church"
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
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onViewAll: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onViewAll != null) {
            TextButton(
                onClick = onViewAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("सभी देखें", style = MaterialTheme.typography.labelMedium)
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
private fun HorizontalVideoCard(
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
