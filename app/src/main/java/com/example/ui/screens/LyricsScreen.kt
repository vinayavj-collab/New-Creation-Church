package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.local.ChristianSongEntity
import com.example.data.bible.model.BibleBookDefinitions
import com.example.data.bible.repository.BibleRepository
import com.example.data.bible.repository.LyricsRepository
import com.example.ui.components.VersePopupDialog
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.util.DetectedVerseRef
import com.example.util.VerseReferenceDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LYRICS_NOTE_COLORS = listOf(
    Color(0xFFFFFBEB), // Warm Yellow
    Color(0xFFF0FDF4), // Soft Green
    Color(0xFFF0F9FF), // Soft Blue
    Color(0xFFFDF2F8), // Soft Pink
    Color(0xFFFAF5FF)  // Soft Purple
)

val LYRICS_NOTE_COLOR_HEXES = listOf(
    "#FFFBEB",
    "#F0FDF4",
    "#F0F9FF",
    "#FDF2F8",
    "#FAF5FF"
)

enum class SongBookIndexMode {
    BY_NUMBER,       // गीत संख्या अनुसार (1, 2, 3...)
    BY_ALPHABET,     // वर्णमाला अनुक्रमणिका (अ, आ, क, ख... / A-Z)
    BY_CATEGORY,     // विषयवार अनुक्रमणिका (Worship, Praise, Prayer, Sadri...)
    FAVORITES        // पसंदीदा गीत
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    lyricsRepository: LyricsRepository,
    onBackClick: () -> Unit,
    onOpenVerse: ((bookId: Int, chapter: Int, verse: Int?) -> Unit)? = null,
    bibleRepository: BibleRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isSyncingBlog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        lyricsRepository.initializePreloadedLyrics()
        // Proactively fetch blog lyrics in background
        try {
            lyricsRepository.fetchAndSyncLyricsFromBlogs()
        } catch (_: Exception) {}
    }

    val allSongs by lyricsRepository.getAllSongs().collectAsState(initial = emptyList())
    var indexMode by remember { mutableStateOf(SongBookIndexMode.BY_NUMBER) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedAlphabet by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeSong by remember { mutableStateOf<ChristianSongEntity?>(null) }

    // Active popup verse for Scripture Modal with TTS
    var activePopupVerse by remember { mutableStateOf<DetectedVerseRef?>(null) }

    // Dialog states
    var showAddSongDialog by remember { mutableStateOf(false) }
    var showJumpByNumberDialog by remember { mutableStateOf(false) }
    var songToEdit by remember { mutableStateOf<ChristianSongEntity?>(null) }

    // Form fields for adding/editing a song (with complete Notes capabilities)
    var formSongNumber by remember { mutableStateOf("") }
    var formTitle by remember { mutableStateOf("") }
    var formArtist by remember { mutableStateOf("Vinay Kumar AVJ") }
    var formCategory by remember { mutableStateOf("स्तुति व आराधना") }
    var formKeyScale by remember { mutableStateOf("D") }
    var formContent by remember { mutableStateOf("") }
    var formColorHex by remember { mutableStateOf("#FFFBEB") }
    var formReferences by remember { mutableStateOf("") }
    var formPersonalNotes by remember { mutableStateOf("") }

    val autoDetectVersesInSongEditor = {
        val detected = VerseReferenceDetector.detectVerseReferences("$formTitle\n$formContent\n$formReferences\n$formPersonalNotes")
        if (detected.isNotEmpty()) {
            val formatted = detected.joinToString(separator = ", ") { it.displayLabel }
            formReferences = formatted
            Toast.makeText(context, "${detected.size} वचन संदर्भ पहचाने गए!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "कोई वचन संदर्भ नहीं मिला (उदा. यूहन्ना 3:16, भजन 23:1-6)", Toast.LENGTH_LONG).show()
        }
    }

    // Voice dictation target ("lyrics", "title", "notes")
    var voiceDictationTarget by remember { mutableStateOf("lyrics") }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                when (voiceDictationTarget) {
                    "title" -> formTitle = if (formTitle.isBlank()) spokenText else "$formTitle $spokenText"
                    "notes" -> formPersonalNotes = if (formPersonalNotes.isBlank()) spokenText else "$formPersonalNotes\n$spokenText"
                    else -> formContent = if (formContent.isBlank()) spokenText else "$formContent\n$spokenText"
                }
            }
        }
    }

    val openVoiceInput = { target: String ->
        voiceDictationTarget = target
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, when (target) {
                    "title" -> "गीत का शीर्षक बोलें (Speak Song Title)..."
                    "notes" -> "व्यक्तिगत नोट्स बोलें (Speak Notes)..."
                    else -> "गीत के बोल बोलें (Speak Lyrics)..."
                })
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val syncFromBlogAction = {
        coroutineScope.launch {
            isSyncingBlog = true
            try {
                val newCount = lyricsRepository.fetchAndSyncLyricsFromBlogs()
                if (newCount > 0) {
                    Toast.makeText(context, "ब्लॉग से $newCount नए गीत स्वतः नंबरिंग के साथ जोड़े गए!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "ब्लॉग से सभी गीत पहले से अप-टू-डेट हैं।", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ब्लॉग सिंक विफल: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isSyncingBlog = false
            }
        }
    }

    val categories = listOf(
        "ALL",
        "स्तुति व आराधना",
        "प्रार्थना व विनती",
        "समर्पण",
        "भजन संहिता",
        "साद्री मसीही गीत",
        "अंग्रेज़ी व हिंदी",
        "क्रिसमस / सुसमाचार",
        "अन्य गीत"
    )

    // Common Hindi & English index letters
    val alphabetLetters = listOf(
        "अ", "आ", "इ", "उ", "ए", "क", "ग", "च", "ज", "त", "द", "न", "प", "ब", "म", "य", "र", "ल", "व", "स", "ह",
        "A", "B", "C", "D", "E", "G", "H", "J", "M", "P", "S", "T", "Y"
    )

    // Compute filtered & sorted songs
    val filteredSongs = remember(allSongs, indexMode, selectedCategory, selectedAlphabet, searchQuery) {
        var list = allSongs

        // Text / Number search
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter { song ->
                song.songNumber.toString() == q ||
                song.songNumber.toString().contains(q) ||
                song.title.lowercase().contains(q) ||
                song.content.lowercase().contains(q) ||
                song.artist.lowercase().contains(q) ||
                song.category.lowercase().contains(q) ||
                song.linkedReferences.lowercase().contains(q) ||
                song.personalNotes.lowercase().contains(q)
            }
        } else {
            // Filter by Mode
            when (indexMode) {
                SongBookIndexMode.BY_NUMBER -> {
                    list = list.sortedBy { if (it.songNumber > 0) it.songNumber else 99999 }
                }
                SongBookIndexMode.BY_ALPHABET -> {
                    if (selectedAlphabet != null) {
                        list = list.filter { song ->
                            song.title.startsWith(selectedAlphabet!!, ignoreCase = true) ||
                            song.title.contains(selectedAlphabet!!, ignoreCase = true)
                        }
                    }
                    list = list.sortedBy { it.title }
                }
                SongBookIndexMode.BY_CATEGORY -> {
                    if (selectedCategory != "ALL") {
                        list = list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
                    }
                    list = list.sortedBy { if (it.songNumber > 0) it.songNumber else 99999 }
                }
                SongBookIndexMode.FAVORITES -> {
                    list = list.filter { it.isFavorite }.sortedBy { if (it.songNumber > 0) it.songNumber else 99999 }
                }
            }
        }
        list
    }

    if (activeSong != null) {
        // Song Reader View with Notes Features
        SongReaderView(
            song = activeSong!!,
            onBack = { activeSong = null },
            onToggleFavorite = {
                coroutineScope.launch {
                    lyricsRepository.toggleFavorite(activeSong!!)
                    activeSong = activeSong?.copy(isFavorite = !activeSong!!.isFavorite)
                }
            },
            onEdit = {
                songToEdit = activeSong
                formSongNumber = if (activeSong!!.songNumber > 0) activeSong!!.songNumber.toString() else ""
                formTitle = activeSong!!.title
                formArtist = activeSong!!.artist
                formCategory = activeSong!!.category
                formKeyScale = activeSong!!.keyScale
                formContent = activeSong!!.content
                formColorHex = activeSong!!.colorHex
                formReferences = activeSong!!.linkedReferences
                formPersonalNotes = activeSong!!.personalNotes
                showAddSongDialog = true
            },
            onDelete = {
                coroutineScope.launch {
                    lyricsRepository.deleteSong(activeSong!!.id)
                    activeSong = null
                    Toast.makeText(context, "गीत हटाया गया", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenVerse = onOpenVerse,
            onVerseClick = { activePopupVerse = it },
            onSavePersonalNotes = { newNotes ->
                coroutineScope.launch {
                    val updated = activeSong!!.copy(personalNotes = newNotes, modifiedAt = System.currentTimeMillis())
                    lyricsRepository.updateSong(updated)
                    activeSong = updated
                    Toast.makeText(context, "नोट्स सुरक्षित किए गए", Toast.LENGTH_SHORT).show()
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("हिंदी मसीही गीत पुस्तक", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Hindi Christian Song Book & Lyrics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Sync from Blog button
                        IconButton(
                            onClick = { syncFromBlogAction() },
                            enabled = !isSyncingBlog
                        ) {
                            if (isSyncingBlog) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = NavyPrimary)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = "Sync from Blog", tint = NavyPrimary)
                            }
                        }

                        // Quick Jump by Song Number
                        IconButton(onClick = { showJumpByNumberDialog = true }) {
                            Icon(Icons.Default.Pin, contentDescription = "Jump to Song Number", tint = GoldWarm)
                        }

                        // Add new song
                        IconButton(onClick = {
                            songToEdit = null
                            coroutineScope.launch {
                                val nextNum = lyricsRepository.getNextSongNumber()
                                formSongNumber = nextNum.toString()
                                formTitle = ""
                                formArtist = "Vinay Kumar AVJ"
                                formCategory = "स्तुति व आराधना"
                                formKeyScale = "D"
                                formContent = ""
                                formColorHex = "#FFFBEB"
                                formReferences = ""
                                formPersonalNotes = ""
                                showAddSongDialog = true
                            }
                        }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add Song", tint = NavyPrimary)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        songToEdit = null
                        coroutineScope.launch {
                            val nextNum = lyricsRepository.getNextSongNumber()
                            formSongNumber = nextNum.toString()
                            formTitle = ""
                            formArtist = "Vinay Kumar AVJ"
                            formCategory = "स्तुति व आराधना"
                            formKeyScale = "D"
                            formContent = ""
                            formColorHex = "#FFFBEB"
                            formReferences = ""
                            formPersonalNotes = ""
                            showAddSongDialog = true
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("नया गीत जोड़ें (Add Lyrics)") },
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
                // Search Bar + Quick Number search + Voice Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("गीत सं. (उदा. 5), बोल, वचन या नोट्स खोजें...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                            IconButton(onClick = { showJumpByNumberDialog = true }) {
                                Icon(Icons.Default.Dialpad, contentDescription = "Number Pad", tint = NavyPrimary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Index Mode Tabs: [गीत संख्या] [वर्णमाला (Index)] [विषयवार] [पसंदीदा]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = indexMode == SongBookIndexMode.BY_NUMBER,
                        onClick = { indexMode = SongBookIndexMode.BY_NUMBER },
                        label = { Text("गीत संख्या (1, 2, 3..)", fontWeight = if (indexMode == SongBookIndexMode.BY_NUMBER) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (indexMode == SongBookIndexMode.BY_NUMBER) {
                            { Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )

                    FilterChip(
                        selected = indexMode == SongBookIndexMode.BY_ALPHABET,
                        onClick = { indexMode = SongBookIndexMode.BY_ALPHABET },
                        label = { Text("वर्णमाला अनुक्रमणिका (A-Z)", fontWeight = if (indexMode == SongBookIndexMode.BY_ALPHABET) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (indexMode == SongBookIndexMode.BY_ALPHABET) {
                            { Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )

                    FilterChip(
                        selected = indexMode == SongBookIndexMode.BY_CATEGORY,
                        onClick = { indexMode = SongBookIndexMode.BY_CATEGORY },
                        label = { Text("विषयवार (Category)", fontWeight = if (indexMode == SongBookIndexMode.BY_CATEGORY) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (indexMode == SongBookIndexMode.BY_CATEGORY) {
                            { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )

                    FilterChip(
                        selected = indexMode == SongBookIndexMode.FAVORITES,
                        onClick = { indexMode = SongBookIndexMode.FAVORITES },
                        label = { Text("पसंदीदा (Favorites)", fontWeight = if (indexMode == SongBookIndexMode.FAVORITES) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (indexMode == SongBookIndexMode.FAVORITES) {
                            { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                // Sub-filter row depending on Index Mode
                if (indexMode == SongBookIndexMode.BY_ALPHABET) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedAlphabet == null,
                            onClick = { selectedAlphabet = null },
                            label = { Text("सभी अक्षर") }
                        )
                        alphabetLetters.forEach { letter ->
                            FilterChip(
                                selected = selectedAlphabet == letter,
                                onClick = { selectedAlphabet = letter },
                                label = { Text(letter, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                } else if (indexMode == SongBookIndexMode.BY_CATEGORY) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }

                // Summary / Count Banner + Blog Sync Hint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "कुल उपलब्ध गीत: ${filteredSongs.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { syncFromBlogAction() }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ब्लॉग से सिंक करें",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Songs List
                if (filteredSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.MusicOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "कोई गीत नहीं मिला: '$searchQuery'" else "इस श्रेणी में कोई गीत उपलब्ध नहीं है",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedCategory = "ALL"
                                        selectedAlphabet = null
                                        indexMode = SongBookIndexMode.BY_NUMBER
                                    }
                                ) {
                                    Text("सभी गीत देखें")
                                }
                                OutlinedButton(onClick = { syncFromBlogAction() }) {
                                    Text("ब्लॉग से लाएं")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSongs, key = { it.id }) { song ->
                            SongItemCard(
                                song = song,
                                onClick = { activeSong = song },
                                onToggleFavorite = {
                                    coroutineScope.launch {
                                        lyricsRepository.toggleFavorite(song)
                                    }
                                },
                                onOpenVerse = onOpenVerse,
                                onVerseClick = { activePopupVerse = it }
                            )
                        }
                    }
                }
            }

            // Jump to Song Number Dialog
            if (showJumpByNumberDialog) {
                var jumpInput by remember { mutableStateOf("") }
                var errorText by remember { mutableStateOf<String?>(null) }

                AlertDialog(
                    onDismissRequest = { showJumpByNumberDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Pin, contentDescription = null, tint = GoldWarm)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("गीत संख्या खोलें", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("सीधे गीत खोलने के लिए गीत संख्या दर्ज करें (उदा. 1, 5, 8, 10):", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = jumpInput,
                                onValueChange = {
                                    jumpInput = it.filter { ch -> ch.isDigit() }
                                    errorText = null
                                },
                                label = { Text("गीत संख्या (Song #)") },
                                placeholder = { Text("उदा. 5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = errorText != null,
                                supportingText = if (errorText != null) { { Text(errorText!!, color = MaterialTheme.colorScheme.error) } } else null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val num = jumpInput.toIntOrNull()
                                if (num == null || num <= 0) {
                                    errorText = "कृपया सही संख्या दर्ज करें"
                                } else {
                                    coroutineScope.launch {
                                        val found = lyricsRepository.getSongByNumber(num)
                                        if (found != null) {
                                            activeSong = found
                                            showJumpByNumberDialog = false
                                        } else {
                                            val memoryFound = allSongs.firstOrNull { it.songNumber == num }
                                            if (memoryFound != null) {
                                                activeSong = memoryFound
                                                showJumpByNumberDialog = false
                                            } else {
                                                errorText = "गीत संख्या $num नहीं मिला"
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("गीत खोलें")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showJumpByNumberDialog = false }) {
                            Text("रद्द करें")
                        }
                    }
                )
            }

            // Add or Edit Song Dialog with FULL Notes Capabilities
            if (showAddSongDialog) {
                AlertDialog(
                    onDismissRequest = { showAddSongDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (songToEdit == null) "नया मसीही गीत जोड़ें (Add Lyrics)" else "गीत व नोट्स संपादित करें",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Color Selector for Note
                            Text("नोट्स / कार्ड का रंग चुनें:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LYRICS_NOTE_COLOR_HEXES.forEachIndexed { index, hex ->
                                    val color = LYRICS_NOTE_COLORS[index]
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (formColorHex == hex) 2.5.dp else 1.dp,
                                                color = if (formColorHex == hex) NavyPrimary else Color.Gray.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .clickable { formColorHex = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (formColorHex == hex) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                                        }
                                    }
                                }
                            }

                            // Song Number + Key Scale
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = formSongNumber,
                                    onValueChange = { formSongNumber = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("गीत सं. (#)") },
                                    placeholder = { Text("उदा. 12") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(0.45f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = formKeyScale,
                                    onValueChange = { formKeyScale = it },
                                    label = { Text("Scale (स्केल)") },
                                    placeholder = { Text("D, G, C") },
                                    modifier = Modifier.weight(0.55f),
                                    singleLine = true
                                )
                            }

                            // Title with Voice Input button
                            OutlinedTextField(
                                value = formTitle,
                                onValueChange = { formTitle = it },
                                label = { Text("गीत का शीर्षक (Song Title)*") },
                                placeholder = { Text("उदा. महिमा हो तेरी...") },
                                trailingIcon = {
                                    IconButton(onClick = { openVoiceInput("title") }) {
                                        Icon(Icons.Default.Mic, contentDescription = "Voice Input for Title", tint = NavyPrimary)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Artist / Singer
                            OutlinedTextField(
                                value = formArtist,
                                onValueChange = { formArtist = it },
                                label = { Text("गायक / लेखक (Artist/Author)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Category
                            OutlinedTextField(
                                value = formCategory,
                                onValueChange = { formCategory = it },
                                label = { Text("श्रेणी / विषय (Category)") },
                                placeholder = { Text("स्तुति व आराधना, साद्री, प्रार्थना...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Scripture Reference Linking
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("बाइबल वचन संदर्भ (Scripture Ref):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                TextButton(
                                    onClick = { autoDetectVersesInSongEditor() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("वचन पहचानें (Auto-Detect)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                }
                            }

                            OutlinedTextField(
                                value = formReferences,
                                onValueChange = { formReferences = it },
                                placeholder = { Text("उदा. भजन 23:1, यूहन्ना 3:16") },
                                leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Scripture Quick Add Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("भजन 23:1", "यूहन्ना 3:16", "भजन 100:1", "फिलिप्पियों 4:13", "इब्रानियों 11:1").forEach { refChip ->
                                    SuggestionChip(
                                        onClick = {
                                            formReferences = if (formReferences.isBlank()) refChip else "$formReferences, $refChip"
                                        },
                                        label = { Text("+ $refChip", fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Helper chord chips for fast insertion
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SuggestionChip(
                                    onClick = { formContent += "\n[Verse 1]\n" },
                                    label = { Text("+ [Verse]") }
                                )
                                SuggestionChip(
                                    onClick = { formContent += "\n[Chorus]\n" },
                                    label = { Text("+ [Chorus]") }
                                )
                                SuggestionChip(
                                    onClick = { formContent += "\n[Bridge]\n" },
                                    label = { Text("+ [Bridge]") }
                                )
                                SuggestionChip(
                                    onClick = { formContent += "[D] [G] [A] " },
                                    label = { Text("+ [Chords]") }
                                )
                            }

                            // Full Lyrics field with Voice input
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = formContent,
                                    onValueChange = { formContent = it },
                                    label = { Text("गीत के पूरे बोल (Full Lyrics)*") },
                                    placeholder = { Text("[Verse 1]\nगाओ हाल्लेलूयाह...\n\n[Chorus]\nहाल्लेलूयाह आमीन...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    maxLines = 14
                                )
                                IconButton(
                                    onClick = { openVoiceInput("lyrics") },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 4.dp)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice Dictate Lyrics", tint = NavyPrimary)
                                }
                            }

                            // Personal Notes & Reflections Section
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = formPersonalNotes,
                                    onValueChange = { formPersonalNotes = it },
                                    label = { Text("व्यक्तिगत नोट्स / विचार (Personal Notes)") },
                                    placeholder = { Text("गिटार कॉर्ड्स टिप्स, कैपो स्थिति, प्रचार नोट्स या विचार...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    maxLines = 6
                                )
                                IconButton(
                                    onClick = { openVoiceInput("notes") },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 4.dp)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice Dictate Notes", tint = NavyPrimary)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (formTitle.isNotBlank() && formContent.isNotBlank()) {
                                    val num = formSongNumber.toIntOrNull() ?: 0
                                    coroutineScope.launch {
                                        if (songToEdit == null) {
                                            lyricsRepository.addSong(
                                                ChristianSongEntity(
                                                    songNumber = num,
                                                    title = formTitle.trim(),
                                                    artist = formArtist.trim().ifBlank { "Vinay Kumar AVJ" },
                                                    category = formCategory.trim().ifBlank { "स्तुति व आराधना" },
                                                    keyScale = formKeyScale.trim().ifBlank { "D" },
                                                    content = formContent.trim(),
                                                    colorHex = formColorHex,
                                                    linkedReferences = formReferences.trim(),
                                                    personalNotes = formPersonalNotes.trim(),
                                                    isUserCreated = true
                                                )
                                            )
                                            Toast.makeText(context, "नया गीत व नोट्स सफलतापूर्वक जोड़े गए", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val updated = songToEdit!!.copy(
                                                songNumber = num,
                                                title = formTitle.trim(),
                                                artist = formArtist.trim().ifBlank { "Vinay Kumar AVJ" },
                                                category = formCategory.trim().ifBlank { "स्तुति व आराधना" },
                                                keyScale = formKeyScale.trim().ifBlank { "D" },
                                                content = formContent.trim(),
                                                colorHex = formColorHex,
                                                linkedReferences = formReferences.trim(),
                                                personalNotes = formPersonalNotes.trim(),
                                                modifiedAt = System.currentTimeMillis()
                                            )
                                            lyricsRepository.updateSong(updated)
                                            activeSong = updated
                                            Toast.makeText(context, "गीत व नोट्स अपडेट किए गए", Toast.LENGTH_SHORT).show()
                                        }
                                        showAddSongDialog = false
                                    }
                                } else {
                                    Toast.makeText(context, "कृपया शीर्षक और बोल भरें", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(if (songToEdit == null) "सुरक्षित करें (Save)" else "अपडेट करें")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddSongDialog = false }) {
                            Text("रद्द करें")
                        }
                    }
                )
            }

            // Scripture Popup Modal with TTS and translation selector
            if (activePopupVerse != null && bibleRepository != null) {
                VersePopupDialog(
                    verseRef = activePopupVerse!!,
                    bibleRepository = bibleRepository,
                    onDismiss = { activePopupVerse = null },
                    onOpenFullChapter = { bId, ch, v ->
                        activePopupVerse = null
                        onOpenVerse?.invoke(bId, ch, v)
                    }
                )
            }
        }
    }
}

@Composable
private fun SongItemCard(
    song: ChristianSongEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenVerse: ((bookId: Int, chapter: Int, verse: Int?) -> Unit)? = null,
    onVerseClick: ((DetectedVerseRef) -> Unit)? = null
) {
    val cardBg = remember(song.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(song.colorHex))
        } catch (_: Exception) {
            Color(0xFFFFFBEB)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song Number Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary),
                contentAlignment = Alignment.Center
            ) {
                if (song.songNumber > 0) {
                    Text(
                        text = "#${song.songNumber}",
                        color = GoldWarm,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = GoldWarm,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        maxLines = 1
                    )
                    if (song.keyScale.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NavyPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Scale: ${song.keyScale}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F766E).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = song.category,
                            fontSize = 10.sp,
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Linked Bible Reference Chip if available
                if (song.linkedReferences.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE0E7FF))
                            .clickable {
                                val parsed = VerseReferenceDetector.parseSingleReference(song.linkedReferences)
                                if (parsed != null && onVerseClick != null) {
                                    onVerseClick(parsed)
                                } else if (onOpenVerse != null) {
                                    val ref = song.linkedReferences.trim()
                                    val book = BibleBookDefinitions.books.firstOrNull {
                                        ref.startsWith(it.nameEnglish, ignoreCase = true) ||
                                        ref.startsWith(it.nameHindi, ignoreCase = true)
                                    }
                                    if (book != null) {
                                        onOpenVerse(book.id, 1, 1)
                                    } else {
                                        onOpenVerse(43, 3, 16)
                                    }
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF3730A3))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = song.linkedReferences,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3730A3)
                        )
                    }
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) Color(0xFFE11D48) else Color(0xFF64748B)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongReaderView(
    song: ChristianSongEntity,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenVerse: ((bookId: Int, chapter: Int, verse: Int?) -> Unit)? = null,
    onVerseClick: ((DetectedVerseRef) -> Unit)? = null,
    onSavePersonalNotes: (newNotes: String) -> Unit
) {
    val context = LocalContext.current
    var fontSizeSp by remember { mutableFloatStateOf(17f) }
    var showChords by remember { mutableStateOf(true) }
    var isAutoScrolling by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableIntStateOf(1) } // 1x, 2x, 3x
    val scrollState = rememberScrollState()

    var showNotesEditorDialog by remember { mutableStateOf(false) }
    var tempPersonalNotes by remember { mutableStateOf(song.personalNotes) }

    val readerBgColor = remember(song.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(song.colorHex))
        } catch (_: Exception) {
            Color(0xFFFFFBEB)
        }
    }

    // Auto scroll effect
    LaunchedEffect(isAutoScrolling, scrollSpeed) {
        while (isAutoScrolling) {
            delay((60 / scrollSpeed).toLong())
            if (scrollState.value < scrollState.maxValue) {
                scrollState.dispatchRawDelta(2f)
            } else {
                isAutoScrolling = false
            }
        }
    }

    val copyLyrics = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val noteAppendix = if (song.personalNotes.isNotBlank()) "\n\n[नोट्स / विचार]\n${song.personalNotes}" else ""
        val refAppendix = if (song.linkedReferences.isNotBlank()) "\n[वचन संदर्भ]: ${song.linkedReferences}" else ""
        val clip = ClipData.newPlainText(
            song.title,
            "${if (song.songNumber > 0) "गीत #${song.songNumber} - " else ""}${song.title}\nगायक: ${song.artist}$refAppendix\n\n${song.content}$noteAppendix"
        )
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "गीत के बोल व नोट्स कॉपी किए गए", Toast.LENGTH_SHORT).show()
    }

    val shareLyrics = {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, song.title)
            val noteAppendix = if (song.personalNotes.isNotBlank()) "\n\n[व्यक्तिगत नोट्स]\n${song.personalNotes}" else ""
            val refAppendix = if (song.linkedReferences.isNotBlank()) "\nवचन: ${song.linkedReferences}" else ""
            putExtra(
                Intent.EXTRA_TEXT,
                "${if (song.songNumber > 0) "मसीही गीत #${song.songNumber}\n" else ""}${song.title}\nगायक: ${song.artist}$refAppendix\n\n${song.content}$noteAppendix\n\nसाझा किया गया: Vinay Kumar AVJ App"
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Christian Song Lyrics"))
    }

    Scaffold(
        containerColor = readerBgColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (song.songNumber > 0) {
                                Surface(
                                    color = NavyPrimary,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "#${song.songNumber}",
                                        color = GoldWarm,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            text = "${song.artist} • ${song.category}${if (song.keyScale.isNotBlank()) " • Scale: ${song.keyScale}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Song")
                    }
                    IconButton(onClick = copyLyrics) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = shareLyrics) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (song.isUserCreated) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFF991B1B))
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Font Size controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (fontSizeSp > 12f) fontSizeSp -= 2f }) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "Smaller Font")
                        }
                        Text("${fontSizeSp.toInt()}sp", style = MaterialTheme.typography.labelMedium)
                        IconButton(onClick = { if (fontSizeSp < 32f) fontSizeSp += 2f }) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "Larger Font")
                        }
                    }

                    // Chords Visibility Toggle
                    FilterChip(
                        selected = showChords,
                        onClick = { showChords = !showChords },
                        label = { Text(if (showChords) "Chords ON" else "Chords OFF", fontSize = 11.sp) }
                    )

                    // Auto Scroll (Play/Pause for worship singing)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isAutoScrolling = !isAutoScrolling }) {
                            Icon(
                                if (isAutoScrolling) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Auto Scroll",
                                tint = if (isAutoScrolling) Color(0xFF16A34A) else NavyPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        if (isAutoScrolling) {
                            TextButton(
                                onClick = {
                                    scrollSpeed = when (scrollSpeed) {
                                        1 -> 2
                                        2 -> 3
                                        else -> 1
                                    }
                                }
                            ) {
                                Text("${scrollSpeed}x", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Info Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (song.songNumber > 0) "गीत संख्या: #${song.songNumber}" else "मसीही गीत",
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "श्रेणी: ${song.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (song.keyScale.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavyPrimary
                        ) {
                            Text(
                                text = "Scale: ${song.keyScale}",
                                color = GoldWarm,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Scripture Reference Banner
            if (song.linkedReferences.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val parsed = VerseReferenceDetector.parseSingleReference(song.linkedReferences)
                            if (parsed != null && onVerseClick != null) {
                                onVerseClick(parsed)
                            } else if (onOpenVerse != null) {
                                val ref = song.linkedReferences.trim()
                                val book = BibleBookDefinitions.books.firstOrNull {
                                    ref.startsWith(it.nameEnglish, ignoreCase = true) ||
                                    ref.startsWith(it.nameHindi, ignoreCase = true)
                                }
                                if (book != null) {
                                    onOpenVerse(book.id, 1, 1)
                                } else {
                                    onOpenVerse(43, 3, 16)
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("बाइबल वचन संदर्भ:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4338CA), fontWeight = FontWeight.Bold)
                            Text(song.linkedReferences, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF312E81), fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Formatted Lyrics
            val formattedText = formatLyrics(song.content, showChords)
            Text(
                text = formattedText,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.55f).sp,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Personal Notes / Reflection Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("व्यक्तिगत नोट्स / विचार", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                        }
                        IconButton(
                            onClick = {
                                tempPersonalNotes = song.personalNotes
                                showNotesEditorDialog = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Notes", tint = NavyPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (song.personalNotes.isNotBlank()) {
                        Text(
                            text = song.personalNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF334155)
                        )
                    } else {
                        Text(
                            text = "इस गीत के लिए कोई व्यक्तिगत नोट्स नहीं हैं। नोट्स जोड़ने के लिए पेंसिल आइकन दबाएं।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }

        // Edit Personal Notes Dialog
        if (showNotesEditorDialog) {
            AlertDialog(
                onDismissRequest = { showNotesEditorDialog = false },
                title = { Text("व्यक्तिगत नोट्स संपादित करें", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = tempPersonalNotes,
                        onValueChange = { tempPersonalNotes = it },
                        label = { Text("नोट्स / विचार (Personal Notes)") },
                        placeholder = { Text("गिटार कॉर्ड्स, कैपो स्थिति, विचार...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        maxLines = 8
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onSavePersonalNotes(tempPersonalNotes.trim())
                            showNotesEditorDialog = false
                        }
                    ) {
                        Text("सुरक्षित करें")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNotesEditorDialog = false }) {
                        Text("रद्द करें")
                    }
                }
            )
        }
    }
}

/**
 * Parses chords [D], section headers [Verse 1], [Chorus] into styled text
 */
private fun formatLyrics(raw: String, showChords: Boolean = true): AnnotatedString {
    return buildAnnotatedString {
        val lines = raw.lines()
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("[") && trimmed.endsWith("]") && (
                    trimmed.contains("Chorus", ignoreCase = true) ||
                    trimmed.contains("Verse", ignoreCase = true) ||
                    trimmed.contains("Bridge", ignoreCase = true) ||
                    trimmed.contains("Intro", ignoreCase = true) ||
                    trimmed.contains("Outro", ignoreCase = true)
                ) -> {
                    // Section header
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0F766E)))
                    append("\n$trimmed\n")
                    pop()
                }
                trimmed.startsWith("[") && trimmed.contains("]") -> {
                    if (showChords) {
                        // Line with chord indicators like [D] [G]
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFB45309), fontFamily = FontFamily.Monospace))
                        append(line)
                        pop()
                        if (index < lines.size - 1) append("\n")
                    }
                }
                else -> {
                    append(line)
                    if (index < lines.size - 1) append("\n")
                }
            }
        }
    }
}
