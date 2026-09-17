package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AppUpdateModalDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val updateState by viewModel.updateState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.speakUpdateAnnouncement()
    }

    AlertDialog(
        onDismissRequest = {
            if (!updateState.isDownloading) {
                onDismissRequest()
            }
        },
        icon = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (updateState.downloadedApkFile != null && updateState.downloadedApkFile!!.exists()) Icons.Default.CheckCircle else Icons.Default.SystemUpdate,
                        contentDescription = "App Update",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = when {
                    updateState.isChecking -> "जांच की जा रही है... (Checking)"
                    updateState.downloadedApkFile != null && updateState.downloadedApkFile!!.exists() -> "✅ अपडेट इंस्टॉल के लिए तैयार है"
                    updateState.isUpdateAvailable -> "🆕 नया App Update उपलब्ध है"
                    else -> "ऐप अपडेट (App Update)"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // Version Info Card (Current Version & Latest Version)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "वर्तमान वर्शन (Current):",
                                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "v${updateState.currentVersionName} (Build ${updateState.currentVersionCode})",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "नवीनतम वर्शन (Latest):",
                                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = when {
                                        updateState.isChecking -> "जांच हो रही है..."
                                        updateState.latestVersionCode > 0 -> "v${updateState.latestVersionName} (Build ${updateState.latestVersionCode})"
                                        updateState.latestVersionName.isNotBlank() -> "v${updateState.latestVersionName}"
                                        else -> "v${updateState.currentVersionName} (अप-टू-डेट)"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (updateState.isUpdateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // What's New Section (क्या नया है)
                Text(
                    text = "✨ क्या नया है (What's New):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val notesText = when {
                        updateState.releaseNotes.isNotBlank() -> updateState.releaseNotes
                        updateState.releaseTitle.isNotBlank() -> updateState.releaseTitle
                        updateState.isUpdateAvailable -> "• नवीनतम सुरक्षा और प्रदर्शन सुधार\n• नए फीचर्स और यूजर इंटरफेस अपडेट\n• ऐप की गति और स्थिरता में सुधार"
                        else -> "• आपका ऐप नवीनतम संस्करण पर चल रहा है।\n• सभी नवीनतम फीचर्स और सुरक्षा फिक्स शामिल हैं।"
                    }
                    Text(
                        text = notesText,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Downloading Progress Bar
                if (updateState.isDownloading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "APK डाउनलोड हो रहा है... ${updateState.downloadProgressPercentage}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { updateState.downloadProgressPercentage / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                }

                // Downloaded APK Banner
                if (updateState.downloadedApkFile != null && updateState.downloadedApkFile!!.exists()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "APK डाउनलोड पूरा हो गया है! नीचे 'इंस्टॉल करें' बटन दबाएं।",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Error Banner
                if (updateState.errorMessage != null && !updateState.isDownloading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = updateState.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (updateState.downloadedApkFile != null && updateState.downloadedApkFile!!.exists()) {
                    Button(
                        onClick = { viewModel.installDownloadedApk() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("इंस्टॉल करें (Install APK Now)")
                    }
                } else if (updateState.isUpdateAvailable) {
                    Button(
                        onClick = { viewModel.downloadAndInstallAppUpdate() },
                        enabled = !updateState.isDownloading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (updateState.isDownloading) "डाउनलोड हो रहा है..." else "अभी अपडेट करें (Update Now)")
                    }
                } else {
                    Button(
                        onClick = { viewModel.checkForAppUpdates(force = true) },
                        enabled = !updateState.isChecking,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (updateState.isChecking) "जांच हो रही है..." else "पुनः जांचें (Check Again)")
                    }
                }
            }
        },
        dismissButton = {
            if (!updateState.isDownloading) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("बाद में (Later)")
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

