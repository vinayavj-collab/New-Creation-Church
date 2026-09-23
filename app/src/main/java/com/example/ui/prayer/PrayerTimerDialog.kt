package com.example.ui.prayer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.util.AmbientWorshipAudio
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimerDialog(
    onDismissRequest: () -> Unit,
    onPrayerCompleted: () -> Unit
) {
    val context = LocalContext.current
    val isAmbientPlaying by com.example.util.BackgroundMusicManager.isPlaying.collectAsState()
    val currentTrack = remember { com.example.util.BackgroundMusicManager.getSelectedPrayerTrack() }

    val durations = listOf(5, 10, 15, 30) // in minutes
    var selectedMinutes by remember { mutableIntStateOf(15) }
    var totalSeconds by remember { mutableIntStateOf(selectedMinutes * 60) }
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    // Keep screen on during prayer timer
    DisposableEffect(isRunning) {
        val window = (context as? ComponentActivity)?.window
        if (isRunning) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Timer countdown effect
    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        } else if (isRunning && remainingSeconds == 0) {
            isRunning = false
            isCompleted = true
            onPrayerCompleted()

            // Gentle haptic feedback
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    @Suppress("DEPRECATION")
                    v?.vibrate(800)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val progress = if (totalSeconds > 0) {
        (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()
    } else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timerProgress")

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = GoldWarm,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "शांत प्रार्थना टाइमर",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "बंद करें")
                    }
                }

                Text(
                    text = "प्रभु की उपस्थिति में शांत मनन व एकांत प्रार्थना का समय",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Duration Selector Chips (only when not running)
                if (!isRunning && !isCompleted) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        durations.forEach { d ->
                            FilterChip(
                                selected = selectedMinutes == d,
                                onClick = {
                                    selectedMinutes = d
                                    totalSeconds = d * 60
                                    remainingSeconds = totalSeconds
                                },
                                label = { Text("$d मिनट") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldWarm.copy(alpha = 0.25f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Countdown Dial Box
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    NavyDark,
                                    NavyPrimary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(0.92f),
                        color = GoldWarm,
                        trackColor = Color.White.copy(alpha = 0.15f),
                        strokeWidth = 6.dp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GoldWarm,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "आमीन! 🙌",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        } else {
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 38.sp
                                )
                            )
                            Text(
                                text = if (isRunning) "प्रार्थना जारी है..." else "शांत प्रार्थना",
                                style = MaterialTheme.typography.labelSmall.copy(color = GoldWarm)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Ambient Music Toggle within Timer
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { com.example.util.BackgroundMusicManager.togglePrayerMusic(context) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAmbientPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                contentDescription = null,
                                tint = if (isAmbientPlaying) GoldWarm else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "शांत आराधना संगीत",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (isAmbientPlaying) "बज रहा है: ${currentTrack.title}" else "बंद है (चालू करने के लिए टैप करें)",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 1
                                )
                            }
                        }
                        Switch(
                            checked = isAmbientPlaying,
                            onCheckedChange = { com.example.util.BackgroundMusicManager.togglePrayerMusic(context) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Timer Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCompleted) {
                        Button(
                            onClick = {
                                remainingSeconds = totalSeconds
                                isCompleted = false
                                isRunning = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("पुनः प्रारंभ करें")
                        }
                        Button(
                            onClick = onDismissRequest,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldWarm)
                        ) {
                            Text("पूर्ण हुआ")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                isRunning = false
                                remainingSeconds = totalSeconds
                            },
                            enabled = remainingSeconds != totalSeconds
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("रीसेट")
                        }

                        Button(
                            onClick = { isRunning = !isRunning },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRunning) "रोकें (Pause)" else "प्रार्थना शुरू करें")
                        }
                    }
                }
            }
        }
    }
}
