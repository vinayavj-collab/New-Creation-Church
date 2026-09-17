package com.example.ui.screens

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
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
import com.example.util.VideoPlaybackTracker

/**
 * Universal Video Player Screen:
 * - Uses movableContentOf to preserve uninterrupted video playback across Fullscreen,
 *   Landscape, and Picture-in-Picture transitions without restarting the stream.
 * - BackHandler automatically exits landscape mode before navigating back.
 * - Restores Portrait orientation upon disposal so the app never gets stuck in landscape.
 */
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
    val activity = context as? ComponentActivity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val allVideos by viewModel.youtubeVideos.collectAsStateWithLifecycle()

    // Notify tracker that this video is active and playing
    DisposableEffect(video.id) {
        VideoPlaybackTracker.setPlaying(video.id, true)
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Handle Back action: If in landscape, return to portrait first; otherwise exit screen
    val handleBackPress = {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBackPress()
    }

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

    // Single persistent player node across all layout configurations
    val videoPlayerNode = remember(playTarget) {
        movableContentOf {
            UniversalVideoPlayer(
                videoUrlOrId = playTarget,
                modifier = Modifier.fillMaxSize(),
                autoplay = true
            )
        }
    }

    // Full Screen / Landscape / PiP: Pure uninterrupted video display
    if (isInPictureInPictureMode || isLandscape) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            videoPlayerNode()
        }
    } else {
        // Portrait Mode Layout: Video touches status bar at the top with details below
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // FIXED Video Player at the Top touching status bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                videoPlayerNode()
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = shareVideo,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share")
                            }

                            if (onEnterPipClick != null) {
                                OutlinedButton(
                                    onClick = onEnterPipClick,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PictureInPictureAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PiP")
                                }
                            }
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
