package com.example.ui.bible

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.BibleBookDefinitions
import com.example.data.bible.model.BibleVerse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleSavedScreen(
    viewModel: BibleViewModel,
    onBackClick: () -> Unit,
    onVerseClick: (bookId: Int, chapter: Int, verseNumber: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val highlights by viewModel.highlights.collectAsState()
    val notes by viewModel.notes.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingNoteVerse by remember { mutableStateOf<BibleVerse?>(null) }
    var editingNoteText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "सहेजे गए (Saved)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("बुकमार्क (${bookmarks.size})", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("पसंदीदा (${favorites.size})", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF59E0B)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("हाइलाइट्स (${highlights.size})", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("नोट्स (${notes.size})", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Bookmarks List
                    if (bookmarks.isEmpty()) {
                        EmptyStateBox("कोई बुकमार्क नहीं है।", "वचन पढ़ते समय बुकमार्क विकल्प चुनकर उन्हें यहाँ सहेजें।")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(bookmarks, key = { it.id }) { item ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onVerseClick(item.bookId, item.chapter, item.verse)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${item.bookName} ${item.chapter}:${item.verse}",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.verseText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.removeBookmarkById(item.id) }
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Bookmark",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Favorites List
                    if (favorites.isEmpty()) {
                        EmptyStateBox("कोई पसंदीदा वचन नहीं है।", "वचन पर लंबे समय तक दबाकर (Long Press) 'पसंदीदा' (Favorite) बनाएँ।")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(favorites, key = { it.id }) { item ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onVerseClick(item.bookId, item.chapter, item.verse)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${item.bookName} ${item.chapter}:${item.verse}",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.verseText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.removeFavoriteById(item.id) }
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Favorite",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Highlights List
                    if (highlights.isEmpty()) {
                        EmptyStateBox("कोई हाइलाइट नहीं है।", "वचन पर टैप करें या स्वाइप करके अपने पसंदीदा रंग से हाइलाइट करें।")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(highlights, key = { it.id }) { item ->
                                val book = BibleBookDefinitions.getBookById(item.bookId)
                                val bookName = book?.nameHindi ?: "पुस्तक ${item.bookId}"
                                val highlightColor = try {
                                    Color(android.graphics.Color.parseColor(item.colorHex))
                                } catch (e: Exception) {
                                    Color(0xFFFEF08A)
                                }

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onVerseClick(item.bookId, item.chapter, item.verse)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(highlightColor)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "$bookName ${item.chapter}:${item.verse}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        TextButton(onClick = {
                                            onVerseClick(item.bookId, item.chapter, item.verse)
                                        }) {
                                            Text("देखें")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Notes List
                    if (notes.isEmpty()) {
                        EmptyStateBox("कोई व्यक्तिगत नोट नहीं है।", "वचन पर लंबे समय तक दबाकर (Long Press) व्यक्तिगत प्रार्थना या विचार लिखें।")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(notes, key = { it.id }) { item ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val dummyVerse = BibleVerse(
                                                bookId = item.bookId,
                                                bookName = item.bookName,
                                                chapter = item.chapter,
                                                verseNumber = item.verse,
                                                translationId = "hi_irv",
                                                text = "",
                                                note = item.noteText
                                            )
                                            editingNoteVerse = dummyVerse
                                            editingNoteText = item.noteText
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.EditNote,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${item.bookName} ${item.chapter}:${item.verse}",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = item.noteText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.padding(10.dp),
                                                    maxLines = 4,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    val dummyVerse = BibleVerse(
                                                        bookId = item.bookId,
                                                        bookName = item.bookName,
                                                        chapter = item.chapter,
                                                        verseNumber = item.verse,
                                                        translationId = "hi_irv",
                                                        text = "",
                                                        note = item.noteText
                                                    )
                                                    editingNoteVerse = dummyVerse
                                                    editingNoteText = item.noteText
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit Note",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteNote(item.bookId, item.chapter, item.verse) }
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete Note",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Note Editor modal when tapped in Saved list
    if (editingNoteVerse != null) {
        val v = editingNoteVerse!!
        BibleNoteEditorDialog(
            verse = v,
            initialNote = editingNoteText,
            onSave = { updatedText ->
                viewModel.saveNote(v, updatedText)
                editingNoteVerse = null
            },
            onDelete = {
                viewModel.deleteNote(v.bookId, v.chapter, v.verseNumber)
                editingNoteVerse = null
            },
            onDismiss = { editingNoteVerse = null },
            onNavigateToVerse = { bId, chap, vNum ->
                onVerseClick(bId, chap, vNum)
            }
        )
    }
}

@Composable
private fun EmptyStateBox(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Bookmarks,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
