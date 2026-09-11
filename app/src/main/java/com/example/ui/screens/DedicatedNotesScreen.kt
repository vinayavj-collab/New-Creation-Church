package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.local.DedicatedNoteEntity
import com.example.data.bible.repository.BibleRepository
import com.example.data.bible.repository.DedicatedNotesRepository
import com.example.ui.components.VersePopupDialog
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.util.DetectedVerseRef
import com.example.util.VerseReferenceDetector
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val NOTE_COLORS = listOf(
    Color(0xFFFFFBEB), // Soft Warm Amber
    Color(0xFFF0FDF4), // Soft Mint Green
    Color(0xFFF0F9FF), // Soft Sky Blue
    Color(0xFFFDF2F8), // Soft Rose Pink
    Color(0xFFFAF5FF), // Soft Lavender Purple
    Color(0xFFFFFFFF)  // Clean White
)

val NOTE_COLOR_HEXES = listOf(
    "#FFFBEB",
    "#F0FDF4",
    "#F0F9FF",
    "#FDF2F8",
    "#FAF5FF",
    "#FFFFFF"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DedicatedNotesScreen(
    notesRepository: DedicatedNotesRepository,
    bibleRepository: BibleRepository,
    onBackClick: () -> Unit,
    onOpenVerse: (bookId: Int, chapter: Int, verse: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val notes by notesRepository.getAllNotes().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var noteToEdit by remember { mutableStateOf<DedicatedNoteEntity?>(null) }
    var isFullScreenEditorOpen by remember { mutableStateOf(false) }

    // Active popup verse reference for modal dialog
    var activePopupVerse by remember { mutableStateOf<DetectedVerseRef?>(null) }

    // Editor state
    var editorTitle by remember { mutableStateOf("") }
    var editorContent by remember { mutableStateOf("") }
    var editorColorHex by remember { mutableStateOf("#FFFBEB") }
    var editorReferences by remember { mutableStateOf("") }

    // Speech-to-Text launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                editorContent = if (editorContent.isBlank()) spokenText else "$editorContent\n$spokenText"
            }
        }
    }

    val openVoiceInput = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "प्रचार नोट्स या वचन बोलें (Speak your study note)...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice typing is not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val autoDetectVerses = {
        val detected = VerseReferenceDetector.detectVerseReferences("$editorTitle\n$editorContent\n$editorReferences")
        if (detected.isNotEmpty()) {
            val formattedRefs = detected.joinToString(separator = ", ") { it.displayLabel }
            editorReferences = formattedRefs
            Toast.makeText(context, "${detected.size} वचन संदर्भ पहचाने व लिंक किए गए!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "कोई वचन संदर्भ नहीं मिला। (उदा. यूहन्ना 3:16, भजन 23:1-6 लिखें)", Toast.LENGTH_LONG).show()
        }
    }

    // Numbering helpers
    val insertNextNumber = {
        val lines = editorContent.split("\n")
        var highestNumber = 0
        val numberRegex = Regex("""^(\d+)\.\s*""")
        for (line in lines) {
            val match = numberRegex.find(line.trim())
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull() ?: 0
                if (num > highestNumber) highestNumber = num
            }
        }
        val nextNum = highestNumber + 1
        editorContent = if (editorContent.isBlank() || editorContent.endsWith("\n")) {
            "$editorContent$nextNum. "
        } else {
            "$editorContent\n$nextNum. "
        }
    }

    val insertBulletPoint = {
        editorContent = if (editorContent.isBlank() || editorContent.endsWith("\n")) {
            "$editorContent• "
        } else {
            "$editorContent\n• "
        }
    }

    val insertVerseSnippet = { verseHint: String ->
        editorContent = if (editorContent.isBlank() || editorContent.endsWith(" ") || editorContent.endsWith("\n")) {
            "$editorContent$verseHint "
        } else {
            "$editorContent $verseHint "
        }
    }

    // Save logic with comprehensive safety and error handling
    val saveCurrentNote = {
        if (editorTitle.isNotBlank() || editorContent.isNotBlank()) {
            coroutineScope.launch {
                try {
                    // Auto-detect any verses in title and content if references field is blank
                    val finalRefs = if (editorReferences.isBlank()) {
                        val detected = VerseReferenceDetector.detectVerseReferences("$editorTitle\n$editorContent")
                        detected.joinToString(separator = ", ") { it.displayLabel }
                    } else {
                        editorReferences.trim()
                    }

                    val current = noteToEdit
                    if (current == null) {
                        notesRepository.insertNote(
                            DedicatedNoteEntity(
                                title = editorTitle.trim(),
                                content = editorContent.trim(),
                                colorHex = editorColorHex,
                                linkedReferences = finalRefs,
                                createdAt = System.currentTimeMillis(),
                                modifiedAt = System.currentTimeMillis()
                            )
                        )
                        Toast.makeText(context, "नया प्रचार नोट सुरक्षित किया गया!", Toast.LENGTH_SHORT).show()
                    } else {
                        notesRepository.updateNote(
                            current.copy(
                                title = editorTitle.trim(),
                                content = editorContent.trim(),
                                colorHex = editorColorHex,
                                linkedReferences = finalRefs,
                                modifiedAt = System.currentTimeMillis()
                            )
                        )
                        Toast.makeText(context, "प्रचार नोट अपडेट किया गया!", Toast.LENGTH_SHORT).show()
                    }
                    isFullScreenEditorOpen = false
                    noteToEdit = null
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "नोट सुरक्षित करने में त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, "कृपया शीर्षक या नोट लिखें", Toast.LENGTH_SHORT).show()
        }
    }

    // If Full-Screen Editor is active
    if (isFullScreenEditorOpen) {
        BackHandler {
            if (editorTitle.isNotBlank() || editorContent.isNotBlank()) {
                saveCurrentNote()
            } else {
                isFullScreenEditorOpen = false
            }
        }

        val detectedVersesInEditor = remember(editorTitle, editorContent, editorReferences) {
            VerseReferenceDetector.detectVerseReferences("$editorTitle\n$editorContent\n$editorReferences")
        }

        val editorBgColor = try {
            Color(android.graphics.Color.parseColor(editorColorHex))
        } catch (e: Exception) {
            Color(0xFFFFFBEB)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (noteToEdit == null) "नया प्रचार नोट" else "प्रचार नोट संपादित करें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (editorTitle.isNotBlank() || editorContent.isNotBlank()) {
                                saveCurrentNote()
                            } else {
                                isFullScreenEditorOpen = false
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = openVoiceInput) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = NavyPrimary)
                        }
                        IconButton(onClick = { autoDetectVerses() }) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Auto Detect Verses", tint = NavyPrimary)
                        }
                        Button(
                            onClick = { saveCurrentNote() },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = editorBgColor
                    )
                )
            },
            bottomBar = {
                // Editor Quick Action Bar (Numbering, Bullets, Verse templates, Color picker)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        // Color picker + formatting tools
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Formatting buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = insertNextNumber,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbering", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("1. 2. 3.", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = insertBulletPoint,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullets", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("• Bullet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Color Dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NOTE_COLOR_HEXES.forEachIndexed { index, hex ->
                                    val isSelected = editorColorHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(NOTE_COLORS[index])
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) NavyPrimary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable { editorColorHex = hex }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Verse Shortcut Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("वचन जोड़ें:", style = MaterialTheme.typography.labelSmall, color = NavyPrimary, fontWeight = FontWeight.Bold)
                            listOf("यूहन्ना 3:16", "भजन 23:1-6", "रोमियों 8:28", "फिलिप्पियों 4:13", "इब्रानियों 11:1", "यशायाह 40:31", "मत्ती 28:19-20").forEach { verseChip ->
                                SuggestionChip(
                                    onClick = { insertVerseSnippet(verseChip) },
                                    label = { Text(verseChip, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(editorBgColor)
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title Field
                TextField(
                    value = editorTitle,
                    onValueChange = { editorTitle = it },
                    placeholder = {
                        Text(
                            "प्रचार नोट का शीर्षक (Title)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = NavyPrimary,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Live Detected Verses Chips Header
                if (detectedVersesInEditor.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E7FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = Color(0xFF3730A3),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "पहचाने गए वचन (${detectedVersesInEditor.size}) - टैप करके पढ़ें:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3730A3)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                detectedVersesInEditor.forEach { detected ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        modifier = Modifier.clickable {
                                            activePopupVerse = detected
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = detected.displayLabel,
                                                color = Color(0xFF3730A3),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.VolumeUp,
                                                contentDescription = null,
                                                tint = GoldWarm,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Note Content Field
                TextField(
                    value = editorContent,
                    onValueChange = { editorContent = it },
                    placeholder = {
                        Text(
                            "यहाँ अपने नोट्स, प्रचार के बिंदु, और वचन लिखें...\nउदा. 1. परमेश्वर का प्रेम - यूहन्ना 3:16\n2. जीवन का मार्ग - भजन 23:1-6\n3. विश्वास की जय - इब्रानियों 11:1",
                            color = Color.Gray,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, color = Color(0xFF1E293B)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .defaultMinSize(minHeight = 350.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom References Field
                OutlinedTextField(
                    value = editorReferences,
                    onValueChange = { editorReferences = it },
                    label = { Text("अतिरिक्त वचन संदर्भ (Optional Scripture Refs)") },
                    placeholder = { Text("उदा. यूहन्ना 3:16, भजन 23:1-6") },
                    leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }
    } else {
        // Main Notes List Screen
        val filteredNotes = remember(notes, searchQuery) {
            if (searchQuery.isBlank()) notes
            else {
                val q = searchQuery.trim().lowercase()
                notes.filter {
                    it.title.lowercase().contains(q) ||
                    it.content.lowercase().contains(q) ||
                    it.linkedReferences.lowercase().contains(q)
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("प्रचार एवं अध्ययन नोट्स", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("वचन लिंक, ऑडियो व संदर्भ पहचान के साथ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        noteToEdit = null
                        editorTitle = ""
                        editorContent = ""
                        editorColorHex = "#FFFBEB"
                        editorReferences = ""
                        isFullScreenEditorOpen = true
                    },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                    text = { Text("नया प्रचार नोट लिखें (Add Note)") },
                    containerColor = NavyPrimary,
                    contentColor = Color.White
                )
            },
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("नोट्स, विषय या वचन खोजें (उदा. यूहन्ना, भजन, विश्वास)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = NavyPrimary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isBlank())
                                    "अभी कोई प्रचार नोट्स नहीं हैं।\nनया नोट लिखने के लिए नीचे + बटन दबाएं!"
                                else "कोई नोट्स नहीं मिला: '$searchQuery'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            val cardBg = try {
                                Color(android.graphics.Color.parseColor(note.colorHex))
                            } catch (e: Exception) {
                                Color(0xFFFFFBEB)
                            }

                            val detectedRefs = remember(note.content, note.linkedReferences, note.title) {
                                val combined = "${note.linkedReferences}\n${note.title}\n${note.content}"
                                VerseReferenceDetector.detectVerseReferences(combined)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        noteToEdit = note
                                        editorTitle = note.title
                                        editorContent = note.content
                                        editorColorHex = note.colorHex
                                        editorReferences = note.linkedReferences
                                        isFullScreenEditorOpen = true
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = note.title.ifBlank { "बिना शीर्षक का नोट" },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        notesRepository.deleteNote(note.id)
                                                        Toast.makeText(context, "नोट हटाया गया", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = Color(0xFF991B1B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    if (note.content.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF334155),
                                            maxLines = 5
                                        )
                                    }

                                    // Interactive Detected Verse Links
                                    if (detectedRefs.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "पहचाने गए वचन (Tap to open Pop-up with Audio):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NavyPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            detectedRefs.forEach { verseRef ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFEEF2FF),
                                                    modifier = Modifier.clickable {
                                                        activePopupVerse = verseRef
                                                    }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.AutoMirrored.Filled.MenuBook,
                                                            contentDescription = null,
                                                            tint = Color(0xFF4338CA),
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = verseRef.displayLabel,
                                                            color = Color(0xFF3730A3),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            Icons.Default.Visibility,
                                                            contentDescription = null,
                                                            tint = Color(0xFF4338CA),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else if (note.linkedReferences.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFE0E7FF))
                                                .clickable {
                                                    val parsed = VerseReferenceDetector.parseSingleReference(note.linkedReferences)
                                                    if (parsed != null) {
                                                        activePopupVerse = parsed
                                                    } else {
                                                        onOpenVerse(43, 3, 16)
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF3730A3))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = note.linkedReferences,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF3730A3))
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(note.modifiedAt))
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Verse Popup Dialog with Verse Range, Audio TTS, Full Chapter Navigation, & Default Translation Selection
    if (activePopupVerse != null) {
        VersePopupDialog(
            verseRef = activePopupVerse!!,
            bibleRepository = bibleRepository,
            onDismiss = { activePopupVerse = null },
            onOpenFullChapter = { bId, ch, v ->
                activePopupVerse = null
                onOpenVerse(bId, ch, v)
            }
        )
    }
}
