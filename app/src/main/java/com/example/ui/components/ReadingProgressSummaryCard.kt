package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.ReadingInsightsData
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReadingProgressSummaryCard(
    insights: ReadingInsightsData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)
    val versesFormatted = numberFormatter.format(insights.totalVersesRead)

    val infiniteTransition = rememberInfiniteTransition(label = "streak_pulse")
    val fireScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (insights.currentStreak > 0) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("home_reading_progress_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: "आपकी प्रगति (Your Progress)" and View Details ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "आपकी प्रगति (Your Progress)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            text = "रीडिंग इनसाइट्स व स्ट्रीक (Reading Insights)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "डैशबोर्ड",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Insights Dashboard",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two-column metric highlight row (Streak 🔥 & Total Verses 📖)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Streak Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFFAF0),
                                    Color(0xFFFFF3E0)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFFFB74D).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 22.sp,
                            modifier = Modifier.scale(fireScale)
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "${insights.currentStreak}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFE65100),
                                        fontSize = 18.sp
                                    )
                                )
                                Text(
                                    text = if (insights.currentStreak == 1) "Day" else "Days",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFBF360C),
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            Text(
                                text = "दैनिक स्ट्रीक (Streak)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF8D6E63),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Total Verses Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF0FDF4),
                                    Color(0xFFDCFCE7)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFF86EFAC).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = versesFormatted,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF166534),
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "कुल आयतें (Verses Read)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF15803D),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            // Mini 7-day visual activity dots
            if (insights.weeklyStats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    insights.weeklyStats.takeLast(7).forEach { dayStat ->
                        val hasRead = dayStat.versesRead > 0
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasRead) Color(0xFF10B981)
                                        else if (dayStat.isToday) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        width = if (dayStat.isToday) 1.5.dp else 0.dp,
                                        color = if (dayStat.isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasRead) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Text(
                                text = dayStat.dayLabel.take(1),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = if (dayStat.isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (dayStat.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
