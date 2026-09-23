package com.example.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyAudioDevotional

@Composable
fun DailyAudioDevotionalCard(
    devotional: DailyAudioDevotional,
    modifier: Modifier = Modifier
) {
    if (devotional.audioUrl.isBlank()) return

    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    DisposableEffect(devotional.audioUrl) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {}
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "आज का आत्मिक मनन",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                        if (devotional.durationText.isNotBlank()) {
                            Text(
                                text = "• ${devotional.durationText}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = devotional.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )

                    if (devotional.speaker.isNotBlank()) {
                        Text(
                            text = devotional.speaker,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = {
                    if (isPlaying) {
                        try {
                            mediaPlayer?.pause()
                            isPlaying = false
                        } catch (e: Exception) {}
                    } else {
                        if (mediaPlayer == null) {
                            isLoading = true
                            try {
                                val mp = MediaPlayer().apply {
                                    setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                            .build()
                                    )
                                    setDataSource(devotional.audioUrl)
                                    setOnPreparedListener {
                                        isLoading = false
                                        it.start()
                                        isPlaying = true
                                    }
                                    setOnCompletionListener {
                                        isPlaying = false
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        isLoading = false
                                        isPlaying = false
                                        true
                                    }
                                    prepareAsync()
                                }
                                mediaPlayer = mp
                            } catch (e: Exception) {
                                isLoading = false
                            }
                        } else {
                            try {
                                mediaPlayer?.start()
                                isPlaying = true
                            } catch (e: Exception) {}
                        }
                    }
                },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "रोकें" else "चलाएं"
                    )
                }
            }
        }
    }
}
