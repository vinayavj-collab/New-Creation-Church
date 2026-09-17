package com.example.ui.screens

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.YouTubeVideo
import com.example.ui.components.UniversalVideoPlayer
import com.example.ui.components.YouTubeVideoCard
import com.example.ui.viewmodel.MainViewModel

/**
 * Universal Video Player Screen:
 * - Pinned video player at the top so scrolling never reloads or restarts the video.
 * - Clean, distraction-free UI without redundant tech labels.
 * - True immersive full-screen without banners or text overlays.
 * - Dynamic, non-definite mix of related and random video thumbnails from across all sources.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    video: YouTubeVideo,
    viewModel: MainViewModel,
    isInPictureInPictureMode: Boolean = false,
    onEnterPipClick: (() -> Unit)? = null,
    onBack: () -> Unit,
    onRelatedVideoClick: (YouTubeVideo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val allVideos by viewModel.youtubeVideos.collectAsStateWithLifecycle()

    // Dynamic, non-definite mixture of related & random videos from all sources
    val relatedVideos = remember(video.id, allVideos) {
        val validVideos = allVideos.filter { candidate ->
            candidate.id != video.id &&
            !candidate.id.startsWith("local_vid") &&
            !candidate.id.startsWith("dm_x27lzjr_") &&
            !candidate.id.startsWith("dm_x4sr8o4_") &&
            !candidate.thumbnailUrl.contains("local_vid") &&
            candidate.thumbnailUrl.isNotBlank()
        }
        if (validVideos.isEmpty()) return@remember emptyList()

        val sameChannel = validVideos.filter { it.channelId.isNotBlank() && it.channelId == video.channelId }.shuffled()
        val otherSources = validVideos.filter { it.channelId.isBlank() || it.channelId != video.channelId }.shuffled()

        val mixed = mutableListOf<YouTubeVideo>()
        val maxCount = 20
        val sameIter = sameChannel.iterator()
        val otherIter = otherSources.iterator()

        while ((sameIter.hasNext() || otherIter.hasNext()) && mixed.size < maxCount) {
            if (sameIter.hasNext() && (mixed.size % 2 == 0 || !otherIter.hasNext())) {
                mixed.add(sameIter.next())
            } else if (otherIter.hasNext()) {
                mixed.add(otherIter.next())
            }
        }
        mixed.distinctBy { it.id }.shuffled()
    }

    var isDescriptionExpanded by remember { mutableStateOf(false) }
    val playTarget = if (video.videoUrl.isNotBlank()) video.videoUrl else video.id

    val shareVideo = {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "${video.title}\n\nWatch video: ${video.videoUrl}")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Video")
        context.startActivity(shareIntent)
    }

    // Full Screen / Landscape / PiP: Pure uninterrupted video display without any top banner or text
    if (isInPictureInPictureMode || isLandscape) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            UniversalVideoPlayer(
                videoUrlOrId = playTarget,
                modifier = Modifier.fillMaxSize(),
                autoplay = true
            )
        }
        return
    }

    // Portrait Mode Layout
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = video.title.ifBlank { "Video" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    if (onEnterPipClick != null) {
                        IconButton(onClick = onEnterPipClick) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Picture-in-Picture"
                            )
                        }
                    }
                    IconButton(onClick = shareVideo) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share video"
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // FIXED Video Player at the Top: Never gets destroyed or reset when scrolling below!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                UniversalVideoPlayer(
                    videoUrlOrId = playTarget,
                    modifier = Modifier.fillMaxSize(),
                    autoplay = true
                )
            }

            // Scrollable Content (Details + Dynamic Recommended Videos)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Video Meta Details
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (video.channelTitle.isNotBlank()) {
                                Text(
                                    text = video.channelTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            if (video.publishedAt.isNotBlank()) {
                                Text(
                                    text = video.publishedAt,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = shareVideo,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Video")
                        }

                        // Expandable Description
                        if (video.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "विवरण (Description)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Icon(
                                            imageVector = if (isDescriptionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = video.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (relatedVideos.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                            Text(
                                text = "सुझाए गए वीडियो (Suggested Videos)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }

                // Dynamic Recommended & Random Video List
                if (relatedVideos.isNotEmpty()) {
                    items(relatedVideos, key = { it.id }) { itemVideo ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            YouTubeVideoCard(
                                video = itemVideo,
                                onClick = { onRelatedVideoClick(itemVideo) }
                            )
                        }
                    }
                }
            }
        }
    }
}
