package com.example.ui.attendance

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AttendanceSecurityHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsherScannerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val branchSettings by viewModel.branchAttendanceSettings.collectAsState()
    val activeWindow by viewModel.activeServiceWindow.collectAsState()
    val currentAdmin by viewModel.currentAdmin.collectAsState()
    val todayRecords by viewModel.todayAttendanceRecords.collectAsState()
    val offlineQueueCount by viewModel.attendanceOfflineQueueCount.collectAsState()
    val syncStatusText by viewModel.attendanceSyncStatusText.collectAsState()
    val isSyncing by viewModel.attendanceIsSyncing.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var manualSerialInput by remember { mutableStateOf("") }
    var lastScannedRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var scannedMilestoneAlerts by remember { mutableStateOf<List<PastoralMilestoneAlert>>(emptyList()) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessingScan by remember { mutableStateOf(false) }
    var sessionScanCount by remember { mutableIntStateOf(0) }

    // Family Pass Batch BottomSheet State
    var activeFamilyUnitForBatch by remember { mutableStateOf<FamilyUnit?>(null) }
    var familyMemberSelection by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isSubmittingFamilyBatch by remember { mutableStateOf(false) }

    // Speech-to-Text Voice Search State
    var isVoiceListening by remember { mutableStateOf(false) }
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isVoiceListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                searchQuery = spoken
                Toast.makeText(context, "🎤 आवाज खोजी: $spoken", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Search results query
    val searchResults = remember(searchQuery) {
        if (searchQuery.isNotBlank()) {
            viewModel.searchAttendanceMembers(searchQuery)
        } else {
            emptyList()
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            }
        } catch (_: Exception) {}
    }

    fun launchSpeechToText() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PROMPT, "सदस्य का नाम या सीरियल नंबर बोलें (Speak name or Serial)...")
            }
            isVoiceListening = true
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            isVoiceListening = false
            Toast.makeText(context, "वॉइस सर्च उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
        }
    }

    val handleScanResult: (String, String, String) -> Unit = { rawScanned, memberName, method ->
        if (!isProcessingScan && rawScanned.isNotBlank()) {
            val cleanRaw = rawScanned.trim()

            // Check if Family Pass QR Payload
            if (AttendanceSecurityHelper.isFamilyPassPayload(cleanRaw)) {
                val famInfo = AttendanceSecurityHelper.extractFamilyPassInfo(cleanRaw)
                val familyId = famInfo?.first ?: ""
                val headSerial = famInfo?.second ?: ""
                val familyUnit = viewModel.getFamilyUnitByHeadSerialOrId(familyId, headSerial)
                
                // Initialize all family members selected by default
                val initialMap = familyUnit.members.associate { it.memberSerial to true }
                familyMemberSelection = initialMap
                activeFamilyUnitForBatch = familyUnit
                triggerHapticFeedback()
            } else {
                // Standard Single Member Check-in
                isProcessingScan = true
                scanErrorMessage = null

                viewModel.processSmartCheckIn(
                    memberSerial = cleanRaw,
                    memberName = memberName,
                    checkInMethod = method,
                    verifiedByAdmin = currentAdmin
                ) { success, error, record ->
                    isProcessingScan = false
                    if (success && record != null) {
                        triggerHapticFeedback()
                        lastScannedRecord = record
                        sessionScanCount++
                        manualSerialInput = ""
                        searchQuery = ""

                        // Check for pastoral milestones (Birthday / Anniversary)
                        val member = searchResults.find { it.serialNumber.equals(record.memberSerial, ignoreCase = true) }
                        if (member != null) {
                            scannedMilestoneAlerts = AttendanceSecurityHelper.evaluatePastoralMilestones(
                                dobStr = member.dateOfBirth,
                                anniversaryStr = member.anniversaryDate,
                                memberName = record.memberName,
                                memberSerial = record.memberSerial
                            )
                        } else {
                            scannedMilestoneAlerts = emptyList()
                        }

                        coroutineScope.launch {
                            delay(4000)
                            if (lastScannedRecord == record) {
                                lastScannedRecord = null
                                scannedMilestoneAlerts = emptyList()
                            }
                        }
                    } else {
                        scanErrorMessage = error ?: "उपस्थिति दर्ज नहीं हो सकी"
                        coroutineScope.launch {
                            delay(3500)
                            scanErrorMessage = null
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "उशर अटेंडेंस स्कैनर (Usher Scanner)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${branchSettings.branchName} • आज: ${todayRecords.size} • सत्र: $sessionScanCount",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("usher_scanner_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (offlineQueueCount > 0) {
                        IconButton(
                            onClick = {
                                viewModel.forceSyncOfflineAttendance { success, count ->
                                    if (success) {
                                        Toast.makeText(context, "$count रिकॉर्ड्स सिंक हो गए!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "सिंक विफल, इंटरनेट जांचें", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GoldWarm, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = "Sync", tint = GoldWarm)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Offline Status Pill Banner with Loading Animation & Progress Indicator
            Surface(
                color = when {
                    isSyncing -> Color(0xFF6366F1).copy(alpha = 0.15f)
                    offlineQueueCount > 0 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                    else -> Color(0xFF10B981).copy(alpha = 0.12f)
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        isSyncing -> Color(0xFF6366F1).copy(alpha = 0.5f)
                        offlineQueueCount > 0 -> Color(0xFFF59E0B).copy(alpha = 0.5f)
                        else -> Color(0xFF10B981).copy(alpha = 0.4f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF6366F1),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (offlineQueueCount > 0) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (offlineQueueCount > 0) Color(0xFFD97706) else Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = syncStatusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSyncing -> Color(0xFF4F46E5)
                                    offlineQueueCount > 0 -> Color(0xFFB45309)
                                    else -> Color(0xFF047857)
                                }
                            )
                            if (isSyncing) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(3.dp)
                                        .padding(top = 2.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFF6366F1),
                                    trackColor = Color(0xFF6366F1).copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    if (offlineQueueCount > 0 && !isSyncing) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                viewModel.forceSyncOfflineAttendance { success, count ->
                                    if (success) {
                                        Toast.makeText(context, "$count रिकॉर्ड्स सिंक हो गए!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "सिंक विफल, इंटरनेट कनेक्शन जांचें", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "अभी सिंक करें",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Success Banner Toast (Instant Feedback with Milestone Badges)
            AnimatedVisibility(
                visible = lastScannedRecord != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                lastScannedRecord?.let { record ->
                    Surface(
                        color = if (record.isOfflineSynced) Color(0xFF10B981) else Color(0xFF2563EB),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "✓ [ ${record.memberSerial} ] ${record.memberName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (record.isOfflineSynced) "उपस्थिति दर्ज! (${record.serviceName})" else "✓ सुरक्षित (ऑफलाइन मोड - नेटवर्क आने पर स्वतः सिंक)",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            // Pastoral Care Milestone Alerts on Scan (Birthday / Anniversary)
                            if (scannedMilestoneAlerts.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    scannedMilestoneAlerts.forEach { alert ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (alert.type == "BIRTHDAY") Color(0xFFF59E0B) else Color(0xFF8B5CF6),
                                            contentColor = Color.White
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (alert.type == "BIRTHDAY") "🎂 ${alert.title}" else "💍 ${alert.title}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Error Banner Toast
            AnimatedVisibility(
                visible = scanErrorMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                scanErrorMessage?.let { err ->
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = err,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // High-Speed Camera Scanner Simulation Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .border(2.dp, if (lastScannedRecord != null) Color(0xFF10B981) else GoldWarm, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (lastScannedRecord != null) Icons.Default.Check else Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = if (lastScannedRecord != null) Color(0xFF10B981) else GoldWarm,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isProcessingScan) "प्रोसेसिंग..." else "डिजिटल बैज / फैमिली पास QR दिखाएं",
                                color = Color.White,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "⚡ सतत उशर स्कैनर (Line Clearance: <1s • 👨‍👩‍👧‍👦 फैमिली पास समर्थित)",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }

            // Speech-to-Text Listening Pulse Animation
            if (isVoiceListening) {
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "🎙️ सुन रहे हैं... (Listening for Name / Serial)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            // Manual Search & Directory Fallback Section with Speech-to-Text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Search Row with Active Voice Search Microphone
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        placeholder = { Text("🔍 नाम या सीरियल नंबर खोजें...", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_manual_search"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    FilledTonalIconButton(
                        onClick = { launchSpeechToText() },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isVoiceListening) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isVoiceListening) Color(0xFFDC2626) else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // If Search is Active: Show Matching Member List with 1-Tap Check-In
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "खोज परिणाम (${searchResults.size} सदस्य मिले):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(4.dp))

                    if (searchResults.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("कोई सदस्य नहीं मिला।", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.height(6.dp))
                                Button(
                                    onClick = { handleScanResult(searchQuery.trim().uppercase(), "", "manual_usher_search") },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("'$searchQuery' को सीधे चेक-इन करें", fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(searchResults) { member ->
                                val memberSerial = member.serialNumber.ifBlank { member.userId }
                                val memberName = member.displayName.ifBlank { member.fullName.ifBlank { "सदस्य $memberSerial" } }
                                val isAlreadyCheckedIn = todayRecords.any { it.memberSerial == memberSerial }

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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(memberName.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(memberName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = GoldWarm.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            memberSerial,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = GoldWarm,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                if (member.phoneNumber.isNotBlank() || member.phone.isNotBlank()) {
                                                    Text(
                                                        text = "📞 ${member.phoneNumber.ifBlank { member.phone }}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }

                                        if (isAlreadyCheckedIn) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "✓ उपस्थित",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    handleScanResult(memberSerial, memberName, "manual_usher_search")
                                                },
                                                enabled = !isProcessingScan,
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text("हाजिरी दर्ज करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Default View: Recent Scans & Quick Keypad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "हालिया उपस्थिति (${todayRecords.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "आज का सत्र",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(todayRecords.take(8)) { rec ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(rec.memberName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text("[ ${rec.memberSerial} ]", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = GoldWarm)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!rec.isOfflineSynced) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "ऑफलाइन",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD97706),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(6.dp))
                                        }

                                        Text(
                                            text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(rec.checkInTimestamp)),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1-Tap Family Pass Batch Check-in BottomSheet Modal (<50ms fast popup)
    activeFamilyUnitForBatch?.let { familyUnit ->
        ModalBottomSheet(
            onDismissRequest = { activeFamilyUnitForBatch = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👨‍👩‍👧‍👦", fontSize = 20.sp)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = familyUnit.familyName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "कुल ${familyUnit.members.size} सदस्य • बैच क्यूआर अटेंडेंस",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(onClick = { activeFamilyUnitForBatch = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "हाजिरी हेतु उपस्थित सदस्यों का चयन करें:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(8.dp))

                // Family Member Checkbox List
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    familyUnit.members.forEach { member ->
                        val isChecked = familyMemberSelection[member.memberSerial] ?: true
                        val isAlreadyChecked = todayRecords.any { it.memberSerial == member.memberSerial }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                1.dp,
                                if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isAlreadyChecked) {
                                    familyMemberSelection = familyMemberSelection.toMutableMap().apply {
                                        put(member.memberSerial, !isChecked)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (!isAlreadyChecked) {
                                                familyMemberSelection = familyMemberSelection.toMutableMap().apply {
                                                    put(member.memberSerial, checked)
                                                }
                                            }
                                        },
                                        enabled = !isAlreadyChecked
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(member.memberName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = GoldWarm.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = member.memberSerial,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    color = GoldWarm,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "नाता: ${member.relationship} • ${member.gender}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                if (isAlreadyChecked) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "✓ उपस्थित",
                                            color = Color(0xFF047857),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                val selectedCount = familyUnit.members.count { familyMemberSelection[it.memberSerial] == true && !todayRecords.any { rec -> rec.memberSerial == it.memberSerial } }

                Button(
                    onClick = {
                        isSubmittingFamilyBatch = true
                        val selectedList = familyUnit.members.filter { familyMemberSelection[it.memberSerial] == true }
                        viewModel.processFamilyBatchCheckIn(familyUnit, selectedList) { success, records, err ->
                            isSubmittingFamilyBatch = false
                            activeFamilyUnitForBatch = null
                            if (success && records.isNotEmpty()) {
                                triggerHapticFeedback()
                                sessionScanCount += records.size
                                lastScannedRecord = records.firstOrNull()
                                Toast.makeText(context, "✓ पूरे परिवार (${records.size} सदस्य) की हाजिरी दर्ज हुई!", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    delay(4000)
                                    if (lastScannedRecord == records.firstOrNull()) {
                                        lastScannedRecord = null
                                    }
                                }
                            } else {
                                Toast.makeText(context, err ?: "पारिवारिक हाजिरी दर्ज नहीं हो सकी", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = selectedCount > 0 && !isSubmittingFamilyBatch,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isSubmittingFamilyBatch) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.GroupAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "➕ सभी चयनित की हाजिरी लगाएं ($selectedCount सदस्य)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
