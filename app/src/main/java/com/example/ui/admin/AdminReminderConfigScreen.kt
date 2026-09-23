package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminReminderScheduleConfig
import com.example.data.model.ReminderTargetScope
import com.example.data.model.UserProfileData
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReminderConfigScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentConfig by viewModel.adminReminderScheduleConfig.collectAsState()
    val userProfiles by viewModel.appUserProfiles.collectAsState()

    // Form state initialized from currentConfig
    var prayerHour by remember(currentConfig) { mutableIntStateOf(currentConfig.prayerHour) }
    var prayerMinute by remember(currentConfig) { mutableIntStateOf(currentConfig.prayerMinute) }
    var prayerEnabled by remember(currentConfig) { mutableStateOf(currentConfig.prayerEnabled) }

    var verseAlarmHour by remember(currentConfig) { mutableIntStateOf(currentConfig.verseAlarmHour) }
    var verseAlarmMinute by remember(currentConfig) { mutableIntStateOf(currentConfig.verseAlarmMinute) }
    var verseAlarmEnabled by remember(currentConfig) { mutableStateOf(currentConfig.verseAlarmEnabled) }

    var readingMorningHour by remember(currentConfig) { mutableIntStateOf(currentConfig.readingMorningHour) }
    var readingMorningMinute by remember(currentConfig) { mutableIntStateOf(currentConfig.readingMorningMinute) }
    var readingEveningHour by remember(currentConfig) { mutableIntStateOf(currentConfig.readingEveningHour) }
    var readingEveningMinute by remember(currentConfig) { mutableIntStateOf(currentConfig.readingEveningMinute) }
    var readingPlanEnabled by remember(currentConfig) { mutableStateOf(currentConfig.readingPlanEnabled) }

    var selectedScope by remember(currentConfig) { mutableStateOf(currentConfig.targetScope) }
    var selectedTargetProfileId by remember(currentConfig) { mutableStateOf(currentConfig.targetProfileId) }
    var selectedTargetProfileName by remember(currentConfig) { mutableStateOf(currentConfig.targetProfileName) }

    var isSaving by remember { mutableStateOf(false) }
    var showProfilePickerSheet by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "रिमाइंडर व अलार्म समय",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "मास्टर एडमिन: डिफ़ॉल्ट व लक्षित समय नियंत्रण",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "वापस जाएं"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Reset to system defaults
                            prayerHour = 4; prayerMinute = 0; prayerEnabled = true
                            verseAlarmHour = 6; verseAlarmMinute = 0; verseAlarmEnabled = true
                            readingMorningHour = 5; readingMorningMinute = 0
                            readingEveningHour = 21; readingEveningMinute = 0
                            readingPlanEnabled = true
                            selectedScope = ReminderTargetScope.ONLY_UNMODIFIED_DEFAULTS
                            selectedTargetProfileId = ""
                            selectedTargetProfileName = ""
                            Toast.makeText(context, "डिफ़ॉल्ट समय रीसेट किया गया", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("डिफ़ॉल्ट")
                    }

                    Button(
                        onClick = {
                            if (selectedScope == ReminderTargetScope.SPECIFIC_PROFILE && selectedTargetProfileId.isBlank()) {
                                Toast.makeText(context, "कृपया पहले लक्षित प्रोफाइल चुनें", Toast.LENGTH_SHORT).show()
                                showProfilePickerSheet = true
                            } else {
                                showConfirmDialog = true
                            }
                        },
                        modifier = Modifier.weight(2f),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("लागू हो रहा है...")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("समय लागू करें", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            // Master Admin Info Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = GoldWarm.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoldWarm.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = NavyPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "मास्टर एडमिन समय कॉन्फ़िगरेशन",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                            )
                            Text(
                                text = "यहाँ से आप सभी विश्वासी सदस्यों के लिए दैनिक प्रार्थना, आज का वचन अलार्म और बाइबल रीडिंग का समय सेट कर सकते हैं।",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }

            // 1. Prayer Notification Time
            item {
                ReminderSectionCard(
                    title = "1. दैनिक प्रार्थना नोटिफिकेशन समय",
                    subtitle = "सुबह की प्रार्थना व प्रेरक बाइबल वचन की पुश सूचना",
                    icon = Icons.Default.VolunteerActivism,
                    enabled = prayerEnabled,
                    onEnabledChange = { prayerEnabled = it },
                    hour = prayerHour,
                    minute = prayerMinute,
                    onTimeChange = { h, m ->
                        prayerHour = h
                        prayerMinute = m
                    }
                )
            }

            // 2. Verse of the Day TTS Alarm
            item {
                ReminderSectionCard(
                    title = "2. आज का वचन TTS अलार्म समय",
                    subtitle = "निर्धारित समय पर अलार्म बजने पर हिंदी में बोलकर वचन सुनाएगा",
                    icon = Icons.Default.Alarm,
                    enabled = verseAlarmEnabled,
                    onEnabledChange = { verseAlarmEnabled = it },
                    hour = verseAlarmHour,
                    minute = verseAlarmMinute,
                    onTimeChange = { h, m ->
                        verseAlarmHour = h
                        verseAlarmMinute = m
                    }
                )
            }

            // 3. Bible Reading Plan Reminder
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "3. बाइबल पठन अनुस्मारक समय",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "दैनिक बाइबल रीडिंग प्लान का सुबह व शाम का स्मरण",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                            Switch(
                                checked = readingPlanEnabled,
                                onCheckedChange = { readingPlanEnabled = it }
                            )
                        }

                        if (readingPlanEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "सुबह का स्मरण (Morning):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            InlineTimePicker(
                                hour = readingMorningHour,
                                minute = readingMorningMinute,
                                onTimeChange = { h, m ->
                                    readingMorningHour = h
                                    readingMorningMinute = m
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "शाम का स्मरण (Evening):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            InlineTimePicker(
                                hour = readingEveningHour,
                                minute = readingEveningMinute,
                                onTimeChange = { h, m ->
                                    readingEveningHour = h
                                    readingEveningMinute = m
                                }
                            )
                        }
                    }
                }
            }

            // 4. Target Scope Selection (लागू करने का दायरा)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "लागू करने का दायरा (Target Scope)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "चुनें कि यह नया समय किन उपयोगकर्ताओं के लिए लागू होना चाहिए:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        ReminderTargetScope.entries.forEach { scope ->
                            val isSelected = selectedScope == scope
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) NavyPrimary.copy(alpha = 0.08f) else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) NavyPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedScope = scope }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedScope = scope },
                                        colors = RadioButtonDefaults.colors(selectedColor = NavyPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = scope.titleHindi,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) NavyPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = scope.description,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // If SPECIFIC_PROFILE selected, show profile picker
                        if (selectedScope == ReminderTargetScope.SPECIFIC_PROFILE) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, GoldWarm),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "चयनित प्रोफाइल:",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                                    )
                                    if (selectedTargetProfileId.isNotBlank()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = selectedTargetProfileName.ifBlank { "अज्ञात उपयोगकर्ता" },
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = "ID: $selectedTargetProfileId",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                            }
                                            TextButton(onClick = { showProfilePickerSheet = true }) {
                                                Text("बदलें")
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "कोई प्रोफाइल नहीं चुना गया",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        Button(
                                            onClick = { showProfilePickerSheet = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = NavyPrimary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("सदस्य प्रोफाइल चुनें")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Last Updated Audit Info
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "वर्तमान स्थिति (Active Status)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "अंतिम संशोधन: ${currentConfig.updatedBy}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        val dateStr = remember(currentConfig.timestamp) {
                            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            sdf.format(Date(currentConfig.timestamp))
                        }
                        Text(
                            text = "तारीख व समय: $dateStr",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "वर्तमान स्कोप: ${currentConfig.targetScope.titleHindi}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }
    }

    // Profile Picker Dialog / Sheet
    if (showProfilePickerSheet) {
        ProfilePickerDialog(
            userProfiles = userProfiles,
            onDismiss = { showProfilePickerSheet = false },
            onProfileSelected = { profile ->
                selectedTargetProfileId = profile.deviceId.ifBlank { profile.phoneNumber.ifBlank { profile.displayName } }
                selectedTargetProfileName = profile.displayName.ifBlank { profile.phoneNumber }
                showProfilePickerSheet = false
            }
        )
    }

    // Confirmation Dialog before applying
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "समय कॉन्फ़िगरेशन लागू करें?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "क्या आप निम्नलिखित नया समय लागू करना चाहते हैं?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("• प्रार्थना: ${formatTime(prayerHour, prayerMinute)} (${if (prayerEnabled) "चालू" else "बंद"})")
                            Text("• वचन अलार्म: ${formatTime(verseAlarmHour, verseAlarmMinute)} (${if (verseAlarmEnabled) "चालू" else "बंद"})")
                            Text("• बाइबल रीडिंग: सुबह ${formatTime(readingMorningHour, readingMorningMinute)} / शाम ${formatTime(readingEveningHour, readingEveningMinute)}")
                            Text("• दायरा: ${selectedScope.titleHindi}", fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                            if (selectedScope == ReminderTargetScope.SPECIFIC_PROFILE) {
                                Text("• प्रोफाइल: $selectedTargetProfileName ($selectedTargetProfileId)")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isSaving = true
                        val newConfig = AdminReminderScheduleConfig(
                            prayerHour = prayerHour,
                            prayerMinute = prayerMinute,
                            prayerEnabled = prayerEnabled,
                            verseAlarmHour = verseAlarmHour,
                            verseAlarmMinute = verseAlarmMinute,
                            verseAlarmEnabled = verseAlarmEnabled,
                            readingMorningHour = readingMorningHour,
                            readingMorningMinute = readingMorningMinute,
                            readingEveningHour = readingEveningHour,
                            readingEveningMinute = readingEveningMinute,
                            readingPlanEnabled = readingPlanEnabled,
                            targetScope = selectedScope,
                            targetProfileId = selectedTargetProfileId,
                            targetProfileName = selectedTargetProfileName
                        )
                        viewModel.updateReminderScheduleConfig(newConfig) { success, errorMsg ->
                            isSaving = false
                            if (success) {
                                Toast.makeText(context, "नया समय सफलतापूर्वक लागू कर दिया गया!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "त्रुटि: ${errorMsg ?: "सहेजा नहीं जा सका"}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("हाँ, लागू करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun ReminderSectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                InlineTimePicker(
                    hour = hour,
                    minute = minute,
                    onTimeChange = onTimeChange
                )
            }
        }
    }
}

@Composable
fun InlineTimePicker(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    val formattedTime = formatTime(hour, minute)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "निर्धारित समय:",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Hour adjustments
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("घंटा", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val nextH = (hour - 1 + 24) % 24
                                onTimeChange(nextH, minute)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-1 घंटा", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                val nextH = (hour + 1) % 24
                                onTimeChange(nextH, minute)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+1 घंटा", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Minute adjustments
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("मिनट", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val nextM = (minute - 5 + 60) % 60
                                onTimeChange(hour, nextM)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-5 मिनट", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                val nextM = (minute + 5) % 60
                                onTimeChange(hour, nextM)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+5 मिनट", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePickerDialog(
    userProfiles: List<UserProfileData>,
    onDismiss: () -> Unit,
    onProfileSelected: (UserProfileData) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredProfiles = remember(userProfiles, searchQuery) {
        if (searchQuery.isBlank()) {
            userProfiles
        } else {
            val q = searchQuery.trim().lowercase()
            userProfiles.filter {
                it.displayName.lowercase().contains(q) ||
                it.phoneNumber.contains(q) ||
                it.city.lowercase().contains(q) ||
                it.role.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "लक्षित विश्वासी प्रोफाइल चुनें",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "जिस प्रोफाइल के लिए समय लागू करना चाहते हैं उसे चुनें:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("नाम या मोबाइल से खोजें...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (filteredProfiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userProfiles.isEmpty()) "कोई सदस्य प्रोफाइल उपलब्ध नहीं है" else "कोई मेल नहीं मिला",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredProfiles) { profile ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onProfileSelected(profile) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(NavyPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = profile.displayName.take(1).ifBlank { "व" },
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.displayName.ifBlank { "अनाम सदस्य" },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val details = listOfNotNull(
                                            profile.phoneNumber.takeIf { it.isNotBlank() },
                                            profile.city.takeIf { it.isNotBlank() },
                                            profile.role.takeIf { it.isNotBlank() }
                                        ).joinToString(" • ")
                                        if (details.isNotBlank()) {
                                            Text(
                                                text = details,
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
            TextButton(onClick = onDismiss) {
                Text("बंद करें")
            }
        }
    )
}

fun formatTime(hour: Int, minute: Int): String {
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (hour < 12) "AM" else "PM"
    val hindiPeriod = if (hour < 12) "सुबह" else if (hour < 16) "दोपहर" else if (hour < 20) "शाम" else "रात"
    return String.format(Locale.getDefault(), "%s %02d:%02d %s", hindiPeriod, h12, minute, amPm)
}
