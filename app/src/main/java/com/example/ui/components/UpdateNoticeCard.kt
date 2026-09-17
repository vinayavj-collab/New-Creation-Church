package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.MainViewModel

@Composable
fun UpdateNoticeCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val updateState by viewModel.updateState.collectAsState()
    var dismissedInSession by rememberSaveable { mutableStateOf(false) }

    if (updateState.isUpdateAvailable && !dismissedInSession) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🆕 नया App Update उपलब्ध है",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (updateState.latestVersionName.isNotBlank()) {
                            Text(
                                text = "नवीनतम संस्करण: v${updateState.latestVersionName} (Build ${updateState.latestVersionCode})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }

                if (updateState.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = updateState.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3
                    )
                }

                if (updateState.isDownloading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "APK डाउनलोड हो रहा है... ${updateState.downloadProgressPercentage}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { updateState.downloadProgressPercentage / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (updateState.downloadedApkFile != null && updateState.downloadedApkFile!!.exists()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✅ डाउनलोड पूर्ण। अभी 'Install APK Now' दबाकर इंस्टॉल करें।",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { dismissedInSession = true },
                        enabled = !updateState.isDownloading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("बाद में (Later)")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (updateState.downloadedApkFile != null && updateState.downloadedApkFile!!.exists()) {
                        Button(
                            onClick = { viewModel.installDownloadedApk() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Install APK Now")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.downloadAndInstallAppUpdate() },
                            enabled = !updateState.isDownloading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (updateState.isDownloading) "डाउनलोड हो रहा है..." else "अभी अपडेट करें (Update Now)")
                        }
                    }
                }
            }
        }
    }
}
