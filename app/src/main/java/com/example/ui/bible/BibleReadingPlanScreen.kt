package com.example.ui.bible

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.bible.local.ReadingPlanProgressEntity
import com.example.data.bible.model.*
import com.example.data.bible.repository.ReadingPlanRepository
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReadingPlanScreen(
    planRepository: ReadingPlanRepository,
    onBackClick: () -> Unit,
    onOpenBible: (bookId: Int, chapter: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val plans = remember { planRepository.getAllPlans() }
    var selectedPlan by remember { mutableStateOf<ReadingPlanInfo?>(null) }
    var progressList by remember { mutableStateOf<List<ReadingPlanProgressEntity>>(emptyList()) }

    // Listen to progress for selected plan
    LaunchedEffect(selectedPlan?.id) {
        val id = selectedPlan?.id
        if (id != null) {
            planRepository.getPlanProgress(id).collect { list ->
                progressList = list
            }
        }
    }

    val completedDaysMap = remember(progressList) {
        progressList.filter { it.isCompleted }.associateBy { it.dayNumber }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedPlan?.titleEnglish ?: "Bible Reading Plans",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedPlan != null) {
                            selectedPlan = null
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedPlan != null) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                planRepository.resetPlan(selectedPlan!!.id)
                            }
                        }) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Plan")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (selectedPlan == null) {
            // List of available plans
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Choose a Bible Reading Plan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Build a consistent daily Bible reading habit with structured guides.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(plans) { plan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlan = plan },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(NavyPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = GoldWarm,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plan.titleEnglish,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = plan.titleHindi,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                                    )
                                }

                                SuggestionChip(
                                    onClick = { selectedPlan = plan },
                                    label = { Text("${plan.totalDays} Days", fontWeight = FontWeight.SemiBold) }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = plan.descriptionEnglish,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // Plan Day Detail & Progress
            val currentPlan = selectedPlan!!
            val completedCount = completedDaysMap.size
            val progressPercent = if (currentPlan.totalDays > 0) (completedCount.toFloat() / currentPlan.totalDays) else 0f

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = currentPlan.titleEnglish,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = currentPlan.titleHindi,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$completedCount of ${currentPlan.totalDays} days completed",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${(progressPercent * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "DAILY READINGS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(currentPlan.days) { day ->
                    val isDone = completedDaysMap.containsKey(day.dayNumber)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = { checked ->
                                    coroutineScope.launch {
                                        planRepository.markDayCompleted(currentPlan.id, day.dayNumber, checked)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = day.title,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                day.portions.forEach { portion ->
                                    Text(
                                        text = "${portion.displayEnglish} (${portion.displayHindi})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (day.portions.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        val firstPortion = day.portions.first()
                                        onOpenBible(firstPortion.bookId, firstPortion.startChapter)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Read", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
