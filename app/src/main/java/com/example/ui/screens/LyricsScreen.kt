package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.local.ChristianSongEntity
import com.example.data.bible.repository.BibleRepository
import com.example.data.bible.repository.LyricsRepository
import com.example.ui.components.VersePopupDialog
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.util.DetectedVerseRef
import com.example.util.VerseReferenceDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SongSortOrder {
    BY_NUMBER,
    BY_NAME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    lyricsRepository: LyricsRepository,
    onBackClick: () -> Unit,
    initialSongId: Long? = null,
    onOpenVerse: ((bookId: Int, chapter: Int, verse: Int?) -> Unit)? = null,
    bibleRepository: BibleRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isSyncingSheet by remember { mutableStateOf(false) }

    // Initial background sync / preloading
    LaunchedEffect(Unit) {
        lyricsRepository.initializePreloadedLyrics()
    }

    val allSongs by lyricsRepository.getAllSongs().collectAsState(initial = emptyList())

    // Active song for Song Detail Screen
    var activeSong by remember { mutableStateOf<ChristianSongEntity?>(null) }

    // Jump to initialSongId if supplied
    LaunchedEffect(initialSongId, allSongs) {
        if (initialSongId != null && activeSong == null && allSongs.isNotEmpty()) {
            val matched = allSongs.firstOrNull { it.id == initialSongId }
            if (matched != null) {
                activeSong = matched
            }
        }
    }

    // Keep activeSong in sync with database updates (e.g. when favorite changes)
    LaunchedEffect(allSongs) {
        activeSong?.let { current ->
            val updated = allSongs.firstOrNull { it.id == current.id }
            if (updated != null) {
                activeSong = updated
            }
        }
    }

    // Search and Sort states
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SongSortOrder.BY_NUMBER) }
    var filterFavoritesOnly by remember { mutableStateOf(false) }
    var showSortBottomSheet by remember { mutableStateOf(false) }

    // Retain list state across detail view navigation
    val listState = rememberLazyListState()

    // Scripture Popup modal
    var activePopupVerse by remember { mutableStateOf<DetectedVerseRef?>(null) }

    // Filter and sort songs
    val displaySongs = remember(allSongs, searchQuery, sortOrder, filterFavoritesOnly) {
        var list = allSongs

        // Search filtering (title or song number or lyrics content)
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter { song ->
                song.songNumber.toString() == q ||
                song.songNumber.toString().contains(q) ||
                song.title.lowercase().contains(q) ||
                song.content.lowercase().contains(q) ||
                song.artist.lowercase().contains(q) ||
                song.category.lowercase().contains(q)
            }
        }

        // Favorites filter
        if (filterFavoritesOnly) {
            list = list.filter { it.isFavorite }
        }

        // Sorting
        when (sortOrder) {
            SongSortOrder.BY_NUMBER -> {
                list.sortedWith(compareBy({ if (it.songNumber > 0) it.songNumber else 999999 }, { it.title }))
            }
            SongSortOrder.BY_NAME -> {
                list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            }
        }
    }

    // Manual Sync Action
    val syncFromSheetAction: () -> Unit = {
        coroutineScope.launch {
            if (isSyncingSheet) return@launch
            isSyncingSheet = true
            try {
                val result = lyricsRepository.syncLyricsFromGoogleSheet()
                val count = result.getOrThrow()
                if (count > 0) {
                    Toast.makeText(context, "गूगल शीट से $count गीत सफलतापूर्वक सिंक हुए!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "गीत पहले से अप-टू-डेट हैं", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "सिंक विफल: ${e.localizedMessage ?: "Network error"}", Toast.LENGTH_SHORT).show()
            } finally {
                isSyncingSheet = false
            }
        }
    }

    // Return to the exact position in the list
    val handleBackFromDetail: () -> Unit = {
        val currentSongId = activeSong?.id
        val targetIdx = displaySongs.indexOfFirst { it.id == currentSongId }
        if (targetIdx >= 0) {
            coroutineScope.launch {
                listState.scrollToItem(targetIdx)
            }
        }
        activeSong = null
    }

    // Intercept hardware/system back button when in detail view
    BackHandler(enabled = activeSong != null) {
        handleBackFromDetail()
    }

    if (activeSong != null) {
        // SONG DETAIL SCREEN (Reading Experience with Swipe support)
        SongDetailScreen(
            song = activeSong!!,
            allSongs = displaySongs,
            onBack = handleBackFromDetail,
            onToggleFavorite = { targetSong ->
                coroutineScope.launch {
                    lyricsRepository.toggleFavorite(targetSong)
                }
            },
            onSelectSong = { newSong ->
                activeSong = newSong
            },
            onOpenVerse = onOpenVerse,
            onVerseClick = { activePopupVerse = it },
            modifier = modifier
        )
    } else {
        // SONG LIST SCREEN (Modern UI)
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Christian Song Book",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "मसीही गीत पुस्तक • ${allSongs.size} गीत",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("song_list_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        // Refresh / Update Songs Button
                        val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "spin"
                        )

                        IconButton(
                            onClick = syncFromSheetAction,
                            enabled = !isSyncingSheet,
                            modifier = Modifier.testTag("refresh_songs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Refresh / Update Songs",
                                modifier = if (isSyncingSheet) Modifier.rotate(rotation) else Modifier,
                                tint = if (isSyncingSheet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Persistent, Sleek Search Bar + Filter/Sort Button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("song_search_bar"),
                            placeholder = {
                                Text(
                                    text = "शीर्षक या संख्या से खोजें (Search title or #)...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // Filter/Sort Icon Button
                        FilledTonalIconButton(
                            onClick = { showSortBottomSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("filter_sort_button"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (sortOrder == SongSortOrder.BY_NAME || filterFavoritesOnly)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter / Sort",
                                tint = if (sortOrder == SongSortOrder.BY_NAME || filterFavoritesOnly)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Active Filter Chips indicator if any
                if (filterFavoritesOnly || sortOrder == SongSortOrder.BY_NAME || searchQuery.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${displaySongs.size} गीत",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (sortOrder == SongSortOrder.BY_NAME) {
                            FilterChip(
                                selected = true,
                                onClick = { sortOrder = SongSortOrder.BY_NUMBER },
                                label = { Text("A-Z नाम अनुसार") },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        if (filterFavoritesOnly) {
                            FilterChip(
                                selected = true,
                                onClick = { filterFavoritesOnly = false },
                                label = { Text("⭐ केवल पसंदीदा") },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        if (searchQuery.isNotBlank()) {
                            FilterChip(
                                selected = true,
                                onClick = { searchQuery = "" },
                                label = { Text("\"$searchQuery\"") },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                }

                // Song List
                if (displaySongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "'$searchQuery' के लिए कोई गीत नहीं मिला"
                                else if (filterFavoritesOnly)
                                    "कोई पसंदीदा गीत सुरक्षित नहीं है"
                                else
                                    "गीत लोड हो रहे हैं...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isNotBlank() || filterFavoritesOnly) {
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        filterFavoritesOnly = false
                                        sortOrder = SongSortOrder.BY_NUMBER
                                    }
                                ) {
                                    Text("सभी गीत देखें")
                                }
                            } else {
                                OutlinedButton(onClick = syncFromSheetAction) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("शीट से सिंक करें (Sync)")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("song_list_view"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displaySongs, key = { it.id }) { song ->
                            SongListItemCard(
                                song = song,
                                onClick = { activeSong = song },
                                onToggleFavorite = {
                                    coroutineScope.launch {
                                        lyricsRepository.toggleFavorite(song)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter/Sort Bottom Sheet
    if (showSortBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "गीत क्रमबद्ध करें (Sort & Filter)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showSortBottomSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Option 1: Sort by Number (Default)
                Surface(
                    onClick = {
                        sortOrder = SongSortOrder.BY_NUMBER
                        showSortBottomSheet = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (sortOrder == SongSortOrder.BY_NUMBER)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatListNumbered,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "गीत संख्या अनुसार (Sort by Number)",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "1, 2, 3... (डिफ़ॉल्ट क्रम)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        RadioButton(
                            selected = sortOrder == SongSortOrder.BY_NUMBER,
                            onClick = {
                                sortOrder = SongSortOrder.BY_NUMBER
                                showSortBottomSheet = false
                            }
                        )
                    }
                }

                // Option 2: Sort by Name (A-Z / अ-ज्ञ)
                Surface(
                    onClick = {
                        sortOrder = SongSortOrder.BY_NAME
                        showSortBottomSheet = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (sortOrder == SongSortOrder.BY_NAME)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SortByAlpha,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "नाम अनुसार (Sort by Name)",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "A-Z / अ-ज्ञ वर्णमाला क्रम",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        RadioButton(
                            selected = sortOrder == SongSortOrder.BY_NAME,
                            onClick = {
                                sortOrder = SongSortOrder.BY_NAME
                                showSortBottomSheet = false
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Option 3: Filter Favorites Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { filterFavoritesOnly = !filterFavoritesOnly }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "केवल पसंदीदा गीत (Favorites Only)",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "स्टार चिह्नित गीत दिखाएं",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = filterFavoritesOnly,
                        onCheckedChange = { filterFavoritesOnly = it }
                    )
                }
            }
        }
    }

    // Scripture Popup Modal
    if (activePopupVerse != null && bibleRepository != null) {
        VersePopupDialog(
            verseRef = activePopupVerse!!,
            bibleRepository = bibleRepository,
            onDismiss = { activePopupVerse = null },
            onOpenFullChapter = { bId, ch, v ->
                activePopupVerse = null
                onOpenVerse?.invoke(bId, ch, v)
            }
        )
    }
}

/**
 * Modern Song List Item Card
 * - Circular badge for number
 * - Bold title
 * - Star icon toggle
 */
@Composable
private fun SongListItemCard(
    song: ChristianSongEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("song_item_${song.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Badge for Song Number
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (song.songNumber > 0) {
                    Text(
                        text = "${song.songNumber}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Song Info (Title & Snippet)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // First line snippet from lyrics
                val firstLine = remember(song.content) {
                    song.content.lines()
                        .map { it.trim() }
                        .firstOrNull { it.isNotBlank() && !it.startsWith("[") }
                        ?: song.artist
                }

                Text(
                    text = firstLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Star (Favorite) Toggle Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("song_favorite_btn_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (song.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (song.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * SONG DETAIL SCREEN (Reading Experience)
 * - Clean, distraction-free full-screen layout for lyrics
 * - Left/Right swipe (HorizontalPager) to seamlessly read Next / Previous songs
 * - Renders \n line breaks correctly
 * - Top App Bar: Font size controls (A- / A+), Favorite Star, Share
 * - Wake Lock: Keeps screen awake during singing
 * - Next / Prev song navigation buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongDetailScreen(
    song: ChristianSongEntity,
    allSongs: List<ChristianSongEntity>,
    onBack: () -> Unit,
    onToggleFavorite: (ChristianSongEntity) -> Unit,
    onSelectSong: (ChristianSongEntity) -> Unit,
    onOpenVerse: ((bookId: Int, chapter: Int, verse: Int?) -> Unit)? = null,
    onVerseClick: ((DetectedVerseRef) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Wake Lock: Keep device screen awake while reading/singing lyrics
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 2. Font Size Scaling: 'A-' and 'A+' (range 13sp to 32sp)
    var fontSizeSp by remember { mutableFloatStateOf(19f) }

    // Auto-scroll toggle for worship leading
    var isAutoScrolling by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableIntStateOf(1) } // 1x, 2x, 3x

    // Initial page based on current song
    val initialIndex = remember {
        allSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialIndex) { allSongs.size }

    // Sync active song when user swipes between pages
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage in allSongs.indices) {
            val current = allSongs[pagerState.currentPage]
            onSelectSong(current)
            isAutoScrolling = false
        }
    }

    val currentSong = allSongs.getOrNull(pagerState.currentPage) ?: song
    val currentIndex = pagerState.currentPage

    // Share Lyrics
    val shareLyricsAction = {
        val shareText = buildString {
            if (currentSong.songNumber > 0) {
                append("गीत संख्या #${currentSong.songNumber} - ")
            }
            appendLine(currentSong.title)
            appendLine()
            appendLine(currentSong.content)
            appendLine()
            append("— Christian Song Book")
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, currentSong.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "गीत साझा करें (Share Lyrics)"))
    }

    // Copy Lyrics
    val copyLyricsAction: (ChristianSongEntity) -> Unit = { targetSong ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(
            targetSong.title,
            "${if (targetSong.songNumber > 0) "#${targetSong.songNumber} - " else ""}${targetSong.title}\n\n${targetSong.content}"
        )
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "गीत के बोल कॉपी किए गए", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (currentSong.songNumber > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "#${currentSong.songNumber}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("song_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // a) Font size controls: 'A-' and 'A+'
                    IconButton(
                        onClick = { if (fontSizeSp > 13f) fontSizeSp -= 2f },
                        enabled = fontSizeSp > 13f,
                        modifier = Modifier.testTag("font_decrease_btn")
                    ) {
                        Text(
                            text = "A-",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (fontSizeSp > 13f)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    IconButton(
                        onClick = { if (fontSizeSp < 32f) fontSizeSp += 2f },
                        enabled = fontSizeSp < 32f,
                        modifier = Modifier.testTag("font_increase_btn")
                    ) {
                        Text(
                            text = "A+",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (fontSizeSp < 32f)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // b) Favorite Toggle: Star icon to add/remove from global Saved list
                    IconButton(
                        onClick = { onToggleFavorite(currentSong) },
                        modifier = Modifier.testTag("song_detail_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (currentSong.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (currentSong.isFavorite) "Saved in Favorites" else "Add to Favorites",
                            tint = if (currentSong.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // c) Share Button
                    IconButton(
                        onClick = shareLyricsAction,
                        modifier = Modifier.testTag("song_share_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Lyrics"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            // Distraction-free Bottom Bar with Prev/Next and AutoScroll
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Song Button
                    val hasPrev = currentIndex > 0
                    FilledTonalButton(
                        onClick = {
                            if (hasPrev) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(currentIndex - 1)
                                }
                            }
                        },
                        enabled = hasPrev,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("पिछला (Prev)", fontSize = 12.sp)
                    }

                    // Auto-Scroll Play/Pause
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { isAutoScrolling = !isAutoScrolling },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isAutoScrolling) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Auto Scroll",
                                tint = if (isAutoScrolling) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        if (isAutoScrolling) {
                            TextButton(
                                onClick = {
                                    scrollSpeed = when (scrollSpeed) {
                                        1 -> 2
                                        2 -> 3
                                        else -> 1
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("${scrollSpeed}x", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    // Next Song Button
                    val hasNext = currentIndex in 0 until (allSongs.size - 1)
                    FilledTonalButton(
                        onClick = {
                            if (hasNext) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(currentIndex + 1)
                                }
                            }
                        },
                        enabled = hasNext,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("अगला (Next)", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            val pageSong = allSongs.getOrNull(page) ?: return@HorizontalPager
            val pageScrollState = rememberScrollState()

            if (page == pagerState.currentPage) {
                LaunchedEffect(isAutoScrolling, scrollSpeed) {
                    while (isAutoScrolling) {
                        delay((60 / scrollSpeed).toLong())
                        if (pageScrollState.value < pageScrollState.maxValue) {
                            pageScrollState.dispatchRawDelta(2f)
                        } else {
                            isAutoScrolling = false
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(pageScrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Header Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pageSong.songNumber > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "गीत संख्या #${pageSong.songNumber}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "मसीही गीत",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Copy button
                            IconButton(
                                onClick = { copyLyricsAction(pageSong) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy lyrics",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = pageSong.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (pageSong.artist.isNotBlank() || pageSong.category.isNotBlank() || pageSong.keyScale.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (pageSong.artist.isNotBlank()) {
                                    Text(
                                        text = pageSong.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (pageSong.keyScale.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "Scale: ${pageSong.keyScale}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Full Lyrics content with exact \n line break handling
                val formattedLyrics = remember(pageSong.content) {
                    formatLyricsText(pageSong.content, primaryColor = Color(0xFF0284C7))
                }

                Text(
                    text = formattedLyrics,
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp * 1.6f).sp,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("song_lyrics_content_${pageSong.id}")
                )

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

/**
 * Format lyrics with proper line break rendering (\n),
 * and highlight section titles like [Chorus], [Verse 1], [Bridge].
 */
private fun formatLyricsText(raw: String, primaryColor: Color): AnnotatedString {
    // Standardize CRLF and escaped newlines \n that may come from CSV
    val sanitized = raw.replace("\\n", "\n").replace("\r\n", "\n").replace("\r", "\n")

    return buildAnnotatedString {
        val lines = sanitized.lines()
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                // Section Header like [Chorus], [Verse 1]
                pushStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                )
                append(trimmed)
                pop()
            } else {
                append(line)
            }
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}
