package com.example.ui.admin

import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdminUser
import com.example.data.model.SermonItem
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AudioCompressionService
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SermonUploadBottomSheet(
    viewModel: MainViewModel,
    currentAdmin: AdminUser?,
    onDismiss: () -> Unit,
    onSuccess: (SermonItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaGovConfig by viewModel.mediaGovernanceConfig.collectAsStateWithLifecycle()

    var sermonTitle by remember { mutableStateOf("") }
    var preacherName by remember { mutableStateOf(currentAdmin?.name ?: "Rev. Vinay Kumar") }
    var scripturePassages by remember { mutableStateOf("रोमियों 8:28-39") }
    var selectedCategory by remember { mutableStateOf("संडे आराधना") } // "संडे आराधना", "बाइबल स्टडी", "उपवास प्रार्थना"

    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var audioFileName by remember { mutableStateOf("") }

    // Pre-compression preview state
    var isPreCompressing by remember { mutableStateOf(false) }
    var compressionResult by remember { mutableStateOf<AudioCompressionService.CompressionResult?>(null) }
    var compressionProgress by remember { mutableFloatStateOf(0f) }

    // Upload & Publish state
    var isPublishing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }

    // Audio Preview Player
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Audio Picker Launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAudioUri = uri
            audioFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "audio_sermon.mp3"
            mediaPlayer?.release()
            mediaPlayer = null
            isPlayingPreview = false

            // Auto trigger background compression preview
            isPreCompressing = true
            compressionProgress = 0.05f
            coroutineScope.launch {
                val res = AudioCompressionService.compressAudio(
                    context = context,
                    inputUri = uri,
                    bitrateKey = mediaGovConfig.compressionBitrate,
                    onProgress = { p -> compressionProgress = p }
                )
                isPreCompressing = false
                if (res.isSuccess) {
                    compressionResult = res.getOrNull()
                } else {
                    Toast.makeText(context, "कंप्रेशन में समस्या: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                .imePadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "नया उपदेश व संडे संदेश अपलोड",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ऑन-डिवाइस ऑडियो कंप्रेशन इंजन (${mediaGovConfig.compressionBitrate.uppercase()})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category ChoiceChips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("श्रेणी (Category) *", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("संडे आराधना", "बाइबल स्टडी", "उपवास प्रार्थना", "विशेष संगति").forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldWarm.copy(alpha = 0.25f),
                                        selectedLabelColor = GoldWarm
                                    )
                                )
                            }
                        }
                    }
                }

                // Sermon Title
                item {
                    OutlinedTextField(
                        value = sermonTitle,
                        onValueChange = { sermonTitle = it },
                        label = { Text("उपदेश का शीर्षक (Sermon Title) *") },
                        placeholder = { Text("उदा. परमेश्वर का अटूट अनुग्रह...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Preacher Name
                item {
                    OutlinedTextField(
                        value = preacherName,
                        onValueChange = { preacherName = it },
                        label = { Text("प्रचारक / पास्टर का नाम (Preacher Name) *") },
                        placeholder = { Text("उदा. Rev. Vinay Kumar") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Scripture References
                item {
                    OutlinedTextField(
                        value = scripturePassages,
                        onValueChange = { scripturePassages = it },
                        label = { Text("बाइबल संदर्भ (Scripture References) *") },
                        placeholder = { Text("उदा. रोमियों 8:28-39, यूहन्ना 3:16") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Audio File Picker & Compression Status Card
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("ऑडियो फ़ाइल (Raw Audio File) *", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            if (selectedAudioUri == null) {
                                Button(
                                    onClick = { audioPickerLauncher.launch("audio/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("ऑडियो फ़ाइल चुनें (MP3, WAV, M4A)")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Audiotrack, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = audioFileName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }

                                    TextButton(
                                        onClick = { audioPickerLauncher.launch("audio/*") },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("बदलें", fontSize = 11.sp)
                                    }
                                }

                                // Active Compression Progress
                                if (isPreCompressing) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "ऑडियो कंप्रेस हो रहा है...",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${(compressionProgress * 100).toInt()}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { compressionProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = GoldWarm
                                        )
                                    }
                                }

                                // Compression Result Badge & Preview Player
                                if (compressionResult != null && !isPreCompressing) {
                                    val cr = compressionResult!!
                                    val origMb = String.format(Locale.US, "%.1f MB", cr.originalSizeBytes / (1024.0 * 1024.0))
                                    val compMb = String.format(Locale.US, "%.1f MB", cr.compressedSizeBytes / (1024.0 * 1024.0))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoldWarm.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "✨ $origMb ➔ $compMb (बचत: ${cr.compressionRatio})",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = GoldWarm,
                                                    contentColor = Color.Black
                                                ) {
                                                    Text(
                                                        text = cr.bitrate.uppercase(),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "अनुमानित अवधि: ${cr.durationMinutes} मिनट • AAC Mono",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Preview Play Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (isPlayingPreview) {
                                                    mediaPlayer?.pause()
                                                    isPlayingPreview = false
                                                } else {
                                                    try {
                                                        if (mediaPlayer == null) {
                                                            mediaPlayer = MediaPlayer().apply {
                                                                setDataSource(cr.compressedFile.absolutePath)
                                                                prepare()
                                                                setOnCompletionListener { isPlayingPreview = false }
                                                            }
                                                        }
                                                        mediaPlayer?.start()
                                                        isPlayingPreview = true
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "प्लेबैक में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isPlayingPreview) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                if (isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (isPlayingPreview) "रोकें" else "कंप्रेस ऑडियो सुनें", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Publishing Progress
                if (isPublishing) {
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = GoldWarm.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("क्लाउड पर अपलोड हो रहा है...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${(uploadProgress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { uploadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = GoldWarm
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Submit Button
            Button(
                onClick = {
                    if (sermonTitle.isBlank()) {
                        Toast.makeText(context, "कृपया उपदेश का शीर्षक दर्ज करें", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedAudioUri == null) {
                        Toast.makeText(context, "कृपया ऑडियो फ़ाइल चुनें", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isPublishing = true
                    val scriptureList = scripturePassages.split(',', '।', ';').map { it.trim() }.filter { it.isNotBlank() }

                    viewModel.compressAndUploadSermon(
                        context = context,
                        inputUri = selectedAudioUri!!,
                        title = sermonTitle.trim(),
                        preacherName = preacherName.trim(),
                        scriptureReferences = scriptureList,
                        category = selectedCategory,
                        branchId = "branch_ncc_main",
                        uploadedBy = currentAdmin?.id ?: "pastor_vinay",
                        onCompressionProgress = { p -> compressionProgress = p },
                        onUploadProgress = { p -> uploadProgress = p }
                    ) { success, errorMsg, createdSermon ->
                        isPublishing = false
                        if (success && createdSermon != null) {
                            Toast.makeText(context, "उपदेश सफलतापूर्वक अपलोड व लाइव हुआ! 🎉", Toast.LENGTH_LONG).show()
                            onSuccess(createdSermon)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "अपलोड विफल: ${errorMsg ?: "अज्ञात त्रुटि"}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isPublishing && !isPreCompressing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_publish_sermon"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("अपलोड व लाइव हो रहा है...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("उपदेश अपलोड व लाइव करें (Compress & Publish)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
