package com.example.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.util.ProfileManager
import com.example.ui.components.FavoriteCategoriesDialog
import com.example.ui.viewmodel.MainViewModel
import com.example.widget.BibleVerseWidgetProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onAboutClick: () -> Unit,
    onCustomizeHomeClick: () -> Unit,
    onSyncCenterClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    scrollToUpdateSection: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = appStrings()
    val settings by viewModel.settings.collectAsState()
    val fellowshipCategories by viewModel.fellowshipCategories.collectAsState()

    val currentProfile = LocalAppProfile.current
    var showPasswordDialogForProfileB by remember { mutableStateOf(false) }
    var profilePasswordInput by remember { mutableStateOf("") }
    var profilePasswordVisible by remember { mutableStateOf(false) }
    var profileTapCount by remember { mutableIntStateOf(0) }
    var lastProfileTapTime by remember { mutableLongStateOf(0L) }
    var activeWarningToast by remember { mutableStateOf<Toast?>(null) }

    var showFavCategoriesDialog by remember { mutableStateOf(false) }
    var showWelcomeCustomizationDialog by remember { mutableStateOf(false) }
    var showVerseAlarmTimePicker by remember { mutableStateOf(false) }
    var showReadingPlanTimePicker by remember { mutableStateOf(false) }
    var showDailyPrayerTimePicker by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(scrollToUpdateSection) {
        if (scrollToUpdateSection) {
            // Index for AppUpdateSection
            listState.animateScrollToItem(index = 28)
        }
    }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "सूचना अनुमति सक्रिय है (Notification Permission Granted)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "अलार्म और रिमाइंडर के लिए नोटिफिकेशन अनुमति आवश्यक है", Toast.LENGTH_LONG).show()
        }
    }

    val requestNotificationPermissionIfNeeded: (() -> Unit) -> Unit = { onPermissionReady ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                onPermissionReady()
            } else {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onPermissionReady()
        }
    }

    val shareApp = {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "Vinay Kumar AVJ - Fellowship Events • Videos • Photos • Holy Bible • Christian Songs\nDownload the official app to stay connected with fellowship events and worship!"
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, strings.shareApp))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(strings.settingsTitle, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // PROFILE SWITCH SECTION (Dual-Profile System)
            item {
                SettingsSectionHeader(
                    title = "ऐप प्रोफ़ाइल (APP PROFILE & BRANDING)",
                    icon = Icons.Default.AccountCircle
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (currentProfile == AppProfile.CHURCH) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(id = currentProfile.appIconRes),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentProfile.displayNameEnglish,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (currentProfile == AppProfile.CHURCH) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (currentProfile == AppProfile.CHURCH) "डिफ़ॉल्ट" else "निजी",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (currentProfile == AppProfile.CHURCH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = currentProfile.displayNameHindi,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        if (currentProfile == AppProfile.CHURCH) {
                            Button(
                                onClick = {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastProfileTapTime < 500L) {
                                        profileTapCount++
                                    } else {
                                        profileTapCount = 1
                                    }
                                    lastProfileTapTime = currentTime

                                    if (profileTapCount >= 3) {
                                        profileTapCount = 0
                                        activeWarningToast?.cancel()
                                        profilePasswordInput = ""
                                        profilePasswordVisible = false
                                        showPasswordDialogForProfileB = true
                                    } else {
                                        activeWarningToast?.cancel()
                                        activeWarningToast = Toast.makeText(
                                            context,
                                            "Unauthorised Access (अनुमति नहीं है)",
                                            Toast.LENGTH_SHORT
                                        )
                                        activeWarningToast?.show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Switch to Vinay Kumar Avj (पासवर्ड आवश्यक)")
                            }
                        } else {
                            Button(
                                onClick = {
                                    ProfileManager.setActiveProfile(context, AppProfile.CHURCH)
                                    Toast.makeText(context, "प्रोफ़ाइल 'New Creation Church' सक्रिय किया गया", Toast.LENGTH_SHORT).show()
                                    (context as? android.app.Activity)?.let {
                                        ProfileManager.restartApp(it)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.Church, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Switch to New Creation Church (डिफ़ॉल्ट)")
                            }
                        }
                    }
                }
            }

            // 0. Welcome Speech & Greeting Customization Section
            item {
                SettingsSectionHeader(title = "अभिवादन एवं वॉइस सेटिंग (WELCOME & VOICE)", icon = Icons.Default.RecordVoiceOver)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Saved Name: ${settings.userName.ifBlank { "सहेजा नहीं गया (Not set)" }}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (settings.userName.isNotBlank()) "अभिवादन वाक्य: \"${settings.userName} जी, जय मसीह की\"" else "नाम न होने पर केवल आज का वचन पढ़ा जाएगा.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Button(
                                onClick = { showWelcomeCustomizationDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("कस्टमाइज़")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        SettingsSwitchRow(
                            title = "नाम के साथ अभिवादन सुनें (Foreground Only)",
                            subtitle = "केवल ऐप चालू रहने पर '[नाम] जी, जय मसीह की' बोलेगा",
                            checked = settings.enableWelcomeSpeech,
                            onCheckedChange = { viewModel.updateEnableWelcomeSpeech(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsSwitchRow(
                            title = "अभिवादन के बाद 'आज का वचन' सुनें",
                            subtitle = "'आज का वचन है' कहकर आज का पवित्र शास्त्र वचन पढ़ेगा",
                            checked = settings.enableVerseSpeechOnLaunch,
                            onCheckedChange = { viewModel.updateEnableVerseSpeechOnLaunch(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsSwitchRow(
                            title = "हर दिन केवल एक बार अभिवादन सुनें",
                            subtitle = "दिन में पहली बार ऐप ओपन करने पर ही अभिवादन बोलेगा",
                            checked = settings.welcomeSpeechOncePerDay,
                            onCheckedChange = { viewModel.updateWelcomeSpeechOncePerDay(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsSwitchRow(
                            title = "दिन में केवल एक ही बार वचन सुनें",
                            subtitle = "आज का वचन दिन में केवल एक बार बोला जाएगा",
                            checked = settings.verseSpeechOncePerDay,
                            onCheckedChange = { viewModel.updateVerseSpeechOncePerDay(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Text(
                            text = "🔊 स्वतंत्र आवाज़ व टोन सेटिंग (Voice Controls)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "वॉइस वॉल्यूम: ${(settings.greetingSpeechVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = settings.greetingSpeechVolume,
                            onValueChange = { viewModel.updateGreetingSpeechVolume(it) },
                            valueRange = 0.1f..1.0f
                        )

                        Text(
                            text = "वॉइस पिच (Pitch): ${String.format(java.util.Locale.US, "%.1f", settings.greetingSpeechPitch)}x",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = settings.greetingSpeechPitch,
                            onValueChange = { viewModel.updateGreetingSpeechPitch(it) },
                            valueRange = 0.5f..1.5f
                        )

                        Text(
                            text = "बोलने की गति (Speed): ${String.format(java.util.Locale.US, "%.1f", settings.greetingSpeechSpeed)}x",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = settings.greetingSpeechSpeed,
                            onValueChange = { viewModel.updateGreetingSpeechSpeed(it) },
                            valueRange = 0.5f..1.5f
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.testWelcomeSpeech() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔊 टेस्ट वॉइस (Test TTS Voice)")
                        }
                    }
                }
            }

            // 0.5 Today's Verse Alarm & Customization Section
            item {
                SettingsSectionHeader(title = "आज का वचन अलार्म एवं अनुस्मारक (VERSE ALARM)", icon = Icons.Default.Alarm)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsSwitchRow(
                            title = "आज का वचन अलार्म सक्षम करें (Enable Alarm)",
                            subtitle = "निर्धारित समय पर आज का वचन अलार्म व नोटिफिकेशन प्राप्त करें",
                            checked = settings.verseAlarmEnabled,
                            onCheckedChange = { isEnabled ->
                                if (isEnabled) {
                                    requestNotificationPermissionIfNeeded {
                                        viewModel.updateVerseAlarmEnabled(true)
                                    }
                                } else {
                                    viewModel.updateVerseAlarmEnabled(false)
                                }
                            }
                        )

                        if (settings.verseAlarmEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // Alarm Time Picker Button
                            val formattedAlarmTime = String.format(
                                java.util.Locale.US,
                                "%02d:%02d %s",
                                if (settings.verseAlarmHour % 12 == 0) 12 else settings.verseAlarmHour % 12,
                                settings.verseAlarmMinute,
                                if (settings.verseAlarmHour >= 12) "PM" else "AM"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "अलार्म समय (Alarm Time)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "निर्धारित: $formattedAlarmTime",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Button(
                                    onClick = { showVerseAlarmTimePicker = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("समय बदलें")
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // Frequency Option: Daily Once vs Fixed Interval
                            Text(
                                text = "अलार्म दोहराव (Frequency):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = settings.verseAlarmFrequency == VerseAlarmFrequency.DAILY,
                                    onClick = { viewModel.updateVerseAlarmFrequency(VerseAlarmFrequency.DAILY) },
                                    label = { Text("प्रतिदिन 1 बार") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = settings.verseAlarmFrequency == VerseAlarmFrequency.INTERVAL_HOURS,
                                    onClick = { viewModel.updateVerseAlarmFrequency(VerseAlarmFrequency.INTERVAL_HOURS) },
                                    label = { Text("निश्चित अंतराल पर") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (settings.verseAlarmFrequency == VerseAlarmFrequency.INTERVAL_HOURS) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "प्रत्येक कितने घंटे बाद अलार्म बजे:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(2, 4, 6, 8, 12).forEach { interval ->
                                        FilterChip(
                                            selected = settings.verseAlarmIntervalHours == interval,
                                            onClick = { viewModel.updateVerseAlarmIntervalHours(interval) },
                                            label = { Text("${interval}h") }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // Alarm Content Option: Greeting + Verse vs Only Verse
                            Text(
                                text = "अलार्म सामग्री (Content):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = settings.verseAlarmContent == VerseAlarmContent.GREETING_AND_VERSE,
                                    onClick = { viewModel.updateVerseAlarmContent(VerseAlarmContent.GREETING_AND_VERSE) },
                                    label = { Text("अभिवादन + आज का वचन") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = settings.verseAlarmContent == VerseAlarmContent.VERSE_ONLY,
                                    onClick = { viewModel.updateVerseAlarmContent(VerseAlarmContent.VERSE_ONLY) },
                                    label = { Text("केवल आज का वचन") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // Alarm Flow / Mode Option: Notification only vs Alarm & Direct TTS vs Music first then TTS
                            Text(
                                text = "अलार्म प्रकार (Alarm Mode & Music):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = settings.verseAlarmMode == VerseAlarmMode.NOTIFICATION_ONLY,
                                    onClick = { viewModel.updateVerseAlarmMode(VerseAlarmMode.NOTIFICATION_ONLY) },
                                    label = { Text("1. केवल नोटिफिकेशन (Notification Only)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                FilterChip(
                                    selected = settings.verseAlarmMode == VerseAlarmMode.SPEECH_DIRECT,
                                    onClick = { viewModel.updateVerseAlarmMode(VerseAlarmMode.SPEECH_DIRECT) },
                                    label = { Text("2. अलार्म व तुरंत वचन वाचन (Direct Speech)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                FilterChip(
                                    selected = settings.verseAlarmMode == VerseAlarmMode.MUSIC_THEN_SPEECH,
                                    onClick = { viewModel.updateVerseAlarmMode(VerseAlarmMode.MUSIC_THEN_SPEECH) },
                                    label = { Text("3. पहले संगीत/अलार्म, Stop के बाद वाचन (Music First)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // Sound Sync & Volume Controls
                            SettingsSwitchRow(
                                title = "अभिवादन की आवाज़ अलार्म के साथ सिंक रखें",
                                subtitle = "अभिवादन की आवाज़ अलार्म वॉल्यूम के बराबर रहेगी",
                                checked = settings.syncGreetingVolumeWithAlarm,
                                onCheckedChange = { viewModel.updateSyncGreetingVolumeWithAlarm(it) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "अलार्म ध्वनि वॉल्यूम: ${(settings.alarmVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = settings.alarmVolume,
                                onValueChange = { viewModel.updateAlarmVolume(it) },
                                valueRange = 0.1f..1.0f
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    requestNotificationPermissionIfNeeded {
                                        // Channel Initialization: before building notification
                                        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            val channel = android.app.NotificationChannel(
                                                "verse_channel_id",
                                                "Daily Verse",
                                                android.app.NotificationManager.IMPORTANCE_HIGH
                                            ).apply {
                                                description = "अलार्म एवं आज का वचन अनुस्मारक"
                                                enableVibration(true)
                                                enableLights(true)
                                            }
                                            notificationManager.createNotificationChannel(channel)
                                        }

                                        val launchIntent = Intent(context, com.example.MainActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            putExtra("open_tab", "BIBLE")
                                        }
                                        val pendingIntent = android.app.PendingIntent.getActivity(
                                            context,
                                            9024,
                                            launchIntent,
                                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
                                        )

                                        val testVerse = "विश्वास आशा की हुई वस्तुओं का निश्चय, और अनदेखी वस्तुओं का प्रमाण है। - इब्रानियों 11:1"
                                        val notif = androidx.core.app.NotificationCompat.Builder(context, "verse_channel_id")
                                            .setSmallIcon(com.example.R.mipmap.ic_launcher)
                                            .setContentTitle("📖 आज का वचन (Daily Verse)")
                                            .setContentText(testVerse)
                                            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(testVerse))
                                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                                            .setAutoCancel(true)
                                            .setContentIntent(pendingIntent)
                                            .addAction(android.R.drawable.ic_menu_view, "📖 बाइबल में पढ़ें", pendingIntent)
                                            .build()

                                        notificationManager.notify(9024, notif)
                                        Toast.makeText(context, "वचन नोटिफिकेशन भेजा गया! (Notification Sent)", Toast.LENGTH_SHORT).show()
                                        viewModel.testVerseAlarm()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🔔 अलार्म व नोटिफिकेशन टेस्ट करें (Test Flow)")
                            }
                        }
                    }
                }
            }

            // 0.8 Bible Reading Plan Reminders
            item {
                SettingsSectionHeader(title = "दैनिक बाइबल रीडिंग प्लान अनुस्मारक (READING PLAN)", icon = Icons.Default.EventNote)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsSwitchRow(
                            title = "दैनिक रीडिंग प्लान अनुस्मारक सक्षम करें",
                            subtitle = "प्रतिदिन निर्धारित समय पर बाइबल पाठ पूरा करने का रिमाइंडर पाएं",
                            checked = settings.readingPlanReminderEnabled,
                            onCheckedChange = { viewModel.updateReadingPlanReminderEnabled(it) }
                        )

                        if (settings.readingPlanReminderEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            val formattedReminderTime = String.format(
                                java.util.Locale.US,
                                "%02d:%02d %s",
                                if (settings.readingPlanReminderHour % 12 == 0) 12 else settings.readingPlanReminderHour % 12,
                                settings.readingPlanReminderMinute,
                                if (settings.readingPlanReminderHour >= 12) "PM" else "AM"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "रिमाइंडर समय (Daily Reminder Time)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "निर्धारित: $formattedReminderTime",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Button(
                                    onClick = { showReadingPlanTimePicker = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("समय बदलें")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "रीडिंग प्लान नोटिफिकेशन भेजा गया", Toast.LENGTH_SHORT).show()
                                    viewModel.testReadingPlanReminder()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🔔 टेस्ट नोटिफिकेशन (Test Reading Plan Reminder)")
                            }
                        }
                    }
                }
            }

            // 0.85 Daily Prayer & Motivational Verse Reminder Section
            item {
                SettingsSectionHeader(title = "दैनिक प्रार्थना व प्रेरक वचन अनुस्मारक (DAILY PRAYER REMINDER)", icon = Icons.Default.VolunteerActivism)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsSwitchRow(
                            title = "दैनिक प्रार्थना व प्रेरक वचन रिमाइंडर",
                            subtitle = "प्रतिदिन चुने हुए समय पर प्रेरक बाइबल वचन व प्रार्थना का नोटिफिकेशन पाएं",
                            checked = settings.dailyPrayerReminderEnabled,
                            onCheckedChange = { viewModel.updateDailyPrayerReminderEnabled(it) }
                        )

                        if (settings.dailyPrayerReminderEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Text(
                                text = "प्रार्थना का समय स्लॉट (Prayer Time Slot)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            com.example.data.model.DailyPrayerSlot.values().forEach { slot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateDailyPrayerReminderSlot(slot)
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = settings.dailyPrayerReminderSlot == slot,
                                        onClick = { viewModel.updateDailyPrayerReminderSlot(slot) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = slot.titleHindi,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (settings.dailyPrayerReminderSlot == slot) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                        Text(
                                            text = slot.titleEnglish,
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val formattedPrayerTime = String.format(
                                java.util.Locale.US,
                                "%02d:%02d %s",
                                if (settings.dailyPrayerReminderHour % 12 == 0) 12 else settings.dailyPrayerReminderHour % 12,
                                settings.dailyPrayerReminderMinute,
                                if (settings.dailyPrayerReminderHour >= 12) "PM" else "AM"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "सक्रिय समय (Current Set Time)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "समय: $formattedPrayerTime",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Button(
                                    onClick = { showDailyPrayerTimePicker = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("कस्टम समय बदलें")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "दैनिक प्रार्थना नोटिफिकेशन भेजा गया", Toast.LENGTH_SHORT).show()
                                    viewModel.testDailyPrayerReminder()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🙏 टेस्ट दैनिक प्रार्थना नोटिफिकेशन (Test Notification)")
                            }
                        }
                    }
                }
            }

            // 0.9 Home Screen Bible Verse Widget Section
            item {
                SettingsSectionHeader(title = "होम स्क्रीन वचन विजेट (HOME SCREEN WIDGET)", icon = Icons.Default.Widgets)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "आज का वचन विजेट (Daily Verse Widget)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "अपने फोन की होम स्क्रीन पर प्रतिदिन नया वचन देखें",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Widget Preview Mini Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "📖 आज का वचन • Verse of the Day",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"क्योंकि परमेश्वर ने जगत से ऐसा प्रेम रखा कि उसने अपना एकलौता पुत्र दे दिया...\"",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "— यूहन्ना 3:16",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Pin to Home Screen Action / Instructions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val appWidgetManager = AppWidgetManager.getInstance(context)
                                        val myProvider = ComponentName(context, BibleVerseWidgetProvider::class.java)
                                        if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                            appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                            Toast.makeText(context, "विजेट जोड़ने का अनुरोध भेजा गया!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "होम स्क्रीन पर खाली जगह दबाए रखें और 'Widgets' में से चुनें.", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "होम स्क्रीन पर खाली जगह दबाए रखें और 'Widgets' में से चुनें.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddHome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("होम स्क्रीन पर जोड़ें")
                            }

                            OutlinedButton(
                                onClick = {
                                    BibleVerseWidgetProvider.updateAllWidgets(context)
                                    Toast.makeText(context, "विजेट रीफ्रेश कर दिया गया है!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("रिफ्रेश")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 सुझाव: आप अपने फोन की होम स्क्रीन (Home Screen) पर खाली जगह पर लॉन्ग प्रेस (Long Press) करके भी 'Widgets' मेन्यू से इस ऐप का 'आज का वचन' विजेट चुनकर रख सकते हैं.",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            // 1. Language & Localization
            item {
                SettingsSectionHeader(title = strings.secLanguage, icon = Icons.Default.Language)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.appInterfaceLanguage,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.appLanguageDesc,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Switch Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.appLanguage == AppLanguage.ENGLISH,
                                onClick = {
                                    if (settings.appLanguage != AppLanguage.ENGLISH) {
                                        viewModel.updateAppLanguage(AppLanguage.ENGLISH)
                                        Toast.makeText(context, "Language switched to English", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text("English") },
                                leadingIcon = if (settings.appLanguage == AppLanguage.ENGLISH) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.appLanguage == AppLanguage.HINDI,
                                onClick = {
                                    if (settings.appLanguage != AppLanguage.HINDI) {
                                        viewModel.updateAppLanguage(AppLanguage.HINDI)
                                        Toast.makeText(context, "भाषा हिंदी में बदली गई", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text("हिंदी (Hindi)") },
                                leadingIcon = if (settings.appLanguage == AppLanguage.HINDI) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        FilterChip(
                            selected = settings.appLanguage == AppLanguage.SYSTEM,
                            onClick = {
                                if (settings.appLanguage != AppLanguage.SYSTEM) {
                                    viewModel.updateAppLanguage(AppLanguage.SYSTEM)
                                    Toast.makeText(context, strings.langSystem, Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { Text(strings.langSystem) },
                            leadingIcon = if (settings.appLanguage == AppLanguage.SYSTEM) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2. Personal Vlog Display Mode (Requirement 13)
            item {
                SettingsSectionHeader(title = "PERSONAL VLOG DISPLAY MODE", icon = Icons.Default.Person)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Choose how Personal Vlogs are displayed in the app:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        PersonalVlogMode.entries.forEach { mode ->
                            val isSelected = settings.personalVlogMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updatePersonalVlogMode(mode) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updatePersonalVlogMode(mode) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = when (mode) {
                                            PersonalVlogMode.HIDDEN -> "Hidden (Default - Fellowship Events only)"
                                            PersonalVlogMode.SECONDARY -> "Secondary Section (Blogs sub-tab)"
                                            PersonalVlogMode.HOME_AND_SECONDARY -> "Home + Secondary Section"
                                            PersonalVlogMode.PRIORITY_OVERRIDE -> "Priority / Override"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. YouTube Default Tab (Requirement 17)
            item {
                SettingsSectionHeader(title = "डिफ़ॉल्ट यूट्यूब चैनल / टैब (DEFAULT YOUTUBE TAB)", icon = Icons.Default.PlayCircle)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "ऐप में YouTube स्क्रीन खोलते समय कौन सा चैनल पहले दिखेगा:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.youtubeDefaultTab == YouTubeDefaultTab.ALL,
                                onClick = { viewModel.updateYouTubeDefaultTab(YouTubeDefaultTab.ALL) },
                                label = { Text("All Channels") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.youtubeDefaultTab == YouTubeDefaultTab.AVJ_WORSHIP,
                                onClick = { viewModel.updateYouTubeDefaultTab(YouTubeDefaultTab.AVJ_WORSHIP) },
                                label = { Text("Worship") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.youtubeDefaultTab == YouTubeDefaultTab.VINAY_KUMAR_AVJ,
                                onClick = { viewModel.updateYouTubeDefaultTab(YouTubeDefaultTab.VINAY_KUMAR_AVJ) },
                                label = { Text("Vinay Kumar AVJ") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.youtubeDefaultTab == YouTubeDefaultTab.NEW_CREATION_CHURCH,
                                onClick = { viewModel.updateYouTubeDefaultTab(YouTubeDefaultTab.NEW_CREATION_CHURCH) },
                                label = { Text("New Creation Church") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Bible Reading Style (Requirement 21)
            item {
                SettingsSectionHeader(title = "BIBLE READING STYLE", icon = Icons.Default.MenuBook)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.bibleReadingStyle == BibleReadingStyle.PRINTED_BIBLE,
                                onClick = { viewModel.updateBibleReadingStyle(BibleReadingStyle.PRINTED_BIBLE) },
                                label = { Text("Printed Bible (Default)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.bibleReadingStyle == BibleReadingStyle.PARAGRAPH,
                                onClick = { viewModel.updateBibleReadingStyle(BibleReadingStyle.PARAGRAPH) },
                                label = { Text("Paragraph Style") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }



            // 6. Blogger Photo Grid Layout Customization (User Request 3)
            item {
                SettingsSectionHeader(title = "ब्लॉगर फ़ोटो लेआउट (BLOG PHOTO GRID)", icon = Icons.Default.GridView)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "आर्टिकल में फ़ोटो प्रदर्शन (Photos in Line)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ब्लॉग आर्टिकल में फ़ोटो को एक लाइन में ग्रिड (1, 2, 3, 4) के अनुसार दिखाएं ताकि कम स्पेस में ज्यादा फ़ोटो दिख सकें:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.bloggerPhotoLayout == BloggerPhotoLayout.SINGLE,
                                onClick = { viewModel.updateBloggerPhotoLayout(BloggerPhotoLayout.SINGLE) },
                                label = { Text("1 (Single)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.bloggerPhotoLayout == BloggerPhotoLayout.GRID_2,
                                onClick = { viewModel.updateBloggerPhotoLayout(BloggerPhotoLayout.GRID_2) },
                                label = { Text("2 (Grid 2)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.bloggerPhotoLayout == BloggerPhotoLayout.GRID_3,
                                onClick = { viewModel.updateBloggerPhotoLayout(BloggerPhotoLayout.GRID_3) },
                                label = { Text("3 (Grid 3)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.bloggerPhotoLayout == BloggerPhotoLayout.GRID_4,
                                onClick = { viewModel.updateBloggerPhotoLayout(BloggerPhotoLayout.GRID_4) },
                                label = { Text("4 (Grid 4)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 7. Data Saver Mode (Requirement 28)
            item {
                SettingsSectionHeader(title = "DATA USAGE & MEDIA", icon = Icons.Default.DataUsage)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsSwitchRow(
                            title = "Data Saver Mode",
                            subtitle = "Reduce mobile data by loading compressed thumbnails and on-demand full resolution images.",
                            checked = settings.dataSaverEnabled,
                            onCheckedChange = { viewModel.updateDataSaver(it) }
                        )
                    }
                }
            }

            // 6. Appearance & Theme
            item {
                SettingsSectionHeader(title = strings.secAppearance, icon = Icons.Default.Brightness4)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = strings.appTheme,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = settings.themeMode == ThemeMode.SYSTEM,
                                onClick = {
                                    viewModel.updateThemeMode(ThemeMode.SYSTEM)
                                    Toast.makeText(context, "${strings.themeSystem} लागू किया गया", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(strings.themeSystem) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.themeMode == ThemeMode.LIGHT,
                                onClick = {
                                    viewModel.updateThemeMode(ThemeMode.LIGHT)
                                    Toast.makeText(context, "${strings.themeLight} लागू किया गया", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(strings.themeLight) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = settings.themeMode == ThemeMode.DARK,
                                onClick = {
                                    viewModel.updateThemeMode(ThemeMode.DARK)
                                    Toast.makeText(context, "${strings.themeDark} लागू किया गया", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(strings.themeDark) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        FilterChip(
                            selected = settings.themeMode == ThemeMode.DYNAMIC,
                            onClick = {
                                viewModel.updateThemeMode(ThemeMode.DYNAMIC)
                                Toast.makeText(context, "Dynamic Colors (Material You / Wallpaper Theme) लागू किया गया!", Toast.LENGTH_SHORT).show()
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Dynamic Colors (Material You) / Wallpaper Theme")
                                }
                            },
                            leadingIcon = if (settings.themeMode == ThemeMode.DYNAMIC) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Sidebar (Navigation Drawer) controls
                        Text(
                            text = "नेविगेशन साइडबार (Navigation Sidebar)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        SettingsSwitchRow(
                            title = "साइडबार सक्षम करें (Enable Sidebar)",
                            subtitle = "साइडबार मेनू को स्वाइप या हेडर आइकन से खोलें",
                            checked = settings.isDrawerEnabled,
                            onCheckedChange = { viewModel.updateIsDrawerEnabled(it) }
                        )

                        if (settings.isDrawerEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "साइडबार स्थिति (Sidebar Position):",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = settings.drawerPosition == "left",
                                    onClick = { viewModel.updateDrawerPosition("left") },
                                    label = { Text("Left (बाईं ओर)") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = settings.drawerPosition == "right",
                                    onClick = { viewModel.updateDrawerPosition("right") },
                                    label = { Text("Right (दाईं ओर)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 7. Home Screen Customization & Categories
            item {
                SettingsSectionHeader(title = strings.secFeedCustomization, icon = Icons.Default.Tune)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = strings.customizeHomeSections,
                            subtitle = strings.customizeHomeSectionsSub,
                            icon = Icons.Default.ViewAgenda,
                            onClick = onCustomizeHomeClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = "${strings.favoriteCategories} (${settings.favoriteCategories.size})",
                            subtitle = strings.favoriteCategoriesSub,
                            icon = Icons.Default.Favorite,
                            onClick = { showFavCategoriesDialog = true }
                        )
                    }
                }
            }

            // 8. Notifications
            item {
                SettingsSectionHeader(title = strings.secNotifications, icon = Icons.Default.Notifications)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsSwitchRow(
                            title = strings.eventReminders,
                            subtitle = strings.eventRemindersSub,
                            checked = settings.notifyUpcomingReminders,
                            onCheckedChange = { viewModel.updateNotifyUpcomingReminders(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsSwitchRow(
                            title = strings.fellowshipAnnounce,
                            subtitle = strings.fellowshipAnnounceSub,
                            checked = settings.notifyFellowshipEvents,
                            onCheckedChange = { viewModel.updateNotifyFellowshipEvents(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsSwitchRow(
                            title = strings.newYouTubeVideos,
                            subtitle = strings.newYouTubeVideosSub,
                            checked = settings.notifyYouTube,
                            onCheckedChange = { viewModel.updateNotifyYouTube(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsSwitchRow(
                            title = strings.personalVlogNotify,
                            subtitle = strings.personalVlogNotifySub,
                            checked = settings.notifyPersonalVlog,
                            onCheckedChange = { viewModel.updateNotifyPersonalVlog(it) }
                        )
                    }
                }
            }

            // 9. Sync & Backup Management
            item {
                SettingsSectionHeader(title = "SYNC & BACKUP TOOLS", icon = Icons.Default.CloudSync)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = "Sync Center",
                            subtitle = "Check sync timestamps and refresh all feeds",
                            icon = Icons.Default.Sync,
                            onClick = onSyncCenterClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = "Backup & Restore",
                            subtitle = "Export or restore study notes, bookmarks & lyrics",
                            icon = Icons.Default.CloudUpload,
                            onClick = onBackupRestoreClick
                        )
                    }
                }
            }

            // 10. Other & Storage
            item {
                SettingsSectionHeader(title = strings.secOtherStorage, icon = Icons.Default.Info)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsClickableRow(
                            title = strings.shareApp,
                            subtitle = strings.shareAppSub,
                            icon = Icons.Default.Share,
                            onClick = shareApp
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = strings.aboutVinay,
                            subtitle = strings.aboutVinaySub,
                            icon = Icons.Default.Info,
                            onClick = onAboutClick
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        SettingsClickableRow(
                            title = strings.clearCache,
                            subtitle = strings.clearCacheSub,
                            icon = Icons.Default.Delete,
                            onClick = {
                                viewModel.clearCache()
                                Toast.makeText(context, strings.cacheClearedToast, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }


        }
    }

    if (showPasswordDialogForProfileB) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialogForProfileB = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "निजी प्रोफ़ाइल अनलॉक करें",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "'Vinay Kumar Avj' प्रोफ़ाइल में स्विच करने के लिए पासवर्ड दर्ज करें:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = profilePasswordInput,
                        onValueChange = {
                            profilePasswordInput = it
                        },
                        label = { Text("पासवर्ड (Password)") },
                        singleLine = true,
                        visualTransformation = if (profilePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { profilePasswordVisible = !profilePasswordVisible }) {
                                Icon(
                                    imageVector = if (profilePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (profilePasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ProfileManager.verifyPasswordForPrivateProfile(profilePasswordInput)) {
                            ProfileManager.setActiveProfile(context, AppProfile.VINAY)
                            Toast.makeText(context, "प्रोफ़ाइल 'Vinay Kumar Avj' सक्रिय किया गया", Toast.LENGTH_SHORT).show()
                            showPasswordDialogForProfileB = false
                            profilePasswordInput = ""
                            (context as? android.app.Activity)?.let {
                                ProfileManager.restartApp(it)
                            }
                        } else {
                            // गलत पासवर्ड डालने से कोई प्रतिक्रिया नहीं होना चाहिए (Zero reaction on wrong password)
                        }
                    }
                ) {
                    Text("अनलॉक एवं स्विच करें")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialogForProfileB = false
                    profilePasswordInput = ""
                }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    if (showFavCategoriesDialog) {
        FavoriteCategoriesDialog(
            availableCategories = fellowshipCategories,
            favoriteCategories = settings.favoriteCategories,
            onToggleCategory = { viewModel.toggleFavoriteCategory(it) },
            onDismiss = { showFavCategoriesDialog = false }
        )
    }

    if (showWelcomeCustomizationDialog) {
        com.example.ui.components.WelcomeCustomizationDialog(
            viewModel = viewModel,
            onDismiss = { showWelcomeCustomizationDialog = false }
        )
    }

    if (showVerseAlarmTimePicker) {
        com.example.ui.components.SimpleTimePickerDialog(
            title = "आज का वचन अलार्म समय चुनें",
            initialHour = settings.verseAlarmHour,
            initialMinute = settings.verseAlarmMinute,
            onTimeSelected = { hour, minute ->
                viewModel.updateVerseAlarmTime(hour, minute)
            },
            onDismiss = { showVerseAlarmTimePicker = false }
        )
    }

    if (showReadingPlanTimePicker) {
        com.example.ui.components.SimpleTimePickerDialog(
            title = "रीडिंग प्लान रिमाइंडर समय चुनें",
            initialHour = settings.readingPlanReminderHour,
            initialMinute = settings.readingPlanReminderMinute,
            onTimeSelected = { hour, minute ->
                viewModel.updateReadingPlanReminderTime(hour, minute)
            },
            onDismiss = { showReadingPlanTimePicker = false }
        )
    }

    if (showDailyPrayerTimePicker) {
        com.example.ui.components.SimpleTimePickerDialog(
            title = "दैनिक प्रार्थना रिमाइंडर समय चुनें",
            initialHour = settings.dailyPrayerReminderHour,
            initialMinute = settings.dailyPrayerReminderMinute,
            onTimeSelected = { hour, minute ->
                viewModel.updateDailyPrayerReminderTime(hour, minute)
            },
            onDismiss = { showDailyPrayerTimePicker = false }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun AppUpdateSection(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.AppUpdateSection(viewModel = viewModel, modifier = modifier)
}
