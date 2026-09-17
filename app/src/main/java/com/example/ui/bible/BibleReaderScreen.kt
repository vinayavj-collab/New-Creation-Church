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
import androidx.compose.foundation.Canvas
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.*
import com.example.ui.bible.components.UsfmTextParser
import com.example.ui.bible.components.UsfmTextParserEngine

/**
 * Custom high-performance drawing extension that renders the continuous highlight overlay
 * using drawWithCache to eliminate object allocations, layout jitter, and overdraw.
 */
fun Modifier.continuousHighlightOverlay(
    isReadingPlanMode: Boolean = true,
    rectTop: Float,
    rectBottom: Float,
    hasTopBorder: Boolean,
    hasBottomBorder: Boolean,
    highlightStyle: ReadingPlanHighlightStyle
): Modifier = if (!isReadingPlanMode) this else this.drawWithCache {
    if (!highlightStyle.isVisible || rectBottom <= rectTop || size.width <= 0f) {
        onDrawBehind { }
    } else {
        val rectW = size.width
        val fillColor = try {
            Color(android.graphics.Color.parseColor(highlightStyle.windowFillColorHex))
        } catch (_: Exception) {
            Color(0xFFFDE68A)
        }
        val strokeColor = try {
            Color(android.graphics.Color.parseColor(highlightStyle.strokeColorHex))
        } catch (_: Exception) {
            Color(0xFFD97706)
        }

        val cRadiusPx = highlightStyle.cornerRadiusDp.dp.toPx()
        val strokePx = highlightStyle.borderThicknessDp.dp.toPx()
        val halfStroke = strokePx / 2f

        val topR = if (hasTopBorder) cRadiusPx else 0f
        val botR = if (hasBottomBorder) cRadiusPx else 0f

        val fillPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = rectTop,
                    right = rectW,
                    bottom = rectBottom,
                    topLeftCornerRadius = CornerRadius(topR, topR),
                    topRightCornerRadius = CornerRadius(topR, topR),
                    bottomRightCornerRadius = CornerRadius(botR, botR),
                    bottomLeftCornerRadius = CornerRadius(botR, botR)
                )
            )
        }

        val borderPath = if (strokePx > 0f) {
            val sLeft = halfStroke
            val sRight = rectW - halfStroke
            val sTop = if (hasTopBorder) rectTop + halfStroke else rectTop
            val sBottom = if (hasBottomBorder) rectBottom - halfStroke else rectBottom
            val sTopR = (topR - halfStroke).coerceAtLeast(0f)
            val sBotR = (botR - halfStroke).coerceAtLeast(0f)

            Path().apply {
                when {
                    hasTopBorder && hasBottomBorder -> {
                        addRoundRect(
                            RoundRect(
                                left = sLeft,
                                top = sTop,
                                right = sRight,
                                bottom = sBottom,
                                topLeftCornerRadius = CornerRadius(sTopR, sTopR),
                                topRightCornerRadius = CornerRadius(sTopR, sTopR),
                                bottomRightCornerRadius = CornerRadius(sBotR, sBotR),
                                bottomLeftCornerRadius = CornerRadius(sBotR, sBotR)
                            )
                        )
                    }
                    hasTopBorder && !hasBottomBorder -> {
                        moveTo(sLeft, sBottom)
                        lineTo(sLeft, sTop + sTopR)
                        if (sTopR > 0f) {
                            quadraticBezierTo(sLeft, sTop, sLeft + sTopR, sTop)
                        }
                        lineTo(sRight - sTopR, sTop)
                        if (sTopR > 0f) {
                            quadraticBezierTo(sRight, sTop, sRight, sTop + sTopR)
                        }
                        lineTo(sRight, sBottom)
                    }
                    !hasTopBorder && hasBottomBorder -> {
                        moveTo(sLeft, sTop)
                        lineTo(sLeft, sBottom - sBotR)
                        if (sBotR > 0f) {
                            quadraticBezierTo(sLeft, sBottom, sLeft + sBotR, sBottom)
                        }
                        lineTo(sRight - sBotR, sBottom)
                        if (sBotR > 0f) {
                            quadraticBezierTo(sRight, sBottom, sRight, sBottom - sBotR)
                        }
                        lineTo(sRight, sTop)
                    }
                    else -> {
                        moveTo(sLeft, sTop)
                        lineTo(sLeft, sBottom)
                        moveTo(sRight, sTop)
                        lineTo(sRight, sBottom)
                    }
                }
            }
        } else null

        val fillPaintColor = fillColor.copy(alpha = highlightStyle.alpha)
        val strokePaintColor = strokeColor.copy(alpha = (highlightStyle.alpha * 1.5f).coerceIn(0f, 1f))
        val strokeStyle = Stroke(width = strokePx)

        onDrawBehind {
            drawPath(path = fillPath, color = fillPaintColor)
            if (borderPath != null && strokePx > 0f) {
                drawPath(path = borderPath, color = strokePaintColor, style = strokeStyle)
            }
        }
    }
}

/**
 * Dedicated Custom Canvas Layer for Bible Reader continuous overlay highlights.
 * Performs a single, high-performance draw pass to render background highlights with zero overdraw.
 */
@Composable
fun ContinuousHighlightCanvasLayer(
    modifier: Modifier = Modifier,
    isReadingPlanMode: Boolean = true,
    rectTop: Float = 0f,
    rectBottom: Float = Float.MAX_VALUE,
    hasTopBorder: Boolean = true,
    hasBottomBorder: Boolean = true,
    highlightStyle: ReadingPlanHighlightStyle = ReadingPlanHighlightStyle()
) {
    if (!isReadingPlanMode || !highlightStyle.isVisible) return
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .continuousHighlightOverlay(
                isReadingPlanMode = isReadingPlanMode,
                rectTop = rectTop,
                rectBottom = rectBottom,
                hasTopBorder = hasTopBorder,
                hasBottomBorder = hasBottomBorder,
                highlightStyle = highlightStyle
            )
    ) {
        // Fast cached canvas draw
    }
}

private fun DrawScope.drawContinuousOverlayWindow(
    rectTop: Float,
    rectBottom: Float,
    hasTopBorder: Boolean,
    hasBottomBorder: Boolean,
    highlightStyle: ReadingPlanHighlightStyle
) {
    if (!highlightStyle.isVisible) return
    val rectH = rectBottom - rectTop
    if (rectH <= 0f) return
    val rectW = size.width
    if (rectW <= 0f) return

    val fillColor = try {
        Color(android.graphics.Color.parseColor(highlightStyle.windowFillColorHex))
    } catch (_: Exception) {
        Color(0xFFFDE68A)
    }
    val strokeColor = try {
        Color(android.graphics.Color.parseColor(highlightStyle.strokeColorHex))
    } catch (_: Exception) {
        Color(0xFFD97706)
    }

    val cRadiusPx = highlightStyle.cornerRadiusDp.dp.toPx()
    val strokePx = highlightStyle.borderThicknessDp.dp.toPx()
    val halfStroke = strokePx / 2f

    val topR = if (hasTopBorder) cRadiusPx else 0f
    val botR = if (hasBottomBorder) cRadiusPx else 0f

    // 1. Unified Background Fill
    val fillPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = 0f,
                top = rectTop,
                right = rectW,
                bottom = rectBottom,
                topLeftCornerRadius = CornerRadius(topR, topR),
                topRightCornerRadius = CornerRadius(topR, topR),
                bottomRightCornerRadius = CornerRadius(botR, botR),
                bottomLeftCornerRadius = CornerRadius(botR, botR)
            )
        )
    }
    drawPath(path = fillPath, color = fillColor.copy(alpha = highlightStyle.alpha))

    // 2. Conditional Borders:
    // - Draw the TOP border ONLY above the very first targeted verse in the sequence.
    // - Draw the BOTTOM border ONLY below the very last targeted verse in the sequence.
    // - Draw the LEFT and RIGHT borders continuously across all targeted verses.
    // - Remove Inner Gaps: Ensure there are absolutely NO inner horizontal lines, dividers, or gaps.
    if (strokePx > 0f) {
        val sLeft = halfStroke
        val sRight = rectW - halfStroke
        val sTop = if (hasTopBorder) rectTop + halfStroke else rectTop
        val sBottom = if (hasBottomBorder) rectBottom - halfStroke else rectBottom
        val sTopR = (topR - halfStroke).coerceAtLeast(0f)
        val sBotR = (botR - halfStroke).coerceAtLeast(0f)

        val borderPath = Path().apply {
            when {
                hasTopBorder && hasBottomBorder -> {
                    addRoundRect(
                        RoundRect(
                            left = sLeft,
                            top = sTop,
                            right = sRight,
                            bottom = sBottom,
                            topLeftCornerRadius = CornerRadius(sTopR, sTopR),
                            topRightCornerRadius = CornerRadius(sTopR, sTopR),
                            bottomRightCornerRadius = CornerRadius(sBotR, sBotR),
                            bottomLeftCornerRadius = CornerRadius(sBotR, sBotR)
                        )
                    )
                }
                hasTopBorder && !hasBottomBorder -> {
                    moveTo(sLeft, sBottom)
                    lineTo(sLeft, sTop + sTopR)
                    if (sTopR > 0f) {
                        quadraticBezierTo(sLeft, sTop, sLeft + sTopR, sTop)
                    }
                    lineTo(sRight - sTopR, sTop)
                    if (sTopR > 0f) {
                        quadraticBezierTo(sRight, sTop, sRight, sTop + sTopR)
                    }
                    lineTo(sRight, sBottom)
                }
                !hasTopBorder && hasBottomBorder -> {
                    moveTo(sLeft, sTop)
                    lineTo(sLeft, sBottom - sBotR)
                    if (sBotR > 0f) {
                        quadraticBezierTo(sLeft, sBottom, sLeft + sBotR, sBottom)
                    }
                    lineTo(sRight - sBotR, sBottom)
                    if (sBotR > 0f) {
                        quadraticBezierTo(sRight, sBottom, sRight, sBottom - sBotR)
                    }
                    lineTo(sRight, sTop)
                }
                else -> {
                    moveTo(sLeft, sTop)
                    lineTo(sLeft, sBottom)
                    moveTo(sRight, sTop)
                    lineTo(sRight, sBottom)
                }
            }
        }

        drawPath(
            path = borderPath,
            color = strokeColor.copy(alpha = (highlightStyle.alpha * 1.5f).coerceIn(0f, 1f)),
            style = Stroke(width = strokePx)
        )
    }
}

data class ChapterVerseGroup(
    val title: String,
    val startVerse: Int,
    val endVerse: Int,
    val targetVerse: Int
)

sealed class BibleReaderItem {
    data class Header(val bookName: String, val chapter: Int) : BibleReaderItem()
    data class ChapterOutlineBlock(val groups: List<ChapterVerseGroup>) : BibleReaderItem()
    data object IncompleteWarning : BibleReaderItem()
    data class SectionHeadingBlock(val text: String, val beforeVerse: Int, val endVerse: Int? = null) : BibleReaderItem()
    data class TitleBlock(val text: String, val beforeVerse: Int) : BibleReaderItem()
    data class ProseParagraphBlock(val verses: List<VerseItem>) : BibleReaderItem()
    data class PoetryBlockBlock(val lines: List<PoetryLineItem>) : BibleReaderItem()
    data class SubHeading(val text: String, val beforeVerse: Int, val endVerse: Int? = null) : BibleReaderItem()
    data class Paragraph(val verses: List<BibleVerse>, val startVerse: Int, val endVerse: Int, val isPoetic: Boolean = false) : BibleReaderItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    viewModel: BibleViewModel,
    bookId: Int,
    chapter: Int,
    targetVerse: Int?,
    isReadingPlanMode: Boolean = false,
    highlightStartVerse: Int? = null,
    highlightEndVerse: Int? = null,
    targetBookId: Int? = bookId,
    targetStartChapter: Int? = chapter,
    targetStartVerse: Int? = highlightStartVerse ?: targetVerse,
    targetEndChapter: Int? = chapter,
    targetEndVerse: Int? = highlightEndVerse,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSavedClick: () -> Unit,
    onReadingPlanClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentBook by viewModel.currentBook.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val verses by viewModel.verses.collectAsState()
    val chapterSections by viewModel.chapterSections.collectAsState()
    val structuredBlocks by viewModel.structuredBlocks.collectAsState()
    val selectedTranslation by viewModel.selectedTranslation.collectAsState()
    val readingSettings by viewModel.readingSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isChapterIncomplete by viewModel.isChapterIncomplete.collectAsState()

    val planHighlightStyle by viewModel.planHighlightStyle.collectAsState()
    val planHighlightRange: IntRange? = remember(
        isReadingPlanMode,
        currentBook,
        currentChapter,
        verses,
        targetBookId,
        targetStartChapter,
        targetStartVerse,
        targetEndChapter,
        targetEndVerse,
        highlightStartVerse,
        highlightEndVerse
    ) {
        if (!isReadingPlanMode) {
            null
        } else {
            val effectiveTargetBook = targetBookId ?: bookId
            val effectiveStartChap = targetStartChapter ?: chapter
            val effectiveEndChap = targetEndChapter ?: effectiveStartChap
            val effectiveStartV = targetStartVerse ?: highlightStartVerse ?: targetVerse
            val effectiveEndV = targetEndVerse ?: highlightEndVerse

            // Clean Reset: If the user navigates completely out of the targeted chapters/books, do not draw any overlays.
            if (currentBook.id != effectiveTargetBook) {
                null
            } else if (currentChapter < effectiveStartChap || currentChapter > effectiveEndChap) {
                null
            } else {
                val minChapterVerse = verses.minOfOrNull { it.verseNumber } ?: 1
                val maxChapterVerse = verses.maxOfOrNull { it.verseNumber } ?: 999

                when {
                    // Conditional Overlay Logic (In-Chapter)
                    effectiveStartChap == effectiveEndChap -> {
                        if (currentChapter == effectiveStartChap) {
                            val s = (effectiveStartV ?: minChapterVerse).coerceIn(minChapterVerse, maxChapterVerse)
                            val e = (effectiveEndV ?: maxChapterVerse).coerceIn(minChapterVerse, maxChapterVerse)
                            if (s <= e) s..e else null
                        } else null
                    }
                    // Cross-Chapter Support
                    currentChapter == effectiveStartChap -> {
                        val s = (effectiveStartV ?: minChapterVerse).coerceIn(minChapterVerse, maxChapterVerse)
                        if (s <= maxChapterVerse) s..maxChapterVerse else null
                    }
                    currentChapter == effectiveEndChap -> {
                        val e = (effectiveEndV ?: maxChapterVerse).coerceIn(minChapterVerse, maxChapterVerse)
                        if (minChapterVerse <= e) minChapterVerse..e else null
                    }
                    currentChapter in (effectiveStartChap + 1) until effectiveEndChap -> {
                        minChapterVerse..maxChapterVerse
                    }
                    else -> null
                }
            }
        }
    }
    var showHighlightStylesDialog by remember { mutableStateOf(false) }

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
    var activeCommentaryVerse by remember { mutableStateOf<BibleVerse?>(null) }

    val listState = rememberLazyListState()
    var slideForward by remember { mutableStateOf(true) }
    var currentTargetVerse by remember { mutableStateOf(targetVerse) }
    var initialScrollDoneForChapter by remember(bookId, chapter, targetVerse) { mutableStateOf(false) }

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

    // Clear reading plan highlights when in normal mode or when exiting
    DisposableEffect(isReadingPlanMode) {
        if (!isReadingPlanMode) {
            viewModel.clearPlanHighlight()
        }
        onDispose {
            viewModel.clearPlanHighlight()
        }
    }

    // Reset selection when changing chapter or book
    LaunchedEffect(bookId, chapter, isReadingPlanMode) {
        selectedVerseNumbers = emptySet()
        currentTargetVerse = targetVerse
        if (targetVerse != null) {
            viewModel.audioManager.setCurrentVerseNumber(targetVerse)
        }
        viewModel.openBook(bookId, chapter, targetVerse, isReadingPlan = isReadingPlanMode)
        if (!isReadingPlanMode) {
            viewModel.clearPlanHighlight()
        }
    }

    val bookName = if (selectedTranslation.language == "hi") currentBook.nameHindi else currentBook.nameEnglish
    val shortBookName = remember(currentBook, selectedTranslation.language) {
        BibleHeaderFormatter.getShortBookName(currentBook, selectedTranslation.language == "hi")
    }

    // Formatted reference title for selected verses (e.g. "प्रेरित 21:1-5")
    val selectionReferenceTitle = remember(selectedVerseNumbers, shortBookName, currentChapter) {
        if (selectedVerseNumbers.isEmpty()) ""
        else {
            val sortedList = selectedVerseNumbers.sorted()
            if (sortedList.size == 1) {
                "$shortBookName $currentChapter:${sortedList.first()}"
            } else if (sortedList.last() - sortedList.first() == sortedList.size - 1) {
                "$shortBookName $currentChapter:${sortedList.first()}-${sortedList.last()}"
            } else {
                "$shortBookName $currentChapter:${sortedList.joinToString(",")}"
            }
        }
    }

    val selectedVersesList = remember(selectedVerseNumbers, verses) {
        verses.filter { it.verseNumber in selectedVerseNumbers }.sortedBy { it.verseNumber }
    }

    // Compute distinct verse groups / pericopes for chapter outline and verse range tags
    val allChapterGroups = remember(
        structuredBlocks,
        chapterSections,
        verses,
        readingSettings.showParagraphAndIndents
    ) {
        val groups = mutableListOf<ChapterVerseGroup>()
        if (structuredBlocks.isNotEmpty() && readingSettings.showParagraphAndIndents) {
            val headings = structuredBlocks.filterIsInstance<BibleContentBlock.SectionHeading>()
            val allVersesNumbers = structuredBlocks.flatMap { block ->
                when (block) {
                    is BibleContentBlock.ProseParagraph -> block.verses.mapNotNull { it.verseNumber }
                    is BibleContentBlock.PoetryBlock -> block.lines.mapNotNull { it.verseNumber }
                    else -> emptyList()
                }
            }.distinct().sorted()

            if (headings.isNotEmpty()) {
                for (i in headings.indices) {
                    val currentHeading = headings[i]
                    val startV = currentHeading.beforeVerse
                    val nextBeforeV = if (i + 1 < headings.size) headings[i + 1].beforeVerse else Int.MAX_VALUE
                    val versesInGroup = allVersesNumbers.filter { it >= startV && it < nextBeforeV }
                    val endV = if (versesInGroup.isNotEmpty()) versesInGroup.max() else (if (nextBeforeV != Int.MAX_VALUE) nextBeforeV - 1 else startV)
                    val cleanTitle = UsfmTextParserEngine.cleanRawUsfmTags(currentHeading.text).trim()
                    if (cleanTitle.isNotBlank()) {
                        groups.add(
                            ChapterVerseGroup(
                                title = cleanTitle,
                                startVerse = startV,
                                endVerse = maxOf(startV, endV),
                                targetVerse = startV
                            )
                        )
                    }
                }
            }
        } else {
            if (chapterSections.isNotEmpty()) {
                chapterSections.forEach { sec ->
                    if (sec.heading != null && sec.heading.headingText.isNotBlank() && sec.verses.isNotEmpty()) {
                        val startV = sec.verses.minOf { it.verseNumber }
                        val endV = sec.verses.maxOf { it.verseNumber }
                        val cleanTitle = UsfmTextParserEngine.cleanRawUsfmTags(sec.heading.headingText).trim()
                        if (cleanTitle.isNotBlank()) {
                            groups.add(
                                ChapterVerseGroup(
                                    title = cleanTitle,
                                    startVerse = startV,
                                    endVerse = maxOf(startV, endV),
                                    targetVerse = startV
                                )
                            )
                        }
                    }
                }
            }
        }
        groups
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
        readingSettings.showChapterOutline,
        readingSettings.showHeadingVerseRanges,
        readingSettings.showParagraphAndIndents,
        allChapterGroups
    ) {
        val items = mutableListOf<BibleReaderItem>()
        items.add(BibleReaderItem.Header(bookName, currentChapter))

        // Chapter outline & verse grouping summary card
        if (readingSettings.showChapterOutline && allChapterGroups.isNotEmpty()) {
            items.add(BibleReaderItem.ChapterOutlineBlock(allChapterGroups))
        }

        if (isChapterIncomplete) {
            items.add(BibleReaderItem.IncompleteWarning)
        }

        if (structuredBlocks.isNotEmpty() && readingSettings.showParagraphAndIndents) {
            structuredBlocks.forEach { block ->
                when (block) {
                    is BibleContentBlock.SectionHeading -> {
                        if (readingSettings.showSubheadings) {
                            val matchingGroup = allChapterGroups.find { it.startVerse == block.beforeVerse }
                            items.add(
                                BibleReaderItem.SectionHeadingBlock(
                                    text = block.text,
                                    beforeVerse = block.beforeVerse,
                                    endVerse = matchingGroup?.endVerse
                                )
                            )
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
                val chunkSize = 5
                verses.chunked(chunkSize).forEach { verseChunk ->
                    items.add(
                        BibleReaderItem.Paragraph(
                            verses = verseChunk,
                            startVerse = verseChunk.first().verseNumber,
                            endVerse = verseChunk.last().verseNumber,
                            isPoetic = isPoeticBook && readingSettings.showParagraphAndIndents
                        )
                    )
                }
            } else {
                chapterSections.forEach { section ->
                    if (readingSettings.showSubheadings && section.heading != null && section.heading.headingText.isNotBlank()) {
                        val matchingGroup = allChapterGroups.find { it.startVerse == section.heading.beforeVerse }
                        items.add(
                            BibleReaderItem.SubHeading(
                                text = section.heading.headingText,
                                beforeVerse = section.heading.beforeVerse,
                                endVerse = matchingGroup?.endVerse ?: section.verses.maxOfOrNull { it.verseNumber }
                            )
                        )
                    }
                    if (section.verses.isNotEmpty()) {
                        val chunkSize = if (section.verses.size > 7) 5 else section.verses.size
                        section.verses.chunked(chunkSize).forEach { verseChunk ->
                            items.add(
                                BibleReaderItem.Paragraph(
                                    verses = verseChunk,
                                    startVerse = verseChunk.first().verseNumber,
                                    endVerse = verseChunk.last().verseNumber,
                                    isPoetic = isPoeticBook && readingSettings.showParagraphAndIndents
                                )
                            )
                        }
                    }
                }
            }
        }
        items
    }

    // Dynamic current visible verse tracking for Top Header
    val currentVisibleVerse by remember(listState, readerItems, selectedVerseNumbers, effectiveTargetVerse) {
        derivedStateOf {
            if (selectedVerseNumbers.isNotEmpty()) {
                return@derivedStateOf selectedVerseNumbers.minOrNull()
            }
            if (effectiveTargetVerse != null && effectiveTargetVerse > 0) {
                return@derivedStateOf effectiveTargetVerse
            }
            val visibleIndex = listState.firstVisibleItemIndex
            if (visibleIndex in readerItems.indices) {
                when (val item = readerItems[visibleIndex]) {
                    is BibleReaderItem.Paragraph -> item.startVerse
                    is BibleReaderItem.ProseParagraphBlock -> item.verses.firstOrNull()?.verseNumber
                    is BibleReaderItem.PoetryBlockBlock -> item.lines.firstOrNull()?.verseNumber
                    is BibleReaderItem.SectionHeadingBlock -> item.beforeVerse
                    is BibleReaderItem.SubHeading -> item.beforeVerse
                    is BibleReaderItem.TitleBlock -> item.beforeVerse
                    else -> {
                        readerItems.drop(visibleIndex).mapNotNull {
                            when (it) {
                                is BibleReaderItem.Paragraph -> it.startVerse
                                is BibleReaderItem.ProseParagraphBlock -> it.verses.firstOrNull()?.verseNumber
                                is BibleReaderItem.PoetryBlockBlock -> it.lines.firstOrNull()?.verseNumber
                                else -> null
                            }
                        }.firstOrNull() ?: 1
                    }
                }
            } else {
                1
            }
        }
    }

    // Scroll to target verse or reset to first verse when chapter changes
    LaunchedEffect(currentBook.id, currentChapter, currentTargetVerse, readerItems) {
        if (readerItems.isNotEmpty()) {
            if (currentTargetVerse != null) {
                val targetIndex = readerItems.indexOfFirst { item ->
                    when (item) {
                        is BibleReaderItem.ProseParagraphBlock -> item.verses.any { it.verseNumber == currentTargetVerse }
                        is BibleReaderItem.PoetryBlockBlock -> item.lines.any { it.verseNumber == currentTargetVerse }
                        is BibleReaderItem.Paragraph -> currentTargetVerse!! in item.startVerse..item.endVerse
                        is BibleReaderItem.SectionHeadingBlock -> item.beforeVerse == currentTargetVerse
                        is BibleReaderItem.TitleBlock -> item.beforeVerse == currentTargetVerse
                        is BibleReaderItem.SubHeading -> item.beforeVerse == currentTargetVerse
                        else -> false
                    }
                }
                if (targetIndex >= 0) {
                    listState.scrollToItem(targetIndex)
                }
            } else {
                // When moving between chapters, always open at the first verse (top)
                listState.scrollToItem(0)
            }
        }
    }

    // Follow active audio verse ONLY when audio is actively playing and item is scrolled out of view
    LaunchedEffect(activeAudioVerse) {
        if (viewModel.audioManager.isPlaying.value) {
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
                    val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
                    if (!isVisible) {
                        listState.animateScrollToItem(targetIndex)
                    }
                }
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

    val activeFontFamily = if (readingSettings.useSerifFont) {
        when (readingSettings.fontStyle) {
            BibleFontFamilyType.MODERN_POPPINS -> FontFamily.SansSerif
            BibleFontFamilyType.MONOSPACE_STUDY -> FontFamily.Monospace
            else -> FontFamily.Serif
        }
    } else {
        when (readingSettings.fontStyle) {
            BibleFontFamilyType.SYSTEM_DEFAULT -> FontFamily.Serif
            BibleFontFamilyType.CLASSIC_SERIF -> FontFamily.Serif
            BibleFontFamilyType.MODERN_POPPINS -> FontFamily.SansSerif
            BibleFontFamilyType.ELEGANT_ROZHA -> FontFamily.Serif
            BibleFontFamilyType.TRADITIONAL_BOOK -> FontFamily.Serif
            BibleFontFamilyType.MONOSPACE_STUDY -> FontFamily.Monospace
        }
    }

    var showTopOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Spacious Book, Chapter & Verse selector
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.clickable { showNavigatorModal = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            val headerTitleText = remember(currentBook, currentChapter, selectedVerseNumbers, currentVisibleVerse, currentTargetVerse, selectedTranslation.language) {
                                BibleHeaderFormatter.formatHeaderTitle(
                                    book = currentBook,
                                    chapter = currentChapter,
                                    selectedVerses = selectedVerseNumbers,
                                    visibleVerse = currentVisibleVerse ?: currentTargetVerse ?: 1,
                                    isHindi = selectedTranslation.language == "hi"
                                )
                            }
                            Text(
                                text = headerTitleText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Book, Chapter and Verse",
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
                                BibleTranslation.HIOV.id -> "HIOV"
                                BibleTranslation.ENGLISH_ESV.id -> "ESV"
                                BibleTranslation.PARALLEL_HI_EN.id -> "HI+ESV"
                                else -> selectedTranslation.id
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
                                text = { Text("ऑडियो सेटिंग्स एवं कस्टमाइजेशन (Audio Settings)") },
                                leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showTopOverflowMenu = false
                                    if (!readingSettings.showAudioPlayer) {
                                        viewModel.toggleAudioPlayer(true)
                                    }
                                    showSettingsDialog = true
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
                                text = { Text("प्लान हाइलाइट शैलियाँ (Highlight Styles)") },
                                leadingIcon = { Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showTopOverflowMenu = false
                                    showHighlightStylesDialog = true
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
                        verses = verses,
                        activeTargetVerse = effectiveTargetVerse
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
                                                    val lastHighlighted = selectedVersesList.firstOrNull()?.verseNumber
                                                    if (lastHighlighted != null) {
                                                        currentTargetVerse = lastHighlighted
                                                        viewModel.audioManager.setCurrentVerseNumber(lastHighlighted)
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

                                // 6. Send to Study Note (Auto-Embed into recent note)
                                YouVersionActionItem(
                                    icon = Icons.Default.PostAdd,
                                    label = "नोट में भेजें",
                                    tint = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                val dao = com.example.data.bible.local.BibleDatabase.getInstance(context).bibleDao()
                                                val notesRepo = com.example.data.bible.repository.StudyNotesRepository(dao)
                                                val combinedText = selectedVersesList.joinToString(" ") { v ->
                                                    if (selectedVersesList.size > 1) "(${v.verseNumber}) ${v.text}" else v.text
                                                }
                                                val (noteId, noteTitle) = notesRepo.appendScriptureToRecentNote(
                                                    referenceLabel = "$selectionReferenceTitle (${selectedTranslation.id})",
                                                    scriptureText = combinedText
                                                )
                                                Toast.makeText(context, "📖 स्टडी नोट '$noteTitle' में जोड़ा गया!", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "नोट में जोड़ने में विफल", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        selectedVerseNumbers = emptySet()
                                    }
                                )

                                // 7. Verse Note Editor
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
                        text = "इस अध्याय के लिए कोई पद उपलब्ध नहीं हैं।",
                        style = MaterialTheme.typography.bodyMedium.copy(color = canvasTextColor.copy(alpha = 0.8f)),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { showNavigatorModal = true }) {
                        Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("नेविगेटर से अध्याय चुनें")
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
                    val targetVersesByItem = remember(readerItems, planHighlightRange) {
                        readerItems.map { item ->
                            when (item) {
                                is BibleReaderItem.ProseParagraphBlock -> item.verses.mapNotNull { it.verseNumber }.filter { planHighlightRange != null && it in planHighlightRange }
                                is BibleReaderItem.PoetryBlockBlock -> item.lines.mapNotNull { it.verseNumber }.filter { planHighlightRange != null && it in planHighlightRange }
                                is BibleReaderItem.Paragraph -> item.verses.map { it.verseNumber }.filter { planHighlightRange != null && it in planHighlightRange }
                                else -> emptyList()
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 60.dp)
                    ) {
                        itemsIndexed(
                            readerItems,
                            key = { index, item ->
                                when (item) {
                                    is BibleReaderItem.Header -> "hdr_${item.bookName}_${item.chapter}"
                                    is BibleReaderItem.ChapterOutlineBlock -> "outline_${index}"
                                    is BibleReaderItem.IncompleteWarning -> "warning"
                                    is BibleReaderItem.SectionHeadingBlock -> "section_${item.beforeVerse}_${item.text.hashCode()}"
                                    is BibleReaderItem.TitleBlock -> "title_${item.beforeVerse}_${item.text.hashCode()}"
                                    is BibleReaderItem.SubHeading -> "sub_${item.beforeVerse}_${item.text.hashCode()}"
                                    is BibleReaderItem.ProseParagraphBlock -> "prose_${index}"
                                    is BibleReaderItem.PoetryBlockBlock -> "poetry_${index}"
                                    is BibleReaderItem.Paragraph -> "para_${item.startVerse}_${item.endVerse}"
                                }
                            }
                        ) { index, item ->
                            val myTargets = targetVersesByItem.getOrElse(index) { emptyList() }
                            val isTargeted = myTargets.isNotEmpty()
                            val containsFirstTarget = planHighlightRange != null && myTargets.contains(planHighlightRange.first)
                            val containsLastTarget = planHighlightRange != null && myTargets.contains(planHighlightRange.last)
                            val hasEarlierTargetedItem = (0 until index).any { targetVersesByItem.getOrElse(it) { emptyList() }.isNotEmpty() }
                            val hasLaterTargetedItem = (index + 1 until targetVersesByItem.size).any { targetVersesByItem.getOrElse(it) { emptyList() }.isNotEmpty() }

                            val hasTopBorder = containsFirstTarget || !hasEarlierTargetedItem
                            val hasBottomBorder = containsLastTarget || !hasLaterTargetedItem
                            val isContinuingToNext = isTargeted && !hasBottomBorder
                            val itemBottomPadding = if (isContinuingToNext) 0.dp else 16.dp

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
                                                fontFamily = FontFamily.Serif,
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

                                is BibleReaderItem.ChapterOutlineBlock -> {
                                    ChapterOutlineCard(
                                        groups = item.groups,
                                        isHindi = selectedTranslation.language == "hi",
                                        onGroupClick = { group ->
                                            coroutineScope.launch {
                                                currentTargetVerse = group.targetVerse
                                                val targetIdx = readerItems.indexOfFirst { rItem ->
                                                    when (rItem) {
                                                        is BibleReaderItem.SectionHeadingBlock -> rItem.beforeVerse == group.startVerse
                                                        is BibleReaderItem.SubHeading -> rItem.beforeVerse == group.startVerse
                                                        is BibleReaderItem.ProseParagraphBlock -> rItem.verses.any { it.verseNumber == group.startVerse }
                                                        is BibleReaderItem.PoetryBlockBlock -> rItem.lines.any { it.verseNumber == group.startVerse }
                                                        is BibleReaderItem.Paragraph -> group.startVerse in rItem.startVerse..rItem.endVerse
                                                        else -> false
                                                    }
                                                }
                                                if (targetIdx >= 0) {
                                                    listState.animateScrollToItem(targetIdx)
                                                }
                                            }
                                        },
                                        onDismiss = { viewModel.toggleChapterOutline(false) }
                                    )
                                }

                                is BibleReaderItem.SectionHeadingBlock -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = UsfmTextParserEngine.cleanRawUsfmTags(item.text),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    fontFamily = FontFamily.Serif,
                                                    color = customSubHeadingColor ?: MaterialTheme.colorScheme.primary,
                                                    letterSpacing = 0.2.sp
                                                ),
                                                modifier = Modifier.weight(1f, fill = false)
                                            )

                                            if (readingSettings.showHeadingVerseRanges && item.endVerse != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                                                    modifier = Modifier.padding(start = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (item.beforeVerse == item.endVerse) {
                                                            if (selectedTranslation.language == "hi") "वचन ${item.beforeVerse}" else "v. ${item.beforeVerse}"
                                                        } else {
                                                            if (selectedTranslation.language == "hi") "वचन ${item.beforeVerse}–${item.endVerse}" else "v. ${item.beforeVerse}–${item.endVerse}"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                is BibleReaderItem.TitleBlock -> {
                                    Text(
                                        text = UsfmTextParserEngine.cleanRawUsfmTags(item.text),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            fontFamily = FontFamily.Serif,
                                            color = canvasTextColor.copy(alpha = 0.85f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 16.dp)
                                    )
                                }

                                 is BibleReaderItem.ProseParagraphBlock -> {
                                    val versesMap = remember(verses) { verses.associateBy { it.verseNumber } }
                                    YouVersionProseParagraph(
                                        verseItems = item.verses,
                                        versesStateMap = versesMap,
                                        selectedVerseNumbers = selectedVerseNumbers,
                                        targetVerse = effectiveTargetVerse,
                                        planHighlightRange = planHighlightRange,
                                        highlightStyle = planHighlightStyle,
                                        hasTopBorder = hasTopBorder,
                                        hasBottomBorder = hasBottomBorder,
                                        isContinuingToNext = isContinuingToNext,
                                        settings = readingSettings,
                                        fontFamily = activeFontFamily,
                                        textColor = canvasTextColor,
                                        isNewTestament = currentBook.id >= 40,
                                        onVerseSingleTap = { vNum ->
                                            currentTargetVerse = vNum
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
                                        onCommentaryClick = { vNum ->
                                            activeCommentaryVerse = versesMap[vNum]
                                        },
                                        modifier = Modifier.padding(bottom = itemBottomPadding)
                                    )
                                }

                                is BibleReaderItem.PoetryBlockBlock -> {
                                    val versesMap = remember(verses) { verses.associateBy { it.verseNumber } }
                                    YouVersionPoetryBlock(
                                        lineItems = item.lines,
                                        versesStateMap = versesMap,
                                        selectedVerseNumbers = selectedVerseNumbers,
                                        targetVerse = effectiveTargetVerse,
                                        planHighlightRange = planHighlightRange,
                                        highlightStyle = planHighlightStyle,
                                        hasTopBorder = hasTopBorder,
                                        hasBottomBorder = hasBottomBorder,
                                        isContinuingToNext = isContinuingToNext,
                                        settings = readingSettings,
                                        fontFamily = activeFontFamily,
                                        textColor = canvasTextColor,
                                        isNewTestament = currentBook.id >= 40,
                                        onVerseSingleTap = { vNum ->
                                            currentTargetVerse = vNum
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
                                        onCommentaryClick = { vNum ->
                                            activeCommentaryVerse = versesMap[vNum]
                                        },
                                        modifier = Modifier.padding(bottom = itemBottomPadding)
                                    )
                                }

                                is BibleReaderItem.SubHeading -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = UsfmTextParserEngine.cleanRawUsfmTags(item.text),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    fontFamily = FontFamily.Serif,
                                                    color = customSubHeadingColor ?: MaterialTheme.colorScheme.primary,
                                                    letterSpacing = 0.2.sp
                                                ),
                                                modifier = Modifier.weight(1f, fill = false)
                                            )

                                            if (readingSettings.showHeadingVerseRanges && item.endVerse != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                                                    modifier = Modifier.padding(start = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (item.beforeVerse == item.endVerse) {
                                                            if (selectedTranslation.language == "hi") "वचन ${item.beforeVerse}" else "v. ${item.beforeVerse}"
                                                        } else {
                                                            if (selectedTranslation.language == "hi") "वचन ${item.beforeVerse}–${item.endVerse}" else "v. ${item.beforeVerse}–${item.endVerse}"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                is BibleReaderItem.Paragraph -> {
                                    val versesMap = remember(verses) { verses.associateBy { it.verseNumber } }
                                    if (item.isPoetic) {
                                        YouVersionPoeticVerseParagraph(
                                            verses = item.verses,
                                            selectedVerseNumbers = selectedVerseNumbers,
                                            targetVerse = effectiveTargetVerse,
                                            planHighlightRange = planHighlightRange,
                                            highlightStyle = planHighlightStyle,
                                            hasTopBorder = hasTopBorder,
                                            hasBottomBorder = hasBottomBorder,
                                            isContinuingToNext = isContinuingToNext,
                                            settings = readingSettings,
                                            fontFamily = activeFontFamily,
                                            textColor = canvasTextColor,
                                            isNewTestament = currentBook.id >= 40,
                                            onVerseSingleTap = { vNum ->
                                                currentTargetVerse = vNum
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
                                            onCommentaryClick = { vNum ->
                                                activeCommentaryVerse = versesMap[vNum] ?: item.verses.find { it.verseNumber == vNum }
                                            },
                                            modifier = Modifier.padding(bottom = itemBottomPadding)
                                        )
                                    } else {
                                        YouVersionStandardParagraph(
                                            verses = item.verses,
                                            selectedVerseNumbers = selectedVerseNumbers,
                                            targetVerse = effectiveTargetVerse,
                                            planHighlightRange = planHighlightRange,
                                            highlightStyle = planHighlightStyle,
                                            hasTopBorder = hasTopBorder,
                                            hasBottomBorder = hasBottomBorder,
                                            isContinuingToNext = isContinuingToNext,
                                            settings = readingSettings,
                                            fontFamily = activeFontFamily,
                                            textColor = canvasTextColor,
                                            isNewTestament = currentBook.id >= 40,
                                            onVerseSingleTap = { vNum ->
                                                currentTargetVerse = vNum
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
                                            onCommentaryClick = { vNum ->
                                                activeCommentaryVerse = versesMap[vNum] ?: item.verses.find { it.verseNumber == vNum }
                                            },
                                            modifier = Modifier.padding(bottom = itemBottomPadding)
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

    // Quick Reading Options Sheet
    if (showQuickFontSheet) {
        YouVersionQuickFontSheet(
            settings = readingSettings,
            onFontSizeChange = { viewModel.updateFontSize(it) },
            onLineSpacingChange = { viewModel.updateLineSpacing(it) },
            onFontStyleChange = { viewModel.updateFontStyle(it) },
            onVerseNumberSizeChange = { viewModel.updateVerseNumberSize(it) },
            onToggleChapterOutline = { viewModel.toggleChapterOutline(it) },
            onToggleHeadingVerseRanges = { viewModel.toggleHeadingVerseRanges(it) },
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
                if (verse != null) {
                    viewModel.audioManager.setCurrentVerseNumber(verse)
                }
                viewModel.openBook(book.id, ch, verse, isReadingPlan = false)
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
            onVerseNumberSizeChange = { viewModel.updateVerseNumberSize(it) },
            onShowSubheadingsChange = { viewModel.toggleSubheadings(it) },
            onShowChapterOutlineChange = { viewModel.toggleChapterOutline(it) },
            onShowHeadingVerseRangesChange = { viewModel.toggleHeadingVerseRanges(it) },
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
            onTtsSmartStartChange = { viewModel.toggleTtsSmartStart(it) },
            onTtsBackgroundPlayChange = { viewModel.toggleTtsBackgroundPlay(it) },
            onTtsSleepTimerMinutesChange = { viewModel.updateTtsSleepTimerMinutes(it) },
            onTtsSleepChapterCountChange = { viewModel.updateTtsSleepChapterCount(it) },
            onEnableDevotionalBgmChange = { viewModel.toggleDevotionalBgm(it) },
            onTtsVolumeChange = { viewModel.updateTtsVolume(it) },
            onBgmVolumeChange = { viewModel.updateBgmVolume(it) },
            onSelectedBgmTrackChange = { viewModel.updateSelectedBgmTrack(it) },
            onCustomBgmFileSelected = { uri, fileName -> viewModel.updateCustomBgmFile(uri, fileName) },
            onCustomBgmUrlSubmitted = { url -> viewModel.updateCustomBgmUrl(url) },
            onScreenTimeoutChange = { viewModel.setScreenTimeout(it) },
            onRememberPositionChange = { viewModel.toggleRememberPosition(it) },
            onResetToDefault = { viewModel.resetReadingSettings() },
            onThemeChange = { viewModel.updateTheme(it) },
            onTranslationChange = { viewModel.selectTranslation(it) },
            onTranslationToggleBehaviorChange = { viewModel.updateTranslationToggleBehavior(it) },
            onVerseTapSelectionModeChange = { viewModel.updateVerseTapSelectionMode(it) },
            onOpenReadingPlan = onReadingPlanClick,
            onExternalFolderSelected = { viewModel.setExternalFolderPath(it) },
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

    if (showHighlightStylesDialog) {
        HighlightStylesDialog(
            currentStyle = planHighlightStyle,
            onDismissRequest = { showHighlightStylesDialog = false },
            onSaveStyle = { newStyle ->
                viewModel.updatePlanHighlightStyle(newStyle)
            }
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

    // Commentary Footnote Modal Bottom Sheet
    if (activeCommentaryVerse != null) {
        val verse = activeCommentaryVerse!!
        val verseRefLabel = "$bookName $currentChapter:${verse.verseNumber}"
        val commText = verse.commentaryText ?: "कोई टीका/टिप्पणी उपलब्ध नहीं है।"

        ModalBottomSheet(
            onDismissRequest = { activeCommentaryVerse = null },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "[*]",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "बाइबिल टीका / Bible Commentary",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = verseRefLabel,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    IconButton(onClick = { activeCommentaryVerse = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Italicized Verse Text Box
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“${verse.text}”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "व्याख्या एवं टिप्पणी (Explanation & Notes)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val cleanCommentaryText = UsfmTextParserEngine.cleanRawUsfmTags(
                    commText.replace(Regex("<a href='.*?'>"), "").replace("</a>", "")
                )

                Text(
                    text = cleanCommentaryText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))
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

private val DEFAULT_RED_LETTER_COLOR = Color(0xFF991B1B) // Deep dark crimson red for Jesus' words in printed bibles

private fun formatVerseNumberText(vNum: Int, sizeSetting: VerseNumberSize): String {
    return if (sizeSetting == VerseNumberSize.NORMAL) {
        "$vNum "
    } else {
        val superscripts = mapOf(
            '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
            '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹'
        )
        val digits = vNum.toString().map { superscripts[it] ?: it }.joinToString("")
        "⁽$digits⁾ "
    }
}

private fun getJesusWordColor(settings: BibleReadingSettings, isNewTestament: Boolean): Color? {
    if (!isNewTestament || !settings.showJesusWordsInRed) return null
    return try {
        Color(android.graphics.Color.parseColor(settings.jesusWordsColorHex))
    } catch (e: Exception) {
        DEFAULT_RED_LETTER_COLOR
    }
}

private fun AnnotatedString.Builder.appendVerseContent(
    text: String,
    baseTextColor: Color,
    jesusColor: Color?,
    isNewTestament: Boolean,
    fontSizeSp: androidx.compose.ui.unit.TextUnit,
    isSelected: Boolean,
    isTarget: Boolean,
    highlightColor: Color?,
    settings: BibleReadingSettings
) {
    val cleanedText = UsfmTextParserEngine.cleanRawUsfmTags(text)
    val effectiveRedColor = if (settings.showJesusWordsInRed && isNewTestament) {
        jesusColor ?: DEFAULT_RED_LETTER_COLOR
    } else {
        null
    }

    if (effectiveRedColor != null) {
        val quoteRegex = Regex("""([“"‘'«][^”"’'»]+?[”"’'»])""")
        var lastIndex = 0
        val matches = quoteRegex.findAll(cleanedText).toList()

        if (matches.isNotEmpty()) {
            for (match in matches) {
                if (match.range.first > lastIndex) {
                    val preText = cleanedText.substring(lastIndex, match.range.first)
                    withStyle(
                        SpanStyle(
                            color = baseTextColor,
                            fontSize = fontSizeSp,
                            fontWeight = if (isSelected || isTarget) FontWeight.Medium else FontWeight.Normal,
                            background = highlightColor ?: Color.Transparent,
                            textDecoration = if (isSelected) androidx.compose.ui.text.style.TextDecoration.Underline else null
                        )
                    ) {
                        append(preText)
                    }
                }

                withStyle(
                    SpanStyle(
                        color = effectiveRedColor,
                        fontSize = fontSizeSp,
                        fontWeight = if (isSelected || isTarget) FontWeight.Medium else FontWeight.Normal,
                        background = highlightColor ?: Color.Transparent,
                        textDecoration = if (isSelected) androidx.compose.ui.text.style.TextDecoration.Underline else null
                    )
                ) {
                    append(match.value)
                }

                lastIndex = match.range.last + 1
            }

            if (lastIndex < cleanedText.length) {
                val postText = cleanedText.substring(lastIndex)
                withStyle(
                    SpanStyle(
                        color = baseTextColor,
                        fontSize = fontSizeSp,
                        fontWeight = if (isSelected || isTarget) FontWeight.Medium else FontWeight.Normal,
                        background = highlightColor ?: Color.Transparent,
                        textDecoration = if (isSelected) androidx.compose.ui.text.style.TextDecoration.Underline else null
                    )
                ) {
                    append(postText)
                }
            }
        } else {
            withStyle(
                SpanStyle(
                    color = baseTextColor,
                    fontSize = fontSizeSp,
                    fontWeight = if (isSelected || isTarget) FontWeight.Medium else FontWeight.Normal,
                    background = highlightColor ?: Color.Transparent,
                    textDecoration = if (isSelected) androidx.compose.ui.text.style.TextDecoration.Underline else null
                )
            ) {
                append(cleanedText)
            }
        }
    } else {
        withStyle(
            SpanStyle(
                color = baseTextColor,
                fontSize = fontSizeSp,
                fontWeight = if (isSelected || isTarget) FontWeight.Medium else FontWeight.Normal,
                background = highlightColor ?: Color.Transparent,
                textDecoration = if (isSelected) androidx.compose.ui.text.style.TextDecoration.Underline else null
            )
        ) {
            append(cleanedText)
        }
    }
}

@Composable
private fun YouVersionStandardParagraph(
    verses: List<BibleVerse>,
    selectedVerseNumbers: Set<Int>,
    targetVerse: Int?,
    planHighlightRange: IntRange? = null,
    highlightStyle: ReadingPlanHighlightStyle = ReadingPlanHighlightStyle(),
    hasTopBorder: Boolean = true,
    hasBottomBorder: Boolean = true,
    isContinuingToNext: Boolean = false,
    settings: BibleReadingSettings,
    fontFamily: FontFamily,
    textColor: Color,
    isNewTestament: Boolean,
    onVerseSingleTap: (Int) -> Unit,
    onVerseLongPress: (Int) -> Unit,
    onAttachmentClick: (Int) -> Unit,
    onCommentaryClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val jesusColor = getJesusWordColor(settings, isNewTestament)
    val accentColor = MaterialTheme.colorScheme.primary
    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * settings.lineSpacing.multiplier).sp
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedString = remember(verses, selectedVerseNumbers, targetVerse, planHighlightRange, highlightStyle, settings, textColor, jesusColor, accentColor) {
        buildAnnotatedString {
            verses.forEachIndexed { index, verse ->
                pushStringAnnotation(tag = "VERSE_NUM", annotation = "${verse.verseNumber}")

                val isSelected = verse.verseNumber in selectedVerseNumbers
                val isTarget = targetVerse != null && verse.verseNumber == targetVerse

                // Verse number rendering (supports both small superscript and normal size)
                if (settings.showVerseNumbers) {
                    pushStringAnnotation(tag = "VERSE_NUM_ONLY", annotation = "${verse.verseNumber}")
                    val verseNumColor = if (isSelected || isTarget) Color(0xFFD97706) else Color(0xFF8E8E93)
                    val isNormalSize = settings.verseNumberSize == VerseNumberSize.NORMAL
                    withStyle(
                        SpanStyle(
                            color = verseNumColor,
                            fontWeight = if (isNormalSize) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isNormalSize) (fontSizeSp.value * 0.90f).sp else (fontSizeSp.value * 0.62f).sp,
                            baselineShift = if (isNormalSize) BaselineShift.None else BaselineShift(0.35f)
                        )
                    ) {
                        append(formatVerseNumberText(verse.verseNumber, settings.verseNumberSize))
                    }
                    pop()
                }

                // Append commentary symbol [*] if commentary is available for this verse
                if (!verse.commentaryText.isNullOrBlank()) {
                    pushStringAnnotation(tag = "COMMENTARY_CLICK", annotation = "${verse.verseNumber}")
                    withStyle(
                        SpanStyle(
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = (fontSizeSp.value * 0.70f).sp,
                            baselineShift = BaselineShift(0.35f)
                        )
                    ) {
                        append(" [*]")
                    }
                    pop()
                }

                // Background highlight or selection effect
                val highlightColor = if (isSelected) {
                    Color(0xFF93C5FD).copy(alpha = 0.40f)
                } else if (settings.showHighlights && verse.highlightColor != null) {
                    try { Color(android.graphics.Color.parseColor(verse.highlightColor)) } catch (e: Exception) { null }
                } else if (isTarget) {
                    Color(0xFFFEF08A).copy(alpha = 0.5f)
                } else null

                appendVerseContent(
                    text = verse.text,
                    baseTextColor = textColor,
                    jesusColor = jesusColor,
                    isNewTestament = isNewTestament,
                    fontSizeSp = fontSizeSp,
                    isSelected = isSelected,
                    isTarget = isTarget,
                    highlightColor = highlightColor,
                    settings = settings
                )

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

    val planOverlayModifier = if (planHighlightRange != null && highlightStyle.isVisible) {
        Modifier.drawWithCache {
            val layout = layoutResult
            if (layout == null) {
                onDrawBehind { }
            } else {
                val annotations = annotatedString.getStringAnnotations(tag = "VERSE_NUM", start = 0, end = annotatedString.length)
                var firstStart = Int.MAX_VALUE
                var lastEnd = -1
                annotations.forEach { ann ->
                    val vNum = ann.item.toIntOrNull()
                    if (vNum != null && vNum in planHighlightRange) {
                        if (ann.start < firstStart) firstStart = ann.start
                        if (ann.end > lastEnd) lastEnd = ann.end
                    }
                }
                if (firstStart >= lastEnd || firstStart == Int.MAX_VALUE) {
                    onDrawBehind { }
                } else {
                    val paddingPx = 3.dp.toPx()
                    val rectTop = if (hasTopBorder) {
                        val sLine = layout.getLineForOffset(firstStart.coerceIn(0, (annotatedString.length - 1).coerceAtLeast(0)))
                        (layout.getLineTop(sLine) - paddingPx).coerceAtLeast(0f)
                    } else 0f

                    val rectBottom = if (hasBottomBorder) {
                        val eLine = layout.getLineForOffset((lastEnd - 1).coerceIn(0, (annotatedString.length - 1).coerceAtLeast(0)))
                        (layout.getLineBottom(eLine) + paddingPx).coerceAtMost(size.height)
                    } else size.height

                    val rectW = size.width
                    val fillColor = try {
                        Color(android.graphics.Color.parseColor(highlightStyle.windowFillColorHex))
                    } catch (_: Exception) {
                        Color(0xFFFDE68A)
                    }
                    val strokeColor = try {
                        Color(android.graphics.Color.parseColor(highlightStyle.strokeColorHex))
                    } catch (_: Exception) {
                        Color(0xFFD97706)
                    }

                    val cRadiusPx = highlightStyle.cornerRadiusDp.dp.toPx()
                    val strokePx = highlightStyle.borderThicknessDp.dp.toPx()
                    val halfStroke = strokePx / 2f

                    val topR = if (hasTopBorder) cRadiusPx else 0f
                    val botR = if (hasBottomBorder) cRadiusPx else 0f

                    val fillPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = 0f,
                                top = rectTop,
                                right = rectW,
                                bottom = rectBottom,
                                topLeftCornerRadius = CornerRadius(topR, topR),
                                topRightCornerRadius = CornerRadius(topR, topR),
                                bottomRightCornerRadius = CornerRadius(botR, botR),
                                bottomLeftCornerRadius = CornerRadius(botR, botR)
                            )
                        )
                    }

                    val borderPath = if (strokePx > 0f) {
                        val sLeft = halfStroke
                        val sRight = rectW - halfStroke
                        val sTop = if (hasTopBorder) rectTop + halfStroke else rectTop
                        val sBottom = if (hasBottomBorder) rectBottom - halfStroke else rectBottom
                        val sTopR = (topR - halfStroke).coerceAtLeast(0f)
                        val sBotR = (botR - halfStroke).coerceAtLeast(0f)

                        Path().apply {
                            when {
                                hasTopBorder && hasBottomBorder -> {
                                    addRoundRect(
                                        RoundRect(
                                            left = sLeft,
                                            top = sTop,
                                            right = sRight,
                                            bottom = sBottom,
                                            topLeftCornerRadius = CornerRadius(sTopR, sTopR),
                                            topRightCornerRadius = CornerRadius(sTopR, sTopR),
                                            bottomRightCornerRadius = CornerRadius(sBotR, sBotR),
                                            bottomLeftCornerRadius = CornerRadius(sBotR, sBotR)
                                        )
                                    )
                                }
                                hasTopBorder && !hasBottomBorder -> {
                                    moveTo(sLeft, sBottom)
                                    lineTo(sLeft, sTop + sTopR)
                                    if (sTopR > 0f) {
                                        quadraticBezierTo(sLeft, sTop, sLeft + sTopR, sTop)
                                    }
                                    lineTo(sRight - sTopR, sTop)
                                    if (sTopR > 0f) {
                                        quadraticBezierTo(sRight, sTop, sRight, sTop + sTopR)
                                    }
                                    lineTo(sRight, sBottom)
                                }
                                !hasTopBorder && hasBottomBorder -> {
                                    moveTo(sLeft, sTop)
                                    lineTo(sLeft, sBottom - sBotR)
                                    if (sBotR > 0f) {
                                        quadraticBezierTo(sLeft, sBottom, sLeft + sBotR, sBottom)
                                    }
                                    lineTo(sRight - sBotR, sBottom)
                                    if (sBotR > 0f) {
                                        quadraticBezierTo(sRight, sBottom, sRight, sBottom - sBotR)
                                    }
                                    lineTo(sRight, sTop)
                                }
                                else -> {
                                    moveTo(sLeft, sTop)
                                    lineTo(sLeft, sBottom)
                                    moveTo(sRight, sTop)
                                    lineTo(sRight, sBottom)
                                }
                            }
                        }
                    } else null

                    val fillPaintColor = fillColor.copy(alpha = highlightStyle.alpha)
                    val strokePaintColor = strokeColor.copy(alpha = (highlightStyle.alpha * 1.5f).coerceIn(0f, 1f))
                    val strokeStyle = Stroke(width = strokePx)

                    onDrawBehind {
                        drawPath(path = fillPath, color = fillPaintColor)
                        if (borderPath != null && strokePx > 0f) {
                            drawPath(path = borderPath, color = strokePaintColor, style = strokeStyle)
                        }
                    }
                }
            }
        }
    } else Modifier

    val internalBottomPadding = if (isContinuingToNext) 14.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(planOverlayModifier)
            .padding(bottom = internalBottomPadding)
    ) {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = fontFamily,
                lineHeight = lineHeightSp,
                letterSpacing = 0.25.sp,
                textAlign = if (settings.justifyBibleText) TextAlign.Justify else TextAlign.Start
            ),
            onTextLayout = { layoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(verses) {
                    detectTapGestures(
                        onTap = { pos ->
                            layoutResult?.let { layout ->
                                val offset = layout.getOffsetForPosition(pos)
                                val commAnnotation = annotatedString.getStringAnnotations(tag = "COMMENTARY_CLICK", start = offset, end = offset).firstOrNull()
                                if (commAnnotation != null) {
                                    val vNum = commAnnotation.item.toIntOrNull()
                                    if (vNum != null) {
                                        onCommentaryClick(vNum)
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
}

@Composable
private fun YouVersionPoeticVerseParagraph(
    verses: List<BibleVerse>,
    selectedVerseNumbers: Set<Int>,
    targetVerse: Int?,
    planHighlightRange: IntRange? = null,
    highlightStyle: ReadingPlanHighlightStyle = ReadingPlanHighlightStyle(),
    hasTopBorder: Boolean = true,
    hasBottomBorder: Boolean = true,
    isContinuingToNext: Boolean = false,
    settings: BibleReadingSettings,
    fontFamily: FontFamily,
    textColor: Color,
    isNewTestament: Boolean,
    onVerseSingleTap: (Int) -> Unit,
    onVerseLongPress: (Int) -> Unit,
    onAttachmentClick: (Int) -> Unit,
    onCommentaryClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val jesusColor = getJesusWordColor(settings, isNewTestament)
    val accentColor = MaterialTheme.colorScheme.primary
    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * (settings.lineSpacing.multiplier + 0.15f)).sp

    val hasTargetLines = planHighlightRange != null && verses.any { v ->
        v.verseNumber in planHighlightRange
    }

    val planOverlayModifier = if (hasTargetLines && highlightStyle.isVisible) {
        Modifier.drawWithCache {
            val rectTop = 0f
            val rectBottom = size.height
            val rectW = size.width

            val fillColor = try {
                Color(android.graphics.Color.parseColor(highlightStyle.windowFillColorHex))
            } catch (_: Exception) {
                Color(0xFFFDE68A)
            }
            val strokeColor = try {
                Color(android.graphics.Color.parseColor(highlightStyle.strokeColorHex))
            } catch (_: Exception) {
                Color(0xFFD97706)
            }

            val cRadiusPx = highlightStyle.cornerRadiusDp.dp.toPx()
            val strokePx = highlightStyle.borderThicknessDp.dp.toPx()
            val halfStroke = strokePx / 2f

            val topR = if (hasTopBorder) cRadiusPx else 0f
            val botR = if (hasBottomBorder) cRadiusPx else 0f

            val fillPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = rectTop,
                        right = rectW,
                        bottom = rectBottom,
                        topLeftCornerRadius = CornerRadius(topR, topR),
                        topRightCornerRadius = CornerRadius(topR, topR),
                        bottomRightCornerRadius = CornerRadius(botR, botR),
                        bottomLeftCornerRadius = CornerRadius(botR, botR)
                    )
                )
            }

            val borderPath = if (strokePx > 0f) {
                val sLeft = halfStroke
                val sRight = rectW - halfStroke
                val sTop = if (hasTopBorder) rectTop + halfStroke else rectTop
                val sBottom = if (hasBottomBorder) rectBottom - halfStroke else rectBottom
                val sTopR = (topR - halfStroke).coerceAtLeast(0f)
                val sBotR = (botR - halfStroke).coerceAtLeast(0f)

                Path().apply {
                    when {
                        hasTopBorder && hasBottomBorder -> {
                            addRoundRect(
                                RoundRect(
                                    left = sLeft,
                                    top = sTop,
                                    right = sRight,
                                    bottom = sBottom,
                                    topLeftCornerRadius = CornerRadius(sTopR, sTopR),
                                    topRightCornerRadius = CornerRadius(sTopR, sTopR),
                                    bottomRightCornerRadius = CornerRadius(sBotR, sBotR),
                                    bottomLeftCornerRadius = CornerRadius(sBotR, sBotR)
                                )
                            )
                        }
                        hasTopBorder && !hasBottomBorder -> {
                            moveTo(sLeft, sBottom)
                            lineTo(sLeft, sTop + sTopR)
                            if (sTopR > 0f) {
                                quadraticBezierTo(sLeft, sTop, sLeft + sTopR, sTop)
                            }
                            lineTo(sRight - sTopR, sTop)
                            if (sTopR > 0f) {
                                quadraticBezierTo(sRight, sTop, sRight, sTop + sTopR)
                            }
                            lineTo(sRight, sBottom)
                        }
                        !hasTopBorder && hasBottomBorder -> {
                            moveTo(sLeft, sTop)
                            lineTo(sLeft, sBottom - sBotR)
                            if (sBotR > 0f) {
                                quadraticBezierTo(sLeft, sBottom, sLeft + sBotR, sBottom)
                            }
                            lineTo(sRight - sBotR, sBottom)
                            if (sBotR > 0f) {
                                quadraticBezierTo(sRight, sBottom, sRight, sBottom - sBotR)
                            }
                            lineTo(sRight, sTop)
                        }
                        else -> {
                            moveTo(sLeft, sTop)
                            lineTo(sLeft, sBottom)
                            moveTo(sRight, sTop)
                            lineTo(sRight, sBottom)
                        }
                    }
                }
            } else null

            val fillPaintColor = fillColor.copy(alpha = highlightStyle.alpha)
            val strokePaintColor = strokeColor.copy(alpha = (highlightStyle.alpha * 1.5f).coerceIn(0f, 1f))
            val strokeStyle = Stroke(width = strokePx)

            onDrawBehind {
                drawPath(path = fillPath, color = fillPaintColor)
                if (borderPath != null && strokePx > 0f) {
                    drawPath(path = borderPath, color = strokePaintColor, style = strokeStyle)
                }
            }
        }
    } else Modifier

    val internalBottomPadding = if (isContinuingToNext) 14.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(planOverlayModifier)
            .padding(bottom = internalBottomPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            verses.forEachIndexed { vIndex, verse ->
                val vNum = verse.verseNumber
                val isSelected = vNum in selectedVerseNumbers
                val isTarget = targetVerse != null && vNum == targetVerse

                val highlightColor = if (isSelected) {
                    Color(0xFF93C5FD).copy(alpha = 0.40f)
                } else if (settings.showHighlights && verse.highlightColor != null) {
                    try { Color(android.graphics.Color.parseColor(verse.highlightColor)) } catch (e: Exception) { null }
                } else if (isTarget) {
                    Color(0xFFFEF08A).copy(alpha = 0.5f)
                } else null

                // Sanitize and split verse into poetic hemistich lines, preventing solitary dangling quotation marks
                val sanitizedVerseText = UsfmTextParserEngine.sanitizeDanglingQuotes(verse.text)
                val initialSplit = when {
                    sanitizedVerseText.contains("\n") -> {
                        sanitizedVerseText.split("\n")
                    }
                    sanitizedVerseText.contains("।") -> {
                        sanitizedVerseText.split(Regex("(?<=।)\\s*"))
                    }
                    sanitizedVerseText.contains(";") -> {
                        sanitizedVerseText.split(Regex("(?<=;)\\s*"))
                    }
                    else -> listOf(sanitizedVerseText)
                }

                // Merge any part that contains only quotation marks or closing punctuation back into the preceding line
                val mergedLines = mutableListOf<String>()
                for (rawPart in initialSplit) {
                    val cleanPart = UsfmTextParserEngine.sanitizeDanglingQuotes(rawPart.trim())
                    if (cleanPart.isBlank()) continue

                    val isOnlyQuotesOrPunctuation = cleanPart.all { it in "\"“”'’»›)]}.,;!?: \t\r\n" }
                    if (isOnlyQuotesOrPunctuation && mergedLines.isNotEmpty()) {
                        val lastIdx = mergedLines.lastIndex
                        mergedLines[lastIdx] = UsfmTextParserEngine.sanitizeDanglingQuotes(mergedLines[lastIdx] + cleanPart)
                    } else {
                        mergedLines.add(cleanPart)
                    }
                }

                if (mergedLines.isEmpty()) {
                    mergedLines.add(sanitizedVerseText.trim())
                }

                val rawLines = mergedLines.mapIndexed { idx, line ->
                    line to (if (idx == 0) 0 else 1)
                }

                rawLines.forEachIndexed { lineIdx, (lineContent, lineIndent) ->
                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    val isFirstLine = lineIdx == 0
                    val isLastLine = lineIdx == rawLines.lastIndex

                    val annotatedString = remember(lineContent, lineIdx, verse, isSelected, isTarget, settings, textColor, jesusColor, accentColor) {
                        buildAnnotatedString {
                            pushStringAnnotation(tag = "VERSE_NUM", annotation = "$vNum")

                            if (isFirstLine && settings.showVerseNumbers) {
                                pushStringAnnotation(tag = "VERSE_NUM_ONLY", annotation = "$vNum")
                                val verseNumColor = if (isSelected || isTarget) Color(0xFFD97706) else Color(0xFF8E8E93)
                                val isNormalSize = settings.verseNumberSize == VerseNumberSize.NORMAL
                                withStyle(
                                    SpanStyle(
                                        color = verseNumColor,
                                        fontWeight = if (isNormalSize) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (isNormalSize) (fontSizeSp.value * 0.90f).sp else (fontSizeSp.value * 0.62f).sp,
                                        baselineShift = if (isNormalSize) BaselineShift.None else BaselineShift(0.35f)
                                    )
                                ) {
                                    append(formatVerseNumberText(verse.verseNumber, settings.verseNumberSize))
                                }
                                pop()
                            }

                            if (isFirstLine && !verse.commentaryText.isNullOrBlank()) {
                                pushStringAnnotation(tag = "COMMENTARY_CLICK", annotation = "$vNum")
                                withStyle(
                                    SpanStyle(
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (fontSizeSp.value * 0.70f).sp,
                                        baselineShift = BaselineShift(0.35f)
                                    )
                                ) {
                                    append(" [*]")
                                }
                                pop()
                            }

                            appendVerseContent(
                                text = lineContent,
                                baseTextColor = textColor,
                                jesusColor = jesusColor,
                                isNewTestament = isNewTestament,
                                fontSizeSp = fontSizeSp,
                                isSelected = isSelected,
                                isTarget = isTarget,
                                highlightColor = highlightColor,
                                settings = settings
                            )

                            if (isLastLine && !verse.secondaryText.isNullOrBlank()) {
                                append("\n")
                                withStyle(
                                    SpanStyle(
                                        color = (jesusColor ?: textColor).copy(alpha = 0.72f),
                                        fontSize = (fontSizeSp.value * 0.90f).sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        fontWeight = FontWeight.Normal
                                    )
                                ) {
                                    append("🇬🇧 ${verse.secondaryText}")
                                }
                            }

                            if (isLastLine) {
                                if (settings.showFavoritesHint && verse.isFavorite) {
                                    pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "$vNum")
                                    withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                                        append(" ⭐")
                                    }
                                    pop()
                                }
                                if (settings.showBookmarkHint && verse.isBookmarked) {
                                    pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "$vNum")
                                    withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                                        append(" 🔖")
                                    }
                                    pop()
                                }
                                if (settings.showNoteHint && !verse.note.isNullOrBlank()) {
                                    pushStringAnnotation(tag = "ATTACHMENT_CLICK", annotation = "$vNum")
                                    withStyle(SpanStyle(fontSize = (settings.fontSize.sp * 0.75f).sp, baselineShift = BaselineShift(0.25f))) {
                                        append(" 📝")
                                    }
                                    pop()
                                }
                            }

                            pop()
                        }
                    }

                    val indentStartPadding = if (settings.showParagraphAndIndents) {
                        (lineIndent * 20 + 8).dp
                    } else 0.dp

                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = fontFamily,
                            lineHeight = lineHeightSp,
                            letterSpacing = 0.20.sp,
                            textAlign = if (settings.justifyBibleText) TextAlign.Justify else TextAlign.Start
                        ),
                        onTextLayout = { layoutResult = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indentStartPadding, top = 2.dp, bottom = 2.dp)
                            .pointerInput(lineContent, vNum) {
                                detectTapGestures(
                                    onTap = { pos ->
                                        layoutResult?.let { layout ->
                                            val offset = layout.getOffsetForPosition(pos)
                                            val commAnnotation = annotatedString.getStringAnnotations(tag = "COMMENTARY_CLICK", start = offset, end = offset).firstOrNull()
                                            if (commAnnotation != null) {
                                                onCommentaryClick(vNum)
                                                return@detectTapGestures
                                            }

                                            val attachAnnotation = annotatedString.getStringAnnotations(tag = "ATTACHMENT_CLICK", start = offset, end = offset).firstOrNull()
                                            if (attachAnnotation != null) {
                                                onAttachmentClick(vNum)
                                                return@detectTapGestures
                                            }

                                            val numOnly = annotatedString.getStringAnnotations(tag = "VERSE_NUM_ONLY", start = offset, end = offset).firstOrNull()?.item?.toIntOrNull()
                                            val fullVerse = annotatedString.getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset).firstOrNull()?.item?.toIntOrNull()

                                            if (settings.verseTapSelectionMode == VerseTapSelectionMode.VERSE_NUMBER_ONLY) {
                                                if (numOnly != null) {
                                                    onVerseSingleTap(numOnly)
                                                } else {
                                                    onVerseLongPress(vNum)
                                                }
                                            } else {
                                                val targetV = fullVerse ?: numOnly ?: vNum
                                                onVerseSingleTap(targetV)
                                            }
                                        }
                                    },
                                    onLongPress = { pos ->
                                        layoutResult?.let { layout ->
                                            val offset = layout.getOffsetForPosition(pos)
                                            annotatedString.getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset)
                                                .firstOrNull()?.let { annotation ->
                                                    annotation.item.toIntOrNull()?.let { onVerseLongPress(it) }
                                                } ?: onVerseLongPress(vNum)
                                        }
                                    }
                                )
                            }
                    )
                }

                // Stanza spacing between poetic verses
                if (vIndex < verses.size - 1 && settings.showParagraphAndIndents) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun YouVersionProseParagraph(
    verseItems: List<VerseItem>,
    versesStateMap: Map<Int, BibleVerse>,
    selectedVerseNumbers: Set<Int>,
    targetVerse: Int?,
    planHighlightRange: IntRange? = null,
    highlightStyle: ReadingPlanHighlightStyle = ReadingPlanHighlightStyle(),
    hasTopBorder: Boolean = true,
    hasBottomBorder: Boolean = true,
    isContinuingToNext: Boolean = false,
    settings: BibleReadingSettings,
    fontFamily: FontFamily,
    textColor: Color,
    isNewTestament: Boolean,
    onVerseSingleTap: (Int) -> Unit,
    onVerseLongPress: (Int) -> Unit,
    onAttachmentClick: (Int) -> Unit,
    onFootnoteClick: (VerseItem, FootnoteItem) -> Unit,
    onCommentaryClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * settings.lineSpacing.multiplier).sp
    val accentColor = MaterialTheme.colorScheme.primary
    val jesusColor = getJesusWordColor(settings, isNewTestament)
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedString = remember(verseItems, versesStateMap, selectedVerseNumbers, targetVerse, planHighlightRange, highlightStyle, settings, textColor, accentColor, jesusColor) {
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
                    val verseNumColor = if (isSelected || isTarget) Color(0xFFD97706) else Color(0xFF8E8E93)
                    val isNormalSize = settings.verseNumberSize == VerseNumberSize.NORMAL
                    withStyle(
                        SpanStyle(
                            color = verseNumColor,
                            fontWeight = if (isNormalSize) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isNormalSize) (fontSizeSp.value * 0.90f).sp else (fontSizeSp.value * 0.62f).sp,
                            baselineShift = if (isNormalSize) BaselineShift.None else BaselineShift(0.35f)
                        )
                    ) {
                        append(formatVerseNumberText(vItem.verseNumber, settings.verseNumberSize))
                    }
                    pop()
                }

                if (!bibleVerse?.commentaryText.isNullOrBlank()) {
                    pushStringAnnotation(tag = "COMMENTARY_CLICK", annotation = "${effectiveVNum ?: 0}")
                    withStyle(
                        SpanStyle(
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = (fontSizeSp.value * 0.70f).sp,
                            baselineShift = BaselineShift(0.35f)
                        )
                    ) {
                        append(" [*]")
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

                appendVerseContent(
                    text = vItem.text,
                    baseTextColor = textColor,
                    jesusColor = jesusColor,
                    isNewTestament = isNewTestament,
                    fontSizeSp = fontSizeSp,
                    isSelected = isSelected,
                    isTarget = isTarget,
                    highlightColor = highlightColor,
                    settings = settings
                )

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

    val planOverlayModifier = if (planHighlightRange != null && highlightStyle.isVisible) {
        Modifier.drawWithCache {
            val layout = layoutResult
            if (layout == null) {
                onDrawBehind { }
            } else {
                val annotations = annotatedString.getStringAnnotations(tag = "VERSE_NUM", start = 0, end = annotatedString.length)
                var firstStart = Int.MAX_VALUE
                var lastEnd = -1
                annotations.forEach { ann ->
                    val vNum = ann.item.toIntOrNull()
                    if (vNum != null && vNum in planHighlightRange) {
                        if (ann.start < firstStart) firstStart = ann.start
                        if (ann.end > lastEnd) lastEnd = ann.end
                    }
                }
                if (firstStart >= lastEnd || firstStart == Int.MAX_VALUE) {
                    onDrawBehind { }
                } else {
                    val paddingPx = 3.dp.toPx()
                    val rectTop = if (hasTopBorder) {
                        val sLine = layout.getLineForOffset(firstStart.coerceIn(0, (annotatedString.length - 1).coerceAtLeast(0)))
                        (layout.getLineTop(sLine) - paddingPx).coerceAtLeast(0f)
                    } else 0f

                    val rectBottom = if (hasBottomBorder) {
                        val eLine = layout.getLineForOffset((lastEnd - 1).coerceIn(0, (annotatedString.length - 1).coerceAtLeast(0)))
                        (layout.getLineBottom(eLine) + paddingPx).coerceAtMost(size.height)
                    } else size.height

                    val rectW = size.width
                    val fillColor = try {
                        Color(android.graphics.Color.parseColor(highlightStyle.windowFillColorHex))
                    } catch (_: Exception) {
                        Color(0xFFFDE68A)
                    }
                    val strokeColor = try {
                        Color(android.graphics.Color.parseColor(highlightStyle.strokeColorHex))
                    } catch (_: Exception) {
                        Color(0xFFD97706)
                    }

                    val cRadiusPx = highlightStyle.cornerRadiusDp.dp.toPx()
                    val strokePx = highlightStyle.borderThicknessDp.dp.toPx()
                    val halfStroke = strokePx / 2f

                    val topR = if (hasTopBorder) cRadiusPx else 0f
                    val botR = if (hasBottomBorder) cRadiusPx else 0f

                    val fillPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = 0f,
                                top = rectTop,
                                right = rectW,
                                bottom = rectBottom,
                                topLeftCornerRadius = CornerRadius(topR, topR),
                                topRightCornerRadius = CornerRadius(topR, topR),
                                bottomRightCornerRadius = CornerRadius(botR, botR),
                                bottomLeftCornerRadius = CornerRadius(botR, botR)
                            )
                        )
                    }

                    val borderPath = if (strokePx > 0f) {
                        val sLeft = halfStroke
                        val sRight = rectW - halfStroke
                        val sTop = if (hasTopBorder) rectTop + halfStroke else rectTop
                        val sBottom = if (hasBottomBorder) rectBottom - halfStroke else rectBottom
                        val sTopR = (topR - halfStroke).coerceAtLeast(0f)
                        val sBotR = (botR - halfStroke).coerceAtLeast(0f)

                        Path().apply {
                            when {
                                hasTopBorder && hasBottomBorder -> {
                                    addRoundRect(
                                        RoundRect(
                                            left = sLeft,
                                            top = sTop,
                                            right = sRight,
                                            bottom = sBottom,
                                            topLeftCornerRadius = CornerRadius(sTopR, sTopR),
                                            topRightCornerRadius = CornerRadius(sTopR, sTopR),
                                            bottomRightCornerRadius = CornerRadius(sBotR, sBotR),
                                            bottomLeftCornerRadius = CornerRadius(sBotR, sBotR)
                                        )
                                    )
                                }
                                hasTopBorder && !hasBottomBorder -> {
                                    moveTo(sLeft, sBottom)
                                    lineTo(sLeft, sTop + sTopR)
                                    if (sTopR > 0f) {
                                        quadraticBezierTo(sLeft, sTop, sLeft + sTopR, sTop)
                                    }
                                    lineTo(sRight - sTopR, sTop)
                                    if (sTopR > 0f) {
                                        quadraticBezierTo(sRight, sTop, sRight, sTop + sTopR)
                                    }
                                    lineTo(sRight, sBottom)
                                }
                                !hasTopBorder && hasBottomBorder -> {
                                    moveTo(sLeft, sTop)
                                    lineTo(sLeft, sBottom - sBotR)
                                    if (sBotR > 0f) {
                                        quadraticBezierTo(sLeft, sBottom, sLeft + sBotR, sBottom)
                                    }
                                    lineTo(sRight - sBotR, sBottom)
                                    if (sBotR > 0f) {
                                        quadraticBezierTo(sRight, sBottom, sRight, sBottom - sBotR)
                                    }
                                    lineTo(sRight, sTop)
                                }
                                else -> {
                                    moveTo(sLeft, sTop)
                                    lineTo(sLeft, sBottom)
                                    moveTo(sRight, sTop)
                                    lineTo(sRight, sBottom)
                                }
                            }
                        }
                    } else null

                    val fillPaintColor = fillColor.copy(alpha = highlightStyle.alpha)
                    val strokePaintColor = strokeColor.copy(alpha = (highlightStyle.alpha * 1.5f).coerceIn(0f, 1f))
                    val strokeStyle = Stroke(width = strokePx)

                    onDrawBehind {
                        drawPath(path = fillPath, color = fillPaintColor)
                        if (borderPath != null && strokePx > 0f) {
                            drawPath(path = borderPath, color = strokePaintColor, style = strokeStyle)
                        }
                    }
                }
            }
        }
    } else Modifier

    val internalBottomPadding = if (isContinuingToNext) 14.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(planOverlayModifier)
            .padding(bottom = internalBottomPadding)
    ) {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = fontFamily,
                lineHeight = lineHeightSp,
                letterSpacing = 0.25.sp,
                textAlign = if (settings.justifyBibleText) TextAlign.Justify else TextAlign.Start
            ),
            onTextLayout = { layoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(verseItems) {
                    detectTapGestures(
                        onTap = { pos ->
                            layoutResult?.let { layout ->
                                val offset = layout.getOffsetForPosition(pos)
                                val commAnnotation = annotatedString.getStringAnnotations(tag = "COMMENTARY_CLICK", start = offset, end = offset).firstOrNull()
                                if (commAnnotation != null) {
                                    val vNum = commAnnotation.item.toIntOrNull()
                                    if (vNum != null) {
                                        onCommentaryClick(vNum)
                                        return@detectTapGestures
                                    }
                                }

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
}

@Composable
private fun YouVersionPoetryBlock(
    lineItems: List<PoetryLineItem>,
    versesStateMap: Map<Int, BibleVerse>,
    selectedVerseNumbers: Set<Int>,
    targetVerse: Int?,
    planHighlightRange: IntRange? = null,
    highlightStyle: ReadingPlanHighlightStyle = ReadingPlanHighlightStyle(),
    hasTopBorder: Boolean = true,
    hasBottomBorder: Boolean = true,
    isContinuingToNext: Boolean = false,
    settings: BibleReadingSettings,
    fontFamily: FontFamily,
    textColor: Color,
    isNewTestament: Boolean,
    onVerseSingleTap: (Int) -> Unit,
    onVerseLongPress: (Int) -> Unit,
    onAttachmentClick: (Int) -> Unit,
    onFootnoteClick: (PoetryLineItem, FootnoteItem) -> Unit,
    onCommentaryClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fontSizeSp = settings.fontSize.sp.sp
    val lineHeightSp = (settings.fontSize.sp * (settings.lineSpacing.multiplier + 0.15f)).sp
    val accentColor = MaterialTheme.colorScheme.primary
    val jesusColor = getJesusWordColor(settings, isNewTestament)

    val hasTargetLines = planHighlightRange != null && lineItems.any { line ->
        val v = line.verseNumber
        v != null && v in planHighlightRange
    }

    val planOverlayModifier = if (hasTargetLines && highlightStyle.isVisible) {
        Modifier.drawWithCache {
            val rectTop = 0f
            val rectBottom = size.height
            val rectW = size.width

            val fillColor = try {
                Color(android.graphics.Color.parseColor(highlightStyle.windowFillColorHex))
            } catch (_: Exception) {
                Color(0xFFFDE68A)
            }
            val strokeColor = try {
                Color(android.graphics.Color.parseColor(highlightStyle.strokeColorHex))
            } catch (_: Exception) {
                Color(0xFFD97706)
            }

            val cRadiusPx = highlightStyle.cornerRadiusDp.dp.toPx()
            val strokePx = highlightStyle.borderThicknessDp.dp.toPx()
            val halfStroke = strokePx / 2f

            val topR = if (hasTopBorder) cRadiusPx else 0f
            val botR = if (hasBottomBorder) cRadiusPx else 0f

            val fillPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = rectTop,
                        right = rectW,
                        bottom = rectBottom,
                        topLeftCornerRadius = CornerRadius(topR, topR),
                        topRightCornerRadius = CornerRadius(topR, topR),
                        bottomRightCornerRadius = CornerRadius(botR, botR),
                        bottomLeftCornerRadius = CornerRadius(botR, botR)
                    )
                )
            }

            val borderPath = if (strokePx > 0f) {
                val sLeft = halfStroke
                val sRight = rectW - halfStroke
                val sTop = if (hasTopBorder) rectTop + halfStroke else rectTop
                val sBottom = if (hasBottomBorder) rectBottom - halfStroke else rectBottom
                val sTopR = (topR - halfStroke).coerceAtLeast(0f)
                val sBotR = (botR - halfStroke).coerceAtLeast(0f)

                Path().apply {
                    when {
                        hasTopBorder && hasBottomBorder -> {
                            addRoundRect(
                                RoundRect(
                                    left = sLeft,
                                    top = sTop,
                                    right = sRight,
                                    bottom = sBottom,
                                    topLeftCornerRadius = CornerRadius(sTopR, sTopR),
                                    topRightCornerRadius = CornerRadius(sTopR, sTopR),
                                    bottomRightCornerRadius = CornerRadius(sBotR, sBotR),
                                    bottomLeftCornerRadius = CornerRadius(sBotR, sBotR)
                                )
                            )
                        }
                        hasTopBorder && !hasBottomBorder -> {
                            moveTo(sLeft, sBottom)
                            lineTo(sLeft, sTop + sTopR)
                            if (sTopR > 0f) {
                                quadraticBezierTo(sLeft, sTop, sLeft + sTopR, sTop)
                            }
                            lineTo(sRight - sTopR, sTop)
                            if (sTopR > 0f) {
                                quadraticBezierTo(sRight, sTop, sRight, sTop + sTopR)
                            }
                            lineTo(sRight, sBottom)
                        }
                        !hasTopBorder && hasBottomBorder -> {
                            moveTo(sLeft, sTop)
                            lineTo(sLeft, sBottom - sBotR)
                            if (sBotR > 0f) {
                                quadraticBezierTo(sLeft, sBottom, sLeft + sBotR, sBottom)
                            }
                            lineTo(sRight - sBotR, sBottom)
                            if (sBotR > 0f) {
                                quadraticBezierTo(sRight, sBottom, sRight, sBottom - sBotR)
                            }
                            lineTo(sRight, sTop)
                        }
                        else -> {
                            moveTo(sLeft, sTop)
                            lineTo(sLeft, sBottom)
                            moveTo(sRight, sTop)
                            lineTo(sRight, sBottom)
                        }
                    }
                }
            } else null

            val fillPaintColor = fillColor.copy(alpha = highlightStyle.alpha)
            val strokePaintColor = strokeColor.copy(alpha = (highlightStyle.alpha * 1.5f).coerceIn(0f, 1f))
            val strokeStyle = Stroke(width = strokePx)

            onDrawBehind {
                drawPath(path = fillPath, color = fillPaintColor)
                if (borderPath != null && strokePx > 0f) {
                    drawPath(path = borderPath, color = strokePaintColor, style = strokeStyle)
                }
            }
        }
    } else Modifier

    val internalBottomPadding = if (isContinuingToNext) 14.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(planOverlayModifier)
            .padding(bottom = internalBottomPadding)
    ) {
        Column(
            modifier = Modifier
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
                            val verseNumColor = if (isSelected || isTarget) Color(0xFFD97706) else Color(0xFF8E8E93)
                            val isNormalSize = settings.verseNumberSize == VerseNumberSize.NORMAL
                            withStyle(
                                SpanStyle(
                                    color = verseNumColor,
                                    fontWeight = if (isNormalSize) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = if (isNormalSize) (fontSizeSp.value * 0.90f).sp else (fontSizeSp.value * 0.62f).sp,
                                    baselineShift = if (isNormalSize) BaselineShift.None else BaselineShift(0.35f)
                                )
                            ) {
                                append(formatVerseNumberText(line.verseNumber, settings.verseNumberSize))
                            }
                            pop()
                        }

                        if (!bibleVerse?.commentaryText.isNullOrBlank()) {
                            pushStringAnnotation(tag = "COMMENTARY_CLICK", annotation = "${effectiveVNum ?: 0}")
                            withStyle(
                                SpanStyle(
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (fontSizeSp.value * 0.70f).sp,
                                    baselineShift = BaselineShift(0.35f)
                                )
                            ) {
                                append(" [*]")
                            }
                            pop()
                        }

                        appendVerseContent(
                            text = line.text,
                            baseTextColor = textColor,
                            jesusColor = jesusColor,
                            isNewTestament = isNewTestament,
                            fontSizeSp = fontSizeSp,
                            isSelected = isSelected,
                            isTarget = isTarget,
                            highlightColor = highlightColor,
                            settings = settings
                        )

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
                        letterSpacing = 0.20.sp,
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
                                        val commAnnotation = annotatedString.getStringAnnotations(tag = "COMMENTARY_CLICK", start = offset, end = offset).firstOrNull()
                                        if (commAnnotation != null) {
                                            val vNum = commAnnotation.item.toIntOrNull()
                                            if (vNum != null) {
                                                onCommentaryClick(vNum)
                                                return@detectTapGestures
                                            }
                                        }

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

@Composable
private fun ChapterOutlineCard(
    groups: List<ChapterVerseGroup>,
    isHindi: Boolean,
    onGroupClick: (ChapterVerseGroup) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isHindi) "अध्याय रूपरेखा एवं वचन समूह" else "Chapter Outline & Groups",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = if (isHindi) "${groups.size} मुख्य विषय / समूह (टैप करके पढ़ें)" else "${groups.size} Sections / Topics (Tap to jump)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hide Outline",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEach { group ->
                        Surface(
                            onClick = { onGroupClick(group) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (group.startVerse == group.endVerse) {
                                            if (isHindi) "व. ${group.startVerse}" else "v. ${group.startVerse}"
                                        } else {
                                            if (isHindi) "व. ${group.startVerse}–${group.endVerse}" else "v. ${group.startVerse}–${group.endVerse}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = group.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Jump",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

