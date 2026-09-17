package com.example.ui.bible.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom USFM Text Parser Data Structures
 */
data class UsfmSpan(
    val text: String,
    val isVerseNum: Boolean = false,
    val verseNum: String = "",
    val isRedLetter: Boolean = false,
    val isBoldItalicHeader: Boolean = false,
    val isFootnote: Boolean = false
)

sealed class UsfmParsedBlock {
    data class Subheading(val text: String) : UsfmParsedBlock()
    data class Paragraph(val spans: List<UsfmSpan>) : UsfmParsedBlock()
    data class PoeticLine(val spans: List<UsfmSpan>, val indentLevel: Int = 1) : UsfmParsedBlock()
    object StanzaBreak : UsfmParsedBlock()
}

object UsfmTextParserEngine {

    /**
     * Sanitizes and parses raw USFM text or tagged Bible text into structured blocks.
     * Rules applied:
     * 1. Clean & Hide: Strips out raw backslash markers (\p, \m, \v, \+wj, \wj, \s, \h, \f, \x, etc.)
     * 2. Paragraphs (\p or \m): Splits into distinct paragraph blocks.
     * 3. Poetic Lines (\q, \q1, \q2, \q3, \b): Splits into indented poetic lines and stanza breaks.
     * 4. Verses (\v): Extracts \v [number] for 12sp superscript subtle gray rendering.
     * 5. Subheadings (\s, \s1, \h): Extracts subheadings for Bold & Italicized rendering.
     * 6. Words of Christ (\wj / \+wj ... \wj*): Enclosed text marked for Red/Crimson letter rendering.
     */
    fun parseUsfmToBlocks(usfmText: String): List<UsfmParsedBlock> {
        if (usfmText.isBlank()) return emptyList()

        val blocks = mutableListOf<UsfmParsedBlock>()
        
        // 0. Pre-sanitize dangling quotes, linebreaks, and trailing whitespace before quotes
        val sanitizedInput = sanitizeDanglingQuotes(usfmText)

        // Normalize line breaks & strip footnote/crossref tags \f ... \f* or \x ... \x*
        val cleanedInput = sanitizedInput
            .replace(Regex("\\\\f\\s+.*?\\\\f\\*"), "")
            .replace(Regex("\\\\x\\s+.*?\\\\x\\*"), "")
            .replace(Regex("\\\\id\\s+.*"), "")
            .replace(Regex("\\\\c\\s+\\d+"), "")
            .replace(Regex("\\\\toc\\d\\s+.*"), "")

        // Split text by paragraph, poetic line, or subheading markers (\p, \m, \nb, \s, \s1, \s2, \h, \q, \q1, \q2, \q3, \b)
        val lineTokens = cleanedInput.split(Regex("(?=\\\\[pmsmnhb]|\\\\q\\d?)"))

        for (token in lineTokens) {
            val trimmed = token.trim()
            if (trimmed.isBlank()) continue

            when {
                // Stanza break marker (\b)
                trimmed.startsWith("\\b") -> {
                    blocks.add(UsfmParsedBlock.StanzaBreak)
                }

                // Subheading markers (\s, \s1, \s2, \h, \ms)
                trimmed.startsWith("\\s") || trimmed.startsWith("\\h") || trimmed.startsWith("\\ms") -> {
                    val headerText = trimmed
                        .replace(Regex("^\\\\[shms]\\d?\\s*"), "")
                        .replace(Regex("\\\\[a-zA-Z0-9+*]+\\s*"), "")
                        .trim()
                    if (headerText.isNotBlank()) {
                        blocks.add(UsfmParsedBlock.Subheading(headerText))
                    }
                }

                // Poetic line markers (\q, \q1, \q2, \q3)
                trimmed.startsWith("\\q") -> {
                    val indentLevel = when {
                        trimmed.startsWith("\\q3") -> 3
                        trimmed.startsWith("\\q2") -> 2
                        else -> 1
                    }
                    val content = trimmed.replace(Regex("^\\\\q\\d?\\s*"), "")
                    val spans = parseSpansFromContent(content)
                    if (spans.isNotEmpty()) {
                        blocks.add(UsfmParsedBlock.PoeticLine(spans, indentLevel))
                    }
                }

                // Paragraph or general text block
                else -> {
                    val content = trimmed.replace(Regex("^\\\\[pmnb]\\d?\\s*"), "")
                    val spans = parseSpansFromContent(content)
                    if (spans.isNotEmpty()) {
                        blocks.add(UsfmParsedBlock.Paragraph(spans))
                    }
                }
            }
        }

        return blocks
    }

    private fun parseSpansFromContent(content: String): List<UsfmSpan> {
        val spans = mutableListOf<UsfmSpan>()
        // Match verse markers (\v 12), words of christ (\wj ... \wj* or \+wj ... \+wj*), and normal text
        val verseRegex = Regex("\\\\v\\s+(\\d+[-–\\d]*)")
        val wjRegex = Regex("\\\\\\+?wj\\s+(.*?)\\\\\\+?wj\\*")

        // Split by \v markers first
        val verseParts = content.split(verseRegex)
        val verseMatches = verseRegex.findAll(content).toList()

        for (i in verseParts.indices) {
            val part = verseParts[i]
            if (i > 0 && i - 1 < verseMatches.size) {
                val vNum = verseMatches[i - 1].groupValues[1]
                spans.add(
                    UsfmSpan(
                        text = "$vNum ",
                        isVerseNum = true,
                        verseNum = vNum
                    )
                )
            }

            if (part.isBlank()) continue

            // Parse words of Christ inside this part
            var lastIdx = 0
            for (match in wjRegex.findAll(part)) {
                if (match.range.first > lastIdx) {
                    val rawNormal = part.substring(lastIdx, match.range.first)
                    val cleanNormal = cleanRawUsfmTags(rawNormal)
                    if (cleanNormal.isNotEmpty()) {
                        spans.add(UsfmSpan(text = cleanNormal))
                    }
                }

                val wjText = cleanRawUsfmTags(match.groupValues[1])
                if (wjText.isNotEmpty()) {
                    spans.add(UsfmSpan(text = wjText, isRedLetter = true))
                }
                lastIdx = match.range.last + 1
            }

            if (lastIdx < part.length) {
                val rawTail = part.substring(lastIdx)
                val cleanTail = cleanRawUsfmTags(rawTail)
                if (cleanTail.isNotEmpty()) {
                    spans.add(UsfmSpan(text = cleanTail))
                }
            }
        }

        return spans
    }

    /**
     * Sanitizes verse and USFM text to prevent dangling quotation marks and unwanted formatting artifacts.
     * Removes whitespace, newlines, or <br> tags immediately preceding quotation marks,
     * and tightly binds quotation marks to the preceding word to prevent Android's text wrapping
     * engine from pushing solitary punctuation to a new line.
     */
    fun sanitizeDanglingQuotes(text: String): String {
        if (text.isEmpty()) return text
        return text
            // 1. Remove newlines, <br>, <br/>, &nbsp;, and whitespace preceding quotes
            .replace(Regex("""(?i)(?:\s|<br\s*/?>|&nbsp;|\r?\n)+([”"’'»])"""), "$1")
            // 2. Remove any space directly preceding a closing quote/punctuation
            .replace(Regex("""(?<=\S)\s+([”"’'»])"""), "$1")
            // 3. Prevent space between danda/punctuation and quotation mark
            .replace(Regex("""([।,;\.!\?:])\s+([”"’'»])"""), "$1$2")
    }

    /**
     * Helper to clean any leftover USFM backslash tags (e.g. \+q, \qs, \*, etc.)
     * and sanitize dangling quotes.
     */
    fun cleanRawUsfmTags(raw: String): String {
        val noTags = raw
            .replace(Regex("\\\\\\+?[a-zA-Z0-9]+\\*?"), "")
            .replace(Regex("\\*"), "")
            .replace(Regex("(?i)(?:<br\\s*/?>|&nbsp;)"), " ")
        
        val sanitized = sanitizeDanglingQuotes(noTags)
        return sanitized
            .replace(Regex("\\s+"), " ")
            .replace(Regex("""\s+([”"’'»,;\.।!\?])"""), "$1")
            .trim()
    }
}

/**
 * Custom USFM Text Parser Component for the Bible Reading Screen.
 * Implements strict rules:
 * 1. Clean & Hide: No raw backslash tags visible.
 * 2. Paragraphs (\p or \m): Bottom margin 16px (16dp).
 * 3. Verses (\v): Superscript, 12sp font size, subtle gray color, small space after.
 * 4. Subheadings (\s, \s1, \h): Bold & Italicized subheadings with 24px top & 16px bottom margin.
 * 5. Words of Christ: Crimson/red font color (#DC2626).
 * 6. Typography: Global Serif font, line-height 1.6, and 16px horizontal padding.
 */
@Composable
fun UsfmTextParser(
    usfmText: String,
    modifier: Modifier = Modifier,
    fontSizeSp: Float = 18f,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    subheadingColor: Color = MaterialTheme.colorScheme.primary,
    redLetterColor: Color = Color(0xFFDC2626), // Crimson / Red
    verseNumColor: Color = Color(0xFF64748B), // Subtle Gray
    onVerseTap: ((Int) -> Unit)? = null,
    onVerseLongPress: ((Int) -> Unit)? = null,
    selectedVerses: Set<Int> = emptySet(),
    targetVerse: Int? = null
) {
    val blocks = remember(usfmText) { UsfmTextParserEngine.parseUsfmToBlocks(usfmText) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // 16px padding on left & right
    ) {
        blocks.forEach { block ->
            when (block) {
                is UsfmParsedBlock.Subheading -> {
                    // Subheading: Bold and Italicized, 24px top margin & 16px bottom margin
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = block.text,
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            fontSize = (fontSizeSp * 1.15f).sp,
                            lineHeight = (fontSizeSp * 1.15f * 1.4f).sp,
                            color = subheadingColor
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                is UsfmParsedBlock.Paragraph -> {
                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    
                    val annotatedString = remember(
                        block.spans,
                        fontSizeSp,
                        textColor,
                        redLetterColor,
                        verseNumColor,
                        selectedVerses,
                        targetVerse
                    ) {
                        buildAnnotatedString {
                            var currentVerseNum: Int? = null

                            block.spans.forEach { span ->
                                if (span.isVerseNum) {
                                    val vNumInt = span.verseNum.toIntOrNull()
                                    if (vNumInt != null) {
                                        currentVerseNum = vNumInt
                                        pushStringAnnotation(tag = "VERSE_NUM", annotation = "$vNumInt")
                                    }

                                    val isSelected = currentVerseNum != null && currentVerseNum in selectedVerses
                                    val isTarget = targetVerse != null && currentVerseNum == targetVerse

                                    // Verses (\v): Superscript, 12sp font size, subtle gray, space after
                                    withStyle(
                                        SpanStyle(
                                            color = if (isSelected || isTarget) Color(0xFFD97706) else verseNumColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            baselineShift = BaselineShift.Superscript
                                        )
                                    ) {
                                        append(span.text)
                                    }
                                    if (vNumInt != null) pop()
                                } else {
                                    val isSelected = currentVerseNum != null && currentVerseNum in selectedVerses
                                    val isTarget = targetVerse != null && currentVerseNum == targetVerse

                                    val highlightBg = when {
                                        isSelected -> Color(0xFF93C5FD).copy(alpha = 0.40f)
                                        isTarget -> Color(0xFFFEF08A).copy(alpha = 0.50f)
                                        else -> Color.Transparent
                                    }

                                    // Words of Christ (Red Letters) vs Normal text
                                    val finalTextColor = if (span.isRedLetter) redLetterColor else textColor

                                    withStyle(
                                        SpanStyle(
                                            color = finalTextColor,
                                            fontFamily = FontFamily.Serif, // Global Serif font
                                            fontSize = fontSizeSp.sp,
                                            fontWeight = if (isSelected || isTarget) FontWeight.Medium else FontWeight.Normal,
                                            background = highlightBg
                                        )
                                    ) {
                                        append(span.text)
                                    }
                                }
                            }
                        }
                    }

                    // Render Paragraph with Line Height 1.6
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(block.spans, onVerseTap, onVerseLongPress) {
                                detectTapGestures(
                                    onTap = { pos ->
                                        layoutResult?.let { layout ->
                                            val offset = layout.getOffsetForPosition(pos)
                                            val annotation = annotatedString
                                                .getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset)
                                                .firstOrNull()
                                            val vNum = annotation?.item?.toIntOrNull()
                                            if (vNum != null && onVerseTap != null) {
                                                onVerseTap(vNum)
                                            }
                                        }
                                    },
                                    onLongPress = { pos ->
                                        layoutResult?.let { layout ->
                                            val offset = layout.getOffsetForPosition(pos)
                                            val annotation = annotatedString
                                                .getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset)
                                                .firstOrNull()
                                            val vNum = annotation?.item?.toIntOrNull()
                                            if (vNum != null && onVerseLongPress != null) {
                                                onVerseLongPress(vNum)
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = annotatedString,
                            style = TextStyle(
                                fontFamily = FontFamily.Serif, // Global Serif font
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.6f).sp // Line-height 1.6
                            ),
                            onTextLayout = { layoutResult = it }
                        )
                    }

                    // Paragraphs (\p or \m): Bottom margin of 16px (16dp)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                is UsfmParsedBlock.StanzaBreak -> {
                    // Stanza Break (\b): 14dp spacing between poetic strophes
                    Spacer(modifier = Modifier.height(14.dp))
                }

                is UsfmParsedBlock.PoeticLine -> {
                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    val indentStart = (block.indentLevel * 18).dp

                    val annotatedString = remember(
                        block.spans,
                        fontSizeSp,
                        textColor,
                        redLetterColor,
                        verseNumColor,
                        selectedVerses,
                        targetVerse
                    ) {
                        buildAnnotatedString {
                            var currentVerseNum: Int? = null

                            block.spans.forEach { span ->
                                if (span.isVerseNum) {
                                    val vNumInt = span.verseNum.toIntOrNull()
                                    if (vNumInt != null) {
                                        currentVerseNum = vNumInt
                                        pushStringAnnotation(tag = "VERSE_NUM", annotation = "$vNumInt")
                                    }

                                    val isSelected = currentVerseNum != null && currentVerseNum in selectedVerses
                                    val isTarget = targetVerse != null && currentVerseNum == targetVerse

                                    withStyle(
                                        SpanStyle(
                                            color = if (isSelected || isTarget) Color(0xFFD97706) else verseNumColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            baselineShift = BaselineShift.Superscript
                                        )
                                    ) {
                                        append(span.text)
                                    }
                                    if (vNumInt != null) pop()
                                } else {
                                    val isSelected = currentVerseNum != null && currentVerseNum in selectedVerses
                                    val isTarget = targetVerse != null && currentVerseNum == targetVerse

                                    val highlightBg = when {
                                        isSelected -> Color(0xFF93C5FD).copy(alpha = 0.40f)
                                        isTarget -> Color(0xFFFEF08A).copy(alpha = 0.50f)
                                        else -> Color.Transparent
                                    }

                                    val finalTextColor = if (span.isRedLetter) redLetterColor else textColor

                                    withStyle(
                                        SpanStyle(
                                            color = finalTextColor,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = fontSizeSp.sp,
                                            fontWeight = if (isSelected || isTarget) FontWeight.Medium else FontWeight.Normal,
                                            background = highlightBg
                                        )
                                    ) {
                                        append(span.text)
                                    }
                                }
                            }
                        }
                    }

                    // Render Poetic Line with indentation and line spacing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indentStart, top = 2.dp, bottom = 2.dp)
                            .pointerInput(block.spans, onVerseTap, onVerseLongPress) {
                                detectTapGestures(
                                    onTap = { pos ->
                                        layoutResult?.let { layout ->
                                            val offset = layout.getOffsetForPosition(pos)
                                            val annotation = annotatedString
                                                .getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset)
                                                .firstOrNull()
                                            val vNum = annotation?.item?.toIntOrNull()
                                            if (vNum != null && onVerseTap != null) {
                                                onVerseTap(vNum)
                                            }
                                        }
                                    },
                                    onLongPress = { pos ->
                                        layoutResult?.let { layout ->
                                            val offset = layout.getOffsetForPosition(pos)
                                            val annotation = annotatedString
                                                .getStringAnnotations(tag = "VERSE_NUM", start = offset, end = offset)
                                                .firstOrNull()
                                            val vNum = annotation?.item?.toIntOrNull()
                                            if (vNum != null && onVerseLongPress != null) {
                                                onVerseLongPress(vNum)
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = annotatedString,
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.55f).sp
                            ),
                            onTextLayout = { layoutResult = it }
                        )
                    }
                }
            }
        }
    }
}
