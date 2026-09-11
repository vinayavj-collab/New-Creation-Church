package com.example.ui.bible

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
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
    onReadingPlanClick: () -> Unit = {},
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

    var showQuickFontSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNavigatorModal by remember { mutableStateOf(false) }
    var showTranslationPickerDialog by remember { mutableStateOf(false) }

    // YouVersion Multi-verse selection state
    var selectedVerseNumbers by remember { mutableStateOf(setOf<Int>()) }
    var showCompareDialog by remember { mutableStateOf(false) }
    var verseForNoteDialog by remember { mutableStateOf<BibleVerse?>(null) }
    var verseForPhotoDialog by remember { mutableStateOf<BibleVerse?>(null) }
    var verseForDetailsSheet by remember { mutableStateOf<BibleVerse?>(null) }
    var activeFootnoteSheet by remember { mutableStateOf<Pair<String, List<FootnoteItem>>?>(null) }

    val listState = rememberLazyListState()
    var slideForward by remember { mutableStateOf(true) }
    var currentTargetVerse by remember { mutableStateOf(targetVerse) }

    val activeAudioVerse by viewModel.audioManager.currentVerseNumber.collectAsState()
    val effectiveTargetVerse = currentTargetVerse ?: activeAudioVerse

    LaunchedEffect(activeAudioVerse) {
        if (activeAudioVerse != null && viewModel.audioManager.isPlaying.value) {
            currentTargetVerse = activeAudioVerse
        }
    }

    // Keep Screen On based on settings
    DisposableEffect(readingSettings.screenTimeoutMinutes) {
        val activity = context as? Activity
        if (readingSettings.screenTimeoutMinutes == -1 || readingSettings.screenTimeoutMinutes > 0) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Reset selection when changing chapter or book
    LaunchedEffect(bookId, chapter) {
        selectedVerseNumbers = emptySet()
        currentTargetVerse = targetVerse
        viewModel.openBook(bookId, chapter, targetVerse)
    }

    val bookName = if (selectedTranslation.language == "hi") currentBook.nameHindi else currentBook.nameEnglish

    // Formatted reference title for selected verses (e.g. "यूहन्ना 3:16-17")
    val selectionReferenceTitle = remember(selectedVerseNumbers, bookName, currentChapter) {
        if (selectedVerseNumbers.isEmpty()) ""
        else {
            val sortedList = selectedVerseNumbers.sorted()
            if (sortedList.size == 1) {
                "$bookName $currentChapter:${sortedList.first()}"
            } else if (sortedList.last() - sortedList.first() == sortedList.size - 1) {
                "$bookName $currentChapter:${sortedList.first()}-${sortedList.last()}"
            } else {
                "$bookName $currentChapter:${sortedList.joinToString(", ")}"
            }
        }
    }

    val selectedVersesList = remember(selectedVerseNumbers, verses) {
        verses.filter { it.verseNumber in selectedVerseNumbers }.sortedBy { it.verseNumber }
    }

    // Prepare list items
    val readerItems = remember(
        currentBook.id,
        currentChapter,
        structuredBlocks,
        chapterSections,
        verses,
        isChapterIncomplete,
        selectedTranslation.language,
        readingSettings.showSubheadings,
        readingSettings.showParagraphAndIndents
    ) {
        val items = mutableListOf<BibleReaderItem>()
        items.add(BibleReaderItem.Header(bookName, currentChapter))

        if (isChapterIncomplete) {
            items.add(BibleReaderItem.IncompleteWarning)
        }

        if (structuredBlocks.isNotEmpty() && readingSettings.showParagraphAndIndents) {
            structuredBlocks.forEach { block ->
                when (block) {
                    is BibleContentBlock.SectionHeading -> {
                        if (readingSettings.showSubheadings) {
                            items.add(BibleReaderItem.SectionHeadingBlock(block.text, block.beforeVerse))
                        }
                    }
                    is BibleContentBlock.Title -> {
                        if (readingSettings.showSubheadings) {
                            items.add(BibleReaderItem.TitleBlock(block.text, block.beforeVerse))
                        }
                    }
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
                        isPoetic = isPoeticBook && readingSettings.showParagraphAndIndents
                    )
                )
            } else {
                chapterSections.forEach { section ->
                    if (readingSettings.showSubheadings && section.heading != null && section.heading.headingText.isNotBlank()) {
                        items.add(BibleReaderItem.SubHeading(section.heading.headingText, section.heading.beforeVerse))
                    }
                    if (section.verses.isNotEmpty()) {
                        items.add(
                            BibleReaderItem.Paragraph(
                                verses = section.verses,
                                startVerse = section.verses.first().verseNumber,
                                endVerse = section.verses.last().verseNumber,
                                isPoetic = isPoeticBook && readingSettings.showParagraphAndIndents
                            )
                        )
                    }
                }
            }
        }
        items
    }

    // Scroll to target verse
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
    // Swipe gesture for chapter navigation
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
                        if (kotlin.math.abs(totalDx) > swipeThresholdPx && kotlin.math.abs(totalDx) > 2.0f * kotlin.math.abs(totalDy)) {
                            if (totalDx < 0) {
                                slideForward = true
                                currentTargetVerse = null
                                selectedVerseNumbers = emptySet()
                                viewModel.nextChapter()
                            } else {
                                slideForward = false
                                currentTargetVerse = null
                                selectedVerseNumbers = emptySet()
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

    // Themes Canvas Colors
    val canvasBgColor = when (readingSettings.theme) {
        BibleTheme.PAPER -> Color(0xFFFBF6EE)
        BibleTheme.WOOD -> Color(0xFFF3E8D3)
        BibleTheme.EYE_PROTECTION, BibleTheme.SEPIA -> Color(0xFFFEF3E2)
        BibleTheme.LIGHT -> Color(0xFFFFFFFF)
        BibleTheme.NIGHT -> Color(0xFF1F2937)
        BibleTheme.DARK -> Color(0xFF0F172A)
        BibleTheme.AMOLED -> Color(0xFF000000)
        BibleTheme.EMERALD -> Color(0xFFEBF2EC)
        BibleTheme.SYSTEM -> MaterialTheme.colorScheme.background
    }

    val defaultTextColor = when (readingSettings.theme) {
        BibleTheme.PAPER -> Color(0xFF2C241E)
        BibleTheme.WOOD -> Color(0xFF3B2B20)
        BibleTheme.EYE_PROTECTION, BibleTheme.SEPIA -> Color(0xFF2E2519)
        BibleTheme.LIGHT -> Color(0xFF1E293B)
        BibleTheme.NIGHT -> Color(0xFFF3F4F6)
        BibleTheme.DARK -> Color(0xFFE2E8F0)
        BibleTheme.AMOLED -> Color(0xFFFFFFFF)
        BibleTheme.EMERALD -> Color(0xFF143522)
        BibleTheme.SYSTEM -> MaterialTheme.colorScheme.onBackground
    }

    val canvasTextColor = readingSettings.customTextColorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { defaultTextColor }
    } ?: defaultTextColor

    val customHeadingColor = readingSettings.customHeadingColorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    }

    val customSubHeadingColor = readingSettings.customSubHeadingColorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    }

    val activeFontFamily = when (readingSettings.fontStyle) {
        BibleFontFamilyType.SYSTEM_DEFAULT -> FontFamily.SansSerif
        BibleFontFamilyType.CLASSIC_SERIF -> FontFamily.Serif
        BibleFontFamilyType.MODERN_POPPINS -> FontFamily.SansSerif
        BibleFontFamilyType.ELEGANT_ROZHA -> FontFamily.Serif
        BibleFontFamilyType.TRADITIONAL_BOOK -> FontFamily.Serif
        BibleFontFamilyType.MONOSPACE_STUDY -> FontFamily.Monospace
    }

    var showTopOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Spacious Book & Chapter selector with ample room
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.clickable { showNavigatorModal = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$bookName $currentChapter",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Book and Chapter",
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
                    // Translation Quick Indicator Chip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .pointerInput(selectedTranslation, readingSettings.translationToggleBehavior) {
                                detectTapGestures(
                                    onTap = {
                                        if (readingSettings.translationToggleBehavior == TranslationToggleBehavior.SINGLE_TAP_SHOW_ALL) {
                                            showTranslationPickerDialog = true
                                        } else {
                                            val all = BibleTranslation.ALL
                                            val currentIndex = all.indexOfFirst { it.id == selectedTranslation.id }
                                            val nextIndex = if (currentIndex == -1 || currentIndex == all.lastIndex) 0 else currentIndex + 1
                                            viewModel.selectTranslation(all[nextIndex])
                                        }
                                    },
                                    onLongPress = {
                                        showTranslationPickerDialog = true
                                    }
                                )
                            }
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = when (selectedTranslation.id) {
                                BibleTranslation.HINDI_IRV.id -> "IRV"
                                BibleTranslation.HINDI_BSI_OV.id -> "BSI"
                                BibleTranslation.HINDI_ERV.id -> "ERV"
                                BibleTranslation.HINDI_ULB.id -> "ULB"
                                BibleTranslation.ENGLISH_KJV.id -> "KJV"
                                BibleTranslation.ENGLISH_WEB.id -> "WEB"
                                BibleTranslation.ENGLISH_BBE.id -> "BBE"
                                else -> "HI+EN"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    // Audio Button (Kept right here as requested!)
                    IconButton(onClick = {
                        viewModel.toggleAudioPlayer(!readingSettings.showAudioPlayer)
                    }) {
                        Icon(
                            imageVector = if (readingSettings.showAudioPlayer) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Audio Bible",
                            tint = if (readingSettings.showAudioPlayer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 3-Dots / Customization & Extra Icons Shifted Here
                    Box {
                        IconButton(onClick = { showTopOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options & Settings")
                        }

                        DropdownMenu(
                            expanded = showTopOverflowMenu,
                            onDismissRequest = { showTopOverflowMenu = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("पठन एवं थीम अनुकूलन (Display & Themes)") },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showTopOverflowMenu = false
                                    showQuickFontSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("खोजें (Search Scripture)") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                onClick = {
                                    showTopOverflowMenu = false
                                    onSearchClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("सहेजे गए (Bookmarks & Notes)") },
                                leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                                onClick = {
                                    showTopOverflowMenu = false
                                    onSavedClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("रीडिंग प्लान (Reading Plan)") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showTopOverflowMenu = false
                                    onReadingPlanClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("अनुवाद बदलें (Switch Translation)") },
                                leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null) },
                                onClick = {
                                    showTopOverflowMenu = false
                                    val all = BibleTranslation.ALL
                                    val currentIndex = all.indexOfFirst { it.id == selectedTranslation.id }
                                    val nextIndex = if (currentIndex == -1 || currentIndex == all.lastIndex) 0 else currentIndex + 1
                                    viewModel.selectTranslation(all[nextIndex])
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("सम्पूर्ण सेटिंग्स (Full Settings)") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showTopOverflowMenu = false
                                    showSettingsDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (readingSettings.showAudioPlayer) {
                    BibleAudioPlayerBar(
                        audioManager = viewModel.audioManager,
                        currentBook = currentBook,
                        currentChapter = currentChapter,
                        verses = verses
                    )
                }

                // YouVersion Multi-verse Floating Action Bar
                AnimatedVisibility(
                    visible = selectedVerseNumbers.isNotEmpty(),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            // Row 1: Verse Reference and Highlight Palette
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectionReferenceTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )

                                // Highlight color dots
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(
                                        Color(0xFFFEF08A) to "#FEF08A",
                                        Color(0xFFBBF7D0) to "#BBF7D0",
                                        Color(0xFFBAE6FD) to "#BAE6FD",
                                        Color(0xFFFBCFE8) to "#FBCFE8",
                                        Color(0xFFDDD6FE) to "#DDD6FE"
                                    ).forEach { (color, hex) ->
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .clickable {
                                                    selectedVersesList.forEach { v ->
                                                        viewModel.setHighlight(v, hex)
                                                    }
                                                    selectedVerseNumbers = emptySet()
                                                }
                                        )
                                    }

                                    // Clear highlight
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                            .clickable {
                                                selectedVersesList.forEach { v ->
                                                    viewModel.removeHighlight(v)
                                                }
                                                selectedVerseNumbers = emptySet()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear Highlight", modifier = Modifier.size(14.dp))
                                    }

                                    // Close selection
                                    IconButton(
                                        onClick = { selectedVerseNumbers = emptySet() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Deselect", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Row 2: YouVersion Signature Action Buttons (Share, Photo Image, Compare, Favorite, Bookmark, Note, Copy, Audio)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Share
                                YouVersionActionItem(
                                    icon = Icons.Default.Share,
                                    label = "साझा",
                                    onClick = {
                                        val fullText = selectedVersesList.joinToString("\n") { "${it.verseNumber}. ${it.text}" }
                                        val shareBody = "$selectionReferenceTitle\n\n$fullText\n\n— YouVersion Bible"
                                        val intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareBody)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Verse"))
                                        selectedVerseNumbers = emptySet()
                                    }
                                )

                                // 2. Image (Photo Verse)
                                YouVersionActionItem(
                                    icon = Icons.Default.PhotoLibrary,
                                    label = "फोटो",
                                    onClick = {
                                        val firstVerse = selectedVersesList.firstOrNull()
                                        if (firstVerse != null) {
                                            val combinedText = selectedVersesList.joinToString(" ") { it.text }
                                            verseForPhotoDialog = firstVerse.copy(text = combinedText)
                                        }
                                        selectedVerseNumbers = emptySet()
                                    }
                                )

                                // 3. Compare Translations
                                YouVersionActionItem(
                                    icon = Icons.Default.CompareArrows,
                                    label = "तुलना",
                                    onClick = {
                                        showCompareDialog = true
                                    }
                                )

                                // 4. Favorite
                                val anyNotFav = selectedVersesList.any { !it.isFavorite }
                                YouVersionActionItem(
                                    icon = if (anyNotFav) Icons.Default.StarBorder else Icons.Default.Star,
                                    label = "पसंदीदा",
                                    tint = if (!anyNotFav) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface,
                                    onClick = {
                                        selectedVersesList.forEach { viewModel.toggleFavorite(it) }
                                        selectedVerseNumbers = emptySet()
                                    }
                                )

                                // 5. Bookmark
                                val anyNotBookmarked = selectedVersesList.any { !it.isBookmarked }
                                YouVersionActionItem(
                                    icon = if (anyNotBookmarked) Icons.Default.BookmarkBorder else Icons.Default.Bookmark,
                                    label = "बुकमार्क",
                                    tint = if (!anyNotBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    onClick = {
                                        selectedVersesList.forEach { viewModel.toggleBookmark(it) }
                                        selectedVerseNumbers = emptySet()
                                    }
                                )

                                // 6. Note Editor
                                YouVersionActionItem(
                                    icon = Icons.Default.EditNote,
                                    label = "नोट्स",
                                    onClick = {
                                        verseForNoteDialog = selectedVersesList.firstOrNull()
                                        selectedVerseNumbers = emptySet()
                                    }
                                )

                                // 7. Copy
                                YouVersionActionItem(
                                    icon = Icons.Default.ContentCopy,
                                    label = "कॉपी",
                                    onClick = {
                                        val fullText = selectedVersesList.joinToString("\n") { "${it.verseNumber}. ${it.text}" }
                                        val clipText = "$selectionReferenceTitle\n$fullText"
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Bible Verse", clipText))
                                        Toast.makeText(context, "कॉपी किया गया ($selectionReferenceTitle)", Toast.LENGTH_SHORT).show()
                                        selectedVerseNumbers = emptySet()
                                    }
                                )

                                // 8. Play Audio
                                YouVersionActionItem(
                                    icon = Icons.Default.PlayCircleOutline,
                                    label = "ऑडियो",
                                    onClick = {
                                        val firstV = selectedVersesList.firstOrNull()?.verseNumber ?: 1
                                        viewModel.audioManager.playFromVerse(firstV, currentBook.id, currentChapter, verses)
                                        selectedVerseNumbers = emptySet()
                                    }
                                )
                            }
                        }
                    }
                }

                // Default Chapter Bottom Navigation Bar (when no verses selected)
                if (selectedVerseNumbers.isEmpty()) {
                    Surface(
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
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
                                Text("पिछला", style = MaterialTheme.typography.labelSmall)
                            }

                            TextButton(onClick = { showNavigatorModal = true }) {
                                Text(
                                    text = "अध्याय $currentChapter / ${currentBook.chapterCount}",
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
                                Text("अगला", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
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
                        text = if (isOnline) "इंटरनेट से अध्याय लोड किया जा रहा है..." else "यह अध्याय अभी ऑफ़लाइन सहेजा नहीं गया है। कृपया नेटवर्क से कनेक्ट होने पर रीफ़्रेश करें।",
                        style = MaterialTheme.typography.bodyMedium.copy(color = canvasTextColor.copy(alpha = 0.8f)),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isOnline) {
                        Button(onClick = { viewModel.refreshCurrentChapter() }) {
                            Text("अध्याय डाउनलोड करें")
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.openBook(43, 1) }) {
                            Text("यूहन्ना 1 पढ़ें (Read John 1)")
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
                                                fontFamily = activeFontFamily,
                                                color = canvasTextColor
                                            )
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier
                                                .width(50.dp)
                                                .padding(top = 10.dp),
                                            thickness = 2.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
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
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "अध्याय डेटा अपूर्ण है",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Text(
                                                    text = "पूर्ण अध्याय सिंक करने के लिए यहाँ टैप करें।",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                            TextButton(onClick = { viewModel.refreshCurrentChapter() }) {
                                                Text("सिंक")
                                            }
                                        }
                                    }
                                }

                                is BibleReaderItem.SectionHeadingBlock -> {
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = activeFontFamily,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.2.sp
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 20.dp, bottom = 8.dp)
                                    )
                                }

                                is BibleReaderItem.TitleBlock -> {
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            fontFamily = activeFontFamily,
                                            color = canvasTextColor.copy(alpha = 0.85f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 6.dp)
                                    )
                                }

                                 is BibleReaderItem.ProseParagraphBlock -> {
                                    val versesMap = remember(verses) { verses.associateBy { it.verseNumber } }
                                    YouVersionProseParagraph(
                                        verseItems = item.verses,
                                        versesStateMap = versesMap,
                                        selectedVerseNumbers = selectedVerseNumbers,
                                        targetVerse = effectiveTargetVerse,
                                        settings = readingSettings,
                                        fontFamily = activeFontFamily,
                                        textColor = canvasTextColor,
                                        isNewTestament = currentBook.id >= 40,
                                        onVerseSingleTap = { vNum ->
                                            currentTargetVerse = vNum
                                            selectedVerseNumbers = emptySet()
                                            viewModel.audioManager.setCurrentVerseNumber(vNum)
                                            if (viewModel.audioManager.isPlaying.value) {
                                                viewModel.audioManager.playFromVerse(vNum, currentBook.id, currentChapter, verses)
                                            }
                                        },
                                        onVerseLongPress = { vNum ->
                                            selectedVerseNumbers = if (vNum in selectedVerseNumbers) {
                                                selectedVerseNumbers - vNum
                                            } else {
                                                selectedVerseNumbers + vNum
                                            }
                                        },
                                        onAttachmentClick = { vNum ->
                                            verseForDetailsSheet = versesMap[vNum]
                                        },
                                        onFootnoteClick = { vItem, fn ->
                                            activeFootnoteSheet = Pair("वचन ${vItem.verseNumber}", vItem.footnotes)
                                        },
                                        modifier = Modifier.padding(bottom = 14.dp)
                                    )
                                }

                                is BibleReaderItem.PoetryBlockBlock -> {
                                    val versesMap = remember(verses) { verses.associateBy { it.verseNumber } }
                                    YouVersionPoetryBlock(
                                        lineItems = item.lines,
                                        versesStateMap = versesMap,
                                        selectedVerseNumbers = selectedVerseNumbers,
                                        targetVerse = effectiveTargetVerse,
                                        settings = readingSettings,
                                        fontFamily = activeFontFamily,
                                        textColor = canvasTextColor,
                                        isNewTestament = currentBook.id >= 40,
                                        onVerseSingleTap = { vNum ->
                                            currentTargetVerse = vNum
                                            selectedVerseNumbers = emptySet()
                                            viewModel.audioManager.setCurrentVerseNumber(vNum)
                                            if (viewModel.audioManager.isPlaying.value) {
                                                viewModel.audioManager.playFromVerse(vNum, currentBook.id, currentChapter, verses)
                                            }
                                        },
                                        onVerseLongPress = { vNum ->
                                            selectedVerseNumbers = if (vNum in selectedVerseNumbers) {
                                                selectedVerseNumbers - vNum
                                            } else {
                                                selectedVerseNumbers + vNum
                                            }
                                        },
                                        onAttachmentClick = { vNum ->
                                            verseForDetailsSheet = versesMap[vNum]
                                        },
                                        onFootnoteClick = { lineItem, fn ->
                                            val refStr = if (lineItem.verseNumber != null) "वचन ${lineItem.verseNumber}" else "टिप्पणी"
                                            activeFootnoteSheet = Pair(refStr, lineItem.footnotes)
                                        },
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }

                                is BibleReaderItem.SubHeading -> {
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = activeFontFamily,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.2.sp
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 20.dp, bottom = 8.dp)
                                    )
                                }

                                is BibleReaderItem.Paragraph -> {
                                    val versesMap = remember(verses) { verses.associateBy { it.verseNumber } }
                                    YouVersionStandardParagraph(
                                        verses = item.verses,
                                        selectedVerseNumbers = selectedVerseNumbers,
                                        targetVerse = effectiveTargetVerse,
                                        settings = readingSettings,
                                        fontFamily = activeFontFamily,
                                        textColor = canvasTextColor,
                                        isNewTestament = currentBook.id >= 40,
                                        onVerseSingleTap = { vNum ->
                                            currentTargetVerse = vNum
                                            selectedVerseNumbers = emptySet()
                                            viewModel.audioManager.setCurrentVerseNumber(vNum)
                                            if (viewModel.audioManager.isPlaying.value) {
                                                viewModel.audioManager.playFromVerse(vNum, currentBook.id, currentChapter, verses)
                                            }
                                        },
                                        onVerseLongPress = { vNum ->
                                            selectedVerseNumbers = if (vNum in selectedVerseNumbers) {
                                                selectedVerseNumbers - vNum
                                            } else {
                                                selectedVerseNumbers + vNum
                                            }
                                        },
                                        onAttachmentClick = { vNum ->
                                            verseForDetailsSheet = versesMap[vNum] ?: item.verses.find { it.verseNumber == vNum }
                                        },
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

    // Quick Reading Options Sheet
    if (showQuickFontSheet) {
        YouVersionQuickFontSheet(
            settings = readingSettings,
            onFontSizeChange = { viewModel.updateFontSize(it) },
            onLineSpacingChange = { viewModel.updateLineSpacing(it) },
            onFontStyleChange = { viewModel.updateFontStyle(it) },
            onThemeChange = { viewModel.updateTheme(it) },
            onScreenTimeoutChange = { viewModel.setScreenTimeout(it) },
            onCustomTextColorChange = { viewModel.updateCustomTextColor(it) },
            onCustomHeadingColorChange = { viewModel.updateCustomHeadingColor(it) },
            onCustomSubHeadingColorChange = { viewModel.updateCustomSubHeadingColor(it) },
            onToggleOriginalFormat = { viewModel.toggleOriginalFormatMode(it) },
            onToggleJesusWordsInRed = { viewModel.toggleJesusWordsInRed(it) },
            onToggleJustify = { viewModel.toggleJustifyBibleText(it) },
            onOpenFullSettings = { showSettingsDialog = true },
            onDismiss = { showQuickFontSheet = false }
        )
    }

    // YouVersion Compare Translations Dialog
    if (showCompareDialog) {
        BibleVerseCompareDialog(
            referenceTitle = selectionReferenceTitle,
            selectedVerses = selectedVersesList,
            onDismiss = { showCompareDialog = false }
        )
    }

    // Rich Note Editor Dialog
    if (verseForNoteDialog != null) {
        val verse = verseForNoteDialog!!
        BibleNoteEditorDialog(
            verse = verse,
            initialNote = verse.note ?: "",
            onSave = { updatedText ->
                viewModel.saveNote(verse, updatedText)
                verseForNoteDialog = null
            },
            onDelete = {
                viewModel.deleteNote(verse.bookId, verse.chapter, verse.verseNumber)
                verseForNoteDialog = null
            },
            onDismiss = { verseForNoteDialog = null }
        )
    }

    // Bible Verse Photo Generator Dialog
    if (verseForPhotoDialog != null) {
        BibleVersePhotoDialog(
            verse = verseForPhotoDialog!!,
            onDismiss = { verseForPhotoDialog = null }
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

    // Full Reading Settings Dialog
    if (showSettingsDialog) {
        BibleSettingsDialog(
            settings = readingSettings,
            selectedTranslation = selectedTranslation,
            onFontSizeChange = { viewModel.updateFontSize(it) },
            onLineSpacingChange = { viewModel.updateLineSpacing(it) },
            onFontStyleChange = { viewModel.updateFontStyle(it) },
            onShowVerseNumbersChange = { viewModel.toggleVerseNumbers(it) },
            onShowSubheadingsChange = { viewModel.toggleSubheadings(it) },
            onShowParagraphAndIndentsChange = { viewModel.toggleParagraphAndIndents(it) },
            onOriginalFormatModeChange = { viewModel.toggleOriginalFormatMode(it) },
            onShowJesusWordsInRedChange = { viewModel.toggleJesusWordsInRed(it) },
            onJesusWordsColorChange = { viewModel.updateJesusWordsColor(it) },
            onCustomTextColorChange = { viewModel.updateCustomTextColor(it) },
            onCustomHeadingColorChange = { viewModel.updateCustomHeadingColor(it) },
            onCustomSubHeadingColorChange = { viewModel.updateCustomSubHeadingColor(it) },
            onDualBibleConfigChange = { enabled, hId, eId, mode ->
                viewModel.configureDualBible(enabled, hId, eId, mode)
            },
            onShowFavoritesHintChange = { viewModel.toggleFavoritesHint(it) },
            onShowBookmarkHintChange = { viewModel.toggleBookmarkHint(it) },
            onShowHighlightsChange = { viewModel.toggleHighlights(it) },
            onShowNoteHintChange = { viewModel.toggleNoteHint(it) },
            onJustifyBibleTextChange = { viewModel.toggleJustifyBibleText(it) },
            onSuggestVerseSelectionChange = { viewModel.toggleSuggestVerseSelection(it) },
            onShowAudioPlayerChange = { viewModel.toggleAudioPlayer(it) },
            onScreenTimeoutChange = { viewModel.setScreenTimeout(it) },
            onRememberPositionChange = { viewModel.toggleRememberPosition(it) },
            onResetToDefault = { viewModel.resetReadingSettings() },
            onThemeChange = { viewModel.updateTheme(it) },
            onTranslationChange = { viewModel.selectTranslation(it) },
            onTranslationToggleBehaviorChange = { viewModel.updateTranslationToggleBehavior(it) },
            onVerseTapSelectionModeChange = { viewModel.updateVerseTapSelectionMode(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showTranslationPickerDialog) {
        TranslationPickerDialog(
            selectedTranslation = selectedTranslation,
            onTranslationSelect = { translation ->
                viewModel.selectTranslation(translation)
                showTranslationPickerDialog = false
            },
            onDismiss = { showTranslationPickerDialog = false }
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

    // Interactive Verse Attachments (Notes, Bookmark, Favorite Details) Modal Bottom Sheet
    if (verseForDetailsSheet != null) {
        val verse = verseForDetailsSheet!!
        val verseRefLabel = "$bookName $currentChapter:${verse.verseNumber}"

        ModalBottomSheet(
            onDismissRequest = { verseForDetailsSheet = null },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = verseRefLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = { verseForDetailsSheet = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Scripture preview text
                Text(
                    text = "“${verse.text}”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 1. Attached Note Section
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!verse.note.isNullOrBlank()) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "📝 आपका नोट (Note)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                )
                            }

                            Row {
                                if (!verse.note.isNullOrBlank()) {
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteNote(verse.bookId, verse.chapter, verse.verseNumber)
                                            Toast.makeText(context, "नोट हटाया गया", Toast.LENGTH_SHORT).show()
                                            verseForDetailsSheet = null
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Note", tint = Color(0xFF991B1B), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (!verse.note.isNullOrBlank()) {
                            Text(
                                text = verse.note,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF334155))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val target = verse
                                    verseForDetailsSheet = null
                                    verseForNoteDialog = target
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("नोट खोलें व संपादित करें (Edit Note)")
                            }
                        } else {
                            Text(
                                text = "इस वचन पर अभी कोई व्यक्तिगत नोट नहीं लिखा गया है।",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val target = verse
                                    verseForDetailsSheet = null
                                    verseForNoteDialog = target
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("नया नोट लिखें (Add Note)")
                            }
                        }
                    }
                }

                // 2. Bookmark & Favorite Status Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bookmark Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (verse.isBookmarked) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.toggleBookmark(verse)
                                Toast.makeText(context, if (verse.isBookmarked) "बुकमार्क हटाया गया" else "बुकमार्क जोड़ा गया", Toast.LENGTH_SHORT).show()
                                verseForDetailsSheet = verse.copy(isBookmarked = !verse.isBookmarked)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (verse.isBookmarked) "🔖 बुकमार्क है" else "🔖 बुकमार्क करें",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (verse.isBookmarked) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    // Favorite Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (verse.isFavorite) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.toggleFavorite(verse)
                                Toast.makeText(context, if (verse.isFavorite) "पसंदीदा हटाया गया" else "पसंदीदा में जोड़ा गया", Toast.LENGTH_SHORT).show()
                                verseForDetailsSheet = verse.copy(isFavorite = !verse.isFavorite)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (verse.isFavorite) "⭐ पसंदीदा है" else "⭐ पसंदीदा बनाएं",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (verse.isFavorite) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    YouVersionActionItem(
                        icon = Icons.Default.VolumeUp,
                        label = "Audio",
                        onClick = {
                            viewModel.audioManager.playFromVerse(verse.verseNumber, verse.bookId, verse.chapter, verses)
                            verseForDetailsSheet = null
                        }
                    )
                    YouVersionActionItem(
                        icon = Icons.Default.CompareArrows,
                        label = "Compare",
                        onClick = {
                            selectedVerseNumbers = setOf(verse.verseNumber)
                            showCompareDialog = true
                            verseForDetailsSheet = null
                        }
                    )
                    YouVersionActionItem(
                        icon = Icons.Default.Image,
                        label = "Photo",
                        onClick = {
                            verseForPhotoDialog = verse
                            verseForDetailsSheet = null
                        }
                    )
                    YouVersionActionItem(
                        icon = Icons.Default.Share,
                        label = "Share",
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                val transName = if (selectedTranslation.language == "hi") selectedTranslation.nameHindi else selectedTranslation.nameEnglish
                                putExtra(Intent.EXTRA_TEXT, "“${verse.text}”\n- $verseRefLabel ($transName)")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Scripture"))
                            verseForDetailsSheet = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun YouVersionActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
    }
}

private fun getJesusWordColor(settings: BibleReadingSettings, isNewTestament: Boolean): Color? {
    if (!settings.showJesusWordsInRed || !isNewTestament) return null
    return try {
        Color(android.graphics.Color.parseColor(settings.jesusWordsColorHex))
    } catch (e: Exception) {
        Color(0xFFDC2626)
    }
}

@Composable
private fun YouVersionStandardParagraph(
    verses: List<BibleVerse>,
    selectedVerseNumbers: Set<Int>,
    targetVerse: Int?,
    settings: BibleReadingSettings,
    fontFamily: FontFamily,
    textColor: Color,
    isNewTestament: Boolean,
    onVerseSingleTap: (Int) -> Unit,
    onVerseLongPress: (Int) -> Unit,
    onAttachmentClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val jesusColor = getJesusWordColor(settings, isNewTestament)
    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * settings.lineSpacing.multiplier).sp
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedString = remember(verses, selectedVerseNumbers, targetVerse, settings, textColor, jesusColor) {
        buildAnnotatedString {
            verses.forEachIndexed { index, verse ->
                pushStringAnnotation(tag = "VERSE_NUM", annotation = "${verse.verseNumber}")

                val isSelected = verse.verseNumber in selectedVerseNumbers
                val isTarget = targetVerse != null && verse.verseNumber == targetVerse

                // Subtle verse number
                if (settings.showVerseNumbers) {
                    pushStringAnnotation(tag = "VERSE_NUM_ONLY", annotation = "${verse.verseNumber}")
                    withStyle(
                        SpanStyle(
                            color = if (isSelected || isTarget) Color(0xFFD97706) else Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (settings.fontSize.sp * 0.70f).sp,
                            baselineShift = BaselineShift(0.25f)
                        )
                    ) {
                        append("${verse.verseNumber} ")
                    }
                    pop()
                }

                // Background highlight or YouVersion selection effect
                val highlightColor = if (isSelected) {
                    Color(0xFF93C5FD).copy(alpha = 0.40f)
                } else if (settings.showHighlights && verse.highlightColor != null) {
                    try { Color(android.graphics.Color.parseColor(verse.highlightColor)) } catch (e: Exception) { null }
                } else if (isTarget) {
                    Color(0xFFFEF08A).copy(alpha = 0.5f)
                } else null

                withStyle(
                    SpanStyle(
                        color = jesusColor ?: textColor,
                        fontSize = fontSizeSp,
                        fontWeight = if (isSelected || isTarget) FontWeight.Medium else FontWeight.Normal,
                        background = highlightColor ?: Color.Transparent,
                        textDecoration = if (isSelected) androidx.compose.ui.text.style.TextDecoration.Underline else null
                    )
                ) {
                    append(verse.text)
                }

                if (!verse.secondaryText.isNullOrBlank()) {
                    append("\n")
                    withStyle(
                        SpanStyle(
                            color = (jesusColor ?: textColor).copy(alpha = 0.72f),
                            fontSize = (fontSizeSp.value * 0.90f).sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Normal
                        )
                    ) {
                        append("🇬🇧 ${verse.secondaryText}\n")
                    }
                }

                // Relatable Indicators with Attachment Click Trigger
                if (settings.showFavoritesHint && verse.isFavorite) {
                    pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "${verse.verseNumber}")
                    withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                        append(" ⭐")
                    }
                    pop()
                }
                if (settings.showBookmarkHint && verse.isBookmarked) {
                    pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "${verse.verseNumber}")
                    withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                        append(" 🔖")
                    }
                    pop()
                }
                if (settings.showNoteHint && !verse.note.isNullOrBlank()) {
                    pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "${verse.verseNumber}")
                    withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                        append(" 📝")
                    }
                    pop()
                }

                pop() // Pop VERSE_NUM

                if (index < verses.size - 1) {
                    append("  ")
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = fontFamily,
            lineHeight = lineHeightSp,
            letterSpacing = 0.2.sp,
            textAlign = if (settings.justifyBibleText) TextAlign.Justify else TextAlign.Start
        ),
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(verses) {
                detectTapGestures(
                    onTap = { pos ->
                        layoutResult?.let { layout ->
                            val offset = layout.getOffsetForPosition(pos)
                            val attachAnnotation = annotatedString.getStringAnnotations(tag = "ATTACHMENT_CLICK", start = offset, end = offset).firstOrNull()
                            if (attachAnnotation != null) {
                                val vNum = attachAnnotation.item.toIntOrNull()
                                if (vNum != null) {
                                    onAttachmentClick(vNum)
                                    return@detectTapGestures
                                }
                            }

                            val numOnly = annotatedString.getStringAnnotations(tag = "VERSE_NUM_ONLY", start = offset, end = offset).firstOrNull()?.item?.toIntOrNull()
                            val fullVerse = annotatedString.getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset).firstOrNull()?.item?.toIntOrNull()

                            if (settings.verseTapSelectionMode == VerseTapSelectionMode.VERSE_NUMBER_ONLY) {
                                if (numOnly != null) {
                                    onVerseSingleTap(numOnly)
                                } else if (fullVerse != null) {
                                    onVerseLongPress(fullVerse)
                                }
                            } else {
                                val vNum = fullVerse ?: numOnly
                                if (vNum != null) {
                                    onVerseSingleTap(vNum)
                                }
                            }
                        }
                    },
                    onLongPress = { pos ->
                        layoutResult?.let { layout ->
                            val offset = layout.getOffsetForPosition(pos)
                            annotatedString.getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    annotation.item.toIntOrNull()?.let { onVerseLongPress(it) }
                                }
                        }
                    }
                )
            }
    )
}

@Composable
private fun YouVersionProseParagraph(
    verseItems: List<VerseItem>,
    versesStateMap: Map<Int, BibleVerse>,
    selectedVerseNumbers: Set<Int>,
    targetVerse: Int?,
    settings: BibleReadingSettings,
    fontFamily: FontFamily,
    textColor: Color,
    isNewTestament: Boolean,
    onVerseSingleTap: (Int) -> Unit,
    onVerseLongPress: (Int) -> Unit,
    onAttachmentClick: (Int) -> Unit,
    onFootnoteClick: (VerseItem, FootnoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * settings.lineSpacing.multiplier).sp
    val accentColor = MaterialTheme.colorScheme.primary
    val jesusColor = getJesusWordColor(settings, isNewTestament)
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedString = remember(verseItems, versesStateMap, selectedVerseNumbers, targetVerse, settings, textColor, accentColor, jesusColor) {
        buildAnnotatedString {
            var currentVerseNum: Int? = null
            verseItems.forEachIndexed { index, vItem ->
                if (vItem.verseNumber != null) {
                    currentVerseNum = vItem.verseNumber
                }
                val effectiveVNum = currentVerseNum
                val bibleVerse = effectiveVNum?.let { versesStateMap[it] }
                val isSelected = effectiveVNum != null && effectiveVNum in selectedVerseNumbers
                val isTarget = targetVerse != null && effectiveVNum == targetVerse

                if (effectiveVNum != null) {
                    pushStringAnnotation(tag = "VERSE_NUM", annotation = "$effectiveVNum")
                }

                if (vItem.verseNumber != null && settings.showVerseNumbers) {
                    pushStringAnnotation(tag = "VERSE_NUM_ONLY", annotation = "$effectiveVNum")
                    withStyle(
                        SpanStyle(
                            color = if (isSelected || isTarget) Color(0xFFD97706) else Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (settings.fontSize.sp * 0.70f).sp,
                            baselineShift = BaselineShift(0.25f)
                        )
                    ) {
                        append("${vItem.verseNumber} ")
                    }
                    pop()
                }

                val highlightColor = if (isSelected) {
                    Color(0xFF93C5FD).copy(alpha = 0.40f)
                } else if (settings.showHighlights && bibleVerse?.highlightColor != null) {
                    try { Color(android.graphics.Color.parseColor(bibleVerse.highlightColor)) } catch (e: Exception) { null }
                } else if (isTarget) {
                    Color(0xFFFEF08A).copy(alpha = 0.5f)
                } else null

                withStyle(
                    SpanStyle(
                        color = jesusColor ?: textColor,
                        fontSize = fontSizeSp,
                        fontWeight = if (isSelected || isTarget) FontWeight.Medium else FontWeight.Normal,
                        background = highlightColor ?: Color.Transparent,
                        textDecoration = if (isSelected) androidx.compose.ui.text.style.TextDecoration.Underline else null
                    )
                ) {
                    append(vItem.text)
                }

                if (!bibleVerse?.secondaryText.isNullOrBlank()) {
                    append("\n")
                    withStyle(
                        SpanStyle(
                            color = (jesusColor ?: textColor).copy(alpha = 0.72f),
                            fontSize = (fontSizeSp.value * 0.90f).sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Normal
                        )
                    ) {
                        append("🇬🇧 ${bibleVerse.secondaryText}\n")
                    }
                }

                if (vItem.footnotes.isNotEmpty()) {
                    pushStringAnnotation(tag = "FOOTNOTE", annotation = "${effectiveVNum ?: 0}")
                    withStyle(
                        SpanStyle(
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = (settings.fontSize.sp * 0.70f).sp,
                            baselineShift = BaselineShift(0.35f)
                        )
                    ) {
                        append(" ✳")
                    }
                    pop()
                }

                // Relatable Indicators
                if (settings.showFavoritesHint && bibleVerse?.isFavorite == true) {
                    pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "${effectiveVNum ?: 0}")
                    withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                        append(" ⭐")
                    }
                    pop()
                }
                if (settings.showBookmarkHint && bibleVerse?.isBookmarked == true) {
                    pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "${effectiveVNum ?: 0}")
                    withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                        append(" 🔖")
                    }
                    pop()
                }
                if (settings.showNoteHint && !bibleVerse?.note.isNullOrBlank()) {
                    pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "${effectiveVNum ?: 0}")
                    withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                        append(" 📝")
                    }
                    pop()
                }

                if (effectiveVNum != null) {
                    pop()
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
            fontFamily = fontFamily,
            lineHeight = lineHeightSp,
            letterSpacing = 0.2.sp,
            textAlign = if (settings.justifyBibleText) TextAlign.Justify else TextAlign.Start
        ),
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(verseItems) {
                detectTapGestures(
                    onTap = { pos ->
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

                            val attachAnnotation = annotatedString.getStringAnnotations(tag = "ATTACHMENT_CLICK", start = offset, end = offset).firstOrNull()
                            if (attachAnnotation != null) {
                                val vNum = attachAnnotation.item.toIntOrNull()
                                if (vNum != null) {
                                    onAttachmentClick(vNum)
                                    return@detectTapGestures
                                }
                            }

                            val numOnly = annotatedString.getStringAnnotations(tag = "VERSE_NUM_ONLY", start = offset, end = offset).firstOrNull()?.item?.toIntOrNull()
                            val fullVerse = annotatedString.getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset).firstOrNull()?.item?.toIntOrNull()

                            if (settings.verseTapSelectionMode == VerseTapSelectionMode.VERSE_NUMBER_ONLY) {
                                if (numOnly != null) {
                                    onVerseSingleTap(numOnly)
                                } else if (fullVerse != null) {
                                    onVerseLongPress(fullVerse)
                                }
                            } else {
                                val vNum = fullVerse ?: numOnly
                                if (vNum != null) {
                                    onVerseSingleTap(vNum)
                                }
                            }
                        }
                    },
                    onLongPress = { pos ->
                        layoutResult?.let { layout ->
                            val offset = layout.getOffsetForPosition(pos)
                            val verseAnnotation = annotatedString.getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset).firstOrNull()
                            if (verseAnnotation != null) {
                                verseAnnotation.item.toIntOrNull()?.let { onVerseLongPress(it) }
                            }
                        }
                    }
                )
            }
    )
}

@Composable
private fun YouVersionPoetryBlock(
    lineItems: List<PoetryLineItem>,
    versesStateMap: Map<Int, BibleVerse>,
    selectedVerseNumbers: Set<Int>,
    targetVerse: Int?,
    settings: BibleReadingSettings,
    fontFamily: FontFamily,
    textColor: Color,
    isNewTestament: Boolean,
    onVerseSingleTap: (Int) -> Unit,
    onVerseLongPress: (Int) -> Unit,
    onAttachmentClick: (Int) -> Unit,
    onFootnoteClick: (PoetryLineItem, FootnoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * (settings.lineSpacing.multiplier + 0.18f)).sp
    val accentColor = MaterialTheme.colorScheme.primary
    val jesusColor = getJesusWordColor(settings, isNewTestament)

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
            val isSelected = effectiveVNum != null && effectiveVNum in selectedVerseNumbers
            val isTarget = targetVerse != null && effectiveVNum == targetVerse

            val highlightColor = if (isSelected) {
                Color(0xFF93C5FD).copy(alpha = 0.40f)
            } else if (settings.showHighlights && bibleVerse?.highlightColor != null) {
                try { Color(android.graphics.Color.parseColor(bibleVerse.highlightColor)) } catch (e: Exception) { null }
            } else if (isTarget) {
                Color(0xFFFEF08A).copy(alpha = 0.5f)
            } else null

            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

            val annotatedString = remember(line, bibleVerse, isSelected, isTarget, settings, textColor, accentColor, jesusColor) {
                buildAnnotatedString {
                    if (effectiveVNum != null) {
                        pushStringAnnotation(tag = "VERSE_NUM", annotation = "$effectiveVNum")
                    }

                    if (line.verseNumber != null && settings.showVerseNumbers) {
                        pushStringAnnotation(tag = "VERSE_NUM_ONLY", annotation = "$effectiveVNum")
                        withStyle(
                            SpanStyle(
                                color = if (isSelected || isTarget) Color(0xFFD97706) else Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (settings.fontSize.sp * 0.70f).sp,
                                baselineShift = BaselineShift(0.25f)
                            )
                        ) {
                            append("${line.verseNumber} ")
                        }
                        pop()
                    }

                    withStyle(
                        SpanStyle(
                            color = jesusColor ?: textColor,
                            fontSize = fontSizeSp,
                            fontWeight = if (isSelected || isTarget) FontWeight.Medium else FontWeight.Normal,
                            background = highlightColor ?: Color.Transparent,
                            textDecoration = if (isSelected) androidx.compose.ui.text.style.TextDecoration.Underline else null
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
                                fontSize = (settings.fontSize.sp * 0.70f).sp,
                                baselineShift = BaselineShift(0.35f)
                            )
                        ) {
                            append(" ✳")
                        }
                        pop()
                    }

                    // Relatable Indicators
                    if (settings.showFavoritesHint && bibleVerse?.isFavorite == true) {
                        pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "${effectiveVNum ?: 0}")
                        withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                            append(" ⭐")
                        }
                        pop()
                    }
                    if (settings.showBookmarkHint && bibleVerse?.isBookmarked == true) {
                        pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "${effectiveVNum ?: 0}")
                        withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                            append(" 🔖")
                        }
                        pop()
                    }
                    if (settings.showNoteHint && !bibleVerse?.note.isNullOrBlank()) {
                        pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "${effectiveVNum ?: 0}")
                        withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                            append(" 📝")
                        }
                        pop()
                    }

                    if (effectiveVNum != null) {
                        pop()
                    }
                }
            }

            val indentStartPadding = if (settings.showParagraphAndIndents) (line.indent * 16 + 8).dp else 0.dp

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = fontFamily,
                    lineHeight = lineHeightSp,
                    letterSpacing = 0.15.sp,
                    textAlign = if (settings.justifyBibleText) TextAlign.Justify else TextAlign.Start
                ),
                onTextLayout = { layoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indentStartPadding, top = 2.dp, bottom = 2.dp)
                    .pointerInput(line) {
                        detectTapGestures(
                            onTap = { pos ->
                                layoutResult?.let { layout ->
                                    val offset = layout.getOffsetForPosition(pos)
                                    if (line.footnotes.isNotEmpty()) {
                                        val fnAnno = annotatedString.getStringAnnotations(tag = "FOOTNOTE", start = offset, end = offset).firstOrNull()
                                        if (fnAnno != null) {
                                            onFootnoteClick(line, line.footnotes.first())
                                            return@detectTapGestures
                                        }
                                    }

                                    val attachAnnotation = annotatedString.getStringAnnotations(tag = "ATTACHMENT_CLICK", start = offset, end = offset).firstOrNull()
                                    if (attachAnnotation != null) {
                                        val vNum = attachAnnotation.item.toIntOrNull()
                                        if (vNum != null) {
                                            onAttachmentClick(vNum)
                                            return@detectTapGestures
                                        }
                                    }

                                    val numOnly = annotatedString.getStringAnnotations(tag = "VERSE_NUM_ONLY", start = offset, end = offset).firstOrNull()?.item?.toIntOrNull()
                                    val fullVerse = annotatedString.getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset).firstOrNull()?.item?.toIntOrNull()

                                    if (settings.verseTapSelectionMode == VerseTapSelectionMode.VERSE_NUMBER_ONLY) {
                                        if (numOnly != null) {
                                            onVerseSingleTap(numOnly)
                                        } else if (effectiveVNum != null) {
                                            onVerseLongPress(effectiveVNum)
                                        }
                                    } else {
                                        val vNum = fullVerse ?: numOnly ?: effectiveVNum
                                        if (vNum != null) {
                                            onVerseSingleTap(vNum)
                                        }
                                    }
                                }
                            },
                            onLongPress = { pos ->
                                layoutResult?.let { layout ->
                                    if (effectiveVNum != null) {
                                        onVerseLongPress(effectiveVNum)
                                    }
                                }
                            }
                        )
                    }
            )
        }
    }
}

@Composable
fun TranslationPickerDialog(
    selectedTranslation: BibleTranslation,
    onTranslationSelect: (BibleTranslation) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "बाइबल अनुवाद चुनें (Select Translation)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "उपलब्ध सभी बाइबल अनुवाद:",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BibleTranslation.ALL.forEach { translation ->
                    val isSelected = translation.id == selectedTranslation.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onTranslationSelect(translation) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = translation.nameHindi,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = translation.nameEnglish,
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("बंद करें (Close)")
            }
        }
    )
}
