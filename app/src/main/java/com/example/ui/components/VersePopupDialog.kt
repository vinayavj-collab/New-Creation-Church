package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.bible.model.BibleTranslation
import com.example.data.bible.model.BibleVerse
import com.example.data.bible.repository.BibleRepository
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.util.DetectedVerseRef
import java.util.*

private const val PREF_NAME = "verse_popup_settings"
private const val KEY_POPUP_TRANSLATION = "default_popup_bible_id"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersePopupDialog(
    verseRef: DetectedVerseRef,
    bibleRepository: BibleRepository,
    onDismiss: () -> Unit,
    onOpenFullChapter: (bookId: Int, chapter: Int, verse: Int?) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }

    // Stored default translation
    var defaultTranslationId by remember {
        mutableStateOf(prefs.getString(KEY_POPUP_TRANSLATION, BibleTranslation.HINDI_IRV.id) ?: BibleTranslation.HINDI_IRV.id)
    }

    // Current active translation in this popup dialog
    var currentTranslationId by remember { mutableStateOf(defaultTranslationId) }

    val coroutineScope = rememberCoroutineScope()

    // Verses loaded for this chapter as a State
    val chapterVersesFlow = remember(currentTranslationId, verseRef.bookId, verseRef.chapter) {
        bibleRepository.getChapterVerses(
            translationId = currentTranslationId,
            bookId = verseRef.bookId,
            chapter = verseRef.chapter,
            coroutineScope = coroutineScope
        )
    }
    val chapterVerses by chapterVersesFlow.collectAsState(initial = emptyList<BibleVerse>())

    // Filter verses according to range (e.g. 7-10 or 16)
    val displayVerses: List<BibleVerse> = remember(chapterVerses, verseRef.startVerse, verseRef.endVerse) {
        if (chapterVerses.isEmpty()) {
            emptyList()
        } else {
            val start = verseRef.startVerse
            val end = verseRef.endVerse
            val filtered = chapterVerses.filter { it.verseNumber in start..end }
            if (filtered.isNotEmpty()) filtered else chapterVerses.take(5)
        }
    }

    // TTS Setup
    var isTtsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })

        ttsInstance = tts

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val playTtsAudio = {
        if (isSpeaking) {
            ttsInstance?.stop()
            isSpeaking = false
        } else {
            val textToRead = displayVerses.joinToString(separator = " ") { verse: BibleVerse ->
                "${verse.text} ${verse.secondaryText ?: ""}"
            }
            if (textToRead.isNotBlank() && ttsInstance != null) {
                val isHindiTrans = currentTranslationId.startsWith("HIN") || currentTranslationId == BibleTranslation.PARALLEL_HI_EN.id
                val locale = if (isHindiTrans) Locale("hi", "IN") else Locale.US
                val result = ttsInstance?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsInstance?.language = Locale.US
                }
                ttsInstance?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "popup_verse_tts")
                isSpeaking = true
            } else {
                Toast.makeText(context, "वचन लोड हो रहे हैं, कृपया प्रतीक्षा करें...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val copyVerses = {
        val fullText = displayVerses.joinToString(separator = "\n") { verse: BibleVerse ->
            "${verse.verseNumber}. ${verse.text}${if (!verse.secondaryText.isNullOrBlank()) "\n   (${verse.secondaryText})" else ""}"
        }
        val header = "${verseRef.displayLabel} (${BibleTranslation.ALL.find { it.id == currentTranslationId }?.nameHindi ?: ""})"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(verseRef.displayLabel, "$header\n\n$fullText")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "वचन कॉपी कर लिए गए", Toast.LENGTH_SHORT).show()
    }

    val shareVerses = {
        val fullText = displayVerses.joinToString(separator = "\n") { verse: BibleVerse ->
            "${verse.verseNumber}. ${verse.text}${if (!verse.secondaryText.isNullOrBlank()) "\n   (${verse.secondaryText})" else ""}"
        }
        val header = "📖 ${verseRef.displayLabel}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, verseRef.displayLabel)
            putExtra(Intent.EXTRA_TEXT, "$header\n\n$fullText\n\n— Vinay Kumar AVJ Bible App")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Scripture"))
    }

    val saveAsDefaultTranslation = { transId: String ->
        prefs.edit().putString(KEY_POPUP_TRANSLATION, transId).apply()
        defaultTranslationId = transId
        Toast.makeText(context, "डिफ़ॉल्ट पॉप-अप बाइबल सेट की गई!", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = {
            ttsInstance?.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // Header: Title + Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(NavyPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = verseRef.displayLabel,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = NavyPrimary
                            )
                            Text(
                                text = if (verseRef.startVerse != verseRef.endVerse) "वचन range: ${verseRef.startVerse} से ${verseRef.endVerse}" else "वचन: ${verseRef.startVerse}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = copyVerses) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Verses", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = shareVerses) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            ttsInstance?.stop()
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(22.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Default Translation Selector & Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BibleTranslation.ALL.forEach { trans ->
                        val isSelected = currentTranslationId == trans.id
                        val isDefault = defaultTranslationId == trans.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                currentTranslationId = trans.id
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (trans.id) {
                                            BibleTranslation.HINDI_IRV.id -> "हिन्दी (IRV)"
                                            BibleTranslation.HINDI_BSI_OV.id -> "हिन्दी (BSI)"
                                            BibleTranslation.HINDI_ERV.id -> "हिन्दी (ERV)"
                                            BibleTranslation.ENGLISH_KJV.id -> "KJV"
                                            BibleTranslation.ENGLISH_WEB.id -> "WEB"
                                            BibleTranslation.PARALLEL_HI_EN.id -> "हिन्दी + Eng"
                                            else -> trans.nameHindi
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                    if (isDefault) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Default",
                                            tint = GoldWarm,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // Default setting indicator & button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentTranslationId == defaultTranslationId) "★ डिफ़ॉल्ट पॉप-अप बाइबल" else "अनुवाद: ${BibleTranslation.ALL.find { it.id == currentTranslationId }?.nameHindi ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (currentTranslationId != defaultTranslationId) {
                        TextButton(
                            onClick = { saveAsDefaultTranslation(currentTranslationId) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.StarBorder, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldWarm)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("डिफ़ॉल्ट बनाएं", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Verses List Body
                if (displayVerses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = NavyPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("वचन लोड हो रहे हैं...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayVerses, key = { "${it.bookId}_${it.chapter}_${it.verseNumber}_${it.translationId}" }) { verse ->
                            VerseItemRow(verse = verse)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Bottom Controls: Text-To-Speech (Audio) + Go to Full Chapter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio / TTS button
                    OutlinedButton(
                        onClick = { playTtsAudio() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSpeaking) NavyPrimary.copy(alpha = 0.1f) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            if (isSpeaking) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = if (isSpeaking) Color(0xFFE11D48) else NavyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSpeaking) "ऑडियो रोकें" else "ऑडियो सुनें (TTS)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (isSpeaking) Color(0xFFE11D48) else NavyPrimary
                        )
                    }

                    // Jump to full chapter button
                    Button(
                        onClick = {
                            ttsInstance?.stop()
                            onOpenFullChapter(verseRef.bookId, verseRef.chapter, verseRef.startVerse)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp), tint = GoldWarm)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "पूरा अध्याय पढ़ें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseItemRow(verse: BibleVerse) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyPrimary.copy(alpha = 0.04f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Verse Number Badge
            Surface(
                shape = CircleShape,
                color = NavyPrimary,
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${verse.verseNumber}",
                        color = GoldWarm,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = verse.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Parallel secondary text if available
                if (!verse.secondaryText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = verse.secondaryText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
