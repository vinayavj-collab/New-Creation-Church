package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.LocalAppProfile
import com.example.data.model.BlogPost
import com.example.data.model.BlogSourceType
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.util.BloggerImageUtils

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    onSearchClick: (() -> Unit)? = null,
    isUpdateAvailable: Boolean = false,
    onUpdateClick: (() -> Unit)? = null,
    onOpenDrawer: (() -> Unit)? = null,
    isDrawerEnabled: Boolean = true,
    drawerPosition: String = "left",
    unreadNotificationCount: Int = 0,
    onNotificationClick: (() -> Unit)? = null
) {
    val activeProfile = LocalAppProfile.current
    // Header Bar with transparent background, maximizing available horizontal and vertical space
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        val hasLeftDrawer = isDrawerEnabled && onOpenDrawer != null && drawerPosition != "right"
        val hasRightDrawer = isDrawerEnabled && onOpenDrawer != null && drawerPosition == "right"

        // Central Logo: Maximized width to fill available center space with 48dp margins on both sides for Drawer and Bell buttons
        Image(
            painter = painterResource(id = activeProfile.headerLogoRes),
            contentDescription = activeProfile.displayNameEnglish,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(
                    start = 48.dp,
                    end = 48.dp,
                    top = 2.dp,
                    bottom = 2.dp
                )
        )

        // Navigation Drawer (Hamburger Menu): Standard 48dp touch target
        if (isDrawerEnabled && onOpenDrawer != null) {
            val drawerAlignModifier = if (drawerPosition == "right") {
                Modifier.align(Alignment.CenterEnd)
            } else {
                Modifier.align(Alignment.CenterStart)
            }
            IconButton(
                onClick = onOpenDrawer,
                modifier = drawerAlignModifier
                    .size(48.dp)
                    .testTag("drawer_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Sidebar",
                    tint = GoldWarm
                )
            }
        }

        // Notification Bell Icon: Dynamically mirrors Navigation Drawer's position
        // If Drawer is Left, Bell MUST be on Right; If Drawer is Right, Bell MUST be on Left.
        val bellAlignModifier = if (drawerPosition == "right") {
            Modifier.align(Alignment.CenterStart)
        } else {
            Modifier.align(Alignment.CenterEnd)
        }

        IconButton(
            onClick = { onNotificationClick?.invoke() },
            modifier = bellAlignModifier
                .size(48.dp)
                .testTag("notification_bell_button")
        ) {
            BadgedBox(
                badge = {
                    if (unreadNotificationCount > 0) {
                        Badge(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (unreadNotificationCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = GoldWarm
                )
            }
        }
    }
}

@Composable
fun AdminNoticeBanner(
    notice: com.example.data.model.AdminNotice?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onActionClick: ((String) -> Unit)? = null
) {
    if (notice == null || !notice.isActive || notice.message.isBlank()) {
        return
    }

    AndroidView(
        factory = { ctx ->
            val view = LayoutInflater.from(ctx).inflate(R.layout.view_marquee_notice_banner, null, false)
            view
        },
        update = { view ->
            val noticeContainer = view.findViewById<View>(R.id.noticeBannerContainer)
            val noticeBadge = view.findViewById<TextView>(R.id.noticeBadge)
            val noticeTextView = view.findViewById<TextView>(R.id.noticeTextView)
            val dismissButton = view.findViewById<ImageView>(R.id.noticeDismissButton)

            val remoteNoticeHeading = com.example.util.RemoteConfigManager.getAppNoticeHeading()
            if (noticeBadge != null && remoteNoticeHeading.isNotBlank()) {
                noticeBadge.text = "📢 $remoteNoticeHeading"
            }

            if (notice != null && notice.isActive && notice.message.isNotBlank()) {
                noticeContainer.visibility = View.VISIBLE
                val titleToDisplay = if (notice.title.isNotBlank()) notice.title else remoteNoticeHeading
                val formattedText = "$titleToDisplay : ${notice.message}               "
                noticeTextView.text = formattedText
                // Activation Logic: In the Kotlin/Java code, explicitly call noticeTextView.isSelected = true
                // as this is required for the Android marquee effect to start animating.
                noticeTextView.isSelected = true

                noticeTextView.setOnClickListener {
                    if (!notice.actionUrl.isNullOrBlank()) {
                        onActionClick?.invoke(notice.actionUrl)
                    }
                }
            } else {
                noticeContainer.visibility = View.GONE
                noticeTextView.isSelected = false
            }

            dismissButton.visibility = if (notice?.isDismissible == true) View.VISIBLE else View.GONE
            dismissButton.setOnClickListener {
                onDismiss()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_notice_ticker")
    )
}

@Composable
fun OfflineNoticeBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Offline indicator",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "You're offline. Showing saved content.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            TextButton(
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SourceBadge(
    source: BlogSourceType,
    modifier: Modifier = Modifier
) {
    val isFellowship = source == BlogSourceType.FELLOWSHIP_EVENTS
    val bgColor = if (isFellowship) NavyPrimary else Color(0xFF5D4037)
    val textColor = if (isFellowship) GoldWarm else Color(0xFFFFD180)

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = source.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                fontSize = 10.5.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun FeaturedPostCard(
    post: BlogPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dataSaverEnabled: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("featured_post_card")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                if (!post.featuredImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(BloggerImageUtils.getOptimizedUrl(post.featuredImageUrl, dataSaverEnabled))
                            .crossfade(true)
                            .build(),
                        contentDescription = post.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Fellowship Events",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Gradient scrim on bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                startY = 100f
                            )
                        )
                )

                // Top source badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SourceBadge(source = post.source)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "FEATURED",
                            color = GoldWarm,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Date label at bottom left
                Text(
                    text = post.publishedDate,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                )

                // Photo Count Badge at bottom right (if post contains photos)
                if (post.allImages.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${post.allImages.size} फ़ोटो",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (post.labels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        post.labels.take(3).forEach { label ->
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }

                if (post.plainTextExcerpt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = post.plainTextExcerpt,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Read More")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlogPostCard(
    post: BlogPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dataSaverEnabled: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!post.featuredImageUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(BloggerImageUtils.getOptimizedUrl(post.featuredImageUrl, dataSaverEnabled))
                            .crossfade(true)
                            .build(),
                        contentDescription = post.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    SourceBadge(
                        source = post.source,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )

                    if (post.allImages.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${post.allImages.size} फ़ोटो",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                if (post.featuredImageUrl.isNullOrEmpty()) {
                    SourceBadge(
                        source = post.source,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = post.publishedDate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    if (post.labels.isNotEmpty()) {
                        Text(
                            text = post.labels.first(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.outline
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (post.plainTextExcerpt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = post.plainTextExcerpt,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Read More", fontWeight = FontWeight.SemiBold)
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
    }
}

@Composable
fun YouTubeVideoCard(
    video: YouTubeVideo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                val effectiveThumbnail = remember(video) {
                    val thumb = video.thumbnailUrl.trim()
                    if (thumb.isNotBlank() && !thumb.contains("local_vid") && !thumb.contains("/default/")) {
                        thumb
                    } else if (video.id.isNotBlank() && !video.id.startsWith("local_vid") && !video.id.startsWith("dm_")) {
                        "https://i.ytimg.com/vi/${video.id}/hqdefault.jpg"
                    } else {
                        thumb
                    }
                }
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(effectiveThumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon pill
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play video",
                        tint = GoldWarm,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = video.publishedAt,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Platform tag badge (YouTube vs Dailymotion)
                val platformInfo = com.example.util.VideoUrlParser.parse(if (video.videoUrl.isNotBlank()) video.videoUrl else video.id)
                Surface(
                    color = if (platformInfo.platform == com.example.util.VideoPlatform.DAILYMOTION) Color(0xFF0066DC) else Color(0xFFD32F2F),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (platformInfo.platform == com.example.util.VideoPlatform.DAILYMOTION) "DAILYMOTION" else "YOUTUBE",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.channelTitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: YouTubePlaylist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(260.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(NavyPrimary)
            ) {
                if (!playlist.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(playlist.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Playlist overlay badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
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
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if ((playlist.videoCountEstimate ?: 0) > 0) "${playlist.videoCountEstimate} Videos" else "PLAYLIST",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = playlist.title.ifBlank { "Playlist" },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = playlist.channelTitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open playlist",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RandomVideoCard(
    video: YouTubeVideo?,
    onPlayClick: (YouTubeVideo) -> Unit,
    onShuffleClick: () -> Unit,
    onChannelSelect: (String) -> Unit,
    selectedChannelFilter: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "यादृच्छिक वीडियो सुझाव (Suggested Random Video)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onShuffleClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Shuffle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Channel Selector Chips for Suggestion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "सभी चैनल",
                    "WORSHIP" to "AVJ Worship",
                    "MAIN" to "Vinay Kumar AVJ",
                    "CHURCH" to "NCC Church"
                ).forEach { (key, label) ->
                    val isSelected = selectedChannelFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { onChannelSelect(key) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (video != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onPlayClick(video) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 65.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyPrimary)
                    ) {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = video.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = video.channelTitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            } else {
                Text(
                    text = "वीडियो खोजने के लिए रिफ्रेश करें...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
