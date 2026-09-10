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
import com.example.ui.components.YouTubeVideoCard
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel

enum class YouTubeTabFilter {
    ALL,
    AVJ_WORSHIP,
    VINAY_KUMAR_AVJ,
    NEW_CREATION_CHURCH
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
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val allVideos by viewModel.youtubeVideos.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Default tab comes from user settings (defaulting to AVJ Worship or ALL or user preference)
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

    // Filtered & sorted videos: newest -> oldest
    val displayVideos = remember(allVideos, selectedTab) {
        val sortedAll = allVideos.sortedByDescending { it.publishedTimestamp }
        when (selectedTab) {
            YouTubeTabFilter.ALL -> sortedAll
            YouTubeTabFilter.AVJ_WORSHIP -> sortedAll.filter { it.channelId == PredefinedPlaylists.channelWorship.id }.ifEmpty { sortedAll }
            YouTubeTabFilter.VINAY_KUMAR_AVJ -> sortedAll.filter { it.channelId == PredefinedPlaylists.channelMain.id }.ifEmpty { sortedAll }
            YouTubeTabFilter.NEW_CREATION_CHURCH -> sortedAll.filter { it.channelId == PredefinedPlaylists.channelNewCreationChurch.id }.ifEmpty { sortedAll }
        }
    }

    val latestVideos = displayVideos.take(16)
    val allPlaylists by viewModel.youtubePlaylists.collectAsState()

    val playlists = remember(allPlaylists, selectedTab) {
        when (selectedTab) {
            YouTubeTabFilter.ALL -> allPlaylists
            YouTubeTabFilter.AVJ_WORSHIP -> allPlaylists.filter { it.channelTitle.contains("Worship", ignoreCase = true) }.ifEmpty { allPlaylists }
            YouTubeTabFilter.VINAY_KUMAR_AVJ -> allPlaylists.filter { it.channelTitle.contains("Vinay Kumar", ignoreCase = true) && !it.channelTitle.contains("Worship", ignoreCase = true) }.ifEmpty { allPlaylists }
            YouTubeTabFilter.NEW_CREATION_CHURCH -> allPlaylists.filter { it.channelTitle.contains("Creation", ignoreCase = true) }.ifEmpty { allPlaylists }
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
                        onClick = { selectedTab = YouTubeTabFilter.ALL },
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
                }
            }

            // Channel Hero Banner
            item {
                val currentInfo = when (selectedTab) {
                    YouTubeTabFilter.ALL -> null
                    YouTubeTabFilter.AVJ_WORSHIP -> PredefinedPlaylists.channelWorship
                    YouTubeTabFilter.VINAY_KUMAR_AVJ -> PredefinedPlaylists.channelMain
                    YouTubeTabFilter.NEW_CREATION_CHURCH -> PredefinedPlaylists.channelNewCreationChurch
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

            // Section: Latest Videos (Newest -> Oldest)
            item {
                SectionHeader(
                    title = if (selectedTab == YouTubeTabFilter.ALL) "ALL VIDEOS (NEWEST FIRST / सभी वीडियो समय अनुसार)" else "LATEST VIDEOS",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (latestVideos.isEmpty()) {
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
                items(latestVideos) { video ->
                    YouTubeVideoCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Section: Playlists
            item {
                SectionHeader(
                    title = "FEATURED PLAYLISTS",
                    modifier = Modifier.padding(top = 16.dp)
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
