package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AppUpdateModalDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val updateState by viewModel.updateState.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }
    var isInstructionsReadChecked by remember { mutableStateOf(false) }

    // Help Dialog with uninstall & website download instructions
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Help",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "सहायता एवं निर्देश (Help)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "यदि यहां से अपडेट नहीं हो रहा है तो वेबसाइट से डाउनलोड कर इंस्टॉल कर सकते हैं, यदि फिर भी इंस्टॉल नहीं होता है तो वर्तमान ऐप को अनइंस्टॉल करके उस नए ऐप को इंस्टॉल करें।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("समझ गया (Got It)")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        updateState.isChecking -> "जांच की जा रही है..."
                        updateState.downloadedApkFile != null && updateState.downloadedApkFile!!.exists() -> "✅ इंस्टॉल के लिए तैयार"
                        updateState.isUpdateAvailable -> "🆕 नया App Update उपलब्ध"
                        else -> "ऐप अपडेट (App Update)"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                // Help Button
                IconButton(
                    onClick = { showHelpDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
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
                                    text = "New Creation Church v${updateState.currentVersionName} (Build ${updateState.currentVersionCode})",
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
                                        updateState.latestVersionCode > 0 -> "New Creation Church v${updateState.latestVersionName} (Build ${updateState.latestVersionCode})"
                                        updateState.latestVersionName.isNotBlank() -> "New Creation Church v${updateState.latestVersionName}"
                                        else -> "New Creation Church v${updateState.currentVersionName} (अप-टू-डेट)"
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

                // Guidance & Instructions Card
                if (updateState.isUpdateAvailable || (updateState.downloadedApkFile != null && updateState.downloadedApkFile!!.exists())) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "अपडेट दिशा-निर्देश (Conflict Guidance):",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val targetApkName = updateState.apkFileName ?: "NCCK_V${updateState.latestVersionCode.takeIf { it > 0 } ?: updateState.currentVersionCode}.apk"
                            Text(
                                text = "• फ़ाइल लोकेशन: Downloads / $targetApkName\n• यदि 'Package Conflicts' एरर आये, तो नीचे बटन दबाकर पुरानी ऐप अनइंस्टॉल करें, फिर फ़ाइल मैनेजर (Downloads) से नया APK इंस्टॉल करें।\n• ऑटो क्लीनअप: इंस्टॉल होने के बाद जब नया ऐप स्टार्ट होगा, तो अस्थायी APK फाइल अपने आप डिलीट हो जाएगी।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("अनइंस्टॉल पेज खोलें (App Settings)")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Checkbox requirement
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isInstructionsReadChecked = !isInstructionsReadChecked }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isInstructionsReadChecked,
                                onCheckedChange = { isInstructionsReadChecked = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "मैंने ऊपर दिए गए निर्देश पढ़ लिए हैं",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInstructionsReadChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
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
                        enabled = isInstructionsReadChecked,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("इंस्टॉल करें (Install APK Now)")
                    }
                } else if (updateState.isUpdateAvailable) {
                    val directDownloadUrl = updateState.apkDownloadUrl 
                        ?: "https://github.com/${updateState.repoPath}/releases/latest"

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(directDownloadUrl)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                viewModel.downloadAndInstallAppUpdate()
                            }
                        },
                        enabled = isInstructionsReadChecked && !updateState.isDownloading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (updateState.isDownloading) "डाउनलोड हो रहा है..." else "डाउनलोड करें (Direct Download)")
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


