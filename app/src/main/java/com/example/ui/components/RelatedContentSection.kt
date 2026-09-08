package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.data.model.BlogPost
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary

@Composable
fun RelatedContentSection(
    currentPost: BlogPost,
    allPosts: List<BlogPost>,
    allVideos: List<YouTubeVideo>,
    playlists: List<YouTubePlaylist>,
    onPostClick: (BlogPost) -> Unit,
    onVideoClick: (YouTubeVideo) -> Unit,
    onPlaylistClick: (YouTubePlaylist) -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val postKeywords = remember(currentPost) {
        val titleWords = currentPost.title.lowercase()
            .split(" ", "-", "_", ",", "|", "•", "–")
            .filter { it.length >= 4 && !it.contains("http") }
        val labelWords = currentPost.labels.map { it.lowercase() }
        (titleWords + labelWords).distinct()
    }

    // Match related videos
    val relatedVideos = remember(currentPost, allVideos, postKeywords) {
        allVideos.filter { vid ->
            if (currentPost.embeddedVideoIds.contains(vid.id)) return@filter true
            val vt = vid.title.lowercase()
            val vd = vid.description.lowercase()
            postKeywords.any { kw -> vt.contains(kw) || vd.contains(kw) }
        }.take(6)
    }

    // Match related playlists
    val relatedPlaylists = remember(currentPost, playlists, postKeywords) {
        playlists.filter { pl ->
            val plt = pl.title.lowercase()
            postKeywords.any { kw -> plt.contains(kw) }
        }.take(3)
    }

    // Match related posts (by shared labels)
    val relatedPosts = remember(currentPost, allPosts) {
        allPosts.filter { p ->
            p.id != currentPost.id && p.labels.any { currentPost.labels.contains(it) }
        }.take(5)
    }

    val hasAnyRelated = currentPost.allImages.isNotEmpty() ||
            relatedVideos.isNotEmpty() ||
            relatedPlaylists.isNotEmpty() ||
            relatedPosts.isNotEmpty()

    if (!hasAnyRelated) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        Text(
            text = "RELATED CONTENT",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // 1. Photos
        if (currentPost.allImages.isNotEmpty()) {
            Text(
                text = "📸 Photos (${currentPost.allImages.size})",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(currentPost.allImages) { imgUrl ->
                    Card(
                        modifier = Modifier
                            .size(130.dp)
                            .clickable { onImageClick(imgUrl) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imgUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Event Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // 2. Videos
        if (relatedVideos.isNotEmpty()) {
            Text(
                text = "▶️ Related Videos (${relatedVideos.size})",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(relatedVideos, key = { it.id }) { vid ->
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .clickable { onVideoClick(vid) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(vid.thumbnailUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = vid.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(vid.channelTitle, color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                            Text(
                                text = vid.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Playlists
        if (relatedPlaylists.isNotEmpty()) {
            Text(
                text = "🎵 Related Playlists (${relatedPlaylists.size})",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(relatedPlaylists, key = { it.id }) { pl ->
                    OutlinedCard(
                        modifier = Modifier
                            .width(180.dp)
                            .clickable { onPlaylistClick(pl) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = GoldWarm,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pl.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Related Posts
        if (relatedPosts.isNotEmpty()) {
            Text(
                text = "📝 Related Posts (${relatedPosts.size})",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(relatedPosts, key = { it.id }) { post ->
                    Card(
                        modifier = Modifier
                            .width(190.dp)
                            .clickable { onPostClick(post) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = post.publishedDate,
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                }
            }
        }
    }
}
