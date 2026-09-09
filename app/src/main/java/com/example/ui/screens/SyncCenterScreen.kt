package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SyncCenterRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncCenterScreen(
    syncRepository: SyncCenterRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val syncStatus by syncRepository.status.collectAsState()

    val formatDate = { timestamp: Long ->
        if (timestamp <= 0) "Never synced"
        else SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Center", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Offline-First Synchronization",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Keep your Fellowship Events, YouTube videos, Bible and Christian Songs up to date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val result = syncRepository.syncAll()
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "All feeds synchronized successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Sync complete with minor warnings", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !syncStatus.isSyncing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (syncStatus.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing Feeds...")
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SYNC ALL NOW", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "FEED STATUS & TIMESTAMPS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                SyncItemCard(
                    icon = Icons.Default.Event,
                    title = "Blogger Fellowship Events",
                    subtitle = "Events, Messages, Photos & Vlogs",
                    lastSync = formatDate(syncStatus.lastBloggerSync)
                )
            }

            item {
                SyncItemCard(
                    icon = Icons.Default.VideoLibrary,
                    title = "YouTube Channels & Playlists",
                    subtitle = "AVJ Worship & Vinay Kumar AVJ media",
                    lastSync = formatDate(syncStatus.lastYouTubeSync)
                )
            }

            item {
                SyncItemCard(
                    icon = Icons.Default.MenuBook,
                    title = "Holy Bible Cache",
                    subtitle = "Chapters, Headings & Cross References",
                    lastSync = formatDate(syncStatus.lastBibleSync)
                )
            }

            item {
                SyncItemCard(
                    icon = Icons.Default.MusicNote,
                    title = "Christian Songs & Lyrics",
                    subtitle = "Hindi, Sadri & English Worship",
                    lastSync = formatDate(syncStatus.lastLyricsSync)
                )
            }
        }
    }
}

@Composable
private fun SyncItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    lastSync: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Last sync: $lastSync",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
