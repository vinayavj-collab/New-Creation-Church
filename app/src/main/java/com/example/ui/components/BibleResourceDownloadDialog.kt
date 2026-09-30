package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.BibleResourceModule
import com.example.data.bible.repository.BibleResourceRepository
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleResourceDownloadDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { BibleResourceRepository.getInstance() }
    val activeResources by repository.observeActiveResources().collectAsState(initial = emptyList())

    var downloadingId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val bibleDir = File(dir, "bible_modules")
        if (bibleDir.exists()) {
            val names = bibleDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
            downloadedFiles = names
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        },
        title = {
            Text("📖 अनुवाद व कमेंट्री डाउनलोड केंद्र", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                Text(
                    "Master Admin द्वारा उपलब्ध कराए गए अतिरिक्त बाइबल अनुवाद व कमेंट्री फ़ाइलें:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (activeResources.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("वर्तमान में कोई अतिरिक्त अनुवाद या कमेंट्री उपलब्ध नहीं है", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeResources, key = { it.id }) { resource ->
                            val isDownloaded = downloadedFiles.contains(resource.fileName)
                            val isThisDownloading = downloadingId == resource.id

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(resource.titleHindi, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                text = "${resource.resourceType} • ${resource.fileFormat} • ${String.format(Locale.getDefault(), "%.1f MB", resource.fileSizeBytes / (1024.0 * 1024.0))}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }

                                        if (isDownloaded) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("डाउनलोड है", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        } else if (isThisDownloading) {
                                            Text("$downloadProgress%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        } else {
                                            FilledTonalButton(
                                                onClick = {
                                                    downloadingId = resource.id
                                                    downloadProgress = 0
                                                    repository.downloadAndSaveResource(
                                                        context = context,
                                                        resource = resource,
                                                        onProgress = { progress -> downloadProgress = progress },
                                                        onComplete = { success, file, err ->
                                                            downloadingId = null
                                                            if (success && file != null) {
                                                                downloadedFiles = downloadedFiles + resource.fileName
                                                                Toast.makeText(context, "${resource.titleHindi} सफलतापूर्वक डाउनलोड हो गया!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "त्रुटि: $err", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    )
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("डाउनलोड", fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    if (isThisDownloading) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { downloadProgress / 100f },
                                            modifier = Modifier.fillMaxWidth().height(4.dp)
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
            TextButton(onClick = onDismissRequest) {
                Text("बंद करें")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
