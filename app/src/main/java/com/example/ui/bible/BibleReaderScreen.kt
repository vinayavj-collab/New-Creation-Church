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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    val selectedTranslation by viewModel.selectedTranslation.collectAsState()
    val readingSettings by viewModel.readingSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isChapterIncomplete by viewModel.isChapterIncomplete.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showChapterPickerSheet by remember { mutableStateOf(false) }
    var selectedVerseForAction by remember { mutableStateOf<BibleVerse?>(null) }
    var verseForNoteDialog by remember { mutableStateOf<BibleVerse?>(null) }

    val scrollState = rememberScrollState()
    var slideForward by remember { mutableStateOf(true) }

    // Initialize book & chapter
    LaunchedEffect(bookId, chapter) {
        viewModel.openBook(bookId, chapter, targetVerse)
    }

    // Reset vertical scroll to top on chapter change
    LaunchedEffect(currentBook.id, currentChapter) {
        scrollState.scrollTo(0)
    }

    val density = LocalDensity.current
    // Swipe gesture listener: only triggers on clear horizontal release, preserves vertical scroll perfectly
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
                                viewModel.nextChapter()
                            } else {
                                // Swipe RIGHT -> PREVIOUS chapter
                                slideForward = false
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
                        modifier = Modifier.clickable { showChapterPickerSheet = true }
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
                                contentDescription = "Select Chapter",
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
                            val next = if (selectedTranslation.language == "hi") {
                                BibleTranslation.ENGLISH_WEB
                            } else {
                                BibleTranslation.HINDI_IRV
                            }
                            viewModel.selectTranslation(next)
                        },
                        label = {
                            Text(
                                text = if (selectedTranslation.language == "hi") "HIN" else "ENG",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.padding(end = 4.dp)
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

                    Text(
                        text = "Ch $currentChapter of ${currentBook.chapterCount}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    val hasNext = currentChapter < currentBook.chapterCount || currentBook.id < 66
                    Button(
                        onClick = {
                            slideForward = true
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Chapter Title Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = bookName,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = canvasTextColor
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "अध्याय $currentChapter",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = canvasTextColor.copy(alpha = 0.7f)
                                )
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .width(80.dp)
                                    .padding(top = 10.dp),
                                thickness = 2.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        }

                        // Incompleteness warning banner if any verse is missing
                        if (isChapterIncomplete) {
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

                        // Continuous paragraph style reading view with dynamic section headings
                        if (chapterSections.isEmpty() && verses.isNotEmpty()) {
                            BibleParagraphContent(
                                verses = verses,
                                settings = readingSettings,
                                textColor = canvasTextColor,
                                onVerseClick = { selectedVerseForAction = it }
                            )
                        } else {
                            chapterSections.forEachIndexed { index, section ->
                                if (section.heading != null && section.heading.headingText.isNotBlank()) {
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(20.dp))
                                    }
                                    Text(
                                        text = section.heading.headingText,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.2.sp
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 6.dp)
                                    )
                                }
                                if (section.verses.isNotEmpty()) {
                                    BibleParagraphContent(
                                        verses = section.verses,
                                        settings = readingSettings,
                                        textColor = canvasTextColor,
                                        onVerseClick = { selectedVerseForAction = it }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(50.dp))
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

    // Chapter Picker Modal Bottom Sheet
    if (showChapterPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChapterPickerSheet = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "$bookName - अध्याय चुनें (Select Chapter)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items((1..currentBook.chapterCount).toList()) { chap ->
                        val isCurrent = chap == currentChapter
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    showChapterPickerSheet = false
                                    viewModel.selectChapter(chap)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$chap",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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
}

@Composable
private fun BibleParagraphContent(
    verses: List<BibleVerse>,
    settings: BibleReadingSettings,
    textColor: Color,
    onVerseClick: (BibleVerse) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * settings.lineSpacing.multiplier).sp
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Build the continuous paragraph AnnotatedString
    val annotatedString = remember(verses, settings, textColor) {
        buildAnnotatedString {
            verses.forEachIndexed { index, verse ->
                pushStringAnnotation(tag = "VERSE_NUM", annotation = "${verse.verseNumber}")

                // Inline verse number (visually smaller and slightly superscripted, on the same line)
                if (settings.showVerseNumbers) {
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = (settings.fontSize.sp * 0.72f).sp,
                            baselineShift = BaselineShift(0.22f)
                        )
                    ) {
                        append("${verse.verseNumber} ")
                    }
                }

                // Verse text with highlight if present
                val highlightColor = verse.highlightColor?.let {
                    try {
                        Color(android.graphics.Color.parseColor(it))
                    } catch (e: Exception) {
                        null
                    }
                }

                withStyle(
                    SpanStyle(
                        color = textColor,
                        fontSize = fontSizeSp,
                        fontWeight = FontWeight.Normal,
                        background = highlightColor ?: Color.Transparent
                    )
                ) {
                    append(verse.text)
                }

                // Subtle inline icon indicators for bookmarks and notes
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
private fun HighlightColorCircle(
    color: Color,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {}
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
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tint)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
