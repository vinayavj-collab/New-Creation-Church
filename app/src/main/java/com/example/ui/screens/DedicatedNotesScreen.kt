package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    Color(0xFFFFFBEB), // Soft Warm Yellow
    Color(0xFFF0FDF4), // Soft Mint Green
    Color(0xFFF0F9FF), // Soft Sky Blue
    Color(0xFFFDF2F8), // Soft Rose Pink
    Color(0xFFFAF5FF)  // Soft Lavender Purple
)

val NOTE_COLOR_HEXES = listOf(
    "#FFFBEB",
    "#F0FDF4",
    "#F0F9FF",
    "#FDF2F8",
    "#FAF5FF"
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
    var showEditor by remember { mutableStateOf(false) }

    // Active popup verse reference for the modal dialog
    var activePopupVerse by remember { mutableStateOf<DetectedVerseRef?>(null) }

    // Editor field states
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
                putExtra(RecognizerIntent.EXTRA_PROMPT, "प्रचार नोट्स या विचार बोलें (Speak your note content)...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val autoDetectVersesInEditor = {
        val detected = VerseReferenceDetector.detectVerseReferences("$editorTitle\n$editorContent\n$editorReferences")
        if (detected.isNotEmpty()) {
            val formattedRefs = detected.joinToString(separator = ", ") { it.displayLabel }
            editorReferences = formattedRefs
            Toast.makeText(context, "${detected.size} वचन संदर्भ पहचाने गए!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "नोट्स में कोई वचन संदर्भ नहीं मिला। (उदा. यूहन्ना 3:16, भजन 23:1-6 लिखें)", Toast.LENGTH_LONG).show()
        }
    }

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
                        Text("प्रचार एवं अध्ययन नोट्स (Sermon Notes)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                    showEditor = true
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

                        // Auto-detect any verse references inside this note (content + linkedReferences)
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
                                    showEditor = true
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
                                                notesRepository.deleteNote(note.id)
                                                Toast.makeText(context, "नोट हटाया गया", Toast.LENGTH_SHORT).show()
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

        // Note Editor Dialog
        if (showEditor) {
            AlertDialog(
                onDismissRequest = { showEditor = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = NavyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (noteToEdit == null) "नया प्रचार नोट (New Note)" else "प्रचार नोट संपादित करें",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Color Selector
                        Text("नोट का रंग चुनें:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NOTE_COLOR_HEXES.forEachIndexed { index, hex ->
                                val isSelected = editorColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(NOTE_COLORS[index])
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) NavyPrimary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { editorColorHex = hex }
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = NavyPrimary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }

                        // Title
                        OutlinedTextField(
                            value = editorTitle,
                            onValueChange = { editorTitle = it },
                            label = { Text("शीर्षक (Title)*") },
                            placeholder = { Text("उदा. रविवार की आराधना - विश्वास की सामर्थ") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Content with Voice Input
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = editorContent,
                                onValueChange = { editorContent = it },
                                label = { Text("प्रचार नोट व विचार (Note Content)*") },
                                placeholder = { Text("प्रचार के मुख्य बिंदु लिखें...\nउदा. आज के वचन: यूहन्ना 3:16-18, भजन 23:1-6, रोमियों 8:28...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                maxLines = 10
                            )
                            IconButton(
                                onClick = openVoiceInput,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 4.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = NavyPrimary)
                            }
                        }

                        // Auto Detect Verses Button + References Field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("वचन संदर्भ (Scripture Refs):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = { autoDetectVersesInEditor() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("वचन पहचानें (Auto-Detect)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                        }

                        OutlinedTextField(
                            value = editorReferences,
                            onValueChange = { editorReferences = it },
                            placeholder = { Text("उदा. यूहन्ना 3:16, भजन 23:1-6, रोमियों 8:28") },
                            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Quick Reference Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("भजन 23:1-6", "यूहन्ना 3:16", "रोमियों 8:28", "फिलिप्पियों 4:13", "इब्रानियों 11:1").forEach { chipRef ->
                                SuggestionChip(
                                    onClick = {
                                        editorReferences = if (editorReferences.isBlank()) chipRef else "$editorReferences, $chipRef"
                                    },
                                    label = { Text("+ $chipRef", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editorTitle.isNotBlank() || editorContent.isNotBlank()) {
                                coroutineScope.launch {
                                    // Also auto-detect if references is blank but content has verses
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
                                                linkedReferences = finalRefs
                                            )
                                        )
                                        Toast.makeText(context, "नोट सुरक्षित किया गया", Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(context, "नोट अपडेट किया गया", Toast.LENGTH_SHORT).show()
                                    }
                                    showEditor = false
                                }
                            } else {
                                Toast.makeText(context, "कृपया कुछ शीर्षक या नोट लिखें", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(if (noteToEdit == null) "सुरक्षित करें (Save)" else "अपडेट करें")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditor = false }) {
                        Text("रद्द करें")
                    }
                }
            )
        }
    }
}
