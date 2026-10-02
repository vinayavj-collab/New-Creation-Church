package com.example.ui.bible

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.local.BibleDatabase
import com.example.data.bible.local.BibleVerseEntity
import com.example.data.bible.model.BibleVerse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleVerseCompareDialog(
    referenceTitle: String,
    selectedVerses: List<BibleVerse>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { BibleDatabase.getInstance(context) }
    val dao = remember { database.bibleDao() }

    var availableVersions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedVersionA by remember { mutableStateOf("HIOV") }
    var selectedVersionB by remember { mutableStateOf("ENG_KJV") }

    var textVersionA by remember { mutableStateOf("") }
    var textVersionB by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Dropdown expanded states
    var showVersionAPopup by remember { mutableStateOf(false) }
    var showVersionBPopup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val ids = try { dao.getDistinctTranslationIds() } catch (_: Exception) { listOf("HIOV", "ENG_KJV") }
            availableVersions = if (ids.isNotEmpty()) ids else listOf("HIOV", "ENG_KJV")
            if (!availableVersions.contains(selectedVersionA) && availableVersions.isNotEmpty()) {
                selectedVersionA = availableVersions.first()
            }
            if (availableVersions.size > 1 && !availableVersions.contains(selectedVersionB)) {
                selectedVersionB = availableVersions.firstOrNull { it != selectedVersionA } ?: availableVersions.getOrElse(1) { availableVersions[0] }
            } else if (availableVersions.size == 1) {
                selectedVersionB = availableVersions[0]
            }
        }
    }

    // Fetch comparison texts when verses or selected versions change
    LaunchedEffect(selectedVerses, selectedVersionA, selectedVersionB) {
        if (selectedVerses.isEmpty()) return@LaunchedEffect
        isLoading = true
        withContext(Dispatchers.IO) {
            val first = selectedVerses.first()
            val bookId = first.bookId
            val chapter = first.chapter
            val verseNums = selectedVerses.map { it.verseNumber }.toSet()

            // Fetch Version A text
            val versesA = try {
                dao.getVersesForChapterSync(selectedVersionA, bookId, chapter).filter { it.verseNumber in verseNums }
            } catch (_: Exception) { emptyList<BibleVerseEntity>() }
            textVersionA = if (versesA.isNotEmpty()) {
                versesA.joinToString(" ") { com.example.ui.bible.components.UsfmTextParserEngine.cleanVerseText(it.text) }
            } else {
                selectedVerses.joinToString(" ") { com.example.ui.bible.components.UsfmTextParserEngine.cleanVerseText(it.text) }
            }

            // Fetch Version B text
            val versesB = try {
                dao.getVersesForChapterSync(selectedVersionB, bookId, chapter).filter { it.verseNumber in verseNums }
            } catch (_: Exception) { emptyList<BibleVerseEntity>() }
            textVersionB = if (versesB.isNotEmpty()) {
                versesB.joinToString(" ") { com.example.ui.bible.components.UsfmTextParserEngine.cleanVerseText(it.text) }
            } else {
                "(${selectedVersionB} अनुवाद इस अध्याय के लिए उपलब्ध नहीं है)"
            }

            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📖 $referenceTitle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "हिंदी-इंग्लिश व संस्करण तुलना (Verse by Verse Compare)",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Version Chooser Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Version A Selector
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showVersionAPopup = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("1️⃣ $selectedVersionA", fontSize = 12.sp, maxLines = 1)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = showVersionAPopup,
                        onDismissRequest = { showVersionAPopup = false }
                    ) {
                        availableVersions.forEach { ver ->
                            DropdownMenuItem(
                                text = { Text(ver, fontWeight = if (ver == selectedVersionA) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedVersionA = ver
                                    showVersionAPopup = false
                                }
                            )
                        }
                    }
                }

                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))

                // Version B Selector
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showVersionBPopup = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("2️⃣ $selectedVersionB", fontSize = 12.sp, maxLines = 1)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = showVersionBPopup,
                        onDismissRequest = { showVersionBPopup = false }
                    ) {
                        availableVersions.forEach { ver ->
                            DropdownMenuItem(
                                text = { Text(ver, fontWeight = if (ver == selectedVersionB) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedVersionB = ver
                                    showVersionBPopup = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Copy / Share buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val combined = "$referenceTitle\n\n【$selectedVersionA】\n$textVersionA\n\n【$selectedVersionB】\n$textVersionB"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Comparison", combined))
                        Toast.makeText(context, "तुलना कॉपी हो गई!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("कॉपी करें", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val combined = "$referenceTitle\n\n【$selectedVersionA】\n$textVersionA\n\n【$selectedVersionB】\n$textVersionB"
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "$referenceTitle (Comparison):\n\n$combined\n\n(New Creation Church App)")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "तुलना शेयर करें"))
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("शेयर करें", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)
                ) {
                    // Card Version A
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF2563EB).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Version A: $selectedVersionA",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2563EB)
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Verse", "$referenceTitle ($selectedVersionA):\n$textVersionA"))
                                            Toast.makeText(context, "$selectedVersionA कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = textVersionA,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                                )
                            }
                        }
                    }

                    // Card Version B
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF059669).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Version B: $selectedVersionB",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Verse", "$referenceTitle ($selectedVersionB):\n$textVersionB"))
                                            Toast.makeText(context, "$selectedVersionB कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = textVersionB,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
