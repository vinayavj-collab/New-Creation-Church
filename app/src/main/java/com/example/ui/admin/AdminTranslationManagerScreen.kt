package com.example.ui.admin

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
import com.example.data.bible.local.BibleDatabase
import com.example.data.bible.local.Sqlite3BibleImporter
import com.example.data.bible.model.BibleTranslation
import com.example.ui.theme.GoldWarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class TranslationStatusItem(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String,
    val language: String,
    val verseCount: Int,
    val headingCount: Int,
    val commentaryCount: Int,
    val isDefault: Boolean,
    val isActive: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTranslationManagerScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember { BibleDatabase.getInstance(context) }
    val dao = remember { database.bibleDao() }
    val prefs = remember { context.getSharedPreferences("admin_custom_translations", Context.MODE_PRIVATE) }

    var translationsList by remember { mutableStateOf<List<TranslationStatusItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isImporting by remember { mutableStateOf(false) }
    var importStatusText by remember { mutableStateOf("") }

    var translationToDelete by remember { mutableStateOf<TranslationStatusItem?>(null) }
    var showAddCustomTranslationDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Import Dialog State
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var importTargetTranslationId by remember { mutableStateOf("HIOV") }
    var importAsCommentary by remember { mutableStateOf(false) }
    var replaceExistingData by remember { mutableStateOf(true) }

    // Add Translation Form State
    var newId by remember { mutableStateOf("") }
    var newNameHindi by remember { mutableStateOf("") }
    var newNameEnglish by remember { mutableStateOf("") }
    var newLanguage by remember { mutableStateOf("hi") }

    fun refreshTranslationStats() {
        coroutineScope.launch {
            isLoading = true
            val items = withContext(Dispatchers.IO) {
                // Base translations
                val list = mutableListOf<BibleTranslation>()
                list.addAll(BibleTranslation.ALL)

                // Custom registered translations from prefs
                val customRaw = prefs.getStringSet("custom_translation_ids", emptySet()) ?: emptySet()
                for (cId in customRaw) {
                    val hi = prefs.getString("name_hi_$cId", cId) ?: cId
                    val en = prefs.getString("name_en_$cId", cId) ?: cId
                    val lang = prefs.getString("lang_$cId", "hi") ?: "hi"
                    if (list.none { it.id.equals(cId, ignoreCase = true) }) {
                        list.add(
                            BibleTranslation(
                                id = cId,
                                nameHindi = hi,
                                nameEnglish = en
                            )
                        )
                    }
                }

                list.map { trans ->
                    val vCount = try { dao.getVerseCount(trans.id) } catch (_: Exception) { 0 }
                    val hCount = try { dao.getHeadingCount(trans.id) } catch (_: Exception) { 0 }
                    val cCount = 0
                    val isDef = trans.id.equals(BibleTranslation.HIOV.id, ignoreCase = true)
                    val active = vCount > 0 || cCount > 0 || isDef

                    TranslationStatusItem(
                        id = trans.id,
                        nameHindi = trans.nameHindi,
                        nameEnglish = trans.nameEnglish,
                        language = trans.language,
                        verseCount = vCount,
                        headingCount = hCount,
                        commentaryCount = cCount,
                        isDefault = isDef,
                        isActive = active
                    )
                }
            }
            translationsList = items
            isLoading = false
        }
    }

    val dbPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst()) {
                        selectedFileName = cursor.getString(nameIndex) ?: "bible.db"
                    }
                }
            } catch (_: Exception) {
                selectedFileName = "bible.db"
            }
            // Auto detect if file name has commentary
            if (selectedFileName.contains("commentar", ignoreCase = true) || selectedFileName.contains("teeka", ignoreCase = true)) {
                importAsCommentary = true
            }
            showImportDialog = true
        }
    }

    fun startImportProcess() {
        val uri = selectedFileUri ?: return
        showImportDialog = false
        isImporting = true
        importStatusText = "फ़ाइल लोड हो रही है..."

        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val contentResolver = context.contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                        ?: return@withContext Result.failure(Exception("फ़ाइल खोली नहीं जा सकी"))

                    val isZip = selectedFileName.endsWith(".zip", ignoreCase = true)
                    if (isZip) {
                        importStatusText = "ZIP संग्रह अनपैक व आयात हो रहा है..."
                        val count = Sqlite3BibleImporter.importZipBibleFile(context, database, inputStream)
                        Result.success(count)
                    } else {
                        importStatusText = "डेटाबेस आयात हो रहा है..."
                        val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}.sqlite")
                        FileOutputStream(tempFile).use { out ->
                            inputStream.copyTo(out)
                        }

                        val targetId = if (importAsCommentary && !importTargetTranslationId.contains("commentar", ignoreCase = true)) {
                            "${importTargetTranslationId}_commentaries"
                        } else {
                            importTargetTranslationId
                        }

                        val res = Sqlite3BibleImporter.importSqliteFileDirect(
                            context = context,
                            appDb = database,
                            dbFile = tempFile,
                            targetTranslationId = targetId,
                            replaceExisting = replaceExistingData
                        )
                        tempFile.delete()
                        res
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            isImporting = false
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                Toast.makeText(context, "सफलतापूर्वक आयात सम्पन्न! कुल प्रविष्टियाँ: $count ✨", Toast.LENGTH_LONG).show()
                refreshTranslationStats()
            } else {
                Toast.makeText(context, "आयात त्रुटि: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshTranslationStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "📖 बाइबल अनुवाद व कमेंट्री प्रबंधन",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "अनुवाद, टीका (Commentary) व SQLite3 डेटा आयात करें",
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
                    IconButton(onClick = { refreshTranslationStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { showAddCustomTranslationDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("नया अनुवाद रजिस्टर करें", fontSize = 12.sp)
                }

                ExtendedFloatingActionButton(
                    onClick = { dbPickerLauncher.launch("*/*") },
                    icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                    text = { Text("डेटाबेस / कमेंट्री अपलोड करें", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Overview Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "बाइबल व कमेंट्री शासन (State Governance)",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "मास्टर Admin सीधे SQLite3 (.db/.sqlite) या Zip फ़ाइल द्वारा नए बाइबल अनुवाद और वचन टीकाएं (Commentaries) अपलोड कर सकते हैं।",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { dbPickerLauncher.launch("*/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("फ़ाइल चुनें (.db/.sqlite)", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Progress Banner if importing
                    if (isImporting) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "डेटाबेस आयात जारी है...",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = importStatusText,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "उपलब्ध अनुवाद एवं कमेंट्री स्थितियां (${translationsList.size})",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(translationsList, key = { it.id }) { item ->
                        TranslationItemCard(
                            item = item,
                            onDeleteClick = { translationToDelete = item },
                            onPurgeVerses = {
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        dao.deleteVersesByTranslation(item.id)
                                        dao.deleteHeadingsByTranslation(item.id)
                                    }
                                    Toast.makeText(context, "${item.id} के वचन रूम डेटाबेस से हटाए गए! 🗑️", Toast.LENGTH_SHORT).show()
                                    refreshTranslationStats()
                                }
                            },
                            onClearCommentaries = {
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        dao.deleteCommentariesByTranslation(item.id)
                                        dao.deleteCommentariesByTranslation("${item.id}_commentaries")
                                    }
                                    Toast.makeText(context, "${item.id} की टीका/कमेंट्री हटा दी गई! 🗑️", Toast.LENGTH_SHORT).show()
                                    refreshTranslationStats()
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }

    // Import Dialog for SQLite / Commentary
    if (showImportDialog && selectedFileUri != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("बाइबल / कमेंट्री डेटाबेस आयात करें", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedFileName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text("डेटा का प्रकार चुनें:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !importAsCommentary,
                            onClick = { importAsCommentary = false },
                            label = { Text("📖 मुख्य अनुवाद (Verses)") }
                        )
                        FilterChip(
                            selected = importAsCommentary,
                            onClick = { importAsCommentary = true },
                            label = { Text("💬 वचन टीका (Commentary)") }
                        )
                    }

                    OutlinedTextField(
                        value = importTargetTranslationId,
                        onValueChange = { importTargetTranslationId = it.trim().uppercase() },
                        label = { Text("लक्षित अनुवाद ID (Target Translation ID)") },
                        placeholder = { Text("HIOV या NKJV") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "मौजूदा डेटा को अधिलेखित (Replace) करें",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Switch(
                            checked = replaceExistingData,
                            onCheckedChange = { replaceExistingData = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { startImportProcess() },
                    enabled = importTargetTranslationId.isNotBlank()
                ) {
                    Text("आयात प्रारंभ करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Confirmation Dialog for Deleting Translation
    if (translationToDelete != null) {
        val target = translationToDelete!!
        AlertDialog(
            onDismissRequest = { translationToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "अनुवाद हटाएं (${target.id})?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Text(
                    text = "क्या आप निश्चित रूप से '${target.nameHindi}' (${target.id}) के सभी ${target.verseCount} वचनों, ${target.headingCount} शीर्षकों और ${target.commentaryCount} टीकाओं को डेटाबेस से पूरी तरह हटाना चाहते हैं?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = target.id
                        translationToDelete = null
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                dao.deleteVersesByTranslation(toRemove)
                                dao.deleteHeadingsByTranslation(toRemove)
                                dao.deleteCommentariesByTranslation(toRemove)
                                dao.deleteCommentariesByTranslation("${toRemove}_commentaries")

                                // Remove from prefs if custom
                                val currentSet = prefs.getStringSet("custom_translation_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                                if (currentSet.contains(toRemove)) {
                                    currentSet.remove(toRemove)
                                    prefs.edit().putStringSet("custom_translation_ids", currentSet).apply()
                                }
                            }
                            Toast.makeText(context, "$toRemove सफलतापूर्वक हटा दिया गया! ✅", Toast.LENGTH_SHORT).show()
                            refreshTranslationStats()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हां, डिलीट करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { translationToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Add Custom Translation Dialog
    if (showAddCustomTranslationDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomTranslationDialog = false },
            title = {
                Text(
                    text = "नया अनुवाद पंजीकृत करें",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newId,
                        onValueChange = { newId = it.trim().uppercase() },
                        label = { Text("अनुवाद ID / कोड (जैसे: HIN_ERV, BSI)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newNameHindi,
                        onValueChange = { newNameHindi = it },
                        label = { Text("हिन्दी नाम (जैसे: सरल हिन्दी बाइबिल)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newNameEnglish,
                        onValueChange = { newNameEnglish = it },
                        label = { Text("English Name (e.g., Easy-to-Read Version)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = newLanguage == "hi",
                            onClick = { newLanguage = "hi" },
                            label = { Text("हिन्दी (hi)") }
                        )
                        FilterChip(
                            selected = newLanguage == "en",
                            onClick = { newLanguage = "en" },
                            label = { Text("English (en)") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newId.isNotBlank() && newNameHindi.isNotBlank()) {
                            val registeredId = newId
                            val set = prefs.getStringSet("custom_translation_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                            set.add(registeredId)
                            prefs.edit()
                                .putStringSet("custom_translation_ids", set)
                                .putString("name_hi_$registeredId", newNameHindi)
                                .putString("name_en_$registeredId", newNameEnglish.ifBlank { newNameHindi })
                                .putString("lang_$registeredId", newLanguage)
                                .apply()

                            showAddCustomTranslationDialog = false
                            newId = ""
                            newNameHindi = ""
                            newNameEnglish = ""
                            Toast.makeText(context, "$registeredId अनुवाद पंजीकृत हुआ! ✅", Toast.LENGTH_SHORT).show()
                            refreshTranslationStats()
                        } else {
                            Toast.makeText(context, "कृपया ID और नाम भरें!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = newId.isNotBlank() && newNameHindi.isNotBlank()
                ) {
                    Text("पंजीकृत करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomTranslationDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
private fun TranslationItemCard(
    item: TranslationStatusItem,
    onDeleteClick: () -> Unit,
    onPurgeVerses: () -> Unit,
    onClearCommentaries: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDefault) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (item.isDefault) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (item.isDefault) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = item.id,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = item.nameHindi,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.nameEnglish,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (item.isDefault) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF16A34A).copy(alpha = 0.15f),
                        contentColor = Color(0xFF15803D)
                    ) {
                        Text(
                            text = "डिफ़ॉल्ट (Default)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stats row with verses, headings, and commentaries
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "वचन: ${item.verseCount}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (item.verseCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Title,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "शीर्षक: ${item.headingCount}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (item.commentaryCount > 0) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "टीका/Commentary: ${item.commentaryCount}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (item.commentaryCount > 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (item.commentaryCount > 0) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.commentaryCount > 0) {
                    OutlinedButton(
                        onClick = onClearCommentaries,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706))
                    ) {
                        Text("टीका हटाएं", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                if (!item.isDefault) {
                    if (item.verseCount > 0) {
                        OutlinedButton(
                            onClick = onPurgeVerses,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("वचन खाली करें", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
