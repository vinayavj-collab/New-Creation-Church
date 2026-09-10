package com.example.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.bible.model.BibleVerse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleNoteEditorDialog(
    verse: BibleVerse,
    initialNote: String,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onNavigateToVerse: ((bookId: Int, chapter: Int, verseNumber: Int) -> Unit)? = null
) {
    var noteText by remember { mutableStateOf(initialNote) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isHtmlMode by remember { mutableStateOf(initialNote.contains("<") && initialNote.contains(">")) }
    var textAlign by remember { mutableStateOf(TextAlign.Start) }
    var selectedFontSize by remember { mutableStateOf(15.sp) }
    var selectedTextColorHex by remember { mutableStateOf("#1E293B") }

    // Helper to insert formatting tags or markdown/HTML
    fun insertTag(startTag: String, endTag: String = "") {
        noteText = if (endTag.isEmpty()) {
            noteText + startTag
        } else {
            noteText + "$startTag$endTag"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = !isFullScreen,
            dismissOnBackPress = true
        )
    ) {
        Card(
            shape = if (isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = if (isFullScreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .padding(8.dp)
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Verse Reference & Fullscreen toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${verse.bookName} ${verse.chapter}:${verse.verseNumber}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            if (onNavigateToVerse != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        onNavigateToVerse(verse.bookId, verse.chapter, verse.verseNumber)
                                        onDismiss()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = "Link Verse to Bible",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "व्यक्तिगत वचन नोट्स (Personal Verse Note)",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Row {
                        IconButton(onClick = { isFullScreen = !isFullScreen }) {
                            Icon(
                                if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullScreen) "Exit Fullscreen" else "Expand Fullscreen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // Verse preview banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = "\"${verse.text}\"",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(10.dp),
                            maxLines = if (isFullScreen) 4 else 2
                        )
                    }
                }

                // Format Mode Selector (Plain text vs HTML)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !isHtmlMode,
                            onClick = { isHtmlMode = false },
                            label = { Text("Plain Text", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = isHtmlMode,
                            onClick = { isHtmlMode = true },
                            label = { Text("HTML Rich", fontSize = 11.sp) }
                        )
                    }

                    // Alignment buttons
                    Row {
                        IconButton(onClick = { textAlign = TextAlign.Start }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.FormatAlignLeft,
                                contentDescription = "Left",
                                tint = if (textAlign == TextAlign.Start) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { textAlign = TextAlign.Center }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.FormatAlignCenter,
                                contentDescription = "Center",
                                tint = if (textAlign == TextAlign.Center) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { textAlign = TextAlign.End }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.FormatAlignRight,
                                contentDescription = "Right",
                                tint = if (textAlign == TextAlign.End) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Rich Formatting Toolbar (Scrollable)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bold
                        ToolbarButton(label = "B", isBold = true) {
                            if (isHtmlMode) insertTag("<b>", "</b>") else insertTag("**", "**")
                        }
                        // Italic
                        ToolbarButton(label = "I", isItalic = true) {
                            if (isHtmlMode) insertTag("<i>", "</i>") else insertTag("*", "*")
                        }
                        // Underline
                        ToolbarButton(label = "U", isUnderline = true) {
                            if (isHtmlMode) insertTag("<u>", "</u>") else insertTag("__", "__")
                        }
                        // Strikethrough
                        ToolbarButton(label = "S", isStrikethrough = true) {
                            if (isHtmlMode) insertTag("<s>", "</s>") else insertTag("~~", "~~")
                        }
                        // Superscript
                        ToolbarButton(label = "X²") {
                            if (isHtmlMode) insertTag("<sup>", "</sup>") else insertTag("^", "")
                        }
                        // Subscript
                        ToolbarButton(label = "X₂") {
                            if (isHtmlMode) insertTag("<sub>", "</sub>") else insertTag("~", "")
                        }
                        // Bullet list
                        ToolbarButton(label = "• List") {
                            noteText = noteText + "\n• "
                        }
                        // Number list
                        ToolbarButton(label = "1. List") {
                            noteText = noteText + "\n1. "
                        }
                        // Link Verse
                        ToolbarButton(label = "🔗 Link") {
                            val link = "[${verse.bookName} ${verse.chapter}:${verse.verseNumber}]"
                            noteText = noteText + " " + link
                        }
                    }
                }

                // Text Input Area
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    placeholder = {
                        Text(
                            text = "इस वचन के बारे में अपने विचार, प्रार्थना या मनन यहाँ लिखें...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = selectedFontSize,
                        textAlign = textAlign,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Action Buttons: Delete, Cancel, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialNote.isNotBlank()) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("हटाएँ (Delete)")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("रद्द करें")
                        }
                        Button(
                            onClick = {
                                onSave(noteText)
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("सहेजें (Save)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    isUnderline: Boolean = false,
    isStrikethrough: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = when {
                    isUnderline -> TextDecoration.Underline
                    isStrikethrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                },
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
