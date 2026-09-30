package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.AdminUser
import com.example.data.model.AppReleaseVersion
import com.example.data.repository.AppReleaseRepository
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppVersionManagerScreen(
    viewModel: MainViewModel,
    currentAdmin: AdminUser?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AppReleaseRepository.getInstance() }
    val allReleases by repository.observeAllReleases().collectAsState(initial = emptyList())
    val latestActiveRelease by repository.observeLatestActiveRelease().collectAsState(initial = null)

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableLongStateOf(0L) }

    var versionCodeText by remember { mutableStateOf((BuildConfig.VERSION_CODE + 1).toString()) }
    var versionNameText by remember { mutableStateOf("71.6") }
    var releaseNotesText by remember { mutableStateOf("") }
    var isForceUpdate by remember { mutableStateOf(false) }
    var setAsActiveImmediately by remember { mutableStateOf(true) }

    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var releaseToDelete by remember { mutableStateOf<AppReleaseVersion?>(null) }
    var showUploadForm by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        selectedFileName = cursor.getString(nameIndex) ?: "app-release.apk"
                        selectedFileSize = cursor.getLong(sizeIndex)
                    }
                }
            } catch (e: Exception) {
                selectedFileName = "app-release.apk"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🚀 ऐप अपडेट व वर्जन प्रबंधन",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Firebase Storage & Firestore Live Releases",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showUploadForm = !showUploadForm }) {
                        Icon(
                            imageVector = if (showUploadForm) Icons.Default.Close else Icons.Default.AddCircle,
                            contentDescription = "New Release",
                            tint = if (showUploadForm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Stats Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "वर्तमान स्थापित ऐप बिल्ड:",
                                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "कुल वर्जन्स: ${allReleases.size}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (latestActiveRelease != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (latestActiveRelease != null) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (latestActiveRelease != null) {
                                    "लाइव सक्रिय वर्जन: v${latestActiveRelease?.versionName} (Build ${latestActiveRelease?.versionCode})" +
                                            if (latestActiveRelease?.isForceUpdate == true) " • अनिवार्य अपडेट" else " • सामान्य अपडेट"
                                } else {
                                    "कोई भी वर्जन अभी सक्रिय नहीं है"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // Upload Form Card (Expandable)
            item {
                AnimatedVisibility(visible = showUploadForm) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "नया APK फ़ाइल अपलोड करें",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // File Select Button & Details
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { filePickerLauncher.launch("application/vnd.android.package-archive") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = if (selectedFileUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (selectedFileUri != null) selectedFileName else "डिवाइस से APK फ़ाइल चुनें (.apk)",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (selectedFileUri != null) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (selectedFileSize > 0L) {
                                            Text(
                                                text = "साइज़: ${String.format(Locale.getDefault(), "%.2f", selectedFileSize / (1024.0 * 1024.0))} MB",
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { filePickerLauncher.launch("application/vnd.android.package-archive") },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("ब्राउज़", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = versionCodeText,
                                    onValueChange = { versionCodeText = it },
                                    label = { Text("Version Code") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = versionNameText,
                                    onValueChange = { versionNameText = it },
                                    label = { Text("Version Name") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = releaseNotesText,
                                onValueChange = { releaseNotesText = it },
                                label = { Text("Release Notes (क्या नया है)") },
                                placeholder = { Text("उदा. नए फीचर्स, सुरक्षा व प्रदर्शन में सुधार...") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Force Update & Set Active Toggles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "अनिवार्य अपडेट (Force Update)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "यूज़र्स को बिना अपडेट किए ऐप इस्तेमाल करने की अनुमति नहीं होगी",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                Switch(
                                    checked = isForceUpdate,
                                    onCheckedChange = { isForceUpdate = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "तुरंत लाइव करें (Set Active Now)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "अपलोड होते ही सभी यूज़र्स को यह अपडेट दिखने लगेगा",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                Switch(
                                    checked = setAsActiveImmediately,
                                    onCheckedChange = { setAsActiveImmediately = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Upload Progress & Button
                            if (isUploading) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Firebase Storage में अपलोड हो रहा है...",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${(uploadProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { uploadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val uri = selectedFileUri
                                        val code = versionCodeText.toIntOrNull() ?: 0
                                        if (uri == null) {
                                            Toast.makeText(context, "कृपया पहले APK फ़ाइल चुनें", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (code <= 0 || versionNameText.isBlank()) {
                                            Toast.makeText(context, "कृपया वैध Version Code व Name दर्ज करें", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        isUploading = true
                                        uploadProgress = 0f
                                        statusMessage = "अपलोड शुरू हो रहा है..."

                                        val release = AppReleaseVersion(
                                            versionCode = code,
                                            versionName = versionNameText.trim(),
                                            apkFileName = selectedFileName.ifBlank { "app-v${versionNameText}.apk" },
                                            fileSizeBytes = selectedFileSize,
                                            releaseNotes = releaseNotesText.trim(),
                                            isForceUpdate = isForceUpdate,
                                            isActive = setAsActiveImmediately,
                                            uploadedByAdmin = currentAdmin?.name ?: "Master Admin",
                                            timestamp = System.currentTimeMillis()
                                        )

                                        repository.uploadApkAndPublishRelease(
                                            context = context,
                                            fileUri = uri,
                                            version = release,
                                            onProgress = { progress -> uploadProgress = progress },
                                            onComplete = { success, error ->
                                                isUploading = false
                                                if (success) {
                                                    Toast.makeText(context, "APK सफलतापूर्वक अपलोड और पब्लिश हो गया!", Toast.LENGTH_LONG).show()
                                                    selectedFileUri = null
                                                    selectedFileName = ""
                                                    selectedFileSize = 0L
                                                    releaseNotesText = ""
                                                    showUploadForm = false
                                                } else {
                                                    Toast.makeText(context, "अपलोड त्रुटि: $error", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("अपलोड करें और पब्लिश करें (Upload & Publish)")
                                }
                            }
                        }
                    }
                }
            }

            // Uploaded Versions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "अपलोड किए गए सभी वर्जन्स (${allReleases.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (!showUploadForm) {
                        FilledTonalButton(
                            onClick = { showUploadForm = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("नया वर्जन", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (allReleases.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "अभी तक कोई APK वर्जन अपलोड नहीं किया गया है",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ऊपर '+ नया वर्जन' बटन दबाकर नया APK अपलोड करें।",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                }
            } else {
                items(allReleases, key = { it.id }) { release ->
                    VersionReleaseCard(
                        release = release,
                        onSetActive = {
                            repository.setActiveRelease(release) { success, err ->
                                if (success) {
                                    Toast.makeText(context, "वर्जन v${release.versionName} को सक्रिय कर दिया गया है", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "त्रुटि: $err", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onToggleForceUpdate = {
                            val newForce = !release.isForceUpdate
                            repository.toggleForceUpdate(release, newForce) { success, err ->
                                if (success) {
                                    Toast.makeText(context, if (newForce) "अनिवार्य अपडेट लागू किया गया" else "सामान्य अपडेट में बदला गया", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "त्रुटि: $err", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCopyDownloadUrl = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("APK URL", release.downloadUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "डाउनलोड लिंक कॉपी हो गया", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { releaseToDelete = release }
                    )
                }
            }

            // GitHub Releases Dual-Mode Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🔗 GitHub Releases बैकअप व सिंक",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ऐप में हाइब्रिड (Dual-Source) अपडेट सिस्टम चालू है। आप चाहे ऐप से Firestore/Firebase Storage में APK अपलोड करें या GitHub पर नया Release पब्लिश करें—ऐप दोनों जगह से नवीनतम वर्जन को पहचान कर यूज़र्स को अपडेट देता है।",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Repository: vinayavj-collab/New-Creation-Church",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val browserIntent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://github.com/vinayavj-collab/New-Creation-Church/releases")
                                        )
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "ब्राउज़र नहीं खोला जा सका", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GitHub Releases", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.checkForAppUpdates(force = true)
                                    Toast.makeText(context, "GitHub व Cloud से जांच शुरू की गई", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("अभी सिंक करें", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (releaseToDelete != null) {
        val rel = releaseToDelete!!
        AlertDialog(
            onDismissRequest = { releaseToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("वर्जन हटाएं?") },
            text = {
                Text("क्या आप निश्चित रूप से वर्जन v${rel.versionName} (Build ${rel.versionCode}) को हटाना चाहते हैं? यह Firebase Storage से APK फ़ाइल और Firestore से रिकॉर्ड दोनों को स्थायी रूप से हटा देगा।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = rel
                        releaseToDelete = null
                        repository.deleteRelease(target) { success, err ->
                            if (success) {
                                Toast.makeText(context, "वर्जन v${target.versionName} हटा दिया गया", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "त्रुटि: $err", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हाँ, हटाएं")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { releaseToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun VersionReleaseCard(
    release: AppReleaseVersion,
    onSetActive: () -> Unit,
    onToggleForceUpdate: () -> Unit,
    onCopyDownloadUrl: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(release.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(release.timestamp))
    }
    val sizeMbStr = remember(release.fileSizeBytes) {
        if (release.fileSizeBytes > 0) {
            String.format(Locale.getDefault(), "%.1f MB", release.fileSizeBytes / (1024.0 * 1024.0))
        } else {
            "APK"
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (release.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = if (release.isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (release.isActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "v${release.versionName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Build ${release.versionCode}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (release.isActive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "सक्रिय (Active)",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSetActive,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("सक्रिय करें", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📦 $sizeMbStr",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "🕒 $dateStr",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                if (release.uploadedByAdmin.isNotBlank()) {
                    Text(
                        text = "👤 ${release.uploadedByAdmin}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            // Release Notes
            if (release.releaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = release.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Force Update indicator / toggle
                FilterChip(
                    selected = release.isForceUpdate,
                    onClick = onToggleForceUpdate,
                    label = {
                        Text(
                            text = if (release.isForceUpdate) "🚨 अनिवार्य (Force)" else "सामान्य अपडेट",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.error
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCopyDownloadUrl, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
