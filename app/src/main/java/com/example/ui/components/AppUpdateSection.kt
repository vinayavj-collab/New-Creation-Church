package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.SettingsSectionHeader
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AppUpdateSection(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val updateState by viewModel.updateState.collectAsState()
    var dismissedLater by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionHeader(title = "APP UPDATE (ऐप अपडेट)", icon = Icons.Default.SystemUpdate)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Info Grid: Current Version | Latest Version
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current Version",
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "v${updateState.currentVersionName} (Build ${updateState.currentVersionCode})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Latest Version",
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = when {
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

                Spacer(modifier = Modifier.height(10.dp))

                // Last Checked
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Last Checked: ",
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = viewModel.appUpdateManager.formatLastCheckedTime(updateState.lastCheckedTimestamp),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Banner
                val statusContainerColor = when {
                    updateState.isUpdateAvailable -> MaterialTheme.colorScheme.primaryContainer
                    updateState.errorMessage != null -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                val statusTextColor = when {
                    updateState.isUpdateAvailable -> MaterialTheme.colorScheme.onPrimaryContainer
                    updateState.errorMessage != null -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusContainerColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    updateState.isUpdateAvailable -> Icons.Default.SystemUpdate
                                    updateState.errorMessage != null -> Icons.Default.Warning
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = statusTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = updateState.statusMessage,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor
                                )
                            )
                        }

                        if (updateState.errorMessage != null && updateState.errorMessage != updateState.statusMessage) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = updateState.errorMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = statusTextColor)
                            )
                        }
                    }
                }

                // Release Notes (If available)
                if (updateState.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Release Notes (${updateState.releaseTitle}):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = updateState.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Downloading Progress Bar
                if (updateState.isDownloading) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "APK डाउनलोड हो रहा है... ${updateState.downloadProgressPercentage}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { updateState.downloadProgressPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // [Check for Updates] Button
                    OutlinedButton(
                        onClick = {
                            dismissedLater = false
                            viewModel.checkForAppUpdates(force = true)
                        },
                        enabled = !updateState.isChecking && !updateState.isDownloading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (updateState.isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("जांच रहे हैं...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check for Updates")
                        }
                    }

                    // If Update Available: [Install APK Now] or [Update Now]
                    if (updateState.isUpdateAvailable) {
                        if (updateState.downloadedApkFile != null && updateState.downloadedApkFile!!.exists()) {
                            Button(
                                onClick = {
                                    viewModel.installDownloadedApk()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Install APK Now")
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.downloadAndInstallAppUpdate()
                                },
                                enabled = !updateState.isDownloading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Update Now")
                            }
                        }
                    }
                }

                // If Update Available and not dismissed, show Later option button
                if (updateState.isUpdateAvailable && !dismissedLater) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { dismissedLater = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Later (बाद में)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
