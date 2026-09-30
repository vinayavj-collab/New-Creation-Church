package com.example.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import com.example.data.model.AudioMessageConfig
import com.example.data.model.DailyAudioDevotional
import com.example.data.model.DailyDevotion
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DailyAudioDevotionalCard(
    devotional: DailyAudioDevotional?,
    dailyDevotions: List<DailyDevotion> = emptyList(),
    config: AudioMessageConfig = AudioMessageConfig(),
    onListenStarted: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!config.isServiceActive) return

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    val todaysDevotions = remember(dailyDevotions, todayDate) {
        dailyDevotions.filter { it.scheduledDate == todayDate && (it.status == "scheduled" || it.status == "published") }
            .sortedBy { it.slotIndex }
    }
    val pinnedDevotion = remember(dailyDevotions) {
        dailyDevotions.firstOrNull { it.isPinned }
    }

    var selectedSlotIndex by remember { mutableIntStateOf(1) }

    val activeDevotion: DailyDevotion? = remember(todaysDevotions, pinnedDevotion, selectedSlotIndex) {
        todaysDevotions.find { it.slotIndex == selectedSlotIndex }
            ?: todaysDevotions.firstOrNull()
            ?: pinnedDevotion
    }

    val isLockedByTime = remember(activeDevotion, currentTime, config.dailyPublishTime) {
        if (activeDevotion == null) false
        else if (activeDevotion.isPinned) false
        else if (activeDevotion.scheduledDate < todayDate) false
        else if (activeDevotion.scheduledDate == todayDate) {
            currentTime < config.dailyPublishTime
        } else true
    }

    val audioUrlToPlay = remember(activeDevotion, devotional, config.fallbackAudioUrl) {
        activeDevotion?.audioUrl?.ifBlank { null }
            ?: devotional?.audioUrl?.ifBlank { null }
            ?: config.fallbackAudioUrl.ifBlank { null }
    }

    if (audioUrlToPlay.isNullOrBlank() && activeDevotion == null) return

    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasRecordedListen by remember { mutableStateOf(false) }

    DisposableEffect(audioUrlToPlay) {
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "दैनिक आत्मिक संदेश",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                        Text(
                            text = if (activeDevotion?.isPinned == true) "📌 विशेष संदेश • ${activeDevotion.pinnedReason.ifBlank { "पिन किया गया" }}"
                            else activeDevotion?.slotLabel ?: "आज का आत्मिक मनन",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (todaysDevotions.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        todaysDevotions.forEach { dev ->
                            FilterChip(
                                selected = selectedSlotIndex == dev.slotIndex,
                                onClick = {
                                    if (selectedSlotIndex != dev.slotIndex) {
                                        try {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                        } catch (e: Exception) {}
                                        mediaPlayer = null
                                        isPlaying = false
                                        selectedSlotIndex = dev.slotIndex
                                    }
                                },
                                label = { Text(if (dev.slotIndex == 1) "सुबह" else "शाम", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLockedByTime) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.LockClock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "आज का संदेश ${config.dailyPublishTime} बजे प्रसारित होगा",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "प्रसारण समय होते ही ऑडियो स्वतः अनलॉक हो जाएगा",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        if (activeDevotion != null && activeDevotion.speakerName.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎙️ वक्ता: ${activeDevotion.speakerName} ${if (activeDevotion.speakerRole.isNotBlank()) "(${activeDevotion.speakerRole})" else ""}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (activeDevotion.title.isNotBlank()) {
                                    Text(
                                        text = activeDevotion.title,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeDevotion?.title ?: devotional?.title ?: "दैनिक आत्मिक मनन",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )

                        if (!activeDevotion?.scriptureRef.isNullOrBlank()) {
                            Text(
                                text = "📖 ${activeDevotion?.scriptureRef}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val speakerText = activeDevotion?.speakerName ?: devotional?.speaker ?: ""
                        if (speakerText.isNotBlank()) {
                            Text(
                                text = "🎙️ $speakerText",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 1
                            )
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
                                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                                    .build()
                                            )
                                            setDataSource(audioUrlToPlay)
                                            setOnPreparedListener {
                                                isLoading = false
                                                it.start()
                                                isPlaying = true
                                                if (!hasRecordedListen && activeDevotion != null) {
                                                    hasRecordedListen = true
                                                    onListenStarted(activeDevotion.devotionId)
                                                }
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
                        modifier = Modifier.size(42.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
