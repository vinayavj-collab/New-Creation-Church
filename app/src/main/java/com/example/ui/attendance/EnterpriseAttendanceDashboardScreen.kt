package com.example.ui.attendance

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseAttendanceDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenUsherScanner: () -> Unit,
    onOpenDynamicKiosk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentAdmin by viewModel.currentAdmin.collectAsState()
    val globalConfig by viewModel.attendanceGlobalConfig.collectAsState()
    val branchSettings by viewModel.branchAttendanceSettings.collectAsState()
    val serviceSchedules by viewModel.attendanceServiceSchedules.collectAsState()
    val todayRecords by viewModel.todayAttendanceRecords.collectAsState()
    val absenteeCareList by viewModel.absenteeCareList.collectAsState()
    val activeWindow by viewModel.activeServiceWindow.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showScheduleDialog by remember { mutableStateOf<ServiceSchedule?>(null) }
    var isNewSchedule by remember { mutableStateOf(false) }
    var showAddUsherDialog by remember { mutableStateOf(false) }
    var showSettingsSavedToast by remember { mutableStateOf(false) }
    var showPrintBadgesDialog by remember { mutableStateOf(false) }

    val isMasterAdmin = currentAdmin?.isMasterAdmin() == true || currentAdmin?.rank ?: 0 >= 5

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "उपस्थिति प्रशासन (Attendance Governance)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            if (!globalConfig.isAttendanceServiceActive) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "स्थगित (Suspended)",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${branchSettings.branchName} • [ ${branchSettings.churchPrefix} ]",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("attendance_gov_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPrintBadgesDialog = true },
                        modifier = Modifier.testTag("btn_print_physical_badges")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print Badges PDF", tint = GoldWarm)
                    }

                    IconButton(
                        onClick = {
                            val uri = viewModel.attendanceGovernanceRepository.exportAttendanceRegisterToCsv()
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "कलीसिया उपस्थिति रजिस्टर - ${branchSettings.branchName}")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "उपस्थिति रजिस्टर शेयर करें"))
                            } else {
                                Toast.makeText(context, "CSV रजिस्टर तैयार करने में त्रुटि हुई", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("btn_export_attendance_register")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Register", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Quick Action Bottom Toolbar
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenUsherScanner,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_launch_usher_scanner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("उशर स्कैनर", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenDynamicKiosk,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_launch_dynamic_kiosk"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, GoldWarm)
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("डायनामिक TV स्क्रीन", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldWarm)
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Active Window Notification Banner
            Surface(
                color = if (activeWindow.isWindowActive) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, if (activeWindow.isWindowActive) Color(0xFF10B981).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (activeWindow.isWindowActive) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (activeWindow.isWindowActive) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (activeWindow.isWindowActive) "🟢 लाइव चेक-इन विंडो सक्रिय" else "⏳ चेक-इन विंडो बंद",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (activeWindow.isWindowActive) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = activeWindow.message,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("लाइव टेलीमेट्री (${todayRecords.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        viewModel.refreshAttendanceAbsenteeAnalytics()
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("सुधि ट्रैकर (${absenteeCareList.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("मोड व जियो-फेंस", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("सभा समय (${serviceSchedules.size})", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("उशर दल (${branchSettings.delegatedUsherNames.size})", fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            // Tab Contents
            when (selectedTab) {
                0 -> LiveTelemetryTab(todayRecords = todayRecords, viewModel = viewModel)
                1 -> AbsenteeCareTab(absenteeList = absenteeCareList, branchName = branchSettings.branchName)
                2 -> CheckInModeAndGeoFenceTab(
                    branchSettings = branchSettings,
                    globalConfig = globalConfig,
                    isMasterAdmin = isMasterAdmin,
                    onSaveBranchSettings = { updated ->
                        viewModel.updateBranchAttendanceSettings(updated) { success, _ ->
                            if (success) Toast.makeText(context, "शाखा उपस्थिति सेटिंग्स सहेजी गईं!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSaveGlobalConfig = { updatedGlobal ->
                        viewModel.updateAttendanceGlobalConfig(updatedGlobal) { success, _ ->
                            if (success) Toast.makeText(context, "ग्लोबल नीतियां अपडेट की गईं!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                3 -> ServiceSchedulesTab(
                    schedules = serviceSchedules,
                    onAddNew = {
                        isNewSchedule = true
                        showScheduleDialog = ServiceSchedule(serviceId = "service_${System.currentTimeMillis()}")
                    },
                    onEdit = {
                        isNewSchedule = false
                        showScheduleDialog = it
                    },
                    onDelete = {
                        viewModel.deleteServiceSchedule(it.serviceId) { success, _ ->
                            if (success) Toast.makeText(context, "सभा अनुसूची हटाई गई", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                4 -> UsherDelegationTab(
                    branchSettings = branchSettings,
                    allAdmins = viewModel.allAdmins.collectAsState().value,
                    onSaveUshers = { names, uids ->
                        val updated = branchSettings.copy(delegatedUsherNames = names, delegatedUsherUserIds = uids)
                        viewModel.updateBranchAttendanceSettings(updated) { success, _ ->
                            if (success) Toast.makeText(context, "उशर सूची अपडेट की गई!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // Schedule Dialog
    showScheduleDialog?.let { schedule ->
        ServiceScheduleEditDialog(
            schedule = schedule,
            isNew = isNewSchedule,
            onDismiss = { showScheduleDialog = null },
            onSave = { saved ->
                viewModel.saveServiceSchedule(saved) { success, _ ->
                    if (success) {
                        Toast.makeText(context, "सभा समय-सारणी सुरक्षित की गई!", Toast.LENGTH_SHORT).show()
                        showScheduleDialog = null
                    }
                }
            }
        )
    }

    // Physical ID Badges Batch PDF Export Dialog
    if (showPrintBadgesDialog) {
        PrintBadgesDialog(
            viewModel = viewModel,
            branchName = branchSettings.branchName,
            churchPrefix = branchSettings.churchPrefix,
            onDismiss = { showPrintBadgesDialog = false }
        )
    }
}

@Composable
fun LiveTelemetryTab(todayRecords: List<AttendanceRecord>, viewModel: MainViewModel? = null) {
    val totalCount = todayRecords.size
    val menCount = todayRecords.count { it.gender.equals("Male", ignoreCase = true) || it.gender.equals("पुरुष", ignoreCase = true) }
    val womenCount = todayRecords.count { it.gender.equals("Female", ignoreCase = true) || it.gender.equals("महिला", ignoreCase = true) }
    val childCount = totalCount - (menCount + womenCount)

    val offlineQueueCount = viewModel?.attendanceOfflineQueueCount?.collectAsState()?.value ?: 0
    val syncStatusText = viewModel?.attendanceSyncStatusText?.collectAsState()?.value ?: "✓ सिंक पूर्ण"
    val isSyncing = viewModel?.attendanceIsSyncing?.collectAsState()?.value ?: false
    val milestoneAlerts = viewModel?.todayMilestoneAlerts?.collectAsState()?.value ?: emptyList()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pulpit Pastoral Care Milestone Announcements Carousel
        if (milestoneAlerts.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.2.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎂💍", fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "आज के विशेष अवसर (Pulpit Announcements)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B)
                            ) {
                                Text(
                                    text = "${milestoneAlerts.size} अवसर",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        milestoneAlerts.forEach { alert ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = alert.formattedMessage,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF78350F)
                                        )
                                        Text(
                                            text = if (alert.isToday) "आज विशेष अवसर है • वेदी से आशीष प्रार्थना करें" else "निकटवर्ती अवसर (${alert.dateStr})",
                                            fontSize = 10.sp,
                                            color = Color(0xFFB45309)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "📢 वेदी उद्घोषणा: ${alert.memberName} के लिए प्रार्थना की जा रही है!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("आशीष दें", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Offline Sync Status Banner
        if (offlineQueueCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("📡 $syncStatusText", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB45309))
                                Text("नेटवर्क मिलते ही रिकॉर्ड्स स्वतः अपलोड होंगे", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Button(
                            onClick = { viewModel?.forceSyncOfflineAttendance() },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("सिंक करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Headcount Gauge Cards
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👥 आज की लाइव हेडकाउंट गेज (Real-Time Gauge)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("कुल उपस्थिति", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                                Text("$totalCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }

                        // Men
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF2563EB)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("👨 पुरुष", fontSize = 11.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                                Text("$menCount", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                            }
                        }

                        // Women
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEC4899).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFEC4899)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("👩 महिलाएँ", fontSize = 11.sp, color = Color(0xFFBE185D), fontWeight = FontWeight.Bold)
                                Text("$womenCount", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBE185D))
                            }
                        }

                        // Children
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🧒 बच्चे", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                                Text("${childCount.coerceAtLeast(0)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                        }
                    }
                }
            }
        }

        // Live Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "आगमन लाइव स्ट्रीम (${todayRecords.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "● लाइव सिंक सक्रिय",
                    fontSize = 11.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (todayRecords.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("आज अभी तक कोई उपस्थिति दर्ज नहीं हुई है।", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                        Text("उशर स्कैनर या वेन्यू QR द्वारा चेक-इन शुरू करें।", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(todayRecords) { record ->
                AttendanceRecordCard(record = record)
            }
        }
    }
}

@Composable
fun AttendanceRecordCard(record: AttendanceRecord) {
    val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
    val timeStr = remember(record.checkInTimestamp) { timeFormat.format(Date(record.checkInTimestamp)) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = record.memberName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.memberName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GoldWarm.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = record.memberSerial,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = GoldWarm,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "समय: $timeStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "• ${record.gender}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Method Badge & Offline Status
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (record.checkInMethod) {
                        "usher_scan" -> MaterialTheme.colorScheme.primaryContainer
                        "manual_usher_search" -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                        "venue_qr_dynamic" -> Color(0xFF10B981).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (record.checkInMethod) {
                            "usher_scan" -> "उशर स्कैन"
                            "manual_usher_search" -> "मैन्युअल खोज"
                            "venue_qr_dynamic" -> "TV स्क्रीन"
                            else -> "वॉल QR"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (record.checkInMethod) {
                            "usher_scan" -> MaterialTheme.colorScheme.primary
                            "manual_usher_search" -> Color(0xFF7C3AED)
                            "venue_qr_dynamic" -> Color(0xFF059669)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (!record.isOfflineSynced) {
                    Spacer(Modifier.height(2.dp))
                    Text("ऑफलाइन", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }
            }
        }
    }
}

@Composable
fun AbsenteeCareTab(absenteeList: List<AbsenteeCareMember>, branchName: String) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("सुधि ट्रैकर (Absentee Pastoral Care)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB45309))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "जो विश्वासी 2 या 3+ रविवारों से अनुपस्थित हैं, उनकी सुधि लेने और आत्मिक कुशलक्षेम जानने हेतु 1-टैप व्हाट्सऐप केयर संदेश भेजें।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (absenteeList.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("बहुत खूब! कोई भी विश्वासी लगातार अनुपस्थित नहीं है।", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(absenteeList) { member ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (member.severityLevel == "CRITICAL") MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(member.memberName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GoldWarm.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        member.memberSerial,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = GoldWarm,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (member.severityLevel == "CRITICAL") MaterialTheme.colorScheme.errorContainer else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = if (member.severityLevel == "CRITICAL") "🚨 ${member.consecutiveAbsenceCount} रविवार अनुपस्थित (क्रिटिकल)" else "⚠️ ${member.consecutiveAbsenceCount} रविवार अनुपस्थित",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (member.severityLevel == "CRITICAL") MaterialTheme.colorScheme.error else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "अंतिम उपस्थिति: ${member.lastAttendedDate} • फोन: ${member.phone.ifBlank { "उपलब्ध नहीं" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(Modifier.height(10.dp))

                        // Actions: WhatsApp Care
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val text = "जय मसीह की ${member.memberName} जी! 🙏 हमने आज ${branchName} की आराधना में आपको मिस किया। आशा है आप और आपका परिवार प्रभु के अनुग्रह में सकुशल हैं। यदि कोई विशेष प्रार्थना निवेदन हो तो हमें अवश्य बताएं।"
                                    val phone = member.phone.filter { it.isDigit() }
                                    val url = if (phone.isNotBlank()) "https://wa.me/$phone?text=${Uri.encode(text)}" else "https://wa.me/?text=${Uri.encode(text)}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("व्हाट्सऐप सुधि संदेश", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            if (member.phone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.phone}"))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("कॉल करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
fun CheckInModeAndGeoFenceTab(
    branchSettings: BranchAttendanceSettings,
    globalConfig: AttendanceGlobalConfig,
    isMasterAdmin: Boolean,
    onSaveBranchSettings: (BranchAttendanceSettings) -> Unit,
    onSaveGlobalConfig: (AttendanceGlobalConfig) -> Unit
) {
    var activeMode by remember { mutableStateOf(branchSettings.activeMode) }
    var venueQrType by remember { mutableStateOf(branchSettings.venueQrType) }
    var latText by remember { mutableStateOf(branchSettings.latitude.toString()) }
    var lngText by remember { mutableStateOf(branchSettings.longitude.toString()) }
    var radius by remember { mutableStateOf(branchSettings.allowedRadiusMeters) }
    var offlineCheckIn by remember { mutableStateOf(branchSettings.isOfflineCheckInEnabled) }

    var isServiceActive by remember { mutableStateOf(globalConfig.isAttendanceServiceActive) }
    var enforceGeoGlobally by remember { mutableStateOf(globalConfig.enforceGeoFencingGlobally) }
    var preventMultiDevice by remember { mutableStateOf(globalConfig.preventDeviceMultiCheckIn) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Admin Global Governance (if Master Admin)
        if (isMasterAdmin) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldWarm.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("मास्टर एडमिन ग्लोबल नीतियां (Global Governance)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldWarm)
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ग्लोबल उपस्थिति मास्टर स्विच", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("सभी शाखाओं के लिए उपस्थिति मॉड्यूल चालू/बंद", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(checked = isServiceActive, onCheckedChange = { isServiceActive = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("जियो-फेंसिंग अनिवार्य (Geo-Fencing)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("चर्च परिसर के बाहर से वेन्यू स्कैन अस्वीकार करें", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(checked = enforceGeoGlobally, onCheckedChange = { enforceGeoGlobally = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("एक डिवाइस - एकल चेक-इन (Anti-Fraud)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("एक मोबाइल से कई सदस्यों की हाजिरी रोकें", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(checked = preventMultiDevice, onCheckedChange = { preventMultiDevice = it })
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onSaveGlobalConfig(
                                    globalConfig.copy(
                                        isAttendanceServiceActive = isServiceActive,
                                        enforceGeoFencingGlobally = enforceGeoGlobally,
                                        preventDeviceMultiCheckIn = preventMultiDevice
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ग्लोबल नीतियां सहेजें", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Branch Level Settings
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("स्थानीय शाखा चेक-इन मोड (Check-in Mode)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))

                    val modes = listOf(
                        "dual_mode" to "दोनों चालू (उशर + सेल्फ स्कैन)",
                        "usher_scans_member" to "उशर केवल (Usher Only)",
                        "member_scans_venue_qr" to "वेन्यू QR केवल (Member Self-Scan)"
                    )

                    modes.forEach { (modeKey, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeMode = modeKey }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = activeMode == modeKey, onClick = { activeMode = modeKey })
                            Spacer(Modifier.width(6.dp))
                            Text(label, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("वेन्यू QR प्रकार (Venue QR Behavior)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    val qrTypes = listOf(
                        "static_wall_poster" to "दीवार का स्थिर पोस्टर (Static Wall QR)",
                        "dynamic_rotating_screen" to "डायनामिक स्क्रीन TV (HMAC 30s टोकन)"
                    )

                    qrTypes.forEach { (typeKey, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { venueQrType = typeKey }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = venueQrType == typeKey, onClick = { venueQrType = typeKey })
                            Spacer(Modifier.width(6.dp))
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Geo-Fencing Configuration
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("चर्च वेन्यू GPS व जियो-फेंस परिधि", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text("अक्षांश (Lat)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lngText,
                            onValueChange = { lngText = it },
                            label = { Text("देशांतर (Lng)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("अनुमत परिधि त्रिज्या (Allowed Radius): $radius मीटर", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    Slider(
                        value = radius.toFloat(),
                        onValueChange = { radius = it.toInt() },
                        valueRange = 25f..200f,
                        steps = 6
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(25, 50, 100, 200).forEach { r ->
                            SuggestionChip(
                                onClick = { radius = r },
                                label = { Text("${r}m", fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Save Branch Settings Button
        item {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: branchSettings.latitude
                    val lng = lngText.toDoubleOrNull() ?: branchSettings.longitude
                    onSaveBranchSettings(
                        branchSettings.copy(
                            activeMode = activeMode,
                            venueQrType = venueQrType,
                            latitude = lat,
                            longitude = lng,
                            allowedRadiusMeters = radius,
                            isOfflineCheckInEnabled = offlineCheckIn
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("शाखा सेटिंग्स सुरक्षित करें", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ServiceSchedulesTab(
    schedules: List<ServiceSchedule>,
    onAddNew: () -> Unit,
    onEdit: (ServiceSchedule) -> Unit,
    onDelete: (ServiceSchedule) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("सभा समय-सारणी व चेक-इन विंडो", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Button(
                    onClick = onAddNew,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("नई सभा जोड़ें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(schedules) { schedule ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(schedule.serviceName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "${schedule.getDayNameHindi()} • ${schedule.serviceStartTime} - ${schedule.serviceEndTime}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "चेक-इन बफर: -${schedule.checkInWindowOpenMinutesBefore} मि. पहले से +${schedule.checkInWindowCloseMinutesAfter} मि. बाद तक",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(onClick = { onEdit(schedule) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { onDelete(schedule) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun UsherDelegationTab(
    branchSettings: BranchAttendanceSettings,
    allAdmins: List<AdminUser>,
    onSaveUshers: (List<String>, List<String>) -> Unit
) {
    var newUsherName by remember { mutableStateOf("") }
    val currentUsherNames = remember(branchSettings) { branchSettings.delegatedUsherNames.toMutableStateList() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("उशर / सेवक दल प्रतिनिधि (Usher Delegation)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "पास्टर युवा या वालंटियर्स को बिना प्रशासनिक पद दिए केवल उपस्थिति स्कैन करने का अधिकार दे सकते हैं।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newUsherName,
                            onValueChange = { newUsherName = it },
                            placeholder = { Text("उशर / सेवक का नाम") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newUsherName.isNotBlank()) {
                                    currentUsherNames.add(newUsherName.trim())
                                    onSaveUshers(currentUsherNames.toList(), emptyList())
                                    newUsherName = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("जोड़ें")
                        }
                    }
                }
            }
        }

        item {
            Text("अधिकृत उशर सूची (${currentUsherNames.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        if (currentUsherNames.isEmpty()) {
            item {
                Text("कोई अतिरिक्त उशर प्रतिनिधि नियुक्त नहीं है। पास्टर व एडमिन स्वयं स्कैन कर सकते हैं।", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            items(currentUsherNames) { name ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        IconButton(
                            onClick = {
                                currentUsherNames.remove(name)
                                onSaveUshers(currentUsherNames.toList(), emptyList())
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceScheduleEditDialog(
    schedule: ServiceSchedule,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (ServiceSchedule) -> Unit
) {
    var serviceName by remember { mutableStateOf(schedule.serviceName) }
    var dayOfWeek by remember { mutableStateOf(schedule.dayOfWeek) }
    var startTime by remember { mutableStateOf(schedule.serviceStartTime) }
    var endTime by remember { mutableStateOf(schedule.serviceEndTime) }
    var bufferBefore by remember { mutableStateOf(schedule.checkInWindowOpenMinutesBefore.toString()) }
    var bufferAfter by remember { mutableStateOf(schedule.checkInWindowCloseMinutesAfter.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (isNew) "नई कलीसिया सभा जोड़ें" else "सभा अनुसूची संपादित करें",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("सभा का नाम") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text("सप्ताह का दिन:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val days = listOf("रवि", "सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि")
                    items(days.indices.toList()) { index ->
                        FilterChip(
                            selected = dayOfWeek == index,
                            onClick = { dayOfWeek = index },
                            label = { Text(days[index], fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("प्रारंभ (09:00)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("समापन (11:30)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bufferBefore,
                        onValueChange = { bufferBefore = it },
                        label = { Text("पहले विंडो (मि.)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bufferAfter,
                        onValueChange = { bufferAfter = it },
                        label = { Text("बाद कट-ऑफ (मि.)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("रद्द करें")
                    }
                    Button(
                        onClick = {
                            if (serviceName.isNotBlank()) {
                                onSave(
                                    schedule.copy(
                                        serviceName = serviceName.trim(),
                                        dayOfWeek = dayOfWeek,
                                        serviceStartTime = startTime.trim(),
                                        serviceEndTime = endTime.trim(),
                                        checkInWindowOpenMinutesBefore = bufferBefore.toIntOrNull() ?: 30,
                                        checkInWindowCloseMinutesAfter = bufferAfter.toIntOrNull() ?: 60
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("सुरक्षित करें")
                    }
                }
            }
        }
    }
}

@Composable
fun PrintBadgesDialog(
    viewModel: MainViewModel,
    branchName: String,
    churchPrefix: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allProfiles by viewModel.appUserProfiles.collectAsState()
    var selectedRoleFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMemberSerials by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectAll by remember { mutableStateOf(true) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    val filteredProfiles = remember(allProfiles, selectedRoleFilter, searchQuery) {
        val base = if (allProfiles.isNotEmpty()) allProfiles else viewModel.attendanceGovernanceRepository.getDefaultDemoMembers()
        base.filter { member ->
            val matchesRole = when (selectedRoleFilter) {
                "ALL" -> true
                "PASTOR" -> member.roleTier.contains("pastor", ignoreCase = true) || member.roleTier.contains("elder", ignoreCase = true)
                "BELIEVER" -> member.roleTier.contains("believer", ignoreCase = true)
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                member.displayName.lowercase().contains(q) ||
                member.fullName.lowercase().contains(q) ||
                member.serialNumber.lowercase().contains(q)
            }
            matchesRole && matchesQuery
        }
    }

    LaunchedEffect(filteredProfiles, selectAll) {
        if (selectAll) {
            selectedMemberSerials = filteredProfiles.map { it.serialNumber.ifBlank { it.userId } }.toSet()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GoldWarm.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "🖨️ फिजिकल आईडी कार्ड प्रिंट (A4 Batch)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "CR80 Lamination Standard (8 कार्ड/पेज)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Role Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedRoleFilter == "ALL",
                        onClick = { selectedRoleFilter = "ALL" },
                        label = { Text("समस्त सदस्य (${filteredProfiles.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedRoleFilter == "BELIEVER",
                        onClick = { selectedRoleFilter = "BELIEVER" },
                        label = { Text("विश्वासी", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedRoleFilter == "PASTOR",
                        onClick = { selectedRoleFilter = "PASTOR" },
                        label = { Text("पास्टर / सेवक", fontSize = 11.sp) }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("नाम या SN से फिल्टर करें...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // Multi-Select Member Checkboxes (Scrollable)
                Text(
                    text = "प्रिंट हेतु चयनित सदस्य: ${selectedMemberSerials.size} / ${filteredProfiles.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredProfiles) { member ->
                            val serial = member.serialNumber.ifBlank { member.userId }
                            val isSelected = selectedMemberSerials.contains(serial)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMemberSerials = if (isSelected) {
                                            selectedMemberSerials - serial
                                        } else {
                                            selectedMemberSerials + serial
                                        }
                                        selectAll = false
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedMemberSerials = if (checked) selectedMemberSerials + serial else selectedMemberSerials - serial
                                        selectAll = false
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = member.fullName.ifBlank { member.displayName.ifBlank { "सदस्य" } },
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GoldWarm.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = serial,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldWarm,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Actions: Print / Share PDF
                Button(
                    onClick = {
                        isGeneratingPdf = true
                        val selectedList = filteredProfiles.filter { selectedMemberSerials.contains(it.serialNumber.ifBlank { it.userId }) }
                        val pdfUri = com.example.util.BadgesPdfGeneratorService.generateBadgesPdf(
                            context = context,
                            members = selectedList,
                            branchName = branchName,
                            churchPrefix = churchPrefix
                        )
                        isGeneratingPdf = false
                        if (pdfUri != null) {
                            com.example.util.BadgesPdfGeneratorService.printOrShareBadgesPdf(context, pdfUri)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "PDF जनरेट करने में त्रुटि हुई", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = selectedMemberSerials.isNotEmpty() && !isGeneratingPdf,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("🖨️ A4 बैच PDF प्रिंट / शेयर करें (${selectedMemberSerials.size} कार्ड)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
