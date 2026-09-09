package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.local.ChristianSongEntity
import com.example.data.bible.repository.LyricsRepository
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    lyricsRepository: LyricsRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        lyricsRepository.initializePreloadedLyrics()
    }

    val allSongs by lyricsRepository.getAllSongs().collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var activeSong by remember { mutableStateOf<ChristianSongEntity?>(null) }
    var showAddSongDialog by remember { mutableStateOf(false) }

    // Dialog fields for user created song
    var newTitle by remember { mutableStateOf("") }
    var newArtist by remember { mutableStateOf("Vinay Kumar AVJ") }
    var newCategory by remember { mutableStateOf("Hindi Worship") }
    var newContent by remember { mutableStateOf("") }

    val categories = listOf("ALL", "FAVORITES", "Hindi Worship", "Hindi Praise", "Sadri Christian", "English Worship")

    val filteredSongs = remember(allSongs, selectedCategory, searchQuery) {
        var list = allSongs
        if (selectedCategory == "FAVORITES") {
            list = list.filter { it.isFavorite }
        } else if (selectedCategory != "ALL") {
            list = list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.content.lowercase().contains(q) ||
                it.artist.lowercase().contains(q)
            }
        }
        list
    }

    if (activeSong != null) {
        // Song Reader View
        SongReaderView(
            song = activeSong!!,
            onBack = { activeSong = null },
            onToggleFavorite = {
                coroutineScope.launch {
                    lyricsRepository.toggleFavorite(activeSong!!)
                    activeSong = activeSong?.copy(isFavorite = !activeSong!!.isFavorite)
                }
            },
            onDelete = {
                coroutineScope.launch {
                    lyricsRepository.deleteSong(activeSong!!.id)
                    activeSong = null
                    Toast.makeText(context, "Song deleted", Toast.LENGTH_SHORT).show()
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Christian Songs & Lyrics", fontWeight = FontWeight.Bold) },
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
                        newTitle = ""
                        newArtist = "Vinay Kumar AVJ"
                        newCategory = "Hindi Worship"
                        newContent = ""
                        showAddSongDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Song")
                }
            },
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search songs, lyrics or artists...") },
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
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Category Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                // Song List
                if (filteredSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No songs found in this category" else "No songs found for '$searchQuery'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredSongs, key = { it.id }) { song ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeSong = song },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(NavyPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = GoldWarm,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = song.artist,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(song.category, fontSize = 10.sp) },
                                                modifier = Modifier.height(24.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                lyricsRepository.toggleFavorite(song)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (song.isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add Song Dialog
            if (showAddSongDialog) {
                AlertDialog(
                    onDismissRequest = { showAddSongDialog = false },
                    title = { Text("Add Christian Song Lyrics", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                label = { Text("Song Title") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newArtist,
                                onValueChange = { newArtist = it },
                                label = { Text("Artist / Channel") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newCategory,
                                onValueChange = { newCategory = it },
                                label = { Text("Category (e.g. Hindi Worship, Sadri)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newContent,
                                onValueChange = { newContent = it },
                                label = { Text("Lyrics (Verses, Chorus, Chords)") },
                                placeholder = { Text("[Verse 1]\nLyrics here...\n\n[Chorus]\nChorus here...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                maxLines = 10
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                                    coroutineScope.launch {
                                        lyricsRepository.addSong(
                                            ChristianSongEntity(
                                                title = newTitle.trim(),
                                                artist = newArtist.trim().ifBlank { "Vinay Kumar AVJ" },
                                                category = newCategory.trim().ifBlank { "Hindi Worship" },
                                                content = newContent.trim(),
                                                isUserCreated = true
                                            )
                                        )
                                        showAddSongDialog = false
                                        Toast.makeText(context, "Song added successfully", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddSongDialog = false }) {
                            Text("Cancel")
                        }
                    }
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
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var fontSizeSp by remember { mutableFloatStateOf(16f) }
    val scrollState = rememberScrollState()

    val copyLyrics = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(song.title, "${song.title} - ${song.artist}\n\n${song.content}")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Lyrics copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    val shareLyrics = {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, song.title)
            putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}\n\n${song.content}\n\nShared via Vinay Kumar AVJ App")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Lyrics"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Zoom out
                    IconButton(onClick = { if (fontSizeSp > 12f) fontSizeSp -= 2f }) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "Smaller Font")
                    }
                    // Zoom in
                    IconButton(onClick = { if (fontSizeSp < 28f) fontSizeSp += 2f }) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "Larger Font")
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            val formattedText = formatLyrics(song.content)
            Text(
                text = formattedText,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.55f).sp,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * Parses chords [D], section headers [Verse 1], [Chorus] into styled text
 */
private fun formatLyrics(raw: String): AnnotatedString {
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
                    // Line with chord indicators like [D] [G]
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFB45309), fontFamily = FontFamily.Monospace))
                    append(line)
                    pop()
                    if (index < lines.size - 1) append("\n")
                }
                else -> {
                    append(line)
                    if (index < lines.size - 1) append("\n")
                }
            }
        }
    }
}
