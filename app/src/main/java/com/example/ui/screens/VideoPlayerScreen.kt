package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.YouTubeVideo
import com.example.ui.components.UniversalVideoPlayer
import com.example.ui.components.YouTubeVideoCard
import com.example.ui.viewmodel.MainViewModel
import com.example.util.VideoPlatform
import com.example.util.VideoUrlParser

/**
 * Universal Video Player Screen: Seamlessly plays YouTube and Dailymotion videos in-app
 * with native Picture-in-Picture (PiP) support and seamless dynamic UI hiding.
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
    val allVideos by viewModel.youtubeVideos.collectAsStateWithLifecycle()
    val relatedVideos = remember(video, allVideos) {
        val validVideos = allVideos.filter { candidate ->
            candidate.id != video.id &&
            !candidate.id.startsWith("local_vid") &&
            !candidate.id.startsWith("dm_x27lzjr_") &&
            !candidate.id.startsWith("dm_x4sr8o4_") &&
            !candidate.thumbnailUrl.contains("local_vid") &&
            candidate.thumbnailUrl.isNotBlank()
        }
        val sameChannel = validVideos.filter { it.channelId.isNotBlank() && it.channelId == video.channelId }
        val otherChannels = validVideos.filter { it.channelId.isBlank() || it.channelId != video.channelId }
        (sameChannel + otherChannels).distinctBy { it.id }.take(8)
    }

    val parsedVideo = remember(video) {
        val target = if (video.videoUrl.isNotBlank()) video.videoUrl else video.id
        VideoUrlParser.parse(target)
    }

    val platformLabel = when (parsedVideo.platform) {
        VideoPlatform.DAILYMOTION -> "Dailymotion"
        VideoPlatform.YOUTUBE -> "YouTube"
        VideoPlatform.UNKNOWN -> "Video"
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

    // When in Picture-in-Picture mode, hide all chrome/toolbars/buttons/scrollbars and display purely the video stream
    if (isInPictureInPictureMode) {
        Box(
            modifier = modifier
                .fillMaxSize(),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Universal Player",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Playing via $platformLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Player Area - Embedded Universal Player (YouTube IFrame API & Dailymotion WebView)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    UniversalVideoPlayer(
                        videoUrlOrId = playTarget,
                        modifier = Modifier.fillMaxSize(),
                        autoplay = true
                    )
                }
            }

            // Video Meta Details
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text(platformLabel) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (parsedVideo.platform == VideoPlatform.DAILYMOTION) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                }
                            )
                        )

                        if (video.publishedAt.isNotBlank()) {
                            Text(
                                text = video.publishedAt,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (video.channelTitle.isNotBlank()) {
                        Text(
                            text = video.channelTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = shareVideo,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Video")
                    }

                    // Description Box
                    if (video.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
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
                                        text = "Description",
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
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        Text(
                            text = "MORE VIDEOS & PRAISES",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }

            // Related videos
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
