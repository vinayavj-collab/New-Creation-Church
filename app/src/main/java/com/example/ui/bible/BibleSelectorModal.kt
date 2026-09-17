package com.example.ui.bible

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.BibleBook
import com.example.data.bible.model.BibleBookDefinitions
import com.example.data.bible.model.BibleVerseCounts
import com.example.data.bible.model.Testament

enum class SelectorStep {
    BOOK,
    CHAPTER,
    VERSE
}

enum class BibleNavigatorMode {
    GRID, // 3-Column Side-by-Side simultaneous Grid Navigator
    LIST  // Step-by-Step Traditional Navigator (1. Book List -> 2. Chapter Grid -> 3. Verse Grid)
}

private const val NAV_PREF_NAME = "NAV_PREF"
private const val KEY_NAV_PREF = "NAV_PREF"
private const val LEGACY_PREFS = "bible_navigator_preferences"
private const val KEY_LEGACY_MODE = "saved_navigator_mode"

fun getSavedNavigatorMode(context: Context): BibleNavigatorMode {
    return try {
        val sp = context.getSharedPreferences(NAV_PREF_NAME, Context.MODE_PRIVATE)
        val modeStr = sp.getString(KEY_NAV_PREF, null)
            ?: context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE).getString(KEY_LEGACY_MODE, null)
        if (modeStr != null && modeStr.equals("LIST", ignoreCase = true)) {
            BibleNavigatorMode.LIST
        } else {
            BibleNavigatorMode.GRID
        }
    } catch (e: Exception) {
        BibleNavigatorMode.GRID
    }
}

fun saveNavigatorMode(context: Context, mode: BibleNavigatorMode) {
    try {
        val sp = context.getSharedPreferences(NAV_PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_NAV_PREF, mode.name).commit()
        
        // Also persist in legacy prefs
        context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LEGACY_MODE, mode.name)
            .commit()
    } catch (_: Exception) {}
}

/**
 * Main Bible Navigator Modal Bottom Sheet Dialog
 * Supports both:
 * 1. 3-Column Grid Navigator (Auto-navigates instantly on verse tap)
 * 2. Step-by-Step List Navigator (Old Tabbed Book -> Chapter -> Verse flow)
 * Persists user preference via SharedPreferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleBookChapterVerseSelectorModal(
    initialBook: BibleBook,
    initialChapter: Int,
    initialVerse: Int? = null,
    initialStep: SelectorStep = SelectorStep.CHAPTER,
    isHindi: Boolean = true,
    onDismiss: () -> Unit,
    onSelectionComplete: (book: BibleBook, chapter: Int, targetVerse: Int?) -> Unit
) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(getSavedNavigatorMode(context)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 12.dp)
        ) {
            // Header Bar with Mode Switcher Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Title and Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (currentMode == BibleNavigatorMode.GRID) Icons.Default.GridView else Icons.Default.ViewList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (currentMode == BibleNavigatorMode.GRID) "ग्रिड नेविगेटर (Grid)" else "सूची नेविगेटर (List)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = if (currentMode == BibleNavigatorMode.GRID) "पुस्तक • अध्याय • पद सीधे चुनें" else "चरणबद्ध पुस्तक, अध्याय एवं पद चुनें",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }

                // Right: Mode Toggle Buttons (Set to Default) + Close
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Consolidated Grid / List View Toggle Icon (Dynamically swaps icon to reflect current state)
                    val isGrid = currentMode == BibleNavigatorMode.GRID
                    IconButton(
                        onClick = {
                            val newMode = if (isGrid) BibleNavigatorMode.LIST else BibleNavigatorMode.GRID
                            currentMode = newMode
                            saveNavigatorMode(context, newMode)
                        },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isGrid) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = if (isGrid) "Switch to List View" else "Switch to Grid View",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Completely Decoupled View Container
            Crossfade(targetState = currentMode, label = "NavigatorModeTransition") { mode ->
                when (mode) {
                    BibleNavigatorMode.GRID -> {
                        Bible3ColumnGridNavigatorContent(
                            initialBook = initialBook,
                            initialChapter = initialChapter,
                            initialVerse = initialVerse,
                            isHindi = isHindi,
                            onNavigate = { book, chap, verse ->
                                onSelectionComplete(book, chap, verse)
                            }
                        )
                    }
                    BibleNavigatorMode.LIST -> {
                        BibleStepByStepListNavigatorContent(
                            initialBook = initialBook,
                            initialChapter = initialChapter,
                            initialVerse = initialVerse,
                            initialStep = initialStep,
                            isHindi = isHindi,
                            onNavigate = { book, chap, verse ->
                                onSelectionComplete(book, chap, verse)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3-Column Side-by-Side Grid Navigator Content
 * Auto-navigates immediately on verse tap!
 */
@Composable
fun Bible3ColumnGridNavigatorContent(
    initialBook: BibleBook,
    initialChapter: Int,
    initialVerse: Int?,
    isHindi: Boolean,
    onNavigate: (book: BibleBook, chapter: Int, targetVerse: Int?) -> Unit
) {
    var selectedBook by remember(initialBook) { mutableStateOf(initialBook) }
    var selectedChapter by remember(initialChapter) { mutableIntStateOf(initialChapter.coerceAtLeast(1)) }
    var selectedVerse by remember(initialVerse) { mutableStateOf(initialVerse) }
    var testamentFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val allBooks = BibleBookDefinitions.books

    val filteredBooks = remember(testamentFilter, searchQuery) {
        allBooks.filter { book ->
            val matchesTestament = when (testamentFilter) {
                "NT" -> book.testament == Testament.NEW
                "OT" -> book.testament == Testament.OLD
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                book.nameHindi.contains(searchQuery, ignoreCase = true) ||
                        book.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                        book.abbreviationHindi.contains(searchQuery, ignoreCase = true) ||
                        book.abbreviationEnglish.contains(searchQuery, ignoreCase = true) ||
                        book.id.toString() == searchQuery.trim()
            }
            matchesTestament && matchesSearch
        }
    }

    val maxChapter = remember(selectedBook) { selectedBook.chapterCount }
    val maxVerse = remember(selectedBook.id, selectedChapter) {
        BibleVerseCounts.getVerseCount(selectedBook.id, selectedChapter)
    }

    val bookListState = rememberLazyListState()
    val chapterGridState = rememberLazyGridState()
    val verseGridState = rememberLazyGridState()

    LaunchedEffect(selectedBook) {
        val index = filteredBooks.indexOfFirst { it.id == selectedBook.id }
        if (index >= 0) {
            bookListState.animateScrollToItem(index.coerceAtLeast(0))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Filter & Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Testament Filter Segmented Buttons
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1.3f)
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    listOf("ALL" to "सभी", "NT" to "नया (NT)", "OT" to "पुराना (OT)").forEach { (key, label) ->
                        val isSel = testamentFilter == key
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { testamentFilter = key }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Compact Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("खोजें...", fontSize = 11.sp) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        // 3-Column Header Titles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Col 1 Title: Books
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.weight(1.25f)
            ) {
                Text(
                    text = "1. पुस्तक (${filteredBooks.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Col 2 Title: Chapters
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.weight(0.9f)
            ) {
                Text(
                    text = "2. अध्याय ($maxChapter)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 11.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Col 3 Title: Verses
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.weight(0.95f)
            ) {
                Text(
                    text = "3. पद ($maxVerse) [टैप करें]",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 11.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // 3-Column Side-by-Side Main Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // COLUMN 1: Books (Scrollable vertical list)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
            ) {
                LazyColumn(
                    state = bookListState,
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredBooks, key = { "grid_book_${it.id}" }) { book ->
                        val isSelected = book.id == selectedBook.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBook = book
                                    selectedChapter = 1.coerceIn(1, book.chapterCount)
                                    selectedVerse = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (book.testament == Testament.NEW) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${book.id}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHindi) book.nameHindi else book.nameEnglish,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isHindi) book.nameEnglish else book.nameHindi,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // COLUMN 2: Chapters Grid
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
            ) {
                LazyVerticalGrid(
                    state = chapterGridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items((1..maxChapter).toList(), key = { "grid_chap_$it" }) { chap ->
                        val isSelected = chap == selectedChapter
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    selectedChapter = chap
                                    selectedVerse = null
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$chap",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // COLUMN 3: Verses Grid (AUTO-NAVIGATES INSTANTLY ON TAP)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
            ) {
                LazyVerticalGrid(
                    state = verseGridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Option 1: "सभी" / Full Chapter -> Auto-Navigates immediately!
                    item(key = "grid_all_verses") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    onNavigate(selectedBook, selectedChapter, null)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "सभी",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // Individual Verses 1..maxVerse -> Auto-Navigates immediately on tap!
                    items((1..maxVerse).toList(), key = { "grid_v_$it" }) { vNum ->
                        val isSelected = selectedVerse == vNum
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    // CRITICAL: Auto-Navigate immediately on verse tap
                                    onNavigate(selectedBook, selectedChapter, vNum)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$vNum",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Reference Status Indicator Footer
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "चयनित: ${if (isHindi) selectedBook.nameHindi else selectedBook.nameEnglish} $selectedChapter",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Text(
                    text = "पद पर टैप करते ही सीधे खुल जाएगा ⚡",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

/**
 * Step-by-Step List Navigator Content (Old Traditional Flow)
 * Step 1: Book List -> Step 2: Chapter Grid -> Step 3: Verse Grid (Auto-navigates on tap)
 * Completely isolated state and click handlers.
 */
@Composable
fun BibleStepByStepListNavigatorContent(
    initialBook: BibleBook,
    initialChapter: Int,
    initialVerse: Int?,
    initialStep: SelectorStep,
    isHindi: Boolean,
    onNavigate: (book: BibleBook, chapter: Int, targetVerse: Int?) -> Unit
) {
    var currentStep by remember(initialStep) { mutableStateOf(initialStep) }
    var activeBook by remember(initialBook) { mutableStateOf(initialBook) }
    var activeChapter by remember(initialChapter) { mutableIntStateOf(initialChapter.coerceAtLeast(1)) }
    var testamentFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val allBooks = BibleBookDefinitions.books
    val filteredBooks = remember(testamentFilter, searchQuery) {
        allBooks.filter { book ->
            val matchesTestament = when (testamentFilter) {
                "NT" -> book.testament == Testament.NEW
                "OT" -> book.testament == Testament.OLD
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                book.nameHindi.contains(searchQuery, ignoreCase = true) ||
                        book.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                        book.abbreviationHindi.contains(searchQuery, ignoreCase = true) ||
                        book.abbreviationEnglish.contains(searchQuery, ignoreCase = true) ||
                        book.id.toString() == searchQuery.trim()
            }
            matchesTestament && matchesSearch
        }
    }

    val maxChapter = remember(activeBook) { activeBook.chapterCount }
    val maxVerse = remember(activeBook.id, activeChapter) {
        BibleVerseCounts.getVerseCount(activeBook.id, activeChapter)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Step Navigation Tabs [1. पुस्तक | 2. अध्याय | 3. पद]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tab 1: Book
            val isBookActive = currentStep == SelectorStep.BOOK
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isBookActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .weight(1.2f)
                    .clickable { currentStep = SelectorStep.BOOK }
            ) {
                Text(
                    text = "1. ${if (isHindi) activeBook.nameHindi else activeBook.nameEnglish}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isBookActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isBookActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.5.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }

            // Tab 2: Chapter
            val isChapterActive = currentStep == SelectorStep.CHAPTER
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isChapterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .weight(1f)
                    .clickable { currentStep = SelectorStep.CHAPTER }
            ) {
                Text(
                    text = "2. अध्याय $activeChapter",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isChapterActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isChapterActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.5.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }

            // Tab 3: Verse
            val isVerseActive = currentStep == SelectorStep.VERSE
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isVerseActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .weight(0.9f)
                    .clickable { currentStep = SelectorStep.VERSE }
            ) {
                Text(
                    text = "3. पद चुनें",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isVerseActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isVerseActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.5.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }
        }

        // Step Content Area
        Box(modifier = Modifier.weight(1f)) {
            when (currentStep) {
                SelectorStep.BOOK -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Quick Testament Filters + Search
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Row(modifier = Modifier.padding(2.dp)) {
                                    listOf("ALL" to "सभी", "NT" to "नया (NT)", "OT" to "पुराना (OT)").forEach { (key, label) ->
                                        val isSel = testamentFilter == key
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { testamentFilter = key }
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 10.sp
                                                ),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("पुस्तक खोजें...", fontSize = 11.sp) },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        // Book List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredBooks, key = { "list_book_${it.id}" }) { book ->
                                val isSelected = book.id == activeBook.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeBook = book
                                            activeChapter = 1
                                            // Automatically transition to Chapter step
                                            currentStep = SelectorStep.CHAPTER
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else if (book.testament == Testament.NEW) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${book.id}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isHindi) book.nameHindi else book.nameEnglish,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Text(
                                                text = "${if (isHindi) book.nameEnglish else book.nameHindi} • ${book.chapterCount} अध्याय",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }

                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SelectorStep.CHAPTER -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (isHindi) activeBook.nameHindi else activeBook.nameEnglish} के अध्याय (1-$maxChapter):",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            // Quick Read Whole Chapter Button
                            TextButton(
                                onClick = {
                                    onNavigate(activeBook, activeChapter, null)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("अध्याय $activeChapter सीधे पढ़ें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 54.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items((1..maxChapter).toList(), key = { "list_chap_$it" }) { chap ->
                                val isSelected = chap == activeChapter
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = if (isSelected) null else BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clickable {
                                            activeChapter = chap
                                            // Advance to Verse step
                                            currentStep = SelectorStep.VERSE
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$chap",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SelectorStep.VERSE -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (isHindi) activeBook.nameHindi else activeBook.nameEnglish} $activeChapter के पद (1-$maxVerse):",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            // Read Whole Chapter
                            FilledTonalButton(
                                onClick = {
                                    onNavigate(activeBook, activeChapter, null)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("पूरा अध्याय पढ़ें", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 54.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items((1..maxVerse).toList(), key = { "list_v_$it" }) { vNum ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clickable {
                                            // CRITICAL: Auto-Navigate immediately on verse tap
                                            onNavigate(activeBook, activeChapter, vNum)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$vNum",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
