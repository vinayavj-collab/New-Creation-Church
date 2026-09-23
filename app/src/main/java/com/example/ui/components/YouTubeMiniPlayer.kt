package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.YouTubeVideo
import com.example.util.GlobalVideoPlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Floating In-App Picture-in-Picture (PiP) Video Player.
 * - Floats dynamically over any screen in the app
 * - Smooth 2D drag and pinch-to-zoom/resize anywhere
 * - Double-tap to enter fullscreen
 * - Single-tap to show Play/Pause controls overlay for 2 seconds
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
        enter = scaleIn(initialScale = 0.8f) + fadeIn(),
        exit = scaleOut(targetScale = 0.8f) + fadeOut(),
        modifier = modifier
    ) {
        if (video != null) {
            val coroutineScope = rememberCoroutineScope()
            var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
            var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
            var pipWidthDp by rememberSaveable { mutableFloatStateOf(230f) }

            val minWidth = 150f
            val maxWidth = 350f
            val pipHeightDp = pipWidthDp * (9f / 16f)

            var showControls by remember { mutableStateOf(false) }
            var hideControlsJob by remember { mutableStateOf<Job?>(null) }

            DisposableEffect(Unit) {
                onDispose {
                    GlobalVideoPlayerState.updatePipBounds(null)
                }
            }

            Surface(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .width(pipWidthDp.dp)
                    .height(pipHeightDp.dp)
                    .shadow(16.dp, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .onGloballyPositioned { coordinates ->
                        val pos = coordinates.positionInWindow()
                        val size = coordinates.size
                        val rect = android.graphics.Rect(
                            pos.x.toInt(),
                            pos.y.toInt(),
                            (pos.x + size.width).toInt(),
                            (pos.y + size.height).toInt()
                        )
                        GlobalVideoPlayerState.updatePipBounds(rect)
                    }
                    .pointerInput(video.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            offsetX += pan.x
                            offsetY += pan.y
                            pipWidthDp = (pipWidthDp * zoom).coerceIn(minWidth, maxWidth)
                        }
                    }
                    .testTag("youtube_in_app_pip_player"),
                color = Color.Black,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. Live Video View Container (Scales proportionally to user pinch zoom while preserving video engine)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMiniPlayerActive) {
                            Box(
                                modifier = Modifier
                                    .requiredSize(width = 360.dp, height = 202.5.dp)
                                    .graphicsLayer {
                                        val scale = pipWidthDp / 360f
                                        scaleX = scale
                                        scaleY = scale
                                        transformOrigin = TransformOrigin.Center
                                    }
                            ) {
                                videoPlayerContent()
                            }
                        }
                    }

                    // 2. Gesture Detector Layer: Single Tap only (shows controls for 2 seconds)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(video.id) {
                                detectTapGestures(
                                    onTap = {
                                        if (showControls) {
                                            showControls = false
                                            hideControlsJob?.cancel()
                                        } else {
                                            showControls = true
                                            hideControlsJob?.cancel()
                                            hideControlsJob = coroutineScope.launch {
                                                delay(2000L)
                                                showControls = false
                                            }
                                        }
                                    }
                                )
                            }
                    )

                    // 3. Controls Overlay: Play/Pause in center, Fullscreen on top-left, Close on top-right (All auto-hide in 2s)
                    AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Center Play/Pause button
                            IconButton(
                                onClick = {
                                    GlobalVideoPlayerState.togglePlayPause()
                                    // Reset 2-second timer so controls stay visible briefly after toggling
                                    hideControlsJob?.cancel()
                                    hideControlsJob = coroutineScope.launch {
                                        delay(2000L)
                                        showControls = false
                                    }
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Fullscreen icon toggle on top-left
                            IconButton(
                                onClick = {
                                    hideControlsJob?.cancel()
                                    showControls = false
                                    onExpand(video)
                                },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Expand Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Close button toggle on top-right (hides with controls after 2 seconds)
                            IconButton(
                                onClick = {
                                    hideControlsJob?.cancel()
                                    showControls = false
                                    onClose()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Mini Player",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
