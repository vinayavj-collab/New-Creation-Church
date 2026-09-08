package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BlogPost
import com.example.data.model.BlogSourceType
import com.example.data.model.appStrings
import com.example.ui.components.BlogPostCard
import com.example.ui.viewmodel.MainViewModel

enum class BlogTab {
    FELLOWSHIP,
    PERSONAL,
    ALL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogsScreen(
    viewModel: MainViewModel,
    onPostClick: (BlogPost) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = appStrings()
    val settings by viewModel.settings.collectAsState()
    val allPosts by viewModel.allPosts.collectAsState()
    val fellowshipPosts by viewModel.fellowshipPosts.collectAsState()
    val personalVlogPosts by viewModel.personalVlogPosts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var selectedTab by remember { mutableStateOf(BlogTab.FELLOWSHIP) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // If personal vlog is disabled, ensure selected tab is not PERSONAL
    LaunchedEffect(settings.showPersonalVlog) {
        if (!settings.showPersonalVlog && selectedTab == BlogTab.PERSONAL) {
            selectedTab = BlogTab.FELLOWSHIP
        }
    }

    val availableTabs = remember(settings.showPersonalVlog) {
        if (settings.showPersonalVlog) {
            listOf(BlogTab.FELLOWSHIP, BlogTab.PERSONAL, BlogTab.ALL)
        } else {
            listOf(BlogTab.FELLOWSHIP, BlogTab.ALL)
        }
    }

    val basePosts = when (selectedTab) {
        BlogTab.FELLOWSHIP -> fellowshipPosts
        BlogTab.PERSONAL -> personalVlogPosts
        BlogTab.ALL -> allPosts
    }

    // Dynamic categories for the active tab
    val activeCategories = remember(basePosts) {
        basePosts.flatMap { it.labels }.distinct().sorted()
    }

    val displayedPosts = remember(basePosts, selectedCategory) {
        if (selectedCategory == null) {
            basePosts
        } else {
            basePosts.filter { it.labels.contains(selectedCategory) }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshAll() },
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.blogsTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = strings.fellowshipEventsSub,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = strings.globalSearch,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Tabs
                    PrimaryTabRow(
                        selectedTabIndex = availableTabs.indexOf(selectedTab).coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableTabs.forEach { tab ->
                            val tabLabel = when (tab) {
                                BlogTab.FELLOWSHIP -> strings.filterFellowship
                                BlogTab.PERSONAL -> strings.filterVlog
                                BlogTab.ALL -> strings.filterAll
                            }
                            Tab(
                                selected = selectedTab == tab,
                                onClick = {
                                    selectedTab = tab
                                    selectedCategory = null
                                },
                                text = {
                                    Text(
                                        text = tabLabel,
                                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Category Chips if any available
            if (activeCategories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(strings.filterAll) }
                        )
                    }
                    items(activeCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat) null else cat
                            },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Posts List
            if (displayedPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRefreshing) "Loading..." else strings.noArticlesFound,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(displayedPosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            BlogPostCard(
                                post = post,
                                onClick = { onPostClick(post) }
                            )
                        }
                    }
                }
            }
        }
    }
}
