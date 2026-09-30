package com.example.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.bible.model.BibleBookDefinitions
import com.example.data.bible.model.BibleTranslation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    viewModel: BibleViewModel,
    onBack: () -> Unit
) {
    val verses by viewModel.versesState.collectAsState()
    val currentBook = BibleBookDefinitions.getBookById(viewModel.currentBookId)

    var showTranslationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${currentBook?.nameHindi ?: "Bible"} ${viewModel.currentChapter}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showTranslationDialog = true }) {
                        Text(viewModel.currentTranslationId, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(verses) { verse ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${verse.verseNumber}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row {
                                    IconButton(onClick = { viewModel.toggleBookmark(verse) }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Bookmark",
                                            tint = if (verse.isBookmarked) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                    }
                                    IconButton(onClick = { viewModel.toggleFavorite(verse) }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = "Favorite",
                                            tint = if (verse.isFavorite) Color.Red else Color.Gray
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = verse.text,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (!verse.secondaryText.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = verse.secondaryText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // Chapter / Book Navigation Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = {
                    if (viewModel.currentChapter > 1) {
                        viewModel.navigateToChapter(viewModel.currentBookId, viewModel.currentChapter - 1)
                    }
                }) {
                    Text("Previous Chapter")
                }
                Button(onClick = {
                    val maxCh = currentBook?.totalChapters ?: 1
                    if (viewModel.currentChapter < maxCh) {
                        viewModel.navigateToChapter(viewModel.currentBookId, viewModel.currentChapter + 1)
                    } else if (viewModel.currentBookId < 44) {
                        viewModel.navigateToChapter(viewModel.currentBookId + 1, 1)
                    }
                }) {
                    Text("Next Chapter")
                }
            }
        }
    }

    if (showTranslationDialog) {
        AlertDialog(
            onDismissRequest = { showTranslationDialog = false },
            title = { Text("Select Translation / Mode") },
            text = {
                Column {
                    BibleTranslation.ALL.forEach { translation ->
                        TextButton(
                            onClick = {
                                viewModel.setTranslation(translation.id)
                                showTranslationDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(translation.nameHindi)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTranslationDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
