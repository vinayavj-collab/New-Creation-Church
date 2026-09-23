package com.example.ui.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HomeSectionItem
import com.example.data.model.HomeSectionsConfig
import com.example.data.model.defaultHomeSections
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLayoutManagerDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig by viewModel.homeSectionsConfig.collectAsStateWithLifecycle()

    var sectionsList by remember(currentConfig) {
        mutableStateOf(
            if (currentConfig.sections.isNotEmpty()) currentConfig.sections else defaultHomeSections()
        )
    }

    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showThemeManagerDialog by remember { mutableStateOf(false) }
    var itemForTimerConfig by remember { mutableStateOf<HomeSectionItem?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyPrimary)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "होम पेज लेआउट प्रबन्धक",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "क्रम बदलें • दिखाएं/छिपाएं • समय सीमा (Timer) • नया जोड़ें",
                            fontSize = 11.sp,
                            color = GoldWarm
                        )
                    }

                    // Reset Default Button
                    IconButton(
                        onClick = {
                            sectionsList = defaultHomeSections()
                            Toast.makeText(context, "डिफ़ॉल्ट लेआउट लोड हुआ", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color.White.copy(alpha = 0.85f))
                    }
                }

                // Sub-Actions Banner
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "कुल सेक्शन्स: ${sectionsList.size} (सक्रिय: ${sectionsList.count { it.isCurrentlyVisible() }})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "↑/↓ से क्रम बदलें, टाइमर से दिन तय करें",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showThemeManagerDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("🎨 क्रिश्चियन थीम", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showAddCustomDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("नया सेक्शन", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Section Items List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sectionsList, key = { _, item -> item.id }) { index, item ->
                        val isVisibleNow = item.isCurrentlyVisible()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isVisibleNow) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                                }
                            ),
                            border = if (!item.isPermanent) {
                                androidx.compose.foundation.BorderStroke(1.dp, GoldWarm)
                            } else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Ordering controls (Up / Down)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val mutable = sectionsList.toMutableList()
                                                    val moved = mutable.removeAt(index)
                                                    mutable.add(index - 1, moved)
                                                    sectionsList = mutable
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move Up",
                                                tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                                            )
                                        }

                                        Text(
                                            text = "#${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        IconButton(
                                            onClick = {
                                                if (index < sectionsList.size - 1) {
                                                    val mutable = sectionsList.toMutableList()
                                                    val moved = mutable.removeAt(index)
                                                    mutable.add(index + 1, moved)
                                                    sectionsList = mutable
                                                }
                                            },
                                            enabled = index < sectionsList.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move Down",
                                                tint = if (index < sectionsList.size - 1) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Content Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (item.type == "CUSTOM") {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "कस्टम",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (item.subtitle.isNotBlank()) {
                                            Text(
                                                text = item.subtitle,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Status badge
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                if (item.isPermanent) Icons.Default.Lock else Icons.Default.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (item.isPermanent) MaterialTheme.colorScheme.primary else GoldWarm
                                            )
                                            Text(
                                                text = item.getTimeStatusDescription(),
                                                fontSize = 11.sp,
                                                fontWeight = if (!item.isPermanent) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (!item.isPermanent) GoldWarm else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Action Switch & Menu
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Switch(
                                            checked = item.isVisible,
                                            onCheckedChange = { checked ->
                                                val mutable = sectionsList.toMutableList()
                                                mutable[index] = item.copy(isVisible = checked)
                                                sectionsList = mutable
                                            }
                                        )

                                        if (item.type == "CUSTOM") {
                                            IconButton(
                                                onClick = {
                                                    val mutable = sectionsList.toMutableList()
                                                    mutable.removeAt(index)
                                                    sectionsList = mutable
                                                    Toast.makeText(context, "कस्टम सेक्शन हटाया गया", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick timer configuration button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { itemForTimerConfig = item },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            if (item.isPermanent) "समय सीमा (Timer) सेट करें" else "समय सीमा बदलें",
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Save Actions
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("रद्द करें")
                        }

                        Button(
                            onClick = {
                                isSaving = true
                                val newConfig = HomeSectionsConfig(
                                    isCustomLayoutEnabled = true,
                                    sections = sectionsList,
                                    lastUpdated = System.currentTimeMillis()
                                )
                                viewModel.updateHomeSectionsConfig(newConfig) { success ->
                                    isSaving = false
                                    if (success) {
                                        Toast.makeText(context, "होम लेआउट सफलतापूर्वक सहेजा गया! ✨", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "लेआउट सहेजने में विफल", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (isSaving) "सहेज रहे हैं..." else "परिवर्तन सहेजें (Save)")
                        }
                    }
                }
            }
        }
    }

    // Timer Duration Config Dialog
    if (itemForTimerConfig != null) {
        val targetItem = itemForTimerConfig!!
        var isPermanentSelected by remember(targetItem) { mutableStateOf(targetItem.isPermanent) }
        var selectedDays by remember(targetItem) {
            mutableIntStateOf(if (targetItem.durationDays > 0) targetItem.durationDays else 3)
        }
        var selectedAction by remember(targetItem) { mutableStateOf(targetItem.temporaryAction) }

        AlertDialog(
            onDismissRequest = { itemForTimerConfig = null },
            icon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldWarm) },
            title = {
                Text(
                    text = "समय सीमा / टाइमर सेटिंग्स",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "सेक्शन: ${targetItem.title}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Mode Selection: Permanent vs Timed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isPermanentSelected,
                            onClick = { isPermanentSelected = true },
                            label = { Text("हमेशा स्थायी (Permanent)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isPermanentSelected,
                            onClick = { isPermanentSelected = false },
                            label = { Text("समय सीमा सहित (Timed)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!isPermanentSelected) {
                        Text(
                            text = "यह कितने दिनों तक लागू रहे:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Preset Day Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 2, 3, 5, 7, 10, 15, 30).forEach { days ->
                                FilterChip(
                                    selected = selectedDays == days,
                                    onClick = { selectedDays = days },
                                    label = { Text("$days दिन", fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "समय पूरा होने पर क्या हो:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Action after expiry
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAction = "HIDE_AFTER" }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAction == "HIDE_AFTER",
                                onClick = { selectedAction = "HIDE_AFTER" }
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text("स्वतः छिप जाए (Hide after $selectedDays days)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("उचित: जब कोई सीमित समय का बैनर या सूचना हो", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAction = "SHOW_AFTER" }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAction == "SHOW_AFTER",
                                onClick = { selectedAction = "SHOW_AFTER" }
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text("स्वतः पुनः दिखने लगे (Revert to visible)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("उचित: कुछ दिनों के लिए छुपाएं, फिर पूर्ववत दिखे", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = sectionsList.indexOfFirst { it.id == targetItem.id }
                        if (index != -1) {
                            val mutable = sectionsList.toMutableList()
                            val now = System.currentTimeMillis()
                            val expiry = if (isPermanentSelected) 0L else now + (selectedDays * 24L * 60L * 60L * 1000L)
                            // If hide_after, initial isVisible is true; if show_after, initial isVisible is false (hidden for N days)
                            val newVisibility = if (isPermanentSelected) targetItem.isVisible else {
                                if (selectedAction == "HIDE_AFTER") true else false
                            }

                            mutable[index] = targetItem.copy(
                                isPermanent = isPermanentSelected,
                                durationDays = if (isPermanentSelected) 0 else selectedDays,
                                expiresAtTimestamp = expiry,
                                temporaryAction = selectedAction,
                                isVisible = newVisibility
                            )
                            sectionsList = mutable
                            Toast.makeText(context, "टाइमर सेटिंग्स अपडेट हुई", Toast.LENGTH_SHORT).show()
                        }
                        itemForTimerConfig = null
                    }
                ) {
                    Text("लागू करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemForTimerConfig = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Add Custom Section Dialog
    if (showAddCustomDialog) {
        var customTitle by remember { mutableStateOf("") }
        var customSubtitle by remember { mutableStateOf("") }
        var customActionText by remember { mutableStateOf("यहाँ क्लिक करें") }
        var customActionUrl by remember { mutableStateOf("") }
        var customImageUrl by remember { mutableStateOf("") }
        var isPermanentCustom by remember { mutableStateOf(true) }
        var customDays by remember { mutableIntStateOf(7) }

        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("नया कस्टम सेक्शन जोड़ें", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("सेक्शन शीर्षक (Title) *") },
                        placeholder = { Text("उदा: विशेष आराधना सभा सूचना") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customSubtitle,
                        onValueChange = { customSubtitle = it },
                        label = { Text("विवरण / संदेश (Message)") },
                        placeholder = { Text("संदेश या जानकारी संक्षेप में...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customActionText,
                        onValueChange = { customActionText = it },
                        label = { Text("बटन का नाम (Button Text)") },
                        placeholder = { Text("यहाँ क्लिक करें / देखें") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customActionUrl,
                        onValueChange = { customActionUrl = it },
                        label = { Text("वेब लिंक या वीडियो लिंक (Action URL)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customImageUrl,
                        onValueChange = { customImageUrl = it },
                        label = { Text("फोटो / बैनर लिंक (Image URL - ऐच्छिक)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Timed vs Permanent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = isPermanentCustom,
                            onClick = { isPermanentCustom = true },
                            label = { Text("स्थायी", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isPermanentCustom,
                            onClick = { isPermanentCustom = false },
                            label = { Text("$customDays दिन के लिए", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!isPermanentCustom) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(1, 2, 3, 5, 7, 14, 30).forEach { d ->
                                FilterChip(
                                    selected = customDays == d,
                                    onClick = { customDays = d },
                                    label = { Text("$d दिन", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customTitle.isNotBlank()) {
                            val newId = "CUSTOM_" + System.currentTimeMillis()
                            val now = System.currentTimeMillis()
                            val expiry = if (isPermanentCustom) 0L else now + (customDays * 24L * 60L * 60L * 1000L)

                            val newItem = HomeSectionItem(
                                id = newId,
                                title = customTitle.trim(),
                                subtitle = customSubtitle.trim(),
                                type = "CUSTOM",
                                isVisible = true,
                                isPermanent = isPermanentCustom,
                                durationDays = if (isPermanentCustom) 0 else customDays,
                                expiresAtTimestamp = expiry,
                                temporaryAction = "HIDE_AFTER",
                                customTitle = customTitle.trim(),
                                customSubtitle = customSubtitle.trim(),
                                customActionText = customActionText.trim(),
                                customActionUrl = customActionUrl.trim(),
                                customImageUrl = customImageUrl.trim()
                            )

                            // Add to top of content sections (index 1)
                            val mutable = sectionsList.toMutableList()
                            mutable.add(1.coerceAtMost(mutable.size), newItem)
                            sectionsList = mutable
                            Toast.makeText(context, "कस्टम सेक्शन जोड़ा गया!", Toast.LENGTH_SHORT).show()
                            showAddCustomDialog = false
                        } else {
                            Toast.makeText(context, "कृपया शीर्षक दर्ज करें", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("जोड़ें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    if (showThemeManagerDialog) {
        AdminThemeManagerDialog(
            viewModel = viewModel,
            onDismiss = { showThemeManagerDialog = false }
        )
    }
}
