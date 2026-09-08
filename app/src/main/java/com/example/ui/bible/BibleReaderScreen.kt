package com.example.ui.bible

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val selectedTranslation by viewModel.selectedTranslation.collectAsState()
    val readingSettings by viewModel.readingSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showChapterPickerSheet by remember { mutableStateOf(false) }
    var selectedVerseForAction by remember { mutableStateOf<BibleVerse?>(null) }
    var verseForNoteDialog by remember { mutableStateOf<BibleVerse?>(null) }

    val listState = rememberLazyListState()

    // Initialize book & chapter
    LaunchedEffect(bookId, chapter) {
        viewModel.openBook(bookId, chapter, targetVerse)
    }

    // Scroll to target verse if specified
    LaunchedEffect(verses, targetVerse) {
        if (targetVerse != null && verses.isNotEmpty()) {
            val index = verses.indexOfFirst { it.verseNumber == targetVerse }
            if (index >= 0) {
                listState.animateScrollToItem(index)
                viewModel.clearTargetVerse()
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
                        onClick = { viewModel.previousChapter() },
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
                        onClick = { viewModel.nextChapter() },
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
                        OutlinedButton(onClick = { viewModel.openBook(43, 3) }) {
                            Text("यूहन्ना 3 पढ़ें (Read John 3 - Offline)")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Chapter Title Header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
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
                    }

                    items(verses, key = { "${it.bookId}_${it.chapter}_${it.verseNumber}" }) { verse ->
                        VerseRow(
                            verse = verse,
                            settings = readingSettings,
                            textColor = canvasTextColor,
                            onClick = { selectedVerseForAction = verse }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(40.dp))
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
private fun VerseRow(
    verse: BibleVerse,
    settings: BibleReadingSettings,
    textColor: Color,
    onClick: () -> Unit
) {
    val highlightColor = verse.highlightColor?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            null
        }
    }

    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * settings.lineSpacing.multiplier).sp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(highlightColor ?: Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            if (settings.showVerseNumbers) {
                Text(
                    text = "${verse.verseNumber}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = (settings.fontSize.sp * 0.75f).sp
                    ),
                    modifier = Modifier
                        .padding(end = 8.dp, top = 2.dp)
                        .widthIn(min = 22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = verse.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSizeSp,
                        lineHeight = lineHeightSp,
                        color = textColor,
                        fontWeight = FontWeight.Normal
                    )
                )

                // Indicators row for Bookmark and Note
                if (verse.isBookmarked || !verse.note.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (verse.isBookmarked) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = "Bookmarked",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!verse.note.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EditNote,
                                        contentDescription = "Note",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = verse.note,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
