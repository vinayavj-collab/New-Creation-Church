package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlogPost
import com.example.data.model.BlogSectionType
import com.example.data.model.DailyDevotion
import com.example.data.model.appStrings
import com.example.ui.components.BlogPostCard
import com.example.ui.components.DailyAudioDevotionalCard
import com.example.ui.viewmodel.MainViewModel

enum class BlogTab {
    FELLOWSHIP,
    AUDIO_MESSAGES,
    PERSONAL,
    ALL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogsScreen(
    viewModel: MainViewModel,
    onPostClick: (BlogPost) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: BlogTab? = null
) {
    val strings = appStrings()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val fellowshipPosts by viewModel.fellowshipPosts.collectAsStateWithLifecycle()
    val personalVlogPosts by viewModel.personalVlogPosts.collectAsStateWithLifecycle()
    val dailyDevotions by viewModel.dailyDevotions.collectAsStateWithLifecycle()
    val dailyAudioDevotional by viewModel.dailyAudioDevotional.collectAsStateWithLifecycle()
    val audioMessageConfig by viewModel.audioMessageConfig.collectAsStateWithLifecycle()
    val isPersonalVlogAllowed by viewModel.isPersonalVlogAllowed.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Determine available tabs based on Master Admin configuration & permissions
    val availableSections = remember(settings.blogSectionsOrder, settings.enabledBlogSections, isPersonalVlogAllowed) {
        val list = settings.blogSectionsOrder.filter { sec ->
            settings.enabledBlogSections.contains(sec) &&
                    (sec != BlogSectionType.PERSONAL || isPersonalVlogAllowed)
        }
        if (list.isEmpty()) {
            listOf(BlogSectionType.FELLOWSHIP, BlogSectionType.ALL)
        } else {
            list
        }
    }

    // Compute effective initial section
    val effectiveInitialSection = remember(initialTab, settings.defaultBlogSection, availableSections) {
        if (initialTab != null) {
            when (initialTab) {
                BlogTab.FELLOWSHIP -> BlogSectionType.FELLOWSHIP
                BlogTab.AUDIO_MESSAGES -> BlogSectionType.AUDIO_MESSAGES
                BlogTab.PERSONAL -> BlogSectionType.PERSONAL
                BlogTab.ALL -> BlogSectionType.ALL
            }
        } else if (availableSections.contains(settings.defaultBlogSection)) {
            settings.defaultBlogSection
        } else {
            availableSections.firstOrNull() ?: BlogSectionType.FELLOWSHIP
        }
    }

    var selectedSection by remember(effectiveInitialSection) {
        mutableStateOf(effectiveInitialSection)
    }

    // Ensure selected section is in available list
    LaunchedEffect(availableSections) {
        if (!availableSections.contains(selectedSection)) {
            selectedSection = availableSections.firstOrNull() ?: BlogSectionType.FELLOWSHIP
        }
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val basePosts = when (selectedSection) {
        BlogSectionType.FELLOWSHIP -> fellowshipPosts
        BlogSectionType.PERSONAL -> if (isPersonalVlogAllowed) personalVlogPosts else emptyList()
        BlogSectionType.ALL -> allPosts
        BlogSectionType.AUDIO_MESSAGES -> emptyList()
    }

    // Dynamic categories for the active tab (when in blog posts mode)
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
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.blogsTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        val isSearchEnabled by viewModel.isSearchEnabled.collectAsStateWithLifecycle()
                        if (isSearchEnabled) {
                            IconButton(onClick = onSearchClick) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = strings.globalSearch,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Dynamic Tabs (Ordered and configured from Master Admin Panel)
                    if (availableSections.size > 1) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = availableSections.indexOf(selectedSection).coerceAtLeast(0),
                            modifier = Modifier.fillMaxWidth(),
                            edgePadding = 16.dp
                        ) {
                            availableSections.forEach { section ->
                                val count = when (section) {
                                    BlogSectionType.FELLOWSHIP -> fellowshipPosts.size
                                    BlogSectionType.AUDIO_MESSAGES -> dailyDevotions.size
                                    BlogSectionType.PERSONAL -> personalVlogPosts.size
                                    BlogSectionType.ALL -> allPosts.size
                                }
                                val tabLabel = section.titleHindi
                                val displayText = if (count > 0) "$tabLabel ($count)" else tabLabel
                                val isSelected = selectedSection == section

                                Tab(
                                    selected = isSelected,
                                    onClick = {
                                        selectedSection = section
                                        selectedCategory = null
                                    },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val icon = when (section) {
                                                BlogSectionType.FELLOWSHIP -> Icons.Default.MenuBook
                                                BlogSectionType.AUDIO_MESSAGES -> Icons.Default.Headphones
                                                BlogSectionType.PERSONAL -> Icons.Default.Person
                                                BlogSectionType.ALL -> Icons.Default.AllInbox
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = displayText,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Main Tab Content: Audio Messages vs Blog Posts
            if (selectedSection == BlogSectionType.AUDIO_MESSAGES) {
                // Audio Messages Tab Stream
                AudioMessagesTabStream(
                    dailyAudioDevotional = dailyAudioDevotional,
                    dailyDevotions = dailyDevotions,
                    audioMessageConfig = audioMessageConfig,
                    isRefreshing = isRefreshing,
                    onRecordListen = { id -> viewModel.incrementDevotionListens(id) }
                )
            } else {
                // Category Chips if any available
                if (activeCategories.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
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
                        contentPadding = PaddingValues(top = 2.dp, bottom = 80.dp)
                    ) {
                        items(displayedPosts, key = { it.id }) { post ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                BlogPostCard(
                                    post = post,
                                    onClick = { onPostClick(post) },
                                    dataSaverEnabled = settings.dataSaverEnabled
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioMessagesTabStream(
    dailyAudioDevotional: com.example.data.model.DailyAudioDevotional?,
    dailyDevotions: List<DailyDevotion>,
    audioMessageConfig: com.example.data.model.AudioMessageConfig,
    isRefreshing: Boolean,
    onRecordListen: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Today's Audio Message Card (with player)
        item {
            DailyAudioDevotionalCard(
                devotional = dailyAudioDevotional,
                dailyDevotions = dailyDevotions,
                config = audioMessageConfig,
                onListenStarted = onRecordListen
            )
        }

        // Section Title: Past / Scheduled Audio Devotions
        if (dailyDevotions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "सभी आत्मिक संदेश व ऑडियो वचन (${dailyDevotions.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(dailyDevotions, key = { it.devotionId }) { devotion ->
                AudioDevotionListItem(
                    devotion = devotion,
                    onPlay = { onRecordListen(devotion.devotionId) }
                )
            }
        } else if (!audioMessageConfig.isServiceActive) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                            Text(
                                text = "ऑडियो संदेश सर्विस सक्रिय नहीं है।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioDevotionListItem(
    devotion: DailyDevotion,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (devotion.isPinned) Icons.Default.PushPin else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = devotion.title.ifBlank { "दैनिक आत्मिक संदेश" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (devotion.scheduledDate.isNotBlank()) {
                        Text(
                            text = "📅 ${devotion.scheduledDate}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (devotion.speakerName.isNotBlank()) {
                        Text(
                            text = "• 🎙️ ${devotion.speakerName}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (devotion.audioUrl.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "ऑडियो उपलब्ध",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
