package com.example.ui.admin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AdminUser
import com.example.data.model.AudioMessageConfig
import com.example.data.model.DailyDevotion
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AudioMessageRecorder
import com.example.util.RecordedAudioInfo
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAudioMessageScreen(
    viewModel: MainViewModel,
    currentAdmin: AdminUser,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val config by viewModel.audioMessageConfig.collectAsState()
    val allDevotions by viewModel.dailyDevotions.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("📅 प्रसारण अनुसूची (Schedule)", "🎙️ नया संदेश (Record/Add)", "⚙️ ग्लोबल सेटिंग्स (Config)")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("दैनिक आत्मिक संदेश प्रबंधन", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (config.isServiceActive) "सेवा सक्रिय • ${config.dailyPublishTime} प्रसारण" else "सेवा बंद (Disabled)",
                            fontSize = 12.sp,
                            color = if (config.isServiceActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Switch(
                        checked = config.isServiceActive,
                        onCheckedChange = { isActive ->
                            viewModel.updateAudioMessageConfig(config.copy(isServiceActive = isActive)) { success ->
                                Toast.makeText(
                                    context,
                                    if (isActive) "ऑडियो संदेश सेवा चालू की गई" else "ऑडियो संदेश सेवा बंद की गई",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AudioMessageScheduleTab(
                    devotions = allDevotions,
                    config = config,
                    currentAdmin = currentAdmin,
                    onApprove = { dev ->
                        viewModel.updateDevotionStatus(dev.devotionId, "scheduled", currentAdmin.name) { success ->
                            if (success) Toast.makeText(context, "संदेश स्वीकृत किया गया", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onReject = { dev ->
                        viewModel.updateDevotionStatus(dev.devotionId, "rejected", currentAdmin.name) { success ->
                            if (success) Toast.makeText(context, "संदेश अस्वीकृत किया गया", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onPin = { dev, isPinned, reason ->
                        viewModel.pinDailyDevotion(dev.devotionId, isPinned, reason) { success ->
                            if (success) Toast.makeText(context, if (isPinned) "पिन किया गया" else "अनपिन किया गया", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDelete = { dev ->
                        viewModel.deleteDailyDevotion(dev.devotionId) { success ->
                            if (success) Toast.makeText(context, "संदेश हटाया गया", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRunFifoCleanup = {
                        viewModel.runSmartFifoCleanup { success, count, bytesFreed ->
                            if (success) {
                                val freedMb = String.format(Locale.getDefault(), "%.2f", bytesFreed / (1024.0 * 1024.0))
                                Toast.makeText(context, "FIFO क्लीनअप संपन्न: $count पुराने संदेश हटाए गए ($freedMb MB मुक्त)", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "FIFO क्लीनअप विफल रहा", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                1 -> AudioMessageRecorderTab(
                    viewModel = viewModel,
                    config = config,
                    currentAdmin = currentAdmin,
                    allDevotions = allDevotions,
                    onUploaded = {
                        selectedTab = 0
                    }
                )
                2 -> AudioMessageConfigTab(
                    config = config,
                    devotions = allDevotions,
                    onRunFifoCleanup = {
                        viewModel.runSmartFifoCleanup { success, count, bytesFreed ->
                            if (success) {
                                val freedMb = String.format(Locale.getDefault(), "%.2f", bytesFreed / (1024.0 * 1024.0))
                                Toast.makeText(context, "FIFO क्लीनअप संपन्न: $count पुराने संदेश हटाए गए ($freedMb MB मुक्त)", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "FIFO क्लीनअप संपन्न: कोई संदेश हटाने की आवश्यकता नहीं", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onSaveConfig = { updatedConfig ->
                        viewModel.updateAudioMessageConfig(updatedConfig) { success ->
                            if (success) Toast.makeText(context, "ग्लोबल सेटिंग्स सहेजी गईं", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

fun formatAudioSize(fileSizeBytes: Long, durationSeconds: Int = 0): String {
    return if (fileSizeBytes > 0L) {
        if (fileSizeBytes < 1024 * 1024) {
            "${String.format(Locale.getDefault(), "%.1f", fileSizeBytes / 1024.0)} KB"
        } else {
            "${String.format(Locale.getDefault(), "%.2f", fileSizeBytes / (1024.0 * 1024.0))} MB"
        }
    } else if (durationSeconds > 0) {
        val approxKb = durationSeconds * 8
        if (approxKb < 1024) "~$approxKb KB" else "~${String.format(Locale.getDefault(), "%.1f", approxKb / 1024.0)} MB"
    } else {
        "साइज अज्ञात"
    }
}

@Composable
private fun AudioStorageLiveUsageMeterCard(
    devotions: List<DailyDevotion>,
    config: AudioMessageConfig,
    onRunFifoCleanup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalUsedBytes = remember(devotions) { devotions.sumOf { it.fileSizeBytes } }
    val totalCapBytes = remember(config.storageCapMb) { config.storageCapMb * 1024L * 1024L }
    val usageRatio = remember(totalUsedBytes, totalCapBytes) {
        if (totalCapBytes > 0) (totalUsedBytes.toFloat() / totalCapBytes.toFloat()).coerceIn(0f, 1f) else 0f
    }
    val usagePercent = usageRatio * 100f

    // Color logic: Green (<70%), Yellow/Orange (70-90%), Red (>=90%)
    val meterColor = when {
        usagePercent >= 90f -> Color(0xFFD32F2F)
        usagePercent >= 70f -> Color(0xFFFFA000)
        else -> Color(0xFF388E3C)
    }

    val meterStatusText = when {
        usagePercent >= 95f -> "🚨 95% पार: स्मार्ट FIFO ऑटो-क्लीनअप सक्रिय"
        usagePercent >= 90f -> "⚠️ क्रिटिकल स्तर (स्टोरेज भरने वाला है)"
        usagePercent >= 70f -> "⚡ मध्यम उपयोग (सतर्कता)"
        else -> "✅ सुरक्षित स्तर (सामान्य)"
    }

    val usedMbText = remember(totalUsedBytes) {
        String.format(Locale.getDefault(), "%.2f", totalUsedBytes / (1024.0 * 1024.0))
    }
    val freeMbText = remember(totalUsedBytes, totalCapBytes) {
        String.format(Locale.getDefault(), "%.2f", (totalCapBytes - totalUsedBytes).coerceAtLeast(0L) / (1024.0 * 1024.0))
    }

    val pinnedCount = remember(devotions) { devotions.count { it.isPinned } }
    val eligibleFifoCount = remember(devotions) { devotions.count { !it.isPinned } }

    var showCleanupDialog by remember { mutableStateOf(false) }

    if (showCleanupDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            title = { Text("स्मार्ट FIFO ऑटो-क्लीनअप", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("क्या आप पुराने गैर-पिन संदेशों को हटाकर स्टोरेज खाली करना चाहते हैं?")
                    Text("• पिन किए गए ($pinnedCount) संदेश सुरक्षित रहेंगे और कभी नहीं हटाए जाएंगे।", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text("• $eligibleFifoCount पुराने गैर-पिन संदेशों में से हटाने योग्य संदेश हटाए जाएंगे।", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCleanupDialog = false
                        onRunFifoCleanup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = meterColor)
                ) {
                    Text("हाँ, क्लीनअप चलाएं")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = meterColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, meterColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = meterColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "लाइव ऑडियो स्टोरेज मीटर",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = meterColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = meterStatusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = meterColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Usage Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$usedMbText MB इस्तेमाल / ${config.storageCapMb} MB कोटा",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", usagePercent)}%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = meterColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { usageRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = meterColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Detail Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("खाली जगह:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text("$freeMbText MB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📌 पिन सुरक्षित:", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Text("$pinnedCount संदेश", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("♻️ FIFO पात्र:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text("$eligibleFifoCount संदेश", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (usagePercent >= 70f || eligibleFifoCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showCleanupDialog = true },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = meterColor)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("स्मार्ट FIFO ऑटो-क्लीनअप चलाएं", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AudioMessageScheduleTab(
    devotions: List<DailyDevotion>,
    config: AudioMessageConfig,
    currentAdmin: AdminUser,
    onApprove: (DailyDevotion) -> Unit,
    onReject: (DailyDevotion) -> Unit,
    onPin: (DailyDevotion, Boolean, String) -> Unit,
    onDelete: (DailyDevotion) -> Unit,
    onRunFifoCleanup: () -> Unit
) {
    val context = LocalContext.current
    var previewMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingDevotionId by remember { mutableStateOf<String?>(null) }
    var filterStatus by remember { mutableStateOf("ALL") }

    DisposableEffect(Unit) {
        onDispose {
            try {
                previewMediaPlayer?.stop()
                previewMediaPlayer?.release()
            } catch (e: Exception) {}
        }
    }

    val filteredDevotions = remember(devotions, filterStatus) {
        when (filterStatus) {
            "PENDING" -> devotions.filter { it.status == "pending_approval" }
            "SCHEDULED" -> devotions.filter { it.status == "scheduled" || it.status == "published" }
            "PINNED" -> devotions.filter { it.isPinned }
            else -> devotions
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Storage Meter Card at top of the schedule tab
        item(key = "storage_usage_meter") {
            AudioStorageLiveUsageMeterCard(
                devotions = devotions,
                config = config,
                onRunFifoCleanup = onRunFifoCleanup
            )
        }

        // Status filter chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterStatus == "ALL",
                    onClick = { filterStatus = "ALL" },
                    label = { Text("सभी (${devotions.size})") }
                )
                FilterChip(
                    selected = filterStatus == "PENDING",
                    onClick = { filterStatus = "PENDING" },
                    label = { Text("अनुमोदन बाकी (${devotions.count { it.status == "pending_approval" }})") }
                )
                FilterChip(
                    selected = filterStatus == "SCHEDULED",
                    onClick = { filterStatus = "SCHEDULED" },
                    label = { Text("अनुसूचित/प्रसारित") }
                )
                FilterChip(
                    selected = filterStatus == "PINNED",
                    onClick = { filterStatus = "PINNED" },
                    label = { Text("📌 पिन किए गए") }
                )
            }
        }

        if (filteredDevotions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("कोई आत्मिक संदेश नहीं मिला", color = MaterialTheme.colorScheme.outline)
                        Text("नया संदेश रिकॉर्ड करने के लिए 'नया संदेश' टैब पर जाएं", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            items(filteredDevotions, key = { it.devotionId }) { dev ->
                val isPlaying = playingDevotionId == dev.devotionId
                val audioSizeStr = formatAudioSize(dev.fileSizeBytes, dev.durationSeconds)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (dev.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(dev.slotLabel.ifBlank { "स्लॉट ${dev.slotIndex}" }, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                                Text(dev.scheduledDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }

                            // Status badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (dev.status) {
                                    "pending_approval" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                    "rejected" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                    else -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = when (dev.status) {
                                        "pending_approval" -> "अनुमोदन प्रतीक्षारत"
                                        "rejected" -> "अस्वीकृत"
                                        else -> "स्वीकृत/अनुसूचित"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (dev.status) {
                                        "pending_approval" -> Color(0xFFFF9800)
                                        "rejected" -> MaterialTheme.colorScheme.error
                                        else -> Color(0xFF4CAF50)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Audio Message Title with Size Badge right next to it
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dev.title.ifBlank { "दैनिक आत्मिक संदेश" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "💾 $audioSizeStr",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (dev.scriptureRef.isNotBlank()) {
                            Text("📖 ${dev.scriptureRef}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("🎙️ वक्ता: ${dev.speakerName} (${dev.speakerRole})", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("साइज़: $audioSizeStr • अवधि: ${dev.durationSeconds}s", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                if (dev.isPinned) {
                                    Text("• 📌 सुरक्षित", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Text("• ♻️ FIFO पात्र", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Play Preview
                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            try {
                                                previewMediaPlayer?.stop()
                                                previewMediaPlayer?.release()
                                            } catch (e: Exception) {}
                                            previewMediaPlayer = null
                                            playingDevotionId = null
                                        } else {
                                            try {
                                                previewMediaPlayer?.release()
                                                val mp = MediaPlayer().apply {
                                                    setAudioAttributes(
                                                        AudioAttributes.Builder()
                                                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                                            .build()
                                                    )
                                                    setDataSource(dev.audioUrl)
                                                    prepareAsync()
                                                    setOnPreparedListener { start() }
                                                    setOnCompletionListener {
                                                        playingDevotionId = null
                                                    }
                                                }
                                                previewMediaPlayer = mp
                                                playingDevotionId = dev.devotionId
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "ऑडियो चलाने में त्रुटि", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Preview",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Pin Toggle
                                IconButton(
                                    onClick = {
                                        onPin(dev, !dev.isPinned, if (!dev.isPinned) "मास्टर एडमिन द्वारा अनुशंसित" else "")
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (dev.isPinned) Icons.Default.PushPin else Icons.Default.OutlinedFlag,
                                        contentDescription = "Pin",
                                        tint = if (dev.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }

                                // Delete
                                IconButton(onClick = { onDelete(dev) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        // Approval buttons if pending
                        if (dev.status == "pending_approval") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onApprove(dev) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("स्वीकार करें (Approve)", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onReject(dev) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("अस्वीकार (Reject)", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioMessageRecorderTab(
    viewModel: MainViewModel,
    config: AudioMessageConfig,
    currentAdmin: AdminUser,
    allDevotions: List<DailyDevotion>,
    onUploaded: () -> Unit
) {
    val context = LocalContext.current
    val recorder = remember { AudioMessageRecorder(context) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingSecondsLeft by remember { mutableIntStateOf(config.maxDurationSeconds) }
    var recordedAudioInfo by remember { mutableStateOf<RecordedAudioInfo?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }

    var title by remember { mutableStateOf("") }
    var scriptureRef by remember { mutableStateOf("") }
    var speakerName by remember { mutableStateOf(currentAdmin.name.ifBlank { "पास्टर / सेवक" }) }
    var scheduledDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var slotIndex by remember { mutableIntStateOf(1) }
    var directAudioUrl by remember { mutableStateOf("") }

    val maxAllowedBytes = remember(config.maxFileSizeMb) { config.maxFileSizeMb * 1024L * 1024L }
    val currentFileSizeBytes = recordedAudioInfo?.file?.length() ?: recordedAudioInfo?.fileSizeBytes ?: 0L
    val isFileExceededLimit = recordedAudioInfo != null && currentFileSizeBytes > maxAllowedBytes

    val slots = listOf(
        1 to "सुबह का मनन (Morning Devotion)",
        2 to "शाम का मनन (Evening Devotion)",
        3 to "रात्रि का मनन (Night Devotion)"
    ).take(config.dailySlotsCount.coerceIn(1, 3))

    // Microphone permission launcher
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "ऑडियो रिकॉर्ड करने के लिए माइक्रोफोन अनुमति आवश्यक है", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-countdown timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSecondsLeft = config.maxDurationSeconds
            while (isRecording && recordingSecondsLeft > 0) {
                delay(1000)
                recordingSecondsLeft--
            }
            if (isRecording && recordingSecondsLeft <= 0) {
                // Auto-stop at 00:00
                recordedAudioInfo = recorder.stopRecording()
                isRecording = false
                Toast.makeText(context, "अधिकतम सीमा समाप्त: रिकॉर्डिंग पूरी हुई", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎙️ इन-ऐप रिकॉर्डिंग स्टूडियो", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "अधिकतम अवधि: ${config.maxDurationSeconds} सेकंड • क्वालिटी: ${config.recordingQuality}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timer Display
                    val minutes = recordingSecondsLeft / 60
                    val seconds = recordingSecondsLeft % 60
                    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isRecording) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeFormatted,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isRecording) "🔴 रिकॉर्डिंग चालू है... (समय समाप्त होने पर स्वतः रुकेगा)"
                                else if (recordedAudioInfo != null) "✅ रिकॉर्डिंग तैयार है (${recordedAudioInfo?.durationSeconds}s, ${(recordedAudioInfo?.fileSizeBytes ?: 0) / 1024} KB)"
                                else "रिकॉर्ड करने के लिए माइक बटन दबाएं",
                                fontSize = 12.sp,
                                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Record Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRecording) {
                            Button(
                                onClick = {
                                    if (!hasMicPermission) {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        val file = recorder.startRecording(config.recordingQuality)
                                        if (file != null) {
                                            isRecording = true
                                        } else {
                                            Toast.makeText(context, "रिकॉर्डिंग शुरू करने में विफल", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.size(64.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Start Recording", modifier = Modifier.size(32.dp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    recordedAudioInfo = recorder.stopRecording()
                                    isRecording = false
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(64.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop Recording", modifier = Modifier.size(32.dp))
                            }
                        }
                    }

                    if (recordedAudioInfo != null && !isRecording) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(
                                onClick = {
                                    recorder.cancelRecording()
                                    recordedAudioInfo = null
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("पुनः रिकॉर्ड करें (Reset)")
                            }
                        }

                        // Block alert if file exceeds Master Admin maxFileSizeMb
                        if (isFileExceededLimit) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("⛔ भारी फ़ाइल: अपलोड पूरी तरह ब्लॉक", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                                        Text(
                                            "इस ऑडियो का साइज (${formatAudioSize(currentFileSizeBytes)}) मास्टर एडमिन सीमा (${config.maxFileSizeMb} MB) से बड़ा है। सुरक्षा नियमों के अनुसार भारी फ़ाइल अपलोड पूरी तरह ब्लॉक है।",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Details form
        item {
            Text("संदेश विवरण व शेड्यूलिंग", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("संदेश का शीर्षक (e.g. नई सुबह, नया अनुग्रह)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = scriptureRef,
                onValueChange = { scriptureRef = it },
                label = { Text("बाइबिल संदर्भ (e.g. विलापगीत 3:22-23)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = speakerName,
                onValueChange = { speakerName = it },
                label = { Text("वक्ता का नाम (Speaker Name)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = scheduledDate,
                onValueChange = { scheduledDate = it },
                label = { Text("प्रसारण तारीख (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("प्रसारण स्लॉट (Daily Slot):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEach { (index, label) ->
                    FilterChip(
                        selected = slotIndex == index,
                        onClick = { slotIndex = index },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("वैकल्पिक: बाहरी ऑडियो URL दर्ज करें (यदि रिकॉर्ड नहीं कर रहे):", fontSize = 12.sp)
            OutlinedTextField(
                value = directAudioUrl,
                onValueChange = { directAudioUrl = it },
                label = { Text("सीधा ऑडियो लिंक (https://...)") },
                placeholder = { Text("https://...") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Publish Button
        item {
            if (isUploading) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(progress = uploadProgress, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("अपलोड हो रहा है... ${(uploadProgress * 100).toInt()}%", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = {
                        val finalTitle = title.ifBlank { "दैनिक आत्मिक संदेश" }
                        val file = recordedAudioInfo?.file

                        if (file == null && directAudioUrl.isBlank()) {
                            Toast.makeText(context, "कृपया ऑडियो रिकॉर्ड करें या लिंक दर्ज करें", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (file != null && isFileExceededLimit) {
                            Toast.makeText(context, "सुरक्षा नियम: फ़ाइल साइज़ (${formatAudioSize(currentFileSizeBytes)}) मास्टर एडमिन सीमा (${config.maxFileSizeMb} MB) से बड़ा है!", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val devotionId = "dev_${scheduledDate.replace("-", "_")}_slot$slotIndex"
                        val slotLabel = slots.find { it.first == slotIndex }?.second ?: "दैनिक मनन"
                        val status = if (config.requiresPrePublishApproval) "pending_approval" else "scheduled"

                        // FIFO check: If storage near 95%, run FIFO cleanup automatically before upload
                        val totalUsedBytes = allDevotions.sumOf { it.fileSizeBytes }
                        val totalCapBytes = config.storageCapMb * 1024L * 1024L
                        if (config.enableFifoAutoCleanup && (totalUsedBytes + currentFileSizeBytes) >= (0.95 * totalCapBytes)) {
                            viewModel.runSmartFifoCleanup { _, freedCount, freedBytes ->
                                if (freedCount > 0) {
                                    val freedMb = String.format(Locale.getDefault(), "%.2f", freedBytes / (1024.0 * 1024.0))
                                    Toast.makeText(context, "FIFO ऑटो-क्लीनअप: $freedCount पुराने संदेश हटाए गए ($freedMb MB स्थान खाली हुआ)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        if (file != null) {
                            // Upload to Firebase Storage
                            isUploading = true
                            val storagePath = "devotions/audio_${devotionId}.m4a"
                            viewModel.uploadAudioDevotionFile(
                                file = file,
                                storagePath = storagePath,
                                onProgress = { progress -> uploadProgress = progress },
                                onComplete = { success, downloadUrl, error ->
                                    isUploading = false
                                    if (success && downloadUrl != null) {
                                        val devotion = DailyDevotion(
                                            devotionId = devotionId,
                                            scheduledDate = scheduledDate,
                                            slotIndex = slotIndex,
                                            slotLabel = slotLabel,
                                            title = finalTitle,
                                            scriptureRef = scriptureRef,
                                            speakerId = currentAdmin.activeDeviceId,
                                            speakerName = speakerName,
                                            speakerRole = currentAdmin.designation.ifBlank { "pastor" },
                                            audioUrl = downloadUrl,
                                            storagePath = storagePath,
                                            durationSeconds = recordedAudioInfo?.durationSeconds ?: 60,
                                            fileSizeBytes = currentFileSizeBytes,
                                            bitrateKbps = if (config.recordingQuality.contains("128")) 128 else 64,
                                            status = status,
                                            approvedBy = if (!config.requiresPrePublishApproval) currentAdmin.name else "",
                                            approvedAt = if (!config.requiresPrePublishApproval) System.currentTimeMillis() else null
                                        )
                                        viewModel.saveDailyDevotion(devotion) { saved, err ->
                                            if (saved) {
                                                Toast.makeText(context, "संदेश सफलतापूर्वक अपलोड व शेड्यूल किया गया", Toast.LENGTH_SHORT).show()
                                                onUploaded()
                                            } else {
                                                Toast.makeText(context, "डेटाबेस में सहेजने में त्रुटि: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "स्टोरेज अपलोड विफल: $error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } else {
                            // Direct URL save
                            val devotion = DailyDevotion(
                                devotionId = devotionId,
                                scheduledDate = scheduledDate,
                                slotIndex = slotIndex,
                                slotLabel = slotLabel,
                                title = finalTitle,
                                scriptureRef = scriptureRef,
                                speakerId = currentAdmin.activeDeviceId,
                                speakerName = speakerName,
                                speakerRole = currentAdmin.designation.ifBlank { "pastor" },
                                audioUrl = directAudioUrl.trim(),
                                storagePath = "",
                                durationSeconds = 120,
                                fileSizeBytes = 0L,
                                status = status,
                                approvedBy = if (!config.requiresPrePublishApproval) currentAdmin.name else "",
                                approvedAt = if (!config.requiresPrePublishApproval) System.currentTimeMillis() else null
                            )
                            viewModel.saveDailyDevotion(devotion) { saved, err ->
                                if (saved) {
                                    Toast.makeText(context, "संदेश सफलतापूर्वक शेड्यूल किया गया", Toast.LENGTH_SHORT).show()
                                    onUploaded()
                                } else {
                                    Toast.makeText(context, "सहेजने में त्रुटि: $err", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isFileExceededLimit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isFileExceededLimit) "अपलोड ब्लॉक (अधिकतम साइज़ सीमा पार)" else "संदेश अपलोड व शेड्यूल करें",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioMessageConfigTab(
    config: AudioMessageConfig,
    devotions: List<DailyDevotion>,
    onRunFifoCleanup: () -> Unit,
    onSaveConfig: (AudioMessageConfig) -> Unit
) {
    var storageCapMb by remember { mutableIntStateOf(config.storageCapMb) }
    var maxFileSizeMb by remember { mutableIntStateOf(config.maxFileSizeMb) }
    var enableFifoAutoCleanup by remember { mutableStateOf(config.enableFifoAutoCleanup) }

    var maxDuration by remember { mutableIntStateOf(config.maxDurationSeconds) }
    var publishTime by remember { mutableStateOf(config.dailyPublishTime) }
    var slotsCount by remember { mutableIntStateOf(config.dailySlotsCount) }
    var recordingQuality by remember { mutableStateOf(config.recordingQuality) }
    var requiresApproval by remember { mutableStateOf(config.requiresPrePublishApproval) }
    var fallbackUrl by remember { mutableStateOf(config.fallbackAudioUrl) }
    var retentionDays by remember { mutableIntStateOf(config.retentionDays) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Storage Meter directly visible in Config
        item(key = "config_live_meter") {
            AudioStorageLiveUsageMeterCard(
                devotions = devotions,
                config = config.copy(
                    storageCapMb = storageCapMb,
                    maxFileSizeMb = maxFileSizeMb,
                    enableFifoAutoCleanup = enableFifoAutoCleanup
                ),
                onRunFifoCleanup = onRunFifoCleanup
            )
        }

        // Section 1: Storage Budget & Governance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("📦 ऑडियो स्टोरेज बजट व कोटा (Storage Cap)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text(
                        "ऑडियो संदेशों के लिए कुल क्लाउड स्टोरेज बजट (100 MB से 4 GB तक सीमित करें):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$storageCapMb MB",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val gbText = String.format(Locale.getDefault(), "%.2f", storageCapMb / 1024.0)
                        Text(
                            text = "(लगभग $gbText GB)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Slider for 100 MB to 4096 MB (4 GB)
                    Slider(
                        value = storageCapMb.toFloat(),
                        onValueChange = { storageCapMb = it.toInt() },
                        valueRange = 100f..4096f,
                        steps = 39,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            100 to "100 MB",
                            250 to "250 MB",
                            500 to "500 MB",
                            1024 to "1 GB",
                            2048 to "2 GB",
                            4096 to "4 GB"
                        ).forEach { (mb, label) ->
                            FilterChip(
                                selected = storageCapMb == mb,
                                onClick = { storageCapMb = mb },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider()

                    // Section 2: Per-file limit (प्रति-फ़ाइल सीमा)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("🛡️ प्रति-फ़ाइल अधिकतम सीमा (Per-File Limit)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        "सिक्योरिटी रूल्स द्वारा किसी भी भारी फ़ाइल का अपलोड पूरी तरह ब्लॉक रहेगा:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "अधिकतम फ़ाइल साइज़:",
                            fontSize = 13.sp
                        )
                        Text(
                            text = "$maxFileSizeMb MB",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(2, 5, 10, 20, 50).forEach { mb ->
                            FilterChip(
                                selected = maxFileSizeMb == mb,
                                onClick = { maxFileSizeMb = mb },
                                label = { Text("$mb MB", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider()

                    // Section 3: Smart FIFO Auto-Cleanup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("♻️ स्मार्ट FIFO ऑटो-क्लीनअप (Smart FIFO Cleanup)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "स्टोरेज 95% भरने पर सिस्टम सबसे पुराने गैर-पिन (Non-pinned) संदेशों को स्वतः हटाकर नए संदेशों के लिए जगह बनाएगा। पिन किए गए संदेश हमेशा सुरक्षित रहेंगे।",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = enableFifoAutoCleanup,
                            onCheckedChange = { enableFifoAutoCleanup = it }
                        )
                    }
                }
            }
        }

        // General settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⏱️ अधिकतम रिकॉर्डिंग समय (Hard Duration Ceiling)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60 to "1 मिनट", 120 to "2 मिनट", 180 to "3 मिनट", 300 to "5 मिनट").forEach { (sec, label) ->
                            FilterChip(
                                selected = maxDuration == sec,
                                onClick = { maxDuration = sec },
                                label = { Text(label) }
                            )
                        }
                    }

                    HorizontalDivider()

                    Text("⏰ दैनिक प्रसारण का समय (Daily Publish Time)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("सदस्यों को इस समय के बाद ही प्लेबैक अनलॉक होगा।", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    OutlinedTextField(
                        value = publishTime,
                        onValueChange = { publishTime = it },
                        label = { Text("प्रसारण समय (24H Format e.g. 06:00)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    Text("🔢 प्रतिदिन स्लॉट संख्या (Daily Slots Quota)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "1 (सुबह)", 2 to "2 (सुबह व शाम)", 3 to "3 (सुबह, दोपहर, रात)").forEach { (count, label) ->
                            FilterChip(
                                selected = slotsCount == count,
                                onClick = { slotsCount = count },
                                label = { Text(label) }
                            )
                        }
                    }

                    HorizontalDivider()

                    Text("🎙️ ऑडियो क्वालिटी (Dynamic Bitrate)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "data_saver_32k" to "डेटा सेवर (32 kbps Mono - 240 KB/min)",
                            "standard_64k" to "मानक स्पष्ट आवाज़ (64 kbps Mono - 480 KB/min)",
                            "high_128k" to "स्टूडियो HD (128 kbps Mono - 960 KB/min)"
                        ).forEach { (key, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { recordingQuality = key }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = recordingQuality == key,
                                    onClick = { recordingQuality = key }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label, fontSize = 13.sp)
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("प्रसारण पूर्व अनुमोदन अनिवार्य (Pre-Publish Approval)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("यदि चालू है तो संदेश मास्टर एडमिन की स्वीकृति के बाद ही प्रसारित होगा।", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = requiresApproval,
                            onCheckedChange = { requiresApproval = it }
                        )
                    }

                    HorizontalDivider()

                    Text("फाॅलबैक ऑडियो URL (जब कोई संदेश उपलब्ध न हो):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = fallbackUrl,
                        onValueChange = { fallbackUrl = it },
                        label = { Text("स्थायी आशीर्वाद संदेश लिंक (Fallback Audio URL)") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    Text("ऑटो-क्लीनअप व अवधारण अवधि (Retention Days):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(7, 15, 30, 60, 90, 0).forEach { days ->
                            FilterChip(
                                selected = retentionDays == days,
                                onClick = { retentionDays = days },
                                label = { Text(if (days == 0) "कभी नहीं" else "$days दिन") }
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val updated = config.copy(
                        storageCapMb = storageCapMb,
                        maxFileSizeMb = maxFileSizeMb,
                        enableFifoAutoCleanup = enableFifoAutoCleanup,
                        maxDurationSeconds = maxDuration,
                        dailyPublishTime = publishTime.trim(),
                        dailySlotsCount = slotsCount,
                        recordingQuality = recordingQuality,
                        requiresPrePublishApproval = requiresApproval,
                        fallbackAudioUrl = fallbackUrl.trim(),
                        retentionDays = retentionDays
                    )
                    onSaveConfig(updated)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ग्लोबल कॉन्फ़िगरेशन सहेजें (Save Config)", fontWeight = FontWeight.Bold)
            }
        }
    }
}
