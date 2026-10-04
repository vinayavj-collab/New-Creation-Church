package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.repository.BackupMetadata
import com.example.data.bible.repository.BackupRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BackupRestoreScreen(
    backupRepository: BackupRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var syncId by remember { mutableStateOf(backupRepository.getSyncId()) }
    var lastCloudBackupTime by remember { mutableLongStateOf(backupRepository.getLastCloudBackupTime()) }
    var lastCloudRestoreTime by remember { mutableLongStateOf(backupRepository.getLastCloudRestoreTime()) }

    var isCloudBackingUp by remember { mutableStateOf(false) }
    var isCloudRestoring by remember { mutableStateOf(false) }
    var isExportingFile by remember { mutableStateOf(false) }
    var isRestoringFile by remember { mutableStateOf(false) }

    var showEditSyncIdDialog by remember { mutableStateOf(false) }
    var editSyncIdInput by remember { mutableStateOf(syncId) }

    var showCloudRestoreConfirmDialog by remember { mutableStateOf(false) }
    var cloudBackupMetadata by remember { mutableStateOf<BackupMetadata?>(null) }
    var isCheckingCloudBackup by remember { mutableStateOf(false) }

    var showManualJsonDialog by remember { mutableStateOf(false) }
    var manualJsonInput by remember { mutableStateOf("") }
    var isManualRestoring by remember { mutableStateOf(false) }

    // File picker launcher for .json backup files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isRestoringFile = true
                val result = backupRepository.restoreFromFileUri(uri)
                isRestoringFile = false
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    Toast.makeText(context, "✅ $count आइटम सफलतापूर्वक रिस्टोर किए गए!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "❌ रिस्टोर त्रुटि: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val handleCloudBackup = {
        coroutineScope.launch {
            isCloudBackingUp = true
            val result = backupRepository.backupToFirebaseCloud()
            isCloudBackingUp = false
            if (result.isSuccess) {
                val meta = result.getOrNull()
                lastCloudBackupTime = backupRepository.getLastCloudBackupTime()
                Toast.makeText(
                    context,
                    "☁️ क्लाउड बैकअप सफल! (${meta?.totalItems ?: 0} आइटम सुरक्षित)",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "❌ क्लाउड बैकअप विफल: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val checkAndOpenCloudRestoreDialog = {
        coroutineScope.launch {
            isCheckingCloudBackup = true
            val result = backupRepository.checkCloudBackupAvailable()
            isCheckingCloudBackup = false
            if (result.isSuccess) {
                val meta = result.getOrNull()
                if (meta != null) {
                    cloudBackupMetadata = meta
                    showCloudRestoreConfirmDialog = true
                } else {
                    Toast.makeText(context, "इस सिंक आईडी पर क्लाउड में कोई बैकअप नहीं मिला।", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "जाँच विफल: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val handleCloudRestore = {
        coroutineScope.launch {
            isCloudRestoring = true
            val result = backupRepository.restoreFromFirebaseCloud()
            isCloudRestoring = false
            showCloudRestoreConfirmDialog = false
            if (result.isSuccess) {
                lastCloudRestoreTime = backupRepository.getLastCloudRestoreTime()
                val count = result.getOrNull() ?: 0
                Toast.makeText(context, "✅ क्लाउड से $count आइटम सफलतापूर्वक रिस्टोर हुए!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "❌ क्लाउड रिस्टोर त्रुटि: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val handleShareBackupFile = {
        coroutineScope.launch {
            isExportingFile = true
            try {
                val file = backupRepository.createShareableBackupFile()
                val uri = backupRepository.getFileUri(file)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Bible Reading & Study Backup")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(sendIntent, "बैकअप फ़ाइल Google Drive या WhatsApp पर सेव करें"))
                Toast.makeText(context, "बैकअप फ़ाइल तैयार!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "फ़ाइल बनाने में त्रुटि: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isExportingFile = false
            }
        }
    }

    val copyBackupJson = {
        coroutineScope.launch {
            try {
                val json = backupRepository.createBackupJson()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("BibleStudyBackup", json)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "बैकअप JSON क्लिपबोर्ड पर कॉपी हो गया!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("बैकअप एवं रिस्टोर (Backup & Restore)", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "डेटा सुरक्षा एवं निरंतरता",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "ऐप अपडेट या फोन बदलने पर अपना डेटा कभी न खोएं",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Included items tags
                        Text(
                            text = "बैकअप में शामिल डेटा:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BackupItemChip(label = "📖 रीडिंग प्लान प्रोग्रेस", icon = Icons.Default.CalendarMonth)
                            BackupItemChip(label = "🔖 बुकमार्क्स", icon = Icons.Default.Bookmark)
                            BackupItemChip(label = "🎨 हाइलाइट्स", icon = Icons.Default.Brush)
                            BackupItemChip(label = "📝 वचन नोट्स", icon = Icons.Default.EditNote)
                            BackupItemChip(label = "✍️ स्टडी नोट्स", icon = Icons.Default.Description)
                            BackupItemChip(label = "📌 अंतिम पढ़ा गया स्थान", icon = Icons.Default.Place)
                        }
                    }
                }
            }

            // SECTION 1: FIREBASE CLOUD BACKUP & RESTORE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "1. Firebase क्लाउड बैकअप",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "एक टैप में ऑनलाइन बैकअप व रिस्टोर",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sync ID display card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "आपकी क्लाउड सिंक आईडी (Sync ID):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = syncId,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("SyncID", syncId))
                                            Toast.makeText(context, "सिंक आईडी कॉपी हो गई!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Sync ID", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            editSyncIdInput = syncId
                                            showEditSyncIdDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Change Sync ID", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        if (lastCloudBackupTime > 0L) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastCloudBackupTime))
                            Text(
                                text = "🕒 अंतिम क्लाउड बैकअप: $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { handleCloudBackup() },
                                enabled = !isCloudBackingUp && !isCloudRestoring,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isCloudBackingUp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("अपलोड...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("क्लाउड बैकअप", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            FilledTonalButton(
                                onClick = { checkAndOpenCloudRestoreDialog() },
                                enabled = !isCloudBackingUp && !isCloudRestoring && !isCheckingCloudBackup,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isCheckingCloudBackup || isCloudRestoring) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("लोडिंग...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("क्लाउड रिस्टोर", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: FILE & DRIVE BACKUP / RESTORE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "2. फ़ाइल बैकअप (Drive / Offline File)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "फ़ाइल को Google Drive, WhatsApp या स्टोरेज में रखें",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { handleShareBackupFile() },
                                enabled = !isExportingFile,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isExportingFile) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("फ़ाइल एक्सपोर्ट", fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = { filePickerLauncher.launch("application/json") },
                                enabled = !isRestoringFile,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isRestoringFile) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                } else {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("फ़ाइल रिस्टोर", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: MANUAL JSON BACKUP & RESTORE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "3. मैन्युअल JSON टेक्स्ट",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { copyBackupJson() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("JSON कॉपी करें", fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = { showManualJsonDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("JSON पेस्ट रिस्टोर", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // DIALOG 1: Edit Sync ID
        if (showEditSyncIdDialog) {
            AlertDialog(
                onDismissRequest = { showEditSyncIdDialog = false },
                title = { Text("क्लाउड सिंक आईडी बदलें", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "यदि आपने ऐप री-इंस्टॉल किया है या दूसरे फोन से अपना डेटा रिस्टोर करना चाहते हैं, तो अपनी पुरानी सिंक आईडी यहाँ दर्ज करें:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = editSyncIdInput,
                            onValueChange = { editSyncIdInput = it },
                            label = { Text("Sync ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editSyncIdInput.isNotBlank()) {
                                backupRepository.setSyncId(editSyncIdInput.trim())
                                syncId = backupRepository.getSyncId()
                                showEditSyncIdDialog = false
                                Toast.makeText(context, "सिंक आईडी अपडेट हो गई!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("सहेजें")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                backupRepository.resetSyncIdToDeviceDefault()
                                syncId = backupRepository.getSyncId()
                                editSyncIdInput = syncId
                                showEditSyncIdDialog = false
                                Toast.makeText(context, "डिफ़ॉल्ट डिवाइस आईडी पर रीसेट!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("डिफ़ॉल्ट रीसेट")
                        }
                        TextButton(onClick = { showEditSyncIdDialog = false }) {
                            Text("रद्द करें")
                        }
                    }
                }
            )
        }

        // DIALOG 2: Confirm Cloud Restore
        if (showCloudRestoreConfirmDialog && cloudBackupMetadata != null) {
            val meta = cloudBackupMetadata!!
            val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(meta.timestamp))
            AlertDialog(
                onDismissRequest = { showCloudRestoreConfirmDialog = false },
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("क्लाउड बैकअप रिस्टोर करें?", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "क्लाउड पर मिला बैकअप विवरण:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📅 दिनांक: $dateFormatted", fontSize = 12.sp)
                                Text("📖 रीडिंग प्लान प्रोग्रेस: ${meta.readingPlanProgressCount} दिन", fontSize = 12.sp)
                                Text("🔖 बुकमार्क्स: ${meta.bookmarkCount}", fontSize = 12.sp)
                                Text("🎨 हाइलाइट्स: ${meta.highlightCount}", fontSize = 12.sp)
                                Text("📝 वचन नोट्स: ${meta.noteCount}", fontSize = 12.sp)
                                Text("✍️ स्टडी नोट्स: ${meta.studyNoteCount}", fontSize = 12.sp)
                                Text("कुल आइटम: ${meta.totalItems}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(
                            text = "यह डेटा आपके वर्तमान डेटा के साथ सुरक्षित रूप से मर्ज (Merge) हो जाएगा।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { handleCloudRestore() },
                        enabled = !isCloudRestoring
                    ) {
                        Text("हाँ, रिस्टोर करें")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCloudRestoreConfirmDialog = false }) {
                        Text("रद्द करें")
                    }
                }
            )
        }

        // DIALOG 3: Manual JSON Paste
        if (showManualJsonDialog) {
            AlertDialog(
                onDismissRequest = { showManualJsonDialog = false },
                title = { Text("JSON टेक्स्ट से रिस्टोर", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "अपना बैकअप JSON नीचे पेस्ट करें:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = manualJsonInput,
                            onValueChange = { manualJsonInput = it },
                            placeholder = { Text("{\"version\":\"1.3\", ...}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            maxLines = 10
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (manualJsonInput.isNotBlank()) {
                                coroutineScope.launch {
                                    isManualRestoring = true
                                    val result = backupRepository.restoreFromJson(manualJsonInput.trim(), mergeMode = true)
                                    isManualRestoring = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "✅ ${result.getOrNull()} आइटम रिस्टोर किए गए!", Toast.LENGTH_LONG).show()
                                        showManualJsonDialog = false
                                        manualJsonInput = ""
                                    } else {
                                        Toast.makeText(context, "❌ अमान्य बैकअप JSON: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isManualRestoring
                    ) {
                        Text("रिस्टोर करें")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualJsonDialog = false }) {
                        Text("रद्द करें")
                    }
                }
            )
        }
    }
}

@Composable
private fun BackupItemChip(
    label: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
