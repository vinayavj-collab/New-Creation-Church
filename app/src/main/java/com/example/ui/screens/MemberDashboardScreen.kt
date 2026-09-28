package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.AdminUser
import com.example.data.model.AttendanceRecord
import com.example.data.model.UpcomingEvent
import com.example.data.model.UserProfileData
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AttendanceSecurityHelper
import com.example.util.QrCodeHelper

/**
 * Dedicated Member Dashboard UI featuring:
 * 1. 'My Profile' Section
 * 2. 'Digital Badge' (Church Digital ID & QR Gateway)
 * 3. 'Reading Plans' Section (Daily Bible Reading & Active Streak)
 * 4. 'Upcoming Events' Section (Church Calendar & Fellowship)
 * 5. Spiritual Engagement Metrics & Quick Actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    onOpenReadingPlans: () -> Unit,
    onOpenUpcomingEvents: () -> Unit,
    onOpenBible: (bookId: Int, chapter: Int, verse: Int) -> Unit = { _, _, _ -> },
    onOpenPrayer: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val currentAdmin by viewModel.currentAdmin.collectAsState()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "सदस्य डैशबोर्ड (Member Dashboard)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (userProfile.displayName.isNotBlank()) "जय मसीह की, ${userProfile.displayName} जी! 🙏" else "कलीसिया प्रबंधन पोर्टल",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("member_dashboard_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "वापस जाएं")
                    }
                },
                actions = {
                    if (currentAdmin != null) {
                        IconButton(onClick = onOpenAdminPanel, modifier = Modifier.testTag("member_dashboard_admin_button")) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "एडमिन पैनल", tint = GoldWarm)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        MemberDashboardContent(
            userProfile = userProfile,
            currentAdmin = currentAdmin,
            upcomingEvents = upcomingEvents,
            onEditProfileClick = onEditProfileClick,
            onOpenReadingPlans = onOpenReadingPlans,
            onOpenUpcomingEvents = onOpenUpcomingEvents,
            onOpenBible = onOpenBible,
            onOpenPrayer = onOpenPrayer,
            onOpenNotes = onOpenNotes,
            onOpenAdminPanel = onOpenAdminPanel,
            viewModel = viewModel,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun MemberDashboardContent(
    userProfile: UserProfileData,
    currentAdmin: AdminUser?,
    upcomingEvents: List<UpcomingEvent>,
    onEditProfileClick: () -> Unit,
    onOpenReadingPlans: () -> Unit,
    onOpenUpcomingEvents: () -> Unit,
    onOpenBible: (bookId: Int, chapter: Int, verse: Int) -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    viewModel: MainViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showQrModal by remember { mutableStateOf(false) }
    var showSelfCheckInDialog by remember { mutableStateOf(false) }

    val activeWindow = viewModel?.activeServiceWindow?.collectAsState()?.value
    val allAttendanceRecords by (viewModel?.allSmartAttendanceRecords?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val serialNumber = userProfile.serialNumber.ifBlank { "NCC01" }
    val memberRole = if (currentAdmin != null) currentAdmin.designation else if (userProfile.role.isNotBlank()) userProfile.role else "सक्रिय सदस्य (Member)"

    var userAttendanceHistory by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(false) }

    LaunchedEffect(serialNumber, userProfile.userId, allAttendanceRecords) {
        if (viewModel != null) {
            userAttendanceHistory = viewModel.getUserAttendanceHistory(serialNumber, userProfile.userId)
            isLoadingHistory = true
            viewModel.refreshUserAttendanceHistory(serialNumber, userProfile.userId) { list ->
                userAttendanceHistory = list
                isLoadingHistory = false
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
    ) {
        // -------------------------------------------------------------
        // SECTION 1: MY PROFILE SUMMARY CARD
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_dashboard_profile_section"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(contentAlignment = Alignment.BottomEnd) {
                            if (userProfile.photoUriOrPath.isNotBlank()) {
                                Image(
                                    painter = rememberAsyncImagePainter(userProfile.photoUriOrPath),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, GoldWarm, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(2.dp, GoldWarm)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = userProfile.displayName.take(1).uppercase().ifBlank { "M" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 26.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = GoldWarm,
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userProfile.displayName.ifBlank { "सदस्य प्रोफ़ाइल" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GoldWarm.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = serialNumber,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = GoldWarm,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = "$memberRole • ${userProfile.churchName.ifBlank { "न्यू क्रिएशन चर्च" }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (userProfile.city.isNotBlank() || userProfile.phoneNumber.isNotBlank()) {
                                Text(
                                    text = listOfNotNull(
                                        userProfile.city.takeIf { it.isNotBlank() },
                                        userProfile.phoneNumber.takeIf { it.isNotBlank() }
                                    ).joinToString(" • "),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Secondary Info Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (userProfile.isBaptized || userProfile.baptismStatus) Icons.Default.WaterDrop else Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text("बपतिस्मा", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = if (userProfile.isBaptized || userProfile.baptismStatus) "हाँ (पूर्ण)" else "प्रतीक्षारत",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text("विश्वास में वर्ष", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = if (userProfile.yearsInFaith > 0) "${userProfile.yearsInFaith} वर्ष" else "नव विश्वासी",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onEditProfileClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_dashboard_edit_profile")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("प्रोफ़ाइल विवरण संपादित करें (Edit Profile)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 2: DIGITAL BADGE (CHURCH DIGITAL ID CARD & QR)
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_dashboard_digital_badge_section"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Premium dark ID badge style
                border = BorderStroke(1.5.dp, GoldWarm)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "डिजिटल सदस्यता पास (Digital Badge)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = "● VERIFIED MEMBER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Digital Badge Family Pass Switch Toggle
                    var selectedBadgeMode by remember { mutableStateOf(if (userProfile.isFamilyHead) "FAMILY" else "INDIVIDUAL") }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedBadgeMode == "INDIVIDUAL") GoldWarm else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedBadgeMode = "INDIVIDUAL" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "👤 व्यक्तिगत पास",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (selectedBadgeMode == "INDIVIDUAL") Color.Black else Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedBadgeMode == "FAMILY") GoldWarm else Color.Transparent,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clickable { selectedBadgeMode = "FAMILY" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "👨‍👩‍👧‍👦 पारिवारिक पास",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (selectedBadgeMode == "FAMILY") Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Member Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedBadgeMode == "FAMILY") "👨‍👩‍👧‍👦 1-टैप फैमिली पास" else "कलीसिया प्रबंधन पोर्टल",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldWarm
                            )
                            Text(
                                text = if (selectedBadgeMode == "FAMILY") "${userProfile.displayName.ifBlank { "डेविड" }} परिवार" else userProfile.displayName.ifBlank { "सदस्य" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("सीरियल नंबर: ", fontSize = 11.sp, color = Color.LightGray)
                                Text(
                                    text = serialNumber,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GoldWarm
                                )
                            }

                            Text(
                                text = if (selectedBadgeMode == "FAMILY") "बैच क्यूआर: 3 सदस्य जुड़े हैं" else "पदनाम: $memberRole",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Right: Interactive QR Code
                        val qrPayload = remember(serialNumber, selectedBadgeMode) {
                            if (selectedBadgeMode == "FAMILY") {
                                AttendanceSecurityHelper.generateFamilyPassPayload(
                                    familyId = userProfile.familyId.ifBlank { "fam_${serialNumber}" },
                                    headSerial = serialNumber
                                )
                            } else {
                                QrCodeHelper.createActivationPayload(serialNumber, role = memberRole)
                            }
                        }
                        val qrBitmap = remember(qrPayload) {
                            QrCodeHelper.generateQrBitmap(qrPayload, sizePx = 250)
                        }

                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(6.dp)
                                    .clickable { showQrModal = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Digital Member QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Badge Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showQrModal = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("QR कोड ज़ूम करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val message = "जय मसीह की! कलीसिया ऐप में मेरी डिजिटल सदस्यता आईडी [ $serialNumber ] है। नाम: ${userProfile.displayName}"
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, message)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "डिजिटल आईडी शेयर करें"))
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("शेयर ID", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Self Check-In Button (Prominently displayed when service window is open)
                    if (viewModel != null) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { showSelfCheckInDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeWindow?.isWindowActive == true) Color(0xFF10B981) else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (activeWindow?.isWindowActive == true) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_member_self_checkin")
                        ) {
                            Icon(
                                imageVector = if (activeWindow?.isWindowActive == true) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (activeWindow?.isWindowActive == true) "हाजिरी दर्ज करें (Self Check-in खुला है)" else "कलीसिया सभा हाजिरी (Smart Check-in)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 2.5: MY ATTENDANCE HISTORY (मेरी उपस्थिति इतिहास)
        // -------------------------------------------------------------
        if (viewModel != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_dashboard_attendance_history_section"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "मेरी उपस्थिति इतिहास (Attendance History)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "कुल ${userAttendanceHistory.size} सभाएं",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (isLoadingHistory && userAttendanceHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (userAttendanceHistory.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.FactCheck,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "अभी तक कोई उपस्थिति रिकॉर्ड नहीं मिला है।",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "कलीसिया सभाओं में उपस्थित होकर डिजिटल बैज अथवा वेन्यू QR से हाजिरी दर्ज करें।",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                userAttendanceHistory.take(5).forEach { record ->
                                    val timeStr = remember(record.checkInTimestamp) {
                                        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(record.checkInTimestamp))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color(0xFF10B981),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.width(10.dp))

                                                Column {
                                                    Text(
                                                        text = record.serviceName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = record.serviceDate,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = "• $timeStr",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
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
                                                        "manual_usher_search" -> "मैन्युअल"
                                                        "venue_qr_dynamic" -> "TV स्क्रीन"
                                                        else -> "वॉल QR"
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (record.checkInMethod) {
                                                        "usher_scan" -> MaterialTheme.colorScheme.primary
                                                        "manual_usher_search" -> Color(0xFF7C3AED)
                                                        "venue_qr_dynamic" -> Color(0xFF059669)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
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
            }
        }

        // -------------------------------------------------------------
        // SECTION 3: BIBLE READING PLANS & ACTIVE STREAK
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_dashboard_reading_plans_section"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "बाइबल पठन योजना (Reading Plans)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("7 दिन स्ट्रीक 🔥", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Active Reading Plan Details Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "365-दिवसीय संपूर्ण बाइबल पठन",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "35% पूर्ण",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { 0.35f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("आज का निर्धारित अध्याय:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text("उत्पत्ति अध्याय 15 (Genesis 15)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onOpenBible(1, 15, 1) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("पढ़ें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onOpenReadingPlans,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("सभी पठन योजनाएं देखें (View All Plans)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 4: UPCOMING EVENTS & FELLOWSHIP
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_dashboard_events_section"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "आगामी कार्यक्रम (Upcoming Events)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        TextButton(onClick = onOpenUpcomingEvents) {
                            Text("सभी देखें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (upcomingEvents.isNotEmpty()) {
                        upcomingEvents.take(2).forEach { event ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${event.dateString} ${event.timeString ?: ""}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (!event.locationString.isNullOrBlank()) {
                                            Text(
                                                text = "📍 ${event.locationString}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Fallback sample upcoming events
                        val defaultEvents = remember {
                            listOf(
                                Triple("रविवार की मुख्य आराधना सेवा", "रविवार, सुबह 9:00 बजे", "मुख्य चर्च हॉल"),
                                Triple("युवा प्रार्थना संगति व बाइबल अध्ययन", "शुक्रवार, शाम 6:30 बजे", "फेलोशिप हॉल")
                            )
                        }

                        defaultEvents.forEach { (title, date, loc) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = date,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "📍 $loc",
                                            fontSize = 11.sp,
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

        // -------------------------------------------------------------
        // SECTION 5: QUICK ACTIONS GRID
        // -------------------------------------------------------------
        item {
            Column {
                Text(
                    text = "त्वरित सुविधाएं (Quick Services)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        onClick = onOpenPrayer,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text("प्रार्थना भेजें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        onClick = onOpenNotes,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(4.dp))
                            Text("नोट्स बनाएं", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (currentAdmin != null) {
                        Card(
                            onClick = onOpenAdminPanel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = GoldWarm.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldWarm)
                                Spacer(Modifier.height(4.dp))
                                Text("एडमिन पोर्टल", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldWarm)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog for QR Expansion
    if (showQrModal) {
        Dialog(onDismissRequest = { showQrModal = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "सदस्यता गेटवे QR कोड",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${userProfile.displayName.ifBlank { "सदस्य" }} • SN: $serialNumber",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    val modalQrPayload = remember(serialNumber) {
                        QrCodeHelper.createActivationPayload(serialNumber, role = memberRole)
                    }
                    val modalQrBitmap = remember(modalQrPayload) {
                        QrCodeHelper.generateQrBitmap(modalQrPayload, sizePx = 600)
                    }

                    if (modalQrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = modalQrBitmap.asImageBitmap(),
                                contentDescription = "Full QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "उपस्थिति अथवा गेटवे ऑथेंटिकेशन के लिए यह QR कोड स्कैन करें।",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { showQrModal = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("बंद करें", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSelfCheckInDialog && viewModel != null) {
        com.example.ui.attendance.MemberSelfCheckInDialog(
            viewModel = viewModel,
            userProfile = userProfile,
            onDismiss = { showSelfCheckInDialog = false },
            onSuccess = {
                Toast.makeText(context, "उपस्थिति दर्ज की गई! 🎉", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
