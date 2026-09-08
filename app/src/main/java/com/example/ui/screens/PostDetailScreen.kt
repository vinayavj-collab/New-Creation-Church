package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.*
import com.example.ui.components.RelatedContentSection
import com.example.ui.components.SourceBadge
import com.example.ui.components.YouTubePlayerView
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.ReminderScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    post: BlogPost,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onPostClick: (BlogPost) -> Unit,
    onPlaylistClick: (YouTubePlaylist) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allPosts by viewModel.allPosts.collectAsState()
    val allVideos by viewModel.youtubeVideos.collectAsState()
    val isSaved by viewModel.isSaved("POST_${post.id}").collectAsState(initial = false)

    val upcomingEvent = remember(post) { EventExtractor.extractUpcomingEvent(post) }
    var showReminderDialog by remember { mutableStateOf(false) }

    // Record view in Recently Viewed
    LaunchedEffect(post.id) {
        viewModel.recordRecentlyViewed(
            id = "POST_${post.id}",
            type = "BLOG",
            title = post.title,
            subtitle = post.publishedDate,
            imageUrl = post.featuredImageUrl
        )
    }

    val sharePost = {
        val text = if (upcomingEvent != null) {
            EventExtractor.generateEventShareText(upcomingEvent)
        } else {
            "${post.title}\n\n${post.plainTextExcerpt.take(200)}...\n\nRead more at: ${post.url}"
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Event")
        context.startActivity(shareIntent)
    }

    val openInBrowser = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }

    val openCalendar = {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, post.title)
                putExtra(CalendarContract.Events.DESCRIPTION, "Vinay Kumar AVJ Fellowship Event\n\n${post.url}")
                if (upcomingEvent?.locationString != null) {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, upcomingEvent.locationString)
                }
                if (upcomingEvent != null && upcomingEvent.startTimestamp > 0) {
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, upcomingEvent.startTimestamp)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, upcomingEvent.startTimestamp + (2 * 60 * 60 * 1000L))
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open calendar app", Toast.LENGTH_SHORT).show()
        }
    }

    val openLocation = {
        val loc = upcomingEvent?.locationString ?: post.title
        try {
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(loc))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open map", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = post.source.displayName,
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
                    // Bookmark / Save toggle
                    IconButton(
                        onClick = {
                            viewModel.toggleSaveItem(
                                id = "POST_${post.id}",
                                type = "BLOG",
                                title = post.title,
                                subtitle = post.publishedDate,
                                imageUrl = post.featuredImageUrl,
                                url = post.url,
                                isCurrentlySaved = isSaved
                            )
                            Toast.makeText(
                                context,
                                if (isSaved) "Removed from Saved" else "Saved for later",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isSaved) "Remove bookmark" else "Save for later",
                            tint = if (isSaved) GoldWarm else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = sharePost) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share post"
                        )
                    }
                    IconButton(onClick = openInBrowser) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open in browser"
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
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // Featured Header Image
            if (!post.featuredImageUrl.isNullOrBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(post.featuredImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = post.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Title and Metadata
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SourceBadge(source = post.source)
                        Text(
                            text = post.publishedDate,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 32.sp
                        )
                    )

                    // Labels
                    if (post.labels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(post.labels) { label ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Upcoming Event banner if detected
            if (upcomingEvent != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = NavyPrimary.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "📅 EVENT SCHEDULE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Date: ${upcomingEvent.dateString}" + (upcomingEvent.timeString?.let { " • $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (!upcomingEvent.locationString.isNullOrBlank()) {
                                Text(
                                    text = "Location: ${upcomingEvent.locationString}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = openCalendar,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Add to Calendar", fontSize = 11.sp, maxLines = 1)
                                }

                                if (!upcomingEvent.locationString.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = openLocation,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Open Location", fontSize = 11.sp, maxLines = 1)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { showReminderDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remind Me", fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // Embedded YouTube Videos
            if (post.embeddedVideoIds.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "EMBEDDED VIDEOS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        post.embeddedVideoIds.forEach { vidId ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                YouTubePlayerView(videoId = vidId)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            // Formatted Article Content (Supports Hindi, English, all scripts via HtmlCompat)
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            TextView(ctx).apply {
                                textSize = 16f
                                setLineSpacing(8f, 1.2f)
                                setTextIsSelectable(true)
                                setTextColor(android.graphics.Color.parseColor("#334155"))
                            }
                        },
                        update = { textView ->
                            val textColor = if (textView.context.resources.configuration.uiMode and
                                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                                android.content.res.Configuration.UI_MODE_NIGHT_YES
                            ) {
                                android.graphics.Color.parseColor("#E2E8F0")
                            } else {
                                android.graphics.Color.parseColor("#1E293B")
                            }
                            textView.setTextColor(textColor)
                            textView.text = HtmlCompat.fromHtml(
                                post.contentHtml.ifEmpty { post.plainTextExcerpt },
                                HtmlCompat.FROM_HTML_MODE_COMPACT
                            )
                        }
                    )
                }
            }

            // Related Content (Photos, Videos, Playlists, Related Posts)
            item {
                RelatedContentSection(
                    currentPost = post,
                    allPosts = allPosts,
                    allVideos = allVideos,
                    playlists = PredefinedPlaylists.items,
                    onPostClick = onPostClick,
                    onVideoClick = { vid -> onVideoClick(vid.id) },
                    onPlaylistClick = onPlaylistClick,
                    onImageClick = onImageClick
                )
            }
        }
    }

    if (showReminderDialog && upcomingEvent != null) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Set Event Reminder") },
            text = {
                Column {
                    Text(
                        text = "Choose when you would like to be reminded for \"${upcomingEvent.title}\":",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ReminderScheduler.ReminderOffset.entries.forEach { offset ->
                        OutlinedButton(
                            onClick = {
                                val success = viewModel.scheduleEventReminder(context, upcomingEvent, offset)
                                showReminderDialog = false
                                if (success) {
                                    Toast.makeText(context, "Reminder set for ${offset.label}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Event is too soon or already passed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(offset.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
