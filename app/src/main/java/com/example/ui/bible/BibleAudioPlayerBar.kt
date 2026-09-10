package com.example.ui.bible

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.BibleBook
import com.example.data.bible.model.BibleVerse
import com.example.util.AudioSourceType
import com.example.util.BibleAudioManager
import java.util.Locale

@Composable
fun BibleAudioPlayerBar(
    audioManager: BibleAudioManager,
    currentBook: BibleBook,
    currentChapter: Int,
    verses: List<BibleVerse>,
    modifier: Modifier = Modifier
) {
    val isPlaying by audioManager.isPlaying.collectAsState()
    val isBuffering by audioManager.isBuffering.collectAsState()
    val audioLanguage by audioManager.audioLanguage.collectAsState()
    val audioSourceType by audioManager.audioSourceType.collectAsState()
    val playbackSpeed by audioManager.playbackSpeed.collectAsState()
    val currentPosMs by audioManager.currentPositionMs.collectAsState()
    val durationMs by audioManager.durationMs.collectAsState()
    val activeVerseNum by audioManager.currentVerseNumber.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Mini Player Bar (Always Visible)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Audio Bible",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (audioLanguage == "en") "${currentBook.nameEnglish} $currentChapter" else "${currentBook.nameHindi} - अध्याय $currentChapter",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (audioLanguage == "en") "Audio: English" else "ऑडियो: हिन्दी",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )
                            )
                            if (activeVerseNum != null) {
                                Text(
                                    text = " • Verse $activeVerseNum",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    audioManager.playPause()
                                } else {
                                    audioManager.playChapterAudio(currentBook.id, currentChapter, verses)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (isExpanded) "Collapse" else "Expand controls"
                        )
                    }
                }
            }

            // Expanded Controls
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Audio Language Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "भाषा (Audio Language):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = audioLanguage == "hi",
                                onClick = { audioManager.setAudioLanguage("hi") },
                                label = { Text("हिन्दी (Hindi)", fontSize = 12.sp) },
                                leadingIcon = if (audioLanguage == "hi") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                            FilterChip(
                                selected = audioLanguage == "en",
                                onClick = { audioManager.setAudioLanguage("en") },
                                label = { Text("English", fontSize = 12.sp) },
                                leadingIcon = if (audioLanguage == "en") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Audio Source Type Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "स्रोतः",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = audioSourceType == AudioSourceType.PRE_RECORDED,
                                onClick = { audioManager.setAudioSourceType(AudioSourceType.PRE_RECORDED) },
                                label = { Text("MP3 Stream", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = audioSourceType == AudioSourceType.TTS_NARRATION,
                                onClick = { audioManager.setAudioSourceType(AudioSourceType.TTS_NARRATION) },
                                label = { Text("वॉइस (TTS)", fontSize = 11.sp) }
                            )
                        }
                    }

                    // Progress Slider (if duration available for MP3 mode)
                    if (audioSourceType == AudioSourceType.PRE_RECORDED && durationMs > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = currentPosMs.coerceAtMost(durationMs).toFloat(),
                                onValueChange = { audioManager.seekTo(it.toLong()) },
                                valueRange = 0f..durationMs.toFloat(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = formatMs(currentPosMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatMs(durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Main Controls Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { audioManager.skipPreviousVerse() }) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s / Prev",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                if (isPlaying) {
                                    audioManager.playPause()
                                } else {
                                    audioManager.playChapterAudio(currentBook.id, currentChapter, verses)
                                }
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(onClick = { audioManager.skipNextVerse() }) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s / Next",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(onClick = { audioManager.stop() }) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Speed Chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "गति (Speed): ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                            val isSpeedSelected = playbackSpeed == speed
                            SuggestionChip(
                                onClick = { audioManager.setPlaybackSpeed(speed) },
                                label = {
                                    Text(
                                        text = "${speed}x",
                                        fontWeight = if (isSpeedSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                border = if (isSpeedSelected) SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = 1.5.dp
                                ) else SuggestionChipDefaults.suggestionChipBorder(enabled = true),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSecs = (ms / 1000).toInt()
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
