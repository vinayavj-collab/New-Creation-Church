package com.example.ui.bible

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.BibleBook
import com.example.data.bible.model.BibleBookDefinitions
import com.example.data.bible.model.BibleVerseCounts
import com.example.data.bible.model.Testament

enum class SelectorStep {
    BOOK,
    CHAPTER,
    VERSE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleBookChapterVerseSelectorModal(
    initialBook: BibleBook,
    initialChapter: Int,
    initialVerse: Int? = null,
    initialStep: SelectorStep = SelectorStep.CHAPTER,
    isHindi: Boolean = true,
    onDismiss: () -> Unit,
    onSelectionComplete: (book: BibleBook, chapter: Int, targetVerse: Int?) -> Unit
) {
    var currentStep by remember(initialBook, initialChapter, initialStep) { mutableStateOf(initialStep) }
    var selectedBook by remember(initialBook) { mutableStateOf(initialBook) }
    var selectedChapter by remember(initialChapter) { mutableIntStateOf(initialChapter) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("सभी") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
        ) {
            // Header: Title and Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "बाइबल नेविगेटर (Bible Navigator)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "पुस्तक ➔ अध्याय ➔ वचन चुनें",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // 3-Step Breadcrumb / Tab Row
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Step 1: Book Tab
                    StepTabItem(
                        title = "1. पुस्तक",
                        subtitle = if (isHindi) selectedBook.nameHindi else selectedBook.nameEnglish,
                        isSelected = currentStep == SelectorStep.BOOK,
                        modifier = Modifier.weight(1f),
                        onClick = { currentStep = SelectorStep.BOOK }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Step 2: Chapter Tab
                    StepTabItem(
                        title = "2. अध्याय",
                        subtitle = "Ch $selectedChapter",
                        isSelected = currentStep == SelectorStep.CHAPTER,
                        modifier = Modifier.weight(1f),
                        onClick = { currentStep = SelectorStep.CHAPTER }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Step 3: Verse Tab
                    StepTabItem(
                        title = "3. वचन",
                        subtitle = if (initialVerse != null) "V $initialVerse" else "सभी / All",
                        isSelected = currentStep == SelectorStep.VERSE,
                        modifier = Modifier.weight(1f),
                        onClick = { currentStep = SelectorStep.VERSE }
                    )
                }
            }

            // Tab Content
            when (currentStep) {
                SelectorStep.BOOK -> {
                    BookSelectionStep(
                        selectedBook = selectedBook,
                        isHindi = isHindi,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        categoryFilter = selectedCategoryFilter,
                        onCategoryFilterChange = { selectedCategoryFilter = it },
                        onBookSelected = { book ->
                            selectedBook = book
                            selectedChapter = 1.coerceIn(1, book.chapterCount)
                            currentStep = SelectorStep.CHAPTER
                        }
                    )
                }

                SelectorStep.CHAPTER -> {
                    ChapterSelectionStep(
                        book = selectedBook,
                        selectedChapter = selectedChapter,
                        isHindi = isHindi,
                        onBackToBooks = { currentStep = SelectorStep.BOOK },
                        onChapterSelected = { ch ->
                            selectedChapter = ch
                            currentStep = SelectorStep.VERSE
                        }
                    )
                }

                SelectorStep.VERSE -> {
                    VerseSelectionStep(
                        book = selectedBook,
                        chapter = selectedChapter,
                        isHindi = isHindi,
                        onBackToChapters = { currentStep = SelectorStep.CHAPTER },
                        onReadWholeChapter = {
                            onSelectionComplete(selectedBook, selectedChapter, null)
                        },
                        onVerseSelected = { verse ->
                            onSelectionComplete(selectedBook, selectedChapter, verse)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepTabItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BookSelectionStep(
    selectedBook: BibleBook,
    isHindi: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    categoryFilter: String,
    onCategoryFilterChange: (String) -> Unit,
    onBookSelected: (BibleBook) -> Unit
) {
    val filterCategories = listOf(
        "सभी",
        "नया नियम",
        "पुराना नियम",
        "सुसमाचार",
        "पत्रियां",
        "इतिहास",
        "काव्य",
        "व्यवस्था",
        "भविष्यद्वाणी"
    )

    val allBooks = BibleBookDefinitions.books

    val filteredBooks = remember(searchQuery, categoryFilter) {
        allBooks.filter { book ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                book.nameHindi.contains(searchQuery, ignoreCase = true) ||
                        book.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                        book.abbreviationHindi.contains(searchQuery, ignoreCase = true) ||
                        book.abbreviationEnglish.contains(searchQuery, ignoreCase = true) ||
                        book.id.toString() == searchQuery.trim()
            }

            val matchesCategory = when (categoryFilter) {
                "सभी" -> true
                "नया नियम" -> book.testament == Testament.NEW
                "पुराना नियम" -> book.testament == Testament.OLD
                "सुसमाचार" -> book.category == "सुसमाचार"
                "पत्रियां" -> book.category == "पत्रियां"
                "इतिहास" -> book.category == "इतिहास"
                "काव्य" -> book.category == "काव्य"
                "व्यवस्था" -> book.category == "व्यवस्था"
                "भविष्यद्वाणी" -> book.category == "भविष्यद्वाणी"
                else -> true
            }

            matchesSearch && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("बाइबल पुस्तक खोजें... (Search Book)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            items(filterCategories) { category ->
                val isSelected = categoryFilter == category
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategoryFilterChange(category) },
                    label = {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Books Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            items(filteredBooks, key = { it.id }) { book ->
                val isCurrent = book.id == selectedBook.id
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBookSelected(book) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (book.testament == Testament.NEW) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${book.id}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (book.testament == Testament.NEW) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) book.nameHindi else book.nameEnglish,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${book.chapterCount} Ch • ${book.category}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterSelectionStep(
    book: BibleBook,
    selectedChapter: Int,
    isHindi: Boolean,
    onBackToBooks: () -> Unit,
    onChapterSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Book Header Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "${book.nameHindi} (${book.nameEnglish})" else "${book.nameEnglish} (${book.nameHindi})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "कुल ${book.chapterCount} अध्याय • ${book.category}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                OutlinedButton(
                    onClick = onBackToBooks,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("पुस्तक", fontSize = 11.sp)
                }
            }
        }

        Text(
            text = "अध्याय चुनें (Select Chapter)",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Chapter Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 54.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            items((1..book.chapterCount).toList()) { chap ->
                val isCurrent = chap == selectedChapter
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onChapterSelected(chap) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$chap",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseSelectionStep(
    book: BibleBook,
    chapter: Int,
    isHindi: Boolean,
    onBackToChapters: () -> Unit,
    onReadWholeChapter: () -> Unit,
    onVerseSelected: (Int) -> Unit
) {
    val verseCount = remember(book.id, chapter) {
        BibleVerseCounts.getVerseCount(book.id, chapter)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${if (isHindi) book.nameHindi else book.nameEnglish} अध्याय $chapter",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "कुल $verseCount वचन (Verses)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                OutlinedButton(
                    onClick = onBackToChapters,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("अध्याय", fontSize = 11.sp)
                }
            }
        }

        // Quick Primary Button: Read Full Chapter
        Button(
            onClick = onReadWholeChapter,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "📖 पूरा अध्याय $chapter पढ़ें (Read Full Chapter)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Text(
            text = "या किसी विशिष्ट पद / वचन पर जाएं (Jump to Verse):",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Verse Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 50.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            items((1..verseCount).toList()) { verseNum ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onVerseSelected(verseNum) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$verseNum",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}
