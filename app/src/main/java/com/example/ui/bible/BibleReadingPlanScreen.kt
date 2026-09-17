package com.example.ui.bible

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.local.ReadingPlanProgressEntity
import com.example.data.bible.model.*
import com.example.data.bible.repository.ReadingPlanRepository
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.launch
import kotlin.random.Random

fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        val cleanedHex = hex.removePrefix("#")
        val colorInt = cleanedHex.toLong(16)
        if (cleanedHex.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        defaultColor
    }
}

enum class ReadingTabMode {
    PLANS,
    INSIGHTS
}

sealed class PasswordAction {
    data class DEACTIVATE(val plan: ReadingPlanInfo) : PasswordAction()
    data class RESET(val plan: ReadingPlanInfo) : PasswordAction()
    data class DELETE(val plan: ReadingPlanInfo) : PasswordAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReadingPlanScreen(
    planRepository: ReadingPlanRepository,
    onBackClick: () -> Unit,
    onOpenBible: (bookId: Int, chapter: Int, startVerse: Int?, endVerse: Int?, startChapter: Int?, endChapter: Int?) -> Unit = { _, _, _, _, _, _ -> },
    behindColorHex: String = "#EF4444",
    onTrackColorHex: String = "#EAB308",
    completedColorHex: String = "#10B981",
    onUpdateColors: (behind: String, onTrack: String, completed: String) -> Unit = { _, _, _ -> },
    bibleViewModel: BibleViewModel? = null,
    initialTabMode: ReadingTabMode = ReadingTabMode.PLANS,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val readingSettings = bibleViewModel?.readingSettings?.collectAsState()?.value ?: BibleReadingSettings()
    val allPlans = bibleViewModel?.getAllPlansList() ?: planRepository.getAllPlans()
    val activatedPlanIds = readingSettings.activatedPlanIds

    var currentTabMode by remember(initialTabMode) { mutableStateOf(initialTabMode) }

    val allProgress by planRepository.getAllProgress().collectAsState(initial = emptyList())
    val insightsData = remember(allProgress, allPlans, activatedPlanIds) {
        ReadingInsightsCalculator.computeInsights(
            progressList = allProgress,
            allPlans = allPlans,
            activePlanIds = activatedPlanIds
        )
    }

    val sortedPlans = remember(allPlans, activatedPlanIds) {
        allPlans.sortedByDescending { it.id in activatedPlanIds }
    }

    var selectedPlan by remember { mutableStateOf<ReadingPlanInfo?>(null) }
    var progressList by remember { mutableStateOf<List<ReadingPlanProgressEntity>>(emptyList()) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordTargetAction by remember { mutableStateOf<PasswordAction?>(null) }
    var enteredPasswordCode by remember { mutableStateOf("") }
    var generatedPasswordCode by remember { mutableStateOf("") }

    var showCreateCustomPlanDialog by remember { mutableStateOf(false) }

    var showSyncDateDialog by remember { mutableStateOf(false) }
    var syncTargetDayText by remember { mutableStateOf("1") }

    var showColorCustomizationDialog by remember { mutableStateOf(false) }
    var showHighlightStylesDialog by remember { mutableStateOf(false) }
    var editBehindHex by remember { mutableStateOf(behindColorHex) }
    var editOnTrackHex by remember { mutableStateOf(onTrackColorHex) }
    var editCompletedHex by remember { mutableStateOf(completedColorHex) }

    val colorBehind = parseHexColor(behindColorHex, Color(0xFFEF4444))
    val colorOnTrack = parseHexColor(onTrackColorHex, Color(0xEAB308))
    val colorCompleted = parseHexColor(completedColorHex, Color(0xFF10B981))

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
                    Column {
                        Text(
                            text = if (selectedPlan != null) {
                                selectedPlan?.titleEnglish ?: "Bible Reading Plans"
                            } else if (currentTabMode == ReadingTabMode.INSIGHTS) {
                                "Reading Insights (सांख्यिकी)"
                            } else {
                                "Bible Reading Plans"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedPlan != null) {
                            Text(
                                text = selectedPlan?.titleHindi ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        } else if (currentTabMode == ReadingTabMode.INSIGHTS) {
                            Text(
                                text = "आपकी बाइबल अध्ययन प्रगति व स्ट्रीक",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
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
                    if (selectedPlan == null && currentTabMode == ReadingTabMode.PLANS) {
                        IconButton(onClick = { showCreateCustomPlanDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Create Custom Plan")
                        }
                        IconButton(onClick = { showHighlightStylesDialog = true }) {
                            Icon(Icons.Default.Style, contentDescription = "Highlight Styles")
                        }
                        IconButton(onClick = { showColorCustomizationDialog = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "Progress Bar Colors")
                        }
                    }
                    if (selectedPlan != null) {
                        IconButton(onClick = {
                            showSyncDateDialog = true
                            syncTargetDayText = "1"
                        }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync to Current Date")
                        }
                        IconButton(onClick = {
                            selectedPlan?.let { p ->
                                passwordTargetAction = PasswordAction.RESET(p)
                                generatedPasswordCode = (1000..9999).random().toString()
                                enteredPasswordCode = ""
                                showPasswordDialog = true
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Progress", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (selectedPlan == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(
                    selectedTabIndex = if (currentTabMode == ReadingTabMode.PLANS) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = currentTabMode == ReadingTabMode.PLANS,
                        onClick = { currentTabMode = ReadingTabMode.PLANS },
                        text = {
                            Text(
                                text = "रीडिंग प्लान्स (Plans)",
                                fontWeight = if (currentTabMode == ReadingTabMode.PLANS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    Tab(
                        selected = currentTabMode == ReadingTabMode.INSIGHTS,
                        onClick = { currentTabMode = ReadingTabMode.INSIGHTS },
                        text = {
                            Text(
                                text = "इनसाइट्स (Insights)",
                                fontWeight = if (currentTabMode == ReadingTabMode.INSIGHTS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }

                if (currentTabMode == ReadingTabMode.INSIGHTS) {
                    com.example.ui.bible.components.ReadingInsightsDashboard(
                        insights = insightsData,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // List of available plans
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📖 बाइबल रीडिंग प्लान (Bible Reading Plans)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { showCreateCustomPlanDialog = true }) {
                                        Icon(Icons.Default.AddCircle, contentDescription = "कस्टम प्लान", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { showHighlightStylesDialog = true }) {
                                        Icon(Icons.Default.Style, contentDescription = "हाइलाइट शैलियाँ", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { showColorCustomizationDialog = true }) {
                                        Icon(Icons.Default.Palette, contentDescription = "कलर")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "सक्रिय प्लान्स शुरू में दिखेंगे (प्रोग्रेस बार के साथ), जबकि निष्क्रिय प्लान्स बिना प्रोग्रेस बार के आखिर में रहेंगे।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(sortedPlans, key = { it.id }) { plan ->
                    val isActivated = plan.id in activatedPlanIds
                    val isManual = plan.id.startsWith("manual_")

                    // Get plan progress
                    var planProgressList by remember { mutableStateOf<List<ReadingPlanProgressEntity>>(emptyList()) }
                    LaunchedEffect(plan.id) {
                        planRepository.getPlanProgress(plan.id).collect { list ->
                            planProgressList = list
                        }
                    }
                    val completedCount = planProgressList.count { it.isCompleted }
                    val progressFraction = if (plan.totalDays > 0) completedCount.toFloat() / plan.totalDays.toFloat() else 0f
                    val percentText = (progressFraction * 100).toInt()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlan = plan },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActivated)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                        border = if (isActivated)
                            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        else null
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
                                        .background(if (isActivated) NavyPrimary else Color.Gray.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = if (isActivated) GoldWarm else Color.DarkGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plan.titleHindi,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = plan.titleEnglish,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isActivated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                FilterChip(
                                    selected = isActivated,
                                    onClick = {
                                        if (isActivated) {
                                            passwordTargetAction = PasswordAction.DEACTIVATE(plan)
                                            generatedPasswordCode = (1000..9999).random().toString()
                                            enteredPasswordCode = ""
                                            showPasswordDialog = true
                                        } else {
                                            bibleViewModel?.activateReadingPlan(plan.id)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (isActivated) "सक्रिय ✓" else "सक्रिय करें",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    },
                                    leadingIcon = if (isActivated) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF10B981),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = plan.descriptionHindi,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Show progress bar ONLY for activated plans
                            if (isActivated) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "प्रगति: $completedCount / ${plan.totalDays} दिन",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "$percentText%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colorCompleted
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progressFraction.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = colorCompleted
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isManual) {
                                    TextButton(
                                        onClick = {
                                            passwordTargetAction = PasswordAction.DELETE(plan)
                                            generatedPasswordCode = (1000..9999).random().toString()
                                            enteredPasswordCode = ""
                                            showPasswordDialog = true
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("हटाएं", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                TextButton(
                                    onClick = {
                                        passwordTargetAction = PasswordAction.RESET(plan)
                                        generatedPasswordCode = (1000..9999).random().toString()
                                        enteredPasswordCode = ""
                                        showPasswordDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("रीसेट", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Button(
                                    onClick = { selectedPlan = plan },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("प्लान खोलें", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                    }
                }
            }
        } else {
            // Plan Day Detail & Progress
            val currentPlan = selectedPlan!!
            val completedCount = completedDaysMap.size
            val progressPercent = if (currentPlan.totalDays > 0) (completedCount.toFloat() / currentPlan.totalDays) else 0f

            // Determine current status bar color
            val activeColor = when {
                completedCount >= currentPlan.totalDays -> colorCompleted
                completedCount > 0 -> colorOnTrack
                else -> colorBehind
            }

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
                                text = currentPlan.titleHindi,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = currentPlan.titleEnglish,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$completedCount / ${currentPlan.totalDays} दिन पूरा हुआ",
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
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = activeColor
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showSyncDateDialog = true
                                        syncTargetDayText = "1"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("आज की तिथि से सिंक", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        passwordTargetAction = PasswordAction.RESET(currentPlan)
                                        generatedPasswordCode = (1000..9999).random().toString()
                                        enteredPasswordCode = ""
                                        showPasswordDialog = true
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("प्रोग्रेस रीसेट", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "दैनिक पठन तालिका (DAILY READINGS)",
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
                                        text = "${portion.displayHindi} (${portion.displayEnglish})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (day.portions.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        val firstPortion = day.portions.first()
                                        val startV = firstPortion.startVerse ?: 1
                                        val totalV = BibleVerseCounts.getVerseCount(firstPortion.bookId, firstPortion.startChapter)
                                        val endV = firstPortion.endVerse ?: (if (firstPortion.startChapter == firstPortion.endChapter && totalV > 0) totalV else firstPortion.endVerse)
                                        onOpenBible(
                                            firstPortion.bookId,
                                            firstPortion.startChapter,
                                            startV,
                                            endV,
                                            firstPortion.startChapter,
                                            firstPortion.endChapter
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("पढ़ें", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Password Confirmation Dialog (For Deactivate, Reset Progress, or Delete)
    if (showPasswordDialog && passwordTargetAction != null) {
        val targetPlan = when (val action = passwordTargetAction) {
            is PasswordAction.DEACTIVATE -> action.plan
            is PasswordAction.RESET -> action.plan
            is PasswordAction.DELETE -> action.plan
            null -> null
        }

        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                passwordTargetAction = null
                enteredPasswordCode = ""
            },
            title = {
                Text(
                    text = when (passwordTargetAction) {
                        is PasswordAction.DEACTIVATE -> "प्लान निष्क्रयांकन (Deactivate Plan)"
                        is PasswordAction.RESET -> "प्रोग्रेस रीसेट (Reset Progress)"
                        is PasswordAction.DELETE -> "कस्टम प्लान हटाएं (Delete Plan)"
                        else -> "सुरक्षा सत्यापन"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "सुरक्षा हेतु नीचे दिए गए 4-अंकों के स्वचालित कोड को इनपुट बॉक्स में दर्ज करें:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Auto-generated password box
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = generatedPasswordCode,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 8.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = enteredPasswordCode,
                        onValueChange = { if (it.length <= 4) enteredPasswordCode = it },
                        label = { Text("4-अंकों का कोड दर्ज करें") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetPlan != null) {
                            when (val action = passwordTargetAction) {
                                is PasswordAction.DEACTIVATE -> {
                                    bibleViewModel?.deactivateReadingPlan(action.plan.id)
                                }
                                is PasswordAction.RESET -> {
                                    bibleViewModel?.resetReadingPlanProgress(action.plan.id)
                                }
                                is PasswordAction.DELETE -> {
                                    bibleViewModel?.deleteManualReadingPlan(action.plan.id)
                                    if (selectedPlan?.id == action.plan.id) {
                                        selectedPlan = null
                                    }
                                }
                                null -> {}
                            }
                        }
                        showPasswordDialog = false
                        passwordTargetAction = null
                        enteredPasswordCode = ""
                    },
                    enabled = enteredPasswordCode.trim() == generatedPasswordCode,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("पुष्टि करें (Confirm)")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    passwordTargetAction = null
                    enteredPasswordCode = ""
                }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // 2. Create Custom Manual Reading Plan Dialog
    if (showCreateCustomPlanDialog) {
        var customTitleHindi by remember { mutableStateOf("") }
        var customTitleEnglish by remember { mutableStateOf("") }
        var customTotalDaysText by remember { mutableStateOf("30") }
        var customDescHindi by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateCustomPlanDialog = false },
            title = { Text("नया कस्टम प्लान बनाएं (Custom Plan)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customTitleHindi,
                        onValueChange = { customTitleHindi = it },
                        label = { Text("प्लान शीर्षक (हिंदी)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customTitleEnglish,
                        onValueChange = { customTitleEnglish = it },
                        label = { Text("Plan Title (English)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customTotalDaysText,
                        onValueChange = { customTotalDaysText = it.filter { c -> c.isDigit() } },
                        label = { Text("कुल दिन (Total Days - e.g. 30, 60, 365)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customDescHindi,
                        onValueChange = { customDescHindi = it },
                        label = { Text("विवरण (Description - optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = customTotalDaysText.toIntOrNull() ?: 30
                        bibleViewModel?.addManualReadingPlan(
                            titleHindi = customTitleHindi,
                            titleEnglish = customTitleEnglish,
                            totalDays = days,
                            descriptionHindi = customDescHindi,
                            descriptionEnglish = customDescHindi
                        )
                        showCreateCustomPlanDialog = false
                    },
                    enabled = customTitleHindi.isNotBlank() || customTitleEnglish.isNotBlank()
                ) {
                    Text("बनाएं एवं सक्रिय करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCustomPlanDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // 2. Sync to Current Date Dialog
    if (showSyncDateDialog && selectedPlan != null) {
        AlertDialog(
            onDismissRequest = { showSyncDateDialog = false },
            title = { Text("आज की तिथि से सिंक करें", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("आज के पठन दिन (Day Number) संख्या चुनें ताकि पिछले छूटे हुए दिन स्किप हो जाएं:")
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = syncTargetDayText,
                        onValueChange = { syncTargetDayText = it.filter { c -> c.isDigit() } },
                        label = { Text("आज का लक्ष्य दिन (1 - ${selectedPlan?.totalDays})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dayNum = syncTargetDayText.toIntOrNull() ?: 1
                        val validDay = dayNum.coerceIn(1, selectedPlan?.totalDays ?: 1)
                        coroutineScope.launch {
                            planRepository.syncPlanToCurrentDate(selectedPlan!!.id, validDay)
                            showSyncDateDialog = false
                        }
                    }
                ) {
                    Text("सिंक करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDateDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // 3. Color Customization Dialog for Progress Bar
    if (showColorCustomizationDialog) {
        AlertDialog(
            onDismissRequest = { showColorCustomizationDialog = false },
            title = { Text("प्रगति बार का रंग बदलें (Bar Colors)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("पठन प्रगति के अनुसार बार का रंग चुनें (Hex Code):", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = editBehindHex,
                        onValueChange = { editBehindHex = it },
                        label = { Text("पीछे होने पर (Behind) - e.g. #EF4444") },
                        singleLine = true,
                        leadingIcon = {
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(parseHexColor(editBehindHex, Color.Red)))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editOnTrackHex,
                        onValueChange = { editOnTrackHex = it },
                        label = { Text("ट्रैक पर होने पर (On Track) - e.g. #EAB308") },
                        singleLine = true,
                        leadingIcon = {
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(parseHexColor(editOnTrackHex, Color.Yellow)))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCompletedHex,
                        onValueChange = { editCompletedHex = it },
                        label = { Text("दैनिक पूरा होने पर (Completed) - e.g. #10B981") },
                        singleLine = true,
                        leadingIcon = {
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(parseHexColor(editCompletedHex, Color.Green)))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateColors(editBehindHex, editOnTrackHex, editCompletedHex)
                        showColorCustomizationDialog = false
                    }
                ) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorCustomizationDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    if (showHighlightStylesDialog) {
        val currentStyle = bibleViewModel?.planHighlightStyle?.collectAsState()?.value ?: ReadingPlanHighlightStyle()
        HighlightStylesDialog(
            currentStyle = currentStyle,
            onDismissRequest = { showHighlightStylesDialog = false },
            onSaveStyle = { newStyle ->
                bibleViewModel?.updatePlanHighlightStyle(newStyle)
            }
        )
    }
}
