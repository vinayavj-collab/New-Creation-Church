package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.ui.components.YouTubeVideoCard
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: YouTubePlaylist,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onVideoClick: (YouTubeVideo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playlistVideos by viewModel.currentPlaylistVideos.collectAsState()
    val isLoading by viewModel.isLoadingPlaylist.collectAsState()

    LaunchedEffect(playlist.id) {
        viewModel.loadPlaylistVideos(playlist)
    }

    val openPlaylistInYouTube = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playlist.playlistUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = openPlaylistInYouTube) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open in YouTube"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                val effectiveThumbnail = remember(playlist.thumbnailUrl, playlistVideos) {
                    val direct = playlist.thumbnailUrl?.trim()
                    if (!direct.isNullOrBlank() && !direct.contains("/default/")) {
                        direct
                    } else {
                        playlistVideos.firstOrNull()?.thumbnailUrl
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Prominent Playlist Thumbnail Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(NavyPrimary)
                        ) {
                            if (!effectiveThumbnail.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(effectiveThumbnail)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = playlist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    NavyPrimary,
                                                    MaterialTheme.colorScheme.primaryContainer
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }

                            // Dark Gradient Scrim overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.3f),
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )

                            // Playlist video count badge
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.75f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (playlistVideos.isNotEmpty()) {
                                            "${playlistVideos.size} Videos"
                                        } else if ((playlist.videoCountEstimate ?: 0) > 0) {
                                            "${playlist.videoCountEstimate} Videos"
                                        } else {
                                            "PLAYLIST"
                                        },
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Title & Channel info over thumbnail
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = playlist.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = playlist.channelTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = GoldAccent
                                    )
                                )
                            }
                        }

                        // Action Buttons Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (playlistVideos.isNotEmpty()) {
                                Button(
                                    onClick = { onVideoClick(playlistVideos.first()) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play First")
                                }
                            }

                            OutlinedButton(
                                onClick = openPlaylistInYouTube,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open YouTube")
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (playlistVideos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Playlist content loaded via YouTube.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = openPlaylistInYouTube) {
                                Text("View on YouTube")
                            }
                        }
                    }
                }
            } else {
                items(playlistVideos, key = { it.id }) { video ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        YouTubeVideoCard(
                            video = video,
                            onClick = { onVideoClick(video) }
                        )
                    }
                }
            }
        }
    }
}
