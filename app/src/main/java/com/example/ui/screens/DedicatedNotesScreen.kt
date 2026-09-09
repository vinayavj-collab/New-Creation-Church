package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.local.DedicatedNoteEntity
import com.example.data.bible.model.BibleBookDefinitions
import com.example.data.bible.repository.DedicatedNotesRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val NOTE_COLORS = listOf(
    Color(0xFFFFFBEB), // Soft Yellow
    Color(0xFFF0FDF4), // Soft Green
    Color(0xFFF0F9FF), // Soft Blue
    Color(0xFFFDF2F8), // Soft Pink
    Color(0xFFFAF5FF)  // Soft Purple
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
                editorContent = if (editorContent.isBlank()) spokenText else "$editorContent $spokenText"
            }
        }
    }

    val openVoiceInput = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your note content...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input not available on this device", Toast.LENGTH_SHORT).show()
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
                title = { Text("My Notes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    noteToEdit = null
                    editorTitle = ""
                    editorContent = ""
                    editorColorHex = "#FFFBEB"
                    editorReferences = ""
                    showEditor = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
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
                placeholder = { Text("Search notes, topics or verses...") },
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
                shape = RoundedCornerShape(12.dp),
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
                            Icons.Default.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No notes yet. Tap + to write your first note!" else "No notes found matching '$searchQuery'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        val cardBg = try {
                            Color(android.graphics.Color.parseColor(note.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.surfaceVariant
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
                                        text = note.title.ifBlank { "Untitled Note" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                notesRepository.deleteNote(note.id)
                                                Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
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
                                        maxLines = 4
                                    )
                                }

                                if (note.linkedReferences.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = note.linkedReferences,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.clickable {
                                                // Parse first referenced book if possible
                                                val ref = note.linkedReferences.trim()
                                                val book = BibleBookDefinitions.books.firstOrNull {
                                                    ref.startsWith(it.nameEnglish, ignoreCase = true) ||
                                                    ref.startsWith(it.nameHindi, ignoreCase = true)
                                                }
                                                if (book != null) {
                                                    onOpenVerse(book.id, 1, 1)
                                                } else {
                                                    onOpenVerse(43, 3, 16) // default John 3
                                                }
                                            }
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

        // Editor Dialog
        if (showEditor) {
            AlertDialog(
                onDismissRequest = { showEditor = false },
                title = { Text(if (noteToEdit == null) "New Note" else "Edit Note", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editorTitle,
                            onValueChange = { editorTitle = it },
                            label = { Text("Title") },
                            placeholder = { Text("E.g. Sunday Sermon Reflection") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editorContent,
                            onValueChange = { editorContent = it },
                            label = { Text("Note content") },
                            placeholder = { Text("Type or use speech-to-text...") },
                            trailingIcon = {
                                IconButton(onClick = openVoiceInput) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            maxLines = 6
                        )

                        OutlinedTextField(
                            value = editorReferences,
                            onValueChange = { editorReferences = it },
                            label = { Text("Bible References (optional)") },
                            placeholder = { Text("E.g. John 3:16, Psalm 23:1") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Color picker
                        Text("Note Color", style = MaterialTheme.typography.labelMedium)
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
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { editorColorHex = hex }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val current = noteToEdit
                                if (current == null) {
                                    notesRepository.insertNote(
                                        DedicatedNoteEntity(
                                            title = editorTitle.trim(),
                                            content = editorContent.trim(),
                                            colorHex = editorColorHex,
                                            linkedReferences = editorReferences.trim()
                                        )
                                    )
                                } else {
                                    notesRepository.updateNote(
                                        current.copy(
                                            title = editorTitle.trim(),
                                            content = editorContent.trim(),
                                            colorHex = editorColorHex,
                                            linkedReferences = editorReferences.trim(),
                                            modifiedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                                showEditor = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditor = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
