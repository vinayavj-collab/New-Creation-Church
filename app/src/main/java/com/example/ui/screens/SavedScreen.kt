package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
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
import com.example.data.local.SavedItemEntity
import com.example.data.model.BlogPost
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: MainViewModel,
    onPostClick: (BlogPost) -> Unit,
    onVideoClick: (YouTubeVideo) -> Unit,
    onPlaylistClick: (YouTubePlaylist) -> Unit,
    onBibleClick: (Int, Int) -> Unit,
    onSongClick: (Long) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()
    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val allVideos by viewModel.youtubeVideos.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTypeFilter by remember { mutableStateOf("ALL") }

    val filteredItems = remember(savedItems, selectedTypeFilter) {
        if (selectedTypeFilter == "ALL") savedItems
        else savedItems.filter { it.type == selectedTypeFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "⭐ Saved for Later",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All (${savedItems.size})",
                    "SONG" to "Songs (${savedItems.count { it.type == "SONG" }})",
                    "BLOG" to "Posts (${savedItems.count { it.type == "BLOG" }})",
                    "VIDEO" to "Videos (${savedItems.count { it.type == "VIDEO" }})",
                    "PLAYLIST" to "Playlists (${savedItems.count { it.type == "PLAYLIST" }})",
                    "BIBLE" to "Bible (${savedItems.count { it.type == "BIBLE" }})"
                )
                items(filters) { (typeKey, typeLabel) ->
                    FilterChip(
                        selected = selectedTypeFilter == typeKey,
                        onClick = { selectedTypeFilter = typeKey },
                        label = { Text(typeLabel) }
                    )
                }
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Saved Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Save posts, videos, playlists, or bible verses to view them offline anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        SavedItemRow(
                            item = item,
                            onClick = {
                                when (item.type) {
                                    "BLOG" -> {
                                        val pId = item.id.removePrefix("POST_")
                                        val post = allPosts.find { it.id == pId }
                                        if (post != null) onPostClick(post)
                                    }
                                    "VIDEO" -> {
                                        val vId = item.id.removePrefix("VID_")
                                        val video = allVideos.find { it.id == vId }
                                        if (video != null) onVideoClick(video)
                                    }
                                    "PLAYLIST" -> {
                                        val plId = item.id.removePrefix("PL_")
                                        val pl = com.example.data.model.PredefinedPlaylists.items.find { it.id == plId }
                                        if (pl != null) onPlaylistClick(pl)
                                    }
                                    "BIBLE" -> {
                                        val parts = item.id.removePrefix("BIBLE_").split("_")
                                        val bId = parts.getOrNull(0)?.toIntOrNull() ?: 1
                                        val ch = parts.getOrNull(1)?.toIntOrNull() ?: 1
                                        onBibleClick(bId, ch)
                                    }
                                    "SONG" -> {
                                        val songId = item.extraDataJson?.toLongOrNull()
                                            ?: item.id.removePrefix("SONG_").toLongOrNull()
                                            ?: 0L
                                        if (songId > 0) {
                                            onSongClick(songId)
                                        }
                                    }
                                }
                            },
                            onRemove = {
                                viewModel.removeSavedItem(item.id)
                            },
                            onShare = {
                                val text = "${item.title}\n${item.url ?: item.subtitle}"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share"))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedItemRow(
    item: SavedItemEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onShare: () -> Unit,
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else if (item.type == "SONG") {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎵",
                        fontSize = 22.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (item.type) {
                        "BLOG" -> NavyPrimary
                        "VIDEO" -> Color(0xFFCC0000)
                        "PLAYLIST" -> GoldWarm
                        "SONG" -> Color(0xFF7C3AED)
                        else -> Color(0xFF2E7D32)
                    }
                ) {
                    Text(
                        text = if (item.type == "SONG") "SONG BOOK" else item.type,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.outline)
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.BookmarkRemove, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
