package com.example.ui.screens

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.example.data.model.YouTubeVideo
import com.example.ui.components.UniversalVideoPlayer
import com.example.ui.components.YouTubeVideoCard
import com.example.ui.viewmodel.MainViewModel
import com.example.util.GlobalVideoPlayerState
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
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()
    val isMasterOrAdmin = currentAdmin != null || com.example.util.ProfileManager.isVinayProfile()
    val pinnedVideoId by viewModel.pinnedVideoId.collectAsStateWithLifecycle()
    val isCurrentVideoPinned = (pinnedVideoId == video.id || video.isPinned)

    // Notify tracker that this video is active and playing
    DisposableEffect(video.id) {
        VideoPlaybackTracker.setPlaying(video.id, true)
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Handle Back action: If in landscape, return to portrait first; otherwise exit to mini player
    val handleBackPress = {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            GlobalVideoPlayerState.minimizeToMiniPlayer()
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBackPress()
    }

    // Dynamic swipe-down action for both portrait and landscape
    val onSwipeDownAction = remember(isLandscape) {
        {
            if (isLandscape) {
                // यदि लैंडस्केप में वीडियो चल रहा है तो swipe down से बैक नेविगेशन ट्रिगर हो
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                handleBackPress()
            } else {
                // जब वीडियो देखते समय वीडियो को swipe down करें तो back navigation trigger हो और वीडियो PiP में शिफ्ट हो जाए
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                GlobalVideoPlayerState.minimizeToMiniPlayer()
                onBack()
            }
        }
    }
    val currentOnSwipeDown by androidx.compose.runtime.rememberUpdatedState(onSwipeDownAction)

    // Dynamic, non-definite mixture of related & random videos from all sources
    val relatedVideos = remember(video.id, allVideos) {
        val validVideos = allVideos.filter { candidate ->
            candidate.id != video.id &&
            !candidate.id.startsWith("local_vid") &&
            !candidate.thumbnailUrl.contains("local_vid") &&
            candidate.thumbnailUrl.isNotBlank()
        }
        val pool = if (validVideos.isNotEmpty()) validVideos else allVideos.filter { it.id != video.id }
        if (pool.isEmpty()) return@remember emptyList()

        val sameChannel = pool.filter { it.channelId.isNotBlank() && it.channelId == video.channelId }.shuffled()
        val otherSources = pool.filter { it.channelId.isBlank() || it.channelId != video.channelId }.shuffled()

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
        mixed.distinctBy { it.id }.ifEmpty { pool }.shuffled()
    }

    var showYouTubeSettingsDialog by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    val playTarget = if (video.videoUrl.isNotBlank()) video.videoUrl else video.id
    
    // Check if video is Shorts or vertical video (by URL, title, description or user toggle)
    val isInitiallyVertical = video.videoUrl.contains("/shorts/", ignoreCase = true) ||
            video.title.contains("#shorts", ignoreCase = true) ||
            video.description.contains("#shorts", ignoreCase = true) ||
            video.title.contains("vertical", ignoreCase = true) ||
            video.description.contains("vertical", ignoreCase = true) ||
            video.title.contains("9:16", ignoreCase = true)

    var is916VerticalMode by remember(video.id) { mutableStateOf(isInitiallyVertical) }

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
                autoplay = true,
                onSwipeDown = { currentOnSwipeDown() }
            )
        }
    }

    // Full Screen / Landscape / PiP: Pure uninterrupted video display with swipe down back navigation
    if (isInPictureInPictureMode || isLandscape) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(video.id, isLandscape) {
                    var totalDragY = 0f
                    detectDragGestures(
                        onDragEnd = {
                            if (totalDragY > 50f) {
                                totalDragY = 0f
                                onSwipeDownAction()
                            } else {
                                totalDragY = 0f
                            }
                        },
                        onDragCancel = { totalDragY = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragY += dragAmount.y
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            videoPlayerNode()
        }
    } else {
        // Portrait Mode Layout: Video below status bar with details below
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // FIXED Video Player at the Top below status bar with swipe down / swipe up support
            Box(
                modifier = (if (is916VerticalMode) {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .aspectRatio(9f / 16f)
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                })
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
                    .pointerInput(video.id, isLandscape) {
                        var totalDragY = 0f
                        detectDragGestures(
                            onDragEnd = {
                                if (totalDragY > 50f) {
                                    // Swipe DOWN -> Minimize to In-App Mini Player and trigger back navigation
                                    totalDragY = 0f
                                    onSwipeDownAction()
                                } else if (totalDragY < -50f) {
                                    // Swipe UP -> Enter Landscape Full Screen mode
                                    totalDragY = 0f
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    totalDragY = 0f
                                }
                            },
                            onDragCancel = { totalDragY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragY += dragAmount.y
                            }
                        )
                    }
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isMasterOrAdmin) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.togglePinVideo(video.id) { success, isPinned ->
                                            if (success) {
                                                Toast.makeText(
                                                    context,
                                                    if (isPinned) "वीडियो को सबसे ऊपर PIN कर दिया गया! 📌" else "वीडियो से PIN हटा दिया गया!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    colors = if (isCurrentVideoPinned) {
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = com.example.ui.theme.GoldWarm.copy(alpha = 0.2f),
                                            contentColor = com.example.ui.theme.GoldAccent
                                        )
                                    } else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pin Video",
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isCurrentVideoPinned) com.example.ui.theme.GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isCurrentVideoPinned) "Pinned 📌" else "Pin Video", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = shareVideo,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp)
                            }

                            // 9:16 Aspect Ratio Toggle for long or short vertical videos
                            OutlinedButton(
                                onClick = { is916VerticalMode = !is916VerticalMode },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (is916VerticalMode) Icons.Default.Smartphone else Icons.Default.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (is916VerticalMode) "9:16 Mode" else "16:9 Mode", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { showYouTubeSettingsDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "YouTube Settings", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Player Setting", fontSize = 12.sp)
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
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Icon(
                                            imageVector = if (isDescriptionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = video.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        ),
                                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Header for Recommended Videos
                if (relatedVideos.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "🎬 अनुशंसित एवं अन्य संबंधित वीडियो (Recommended Videos)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    items(relatedVideos, key = { it.id }) { relVideo ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            YouTubeVideoCard(
                                video = relVideo,
                                onClick = { onRelatedVideoClick(relVideo) }
                            )
                        }
                    }
                }
            }
        }
    }

    // YouTube Player Settings Dialog
    if (showYouTubeSettingsDialog) {
        val ytSettings by com.example.util.YouTubeSettingsManager.settings.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { showYouTubeSettingsDialog = false },
            title = {
                Text(
                    text = "⚙️ YouTube प्लेयर सेटिंग्स",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("शीर्षक एवं शेयर बटन छुपाएं", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("वीडियो के ऊपर शीर्षक व शेयर आइकन हटाता है", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = ytSettings.hideTitleAndShare,
                            onCheckedChange = {
                                com.example.util.YouTubeSettingsManager.updateSettings(context, ytSettings.copy(hideTitleAndShare = it))
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("बैक/होम बटन पर PiP चालू रखें", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("ऐप बंद या होम करने पर वीडियो तैरती खिड़की (PiP) में चलती रहेगी", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = ytSettings.enableBackgroundPip,
                            onCheckedChange = {
                                com.example.util.YouTubeSettingsManager.updateSettings(context, ytSettings.copy(enableBackgroundPip = it))
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("अगला संबंधित वीडियो ऑटो-प्ले", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("वीडियो समाप्त होने पर अगला अनुशंसित वीडियो स्वतः चलाएं", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = ytSettings.autoplayNext,
                            onCheckedChange = {
                                com.example.util.YouTubeSettingsManager.updateSettings(context, ytSettings.copy(autoplayNext = it))
                            }
                        )
                    }

                    HorizontalDivider()

                    // Video Quality Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("वीडियो क्वालिटी (Video Quality)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("क्वालिटी चुनें (डिफ़ॉल्ट: ऑटोमैटिक / Automatic)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        val qualityList = listOf(
                            "auto" to "ऑटोमैटिक (Auto)",
                            "hd1080" to "1080p HD",
                            "hd720" to "720p HD",
                            "large" to "480p SD",
                            "medium" to "360p",
                            "small" to "240p",
                            "tiny" to "144p"
                        )

                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(qualityList) { (code, label) ->
                                val isSelected = ytSettings.selectedQuality == code
                                androidx.compose.material3.FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        com.example.util.YouTubeSettingsManager.updateSettings(
                                            context,
                                            ytSettings.copy(selectedQuality = code)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showYouTubeSettingsDialog = false }) {
                    Text("ठीक है (Done)", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
