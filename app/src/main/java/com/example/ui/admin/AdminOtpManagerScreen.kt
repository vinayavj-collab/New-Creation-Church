package com.example.ui.admin

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminHierarchy
import com.example.data.model.AdminUser
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminOtpManagerScreen(
    viewModel: MainViewModel,
    currentAdmin: AdminUser?,
    allAdmins: List<AdminUser>,
    onNavigateToRoleManager: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val creatorRank = currentAdmin?.rank ?: 1
    val isVinayKumar = creatorRank >= AdminHierarchy.RANK_VINAY_KUMAR ||
            (currentAdmin?.designation?.contains("Vinay", ignoreCase = true) == true)

    // Subordinate admins: ONLY those below current admin rank
    val subordinateAdmins = remember(allAdmins, currentAdmin) {
        if (currentAdmin == null) emptyList()
        else allAdmins.filter { AdminHierarchy.canManageAdmin(currentAdmin.rank, it.rank) }
            .sortedByDescending { it.rank }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("सभी") }
    var selectedStatusFilter by remember { mutableStateOf("सभी") } // "सभी", "सक्रिय", "समाप्त"

    var adminForCustomOtp by remember { mutableStateOf<AdminUser?>(null) }
    var showBatchRefreshDialog by remember { mutableStateOf(false) }

    // Live tick to trigger recomposition for live remaining time
    var currentTickerTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTickerTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Filtered subordinates
    val filteredSubordinates = remember(subordinateAdmins, searchQuery, selectedCategoryFilter, selectedStatusFilter, currentTickerTime) {
        subordinateAdmins.filter { admin ->
            val matchesSearch = searchQuery.isBlank() ||
                    admin.name.contains(searchQuery, ignoreCase = true) ||
                    admin.designation.contains(searchQuery, ignoreCase = true) ||
                    admin.pin.contains(searchQuery)

            val matchesCategory = when (selectedCategoryFilter) {
                "सभी" -> true
                else -> admin.designation.contains(selectedCategoryFilter, ignoreCase = true)
            }

            val isOtpActive = admin.isOtpValid()
            val matchesStatus = when (selectedStatusFilter) {
                "सभी" -> true
                "सक्रिय" -> isOtpActive
                "समाप्त" -> !isOtpActive
                else -> true
            }

            matchesSearch && matchesCategory && matchesStatus
        }
    }

    val totalSubordinatesCount = subordinateAdmins.size
    val activeOtpCount = remember(subordinateAdmins, currentTickerTime) {
        subordinateAdmins.count { it.isOtpValid() }
    }
    val expiredOtpCount = totalSubordinatesCount - activeOtpCount

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
    ) {
        // 1. Hero Card: OTP Rules & Authority Context
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_otp_manager_hero_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isVinayKumar) GoldWarm.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isVinayKumar) GoldWarm else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isVinayKumar) GoldWarm else MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LockClock,
                                contentDescription = null,
                                tint = if (isVinayKumar) Color(0xFF1E1B4B) else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "10-मिनट अस्थायी OTP जनरेटर",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (isVinayKumar) GoldWarm else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "दूसरा पासवर्ड (Password 2) ऑथोरिटी कंसोल",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "लेवल $creatorRank",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Explanation Notice Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 1.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "दोहरे पासवर्ड (Dual-Security) का नियम:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "• पासवर्ड 1: व्यक्ति का स्थायी व्यक्तिगत पासवर्ड।\n" +
                                                "• पासवर्ड 2 (OTP): हाइयर ऑथोरिटी द्वारा जनरेट किया गया 6-अंकों का डायनामिक कोड। यह जनरेट होने के ठीक 10 मिनट तक ही वैध रहता है, जिसके बाद स्वतः समाप्त (Expire) हो जाता है।",
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (creatorRank <= AdminHierarchy.RANK_PURANIYA) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ℹ️ पुरनिया (Puraniya) के पास कोई अधीनस्थ श्रेणी नहीं है। आपका दूसरा पासवर्ड (OTP) आपके पास्टर/बिशप/मास्टर एडमिन द्वारा जारी किया जाता है।",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        if (currentAdmin != null && currentAdmin.secondaryPin.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "आपका वर्तमान पासवर्ड 2 (OTP):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            OtpCountdownTimer(
                                timestamp = currentAdmin.secondaryPinGeneratedTimestamp,
                                totalDurationMs = 600000L,
                                isCompact = false
                            )
                        }
                    }
                }
            }
        }

        // Only show controls if rank allows subordinate management
        if (creatorRank > AdminHierarchy.RANK_PURANIYA) {
            // 2. Metric Counters Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricStatCard(
                        title = "कुल अधीनस्थ",
                        count = totalSubordinatesCount.toString(),
                        subtitle = "आपके अधिकार में",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        icon = Icons.Default.Groups,
                        modifier = Modifier.weight(1f)
                    )

                    MetricStatCard(
                        title = "सक्रिय OTP",
                        count = activeOtpCount.toString(),
                        subtitle = "10 मिनट के अंदर",
                        containerColor = Color(0xFF10B981).copy(alpha = 0.12f),
                        contentColor = Color(0xFF047857),
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )

                    MetricStatCard(
                        title = "समाप्त / शून्य",
                        count = expiredOtpCount.toString(),
                        subtitle = "नया जनरेट आवश्यक",
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        contentColor = MaterialTheme.colorScheme.error,
                        icon = Icons.Default.HourglassDisabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Batch Action & Role Manager shortcut
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showBatchRefreshDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_batch_refresh_otps"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "सभी के लिए नया OTP बनाएं",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateToRoleManager,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_go_to_role_manager")
                    ) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("रोल मैनेजमेंट", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 4. Search and Filter Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_otp_admins_input"),
                    placeholder = { Text("नाम, पद या पिन द्वारा खोजें...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 5. Category & Status Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Status filter row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "स्थिति:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf("सभी", "सक्रिय", "समाप्त").forEach { status ->
                            FilterChip(
                                selected = selectedStatusFilter == status,
                                onClick = { selectedStatusFilter = status },
                                label = {
                                    Text(
                                        when (status) {
                                            "सक्रिय" -> "सक्रिय (Active)"
                                            "समाप्त" -> "समाप्त (Expired)"
                                            else -> "सभी ($totalSubordinatesCount)"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                leadingIcon = if (selectedStatusFilter == status) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    // Category filter chips
                    val categories = listOf("सभी", "बिशप", "उप बिशप", "पास्टर", "पुरनिया")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text(
                                text = "पदनाम:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // 6. Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "अधीनस्थ प्रशासक सूची (${filteredSubordinates.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "प्रत्येक OTP ठीक 10 मिनट मान्य",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 7. Subordinate List or Empty State
            if (filteredSubordinates.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (subordinateAdmins.isEmpty())
                                    "अभी तक कोई अधीनस्थ एडमिन नहीं जोड़ा गया है।"
                                else
                                    "चयनित फ़िल्टर के अनुसार कोई एडमिन नहीं मिला।",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (subordinateAdmins.isEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = onNavigateToRoleManager,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("नया एडमिन अलॉट करें", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredSubordinates, key = { it.id }) { admin ->
                    SubordinateOtpCard(
                        admin = admin,
                        onRegenerate = {
                            viewModel.regenerateSecondaryPin(admin.id) { success, newPin, err ->
                                if (success && newPin != null) {
                                    clipboardManager.setText(AnnotatedString(newPin))
                                    Toast.makeText(
                                        context,
                                        "${admin.name} के लिए नया OTP ($newPin) जनरेट हुआ और कॉपी कर लिया गया! 10 मिनट के लिए वैध है। ⏳",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCustomOtp = {
                            adminForCustomOtp = admin
                        },
                        onShareOtp = { pin ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "नमस्ते ${admin.name} जी,\n\nचर्च एडमिन पोर्टल में लॉगिन हेतु आपका दूसरा पासवर्ड (OTP) है:\n👉 $pin\n\n⚠️ यह OTP अगले 10 मिनट के लिए वैध है। इसके बाद स्वतः अमान्य हो जाएगा।"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "${admin.name} को OTP भेजें"))
                        }
                    )
                }
            }
        }
    }

    // --- Dialogs ---

    // Custom OTP Modal Dialog
    adminForCustomOtp?.let { target ->
        CustomOtpSettingDialog(
            admin = target,
            onDismiss = { adminForCustomOtp = null },
            onConfirm = { customOtp ->
                viewModel.regenerateSecondaryPin(target.id, customOtp) { success, updatedPin, err ->
                    if (success && updatedPin != null) {
                        clipboardManager.setText(AnnotatedString(updatedPin))
                        Toast.makeText(
                            context,
                            "${target.name} के लिए कस्टम OTP ($updatedPin) सेट किया गया! 10 मिनट के लिए वैध है। ⏳",
                            Toast.LENGTH_LONG
                        ).show()
                        adminForCustomOtp = null
                    } else {
                        Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Batch Refresh Dialog
    if (showBatchRefreshDialog && subordinateAdmins.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchRefreshDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("सभी के लिए नया OTP बनाएं?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "क्या आप अपने सभी ${subordinateAdmins.size} अधीनस्थ एडमिन के लिए एक साथ नया 10-मिनट का OTP जनरेट करना चाहते हैं? इससे उनके पुराने सभी सक्रिय OTP तुरंत समाप्त हो जाएंगे।",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ids = subordinateAdmins.map { it.id }
                        viewModel.regenerateAllSubordinateOtps(ids) { success, count, err ->
                            if (success) {
                                Toast.makeText(
                                    context,
                                    "$count एडमिन के लिए नए OTP सफलतापूर्वक जनरेट किए गए! ✅",
                                    Toast.LENGTH_LONG
                                ).show()
                                showBatchRefreshDialog = false
                            } else {
                                Toast.makeText(context, err ?: "त्रुटि", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("हां, सभी जनरेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchRefreshDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

/**
 * High-craft Subordinate OTP Management Card
 */
@Composable
fun SubordinateOtpCard(
    admin: AdminUser,
    onRegenerate: () -> Unit,
    onCustomOtp: () -> Unit,
    onShareOtp: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
    val progressFraction = (remainingTimeMs.toFloat() / 600000f).coerceIn(0f, 1f)

    val rankColor = when (admin.rank) {
        AdminHierarchy.RANK_BISHOP -> Color(0xFF9333EA)
        AdminHierarchy.RANK_DEPUTY_BISHOP -> Color(0xFF3B82F6)
        AdminHierarchy.RANK_PASTOR -> Color(0xFF10B981)
        else -> Color(0xFFEAB308)
    }
    val view = LocalView.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subordinate_otp_card_${admin.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOtpValid) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.2.dp,
            if (isOtpValid) Color(0xFF10B981).copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Designation, Name, Active status
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
                    Text(
                        text = admin.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (admin.createdByDesignation.isNotBlank()) {
                        Text(
                            text = "अलॉटकर्ता: ${admin.createdByDesignation}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // OTP Status Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isOtpValid) Color(0xFF10B981).copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    border = BorderStroke(
                        0.8.dp,
                        if (isOtpValid) Color(0xFF10B981).copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOtpValid) Color(0xFF10B981) else MaterialTheme.colorScheme.error)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isOtpValid) "सक्रिय (Active)" else "समाप्त (Expired)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isOtpValid) Color(0xFF047857) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // OTP Showcase Container
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isOtpValid) Color(0xFF10B981).copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f),
                border = BorderStroke(
                    1.dp,
                    if (isOtpValid) Color(0xFF10B981).copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "पासवर्ड 2 (10-Minute OTP):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            if (admin.secondaryPin.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    admin.secondaryPin.forEach { digit ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isOtpValid) Color(0xFF10B981).copy(alpha = 0.6f)
                                                else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Text(
                                                text = digit.toString(),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isOtpValid) Color(0xFF047857) else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "अनुपलब्ध (Not Generated)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Copy & Share buttons
                        if (admin.secondaryPin.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(admin.secondaryPin))
                                        Toast.makeText(context, "OTP कॉपी किया गया: ${admin.secondaryPin}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy OTP",
                                        tint = if (isOtpValid) Color(0xFF047857) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onShareOtp(admin.secondaryPin) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share OTP",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Visual 10-minute Countdown Timer specifically for Password 2 (OTP)
                    OtpCountdownTimer(
                        timestamp = admin.secondaryPinGeneratedTimestamp,
                        totalDurationMs = 600000L,
                        isCompact = false
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Action Buttons Row: Instant Refresh & Custom OTP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        onRegenerate()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOtpValid) MaterialTheme.colorScheme.primary else Color(0xFF10B981)
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isOtpValid) "नया OTP जनरेट करें" else "⚡ नया OTP सक्रिय करें",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        onCustomOtp()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("कस्टम OTP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Metric summary card
 */
@Composable
fun MetricStatCard(
    title: String,
    count: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = count,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = contentColor.copy(alpha = 0.7f),
                lineHeight = 11.sp
            )
        }
    }
}

/**
 * Modal dialog to set a custom 4 to 6 digit temporary OTP
 */
@Composable
fun CustomOtpSettingDialog(
    admin: AdminUser,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var customOtpInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dialpad, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("कस्टम OTP निर्धारित करें", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "'${admin.name}' (${admin.designation}) के लिए अस्थायी OTP दर्ज करें:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = customOtpInput,
                    onValueChange = {
                        if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                            customOtpInput = it
                            errorMessage = null
                        }
                    },
                    label = { Text("4 से 6 अंकों का OTP") },
                    placeholder = { Text("उदा. 482910") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ℹ️ यह OTP भी सेट होने के ठीक 10 मिनट तक ही मान्य रहेगा और फिर स्वतः समाप्त हो जाएगा।",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customOtpInput.length < 4) {
                        errorMessage = "कम से कम 4 अंकों का OTP आवश्यक है।"
                        return@Button
                    }
                    onConfirm(customOtpInput)
                }
            ) {
                Text("सेट करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
