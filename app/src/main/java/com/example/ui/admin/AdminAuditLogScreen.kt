package com.example.ui.admin

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminAuditLog
import com.example.data.model.QrAuditLogEntry
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldWarm
import com.example.util.CsvExportHelper
import java.text.SimpleDateFormat
import java.util.*

sealed class AuditDisplayItem {
    data class Single(val log: AdminAuditLog) : AuditDisplayItem()
    data class Bulk(
        val key: String,
        val actionType: String,
        val adminName: String,
        val adminDesignation: String,
        val logs: List<AdminAuditLog>,
        val timestamp: Long
    ) : AuditDisplayItem()
}

enum class AuditTabMode {
    SYSTEM_LOGS,
    QR_AUDIT_TRAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditLogScreen(
    auditLogs: List<AdminAuditLog>,
    qrAuditLogs: List<QrAuditLogEntry> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentTabMode by remember { mutableStateOf(AuditTabMode.QR_AUDIT_TRAIL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showAnalyticsChart by remember { mutableStateOf(false) }

    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
    val last30DaysLogs = remember(auditLogs) {
        auditLogs.filter { it.timestamp >= thirtyDaysAgo }
    }

    val roleBreakdown = remember(last30DaysLogs) {
        last30DaysLogs.groupBy { it.adminDesignation.ifBlank { "व्यवस्थापक" } }
            .mapValues { entry -> entry.value.size }
            .entries.sortedByDescending { it.value }
    }

    val actionTypeBreakdown = remember(last30DaysLogs) {
        last30DaysLogs.groupBy { log ->
            when {
                log.actionType.contains("MEMBER", ignoreCase = true) -> "सदस्य (Member)"
                log.actionType.contains("ATTENDANCE", ignoreCase = true) -> "उपस्थिति (Attendance)"
                log.actionType.contains("ACCOUNT", ignoreCase = true) -> "लेखा (Accounts)"
                log.actionType.contains("PUSH", ignoreCase = true) -> "पुश (Push)"
                log.actionType.contains("ADMIN", ignoreCase = true) || log.actionType.contains("LOGIN", ignoreCase = true) -> "प्रशासक (Admin/Login)"
                else -> "अन्य (Other)"
            }
        }.mapValues { entry -> entry.value.size }
            .entries.sortedByDescending { it.value }
    }

    val filterOptions = listOf(
        "ALL" to "सभी",
        "MEMBER" to "सदस्य",
        "ATTENDANCE" to "उपस्थिति",
        "ACCOUNT" to "लेखा",
        "PUSH" to "पुश",
        "ADMIN" to "प्रशासक",
        "ANNOUNCEMENT" to "घोषणा"
    )

    val filteredLogs = remember(auditLogs, searchQuery, selectedFilter) {
        auditLogs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "MEMBER" -> log.actionType.contains("MEMBER", ignoreCase = true)
                "ATTENDANCE" -> log.actionType.contains("ATTENDANCE", ignoreCase = true)
                "ACCOUNT" -> log.actionType.contains("ACCOUNT", ignoreCase = true)
                "PUSH" -> log.actionType.contains("PUSH", ignoreCase = true)
                "ADMIN" -> log.actionType.contains("ADMIN", ignoreCase = true) || log.actionType.contains("LOGIN", ignoreCase = true)
                "ANNOUNCEMENT" -> log.actionType.contains("ANNOUNCE", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    log.adminName.contains(searchQuery, ignoreCase = true) ||
                    log.adminDesignation.contains(searchQuery, ignoreCase = true) ||
                    log.description.contains(searchQuery, ignoreCase = true) ||
                    log.actionType.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    // Group bulk operations
    val displayItems = remember(filteredLogs) {
        val sorted = filteredLogs.sortedByDescending { it.timestamp }
        val items = mutableListOf<AuditDisplayItem>()
        val visited = mutableSetOf<String>()

        for (i in sorted.indices) {
            val log = sorted[i]
            if (visited.contains(log.id)) continue

            val isBulkExplicit = log.actionType.contains("BULK", ignoreCase = true) || log.description.contains("एक साथ", ignoreCase = true)
            val sameGroup = sorted.filterIndexed { index, other ->
                index >= i && !visited.contains(other.id) &&
                        other.actionType == log.actionType &&
                        other.adminId == log.adminId &&
                        kotlin.math.abs(other.timestamp - log.timestamp) <= 30_000L
            }

            if (sameGroup.size >= 3 || isBulkExplicit) {
                sameGroup.forEach { visited.add(it.id) }
                items.add(
                    AuditDisplayItem.Bulk(
                        key = "bulk_${log.id}",
                        actionType = log.actionType,
                        adminName = log.adminName,
                        adminDesignation = log.adminDesignation,
                        logs = sameGroup,
                        timestamp = log.timestamp
                    )
                )
            } else {
                visited.add(log.id)
                items.add(AuditDisplayItem.Single(log))
            }
        }
        items
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (currentTabMode == AuditTabMode.QR_AUDIT_TRAIL) "QR व OTP ऑडिट ट्रेल" else "प्रशासन ऑडिट लॉग्स",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (currentTabMode == AuditTabMode.QR_AUDIT_TRAIL) "QR कोड जनरेशन, सीरियल ID व OTP स्थिति लॉग्स" else "समस्त प्रशासनिक कार्यवाहियों का अपरिवर्तनीय इतिहास",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentTabMode == AuditTabMode.SYSTEM_LOGS) {
                        IconButton(onClick = { showAnalyticsChart = !showAnalyticsChart }) {
                            Icon(
                                if (showAnalyticsChart) Icons.Default.List else Icons.Default.BarChart,
                                contentDescription = "Toggle Analytics",
                                tint = if (showAnalyticsChart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (currentTabMode == AuditTabMode.QR_AUDIT_TRAIL) {
                                val uri = CsvExportHelper.exportQrAuditLogsToCsv(context, qrAuditLogs)
                                if (uri != null) {
                                    CsvExportHelper.shareCsvFile(context, uri, "कलीसिया QR व OTP ऑडिट ट्रेल")
                                    Toast.makeText(context, "QR ऑडिट ट्रेल CSV तैयार है", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "CSV निर्यात विफल", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val uri = CsvExportHelper.exportAuditLogsToCsv(context, filteredLogs)
                                if (uri != null) {
                                    CsvExportHelper.shareCsvFile(context, uri, "कलीसिया प्रशासन ऑडिट लॉग")
                                    Toast.makeText(context, "ऑडिट रिपोर्ट CSV तैयार है", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "CSV निर्यात विफल", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("audit_logs_export_csv_button")
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "CSV डाउनलोड (Export CSV)",
                            tint = GoldWarm
                        )
                    }

                    IconButton(
                        onClick = {
                            if (currentTabMode == AuditTabMode.QR_AUDIT_TRAIL) {
                                val sb = StringBuilder("=== कलीसिया QR कोड व OTP ऑडिट ट्रेल ===\n\n")
                                qrAuditLogs.forEach { log ->
                                    val issuedStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                                    val actStr = if (log.activatedTimestamp > 0) SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(log.activatedTimestamp)) else "सक्रिय नहीं"
                                    sb.append("• सीरियल: ${log.targetSerial} | नाम: ${log.targetName} (${log.targetDesignation})\n")
                                    sb.append("  जारीकर्ता: ${log.generatedByAdminName} (${log.generatedByAdminDesignation})\n")
                                    sb.append("  जारी समय: $issuedStr | स्थिति: ${log.otpStatus}\n")
                                    if (log.isActivated()) sb.append("  सक्रियण समय: $actStr\n")
                                    sb.append("\n")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Church QR & OTP Audit Trail")
                                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                                }
                                context.startActivity(Intent.createChooser(intent, "QR ऑडिट ट्रेल साझा करें"))
                            } else {
                                val sb = StringBuilder("=== कलीसिया प्रशासन ऑडिट लॉग ===\n\n")
                                filteredLogs.forEach { log ->
                                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                                    sb.append("[$dateStr] ${log.adminDesignation} (${log.adminName}): ${log.description}\n")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Church Admin Audit Logs")
                                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                                }
                                context.startActivity(Intent.createChooser(intent, "ऑडिट लॉग साझा करें"))
                            }
                        },
                        modifier = Modifier.testTag("audit_logs_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Logs")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Tab Row Switcher (QR Audit Trail vs System Logs)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                TabRow(
                    selectedTabIndex = currentTabMode.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Tab(
                        selected = currentTabMode == AuditTabMode.QR_AUDIT_TRAIL,
                        onClick = { currentTabMode = AuditTabMode.QR_AUDIT_TRAIL },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("QR व OTP ट्रेल (${qrAuditLogs.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = currentTabMode == AuditTabMode.SYSTEM_LOGS,
                        onClick = { currentTabMode = AuditTabMode.SYSTEM_LOGS },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("सिस्टम कार्य इतिहास (${auditLogs.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (currentTabMode == AuditTabMode.QR_AUDIT_TRAIL) {
                    QrAuditTrailSection(qrLogs = qrAuditLogs)
                } else {
                    // System Logs View
                    if (showAnalyticsChart) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                            Column {
                                                Text(text = "पिछले 30 दिनों का गतिविधि विश्लेषण", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text(text = "विभिन्न भूमिकाओं (Roles) द्वारा कुल ${last30DaysLogs.size} कार्य निष्पादित", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = "🏆 विभिन्न भूमिकाओं (Roles) का योगदान (30 दिन)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (roleBreakdown.isEmpty()) {
                                            Text(text = "पिछले 30 दिनों में कोई डेटा उपलब्ध नहीं है।", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            val maxRoleCount = roleBreakdown.maxOf { it.value }.coerceAtLeast(1)
                                            roleBreakdown.forEach { (role, count) ->
                                                val progress = count.toFloat() / maxRoleCount
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(text = role, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                        Text(text = "$count कार्य", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    LinearProgressIndicator(
                                                        progress = { progress },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(8.dp)
                                                            .clip(RoundedCornerShape(4.dp)),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = "📊 गतिविधि श्रेणियां (Action Types Breakdown)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (actionTypeBreakdown.isEmpty()) {
                                            Text(text = "कोई श्रेणी डेटा उपलब्ध नहीं है।", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            actionTypeBreakdown.forEach { (category, count) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primary)
                                                        )
                                                        Text(text = category, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                                        Text(text = "$count", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Search field
                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("खोजें (प्रशासक, पद, या गतिविधि)") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .testTag("audit_log_search_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Filter chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                items(filterOptions) { (key, label) ->
                                    FilterChip(
                                        selected = selectedFilter == key,
                                        onClick = { selectedFilter = key },
                                        label = { Text(label, fontSize = 13.sp) },
                                        leadingIcon = if (selectedFilter == key) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }

                            if (filteredLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "कोई ऑडिट रिकॉर्ड नहीं मिला",
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    items(displayItems, key = {
                                        when (it) {
                                            is AuditDisplayItem.Single -> it.log.id
                                            is AuditDisplayItem.Bulk -> it.key
                                        }
                                    }) { item ->
                                        when (item) {
                                            is AuditDisplayItem.Single -> AuditLogItemCard(log = item.log)
                                            is AuditDisplayItem.Bulk -> AuditLogBulkCard(bulkItem = item)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * QR Code and 10-Minute OTP Audit Trail Section
 */
@Composable
fun QrAuditTrailSection(qrLogs: List<QrAuditLogEntry>) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var qrSearchQuery by remember { mutableStateOf("") }
    var selectedQrStatusFilter by remember { mutableStateOf("ALL") }
    var inspectingEntry by remember { mutableStateOf<QrAuditLogEntry?>(null) }

    val statusFilters = listOf(
        "ALL" to "सभी (All)",
        "ACTIVATED" to "🟢 Activated (सक्रिय)",
        "ACTIVE" to "🟡 Pending (प्रतीक्षारत)",
        "EXPIRED" to "🔴 Expired (समाप्त)"
    )

    val filteredQrLogs = remember(qrLogs, qrSearchQuery, selectedQrStatusFilter) {
        qrLogs.filter { entry ->
            val matchesStatus = when (selectedQrStatusFilter) {
                "ALL" -> true
                "ACTIVATED" -> entry.isActivated()
                "ACTIVE" -> entry.isPendingActive()
                "EXPIRED" -> entry.isExpired()
                else -> true
            }
            val matchesSearch = qrSearchQuery.isBlank() ||
                    entry.targetSerial.contains(qrSearchQuery, ignoreCase = true) ||
                    entry.targetName.contains(qrSearchQuery, ignoreCase = true) ||
                    entry.targetDesignation.contains(qrSearchQuery, ignoreCase = true) ||
                    entry.generatedByAdminName.contains(qrSearchQuery, ignoreCase = true) ||
                    entry.otpCode.contains(qrSearchQuery, ignoreCase = true)

            matchesStatus && matchesSearch
        }.sortedByDescending { it.timestamp }
    }

    val totalCount = qrLogs.size
    val activatedCount = qrLogs.count { it.isActivated() }
    val activePendingCount = qrLogs.count { it.isPendingActive() }
    val expiredCount = qrLogs.count { it.isExpired() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // 1. Metric Summary Cards
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                            Text("QR व OTP ऑडिट विश्लेषण (Overview)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "कुल $totalCount QR पास",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Activated Metric Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("सक्रिय (Activated)", fontSize = 10.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("$activatedCount", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF059669))
                                    if (totalCount > 0) {
                                        val percent = (activatedCount * 100) / totalCount
                                        Spacer(Modifier.width(4.dp))
                                        Text("($percent%)", fontSize = 10.sp, color = Color(0xFF059669))
                                    }
                                }
                            }
                        }

                        // Pending Active 10-Min OTP Metric Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("प्रतीक्षारत (Pending)", fontSize = 10.sp, color = Color(0xFFD97706), fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text("$activePendingCount", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                            }
                        }

                        // Expired Metric Card (Red)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("समाप्त (Expired)", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text("$expiredCount", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            }
        }

        // 2. Search Box
        item {
            OutlinedTextField(
                value = qrSearchQuery,
                onValueChange = { qrSearchQuery = it },
                label = { Text("सीरियल ID, सदस्य नाम, या व्यवस्थापक खोजें...") },
                placeholder = { Text("उदा. VINAY-01, NCC42...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (qrSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { qrSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_audit_search_field")
            )
        }

        // 3. Status Filters
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(statusFilters) { (key, label) ->
                    val isSelected = selectedQrStatusFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedQrStatusFilter = key },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }

        // 4. Log Cards List
        if (filteredQrLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "कोई QR ऑडिट रिकॉर्ड नहीं मिला",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredQrLogs, key = { it.id }) { entry ->
                QrAuditLogCard(
                    entry = entry,
                    onClick = { inspectingEntry = entry },
                    onCopySerial = {
                        clipboardManager.setText(AnnotatedString(entry.targetSerial))
                        Toast.makeText(context, "सीरियल ID कॉपी किया गया: ${entry.targetSerial}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Full Details Inspection Dialog
    if (inspectingEntry != null) {
        QrAuditDetailDialog(
            entry = inspectingEntry!!,
            onDismiss = { inspectingEntry = null }
        )
    }
}

/**
 * Individual QR Audit Log Card with Color-Coded Status Badges:
 * - 🟢 Green for 'Activated'
 * - 🟡 Amber for 'Pending'
 * - 🔴 Red for 'Expired'
 */
@Composable
fun QrAuditLogCard(
    entry: QrAuditLogEntry,
    onClick: () -> Unit,
    onCopySerial: () -> Unit
) {
    val issuedDateStr = remember(entry.timestamp) {
        SimpleDateFormat("dd MMM yyyy • hh:mm:ss a", Locale.getDefault()).format(Date(entry.timestamp))
    }

    val isActivated = entry.isActivated()
    val isPending = entry.isPendingActive()

    val cardBorderColor = when {
        isActivated -> Color(0xFF10B981).copy(alpha = 0.4f)
        isPending -> Color(0xFFF59E0B).copy(alpha = 0.45f)
        else -> Color(0xFFEF4444).copy(alpha = 0.35f)
    }

    val statusAccentColor = when {
        isActivated -> Color(0xFF10B981)
        isPending -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    val minutesLeft = remember(entry.otpExpiresAt, isPending) {
        if (isPending && entry.otpExpiresAt > System.currentTimeMillis()) {
            val diffMs = entry.otpExpiresAt - System.currentTimeMillis()
            (diffMs / 60000).coerceAtLeast(1)
        } else {
            0L
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Status Color Accent Bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(statusAccentColor)
            )

            Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                // Header: Serial ID Badge & Color-Coded Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Serial Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = entry.targetSerial,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Color-Coded Status Badges:
                    // Green for Activated, Amber for Pending, Red for Expired
                    when {
                        isActivated -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Activated",
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "🟢 Activated (सक्रिय)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }
                        }
                        isPending -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = "Pending",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "🟡 Pending (प्रतीक्षारत)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Expired",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "🔴 Expired (समाप्त)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Target Name & Role
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = entry.targetName.ifBlank { "अनाम सदस्य" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "पद: ${entry.targetDesignation.ifBlank { entry.targetRoleTier }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = onCopySerial,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Serial", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))

                // Issuer & Timestamp Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "जारीकर्ता: ${entry.generatedByAdminName} (${entry.generatedByAdminDesignation})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "समय: $issuedDateStr",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    when {
                        isActivated && entry.activatedTimestamp > 0 -> {
                            val actDate = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(entry.activatedTimestamp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(11.dp))
                                    Text(
                                        text = "सक्रिय: $actDate",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }
                        }
                        isPending && minutesLeft > 0 -> {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(11.dp))
                                    Text(
                                        text = "सत्र: ${minutesLeft}m शेष",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "सत्र समाप्त",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full Detailed Inspector Dialog for a single QR Audit Trail record
 */
@Composable
fun QrAuditDetailDialog(
    entry: QrAuditLogEntry,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val issuedStr = remember(entry.timestamp) {
        SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(entry.timestamp))
    }
    val expiryStr = remember(entry.otpExpiresAt) {
        if (entry.otpExpiresAt > 0) SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(entry.otpExpiresAt)) else "N/A"
    }
    val activatedStr = remember(entry.activatedTimestamp) {
        if (entry.activatedTimestamp > 0) SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(entry.activatedTimestamp)) else "सक्रियण की प्रतीक्षा में"
    }

    val isActivated = entry.isActivated()
    val isPending = entry.isPendingActive()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("QR कोड ऑडिट विवरण", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("सीरियल ID: ${entry.targetSerial}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Color-Coded Status Box
                when {
                    isActivated -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "🟢 स्थिति: सक्रिय व सत्यापित (ACTIVATED)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF059669)
                                    )
                                    Text(
                                        text = "OTP का उपभोग कर लिया गया है (Consumed & Verified)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    isPending -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "🟡 स्थिति: सक्रिय सत्र - प्रतीक्षारत (PENDING)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFD97706)
                                    )
                                    Text(
                                        text = "10 मिनट की अस्थायी समय सीमा में सक्रिय",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "🔴 स्थिति: समय सीमा समाप्त (EXPIRED)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                    Text(
                                        text = "10 मिनट की अस्थायी समय सीमा समाप्त हो चुकी है",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Metadata Rows
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    AuditDetailRow("लक्षित सदस्य (Target Name)", entry.targetName)
                    AuditDetailRow("लक्षित पद (Designation/Role)", "${entry.targetDesignation} (${entry.targetRoleTier})")
                    AuditDetailRow("QR जारीकर्ता (Generated By)", "${entry.generatedByAdminName} (${entry.generatedByAdminDesignation})")
                    AuditDetailRow("QR जारी समय (Timestamp)", issuedStr)
                    AuditDetailRow("OTP कोड (6-Digit Code)", if (entry.otpCode.isNotBlank()) entry.otpCode else "******")
                    AuditDetailRow("OTP समय सीमा (Expiry)", expiryStr)
                    AuditDetailRow("सक्रियण समय (Activated At)", activatedStr)
                    if (entry.activatedDeviceId.isNotBlank()) {
                        AuditDetailRow("डिवाइस पहचान (Device ID)", entry.activatedDeviceId)
                    }
                    if (entry.notes.isNotBlank()) {
                        AuditDetailRow("टिप्पणी (Notes)", entry.notes)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("बंद करें (Close)")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(entry.targetSerial))
                    Toast.makeText(context, "सीरियल ID कॉपी किया गया!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("सीरियल कॉपी")
            }
        }
    )
}

@Composable
private fun AuditDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun getBadgeColorAndIcon(actionType: String): Pair<Color, androidx.compose.ui.graphics.vector.ImageVector> {
    return when {
        actionType.contains("MEMBER", ignoreCase = true) -> Color(0xFF1976D2) to Icons.Default.Person
        actionType.contains("ATTENDANCE", ignoreCase = true) -> Color(0xFF388E3C) to Icons.Default.Groups
        actionType.contains("ACCOUNT", ignoreCase = true) -> Color(0xFFF57C00) to Icons.Default.AccountBalanceWallet
        actionType.contains("PUSH", ignoreCase = true) -> Color(0xFF7B1FA2) to Icons.Default.NotificationsActive
        actionType.contains("ADMIN", ignoreCase = true) -> Color(0xFFD32F2F) to Icons.Default.AdminPanelSettings
        actionType.contains("ANNOUNCE", ignoreCase = true) -> Color(0xFF0097A7) to Icons.Default.Campaign
        actionType.contains("LOGIN", ignoreCase = true) -> Color(0xFF455A64) to Icons.Default.VpnKey
        actionType.contains("QR", ignoreCase = true) -> GoldWarm to Icons.Default.QrCode
        else -> MaterialTheme.colorScheme.primary to Icons.Default.Info
    }
}

@Composable
fun AuditLogBulkCard(bulkItem: AuditDisplayItem.Bulk) {
    var expanded by remember { mutableStateOf(false) }
    val dateStr = remember(bulkItem.timestamp) {
        SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(bulkItem.timestamp))
    }

    val (badgeColor, badgeIcon) = getBadgeColorAndIcon(bulkItem.actionType)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        badgeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bulkItem.adminDesignation.ifBlank { "व्यवस्थापक" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (bulkItem.adminName.isNotBlank()) {
                        Text(
                            text = bulkItem.adminName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📦 समूहीकृत सामूहिक गतिविधि: ${bulkItem.logs.size} कार्य एक साथ निष्पादित",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bulkItem.logs.forEachIndexed { index, subLog ->
                        val subDateStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(subLog.timestamp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "#${index + 1} • ${subLog.actionType}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                    Text(
                                        text = subDateStr,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subLog.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogItemCard(log: AdminAuditLog) {
    val dateStr = remember(log.timestamp) {
        SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
    }

    val (badgeColor, badgeIcon) = getBadgeColorAndIcon(log.actionType)

    // Color-coded status assessment:
    // Green (Activated / Success / Active)
    // Amber (Pending / Warning / In-Progress)
    // Red (Expired / Deleted / Blocked / Error)
    val statusColor = when {
        log.description.contains("सक्रिय", ignoreCase = true) ||
        log.description.contains("सफल", ignoreCase = true) ||
        log.description.contains("स्वीकृत", ignoreCase = true) ||
        log.description.contains("जोड़ा", ignoreCase = true) ||
        log.actionType.contains("LOGIN", ignoreCase = true) ||
        log.actionType.contains("ACTIVATE", ignoreCase = true) -> Color(0xFF10B981) // Green

        log.description.contains("प्रतीक्षा", ignoreCase = true) ||
        log.description.contains("समीक्षा", ignoreCase = true) ||
        log.description.contains("अस्थायी", ignoreCase = true) ||
        log.description.contains("OTP", ignoreCase = true) ||
        log.actionType.contains("PENDING", ignoreCase = true) -> Color(0xFFF59E0B) // Amber

        log.description.contains("हटाया", ignoreCase = true) ||
        log.description.contains("ब्लॉक", ignoreCase = true) ||
        log.description.contains("निष्क्रिय", ignoreCase = true) ||
        log.description.contains("समाप्त", ignoreCase = true) ||
        log.description.contains("विफल", ignoreCase = true) ||
        log.actionType.contains("DELETE", ignoreCase = true) ||
        log.actionType.contains("EXPIRE", ignoreCase = true) -> Color(0xFFEF4444) // Red

        else -> Color(0xFF10B981) // Default Green / Active for verified logs
    }

    val statusLabel = when (statusColor) {
        Color(0xFF10B981) -> "🟢 Active"
        Color(0xFFF59E0B) -> "🟡 Pending"
        else -> "🔴 Expired"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    badgeIcon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = log.adminDesignation.ifBlank { "व्यवस्थापक" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                        // Status Badge: Green / Amber / Red
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (log.adminName.isNotBlank()) {
                    Text(
                        text = log.adminName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
