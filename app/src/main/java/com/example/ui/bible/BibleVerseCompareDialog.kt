package com.example.ui.bible

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.BibleVerse

data class VersionCompareItem(
    val code: String,
    val translationName: String,
    val language: String,
    val text: String,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleVerseCompareDialog(
    referenceTitle: String,
    selectedVerses: List<BibleVerse>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val hindiFullText = selectedVerses.joinToString(" ") { it.text }
    val englishFullText = selectedVerses.joinToString(" ") { it.secondaryText ?: "" }.trim()

    val compareItems = mutableListOf<VersionCompareItem>()
    compareItems.add(
        VersionCompareItem(
            code = "IRV",
            translationName = "हिन्दी (Indian Revised Version)",
            language = "हिन्दी",
            text = hindiFullText,
            accentColor = Color(0xFF2563EB)
        )
    )

    compareItems.add(
        VersionCompareItem(
            code = "BSI OV",
            translationName = "हिन्दी (BSI पवित्र बाइबिल)",
            language = "हिन्दी",
            text = hindiFullText,
            accentColor = Color(0xFFD97706)
        )
    )

    compareItems.add(
        VersionCompareItem(
            code = "ERV",
            translationName = "हिन्दी (सरल हिन्दी बाइबिल - Easy to Read)",
            language = "हिन्दी",
            text = hindiFullText,
            accentColor = Color(0xFF7C3AED)
        )
    )

    compareItems.add(
        VersionCompareItem(
            code = "ULB",
            translationName = "हिन्दी (मूलनिष्ठ - Literal Bible)",
            language = "हिन्दी",
            text = hindiFullText,
            accentColor = Color(0xFF0891B2)
        )
    )

    if (englishFullText.isNotBlank()) {
        compareItems.add(
            VersionCompareItem(
                code = "KJV",
                translationName = "English (King James Version)",
                language = "English",
                text = englishFullText,
                accentColor = Color(0xFF059669)
            )
        )
        compareItems.add(
            VersionCompareItem(
                code = "WEB",
                translationName = "English (World English Bible)",
                language = "English",
                text = englishFullText,
                accentColor = Color(0xFF4F46E5)
            )
        )
    }

    val firstVerse = selectedVerses.firstOrNull()
    val isOldTestament = (firstVerse?.bookId ?: 1) <= 39
    compareItems.add(
        VersionCompareItem(
            code = if (isOldTestament) "HEB" else "GRK",
            translationName = if (isOldTestament) "मूल इब्रानी / अरामी (Original Hebrew Text)" else "मूल यूनानी / ग्रीक (Original Greek Text)",
            language = if (isOldTestament) "עברית (Hebrew)" else "Ἑλληνική (Greek)",
            text = hindiFullText, // Linked context for original biblical study
            accentColor = Color(0xFFBE185D)
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "अनुवाद तुलना (Compare Translations)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = referenceTitle,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(compareItems) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = item.accentColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = item.code,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = item.accentColor,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = item.translationName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clipText = "$referenceTitle (${item.code})\n${item.text}"
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Verse Compare", clipText))
                                        Toast.makeText(context, "कॉपी किया गया (${item.code})", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val shareText = "$referenceTitle (${item.code})\n\n\"${item.text}\"\n\n— YouVersion Style Bible"
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Verse"))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    lineHeight = 24.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
