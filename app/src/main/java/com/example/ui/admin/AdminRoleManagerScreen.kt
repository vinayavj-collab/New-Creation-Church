package com.example.ui.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.GoldAccent
import com.example.ui.components.SerialQrCard
import com.example.ui.components.SerialQrDisplayDialog
import com.example.ui.viewmodel.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminRoleManagerScreen(
    viewModel: MainViewModel,
    currentAdmin: AdminUser?,
    allAdmins: List<AdminUser>,
    allDesignations: List<DesignationAuthority>,
    onOpenAdminPanel: () -> Unit = {},
    onNavigateToOtpManager: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showAddSubordinateDialog by remember { mutableStateOf(false) }
    var assignedSuccessAdmin by remember { mutableStateOf<AdminUser?>(null) }
    var showCategoryCustomizationDialog by remember { mutableStateOf(false) }
    var showDefineDesignationDialog by remember { mutableStateOf(false) }
    var showMasterLoginDialog by remember { mutableStateOf(false) }
    var showMasterPermanentPinDialog by remember { mutableStateOf(false) }
    var showProfileSwitchPasswordDialog by remember { mutableStateOf(false) }
    var showBulkPermissionsDialog by remember { mutableStateOf(false) }
    var functionToDelegate by remember { mutableStateOf<AdminFunction?>(null) }

    var adminToChangePin by remember { mutableStateOf<AdminUser?>(null) }
    var adminToEditRights by remember { mutableStateOf<AdminUser?>(null) }
    var adminToRename by remember { mutableStateOf<AdminUser?>(null) }
    var adminToDelete by remember { mutableStateOf<AdminUser?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var masterP1Input by remember { mutableStateOf("") }
    var masterP2Input by remember { mutableStateOf("") }
    var profileSwitchPasswordInput by remember { mutableStateOf("") }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") } // "All", "Active", "Blocked", "DeviceLocked"
    var showBlogPasswordDialog by remember { mutableStateOf(false) }
    var blogPasswordInput by remember { mutableStateOf("") }
    var showDailyGreetingDialog by remember { mutableStateOf(false) }
    var dailyGreetingInput by remember { mutableStateOf("") }
    val dailyGreetingText by viewModel.dailyGreetingText.collectAsStateWithLifecycle()
    val dailyGreetingConfig by viewModel.dailyGreetingConfig.collectAsStateWithLifecycle()
    val quickAccessConfig by viewModel.quickAccessConfig.collectAsStateWithLifecycle()
    val prayerRequestsConfig by viewModel.prayerRequestsConfig.collectAsStateWithLifecycle()
    var showQuickAccessDialog by remember { mutableStateOf(false) }
    var showPrayerConfigDialog by remember { mutableStateOf(false) }
    var showYouTubePlaylistManagerDialog by remember { mutableStateOf(false) }
    var showVideoQuickAccessDialog by remember { mutableStateOf(false) }
    var showHomeLayoutDialog by remember { mutableStateOf(false) }

    val isVinayKumar = currentAdmin?.rank == AdminHierarchy.RANK_VINAY_KUMAR ||
            (currentAdmin?.designation?.contains("Vinay", ignoreCase = true) == true)

    val creatorRank = currentAdmin?.rank ?: 1

    // Subordinate admins: ONLY those below current admin rank (Rule: No one can view or manage equals or superiors)
    val subordinateAdmins = remember(allAdmins, currentAdmin) {
        if (currentAdmin == null) emptyList()
        else allAdmins.filter { AdminHierarchy.canManageAdmin(currentAdmin.rank, it.rank) }
            .sortedByDescending { it.rank }
    }

    val filteredSubordinates = remember(subordinateAdmins, searchQuery, statusFilter) {
        var list = subordinateAdmins
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.designation.contains(searchQuery, ignoreCase = true) ||
                it.linkedGmail.contains(searchQuery, ignoreCase = true)
            }
        }
        when (statusFilter) {
            "Active" -> list.filter { it.isEnabled && !it.isDeviceBlocked }
            "Blocked" -> list.filter { !it.isEnabled }
            "DeviceLocked" -> list.filter { it.isDeviceBlocked }
            else -> list
        }
    }

    val allowedCategories = remember(creatorRank) {
        AdminHierarchy.getAllowedSubordinateCategories(creatorRank)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
    ) {
        // 1. Current Authority Level Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_role_manager_hero_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isVinayKumar) GoldWarm.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isVinayKumar) GoldWarm else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isVinayKumar) GoldWarm else MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isVinayKumar) Icons.Default.WorkspacePremium else Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = if (isVinayKumar) Color(0xFF1E1B4B) else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentAdmin?.name ?: "एडमिन",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = if (isVinayKumar) GoldWarm else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = if (isVinayKumar) GoldWarm else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "${currentAdmin?.designation ?: "अधिकृत एडमिन"} • लेवल $creatorRank",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (creatorRank <= AdminHierarchy.RANK_PURANIYA) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ℹ️ पुरनिया (Puraniya) के पास कोई अधीनस्थ श्रेणी नहीं है। आप केवल आपको सौंपे गए कार्यों का संचालन कर सकते हैं।",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "आपके अधिकार क्षेत्र में आने वाली अधीनस्थ श्रेणियां: " +
                                    allowedCategories.joinToString(", ") { it.first },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddSubordinateDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_add_subordinate_admin"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("+ नया एडमिन अलॉट करें", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            if (onNavigateToOtpManager != null) {
                                Button(
                                    onClick = onNavigateToOtpManager,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.testTag("btn_navigate_otp_manager")
                                ) {
                                    Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("10-मिनट OTP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (isVinayKumar || currentAdmin?.isMasterAdmin() == true) {
                                OutlinedButton(
                                    onClick = { showCategoryCustomizationDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_category_customization")
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("श्रेणी कस्टमाइजेशन", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        masterP1Input = currentAdmin?.pin ?: ""
                                        masterP2Input = currentAdmin?.secondaryPin ?: ""
                                        showMasterPermanentPinDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldWarm),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldWarm),
                                    modifier = Modifier.testTag("btn_master_permanent_pin")
                                ) {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("🔐 स्थायी P1/P2", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        profileSwitchPasswordInput = ""
                                        showProfileSwitchPasswordDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_profile_switch_pwd")
                                ) {
                                    Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("स्विच पासवर्ड", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1.5 TreeView-Style Delegation Hierarchy Overview Component
        item {
            var showTreeView by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_delegation_tree_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTreeView = !showTreeView },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = "🌳 पदानुक्रम एवं अधिकार ट्री (Delegation TreeView)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "समस्त एडमिन पदानुक्रम और उनके विशेषाधिकार एक नजर में देखें",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { showTreeView = !showTreeView }) {
                            Icon(
                                imageVector = if (showTreeView) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Tree"
                            )
                        }
                    }

                    if (showTreeView) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))

                        // Group all admins by Rank / Hierarchy Level
                        val rootAdmins = allAdmins.filter { it.rank >= AdminHierarchy.RANK_VINAY_KUMAR || it.isDefaultMaster }
                        val bishopAdmins = allAdmins.filter { it.rank == AdminHierarchy.RANK_BISHOP || it.rank == AdminHierarchy.RANK_DEPUTY_BISHOP }
                        val pastorElderAdmins = allAdmins.filter { it.rank == AdminHierarchy.RANK_PASTOR || it.rank == AdminHierarchy.RANK_PURANIYA }

                        // Recursive or structured TreeView representation
                        TreeViewNodeView(
                            title = "👑 सर्वोच्च मास्टर एडमिन (Vinay Kumar / Root)",
                            admins = rootAdmins,
                            levelColor = GoldWarm,
                            isRoot = true
                        )

                        Spacer(Modifier.height(8.dp))
                        TreeViewNodeView(
                            title = "🏛️ बिशप एवं उप बिशप (Level 3-4)",
                            admins = bishopAdmins,
                            levelColor = MaterialTheme.colorScheme.primary,
                            isRoot = false
                        )

                        Spacer(Modifier.height(8.dp))
                        TreeViewNodeView(
                            title = "👥 पास्टर एवं पुरनिया (Level 1-2)",
                            admins = pastorElderAdmins,
                            levelColor = MaterialTheme.colorScheme.tertiary,
                            isRoot = false
                        )
                    }
                }
            }
        }

        // 1.8 Master Admin Blog & Security Policy Control (Exclusive to Master Admin)
        if (isVinayKumar) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("master_admin_policy_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldWarm.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = GoldWarm)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "👑 मास्टर एडमिन ब्लॉग व सुरक्षा नीति नियंत्रण",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GoldWarm
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        // Personal Blog Global ON/OFF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "🚨 सर्वर-स्तरीय वैश्विक किल स्विच (Global Kill Switch)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                Text(text = "सभी डिवाइसेस पर तत्काल प्रभाव से पर्सनल ब्लॉग बंद/चालू करें (Firebase Realtime Sync)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val isServerVlogOn by viewModel.isPersonalVlogServerEnabled.collectAsState()
                            val isVlogActive = settings.personalVlogMode != PersonalVlogMode.HIDDEN && isServerVlogOn
                            Switch(
                                checked = isVlogActive,
                                onCheckedChange = { enabled ->
                                    val newMode = if (enabled) PersonalVlogMode.SECONDARY else PersonalVlogMode.HIDDEN
                                    viewModel.updatePersonalVlogMode(newMode)
                                    viewModel.setGlobalPersonalVlogServerEnabled(enabled)
                                    Toast.makeText(context, "ग्लोबल सर्वर स्विच: पर्सनल ब्लॉग ${if (enabled) "सक्रिय" else "किल (बंद)"} कर दिया गया।", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = GoldWarm.copy(alpha = 0.2f))
                        Spacer(Modifier.height(10.dp))
                        // Blog Access Password
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "ब्लॉग एक्सेस पासवर्ड (${settings.personalBlogPassword})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(text = "पर्सनल ब्लॉग खोलने के लिए आवश्यक पासवर्ड बदलें", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(
                                onClick = {
                                    blogPasswordInput = settings.personalBlogPassword
                                    showBlogPasswordDialog = true
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("पासवर्ड बदलें", fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = GoldWarm.copy(alpha = 0.2f))
                        Spacer(Modifier.height(10.dp))
                        // Global Daily Greeting Message
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "वैश्विक दैनिक अभिवादन (\"${dailyGreetingText.ifBlank { "जय मसीह की" }}\")", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(text = "होम स्क्रीन पर दिखने वाला मुख्य अभिवादन बदलें (Realtime Sync)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(
                                onClick = {
                                    dailyGreetingInput = dailyGreetingText.ifBlank { "जय मसीह की" }
                                    showDailyGreetingDialog = true
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("अभिवादन बदलें", fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = GoldWarm.copy(alpha = 0.2f))
                        Spacer(Modifier.height(10.dp))
                        // Quick Access Bar Customization
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val currentModeName = when (quickAccessConfig.prayerCountMode.uppercase()) {
                                    "TODAY" -> "आज का निवेदन"
                                    "TESTIMONY" -> "गवाही काउंट"
                                    "ACTIVE" -> "सक्रिय निवेदन"
                                    else -> "कुल निवेदन"
                                }
                                Text(text = "⚡ क्विक एक्सेस बार अनुकूलन ($currentModeName)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(text = "निवेदन काउंट मोड (${currentModeName}), चिप्स दिखाएं/छिपाएं", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(
                                onClick = { showQuickAccessDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("कस्टम सेटिंग्स", fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = GoldWarm.copy(alpha = 0.2f))
                        Spacer(Modifier.height(10.dp))
                        // Prayer Wall & Daily Pray Tap Limit Customization
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🙏 प्रार्थना दीवार एवं 'प्रार्थना किया' दैनिक सीमा (${prayerRequestsConfig.maxDailyPrayTapsPerUser} बार/दिन)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "प्रति व्यक्ति प्रतिदिन टैप सीमा तय करें, जो इससे अधिक टैप करने से रोकेगा",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { showPrayerConfigDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("सीमा तय करें", fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = GoldWarm.copy(alpha = 0.2f))
                        Spacer(Modifier.height(10.dp))
                        // YouTube Video Playlist Link Management
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "📺 YouTube वीडियो प्लेलिस्ट प्रबन्धक", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(text = "ऐप में दिखाने हेतु YouTube प्लेलिस्ट के लिंक जोड़ें / प्रबंधित करें", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(
                                onClick = { showYouTubePlaylistManagerDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("प्लेलिस्ट लिंक जोड़ें", fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = GoldWarm.copy(alpha = 0.2f))
                        Spacer(Modifier.height(10.dp))
                        // Video Quick Access Bar Management
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "🎬 वीडियो क्विक एक्सेस बार प्रबंधन", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(text = "वीडियो स्क्रीन के बार को दिखाएं/छिपाएं, नए बार जोड़ें, क्रम बदलें या पिन करें", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(
                                onClick = { showVideoQuickAccessDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("बार सेटिंग्स", fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = GoldWarm.copy(alpha = 0.2f))
                        Spacer(Modifier.height(10.dp))
                        // Home Page Layout & Sections Management
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "🏠 होम पेज लेआउट व सेक्शन्स प्रबंधन", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(text = "क्रम बदलें, जोड़ें/हटाएं, दिखाएं/छिपाएं, समय सीमा (टाइमर) जैसे केवल कुछ दिनों के लिए या स्थायी", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(
                                onClick = { showHomeLayoutDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("लेआउट सेटिंग्स", fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = GoldWarm.copy(alpha = 0.2f))
                        Spacer(Modifier.height(10.dp))
                        // Inactive Admin Auto-Disable Policy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "निष्क्रिय एडमिन स्वतः ब्लॉक नीति", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(text = "यदि एडमिन ${settings.inactiveAdminAutoDisableDays} दिन अक्रिय रहे तो स्वतः ब्लॉक", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            var expandedDays by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expandedDays = true }, shape = RoundedCornerShape(8.dp)) {
                                    Text("${settings.inactiveAdminAutoDisableDays} दिन", fontSize = 11.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(expanded = expandedDays, onDismissRequest = { expandedDays = false }) {
                                    listOf(30, 60, 90, 180, 365).forEach { days ->
                                        DropdownMenuItem(
                                            text = { Text("$days दिन") },
                                            onClick = {
                                                viewModel.updateInactiveAdminAutoDisableDays(days)
                                                expandedDays = false
                                                Toast.makeText(context, "निष्क्रिय नीति बदलकर $days दिन की गई।", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = GoldWarm.copy(alpha = 0.2f))
                        Spacer(Modifier.height(10.dp))
                        // Global Admin Emergency Kill-Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🚨 मास्टर 'किल-स्विच' (Global Emergency Lock)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (settings.isGlobalAdminEmergencyLock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "एक क्लिक में बिशप से पुरनिया तक सभी निचले एडमिन लॉग-आउट होंगे व पैनल बंद होगा",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.isGlobalAdminEmergencyLock,
                                onCheckedChange = { locked ->
                                    viewModel.updateGlobalAdminEmergencyLock(locked)
                                    val msg = if (locked)
                                        "🚨 आपातकालीन लॉक सक्रिय! बिशप से पुरनिया तक सभी निचले एडमिन तुरंत लॉग-आउट हो गए हैं।"
                                    else
                                        "✅ आपातकालीन लॉक निष्क्रिय। सामान्य एडमिन एक्सेस पुनः बहाल कर दिया गया है।"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.error,
                                    checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Subordinate Admins Header & Search Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "आपके अधीनस्थ एडमिन (${filteredSubordinates.size} / ${subordinateAdmins.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "हाइयर ऑथोरिटी नियंत्रण",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Search bar for subordinate administrators across delegation tree
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_search_bar"),
                    placeholder = { Text("नाम, पदनाम या श्रेणी से खोजें... (Search)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Status Filter Chips Row (All, Active, Blocked, Device Locked)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All" to "सभी", "Active" to "🟢 सक्रिय", "Blocked" to "🔴 निष्क्रिय", "DeviceLocked" to "🚫 डिवाइस लॉक")
                    filters.forEach { (key, label) ->
                        val isSelected = statusFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { statusFilter = key },
                            label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // 2.5 Extra Access Delegation Hub
        item {
            ExtraAccessDelegationHubCard(
                currentAdmin = currentAdmin ?: AdminUser(),
                onDelegateFunction = { fn ->
                    functionToDelegate = fn
                }
            )
        }

        // 3. Subordinates List or Empty State
        if (filteredSubordinates.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SupervisorAccount,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank())
                                "खोज परिणाम में कोई एडमिन नहीं मिला।"
                            else if (creatorRank <= AdminHierarchy.RANK_PURANIYA)
                                "पुरनिया के अधीन कोई एडमिन नहीं है।"
                            else
                                "अभी तक कोई अधीनस्थ एडमिन नहीं जोड़ा गया है।",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredSubordinates, key = { it.id }) { subordinate ->
                SubordinateAdminCard(
                    admin = subordinate,
                    onRegenerateOtp = {
                        viewModel.regenerateSecondaryPin(subordinate.id) { success, newOtp, err ->
                            if (success && newOtp != null) {
                                clipboardManager.setText(AnnotatedString(newOtp))
                                Toast.makeText(
                                    context,
                                    "नया OTP ($newOtp) जनरेट हुआ और कॉपी कर लिया गया! 10 मिनट के लिए वैध है। ⏳",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, err ?: "OTP जनरेट करने में त्रुटि", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onRename = {
                        adminToRename = subordinate
                        renameInput = subordinate.name
                    },
                    onChangePin = { adminToChangePin = subordinate },
                    onEditRights = { adminToEditRights = subordinate },
                    onToggleBlockDevice = { isBlocked ->
                        viewModel.toggleBlockDevice(subordinate.id, isBlocked) { success, err ->
                            if (success) {
                                val msg = if (isBlocked) "${subordinate.name} का डिवाइस ब्लॉक कर दिया गया! 🚫" else "${subordinate.name} का डिवाइस अनब्लॉक हो गया! ✅"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onToggleDisable = { isEnabled ->
                        viewModel.toggleAdminStatus(subordinate.id, isEnabled) { success, err ->
                            if (success) {
                                val statusText = if (isEnabled) "सक्रिय (Active)" else "गुप्त रूप से निष्क्रिय (Disabled)"
                                Toast.makeText(context, "${subordinate.name} का खाता $statusText कर दिया गया।", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDelete = { adminToDelete = subordinate }
                )
            }
        }
    }

    // --- DIALOGS ---

    // Blog Password Change Dialog
    if (showBlogPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showBlogPasswordDialog = false },
            title = { Text("पर्सनल ब्लॉग पासवर्ड बदलें") },
            text = {
                Column {
                    Text("मास्टर एडमिन द्वारा पर्सनल ब्लॉग एक्सेस हेतु नया पासवर्ड दर्ज करें:", fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = blogPasswordInput,
                        onValueChange = { blogPasswordInput = it },
                        singleLine = true,
                        label = { Text("नया पासवर्ड") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (blogPasswordInput.isNotBlank()) {
                            viewModel.updatePersonalBlogPassword(blogPasswordInput)
                            Toast.makeText(context, "पर्सनल ब्लॉग पासवर्ड सफलतापूर्वक अपडेट हुआ!", Toast.LENGTH_SHORT).show()
                            showBlogPasswordDialog = false
                        } else {
                            Toast.makeText(context, "पासवर्ड खाली नहीं हो सकता", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlogPasswordDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Daily Greeting Change Dialog
    if (showDailyGreetingDialog) {
        var isPermanentGreeting by remember { mutableStateOf(dailyGreetingConfig.isPermanent) }
        var greetingDurationType by remember { mutableStateOf(if (dailyGreetingConfig.durationDays > 0) "days" else if (dailyGreetingConfig.durationHours > 0) "hours" else "permanent") }
        var greetingDurationValue by remember {
            mutableStateOf(
                if (dailyGreetingConfig.durationDays > 0) dailyGreetingConfig.durationDays.toString()
                else if (dailyGreetingConfig.durationHours > 0) dailyGreetingConfig.durationHours.toString()
                else "24"
            )
        }

        AlertDialog(
            onDismissRequest = { showDailyGreetingDialog = false },
            title = { Text("वैश्विक दैनिक अभिवादन बदलें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("मास्टर एडमिन द्वारा सभी उपयोगकर्ताओं की होम स्क्रीन के लिए नया अभिवादन वाक्य दर्ज करें:", fontSize = 12.sp)
                    Text("नोट: उपयोगकर्ता का नाम स्वचालित रूप से जुड़ जाएगा। यदि विशेष स्थान पर नाम रखना चाहें तो {name} लिखें (उदा. '{name} जी, जय मसीह की')।", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = dailyGreetingInput,
                        onValueChange = { dailyGreetingInput = it },
                        singleLine = true,
                        label = { Text("अभिवादन वाक्य") },
                        placeholder = { Text("जय मसीह की") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("समय सीमा (Duration / Expiry):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = greetingDurationType == "permanent",
                            onClick = { greetingDurationType = "permanent" },
                            label = { Text("स्थायी (Permanent)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = greetingDurationType == "hours",
                            onClick = { greetingDurationType = "hours" },
                            label = { Text("घंटे (Hours)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = greetingDurationType == "days",
                            onClick = { greetingDurationType = "days" },
                            label = { Text("दिन (Days)", fontSize = 11.sp) }
                        )
                    }

                    if (greetingDurationType != "permanent") {
                        OutlinedTextField(
                            value = greetingDurationValue,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) greetingDurationValue = it },
                            label = { Text(if (greetingDurationType == "hours") "घंटे दर्ज करें (उदा. 12, 24)" else "दिन दर्ज करें (उदा. 1, 7)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (!dailyGreetingConfig.isPermanent && dailyGreetingConfig.expiresAtTimestamp > 0L) {
                        Text(
                            text = "वर्तमान स्थिति: ${dailyGreetingConfig.getRemainingTimeFormatted()}",
                            fontSize = 11.sp,
                            color = if (dailyGreetingConfig.isExpired()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dailyGreetingInput.isNotBlank()) {
                            val isPerm = greetingDurationType == "permanent"
                            val durVal = greetingDurationValue.toIntOrNull() ?: 1
                            val hours = if (greetingDurationType == "hours") durVal else 0
                            val days = if (greetingDurationType == "days") durVal else 0
                            val totalMillis = if (isPerm) 0L else ((days * 24L * 60 * 60 * 1000) + (hours * 60L * 60 * 1000))
                            val expiry = if (isPerm) 0L else System.currentTimeMillis() + totalMillis

                            val newConfig = com.example.data.model.DailyGreetingConfig(
                                greetingText = dailyGreetingInput.trim(),
                                isPermanent = isPerm,
                                durationHours = hours,
                                durationDays = days,
                                expiresAtTimestamp = expiry,
                                updatedTimestamp = System.currentTimeMillis()
                            )
                            viewModel.updateDailyGreetingConfig(newConfig) { ok ->
                                if (ok) {
                                    Toast.makeText(context, "वैश्विक अभिवादन सफलतापूर्वक अपडेट हुआ!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showDailyGreetingDialog = false
                        } else {
                            Toast.makeText(context, "अभिवादन वाक्य खाली नहीं हो सकता", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyGreetingDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Quick Access Bar Customization Dialog
    if (showQuickAccessDialog) {
        var selectedMode by remember { mutableStateOf(quickAccessConfig.prayerCountMode) }
        var customLabel by remember { mutableStateOf(quickAccessConfig.prayerChipLabel) }
        var showPrayer by remember { mutableStateOf(quickAccessConfig.showPrayerChip) }
        var showEvent by remember { mutableStateOf(quickAccessConfig.showEventChip) }
        var showSongbook by remember { mutableStateOf(quickAccessConfig.showSongbookChip) }
        var showDailyPrayer by remember { mutableStateOf(quickAccessConfig.showDailyPrayerChip) }
        var showSaved by remember { mutableStateOf(quickAccessConfig.showSavedChip) }

        AlertDialog(
            onDismissRequest = { showQuickAccessDialog = false },
            title = { Text("⚡ क्विक एक्सेस बार अनुकूलन", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("1. निवेदन चिप में कौन सा काउंट प्रदर्शित करना है?", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                    val modes = listOf(
                        "TOTAL" to "📊 कुल निवेदन (Total Requests)",
                        "TODAY" to "🌅 आज का निवेदन (Today's Requests)",
                        "TESTIMONY" to "🕊️ गवाही काउंट (Testimonies)",
                        "ACTIVE" to "⚡ सक्रिय निवेदन (Active Requests)"
                    )

                    modes.forEach { (modeKey, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = modeKey }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedMode.uppercase() == modeKey),
                                onClick = { selectedMode = modeKey }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("2. निवेदन चिप का कस्टम नाम / लेबल:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        singleLine = true,
                        label = { Text("चिप लेबल") },
                        placeholder = { Text("🙏 निवेदन") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("3. होम स्क्रीन पर कौन से चिप्स दिखाएं?", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🙏 निवेदन (Prayer Requests)", fontSize = 12.sp)
                        Switch(checked = showPrayer, onCheckedChange = { showPrayer = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📅 इवेंट (Event Calendar)", fontSize = 12.sp)
                        Switch(checked = showEvent, onCheckedChange = { showEvent = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎵 गीत पुस्तक (Songbook)", fontSize = 12.sp)
                        Switch(checked = showSongbook, onCheckedChange = { showSongbook = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🙏 दैनिक प्रार्थना (Daily Prayer)", fontSize = 12.sp)
                        Switch(checked = showDailyPrayer, onCheckedChange = { showDailyPrayer = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔖 सहेजे गए (Saved Collection)", fontSize = 12.sp)
                        Switch(checked = showSaved, onCheckedChange = { showSaved = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedConfig = quickAccessConfig.copy(
                            prayerCountMode = selectedMode,
                            prayerChipLabel = customLabel.ifBlank { "🙏 निवेदन" },
                            showPrayerChip = showPrayer,
                            showEventChip = showEvent,
                            showSongbookChip = showSongbook,
                            showDailyPrayerChip = showDailyPrayer,
                            showSavedChip = showSaved
                        )
                        viewModel.updateQuickAccessConfig(updatedConfig)
                        Toast.makeText(context, "क्विक एक्सेस बार सेटिंग्स अपडेट हो गईं!", Toast.LENGTH_SHORT).show()
                        showQuickAccessDialog = false
                    }
                ) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAccessDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Prayer Wall & Daily Pray Tap Limit Settings Dialog
    if (showPrayerConfigDialog) {
        var dailyLimitInput by remember { mutableStateOf(prayerRequestsConfig.maxDailyPrayTapsPerUser.toString()) }
        var wallEnabled by remember { mutableStateOf(prayerRequestsConfig.enabled) }

        AlertDialog(
            onDismissRequest = { showPrayerConfigDialog = false },
            title = {
                Text("🙏 'प्रार्थना किया' दैनिक सीमा अनुकूलन", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "मास्टर एडमिन नियंत्रण: तय करें कि एक व्यक्ति एक दिन में किसी प्रार्थना निवेदन पर कितनी बार 'प्रार्थना किया' बटन टैप कर सकता है:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = dailyLimitInput,
                        onValueChange = { dailyLimitInput = it.filter { char -> char.isDigit() } },
                        label = { Text("प्रति व्यक्ति प्रतिदिन टैप सीमा (Max Daily Taps) *") },
                        placeholder = { Text("उदा. 1, 3, 5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("प्रार्थना दीवार चालू/बंद रखें", fontSize = 13.sp)
                        Switch(
                            checked = wallEnabled,
                            onCheckedChange = { wallEnabled = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = dailyLimitInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val updatedConfig = prayerRequestsConfig.copy(
                            enabled = wallEnabled,
                            maxDailyPrayTapsPerUser = limit
                        )
                        viewModel.updatePrayerRequestsConfig(updatedConfig) { success ->
                            if (success) {
                                Toast.makeText(context, "प्रार्थना सेटिंग्स सहेजी गईं! (दैनिक सीमा: $limit बार/दिन)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "सेटिंग्स सहेजने में विफल", Toast.LENGTH_SHORT).show()
                            }
                            showPrayerConfigDialog = false
                        }
                    }
                ) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrayerConfigDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // YouTube Video & Playlist Link Manager Dialog
    if (showYouTubePlaylistManagerDialog) {
        val playlists by viewModel.youtubePlaylists.collectAsStateWithLifecycle(initialValue = emptyList())
        val allVideos by viewModel.youtubeVideos.collectAsStateWithLifecycle(initialValue = emptyList())
        val customRemoteVideos = remember(allVideos) { allVideos.filter { it.isRemote } }

        var selectedTab by remember { mutableIntStateOf(0) } // 0 = Playlists, 1 = Single Videos
        var showAddForm by remember { mutableStateOf(false) }

        // Playlist form fields
        var playlistTitleInput by remember { mutableStateOf("") }
        var playlistUrlInput by remember { mutableStateOf("") }
        var channelTitleInput by remember { mutableStateOf("Vinay Kumar AVJ") }

        // Single video form fields
        var videoTitleInput by remember { mutableStateOf("") }
        var videoUrlInput by remember { mutableStateOf("") }
        var videoDescInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showYouTubePlaylistManagerDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📺 YouTube वीडियो एवं प्लेलिस्ट", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showYouTubePlaylistManagerDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0; showAddForm = false },
                            text = { Text("प्लेलिस्ट (${playlists.size})", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1; showAddForm = false },
                            text = { Text("सिंगल वीडियो (${customRemoteVideos.size})", fontSize = 12.sp) }
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    if (selectedTab == 0) {
                        // --- PLAYLIST TAB ---
                        if (!showAddForm) {
                            Button(
                                onClick = { showAddForm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("नई प्लेलिस्ट का लिंक जोड़ें")
                            }

                            Spacer(Modifier.height(4.dp))
                            if (playlists.isEmpty()) {
                                Text("कोई कस्टम प्लेलिस्ट नहीं मिली।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                playlists.forEach { playlist ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(playlist.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(playlist.channelTitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(playlist.playlistUrl, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteYouTubePlaylist(playlist.id) { success ->
                                                        if (success) {
                                                            Toast.makeText(context, "प्लेलिस्ट हटा दी गई!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("नई वीडियो प्लेलिस्ट का लिंक जोड़ें:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            OutlinedTextField(
                                value = playlistTitleInput,
                                onValueChange = { playlistTitleInput = it },
                                singleLine = true,
                                label = { Text("प्लेलिस्ट का शीर्षक / Title") },
                                placeholder = { Text("उदा. वॉरशिप गीत सीरीज़") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = playlistUrlInput,
                                onValueChange = { playlistUrlInput = it },
                                singleLine = true,
                                label = { Text("YouTube Playlist URL / Link") },
                                placeholder = { Text("https://youtube.com/playlist?list=...") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = channelTitleInput,
                                onValueChange = { channelTitleInput = it },
                                singleLine = true,
                                label = { Text("चैनल का नाम / Channel Name") },
                                placeholder = { Text("Vinay Kumar AVJ") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAddForm = false }) {
                                    Text("पीछे जाएं")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (playlistTitleInput.isNotBlank() && playlistUrlInput.isNotBlank()) {
                                            val newPlaylist = com.example.data.model.YouTubePlaylist(
                                                id = "",
                                                title = playlistTitleInput.trim(),
                                                channelTitle = channelTitleInput.trim().ifBlank { "Vinay Kumar AVJ" },
                                                playlistUrl = playlistUrlInput.trim()
                                            )
                                            viewModel.addOrUpdateYouTubePlaylist(newPlaylist) { success ->
                                                if (success) {
                                                    Toast.makeText(context, "वीडियो प्लेलिस्ट सफलतापूर्वक जुड़ गई!", Toast.LENGTH_SHORT).show()
                                                    playlistTitleInput = ""
                                                    playlistUrlInput = ""
                                                    showAddForm = false
                                                } else {
                                                    Toast.makeText(context, "प्लेलिस्ट जोड़ने में त्रुटि हुई", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "कृपया शीर्षक और प्लेलिस्ट लिंक दोनों भरें", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("लिंक सेव करें")
                                }
                            }
                        }
                    } else {
                        // --- SINGLE VIDEO TAB ---
                        if (!showAddForm) {
                            Button(
                                onClick = { showAddForm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("सिंगल वीडियो का लिंक जोड़ें")
                            }

                            Spacer(Modifier.height(4.dp))
                            if (customRemoteVideos.isEmpty()) {
                                Text("कोई कस्टम वीडियो नहीं मिला।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                customRemoteVideos.forEach { vid ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(vid.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(vid.channelTitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(vid.videoUrl, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.togglePinVideo(vid.id) { success, isPinned ->
                                                            if (success) {
                                                                Toast.makeText(
                                                                    context,
                                                                    if (isPinned) "वीडियो को सबसे ऊपर PIN कर दिया गया! 📌" else "वीडियो से PIN हटा दिया गया!",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PushPin,
                                                        contentDescription = if (vid.isPinned) "Unpin" else "Pin",
                                                        tint = if (vid.isPinned) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteCustomVideo(vid.id) { success ->
                                                            if (success) {
                                                                Toast.makeText(context, "वीडियो हटा दिया गया!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("सिंगल YouTube वीडियो का लिंक जोड़ें:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            OutlinedTextField(
                                value = videoTitleInput,
                                onValueChange = { videoTitleInput = it },
                                singleLine = true,
                                label = { Text("वीडियो का शीर्षक / Title") },
                                placeholder = { Text("उदा. आज का विशेष सन्देश") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = videoUrlInput,
                                onValueChange = { videoUrlInput = it },
                                singleLine = true,
                                label = { Text("YouTube / Video URL") },
                                placeholder = { Text("https://www.youtube.com/watch?v=...") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = channelTitleInput,
                                onValueChange = { channelTitleInput = it },
                                singleLine = true,
                                label = { Text("चैनल का नाम / Channel Name") },
                                placeholder = { Text("Vinay Kumar AVJ") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = videoDescInput,
                                onValueChange = { videoDescInput = it },
                                singleLine = false,
                                maxLines = 3,
                                label = { Text("विवरण / Description (ऐच्छिक)") },
                                placeholder = { Text("वीडियो के बारे में संक्षेप में लिखें...") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAddForm = false }) {
                                    Text("पीछे जाएं")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (videoTitleInput.isNotBlank() && videoUrlInput.isNotBlank()) {
                                            val parsed = com.example.util.VideoUrlParser.parse(videoUrlInput.trim())
                                            val vidId = if (parsed.videoId.isNotBlank()) parsed.videoId else "vid_" + System.currentTimeMillis()
                                            val thumb = if (parsed.videoId.isNotBlank()) "https://i.ytimg.com/vi/${parsed.videoId}/hqdefault.jpg" else ""

                                            val newVideo = com.example.data.model.YouTubeVideo(
                                                id = vidId,
                                                title = videoTitleInput.trim(),
                                                channelId = "custom",
                                                channelTitle = channelTitleInput.trim().ifBlank { "Vinay Kumar AVJ" },
                                                thumbnailUrl = thumb,
                                                publishedAt = "",
                                                publishedTimestamp = System.currentTimeMillis(),
                                                description = videoDescInput.trim(),
                                                videoUrl = videoUrlInput.trim(),
                                                isRemote = true
                                            )
                                            viewModel.addOrUpdateCustomVideo(newVideo) { success ->
                                                if (success) {
                                                    Toast.makeText(context, "वीडियो लिंक सफलतापूर्वक जुड़ गया!", Toast.LENGTH_SHORT).show()
                                                    videoTitleInput = ""
                                                    videoUrlInput = ""
                                                    videoDescInput = ""
                                                    showAddForm = false
                                                } else {
                                                    Toast.makeText(context, "वीडियो जोड़ने में त्रुटि हुई", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "कृपया शीर्षक और वीडियो लिंक दोनों भरें", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("वीडियो सेव करें")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showVideoQuickAccessDialog) {
        VideoQuickAccessManagerDialog(
            viewModel = viewModel,
            onDismiss = { showVideoQuickAccessDialog = false }
        )
    }

    if (showHomeLayoutDialog) {
        HomeLayoutManagerDialog(
            viewModel = viewModel,
            onDismiss = { showHomeLayoutDialog = false }
        )
    }

    // 1. Add Subordinate Admin Dialog
    if (showAddSubordinateDialog && currentAdmin != null) {
        AddSubordinateAdminDialog(
            creatorAdmin = currentAdmin,
            allowedCategories = allowedCategories,
            allAdmins = allAdmins,
            onDismiss = { showAddSubordinateDialog = false },
            onConfirm = { designation, name, phone, roleTier, reportsToSeniorId, reportsToSeniorName, pin, secondaryPin, assignedFunctions, applyToCategory ->
                viewModel.assignOrUpdateHierarchicalRole(
                    targetUserId = "usr_${System.currentTimeMillis() % 100000}",
                    name = name,
                    phone = phone,
                    roleTier = roleTier,
                    reportsToSeniorId = reportsToSeniorId,
                    reportsToSeniorName = reportsToSeniorName,
                    customOverrides = assignedFunctions,
                    p1PasswordInput = pin,
                    p2OtpInput = secondaryPin,
                    expectedP2Otp = secondaryPin,
                    isVerifiedBeliever = (roleTier == "believer"),
                    existingAdminId = null
                ) { success, createdAdmin, err ->
                    if (success && createdAdmin != null) {
                        clipboardManager.setText(AnnotatedString(secondaryPin))
                        assignedSuccessAdmin = createdAdmin
                        if (applyToCategory) {
                            val rank = AdminHierarchy.getRankForDesignation(designation)
                            viewModel.applyBulkPermissionsToCategory(rank, designation, assignedFunctions) { bulkOk, count, _ ->
                                if (bulkOk) {
                                    Toast.makeText(context, "$designation श्रेणी के $count एडमिनों पर समान अधिकार लागू किए गए।", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        showAddSubordinateDialog = false
                    } else {
                        Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Success Confirmation Animation Dialog (P1 & P2 Completed)
    if (assignedSuccessAdmin != null) {
        AdminAssignmentSuccessDialog(
            admin = assignedSuccessAdmin!!,
            onDismiss = { assignedSuccessAdmin = null }
        )
    }

    // Rename Admin Dialog
    if (adminToRename != null) {
        val target = adminToRename!!
        AlertDialog(
            onDismissRequest = { adminToRename = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("एडमिन का नाम बदलें (Rename)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text("पदनाम: ${target.designation}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("नया प्रोफ़ाइल नाम (Profile Name) *") },
                        placeholder = { Text("उदा. पास्टर डेविड") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.trim().length >= 2) {
                            viewModel.renameAdmin(target.id, renameInput.trim()) { ok, err ->
                                if (ok) {
                                    Toast.makeText(context, "नाम सफलतापूर्वक अपडेट हुआ!", Toast.LENGTH_SHORT).show()
                                    adminToRename = null
                                } else {
                                    Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "कृपया मान्य नाम दर्ज करें", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { adminToRename = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Master Permanent P1 & P2 Dialog
    if (showMasterPermanentPinDialog && currentAdmin != null) {
        var isP1Visible by remember { mutableStateOf(false) }
        var isP2Visible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showMasterPermanentPinDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = GoldWarm)
                    Spacer(Modifier.width(8.dp))
                    Text("मास्टर एडमिन स्थायी P1 व P2", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "मास्टर एडमिन के लिए अपना स्थायी P1 (पासकी) और स्थायी P2 (वैकल्पिक) सेट करें। मास्टर एडमिन के लिए 10-मिनट OTP वैकल्पिक है।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = masterP1Input,
                        onValueChange = { if (it.length <= 8) masterP1Input = it },
                        label = { Text("स्थायी P1 (मास्टर पासकी/पिन) *") },
                        singleLine = true,
                        visualTransformation = if (isP1Visible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isP1Visible = !isP1Visible }) {
                                Icon(if (isP1Visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = masterP2Input,
                        onValueChange = { if (it.length <= 8) masterP2Input = it },
                        label = { Text("स्थायी P2 (वैकल्पिक स्थायी सेकंडरी कोड)") },
                        placeholder = { Text("खाली छोड़ने पर सिर्फ P1 से लॉगिन होगा") },
                        singleLine = true,
                        visualTransformation = if (isP2Visible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isP2Visible = !isP2Visible }) {
                                Icon(if (isP2Visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (masterP1Input.trim().length >= 4) {
                            viewModel.updateAdminPin(currentAdmin.id, masterP1Input.trim()) { ok, err ->
                                if (ok) {
                                    if (masterP2Input.isNotBlank()) {
                                        viewModel.regenerateSecondaryPin(currentAdmin.id, masterP2Input.trim()) { ok2, _, _ ->
                                            Toast.makeText(context, "मास्टर P1 और P2 सफलतापूर्वक अपडेट हुए! 🔐", Toast.LENGTH_SHORT).show()
                                            showMasterPermanentPinDialog = false
                                        }
                                    } else {
                                        Toast.makeText(context, "मास्टर P1 सफलतापूर्वक अपडेट हुआ! 🔐", Toast.LENGTH_SHORT).show()
                                        showMasterPermanentPinDialog = false
                                    }
                                } else {
                                    Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "P1 कम से कम 4 अंकों का होना चाहिए", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMasterPermanentPinDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Profile Switch Password Change Dialog
    if (showProfileSwitchPasswordDialog) {
        var isPwdVisible by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showProfileSwitchPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("प्रोफ़ाइल स्विच पासवर्ड बदलें", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "प्रोफ़ाइल स्विच (Master Admin) करने हेतु मुख्य पासवर्ड निर्धारित करें:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = profileSwitchPasswordInput,
                        onValueChange = { profileSwitchPasswordInput = it },
                        label = { Text("नया प्रोफ़ाइल स्विच पासवर्ड *") },
                        singleLine = true,
                        visualTransformation = if (isPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPwdVisible = !isPwdVisible }) {
                                Icon(if (isPwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (profileSwitchPasswordInput.trim().length >= 4) {
                            viewModel.updateProfileSwitchPassword(profileSwitchPasswordInput.trim()) { ok ->
                                if (ok) {
                                    Toast.makeText(context, "प्रोफ़ाइल स्विच पासवर्ड सफलतापूर्वक अपडेट हुआ! ✅", Toast.LENGTH_SHORT).show()
                                    showProfileSwitchPasswordDialog = false
                                } else {
                                    Toast.makeText(context, "त्रुटि", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "पासवर्ड कम से कम 4 अक्षरों का होना चाहिए", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileSwitchPasswordDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // 2. Master Admin Category Customization & Overwrite Dialog
    if (showCategoryCustomizationDialog && isVinayKumar) {
        MasterAdminCategoryCustomizationDialog(
            onDismiss = { showCategoryCustomizationDialog = false },
            onConfirm = { categoryRank, categoryName, functions ->
                viewModel.overwriteCategoryPermissions(categoryRank, categoryName, functions) { success, count, err ->
                    if (success) {
                        Toast.makeText(
                            context,
                            "'$categoryName' श्रेणी के अधिकार अपडेट किए गए और $count सक्रिय एडमिन पर ओवरराइट किए गए! ✅",
                            Toast.LENGTH_LONG
                        ).show()
                        showCategoryCustomizationDialog = false
                    } else {
                        Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // 3. Change Subordinate Password 1 Dialog
    adminToChangePin?.let { target ->
        ChangeSubordinatePinDialog(
            admin = target,
            onDismiss = { adminToChangePin = null },
            onConfirm = { newPin ->
                viewModel.updateAdminPin(target.id, newPin) { success, err ->
                    if (success) {
                        Toast.makeText(
                            context,
                            "${target.name} का पासवर्ड 1 बदल दिया गया! पुराना लॉगिन सत्र तुरंत समाप्त हो जाएगा। ✅",
                            Toast.LENGTH_LONG
                        ).show()
                        adminToChangePin = null
                    } else {
                        Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // 4. Edit Subordinate Assigned Rights Dialog
    adminToEditRights?.let { target ->
        EditSubordinateRightsDialog(
            targetAdmin = target,
            creatorAdmin = currentAdmin ?: AdminUser(),
            onDismiss = { adminToEditRights = null },
            onConfirm = { newFunctions ->
                viewModel.updateAdminAssignedFunctions(target.id, newFunctions) { success, err ->
                    if (success) {
                        Toast.makeText(context, "${target.name} के अधिकार सफलतापूर्वक अपडेट किए गए! ✅", Toast.LENGTH_SHORT).show()
                        adminToEditRights = null
                    } else {
                        Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // 5. Delete Confirmation Dialog
    adminToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { adminToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("एडमिन प्रोफ़ाइल डिलीट करें?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "क्या आप वास्तव में '${target.name}' (${target.designation}) की एडमिन प्रोफ़ाइल स्थायी रूप से डिलीट करना चाहते हैं? इससे उनका लॉगिन तुरंत बंद हो जाएगा।",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAdmin(target.id) { success, err ->
                            if (success) {
                                Toast.makeText(context, "${target.name} की प्रोफ़ाइल डिलीट कर दी गई।", Toast.LENGTH_SHORT).show()
                                adminToDelete = null
                            } else {
                                Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हां, डिलीट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { adminToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // 6. Delegate Specific Extra Access Dialog
    functionToDelegate?.let { function ->
        DelegateExtraAccessDialog(
            functionToDelegate = function,
            currentAdmin = currentAdmin ?: AdminUser(),
            subordinateAdmins = subordinateAdmins,
            onDismiss = { functionToDelegate = null },
            onConfirmGrant = { targetAdmin, functionId ->
                val updatedFunctions = if (targetAdmin.hasFunction(function)) {
                    targetAdmin.assignedFunctions.filterNot { it == functionId || it == function.name }
                } else {
                    (targetAdmin.assignedFunctions + functionId).distinct()
                }
                viewModel.updateAdminAssignedFunctions(targetAdmin.id, updatedFunctions) { success, err ->
                    if (success) {
                        val actionMsg = if (targetAdmin.hasFunction(function)) "का एक्सेस वापस ले लिया गया" else "का एक्सेस सफलतापूर्वक दे दिया गया! 🤝✅"
                        Toast.makeText(context, "${targetAdmin.name} को '${function.titleHindi}' $actionMsg", Toast.LENGTH_LONG).show()
                        functionToDelegate = null
                    } else {
                        Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

/**
 * Card representing an individual subordinate admin
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubordinateAdminCard(
    admin: AdminUser,
    onRegenerateOtp: () -> Unit,
    onRename: () -> Unit = {},
    onChangePin: () -> Unit,
    onEditRights: () -> Unit,
    onToggleBlockDevice: (Boolean) -> Unit = {},
    onToggleDisable: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isPinVisible by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    // Live remaining timer
    var remainingTimeMs by remember { mutableLongStateOf(admin.getRemainingOtpTimeMs()) }

    LaunchedEffect(admin.secondaryPinGeneratedTimestamp) {
        while (true) {
            remainingTimeMs = admin.getRemainingOtpTimeMs()
            delay(1000L)
        }
    }

    val isOtpValid = admin.isOtpValid()
    val remainingMinutes = (remainingTimeMs / 60000L)
    val remainingSeconds = ((remainingTimeMs % 60000L) / 1000L)

    val rankColor = when (admin.rank) {
        AdminHierarchy.RANK_BISHOP -> Color(0xFF9333EA)
        AdminHierarchy.RANK_DEPUTY_BISHOP -> Color(0xFF3B82F6)
        AdminHierarchy.RANK_PASTOR -> Color(0xFF10B981)
        else -> Color(0xFFEAB308)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subordinate_admin_card_${admin.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (admin.isEnabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            if (!admin.isEnabled) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            else rankColor.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Designation, Name, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = rankColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, rankColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = admin.designation,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = rankColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = admin.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (admin.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                        IconButton(
                            onClick = onRename,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "नाम बदलें",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    if (admin.createdByDesignation.isNotBlank()) {
                        Text(
                            text = "अलॉटकर्ता: ${admin.createdByDesignation}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (admin.isDeviceBlocked) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "🚫 डिवाइस ब्लॉक (Device Blocked)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Enabled / Disabled & Activity status chip
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            admin.isDeviceBlocked -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            admin.isEnabled -> Color(0xFF10B981).copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = when {
                                            admin.isDeviceBlocked -> MaterialTheme.colorScheme.error
                                            admin.isEnabled -> Color(0xFF10B981)
                                            else -> Color.Gray
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = when {
                                    admin.isDeviceBlocked -> "ब्लॉक (Blocked)"
                                    admin.isEnabled -> "ऑनलाइन (Online)"
                                    else -> "ऑफ़लाइन (Offline)"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    admin.isDeviceBlocked -> MaterialTheme.colorScheme.error
                                    admin.isEnabled -> Color(0xFF10B981)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // PASSWORD 1 BOX (Manual & Permanent)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "पासवर्ड 1 (मैनुअल व स्थायी):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isPinVisible) admin.pin else "••••",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldWarm
                            )
                            IconButton(
                                onClick = { isPinVisible = !isPinVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Hide/Show PIN",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        TextButton(
                            onClick = onChangePin,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("पासवर्ड 1 बदलें", fontSize = 11.sp)
                        }
                    }
                    Text(
                        text = "ℹ️ यह पासवर्ड मैनुअल व स्थाई है। इसे बदलने पर उस व्यक्ति का पुराना लॉगिन तुरंत अवैध हो जाएगा।",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // PASSWORD 2 BOX (OTP - 10 Minutes Valid)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isOtpValid) Color(0xFF10B981).copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                border = BorderStroke(
                    1.dp,
                    if (isOtpValid) Color(0xFF10B981).copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LockClock,
                                contentDescription = null,
                                tint = if (isOtpValid) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "पासवर्ड 2 (OTP):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (admin.secondaryPin.isNotBlank()) admin.secondaryPin else "अनुपलब्ध",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOtpValid) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                            if (admin.secondaryPin.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(admin.secondaryPin))
                                        Toast.makeText(context, "OTP कॉपी किया गया: ${admin.secondaryPin}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy OTP", modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Button(
                            onClick = onRegenerateOtp,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOtpValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("पासवर्ड जनरेट करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Visual 10-minute Countdown Timer specifically for Password 2 (OTP)
                    OtpCountdownTimer(
                        timestamp = admin.secondaryPinGeneratedTimestamp,
                        totalDurationMs = 600000L,
                        isCompact = false
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Assigned Rights Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "अधिकार: ${admin.assignedFunctions.size} सौंपे गए",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onEditRights,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("अधिकार बदलें", fontSize = 11.sp)
                }
            }

            if (admin.assignedFunctions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    admin.assignedFunctions.take(4).forEach { fnKey ->
                        val fn = AdminFunction.fromKey(fnKey)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = rankColor.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, rankColor.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "${fn?.hindiTitle ?: fnKey} ✓",
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (admin.assignedFunctions.size > 4) {
                        Text(
                            text = "+${admin.assignedFunctions.size - 4} और...",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Actions: Disable (गुप्त रूप से निष्क्रिय), Block Device & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = admin.isEnabled,
                        onCheckedChange = { onToggleDisable(it) },
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (admin.isEnabled) "सक्रिय" else "निष्क्रिय",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (admin.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onToggleBlockDevice(!admin.isDeviceBlocked) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (admin.isDeviceBlocked) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (admin.isDeviceBlocked) Color(0xFF10B981) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = if (admin.isDeviceBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (admin.isDeviceBlocked) "अनब्लॉक" else "ब्लॉक डिवाइस",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { showQrDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldWarm),
                        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "QR कोड",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "QR पास",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "डिलीट प्रोफ़ाइल",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        SerialQrDisplayDialog(
            serialNumber = if (admin.id.isNotBlank()) admin.id else admin.name,
            p2Otp = if (admin.secondaryPin.isNotBlank()) admin.secondaryPin else null,
            roleTitle = admin.designation,
            remainingSeconds = (remainingTimeMs / 1000L).coerceAtLeast(0L),
            onDismiss = { showQrDialog = false }
        )
    }
}

/**
 * Specialized Dialog for adding/assigning hierarchical admin role matching exact specs:
 * 1. अधिकार श्रेणी सिलेक्ट करें (Master Admin, Bishop, उप बिशप, पास्टर, पुरनिया, विश्वासी)
 * 2. टारगेट मेंबर खोज व विवरण (नाम, फोन नंबर)
 * 3. वरिष्ठ प्राधिकारी चयन (Cascading Senior Authority Selector)
 * 4. P1 परमानेंट सिक्योरिटी पासवर्ड (4-12 डिजिट, कीबोर्ड टॉगल) व P2 6-अंकीय OTP (10 मिनट)
 * 5. अधिकारों की चेक लिस्ट (केवल अपने अंदर के अधिकार दे सके)
 * 6. प्रॉमिनेंट सेव बटन
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSubordinateAdminDialog(
    creatorAdmin: AdminUser,
    allowedCategories: List<Pair<String, Int>>,
    allAdmins: List<AdminUser> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        designation: String,
        name: String,
        phone: String,
        roleTier: String,
        reportsToSeniorId: String,
        reportsToSeniorName: String,
        pin: String,
        secondaryPin: String,
        assignedFunctions: List<String>,
        applyToCategory: Boolean
    ) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf(allowedCategories.firstOrNull()?.first ?: "पास्टर (Pastor)") }
    var selectedPrefix by remember { mutableStateOf("NCC") }
    var customPrefixInput by remember { mutableStateOf("") }
    var applyToCategory by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    // Map designation to internal roleTier
    val roleTier = remember(selectedCategory) {
        when {
            selectedCategory.contains("मास्टर", ignoreCase = true) || selectedCategory.contains("विनय", ignoreCase = true) -> "master_admin"
            selectedCategory.contains("उप बिशप", ignoreCase = true) || selectedCategory.contains("Deputy", ignoreCase = true) -> "deputy_bishop"
            selectedCategory.contains("बिशप", ignoreCase = true) || selectedCategory.contains("Bishop", ignoreCase = true) -> "bishop"
            selectedCategory.contains("पास्टर", ignoreCase = true) || selectedCategory.contains("Pastor", ignoreCase = true) -> "pastor"
            selectedCategory.contains("पुरनिया", ignoreCase = true) || selectedCategory.contains("Elder", ignoreCase = true) -> "elder"
            else -> "believer"
        }
    }

    // Cascading Senior Authorities list based on selected role tier
    val eligibleSeniors = remember(roleTier, allAdmins, creatorAdmin) {
        val list = when (roleTier) {
            "bishop" -> allAdmins.filter { it.rank >= AdminHierarchy.RANK_VINAY_KUMAR }
            "deputy_bishop" -> allAdmins.filter { it.rank >= AdminHierarchy.RANK_BISHOP }
            "pastor" -> allAdmins.filter { it.rank >= AdminHierarchy.RANK_DEPUTY_BISHOP }
            "elder" -> allAdmins.filter { it.rank >= AdminHierarchy.RANK_PASTOR }
            "believer" -> allAdmins.filter { it.rank >= AdminHierarchy.RANK_PURANIYA }
            else -> allAdmins.filter { it.rank >= AdminHierarchy.RANK_VINAY_KUMAR }
        }
        if (list.isEmpty()) listOf(creatorAdmin) else list
    }

    var selectedSenior by remember(eligibleSeniors) {
        mutableStateOf(eligibleSeniors.firstOrNull() ?: creatorAdmin)
    }

    // Auto-generate Password 2 (OTP) initially
    var otpInput by remember { mutableStateOf((100000..999999).random().toString()) }
    var otpGeneratedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Timer calculation for 10 minutes
    var remainingSeconds by remember { mutableLongStateOf(600L) }
    LaunchedEffect(otpGeneratedTimestamp) {
        while (true) {
            val elapsedSec = (System.currentTimeMillis() - otpGeneratedTimestamp) / 1000L
            val rem = 600L - elapsedSec
            remainingSeconds = if (rem > 0L) rem else 0L
            delay(1000L)
        }
    }

    val availableFunctions = remember(creatorAdmin) {
        if (creatorAdmin.rank >= AdminHierarchy.RANK_VINAY_KUMAR) {
            AdminFunction.entries
        } else {
            AdminFunction.entries.filter { creatorAdmin.hasFunction(it) }
        }
    }

    val selectedFunctions = remember { mutableStateListOf<String>() }

    LaunchedEffect(selectedCategory) {
        val targetRank = AdminHierarchy.getRankForDesignation(selectedCategory)
        val defaultIds = AdminHierarchy.getDefaultFunctionsForRank(targetRank)
        selectedFunctions.clear()
        selectedFunctions.addAll(defaultIds.filter { fnId: String ->
            creatorAdmin.rank >= AdminHierarchy.RANK_VINAY_KUMAR || creatorAdmin.hasFunction(fnId)
        })
    }

    var mintedSerialResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFullscreenQrModal by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (mintedSerialResult != null) "सीरियल आईडी जारी (Serial Issued)" else "नया सीरियल आईडी जारी करें (Issue Serial ID)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            if (mintedSerialResult != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SerialQrCard(
                        serialNumber = mintedSerialResult ?: "",
                        p2Otp = otpInput,
                        roleTitle = selectedCategory,
                        onExpandFullscreen = { showFullscreenQrModal = true }
                    )

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("P2 एक्टिवेशन OTP: $otpInput", fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "⏳ वैधता: %02d:%02d शेष (यह OTP सदस्य को एक्टिवेशन के लिए दें)".format(remainingSeconds / 60, remainingSeconds % 60),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "ℹ️ नया सदस्य अपने फोन में QR कोड स्कैन करके या सीरियल आईडी दर्ज करके अपनी प्रोफ़ाइल एक्टिवेट कर सकते हैं।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("सीरियल आईडी: $mintedSerialResult\nP2 OTP: $otpInput\nपद: $selectedCategory"))
                                Toast.makeText(context, "सीरियल व OTP कॉपी किया गया!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("कॉपी करें", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("संपन्न (Done)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "1. पदानुक्रम पद / श्रेणी चुनें (Role Tier) *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allowedCategories.forEach { (categoryName, _) ->
                                FilterChip(
                                    selected = selectedCategory == categoryName,
                                    onClick = { selectedCategory = categoryName },
                                    label = { Text(categoryName, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    leadingIcon = if (selectedCategory == categoryName) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "2. क्षेत्रीय / कलीसिया प्रिफिक्स चुनें (Prefix) *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        val defaultPrefixes = listOf("NCC", "DIO", "KOL", "VLG", "+ नया प्रिफिक्स")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            defaultPrefixes.forEach { pfx ->
                                val isSelected = if (pfx == "+ नया प्रिफिक्स") selectedPrefix == "CUSTOM" else selectedPrefix == pfx
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (pfx == "+ नया प्रिफिक्स") {
                                            selectedPrefix = "CUSTOM"
                                        } else {
                                            selectedPrefix = pfx
                                        }
                                    },
                                    label = { Text(pfx, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        if (selectedPrefix == "CUSTOM") {
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customPrefixInput,
                                onValueChange = { customPrefixInput = it.uppercase().trim() },
                                label = { Text("नया प्रिफिक्स दर्ज करें (उदा. BPL, RCH)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Text(
                            text = "3. रिपोर्टिंग सीनियर ऑथोरिटी (Reporting Senior):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${selectedSenior.name} (${selectedSenior.designation})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text("आईडी: ${selectedSenior.id}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "💡 जीरो-डेटा-एंट्री: आपको सदस्य का नाम या फोन भरने की आवश्यकता नहीं है। सिर्फ सीरियल आईडी जारी करें, बाकी विवरण सदस्य स्वयं भरेंगे।",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        item {
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (mintedSerialResult == null) {
                Button(
                    onClick = {
                        val effectivePrefix = if (selectedPrefix == "CUSTOM") customPrefixInput.ifBlank { "NCC" } else selectedPrefix
                        val serial = "${effectivePrefix}${(1..99).random()}"
                        mintedSerialResult = serial

                        onConfirm(
                            selectedCategory,
                            "Pending User ($serial)",
                            "",
                            roleTier,
                            selectedSenior.id,
                            "${selectedSenior.name} (${selectedSenior.designation})",
                            "0000",
                            otpInput.trim(),
                            selectedFunctions.toList(),
                            applyToCategory
                        )
                    },
                    modifier = Modifier.testTag("save_subordinate_admin_button")
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("सीरियल आईडी जारी करें (Issue Serial)", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (mintedSerialResult == null) {
                TextButton(onClick = onDismiss) {
                    Text("रद्द करें")
                }
            }
        }
    )

    if (showFullscreenQrModal && mintedSerialResult != null) {
        SerialQrDisplayDialog(
            serialNumber = mintedSerialResult ?: "",
            p2Otp = otpInput,
            roleTitle = selectedCategory,
            remainingSeconds = remainingSeconds,
            onDismiss = { showFullscreenQrModal = false }
        )
    }
}

/**
 * Preview Permissions Dialog showing exact privileges and visibility for an admin rank
 */
@Composable
fun AdminPermissionPreviewDialog(
    categoryName: String,
    assignedFunctions: List<String>,
    onDismiss: () -> Unit
) {
    val rank = AdminHierarchy.getRankForDesignation(categoryName)
    val rankColor = when (rank) {
        AdminHierarchy.RANK_BISHOP -> Color(0xFF9333EA)
        AdminHierarchy.RANK_DEPUTY_BISHOP -> Color(0xFF3B82F6)
        AdminHierarchy.RANK_PASTOR -> Color(0xFF10B981)
        else -> Color(0xFFEAB308)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = rankColor)
                Spacer(Modifier.width(8.dp))
                Text("अधिकार पूर्वावलोकन (Preview Permissions)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = rankColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, rankColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "श्रेणी: $categoryName (रैंक: $rank)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = rankColor
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "यह पूर्वावलोकन दर्शाता है कि इस श्रेणी के सभी सदस्यों के पास कौन-से अधिकार और पैनल विजिबिलिटी होगी।",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "आवंटित अधिकार (${assignedFunctions.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (assignedFunctions.isEmpty()) {
                    Text(
                        text = "कोई विशेषाधिकार नहीं (केवल सामान्य विश्वासी एक्सेस)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(assignedFunctions) { fnKey ->
                            val fn = AdminFunction.fromKey(fnKey)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = fn?.hindiTitle ?: fnKey,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        if (fn?.description != null) {
                                            Text(
                                                text = fn.description,
                                                fontSize = 9.sp,
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
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("समझ गया (Close)")
            }
        }
    )
}

/**
 * Master Admin Category Customization & Overwrite Dialog
 * "Master Admin कभी भी ये fixed कर सकेगा कि कौन सा श्रेणी के लोग कौन कौन सा अधिकार रखेंगे
 * (ऐसा करने पर निचले श्रेणी के admin को दिए गए अधिकार overwrite होंगे) इसके लिए कस्टमाइजेशन अलग से बनाओ"
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasterAdminCategoryCustomizationDialog(
    onDismiss: () -> Unit,
    onConfirm: (categoryRank: Int, categoryName: String, functions: List<String>) -> Unit
) {
    val categories = listOf(
        AdminHierarchy.ROLE_BISHOP to AdminHierarchy.RANK_BISHOP,
        AdminHierarchy.ROLE_DEPUTY_BISHOP to AdminHierarchy.RANK_DEPUTY_BISHOP,
        AdminHierarchy.ROLE_PASTOR to AdminHierarchy.RANK_PASTOR,
        AdminHierarchy.ROLE_PURANIYA to AdminHierarchy.RANK_PURANIYA
    )

    var selectedCategoryPair by remember { mutableStateOf(categories.first()) }
    val selectedFunctions = remember { mutableStateListOf<String>() }

    // Pre-populate with default functions for the selected category
    LaunchedEffect(selectedCategoryPair) {
        val defaultIds = AdminHierarchy.getDefaultFunctionsForRank(selectedCategoryPair.second)
        selectedFunctions.clear()
        selectedFunctions.addAll(defaultIds)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = GoldWarm)
                Spacer(Modifier.width(8.dp))
                Text("मास्टर एडमिन श्रेणी कस्टमाइजेशन", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "1. कस्टमाइज़ करने के लिए श्रेणी चुनें:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { pair ->
                            FilterChip(
                                selected = selectedCategoryPair == pair,
                                onClick = { selectedCategoryPair = pair },
                                label = { Text(pair.first, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldWarm.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ ध्यान दें: यहाँ तय किए गए अधिकार '${selectedCategoryPair.first}' श्रेणी के सभी मौजूदा सक्रिय एडमिन पर ओवरराइट (Overwrite) हो जाएंगे।",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "अधिकार चुनें (${selectedFunctions.size}/${AdminFunction.entries.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Row {
                            TextButton(onClick = {
                                selectedFunctions.clear()
                                selectedFunctions.addAll(AdminFunction.entries.map { it.name })
                            }) {
                                Text("सभी", fontSize = 11.sp)
                            }
                            TextButton(onClick = { selectedFunctions.clear() }) {
                                Text("हटाएं", fontSize = 11.sp)
                            }
                        }
                    }
                }

                AdminFunction.entries.groupBy { it.category }.forEach { (category, functions) ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    items(functions) { fn ->
                        val isChecked = selectedFunctions.contains(fn.name) || selectedFunctions.contains(fn.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        selectedFunctions.remove(fn.name)
                                        selectedFunctions.remove(fn.id)
                                    } else {
                                        selectedFunctions.add(fn.name)
                                    }
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!selectedFunctions.contains(fn.name)) selectedFunctions.add(fn.name)
                                    } else {
                                        selectedFunctions.remove(fn.name)
                                        selectedFunctions.remove(fn.id)
                                    }
                                },
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fn.hindiTitle, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                Text(fn.description, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedCategoryPair.second, selectedCategoryPair.first, selectedFunctions.toList())
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldWarm)
            ) {
                Text("अधिकार सेव करें व ओवरराइट करें", color = Color(0xFF1E1B4B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

/**
 * Dialog to Change Subordinate's Password 1 (Manual & Permanent)
 */
@Composable
fun ChangeSubordinatePinDialog(
    admin: AdminUser,
    onDismiss: () -> Unit,
    onConfirm: (newPin: String) -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    var isPinFullKeyboard by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${admin.name} का पासवर्ड 1 बदलें",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "पदनाम: ${admin.designation}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    FilledTonalButton(
                        onClick = { isPinFullKeyboard = !isPinFullKeyboard },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isPinFullKeyboard) Icons.Default.Pin else Icons.Default.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(if (isPinFullKeyboard) "🔢 123" else "⌨️ Keyboard", fontSize = 10.sp)
                    }
                }

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 12 && (isPinFullKeyboard || it.all { ch -> ch.isDigit() })) {
                            pinInput = it
                            errorMessage = null
                        }
                    },
                    label = { Text("नया पासवर्ड 1 (4 से 12 अंक)") },
                    placeholder = { Text("4 से 12 अंकों का पिन") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPinFullKeyboard) KeyboardType.Password else KeyboardType.NumberPassword
                    ),
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Icon(
                                if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    supportingText = {
                        Text("${pinInput.length}/12 अंक (न्यूनतम 4 आवश्यक)", fontSize = 10.sp)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "⚠️ सुरक्षा सूचना: नया पासवर्ड सेट करने पर इस एडमिन का पुराना सत्र तुरंत समाप्त हो जाएगा और पुराना पासवर्ड अमान्य हो जाएगा।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    lineHeight = 14.sp
                )

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinInput.trim().length !in 4..12) {
                        errorMessage = "पासवर्ड 4 से 12 अंकों का संख्यात्मक होना चाहिए।"
                        return@Button
                    }
                    onConfirm(pinInput.trim())
                }
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("सेव करें (Save PIN)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

/**
 * Dialog to edit assigned rights of an existing subordinate
 */
@Composable
fun EditSubordinateRightsDialog(
    targetAdmin: AdminUser,
    creatorAdmin: AdminUser,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val availableFunctions = remember(creatorAdmin) {
        if (creatorAdmin.rank >= AdminHierarchy.RANK_VINAY_KUMAR) {
            AdminFunction.entries
        } else {
            AdminFunction.entries.filter { creatorAdmin.hasFunction(it) }
        }
    }

    val selectedFunctions = remember {
        mutableStateListOf<String>().apply {
            addAll(targetAdmin.assignedFunctions.filter { fn ->
                creatorAdmin.rank >= AdminHierarchy.RANK_VINAY_KUMAR || creatorAdmin.hasFunction(fn)
            })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "${targetAdmin.name} के अधिकार ट्यून करें",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "पद: ${targetAdmin.designation}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "चयनित: ${selectedFunctions.size}/${availableFunctions.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            TextButton(onClick = {
                                selectedFunctions.clear()
                                selectedFunctions.addAll(availableFunctions.map { it.name })
                            }) {
                                Text("सभी", fontSize = 11.sp)
                            }
                            TextButton(onClick = { selectedFunctions.clear() }) {
                                Text("हटाएं", fontSize = 11.sp)
                            }
                        }
                    }
                }

                availableFunctions.groupBy { it.category }.forEach { (category, functions) ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    items(functions) { fn ->
                        val isChecked = selectedFunctions.contains(fn.name) || selectedFunctions.contains(fn.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        selectedFunctions.remove(fn.name)
                                        selectedFunctions.remove(fn.id)
                                    } else {
                                        selectedFunctions.add(fn.name)
                                    }
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!selectedFunctions.contains(fn.name)) selectedFunctions.add(fn.name)
                                    } else {
                                        selectedFunctions.remove(fn.name)
                                        selectedFunctions.remove(fn.id)
                                    }
                                },
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fn.hindiTitle, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                Text(fn.description, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedFunctions.toList()) }) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("अधिकार सेव करें (Save Permissions)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

/**
 * TreeView Node Composable for rendering admin delegation hierarchy
 */
@Composable
fun TreeViewNodeView(
    title: String,
    admins: List<AdminUser>,
    levelColor: Color,
    isRoot: Boolean
) {
    var expanded by remember { mutableStateOf(true) }
    val view = androidx.compose.ui.platform.LocalView.current
    var isLoadingAccordion by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = levelColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, levelColor.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    val next = !expanded
                    expanded = next
                    if (next) {
                        isLoadingAccordion = true
                        coroutineScope.launch {
                            delay(300)
                            isLoadingAccordion = false
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isRoot) Icons.Default.WorkspacePremium else Icons.Default.Folder,
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$title (${admins.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (expanded) {
            if (isLoadingAccordion) {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 6.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(2) {
                        com.example.ui.components.ShimmerLoadingBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        )
                    }
                }
            } else if (admins.isEmpty()) {
                Text(
                    text = "   └─ (कोई एडमिन इस श्रेणी में नहीं है)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 6.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    admins.forEach { admin ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = "├─", color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(levelColor.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = admin.name.take(1).uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = levelColor
                                            )
                                        }
                                        Column {
                                            Text(text = admin.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(text = "${admin.designation} • लेवल ${admin.rank}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Badge(containerColor = if (admin.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer) {
                                        Text(
                                            text = if (admin.isEnabled) "सक्रिय" else "निष्क्रिय",
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                if (admin.assignedFunctions.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "   권 अधिकार (${admin.assignedFunctions.size}): " + admin.assignedFunctions.joinToString(", ") { fnId ->
                                            AdminFunction.entries.find { it.id == fnId }?.hindiTitle ?: fnId
                                        },
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        lineHeight = 13.sp
                                    )
                                } else {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "   (कोई विशेषाधिकार नहीं सौंपा गया)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.error
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

/**
 * Dialog to delegate / grant or revoke a specific extra access function to a subordinate admin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegateExtraAccessDialog(
    functionToDelegate: AdminFunction,
    currentAdmin: AdminUser,
    subordinateAdmins: List<AdminUser>,
    onDismiss: () -> Unit,
    onConfirmGrant: (targetAdmin: AdminUser, functionId: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAdmin by remember { mutableStateOf<AdminUser?>(null) }

    val filteredList = remember(subordinateAdmins, searchQuery) {
        if (searchQuery.isBlank()) subordinateAdmins
        else subordinateAdmins.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.designation.contains(searchQuery, ignoreCase = true) ||
            it.linkedGmail.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GoldWarm.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "🤝 एक्स्ट्रा एक्सेस सौंपें (Delegate Access)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = functionToDelegate.titleHindi,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "सुविधा विवरण:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = functionToDelegate.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "श्रेणी: ${functionToDelegate.category}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldWarm
                        )
                    }
                }

                Text(
                    text = "चुनें कि किस कनिष्ठ एडमिन को यह एक्स्ट्रा एक्सेस देना चाहते हैं:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("नाम या पदनाम से खोजें...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "कोई परिणाम नहीं मिला।" else "कोई योग्य अधीनस्थ एडमिन नहीं मिला।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredList, key = { it.id }) { admin ->
                            val isSelected = selectedAdmin?.id == admin.id
                            val alreadyHasAccess = admin.hasFunction(functionToDelegate)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) GoldWarm.copy(alpha = 0.22f)
                                else if (alreadyHasAccess) Color(0xFF10B981).copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) GoldWarm else if (alreadyHasAccess) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAdmin = admin }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = admin.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${admin.designation} • लेवल ${admin.rank}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (alreadyHasAccess) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "अधिकृत ✅",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = GoldWarm,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isAlreadyGranted = selectedAdmin?.hasFunction(functionToDelegate) == true
            Button(
                onClick = {
                    selectedAdmin?.let { target ->
                        onConfirmGrant(target, functionToDelegate.id)
                    }
                },
                enabled = selectedAdmin != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAlreadyGranted) MaterialTheme.colorScheme.error else GoldWarm,
                    contentColor = if (isAlreadyGranted) Color.White else Color.Black
                )
            ) {
                Icon(
                    imageVector = if (isAlreadyGranted) Icons.Default.RemoveCircleOutline else Icons.Default.AddTask,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isAlreadyGranted) "एक्सेस वापस लें (Revoke)" else "अधिकार सौंपें (Grant Access)",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

/**
 * Card component displaying all available Extra Access functions with a button to delegate to subordinates
 */
@Composable
fun ExtraAccessDelegationHubCard(
    currentAdmin: AdminUser,
    onDelegateFunction: (AdminFunction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val accessibleFunctions = remember(currentAdmin) {
        if (currentAdmin.isMasterAdmin()) {
            AdminFunction.entries
        } else {
            AdminFunction.entries.filter { currentAdmin.hasFunction(it) }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("extra_access_delegation_hub_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = GoldWarm.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.5.dp, GoldWarm.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GoldWarm),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VolunteerActivism,
                            contentDescription = null,
                            tint = Color(0xFF1E1B4B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "🤝 एक्स्ट्रा एक्सेस एवं अधिकार वितरण हब",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "आपके सभी ${accessibleFunctions.size} एक्स्ट्रा एक्सेस किसी को भी सौंपें",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Extra Access Hub"
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = GoldWarm.copy(alpha = 0.3f))
                Spacer(Modifier.height(10.dp))

                Text(
                    text = "नीचे दिए गए किसी भी एक्स्ट्रा एक्सेस के 'दूसरों को दें' बटन पर क्लिक करके उसे अधीनस्थ एडमिन को तुरंत सौंपें:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accessibleFunctions.forEach { fn ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fn.titleHindi,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = fn.description,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { onDelegateFunction(fn) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldWarm,
                                        contentColor = Color.Black
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("दूसरों को दें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
 * Visual Feedback & Confirmation Dialog for Admin Role Assignment
 * Displays smooth Compose spring & rotation animations verifying P1 Security Password
 * and P2 6-Digit OTP completion.
 */
@Composable
fun AdminAssignmentSuccessDialog(
    admin: AdminUser,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Spring animation for badge entrance
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationStarted = true
    }

    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BadgeScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(400),
        label = "BadgeAlpha"
    )

    // Infinite rotation for shimmering halo
    val infiniteTransition = rememberInfiniteTransition(label = "HaloRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Success Badge with Rotating Glowing Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale)
                        .graphicsLayer { this.alpha = alpha }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF10B981).copy(alpha = 0.1f),
                                    Color(0xFF10B981),
                                    Color(0xFF059669),
                                    Color(0xFF10B981).copy(alpha = 0.1f)
                                )
                            ),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981),
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "पदभार आवंटन सफल!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "सुरक्षा सत्यापन पूर्ण (P1 & P2 Completed)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981)
                )

                Spacer(Modifier.height(14.dp))

                // Verification Steps Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "P1 स्थायी सुरक्षा पासवर्ड: एन्क्रिप्ट व सेट ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "P2 पुष्टि OTP: सत्यापित व सक्रिय (10 मिनट) ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Admin Details & OTP Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = admin.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = admin.designation,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (admin.reportsToSeniorName.isNotBlank()) {
                            Text(
                                text = "वरिष्ठ प्राधिकारी: ${admin.reportsToSeniorName}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (admin.secondaryPin.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "लॉगिन OTP (Password 2):",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = admin.secondaryPin,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(admin.secondaryPin))
                                        Toast.makeText(context, "OTP कॉपी किया गया: ${admin.secondaryPin}", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("कॉपी करें", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_assignment_success_done_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("सम्पन्न (Done)", fontWeight = FontWeight.Bold)
            }
        }
    )
}
