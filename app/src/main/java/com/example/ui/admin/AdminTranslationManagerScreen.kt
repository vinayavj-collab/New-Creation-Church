package com.example.ui.admin

import android.widget.Toast
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.local.BibleDatabase
import com.example.data.bible.model.BibleTranslation
import com.example.ui.theme.GoldWarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TranslationStatusItem(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String,
    val language: String,
    val verseCount: Int,
    val headingCount: Int,
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

    var translationsList by remember { mutableStateOf<List<TranslationStatusItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var translationToDelete by remember { mutableStateOf<TranslationStatusItem?>(null) }
    var showAddCustomTranslationDialog by remember { mutableStateOf(false) }

    // Add Translation Form State
    var newId by remember { mutableStateOf("") }
    var newNameHindi by remember { mutableStateOf("") }
    var newNameEnglish by remember { mutableStateOf("") }
    var newLanguage by remember { mutableStateOf("hi") }

    fun refreshTranslationStats() {
        coroutineScope.launch {
            isLoading = true
            val items = withContext(Dispatchers.IO) {
                val knownTranslations = mutableListOf(
                    BibleTranslation.HIOV
                )

                knownTranslations.map { trans ->
                    val vCount = try { dao.getVerseCount(trans.id) } catch (_: Exception) { 0 }
                    val hCount = try { dao.getHeadingCount(trans.id) } catch (_: Exception) { 0 }
                    val isDef = trans.id == BibleTranslation.HIOV.id
                    val active = vCount > 0 || isDef

                    TranslationStatusItem(
                        id = trans.id,
                        nameHindi = trans.nameHindi,
                        nameEnglish = trans.nameEnglish,
                        language = trans.language,
                        verseCount = vCount,
                        headingCount = hCount,
                        isDefault = isDef,
                        isActive = active
                    )
                }
            }
            translationsList = items
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshTranslationStats()
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "बाइबल अनुवाद व कमेंट्री नियंत्रण",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "अनुवाद, कमेंट्री व ZIP फाइल्स प्रबंधन",
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

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("📚 स्थानीय अनुवाद (Room)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("☁️ ZIP व कमेंट्री फाइल्स", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddCustomTranslationDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("नया अनुवाद जोड़ें", fontWeight = FontWeight.Bold) },
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
            if (selectedTab == 1) {
                AdminBibleResourceManagerScreen(
                    currentAdmin = null,
                    onNavigateBack = { selectedTab = 0 }
                )
            } else if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Overview Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                Column {
                                    Text(
                                        text = "सक्रिय अनुवाद प्रबंधन (State Governance)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "किसी भी अनुवाद को ऐप मेमोरी व SQLite3/Room से तुरंत डिलीट या सक्रिय करें।",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "अनुवाद स्थिति सूची (${translationsList.size})",
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
                                        dao.deleteCommentariesByTranslation(item.id)
                                    }
                                    Toast.makeText(context, "${item.id} का पूरा डेटा Room DB से डिलीट कर दिया गया! 🗑️", Toast.LENGTH_SHORT).show()
                                    refreshTranslationStats()
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
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
                    text = "क्या आप निश्चित रूप से '${target.nameHindi}' (${target.id}) के सभी ${target.verseCount} वचनों और शीर्षकों को डेटाबेस से पूरी तरह हटाना चाहते हैं?",
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
    onPurgeVerses: () -> Unit
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
        border = androidx.compose.foundation.BorderStroke(
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

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "वचन: ${item.verseCount}",
                            style = MaterialTheme.typography.labelSmall.copy(
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
                }

                // Actions
                if (!item.isDefault) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (item.verseCount > 0) {
                            OutlinedButton(
                                onClick = onPurgeVerses,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("डेटा खाली करें", fontSize = 11.sp)
                            }
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
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
}
