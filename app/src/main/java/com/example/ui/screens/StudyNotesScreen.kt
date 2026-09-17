package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.bible.local.StudyNoteEntity
import com.example.data.bible.repository.StudyNotesRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyNotesScreen(
    studyNotesRepository: StudyNotesRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var notesList by remember { mutableStateOf<List<StudyNoteEntity>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("DateDesc") } // "DateDesc" or "SerialAsc"
    var showSortDropdown by remember { mutableStateOf(false) }

    // Screen navigation flow:
    // 0 = List View
    // 1 = Step 1 Metadata Dialog is active
    // 2 = Step 2 Full-Screen WYSIWYG Editor
    var currentScreenState by remember { mutableIntStateOf(0) }
    var showMetadataDialog by remember { mutableStateOf(false) }

    // Active Note State
    var activeNoteId by remember { mutableLongStateOf(0L) }
    var activeTitle by remember { mutableStateOf("") }
    var activeSerialNumber by remember { mutableStateOf("") }
    var activeTag by remember { mutableStateOf("") }
    var activeDate by remember { mutableStateOf("") }
    var activeTime by remember { mutableStateOf("") }
    var activeContent by remember { mutableStateOf(TextFieldValue("")) }

    // Delete PIN Confirmation
    var showDeleteSheet by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<StudyNoteEntity?>(null) }
    var generatedPin by remember { mutableStateOf("") }
    var enteredPin by remember { mutableStateOf("") }

    // Load notes
    LaunchedEffect(searchQuery, sortMode) {
        try {
            val flow = if (searchQuery.isNotBlank()) {
                studyNotesRepository.searchNotes(searchQuery)
            } else if (sortMode == "SerialAsc") {
                studyNotesRepository.getAllNotesByIdAsc()
            } else {
                studyNotesRepository.getAllNotesByDate()
            }
            flow.collectLatest { list ->
                notesList = list ?: emptyList()
            }
        } catch (e: Exception) {
            notesList = emptyList()
        }
    }

    // Collect all existing tags for auto-complete
    val existingTags = remember(notesList) {
        val defaultTags = listOf("Sunday Sermon", "Bible Study", "Youth Meeting", "Prayer", "Devotional", "Worship")
        val fromDb = notesList.flatMap { it.tags.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
        (fromDb + defaultTags).distinct()
    }

    // Handle system back button for 2-step flow
    BackHandler(enabled = currentScreenState == 2 || showMetadataDialog) {
        if (currentScreenState == 2) {
            currentScreenState = 0
        } else if (showMetadataDialog) {
            showMetadataDialog = false
        }
    }

    // ----------------------------------------------------
    // STEP 2: STRICTLY FULL-SCREEN WYSIWYG EDITOR
    // ----------------------------------------------------
    if (currentScreenState == 2) {
        FullScreenNoteEditor(
            title = activeTitle,
            serialNumber = activeSerialNumber,
            tag = activeTag,
            date = activeDate,
            time = activeTime,
            initialContent = activeContent,
            studyNotesRepository = studyNotesRepository,
            onClose = {
                currentScreenState = 0
            },
            onSave = { finalContentText ->
                coroutineScope.launch {
                    try {
                        // Encode serial number and tag into tags field (or title) for persistent storage
                        val formattedTags = if (activeSerialNumber.isNotBlank()) {
                            "SN#$activeSerialNumber, $activeTag".trim().removeSuffix(",")
                        } else {
                            activeTag
                        }

                        val entity = StudyNoteEntity(
                            noteId = if (activeNoteId == 0L) 0L else activeNoteId,
                            title = activeTitle.ifBlank { "Sermon Note #$activeSerialNumber" },
                            tags = formattedTags,
                            date = activeDate.ifBlank {
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            },
                            time = activeTime.ifBlank {
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                            },
                            content = finalContentText
                        )
                        studyNotesRepository.insertNote(entity)
                        Toast.makeText(context, "नोट सफलतापूर्वक सहेजा गया (Note Saved)", Toast.LENGTH_SHORT).show()
                        currentScreenState = 0
                    } catch (e: Exception) {
                        Toast.makeText(context, "त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        return
    }

    // ----------------------------------------------------
    // STEP 1: METADATA ENTRY (INITIAL UI DIALOG)
    // ----------------------------------------------------
    if (showMetadataDialog) {
        NoteMetadataDialog(
            initialTitle = activeTitle,
            initialSerialNumber = activeSerialNumber,
            initialTag = activeTag,
            availableTags = existingTags,
            onDismiss = { showMetadataDialog = false },
            onContinue = { title, serialNo, tag ->
                activeTitle = title
                activeSerialNumber = serialNo
                activeTag = tag
                showMetadataDialog = false
                currentScreenState = 2 // Move directly to Step 2 Full Window Editor
            }
        )
    }

    // ----------------------------------------------------
    // MAIN NOTES LIST SCREEN
    // ----------------------------------------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "सरमन एवं स्टडी नोट्स (Sermon Notes)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "कुल ${notesList.size} नोट्स",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortDropdown = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortDropdown,
                            onDismissRequest = { showSortDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("तारीख अनुसार (नवीनतम / Newest)") },
                                onClick = {
                                    sortMode = "DateDesc"
                                    showSortDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("सीरियल नंबर अनुसार (Serial No.)") },
                                onClick = {
                                    sortMode = "SerialAsc"
                                    showSortDropdown = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // Reset fields for new note
                    activeNoteId = 0L
                    activeTitle = ""
                    // Auto-increment serial number suggestion
                    val nextSerial = (notesList.size + 1).toString().padStart(2, '0')
                    activeSerialNumber = nextSerial
                    activeTag = "Sunday Sermon"
                    val cal = Calendar.getInstance()
                    activeDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                    activeTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
                    activeContent = TextFieldValue("")
                    showMetadataDialog = true // Trigger Step 1 Initial UI
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("नया नोट (Create Note)", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(horizontal = 16.dp)
        ) {
            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("शीर्षक, टैग या विषय खोजें... (Search notes)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (notesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "कोई सरमन नोट नहीं मिला",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "नीचे दिए गए '+ नया नोट' बटन पर क्लिक करके\nपहला नोट बनाएं।",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notesList, key = { it.noteId }) { note ->
                        // Extract Serial Number if available in tags
                        val parsedSerial = note.tags.split(",")
                            .firstOrNull { it.trim().startsWith("SN#") }
                            ?.replace("SN#", "")
                            ?.trim() ?: "${note.noteId}"

                        val cleanTags = note.tags.split(",")
                            .filterNot { it.trim().startsWith("SN#") }
                            .joinToString(", ")
                            .trim()

                        StudyNoteCardItem(
                            note = note,
                            serialNumber = parsedSerial,
                            displayTags = cleanTags,
                            onEdit = {
                                activeNoteId = note.noteId
                                activeTitle = note.title
                                activeSerialNumber = parsedSerial
                                activeTag = cleanTags.ifBlank { "Sunday Sermon" }
                                activeDate = note.date
                                activeTime = note.time
                                activeContent = TextFieldValue(note.content)
                                // Open Step 1 Metadata Dialog for editing, then proceed to editor
                                showMetadataDialog = true
                            },
                            onOpenDirectEditor = {
                                activeNoteId = note.noteId
                                activeTitle = note.title
                                activeSerialNumber = parsedSerial
                                activeTag = cleanTags.ifBlank { "Sunday Sermon" }
                                activeDate = note.date
                                activeTime = note.time
                                activeContent = TextFieldValue(note.content)
                                currentScreenState = 2 // Direct to full screen editor
                            },
                            onShare = {
                                try {
                                    val shareText = "📝 Sermon Note #$parsedSerial: ${note.title}\n🏷️ Tags: $cleanTags\n📅 Date: ${note.date} ${note.time}\n\n${note.content}"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, note.title)
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Sermon Note"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to share note", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                noteToDelete = note
                                generatedPin = (1000..9999).random().toString()
                                enteredPin = ""
                                showDeleteSheet = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Secure Delete PIN Bottom Sheet
    if (showDeleteSheet && noteToDelete != null) {
        ModalBottomSheet(onDismissRequest = { showDeleteSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("सुरक्षित विलोपन (Secure Deletion)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "नोट '${noteToDelete?.title}' को हटाने के लिए नीचे दिया गया 4-अंकों का पिन दर्ज करें:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "सुरक्षा पिन (PIN): $generatedPin",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { if (it.length <= 4) enteredPin = it },
                    label = { Text("Enter 4-digit PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteSheet = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("रद्द करें")
                    }
                    Button(
                        onClick = {
                            if (enteredPin == generatedPin) {
                                coroutineScope.launch {
                                    try {
                                        noteToDelete?.let {
                                            studyNotesRepository.deleteNote(it.noteId)
                                        }
                                        Toast.makeText(context, "नोट हटा दिया गया", Toast.LENGTH_SHORT).show()
                                        showDeleteSheet = false
                                        noteToDelete = null
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "हटाने में त्रुटि", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "गलत पिन दर्ज किया गया!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("हटाएं")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Step 1: Metadata Entry Dialog (Clean Centered Dialog with Title, Serial Number, Tag auto-complete)
 */
@Composable
fun NoteMetadataDialog(
    initialTitle: String,
    initialSerialNumber: String,
    initialTag: String,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onContinue: (title: String, serialNo: String, tag: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var serialNumber by remember { mutableStateOf(initialSerialNumber) }
    var tag by remember { mutableStateOf(initialTag) }
    val focusManager = LocalFocusManager.current

    val isFormValid = title.isNotBlank() && serialNumber.isNotBlank() && tag.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PostAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "चरण 1: नोट विवरण (Step 1)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "शीर्षक, क्रमांक और टैग दर्ज करें",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Field 1: Title (Mandatory)
                Column {
                    Text(
                        text = "1. शीर्षक (Title) *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("जैसे: परमेश्वर का अनुग्रह / God's Grace") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        )
                    )
                }

                // Field 2: Serial Number (Mandatory)
                Column {
                    Text(
                        text = "2. सीरियल नंबर (Serial Number) *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = serialNumber,
                        onValueChange = { serialNumber = it },
                        placeholder = { Text("जैसे: 01, 02, SER-101") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )
                }

                // Field 3: Tag / Category (Mandatory, with Autocomplete Chips)
                Column {
                    Text(
                        text = "3. टैग / श्रेणी (Tag / Category) *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tag,
                        onValueChange = { tag = it },
                        placeholder = { Text("जैसे: Sunday Sermon, Bible Study") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (isFormValid) {
                                    onContinue(title.trim(), serialNumber.trim(), tag.trim())
                                }
                            }
                        )
                    )

                    // Autocomplete / Quick Suggestion Chips
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "सुझाए गए टैग (Quick Tags):",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableTags) { sugTag ->
                            val isSelected = tag.equals(sugTag, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { tag = sugTag },
                                label = { Text(sugTag, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Actions: Continue Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("रद्द करें")
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                onContinue(title.trim(), serialNumber.trim(), tag.trim())
                            }
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("आगे बढ़ें (Continue ➔)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Step 2: STRICTLY FULL-SCREEN NOTE-TAKING EDITOR
 * Matches the reference layout with:
 * - Top WYSIWYG formatting toolbar (B, I, U, H1, Link, etc.)
 * - Massive full-window editable area
 * - Anchored bottom "Save / Close" (सहेजें / बन्द करें) buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenNoteEditor(
    title: String,
    serialNumber: String,
    tag: String,
    date: String,
    time: String,
    initialContent: TextFieldValue,
    studyNotesRepository: StudyNotesRepository? = null,
    onClose: () -> Unit,
    onSave: (String) -> Unit
) {
    var contentValue by remember { mutableStateOf(initialContent) }
    var showQuoteScriptureDialog by remember { mutableStateOf(false) }

    // Helper to insert scripture HTML block or markdown at cursor
    fun injectHtmlAtCursor(htmlBlock: String) {
        val currentText = contentValue.text
        val selection = contentValue.selection
        val start = selection.min.coerceIn(0, currentText.length)
        val end = selection.max.coerceIn(0, currentText.length)

        val prefixSpacing = if (start > 0 && !currentText.substring(0, start).endsWith("\n")) "\n\n" else ""
        val suffixSpacing = if (end < currentText.length && !currentText.substring(end).startsWith("\n")) "\n\n" else "\n"
        val formattedInsertion = "$prefixSpacing$htmlBlock$suffixSpacing"

        val newText = currentText.substring(0, start) + formattedInsertion + currentText.substring(end)
        val newCursor = start + formattedInsertion.length

        contentValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    // Helper to insert markdown or text wrap
    fun applyFormatting(prefix: String, suffix: String = "") {
        val currentText = contentValue.text
        val selection = contentValue.selection

        val start = selection.min.coerceIn(0, currentText.length)
        val end = selection.max.coerceIn(0, currentText.length)

        val selectedText = currentText.substring(start, end)
        val replacement = if (selectedText.isNotEmpty()) {
            "$prefix$selectedText$suffix"
        } else {
            "$prefix$suffix"
        }

        val newText = currentText.substring(0, start) + replacement + currentText.substring(end)
        val newCursor = start + prefix.length + selectedText.length

        contentValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    if (showQuoteScriptureDialog && studyNotesRepository != null) {
        com.example.ui.components.QuoteScriptureDialog(
            studyNotesRepository = studyNotesRepository,
            onDismiss = { showQuoteScriptureDialog = false },
            onScriptureEmbed = { htmlBlock ->
                injectHtmlAtCursor(htmlBlock)
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ----------------------------------------------------
            // 1. TOP STATUS & INFO BAR
            // ----------------------------------------------------
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Serial Number Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "#$serialNumber",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$date • $time",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Quick Save Action Icon
                    IconButton(
                        onClick = { onSave(contentValue.text) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ----------------------------------------------------
            // 2. WYSIWYG FORMATTING TOOLBAR (Anchored at Top)
            // ----------------------------------------------------
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bold
                    item {
                        ToolbarButton(label = "B", bold = true) {
                            applyFormatting("**", "**")
                        }
                    }

                    // Italic
                    item {
                        ToolbarButton(label = "I", italic = true) {
                            applyFormatting("*", "*")
                        }
                    }

                    // Underline
                    item {
                        ToolbarButton(label = "U", underline = true) {
                            applyFormatting("<u>", "</u>")
                        }
                    }

                    // Strikethrough
                    item {
                        ToolbarButton(label = "S", strikethrough = true) {
                            applyFormatting("~~", "~~")
                        }
                    }

                    // Heading 1
                    item {
                        ToolbarButton(label = "H1", bold = true) {
                            applyFormatting("\n# ")
                        }
                    }

                    // Heading 2
                    item {
                        ToolbarButton(label = "H2", bold = true) {
                            applyFormatting("\n## ")
                        }
                    }

                    // Bullet list
                    item {
                        ToolbarButton(label = "• List") {
                            applyFormatting("\n• ")
                        }
                    }

                    // Numbered list
                    item {
                        ToolbarButton(label = "1. List") {
                            applyFormatting("\n1. ")
                        }
                    }

                    // Blockquote
                    item {
                        ToolbarButton(label = "❝ Quote") {
                            applyFormatting("\n> ")
                        }
                    }

                    // Link
                    item {
                        ToolbarButton(label = "🔗 Link") {
                            applyFormatting("[Link Text](", ")")
                        }
                    }

                    // Quote Scripture (Auto-Embed)
                    item {
                        ToolbarButton(
                            label = "📖 वचन जोड़ें (Quote Scripture)",
                            bold = true
                        ) {
                            if (studyNotesRepository != null) {
                                showQuoteScriptureDialog = true
                            } else {
                                applyFormatting(" [यूहन्ना 3:16] ")
                            }
                        }
                    }

                    // Divider
                    item {
                        ToolbarButton(label = "— Line") {
                            applyFormatting("\n---\n")
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 3. MASSIVE FULL-SCREEN EDITABLE TEXT AREA
            // ----------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = contentValue,
                    onValueChange = { contentValue = it },
                    placeholder = {
                        Text(
                            text = "यहाँ अपना उपदेश / स्टडी नोट्स लिखें...\n(Type sermon message, scripture references, points, prayer requests here...)",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier.fillMaxSize(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            // ----------------------------------------------------
            // 4. BOTTOM ACTION BAR (Save / Close Buttons)
            // ----------------------------------------------------
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Character / Word count info
                    val wordCount = remember(contentValue.text) {
                        contentValue.text.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
                    }
                    Text(
                        text = "${contentValue.text.length} वर्ण • $wordCount शब्द",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    )

                    // Buttons: Close and Save
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onClose,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("बन्द करें", fontSize = 13.sp)
                        }

                        Button(
                            onClick = { onSave(contentValue.text) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("सहेजें (Save)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * WYSIWYG Toolbar Small Action Button
 */
@Composable
private fun ToolbarButton(
    label: String,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    strikethrough: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .height(34.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
                    fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                    textDecoration = if (underline) androidx.compose.ui.text.style.TextDecoration.Underline
                    else if (strikethrough) androidx.compose.ui.text.style.TextDecoration.LineThrough
                    else androidx.compose.ui.text.style.TextDecoration.None,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

/**
 * Study Note List Item Card
 */
@Composable
fun StudyNoteCardItem(
    note: StudyNoteEntity,
    serialNumber: String,
    displayTags: String,
    onEdit: () -> Unit,
    onOpenDirectEditor: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDirectEditor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Serial Number Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "#$serialNumber",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (displayTags.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = displayTags,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${note.date} • ${note.time}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.title.ifBlank { "Untitled Note" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.content.ifBlank { "(खाली नोट / Empty note content)" },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = "Edit Details", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onOpenDirectEditor, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Open Editor", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
