package com.example.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.BibleBook
import com.example.data.bible.model.BibleBookDefinitions
import com.example.data.bible.model.Testament

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleHomeScreen(
    viewModel: BibleViewModel,
    onBackClick: () -> Unit,
    onOpenReader: (bookId: Int, chapter: Int, targetVerse: Int?) -> Unit,
    onSearchClick: () -> Unit,
    onSavedClick: () -> Unit,
    onReadingPlanClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedTranslation by viewModel.selectedTranslation.collectAsState()
    val readingSettings by viewModel.readingSettings.collectAsState()
    val lastPosition by viewModel.lastReadingPosition.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val todayVerse = viewModel.todayVerse

    var selectedTab by remember { mutableIntStateOf(1) } // 0: Old Testament, 1: New Testament (default New Testament for easy gospel access)
    var showSettingsDialog by remember { mutableStateOf(false) }
    var bookForChapterPicker by remember { mutableStateOf<BibleBook?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "पवित्र बाइबल",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Read the Word of God",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) Color(0xFF10B981) else Color(0xFF3B82F6))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isOnline) "Online" else "Offline Ready",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search Bible")
                    }
                    IconButton(onClick = onSavedClick) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmarks & Notes")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Quick Tools Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            val all = com.example.data.bible.model.BibleTranslation.ALL
                            val currentIndex = all.indexOfFirst { it.id == selectedTranslation.id }
                            val nextIndex = if (currentIndex == -1 || currentIndex == all.lastIndex) 0 else currentIndex + 1
                            viewModel.selectTranslation(all[nextIndex])
                        },
                        label = {
                            Text(
                                text = "📖 ${selectedTranslation.nameHindi}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )

                    AssistChip(
                        onClick = onReadingPlanClick,
                        label = { Text("रीडिंग प्लान (Plan)") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // Reading Plan Quick Access Banner / Active Reading Plans
            if (readingSettings.showActivatedPlansOnHome) {
                val allPlans = viewModel.getAllPlansList()
                val activatedPlans = allPlans.filter { it.id in readingSettings.activatedPlanIds }

                if (activatedPlans.isNotEmpty()) {
                    items(activatedPlans, key = { "home_plan_" + it.id }) { plan ->
                        ActivatedPlanHomeCard(
                            plan = plan,
                            viewModel = viewModel,
                            onClick = onReadingPlanClick
                        )
                    }
                }
            }

            // Today's Verse Card ("आज का वचन") - Default Hidden, shown only if setting enabled
            if (readingSettings.showTodaysScriptureOnHome) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "📖 आज का वचन",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                FilledTonalButton(
                                    onClick = {
                                        onOpenReader(todayVerse.bookId, todayVerse.chapter, todayVerse.verseNumber)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("पढ़ें (Read)", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (selectedTranslation.language == "hi") todayVerse.textHindi else todayVerse.textEnglish,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "— " + if (selectedTranslation.language == "hi") todayVerse.referenceHindi else todayVerse.referenceEnglish,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Continue Reading Card (if available)
            if (lastPosition != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "पढ़ना जारी रखें (Continue Reading)",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${lastPosition?.bookName} : अध्याय ${lastPosition?.chapter}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Button(
                                onClick = {
                                    val pos = lastPosition ?: return@Button
                                    onOpenReader(pos.bookId, pos.chapter, pos.verse)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("जारी रखें")
                            }
                        }
                    }
                }
            }

            // Tabs for Testament Selection
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "पुराना नियम (Old • 39)",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "नया नियम (New • 27)",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Books List
            val currentBooks = if (selectedTab == 0) {
                BibleBookDefinitions.oldTestamentBooks
            } else {
                BibleBookDefinitions.newTestamentBooks
            }

            items(currentBooks, key = { it.id }) { book ->
                BookRowItem(
                    book = book,
                    isHindi = selectedTranslation.language == "hi",
                    onClick = {
                        bookForChapterPicker = book
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    // 3-Step Book -> Chapter -> Verse Selector Modal
    if (bookForChapterPicker != null) {
        val activeBook = bookForChapterPicker!!
        BibleBookChapterVerseSelectorModal(
            initialBook = activeBook,
            initialChapter = 1,
            initialVerse = null,
            isHindi = selectedTranslation.language == "hi",
            onDismiss = { bookForChapterPicker = null },
            onSelectionComplete = { book, chapter, verse ->
                bookForChapterPicker = null
                onOpenReader(book.id, chapter, verse)
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        BibleSettingsDialog(
            settings = readingSettings,
            selectedTranslation = selectedTranslation,
            onFontSizeChange = { viewModel.updateFontSize(it) },
            onLineSpacingChange = { viewModel.updateLineSpacing(it) },
            onShowVerseNumbersChange = { viewModel.toggleVerseNumbers(it) },
            onThemeChange = { viewModel.updateTheme(it) },
            onTranslationChange = { viewModel.selectTranslation(it) },
            onVerseTapSelectionModeChange = { viewModel.updateVerseTapSelectionMode(it) },
            onShowTodaysScriptureOnHomeChange = { viewModel.toggleShowTodaysScriptureOnHome(it) },
            onShowActivatedPlansOnHomeChange = { viewModel.toggleShowActivatedPlansOnHome(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun BookRowItem(
    book: BibleBook,
    isHindi: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${book.id}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = if (isHindi) book.nameHindi else book.nameEnglish,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = (if (isHindi) book.nameEnglish else book.nameHindi) + " • ${book.chapterCount} Ch",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Text(
                    text = book.category,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ActivatedPlanHomeCard(
    plan: com.example.data.bible.model.ReadingPlanInfo,
    viewModel: BibleViewModel,
    onClick: () -> Unit
) {
    var progressList by remember { mutableStateOf<List<com.example.data.bible.local.ReadingPlanProgressEntity>>(emptyList()) }
    LaunchedEffect(plan.id) {
        viewModel.readingPlanRepository.getPlanProgress(plan.id).collect { list ->
            progressList = list
        }
    }
    val completedCount = progressList.count { it.isCompleted }
    val progressFraction = if (plan.totalDays > 0) completedCount.toFloat() / plan.totalDays.toFloat() else 0f
    val percentText = (progressFraction * 100).toInt()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = plan.titleHindi,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = plan.titleEnglish,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Plan",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "प्रोग्रेस: $completedCount / ${plan.totalDays} दिन",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "$percentText%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF10B981)
            )
        }
    }
}
