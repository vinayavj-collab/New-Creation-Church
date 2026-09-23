package com.example.ui.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * High-craft visual countdown timer specifically for Password 2 (OTP) fields.
 * Shows exact mm:ss countdown, circular/linear progress indicator, active/expiring/expired color cues,
 * and pulsing warnings when expiring soon (< 2 minutes).
 */
@Composable
fun OtpCountdownTimer(
    timestamp: Long,
    totalDurationMs: Long = 600000L, // 10 minutes default
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    // Current ticker that updates every second
    var currentTickerTime by remember(timestamp) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(timestamp) {
        while (true) {
            currentTickerTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val elapsedMs = (currentTickerTime - timestamp).coerceAtLeast(0L)
    val remainingMs = (totalDurationMs - elapsedMs).coerceAtLeast(0L)
    val isValid = timestamp > 0 && remainingMs > 0
    val progressFraction = if (isValid) (remainingMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val remainingMinutes = (remainingMs / 60000L)
    val remainingSeconds = ((remainingMs % 60000L) / 1000L)

    // Urgency level
    val isCritical = isValid && remainingMinutes < 2 // Less than 2 mins
    val isWarning = isValid && remainingMinutes < 4 // Less than 4 mins

    // Subtle pulse animation when critical
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_urgency")
    val pulseScale by if (isCritical) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val statusColor by animateColorAsState(
        targetValue = when {
            !isValid -> MaterialTheme.colorScheme.error
            isCritical -> Color(0xFFEF4444) // Bright Red
            isWarning -> Color(0xFFF59E0B) // Amber
            else -> Color(0xFF10B981) // Emerald Green
        },
        label = "status_color"
    )

    val containerBg by animateColorAsState(
        targetValue = when {
            !isValid -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            isCritical -> Color(0xFFEF4444).copy(alpha = 0.12f)
            isWarning -> Color(0xFFF59E0B).copy(alpha = 0.12f)
            else -> Color(0xFF10B981).copy(alpha = 0.12f)
        },
        label = "container_bg"
    )

    if (isCompact) {
        // Compact single-line pill countdown
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = containerBg,
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
            modifier = modifier.testTag("otp_countdown_compact")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (isValid) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "%02d:%02d".format(remainingMinutes, remainingSeconds),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isCritical) "जल्द समाप्त!" else "वैध",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                } else {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "समाप्त (00:00)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    } else {
        // Rich visual countdown block with Circular / Linear Progress & Detailed Time
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = containerBg,
            border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.6f)),
            modifier = modifier
                .fillMaxWidth()
                .testTag("otp_countdown_visual_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Circular mini-dial indicator
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .scale(pulseScale)
                        ) {
                            CircularProgressIndicator(
                                progress = { if (isValid) progressFraction else 0f },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 3.5.dp,
                                color = statusColor,
                                trackColor = statusColor.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                            Icon(
                                imageVector = if (isValid) Icons.Default.LockClock else Icons.Default.Warning,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isValid) "OTP वैधता उलटी गिनती (10-Min Timer)" else "समय सीमा समाप्त (Expired)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = if (isValid) {
                                    if (isCritical) "⚠️ ध्यान दें: OTP समाप्त होने वाला है!"
                                    else "पासवर्ड 2 स्वतः समाप्त होने में समय शेष"
                                } else "नया पासवर्ड 2 जनरेट करने की आवश्यकता है",
                                fontSize = 10.sp,
                                color = if (isCritical || !isValid) statusColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Large Digital Clock Display
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isValid) "%02d:%02d".format(remainingMinutes, remainingSeconds) else "00:00",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = statusColor
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "मिनट",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Linear bar reflecting precise elapsed percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${(progressFraction * 100).toInt()}% शेष",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }
            }
        }
    }
}
