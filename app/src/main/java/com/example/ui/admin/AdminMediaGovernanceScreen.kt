package com.example.ui.admin

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdminUser
import com.example.data.model.MediaGovernanceConfig
import com.example.data.model.SermonItem
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminMediaGovernanceScreen(
    viewModel: MainViewModel,
    currentAdmin: AdminUser?,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaConfig by viewModel.mediaGovernanceConfig.collectAsStateWithLifecycle()
    val allSermons by viewModel.sermons.collectAsStateWithLifecycle()

    var showUploadBottomSheet by remember { mutableStateOf(false) }
    var sermonToDelete by remember { mutableStateOf<SermonItem?>(null) }

    // Audio Player State
    var activePlayingSermonId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val playSermonAudio = { sermon: SermonItem ->
        if (activePlayingSermonId == sermon.sermonId) {
            mediaPlayer?.pause()
            activePlayingSermonId = null
        } else {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(sermon.audioUrl)
                    prepareAsync()
                    setOnPreparedListener {
                        start()
                        activePlayingSermonId = sermon.sermonId
                    }
                    setOnCompletionListener {
                        activePlayingSermonId = null
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ऑडियो चलाने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                activePlayingSermonId = null
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Master Admin Media Governance & Bitrate Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = GoldWarm,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ग्लोबल मीडिया व ऑडियो कंप्रेशन नियंत्रण",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Firebase Storage बचत व जीरो-बफ़रिंग मानक",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Toggle: Allow Pastors Long Messages
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "सभी पास्टरों को संडे/लॉन्ग संदेश अपलोड करने की अनुमति दें",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "सक्षम होने पर शाखा पास्टर सीधे अपने डिवाइस से संदेश कंप्रेस कर अपलोड कर सकते हैं",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = mediaConfig.allowPastorsLongMessages,
                            onCheckedChange = { allowed ->
                                viewModel.updateMediaGovernanceConfig(
                                    mediaConfig.copy(
                                        allowPastorsLongMessages = allowed,
                                        updatedBy = currentAdmin?.id ?: "admin"
                                    )
                                ) { success ->
                                    if (success) Toast.makeText(context, "सेटिंग्स अपडेट हुईं! ✅", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Bitrate Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ऑडियो कंप्रेशन बिटरेट (Default Audio Bitrate for All Uploads):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val bitrateOptions = listOf(
                            "32k" to "32 kbps (अल्ट्रा-लाइट / केवल आवाज़)",
                            "48k" to "48 kbps (अनुशंसित / डिफ़ॉल्ट मानक)",
                            "64k" to "64 kbps (उच्च गुणवत्ता वोकल)",
                            "128k" to "128 kbps (स्टूडियो / म्यूजिक सहित)"
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            bitrateOptions.forEach { (key, label) ->
                                val isSelected = mediaConfig.compressionBitrate == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateMediaGovernanceConfig(
                                            mediaConfig.copy(
                                                compressionBitrate = key,
                                                updatedBy = currentAdmin?.id ?: "admin"
                                            )
                                        ) { success ->
                                            if (success) Toast.makeText(context, "बिटरेट सेट: $key ✅", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldWarm.copy(alpha = 0.25f),
                                        selectedLabelColor = GoldWarm
                                    )
                                )
                            }
                        }

                        // Info Note
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldWarm.copy(alpha = 0.12f),
                            border = BorderStroke(0.8.dp, GoldWarm.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "ℹ️ 48 kbps AAC में 1 घंटे का संडे उपदेश लगभग 18–20 MB में बदल जाता है, जिससे यूज़र का डेटा और सर्वर बिल 75% तक बचता है।",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Header Action: Sermons List & Upload Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "अपलोड किए गए उपदेश व संदेश (${allSermons.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "शाखा आराधना, बाइबल स्टडी व संडे संदेश",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showUploadBottomSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_add_sermon_modal")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("नया उपदेश अपलोड", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sermons List
        if (allSermons.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "अभी कोई संडे उपदेश अपलोड नहीं हुआ है। ऊपर दिए गए बटन से नया उपदेश कंप्रेस कर लाइव करें।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(allSermons, key = { it.sermonId }) { sermon ->
                val isPlaying = activePlayingSermonId == sermon.sermonId
                val origMb = String.format(Locale.US, "%.1f MB", sermon.originalSizeBytes / (1024.0 * 1024.0))
                val compMb = String.format(Locale.US, "%.1f MB", sermon.compressedSizeBytes / (1024.0 * 1024.0))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlaying) GoldWarm.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        if (isPlaying) 1.2.dp else 0.6.dp,
                        if (isPlaying) GoldWarm else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GoldWarm.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = sermon.category,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldWarm,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = sermon.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "प्रचारक: ${sermon.preacherName}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Play Button
                            IconButton(
                                onClick = { playSermonAudio(sermon) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) MaterialTheme.colorScheme.error else GoldWarm)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black
                                )
                            }
                        }

                        if (sermon.scriptureReferences.isNotEmpty()) {
                            Text(
                                text = "📖 बाइबल वचन: ${sermon.scriptureReferences.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Compression Stats & Delete Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "📊 $origMb ➔ $compMb",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${sermon.bitrate.uppercase()} • बचत ${sermon.compressionRatio}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { sermonToDelete = sermon },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUploadBottomSheet) {
        SermonUploadBottomSheet(
            viewModel = viewModel,
            currentAdmin = currentAdmin,
            onDismiss = { showUploadBottomSheet = false },
            onSuccess = {
                showUploadBottomSheet = false
            }
        )
    }

    if (sermonToDelete != null) {
        val s = sermonToDelete!!
        AlertDialog(
            onDismissRequest = { sermonToDelete = null },
            title = { Text("उपदेश हटाएं?") },
            text = { Text("क्या आप '${s.title}' को हटाना चाहते हैं?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSermon(s) { success ->
                            if (success) Toast.makeText(context, "उपदेश हटाया गया", Toast.LENGTH_SHORT).show()
                        }
                        sermonToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हटाएं")
                }
            },
            dismissButton = {
                TextButton(onClick = { sermonToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
