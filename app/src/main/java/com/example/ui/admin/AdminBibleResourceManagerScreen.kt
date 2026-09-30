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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.BibleResourceModule
import com.example.data.bible.repository.BibleResourceRepository
import com.example.data.model.AdminUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBibleResourceManagerScreen(
    currentAdmin: AdminUser?,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { BibleResourceRepository.getInstance() }
    val allResources by repository.observeAllResources().collectAsState(initial = emptyList())

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableLongStateOf(0L) }

    var titleHindiText by remember { mutableStateOf("") }
    var titleEnglishText by remember { mutableStateOf("") }
    var resourceType by remember { mutableStateOf("TRANSLATION") } // TRANSLATION, COMMENTARY, STUDY_GUIDE
    var languageText by remember { mutableStateOf("Hindi") }
    var fileFormat by remember { mutableStateOf("ZIP") }
    var descriptionText by remember { mutableStateOf("") }
    var authorText by remember { mutableStateOf("") }

    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var showUploadForm by remember { mutableStateOf(false) }
    var resourceToDelete by remember { mutableStateOf<BibleResourceModule?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        selectedFileName = cursor.getString(nameIdx) ?: "resource_file"
                        selectedFileSize = cursor.getLong(sizeIdx)
                        if (selectedFileName.endsWith(".zip", ignoreCase = true)) fileFormat = "ZIP"
                        else if (selectedFileName.endsWith(".json", ignoreCase = true)) fileFormat = "JSON"
                        else if (selectedFileName.endsWith(".pdf", ignoreCase = true)) fileFormat = "PDF"
                        else if (selectedFileName.endsWith(".db", ignoreCase = true) || selectedFileName.endsWith(".sqlite", ignoreCase = true)) fileFormat = "SQLITE"
                    }
                }
            } catch (_: Exception) {
                selectedFileName = "resource_file"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📖 बाइबल व कमेंट्री फ़ाइल प्रबंधन", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("ZIP / JSON / DB Modules Cloud Upload", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
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
                            imageVector = if (showUploadForm) Icons.Default.Close else Icons.Default.CloudUpload,
                            contentDescription = "Upload Resource",
                            tint = if (showUploadForm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
            // Summary Header Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("क्लाउड मॉड्यूल लाइब्रेरी", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("अनुवाद, टीका (Commentary) व अध्ययन सामग्री", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "कुल फाइल्स: ${allResources.size}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }

            // Upload Form Card
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
                                Text("नया अनुवाद / कमेंट्री फ़ाइल अपलोड करें", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Resource Type Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "TRANSLATION" to "बाइबल अनुवाद",
                                    "COMMENTARY" to "कमेंट्री (टीका)",
                                    "STUDY_GUIDE" to "अध्ययन सामग्री"
                                ).forEach { (type, label) ->
                                    val isSelected = resourceType == type
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { resourceType = type },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // File Select Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { filePickerLauncher.launch("*/*") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
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
                                            text = if (selectedFileUri != null) selectedFileName else "डिवाइस से ZIP / JSON / DB फ़ाइल चुनें",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (selectedFileUri != null) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (selectedFileSize > 0L) {
                                            Text(
                                                text = "साइज़: ${String.format(Locale.getDefault(), "%.2f", selectedFileSize / (1024.0 * 1024.0))} MB • Format: $fileFormat",
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("चुनें", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = titleHindiText,
                                onValueChange = { titleHindiText = it },
                                label = { Text("शीर्षक (हिंदी में)") },
                                placeholder = { Text("उदा. हिंदी IRV अनुवाद, मैथ्यू हेनरी कमेंट्री...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = titleEnglishText,
                                    onValueChange = { titleEnglishText = it },
                                    label = { Text("Title (English)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = languageText,
                                    onValueChange = { languageText = it },
                                    label = { Text("भाषा (Language)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = authorText,
                                onValueChange = { authorText = it },
                                label = { Text("लेखक / स्रोत / प्रकाशक (Author / Publisher)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = descriptionText,
                                onValueChange = { descriptionText = it },
                                label = { Text("संक्षिप्त विवरण (Description / Notes)") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (isUploading) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Firebase Storage में अपलोड हो रहा है...", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("${(uploadProgress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { uploadProgress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val uri = selectedFileUri
                                        if (uri == null) {
                                            Toast.makeText(context, "कृपया पहले फ़ाइल चुनें", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (titleHindiText.isBlank()) {
                                            Toast.makeText(context, "कृपया शीर्षक दर्ज करें", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        isUploading = true
                                        uploadProgress = 0f

                                        val module = BibleResourceModule(
                                            titleHindi = titleHindiText.trim(),
                                            titleEnglish = titleEnglishText.trim(),
                                            resourceType = resourceType,
                                            language = languageText.trim(),
                                            fileFormat = fileFormat,
                                            fileName = selectedFileName,
                                            fileSizeBytes = selectedFileSize,
                                            description = descriptionText.trim(),
                                            authorOrSource = authorText.trim(),
                                            isActive = true,
                                            uploadedByAdmin = currentAdmin?.name ?: "Master Admin",
                                            timestamp = System.currentTimeMillis()
                                        )

                                        repository.uploadResourceFile(
                                            context = context,
                                            fileUri = uri,
                                            resource = module,
                                            onProgress = { progress -> uploadProgress = progress },
                                            onComplete = { success, error ->
                                                isUploading = false
                                                if (success) {
                                                    Toast.makeText(context, "फ़ाइल सफलतापूर्वक अपलोड और पब्लिश हो गई!", Toast.LENGTH_LONG).show()
                                                    selectedFileUri = null
                                                    selectedFileName = ""
                                                    titleHindiText = ""
                                                    titleEnglishText = ""
                                                    descriptionText = ""
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
                                    Text("अपलोड व पब्लिश करें (Upload Resource)")
                                }
                            }
                        }
                    }
                }
            }

            // Uploaded Resources Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("अपलोड किए गए मॉड्यूल्स (${allResources.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    if (!showUploadForm) {
                        FilledTonalButton(
                            onClick = { showUploadForm = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("नया अपलोड", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (allResources.isEmpty()) {
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
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("कोई भी बाइबल अनुवाद या कमेंट्री फ़ाइल अपलोड नहीं है", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ऊपर '+ नया अपलोड' बटन दबाकर ZIP या JSON फ़ाइल अपलोड करें।", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline))
                        }
                    }
                }
            } else {
                items(allResources, key = { it.id }) { resource ->
                    BibleResourceCard(
                        resource = resource,
                        onToggleActive = {
                            val newActive = !resource.isActive
                            repository.toggleResourceActive(resource, newActive) { success, err ->
                                if (success) {
                                    Toast.makeText(context, if (newActive) "मॉड्यूल सक्रिय किया गया" else "मॉड्यूल निष्क्रिय किया गया", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "त्रुटि: $err", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCopyLink = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Resource Link", resource.downloadUrl))
                            Toast.makeText(context, "डाउनलोड लिंक कॉपी हो गया", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { resourceToDelete = resource }
                    )
                }
            }
        }
    }

    if (resourceToDelete != null) {
        val res = resourceToDelete!!
        AlertDialog(
            onDismissRequest = { resourceToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("मॉड्यूल हटाएं?") },
            text = { Text("क्या आप निश्चित रूप से '${res.titleHindi}' को हटाना चाहते हैं? यह Firebase Storage से फ़ाइल और Firestore से रिकॉर्ड दोनों को स्थायी रूप से हटा देगा।") },
            confirmButton = {
                Button(
                    onClick = {
                        val target = res
                        resourceToDelete = null
                        repository.deleteResource(target) { success, err ->
                            if (success) {
                                Toast.makeText(context, "मॉड्यूल हटा दिया गया", Toast.LENGTH_SHORT).show()
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
                OutlinedButton(onClick = { resourceToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun BibleResourceCard(
    resource: BibleResourceModule,
    onToggleActive: () -> Unit,
    onCopyLink: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(resource.timestamp) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(resource.timestamp))
    }
    val sizeMbStr = remember(resource.fileSizeBytes) {
        if (resource.fileSizeBytes > 0) {
            String.format(Locale.getDefault(), "%.1f MB", resource.fileSizeBytes / (1024.0 * 1024.0))
        } else {
            resource.fileFormat
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (resource.isActive) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                        color = if (resource.isActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(resource.titleHindi, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = when (resource.resourceType) {
                            "COMMENTARY" -> "टीका/कमेंट्री"
                            "STUDY_GUIDE" -> "गाइड"
                            else -> "अनुवाद"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            if (resource.titleEnglish.isNotBlank()) {
                Text(resource.titleEnglish, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("📁 ${resource.fileFormat}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Text("📦 $sizeMbStr", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Text("🌐 ${resource.language}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Text("🕒 $dateStr", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }

            if (resource.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(resource.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = resource.isActive,
                    onClick = onToggleActive,
                    label = { Text(if (resource.isActive) "सक्रिय (Active)" else "निष्क्रिय (Inactive)", fontSize = 11.sp) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCopyLink, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
