package com.example.ui.bible

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.*

sealed class BibleReaderItem {
    data class Header(val bookName: String, val chapter: Int) : BibleReaderItem()
    data object IncompleteWarning : BibleReaderItem()
    data class SectionHeadingBlock(val text: String, val beforeVerse: Int) : BibleReaderItem()
    data class TitleBlock(val text: String, val beforeVerse: Int) : BibleReaderItem()
    data class ProseParagraphBlock(val verses: List<VerseItem>) : BibleReaderItem()
    data class PoetryBlockBlock(val lines: List<PoetryLineItem>) : BibleReaderItem()
    data class SubHeading(val text: String, val beforeVerse: Int) : BibleReaderItem()
    data class Paragraph(val verses: List<BibleVerse>, val startVerse: Int, val endVerse: Int, val isPoetic: Boolean = false) : BibleReaderItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    viewModel: BibleViewModel,
    bookId: Int,
    chapter: Int,
    targetVerse: Int?,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSavedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentBook by viewModel.currentBook.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val verses by viewModel.verses.collectAsState()
    val chapterSections by viewModel.chapterSections.collectAsState()
    val structuredBlocks by viewModel.structuredBlocks.collectAsState()
    val selectedTranslation by viewModel.selectedTranslation.collectAsState()
    val readingSettings by viewModel.readingSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isChapterIncomplete by viewModel.isChapterIncomplete.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNavigatorModal by remember { mutableStateOf(false) }
    var selectedVerseForAction by remember { mutableStateOf<BibleVerse?>(null) }
    var verseForNoteDialog by remember { mutableStateOf<BibleVerse?>(null) }
    var activeFootnoteSheet by remember { mutableStateOf<Pair<String, List<FootnoteItem>>?>(null) }

    val listState = rememberLazyListState()
    var slideForward by remember { mutableStateOf(true) }
    var currentTargetVerse by remember { mutableStateOf(targetVerse) }

    val activeAudioVerse by viewModel.audioManager.currentVerseNumber.collectAsState()
    val effectiveTargetVerse = activeAudioVerse ?: currentTargetVerse

    // Initialize book & chapter
    LaunchedEffect(bookId, chapter, targetVerse) {
        currentTargetVerse = targetVerse
        viewModel.openBook(bookId, chapter, targetVerse)
    }

    // Prepare list items: Structured blocks (Headings, Titles, Prose Paragraphs, Poetry Blocks) or legacy fallback
    val readerItems = remember(currentBook.id, currentChapter, structuredBlocks, chapterSections, verses, isChapterIncomplete, selectedTranslation.language) {
        val items = mutableListOf<BibleReaderItem>()
        val bName = if (selectedTranslation.language == "hi") currentBook.nameHindi else currentBook.nameEnglish
        items.add(BibleReaderItem.Header(bName, currentChapter))

        if (isChapterIncomplete) {
            items.add(BibleReaderItem.IncompleteWarning)
        }

        if (structuredBlocks.isNotEmpty()) {
            structuredBlocks.forEach { block ->
                when (block) {
                    is BibleContentBlock.SectionHeading -> items.add(BibleReaderItem.SectionHeadingBlock(block.text, block.beforeVerse))
                    is BibleContentBlock.Title -> items.add(BibleReaderItem.TitleBlock(block.text, block.beforeVerse))
                    is BibleContentBlock.ProseParagraph -> items.add(BibleReaderItem.ProseParagraphBlock(block.verses))
                    is BibleContentBlock.PoetryBlock -> items.add(BibleReaderItem.PoetryBlockBlock(block.lines))
                }
            }
        } else {
            val isPoeticBook = currentBook.id in listOf(18, 19, 20, 21, 22, 25)
            if (chapterSections.isEmpty() && verses.isNotEmpty()) {
                items.add(
                    BibleReaderItem.Paragraph(
                        verses = verses,
                        startVerse = verses.first().verseNumber,
                        endVerse = verses.last().verseNumber,
                        isPoetic = isPoeticBook
                    )
                )
            } else {
                chapterSections.forEach { section ->
                    if (section.heading != null && section.heading.headingText.isNotBlank()) {
                        items.add(BibleReaderItem.SubHeading(section.heading.headingText, section.heading.beforeVerse))
                    }
                    if (section.verses.isNotEmpty()) {
                        items.add(
                            BibleReaderItem.Paragraph(
                                verses = section.verses,
                                startVerse = section.verses.first().verseNumber,
                                endVerse = section.verses.last().verseNumber,
                                isPoetic = isPoeticBook
                            )
                        )
                    }
                }
            }
        }
        items
    }

    // Scroll to target verse if specified
    LaunchedEffect(currentTargetVerse, readerItems) {
        val tVerse = currentTargetVerse
        if (tVerse != null && readerItems.isNotEmpty()) {
            val targetIndex = readerItems.indexOfFirst { item ->
                when (item) {
                    is BibleReaderItem.SectionHeadingBlock -> item.beforeVerse == tVerse
                    is BibleReaderItem.TitleBlock -> item.beforeVerse == tVerse
                    is BibleReaderItem.ProseParagraphBlock -> item.verses.any { it.verseNumber == tVerse }
                    is BibleReaderItem.PoetryBlockBlock -> item.lines.any { it.verseNumber == tVerse }
                    is BibleReaderItem.Paragraph -> tVerse in item.startVerse..item.endVerse
                    is BibleReaderItem.SubHeading -> item.beforeVerse == tVerse
                    else -> false
                }
            }
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    LaunchedEffect(activeAudioVerse) {
        activeAudioVerse?.let { vNum ->
            val targetIndex = readerItems.indexOfFirst { item ->
                when (item) {
                    is BibleReaderItem.ProseParagraphBlock -> item.verses.any { it.verseNumber == vNum }
                    is BibleReaderItem.PoetryBlockBlock -> item.lines.any { it.verseNumber == vNum }
                    is BibleReaderItem.Paragraph -> item.verses.any { it.verseNumber == vNum }
                    else -> false
                }
            }
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    val density = LocalDensity.current
    // Swipe gesture listener: horizontal release triggers prev/next chapter
    val swipeModifier = Modifier.pointerInput(currentBook.id, currentChapter) {
        val swipeThresholdPx = with(density) { 70.dp.toPx() }
        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false)
                var totalDx = 0f
                var totalDy = 0f
                val pointerId = down.id

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (!change.pressed) {
                        // Finger lifted
                        if (kotlin.math.abs(totalDx) > swipeThresholdPx && kotlin.math.abs(totalDx) > 2.0f * kotlin.math.abs(totalDy)) {
                            if (totalDx < 0) {
                                // Swipe LEFT -> NEXT chapter
                                slideForward = true
                                currentTargetVerse = null
                                viewModel.nextChapter()
                            } else {
                                // Swipe RIGHT -> PREVIOUS chapter
                                slideForward = false
                                currentTargetVerse = null
                                viewModel.previousChapter()
                            }
                        }
                        break
                    }
                    totalDx += change.position.x - change.previousPosition.x
                    totalDy += change.position.y - change.previousPosition.y
                }
            }
        }
    }

    // Canvas styling based on reading settings theme
    val canvasBgColor = when (readingSettings.theme) {
        BibleTheme.LIGHT -> Color(0xFFFCFCFC)
        BibleTheme.DARK -> Color(0xFF0F172A)
        BibleTheme.SEPIA -> Color(0xFFFBF0D9)
        BibleTheme.SYSTEM -> MaterialTheme.colorScheme.background
    }

    val canvasTextColor = when (readingSettings.theme) {
        BibleTheme.LIGHT -> Color(0xFF1E293B)
        BibleTheme.DARK -> Color(0xFFF1F5F9)
        BibleTheme.SEPIA -> Color(0xFF382D20)
        BibleTheme.SYSTEM -> MaterialTheme.colorScheme.onBackground
    }

    val bookName = if (selectedTranslation.language == "hi") currentBook.nameHindi else currentBook.nameEnglish

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { showNavigatorModal = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$bookName $currentChapter",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Book, Chapter, Verse",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Translation Switcher Chip
                    AssistChip(
                        onClick = {
                            val next = when (selectedTranslation.id) {
                                BibleTranslation.HINDI_IRV.id -> BibleTranslation.ENGLISH_KJV
                                BibleTranslation.ENGLISH_KJV.id -> BibleTranslation.PARALLEL_HI_EN
                                else -> BibleTranslation.HINDI_IRV
                            }
                            viewModel.selectTranslation(next)
                        },
                        label = {
                            Text(
                                text = when (selectedTranslation.id) {
                                    BibleTranslation.HINDI_IRV.id -> "HIN"
                                    BibleTranslation.ENGLISH_KJV.id -> "ENG"
                                    else -> "HIN+ENG"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.padding(end = 2.dp)
                    )

                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onSavedClick) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Saved")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.FormatSize, contentDescription = "Text Settings")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                BibleAudioPlayerBar(
                    audioManager = viewModel.audioManager,
                    currentBook = currentBook,
                    currentChapter = currentChapter,
                    verses = verses
                )

                // Chapter navigation bar
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasPrev = currentChapter > 1 || currentBook.id > 1
                    OutlinedButton(
                        onClick = {
                            slideForward = false
                            currentTargetVerse = null
                            viewModel.previousChapter()
                        },
                        enabled = hasPrev,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("पिछला (Prev)", style = MaterialTheme.typography.labelSmall)
                    }

                    TextButton(onClick = { showNavigatorModal = true }) {
                        Text(
                            text = "Ch $currentChapter of ${currentBook.chapterCount} ▾",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    val hasNext = currentChapter < currentBook.chapterCount || currentBook.id < 66
                    Button(
                        onClick = {
                            slideForward = true
                            currentTargetVerse = null
                            viewModel.nextChapter()
                        },
                        enabled = hasNext,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("अगला (Next)", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(canvasBgColor)
                .padding(innerPadding)
                .then(swipeModifier)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (verses.isEmpty()) {
                // Empty state / Offline chapter placeholder
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "$bookName : अध्याय $currentChapter",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = canvasTextColor)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isOnline) {
                            "इंटरनेट से अध्याय लोड किया जा रहा है..."
                        } else {
                            "यह अध्याय अभी ऑफ़लाइन सहेजा नहीं गया है। कृपया नेटवर्क से कनेक्ट होने पर इसे रीफ़्रेश करें या अन्य अध्याय पढ़ें।"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(color = canvasTextColor.copy(alpha = 0.8f)),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isOnline) {
                        Button(onClick = { viewModel.refreshCurrentChapter() }) {
                            Text("अध्याय डाउनलोड करें (Download Chapter)")
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.openBook(43, 1) }) {
                            Text("यूहन्ना 1 पढ़ें (Read John 1 - Offline)")
                        }
                    }
                }
            } else {
                AnimatedContent(
                    targetState = "${currentBook.id}_$currentChapter",
                    transitionSpec = {
                        if (slideForward) {
                            (slideInHorizontally { width -> width / 3 } + fadeIn(tween(250))).togetherWith(
                                slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(250))
                            )
                        } else {
                            (slideInHorizontally { width -> -width / 3 } + fadeIn(tween(250))).togetherWith(
                                slideOutHorizontally { width -> width / 3 } + fadeOut(tween(250))
                            )
                        }
                    },
                    label = "ChapterSlide"
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 60.dp)
                    ) {
                        itemsIndexed(readerItems) { index, item ->
                            when (item) {
                                is BibleReaderItem.Header -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${item.bookName} ${item.chapter}",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = canvasTextColor
                                            )
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier
                                                .width(72.dp)
                                                .padding(top = 10.dp),
                                            thickness = 2.5.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                is BibleReaderItem.IncompleteWarning -> {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "अध्याय डेटा अपूर्ण है (Data Incomplete)",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Text(
                                                    text = "कुछ वचन अनुपलब्ध हैं। पूर्ण अध्याय सिंक करने के लिए यहाँ टैप करें।",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                            TextButton(onClick = { viewModel.refreshCurrentChapter() }) {
                                                Text("सिंक (Sync)")
                                            }
                                        }
                                    }
                                }

                                is BibleReaderItem.SectionHeadingBlock -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 22.dp, bottom = 10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(20.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.text,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 0.2.sp
                                            )
                                        )
                                    }
                                }

                                is BibleReaderItem.TitleBlock -> {
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = canvasTextColor.copy(alpha = 0.85f),
                                            letterSpacing = 0.1.sp
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp, bottom = 8.dp)
                                    )
                                }

                                is BibleReaderItem.ProseParagraphBlock -> {
                                    val versesMap = remember(verses) { verses.associateBy { it.verseNumber } }
                                    StructuredProseParagraphContent(
                                        verseItems = item.verses,
                                        versesStateMap = versesMap,
                                        targetVerse = effectiveTargetVerse,
                                        settings = readingSettings,
                                        textColor = canvasTextColor,
                                        onVerseClick = { selectedVerseForAction = it },
                                        onFootnoteClick = { vItem, fn ->
                                            activeFootnoteSheet = Pair("वचन ${vItem.verseNumber}", vItem.footnotes)
                                        },
                                        modifier = Modifier.padding(bottom = 14.dp)
                                    )
                                }

                                is BibleReaderItem.PoetryBlockBlock -> {
                                    val versesMap = remember(verses) { verses.associateBy { it.verseNumber } }
                                    StructuredPoetryBlockContent(
                                        lineItems = item.lines,
                                        versesStateMap = versesMap,
                                        targetVerse = effectiveTargetVerse,
                                        settings = readingSettings,
                                        textColor = canvasTextColor,
                                        onVerseClick = { selectedVerseForAction = it },
                                        onFootnoteClick = { lineItem, fn ->
                                            val refStr = if (lineItem.verseNumber != null) "वचन ${lineItem.verseNumber}" else "टिप्पणी"
                                            activeFootnoteSheet = Pair(refStr, lineItem.footnotes)
                                        },
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }

                                is BibleReaderItem.SubHeading -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 22.dp, bottom = 10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(20.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.text,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 0.2.sp
                                            )
                                        )
                                    }
                                }

                                is BibleReaderItem.Paragraph -> {
                                    if (item.isPoetic) {
                                        BiblePoeticContent(
                                            verses = item.verses,
                                            targetVerse = effectiveTargetVerse,
                                            settings = readingSettings,
                                            textColor = canvasTextColor,
                                            onVerseClick = { selectedVerseForAction = it },
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )
                                    } else {
                                        BibleParagraphContent(
                                            verses = item.verses,
                                            targetVerse = effectiveTargetVerse,
                                            settings = readingSettings,
                                            textColor = canvasTextColor,
                                            onVerseClick = { selectedVerseForAction = it },
                                            modifier = Modifier.padding(bottom = 16.dp)
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

    // Verse Action Bottom Sheet
    if (selectedVerseForAction != null) {
        val verse = selectedVerseForAction!!
        ModalBottomSheet(
            onDismissRequest = { selectedVerseForAction = null },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$bookName ${verse.chapter}:${verse.verseNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { selectedVerseForAction = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = verse.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Highlight Color Palette
                Text(
                    text = "हाइलाइट करें (Highlight)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HighlightColorCircle(color = Color(0xFFFEF08A), label = "Yellow") {
                        viewModel.setHighlight(verse, "#FEF08A")
                        selectedVerseForAction = null
                    }
                    HighlightColorCircle(color = Color(0xFFBAE6FD), label = "Blue") {
                        viewModel.setHighlight(verse, "#BAE6FD")
                        selectedVerseForAction = null
                    }
                    HighlightColorCircle(color = Color(0xFFBBF7D0), label = "Green") {
                        viewModel.setHighlight(verse, "#BBF7D0")
                        selectedVerseForAction = null
                    }
                    HighlightColorCircle(color = Color(0xFFFBCFE8), label = "Pink") {
                        viewModel.setHighlight(verse, "#FBCFE8")
                        selectedVerseForAction = null
                    }
                    if (verse.highlightColor != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.removeHighlight(verse)
                                selectedVerseForAction = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("हटाएँ", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Copy, Share, Bookmark, Note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Copy
                    ActionColumnButton(
                        icon = Icons.Default.ContentCopy,
                        label = "प्रतिलिपि (Copy)",
                        onClick = {
                            val ref = "$bookName ${verse.chapter}:${verse.verseNumber}"
                            val clipText = "$ref\n${verse.text}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Bible Verse", clipText))
                            Toast.makeText(context, "वचन कॉपी किया गया ($ref)", Toast.LENGTH_SHORT).show()
                            selectedVerseForAction = null
                        }
                    )

                    // Share
                    ActionColumnButton(
                        icon = Icons.Default.Share,
                        label = "साझा (Share)",
                        onClick = {
                            val ref = "$bookName ${verse.chapter}:${verse.verseNumber}"
                            val shareText = "$ref\n\n\"${verse.text}\"\n\n— Vinay Kumar AVJ"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Verse"))
                            selectedVerseForAction = null
                        }
                    )

                    // Bookmark
                    ActionColumnButton(
                        icon = if (verse.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        label = if (verse.isBookmarked) "सहेजा गया" else "बुकमार्क",
                        tint = if (verse.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            viewModel.toggleBookmark(verse)
                            selectedVerseForAction = null
                        }
                    )

                    // Note
                    ActionColumnButton(
                        icon = Icons.Default.EditNote,
                        label = "नोट्स (Note)",
                        onClick = {
                            verseForNoteDialog = verse
                            selectedVerseForAction = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add / Edit Note Dialog
    if (verseForNoteDialog != null) {
        val verse = verseForNoteDialog!!
        var noteText by remember { mutableStateOf(verse.note ?: "") }

        AlertDialog(
            onDismissRequest = { verseForNoteDialog = null },
            title = {
                Text(
                    text = "नोट लिखें: $bookName ${verse.chapter}:${verse.verseNumber}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("व्यक्तिगत विचार या प्रार्थना") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveNote(verse, noteText)
                    verseForNoteDialog = null
                }) {
                    Text("सहेजें (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = { verseForNoteDialog = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // 3-Step Book -> Chapter -> Verse Selector Modal
    if (showNavigatorModal) {
        BibleBookChapterVerseSelectorModal(
            initialBook = currentBook,
            initialChapter = currentChapter,
            initialVerse = currentTargetVerse,
            isHindi = selectedTranslation.language == "hi",
            onDismiss = { showNavigatorModal = false },
            onSelectionComplete = { book, ch, verse ->
                showNavigatorModal = false
                currentTargetVerse = verse
                viewModel.openBook(book.id, ch, verse)
            }
        )
    }

    // Reading Settings Dialog
    if (showSettingsDialog) {
        BibleSettingsDialog(
            settings = readingSettings,
            selectedTranslation = selectedTranslation,
            onFontSizeChange = { viewModel.updateFontSize(it) },
            onLineSpacingChange = { viewModel.updateLineSpacing(it) },
            onShowVerseNumbersChange = { viewModel.toggleVerseNumbers(it) },
            onThemeChange = { viewModel.updateTheme(it) },
            onTranslationChange = { viewModel.selectTranslation(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Footnote Details Modal
    if (activeFootnoteSheet != null) {
        val (titleStr, fnList) = activeFootnoteSheet!!
        ModalBottomSheet(
            onDismissRequest = { activeFootnoteSheet = null },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "पाद-टिप्पणी (Footnote) • $titleStr",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { activeFootnoteSheet = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                fnList.forEach { fn ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (fn.target.isNotBlank()) {
                                Text(
                                    text = fn.target,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = fn.text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HighlightColorCircle(
    color: Color,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = color,
        modifier = Modifier
            .size(38.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
private fun ActionColumnButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BibleParagraphContent(
    verses: List<BibleVerse>,
    targetVerse: Int?,
    settings: BibleReadingSettings,
    textColor: Color,
    onVerseClick: (BibleVerse) -> Unit,
    modifier: Modifier = Modifier
) {
    if (verses.any { !it.secondaryText.isNullOrBlank() }) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            verses.forEach { verse ->
                val isTarget = targetVerse != null && verse.verseNumber == targetVerse
                val highlightColor = verse.highlightColor?.let {
                    try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
                } ?: if (isTarget) Color(0xFFFEF08A).copy(alpha = 0.5f) else null

                val fontSizeSp = settings.fontSize.sp.sp
                val secondaryFontSizeSp = (settings.fontSize.sp * 0.9f).sp

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = highlightColor ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = if (isTarget) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD97706)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVerseClick(verse) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isTarget) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${verse.verseNumber}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "हिन्दी + English",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (verse.isBookmarked) {
                                    Text("🔖 ", fontSize = 12.sp)
                                }
                                if (!verse.note.isNullOrBlank()) {
                                    Text("📝 ", fontSize = 12.sp)
                                }
                            }
                        }

                        // Primary (Hindi) Verse
                        Text(
                            text = verse.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = fontSizeSp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                lineHeight = (settings.fontSize.sp * settings.lineSpacing.multiplier).sp
                            )
                        )

                        // Secondary (English) Verse
                        if (!verse.secondaryText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = verse.secondaryText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = secondaryFontSizeSp,
                                    fontWeight = FontWeight.Normal,
                                    color = textColor.copy(alpha = 0.88f),
                                    lineHeight = (settings.fontSize.sp * 0.9f * settings.lineSpacing.multiplier).sp
                                )
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * settings.lineSpacing.multiplier).sp
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Build the paragraph AnnotatedString with verse numbers and highlights
    val annotatedString = remember(verses, targetVerse, settings, textColor) {
        buildAnnotatedString {
            verses.forEachIndexed { index, verse ->
                pushStringAnnotation(tag = "VERSE_NUM", annotation = "${verse.verseNumber}")

                val isTarget = targetVerse != null && verse.verseNumber == targetVerse

                // Inline verse number (visually smaller and slightly superscripted, on the same line)
                if (settings.showVerseNumbers) {
                    withStyle(
                        SpanStyle(
                            color = if (isTarget) Color(0xFFD97706) else Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = (settings.fontSize.sp * 0.72f).sp,
                            baselineShift = BaselineShift(0.22f)
                        )
                    ) {
                        append("${verse.verseNumber} ")
                    }
                }

                // Verse text with highlight or target accent if present
                val highlightColor = verse.highlightColor?.let {
                    try {
                        Color(android.graphics.Color.parseColor(it))
                    } catch (e: Exception) {
                        null
                    }
                } ?: if (isTarget) Color(0xFFFEF08A).copy(alpha = 0.5f) else null

                withStyle(
                    SpanStyle(
                        color = textColor,
                        fontSize = fontSizeSp,
                        fontWeight = if (isTarget) FontWeight.SemiBold else FontWeight.Normal,
                        background = highlightColor ?: Color.Transparent
                    )
                ) {
                    append(verse.text)
                }

                // Inline icon indicators for bookmarks and notes
                if (verse.isBookmarked) {
                    withStyle(
                        SpanStyle(
                            fontSize = (settings.fontSize.sp * 0.7f).sp,
                            baselineShift = BaselineShift(0.25f)
                        )
                    ) {
                        append(" 🔖")
                    }
                }
                if (!verse.note.isNullOrBlank()) {
                    withStyle(
                        SpanStyle(
                            fontSize = (settings.fontSize.sp * 0.7f).sp,
                            baselineShift = BaselineShift(0.25f)
                        )
                    ) {
                        append(" 📝")
                    }
                }

                pop() // Pop VERSE_NUM tag

                if (index < verses.size - 1) {
                    append("  ")
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            lineHeight = lineHeightSp,
            letterSpacing = 0.2.sp
        ),
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(verses) {
                detectTapGestures { pos ->
                    layoutResult?.let { layout ->
                        val offset = layout.getOffsetForPosition(pos)
                        annotatedString.getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                val vNum = annotation.item.toIntOrNull()
                                val clicked = verses.find { it.verseNumber == vNum }
                                if (clicked != null) {
                                    onVerseClick(clicked)
                                }
                            }
                    }
                }
            }
    )
}

@Composable
private fun BiblePoeticContent(
    verses: List<BibleVerse>,
    targetVerse: Int?,
    settings: BibleReadingSettings,
    textColor: Color,
    onVerseClick: (BibleVerse) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * (settings.lineSpacing.multiplier + 0.18f)).sp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        verses.forEach { verse ->
            val isTarget = targetVerse != null && verse.verseNumber == targetVerse
            val highlightColor = verse.highlightColor?.let {
                try {
                    Color(android.graphics.Color.parseColor(it))
                } catch (e: Exception) {
                    null
                }
            } ?: if (isTarget) Color(0xFFFEF08A).copy(alpha = 0.5f) else null

            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

            val annotatedString = remember(verse, isTarget, settings, textColor) {
                buildAnnotatedString {
                    pushStringAnnotation(tag = "VERSE_NUM", annotation = "${verse.verseNumber}")

                    if (settings.showVerseNumbers) {
                        withStyle(
                            SpanStyle(
                                color = if (isTarget) Color(0xFFD97706) else Color(0xFF2563EB),
                                fontWeight = FontWeight.Bold,
                                fontSize = (settings.fontSize.sp * 0.72f).sp,
                                baselineShift = BaselineShift(0.22f)
                            )
                        ) {
                            append("${verse.verseNumber} ")
                        }
                    }

                    withStyle(
                        SpanStyle(
                            color = textColor,
                            fontSize = fontSizeSp,
                            fontWeight = if (isTarget) FontWeight.SemiBold else FontWeight.Normal,
                            background = highlightColor ?: Color.Transparent
                        )
                    ) {
                        append(verse.text)
                    }

                    if (verse.isBookmarked) {
                        withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.7f).sp, baselineShift = BaselineShift(0.25f))) {
                            append(" 🔖")
                        }
                    }
                    if (!verse.note.isNullOrBlank()) {
                        withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.7f).sp, baselineShift = BaselineShift(0.25f))) {
                            append(" 📝")
                        }
                    }

                    pop()
                }
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = lineHeightSp,
                    letterSpacing = 0.15.sp
                ),
                onTextLayout = { layoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 3.dp, bottom = 3.dp)
                    .pointerInput(verse) {
                        detectTapGestures {
                            onVerseClick(verse)
                        }
                    }
            )
        }
    }
}

@Composable
private fun StructuredProseParagraphContent(
    verseItems: List<VerseItem>,
    versesStateMap: Map<Int, BibleVerse>,
    targetVerse: Int?,
    settings: BibleReadingSettings,
    textColor: Color,
    onVerseClick: (BibleVerse) -> Unit,
    onFootnoteClick: (VerseItem, FootnoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * settings.lineSpacing.multiplier).sp
    val accentColor = MaterialTheme.colorScheme.primary
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedString = remember(verseItems, versesStateMap, targetVerse, settings, textColor, accentColor) {
        buildAnnotatedString {
            var currentVerseNum: Int? = null
            verseItems.forEachIndexed { index, vItem ->
                if (vItem.verseNumber != null) {
                    currentVerseNum = vItem.verseNumber
                }
                val effectiveVNum = currentVerseNum
                val bibleVerse = effectiveVNum?.let { versesStateMap[it] }
                val isTarget = targetVerse != null && effectiveVNum == targetVerse

                if (effectiveVNum != null) {
                    pushStringAnnotation(tag = "VERSE_NUM", annotation = "$effectiveVNum")
                }

                if (vItem.verseNumber != null && settings.showVerseNumbers) {
                    withStyle(
                        SpanStyle(
                            color = if (isTarget) Color(0xFFD97706) else Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = (settings.fontSize.sp * 0.72f).sp,
                            baselineShift = BaselineShift(0.22f)
                        )
                    ) {
                        append("${vItem.verseNumber} ")
                    }
                }

                val highlightColor = bibleVerse?.highlightColor?.let {
                    try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
                } ?: if (isTarget) Color(0xFFFEF08A).copy(alpha = 0.5f) else null

                withStyle(
                    SpanStyle(
                        color = textColor,
                        fontSize = fontSizeSp,
                        fontWeight = if (isTarget) FontWeight.SemiBold else FontWeight.Normal,
                        background = highlightColor ?: Color.Transparent
                    )
                ) {
                    append(vItem.text)
                }

                if (vItem.footnotes.isNotEmpty()) {
                    pushStringAnnotation(tag = "FOOTNOTE", annotation = "${effectiveVNum ?: 0}")
                    withStyle(
                        SpanStyle(
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = (settings.fontSize.sp * 0.72f).sp,
                            baselineShift = BaselineShift(0.35f)
                        )
                    ) {
                        append(" ✳")
                    }
                    pop()
                }

                if (bibleVerse?.isBookmarked == true) {
                    withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.7f).sp, baselineShift = BaselineShift(0.25f))) {
                        append(" 🔖")
                    }
                }
                if (!bibleVerse?.note.isNullOrBlank()) {
                    withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.7f).sp, baselineShift = BaselineShift(0.25f))) {
                        append(" 📝")
                    }
                }

                if (effectiveVNum != null) {
                    pop() // Pop VERSE_NUM
                }

                if (index < verseItems.size - 1) {
                    append("  ")
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            lineHeight = lineHeightSp,
            letterSpacing = 0.2.sp
        ),
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(verseItems) {
                detectTapGestures { pos ->
                    layoutResult?.let { layout ->
                        val offset = layout.getOffsetForPosition(pos)
                        val fnAnnotation = annotatedString.getStringAnnotations(tag = "FOOTNOTE", start = offset, end = offset).firstOrNull()
                        if (fnAnnotation != null) {
                            val vNum = fnAnnotation.item.toIntOrNull()
                            val clickedItem = verseItems.find { (it.verseNumber ?: vNum) == vNum && it.footnotes.isNotEmpty() }
                            if (clickedItem != null) {
                                onFootnoteClick(clickedItem, clickedItem.footnotes.first())
                                return@detectTapGestures
                            }
                        }

                        val verseAnnotation = annotatedString.getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset).firstOrNull()
                        if (verseAnnotation != null) {
                            val vNum = verseAnnotation.item.toIntOrNull()
                            if (vNum != null) {
                                val bv = versesStateMap[vNum] ?: BibleVerse(
                                    bookId = 0, bookName = "", chapter = 0, verseNumber = vNum, text = "", translationId = ""
                                )
                                onVerseClick(bv)
                            }
                        }
                    }
                }
            }
    )
}

@Composable
private fun StructuredPoetryBlockContent(
    lineItems: List<PoetryLineItem>,
    versesStateMap: Map<Int, BibleVerse>,
    targetVerse: Int?,
    settings: BibleReadingSettings,
    textColor: Color,
    onVerseClick: (BibleVerse) -> Unit,
    onFootnoteClick: (PoetryLineItem, FootnoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * (settings.lineSpacing.multiplier + 0.18f)).sp
    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        var activeVerseNum: Int? = null
        lineItems.forEach { line ->
            if (line.verseNumber != null) {
                activeVerseNum = line.verseNumber
            }
            val effectiveVNum = line.verseNumber ?: activeVerseNum
            val bibleVerse = effectiveVNum?.let { versesStateMap[it] }
            val isTarget = targetVerse != null && effectiveVNum == targetVerse

            val highlightColor = bibleVerse?.highlightColor?.let {
                try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
            } ?: if (isTarget) Color(0xFFFEF08A).copy(alpha = 0.5f) else null

            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

            val annotatedString = remember(line, bibleVerse, isTarget, settings, textColor, accentColor) {
                buildAnnotatedString {
                    if (effectiveVNum != null) {
                        pushStringAnnotation(tag = "VERSE_NUM", annotation = "$effectiveVNum")
                    }

                    if (line.verseNumber != null && settings.showVerseNumbers) {
                        withStyle(
                            SpanStyle(
                                color = if (isTarget) Color(0xFFD97706) else Color(0xFF2563EB),
                                fontWeight = FontWeight.Bold,
                                fontSize = (settings.fontSize.sp * 0.72f).sp,
                                baselineShift = BaselineShift(0.22f)
                            )
                        ) {
                            append("${line.verseNumber} ")
                        }
                    }

                    withStyle(
                        SpanStyle(
                            color = textColor,
                            fontSize = fontSizeSp,
                            fontWeight = if (isTarget) FontWeight.SemiBold else FontWeight.Normal,
                            background = highlightColor ?: Color.Transparent
                        )
                    ) {
                        append(line.text)
                    }

                    if (line.footnotes.isNotEmpty()) {
                        pushStringAnnotation(tag = "FOOTNOTE", annotation = "${effectiveVNum ?: 0}")
                        withStyle(
                            SpanStyle(
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = (settings.fontSize.sp * 0.72f).sp,
                                baselineShift = BaselineShift(0.35f)
                            )
                        ) {
                            append(" ✳")
                        }
                        pop()
                    }

                    if (bibleVerse?.isBookmarked == true) {
                        withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.7f).sp, baselineShift = BaselineShift(0.25f))) {
                            append(" 🔖")
                        }
                    }
                    if (!bibleVerse?.note.isNullOrBlank()) {
                        withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.7f).sp, baselineShift = BaselineShift(0.25f))) {
                            append(" 📝")
                        }
                    }

                    if (effectiveVNum != null) {
                        pop()
                    }
                }
            }

            val indentStartPadding = (line.indent * 18 + 10).dp

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = lineHeightSp,
                    letterSpacing = 0.15.sp
                ),
                onTextLayout = { layoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indentStartPadding, top = 2.dp, bottom = 2.dp)
                    .pointerInput(line) {
                        detectTapGestures { pos ->
                            layoutResult?.let { layout ->
                                val offset = layout.getOffsetForPosition(pos)
                                if (line.footnotes.isNotEmpty()) {
                                    val fnAnno = annotatedString.getStringAnnotations(tag = "FOOTNOTE", start = offset, end = offset).firstOrNull()
                                    if (fnAnno != null) {
                                        onFootnoteClick(line, line.footnotes.first())
                                        return@detectTapGestures
                                    }
                                }

                                if (effectiveVNum != null) {
                                    val bv = versesStateMap[effectiveVNum] ?: BibleVerse(
                                        bookId = 0, bookName = "", chapter = 0, verseNumber = effectiveVNum, text = line.text, translationId = ""
                                    )
                                    onVerseClick(bv)
                                }
                            }
                        }
                    }
            )
        }
    }
}
