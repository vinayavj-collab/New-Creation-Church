package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.YouTubeVideo
import com.example.util.GlobalVideoPlayerState
import kotlin.math.roundToInt

/**
 * Floating YouTube/Universal Mini Player Bar located docked right above the Bottom Navigation Bar.
 * - Displays video thumbnail/live player node on left
 * - Video title and channel info
 * - Play / Pause button
 * - Close button
 * - Swipe up to expand to Full Screen
 * - Tap to expand back to Full Screen
 */
@Composable
fun YouTubeMiniPlayer(
    videoPlayerContent: @Composable () -> Unit,
    onExpand: (YouTubeVideo) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentVideo by GlobalVideoPlayerState.currentVideo.collectAsState()
    val isMiniPlayerActive by GlobalVideoPlayerState.isMiniPlayerActive.collectAsState()
    val isPlaying by GlobalVideoPlayerState.isPlaying.collectAsState()

    val video = currentVideo
    val isVisible = isMiniPlayerActive && video != null

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (video != null) {
            var offsetY by remember { mutableFloatStateOf(0f) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .shadow(12.dp, shape = RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(video.id) {
                        detectDragGestures(
                            onDragEnd = {
                                if (offsetY < -40f) {
                                    // Swiped UP -> Expand to Full Screen
                                    offsetY = 0f
                                    onExpand(video)
                                } else if (offsetY > 40f) {
                                    // Swiped DOWN -> Close Mini Player
                                    offsetY = 0f
                                    onClose()
                                } else {
                                    offsetY = 0f
                                }
                            },
                            onDragCancel = {
                                offsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetY = (offsetY + dragAmount.y).coerceIn(-120f, 120f)
                            }
                        )
                    }
                    .clickable { onExpand(video) }
                    .testTag("youtube_mini_player"),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini Video Container
                    Box(
                        modifier = Modifier
                            .width(108.dp)
                            .fillMaxHeight()
                            .background(Color.Black)
                            .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        videoPlayerContent()
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title & Channel
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (video.channelTitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = video.channelTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Controls
                    IconButton(
                        onClick = {
                            GlobalVideoPlayerState.togglePlayPause()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            onClose()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Mini Player",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
