package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BlogPost
import com.example.data.model.SearchResultItem
import com.example.data.model.SearchResultType
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onPostClick: (BlogPost) -> Unit,
    onVideoClick: (YouTubeVideo) -> Unit,
    onPlaylistClick: (YouTubePlaylist) -> Unit,
    onBibleClick: (Int, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.selectedSearchFilter.collectAsState()
    val searchResults by viewModel.globalSearchResults.collectAsState()
    val fellowshipCategories by viewModel.fellowshipCategories.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search blogs, videos, playlists, bible...") },
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            // Scope notice
            item {
                Text(
                    text = if (settings.showPersonalVlog) {
                        "Searching: Fellowship Events, Personal Vlog, YouTube Channels, Playlists, Bible"
                    } else {
                        "Searching: Fellowship Events, YouTube Channels, Playlists, Bible"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Filter Tabs: ALL, BLOG, VIDEO, PLAYLIST, BIBLE
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All",
                        "BLOG" to "Blogs",
                        "VIDEO" to "Videos",
                        "PLAYLIST" to "Playlists",
                        "BIBLE" to "Bible"
                    )
                    items(filters) { (fKey, fLabel) ->
                        FilterChip(
                            selected = filter == fKey,
                            onClick = { viewModel.setSelectedSearchFilter(fKey) },
                            label = { Text(fLabel) }
                        )
                    }
                }
            }

            // Suggested labels when query is blank
            if (query.isBlank() && fellowshipCategories.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "POPULAR LABELS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(fellowshipCategories.take(10)) { label ->
                                SuggestionChip(
                                    onClick = { viewModel.setSearchQuery(label) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // Header for results
            if (query.isNotBlank()) {
                item {
                    Text(
                        text = "SEARCH RESULTS (${searchResults.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            if (query.isNotBlank() && searchResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🔍 No results found for \"$query\"",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try different keywords or check other filters.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            } else {
                items(searchResults, key = { it.id }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        SearchResultRow(
                            item = item,
                            onClick = {
                                when (item.type) {
                                    SearchResultType.BLOG -> item.post?.let { onPostClick(it) }
                                    SearchResultType.VIDEO -> item.video?.let { onVideoClick(it) }
                                    SearchResultType.PLAYLIST -> item.playlist?.let { onPlaylistClick(it) }
                                    SearchResultType.BIBLE -> {
                                        val bId = item.bibleBookId?.toIntOrNull() ?: 1
                                        val ch = item.bibleChapter ?: 1
                                        onBibleClick(bId, ch)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(
    item: SearchResultItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or icon
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (item.type) {
                                SearchResultType.BLOG -> NavyPrimary.copy(alpha = 0.15f)
                                SearchResultType.VIDEO -> Color(0xFFFFEAEA)
                                SearchResultType.PLAYLIST -> GoldWarm.copy(alpha = 0.2f)
                                SearchResultType.BIBLE -> Color(0xFFE8F5E9)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.type) {
                            SearchResultType.BLOG -> Icons.Default.Description
                            SearchResultType.VIDEO -> Icons.Default.PlayArrow
                            SearchResultType.PLAYLIST -> Icons.Default.VideoLibrary
                            SearchResultType.BIBLE -> Icons.Default.Book
                        },
                        contentDescription = null,
                        tint = when (item.type) {
                            SearchResultType.BLOG -> NavyPrimary
                            SearchResultType.VIDEO -> Color(0xFFD32F2F)
                            SearchResultType.PLAYLIST -> GoldWarm
                            SearchResultType.BIBLE -> Color(0xFF2E7D32)
                        }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                // Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (item.type) {
                        SearchResultType.BLOG -> NavyPrimary
                        SearchResultType.VIDEO -> Color(0xFFCC0000)
                        SearchResultType.PLAYLIST -> GoldWarm
                        SearchResultType.BIBLE -> Color(0xFF2E7D32)
                    }
                ) {
                    Text(
                        text = item.type.label,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.snippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.snippet,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
