package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.repository.BackupRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    backupRepository: BackupRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isBackingUp by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var isRestoring by remember { mutableStateOf(false) }

    val handleExportBackup = {
        coroutineScope.launch {
            isBackingUp = true
            try {
                val json = backupRepository.createBackupJson()
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_SUBJECT, "Vinay Kumar AVJ App Backup")
                    putExtra(Intent.EXTRA_TEXT, json)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Export Backup to Drive or Files"))
                Toast.makeText(context, "Backup generated successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to create backup: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isBackingUp = false
            }
        }
    }

    val copyBackupJson = {
        coroutineScope.launch {
            try {
                val json = backupRepository.createBackupJson()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("VinayAppBackup", json)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Export & Save Backup",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Safely export your Bible bookmarks, highlights, verse notes, dedicated personal notes, reading plan progress, and Christian song lyrics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { handleExportBackup() },
                                enabled = !isBackingUp,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isBackingUp) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export / Drive")
                                }
                            }

                            OutlinedButton(
                                onClick = { copyBackupJson() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy JSON")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Restore from Backup",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Paste a previously saved backup JSON to restore your personal study data, notes, and custom lyrics without losing anything.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showRestoreDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RESTORE BACKUP DATA", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Restore Backup Data", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Paste your backup JSON below to restore items:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = restoreJsonInput,
                            onValueChange = { restoreJsonInput = it },
                            placeholder = { Text("{\"version\":\"1.2\", ...}") },
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
                            if (restoreJsonInput.isNotBlank()) {
                                coroutineScope.launch {
                                    isRestoring = true
                                    val result = backupRepository.restoreFromJson(restoreJsonInput.trim(), mergeMode = true)
                                    isRestoring = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Restored ${result.getOrNull()} items successfully!", Toast.LENGTH_LONG).show()
                                        showRestoreDialog = false
                                        restoreJsonInput = ""
                                    } else {
                                        Toast.makeText(context, "Invalid backup format: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isRestoring
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
