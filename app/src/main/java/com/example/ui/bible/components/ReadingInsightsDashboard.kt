package com.example.ui.bible.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.PlanBreakdownStat
import com.example.data.bible.model.ReadingDayStat
import com.example.data.bible.model.ReadingInsightsData
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import java.text.NumberFormat
import java.util.Locale

enum class InsightsTimeframe(val titleHindi: String, val titleEnglish: String) {
    WEEKLY("साप्ताहिक", "Weekly"),
    MONTHLY("मासिक", "Monthly"),
    YEARLY("वार्षिक", "Yearly"),
    TOTAL("कुल", "Total")
}

@Composable
fun ReadingInsightsDashboard(
    insights: ReadingInsightsData,
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf(InsightsTimeframe.WEEKLY) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reading_insights_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card (Streak & Overall Activity)
        item {
            InsightsHeroCard(insights = insights)
        }

        // Timeframe Selector Tabs (Weekly, Monthly, Yearly, Total)
        item {
            TimeframeSelectorRow(
                selected = selectedTimeframe,
                onSelect = { selectedTimeframe = it }
            )
        }

        // Main Graph & Visual Analytics Canvas
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when (selectedTimeframe) {
                                    InsightsTimeframe.WEEKLY -> "साप्ताहिक पठन विश्लेषण (Weekly Reading Activity)"
                                    InsightsTimeframe.MONTHLY -> "मासिक प्रगति चार्ट (Monthly Progress Chart)"
                                    InsightsTimeframe.YEARLY -> "वार्षिक रुझान (Yearly Reading Trend)"
                                    InsightsTimeframe.TOTAL -> "कुल अध्ययन अवलोकन (All-Time Bible Overview)"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = when (selectedTimeframe) {
                                    InsightsTimeframe.WEEKLY -> "पिछले 7 दिनों में पढ़ी गई आयतें"
                                    InsightsTimeframe.MONTHLY -> "गत 4 सप्ताहों की पठन प्रगति"
                                    InsightsTimeframe.YEARLY -> "12 महीनों का पठन विवरण"
                                    InsightsTimeframe.TOTAL -> "पुराना नियम एवं नया नियम का विवरण"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedContent(
                        targetState = selectedTimeframe,
                        label = "chart_transition"
                    ) { tf ->
                        when (tf) {
                            InsightsTimeframe.WEEKLY -> {
                                CanvasBarChart(
                                    dataList = insights.weeklyStats,
                                    barColor = NavyPrimary,
                                    accentColor = GoldWarm,
                                    yAxisLabel = "Verses"
                                )
                            }
                            InsightsTimeframe.MONTHLY -> {
                                CanvasBarChart(
                                    dataList = insights.monthlyStats,
                                    barColor = Color(0xFF0284C7),
                                    accentColor = Color(0xFF38BDF8),
                                    yAxisLabel = "Verses"
                                )
                            }
                            InsightsTimeframe.YEARLY -> {
                                CanvasBarChart(
                                    dataList = insights.yearlyStats,
                                    barColor = Color(0xFF7C3AED),
                                    accentColor = Color(0xFFA78BFA),
                                    yAxisLabel = "Verses"
                                )
                            }
                            InsightsTimeframe.TOTAL -> {
                                TotalBibleBreakdownChart(insights = insights)
                            }
                        }
                    }
                }
            }
        }

        // Key Milestone Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "कुल अध्याय (Chapters)",
                    value = numberFormat.format(insights.totalChaptersRead),
                    icon = Icons.Default.BookmarkAdded,
                    iconTint = NavyPrimary,
                    containerColor = NavyPrimary.copy(alpha = 0.08f)
                )

                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "सर्वश्रेष्ठ स्ट्रीक (Longest)",
                    value = "${insights.longestStreak} दिन",
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = Color(0xFFEA580C),
                    containerColor = Color(0xFFFFF7ED)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "सक्रिय प्लान्स (Active Plans)",
                    value = "${insights.totalActivePlans}",
                    icon = Icons.Default.AutoStories,
                    iconTint = Color(0xFF059669),
                    containerColor = Color(0xFFECFDF5)
                )

                MetricSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "पूर्ण प्लान्स (Completed)",
                    value = "${insights.totalPlansCompleted}",
                    icon = Icons.Default.CheckCircle,
                    iconTint = GoldWarm,
                    containerColor = GoldWarm.copy(alpha = 0.12f)
                )
            }
        }

        // Plan-by-Plan Performance Breakdown Section
        if (insights.planBreakdowns.isNotEmpty()) {
            item {
                Text(
                    text = "प्लान अनुसार प्रगति (Plan-by-Plan Breakdown)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(insights.planBreakdowns, key = { it.planId }) { planStat ->
                PlanBreakdownCard(planStat = planStat)
            }
        }
    }
}

@Composable
fun InsightsHeroCard(
    insights: ReadingInsightsData,
    modifier: Modifier = Modifier
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = NavyPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "बाइबल अध्ययन सांख्यिकी",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Bible Reading Insights & Milestones",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }

                // Flame streak badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldWarm)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🔥", fontSize = 16.sp)
                        Text(
                            text = "${insights.currentStreak} Days",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NavyPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Large Verses Read Display
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = numberFormat.format(insights.totalVersesRead),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp
                    )
                )
                Text(
                    text = "कुल आयतें पढ़ी गईं (Verses Read)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = GoldWarm,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Status Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(
                    icon = Icons.Default.EventAvailable,
                    label = "${insights.totalDaysCompleted} अध्ययन सत्र",
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    icon = Icons.Default.MenuBook,
                    label = "${insights.totalChaptersRead} कुल अध्याय",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldWarm,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun TimeframeSelectorRow(
    selected: InsightsTimeframe,
    onSelect: (InsightsTimeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        InsightsTimeframe.entries.forEach { tf ->
            val isSelected = tf == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(tf) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${tf.titleHindi}\n(${tf.titleEnglish})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp
                    )
                )
            }
        }
    }
}

@Composable
fun CanvasBarChart(
    dataList: List<ReadingDayStat>,
    barColor: Color,
    accentColor: Color,
    yAxisLabel: String,
    modifier: Modifier = Modifier
) {
    if (dataList.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("कोई डेटा उपलब्ध नहीं है (No Data Available)", color = Color.Gray)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxVal = remember(dataList) {
        val max = dataList.maxOfOrNull { it.versesRead } ?: 0
        if (max > 0) (max * 1.25f).toInt() else 100
    }

    val animatedProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(dataList) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip display on tap
        if (selectedIndex != null && selectedIndex!! in dataList.indices) {
            val item = dataList[selectedIndex!!]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.dayLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${item.versesRead} आयतें (${item.chaptersRead} अध्याय)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = barColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(dataList) {
                    detectTapGestures { offset ->
                        val barWidth = size.width / dataList.size
                        val idx = (offset.x / barWidth).toInt().coerceIn(0, dataList.size - 1)
                        selectedIndex = if (selectedIndex == idx) null else idx
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val bottomPadding = 24.dp.toPx()
                val topPadding = 16.dp.toPx()
                val chartHeight = h - bottomPadding - topPadding

                val count = dataList.size
                val slotWidth = w / count
                val barWidth = (slotWidth * 0.52f).coerceAtLeast(8.dp.toPx())

                // Draw background grid lines (3 horizontal levels)
                val gridPaintColor = Color.LightGray.copy(alpha = 0.35f)
                for (step in 1..3) {
                    val y = topPadding + chartHeight * (step / 3f)
                    drawLine(
                        color = gridPaintColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw bars and x-axis labels
                dataList.forEachIndexed { i, stat ->
                    val centerX = i * slotWidth + slotWidth / 2f
                    val normalizedValue = (stat.versesRead.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                    val barH = (normalizedValue * chartHeight * animatedProgress.value).coerceAtLeast(
                        if (stat.versesRead > 0) 6.dp.toPx() else 2.dp.toPx()
                    )
                    val barTop = topPadding + chartHeight - barH
                    val barLeft = centerX - barWidth / 2f

                    val isSelected = selectedIndex == i
                    val isToday = stat.isToday

                    val brush = if (stat.versesRead > 0) {
                        Brush.verticalGradient(
                            colors = if (isSelected || isToday) {
                                listOf(accentColor, barColor)
                            } else {
                                listOf(barColor.copy(alpha = 0.8f), barColor)
                            },
                            startY = barTop,
                            endY = topPadding + chartHeight
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color.LightGray.copy(alpha = 0.3f), Color.LightGray.copy(alpha = 0.2f))
                        )
                    }

                    // Rounded top corners for bars
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Draw highlight indicator dot for today
                    if (isToday) {
                        drawCircle(
                            color = accentColor,
                            radius = 3.dp.toPx(),
                            center = Offset(centerX, barTop - 6.dp.toPx())
                        )
                    }

                    // Draw label text using native canvas
                    val labelText = stat.dayLabel.take(3)
                    val paint = android.graphics.Paint().apply {
                        color = if (isToday) barColor.toArgb() else android.graphics.Color.GRAY
                        textSize = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        if (isToday || isSelected) {
                            isFakeBoldText = true
                        }
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        labelText,
                        centerX,
                        h - 4.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}

@Composable
fun TotalBibleBreakdownChart(
    insights: ReadingInsightsData,
    modifier: Modifier = Modifier
) {
    val totalVerses = (insights.otVersesRead + insights.ntVersesRead).coerceAtLeast(1)
    val otFraction = (insights.otVersesRead.toFloat() / totalVerses).coerceIn(0f, 1f)
    val ntFraction = (insights.ntVersesRead.toFloat() / totalVerses).coerceIn(0f, 1f)
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Dual Progress Bar for OT vs NT
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NavyPrimary))
                    Text(
                        text = "पुराना नियम (Old Testament)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "${numberFormat.format(insights.otVersesRead)} आयतें (${(otFraction * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall.copy(color = NavyPrimary, fontWeight = FontWeight.Bold)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (otFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(otFraction)
                                .background(NavyPrimary)
                        )
                    }
                    if (ntFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .background(GoldWarm)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(GoldWarm))
                    Text(
                        text = "नया नियम (New Testament)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "${numberFormat.format(insights.ntVersesRead)} आयतें (${(ntFraction * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall.copy(color = GoldWarm, fontWeight = FontWeight.Bold)
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Total Summary Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${insights.totalDaysCompleted}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyPrimary
                    )
                )
                Text(
                    text = "सक्रिय दिन (Active Days)",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = numberFormat.format(insights.totalVersesRead),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF16A34A)
                    )
                )
                Text(
                    text = "कुल पढ़ी गई आयतें",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${insights.totalChaptersRead}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD97706)
                    )
                )
                Text(
                    text = "कुल अध्याय (Chapters)",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }
        }
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
fun PlanBreakdownCard(
    planStat: PlanBreakdownStat,
    modifier: Modifier = Modifier
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = planStat.titleHindi,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = planStat.titleEnglish,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (planStat.progressPercent >= 100) Color(0xFF10B981).copy(alpha = 0.15f)
                            else NavyPrimary.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${planStat.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (planStat.progressPercent >= 100) Color(0xFF10B981) else NavyPrimary
                        )
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { (planStat.progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (planStat.progressPercent >= 100) Color(0xFF10B981) else NavyPrimary,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${planStat.completedDays}/${planStat.totalDays} दिन पूर्ण",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "${numberFormat.format(planStat.versesRead)} आयतें",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = NavyPrimary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
