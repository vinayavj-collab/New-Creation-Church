package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PredefinedPlaylists
import com.example.data.model.VideoQuickAccessConfig
import com.example.data.model.VideoQuickAccessItem
import com.example.data.model.defaultVideoQuickAccessItems
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoQuickAccessManagerDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig by viewModel.videoQuickAccessConfig.collectAsStateWithLifecycle()

    var isBarVisible by remember(currentConfig) { mutableStateOf(currentConfig.isBarVisible) }
    var items by remember(currentConfig) {
        mutableStateOf(currentConfig.items.sortedWith(compareByDescending<VideoQuickAccessItem> { it.isPinned }.thenBy { it.order }))
    }

    var showAddOrEditForm by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }

    var formLabel by remember { mutableStateOf("") }
    var formFilterType by remember { mutableStateOf("KEYWORD") } // "ALL", "CHANNEL", "KEYWORD"
    var formFilterValue by remember { mutableStateOf("") }
    var formIsPinned by remember { mutableStateOf(false) }
    var formIsVisible by remember { mutableStateOf(true) }

    val resetForm = {
        editingItemIndex = null
        formLabel = ""
        formFilterType = "KEYWORD"
        formFilterValue = ""
        formIsPinned = false
        formIsVisible = true
        showAddOrEditForm = false
    }

    val openEditForm: (Int, VideoQuickAccessItem) -> Unit = { index, item ->
        editingItemIndex = index
        formLabel = item.label
        formFilterType = item.filterType
        formFilterValue = item.filterValue
        formIsPinned = item.isPinned
        formIsVisible = item.isVisible
        showAddOrEditForm = true
    }

    val saveConfigToFirebase: (Boolean, List<VideoQuickAccessItem>) -> Unit = { barVis, list ->
        val updatedConfig = VideoQuickAccessConfig(
            isBarVisible = barVis,
            items = list.mapIndexed { idx, itm -> itm.copy(order = idx) }
        )
        viewModel.updateVideoQuickAccessConfig(updatedConfig) { success ->
            if (success) {
                Toast.makeText(context, "क्विक एक्सेस बार सेटिंग्स अपडेट हो गईं! ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "अपडेट करने में विफल!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎬 वीडियो क्विक एक्सेस बार",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "बार को दिखाएं/छिपाएं, नए बार जोड़ें, क्रम बदलें या पिन करें",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Master Toggle: Show/Hide Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBarVisible) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "वीडियो स्क्रीन पर क्विक बार दिखाएं",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isBarVisible) "वर्तमान में सभी यूजर्स को बार दिखाई दे रहा है" else "बार पूरी तरह छिपा हुआ है",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isBarVisible,
                            onCheckedChange = { checked ->
                                isBarVisible = checked
                                saveConfigToFirebase(checked, items)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar: Add New Item & Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            resetForm()
                            showAddOrEditForm = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("नया बार जोड़ें", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val defaultList = defaultVideoQuickAccessItems()
                            items = defaultList
                            isBarVisible = true
                            saveConfigToFirebase(true, defaultList)
                            Toast.makeText(context, "डिफ़ॉल्ट सेटिंग्स रीसेट हो गईं!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("रीसेट", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Form to Add/Edit Item
                if (showAddOrEditForm) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (editingItemIndex == null) "➕ नया क्विक बार / फ़िल्टर जोड़ें" else "✏️ बार संपादित करें",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = formLabel,
                                onValueChange = { formLabel = it },
                                label = { Text("बार / चिप का नाम (Label)") },
                                placeholder = { Text("उदा. आराधना गीत, प्रवचन, गवाही, Shorts") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("फ़िल्टर प्रकार (Filter Type):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = formFilterType == "KEYWORD",
                                    onClick = { formFilterType = "KEYWORD" },
                                    label = { Text("कीवर्ड खोज", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = formFilterType == "CHANNEL",
                                    onClick = { formFilterType = "CHANNEL" },
                                    label = { Text("चैनल", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = formFilterType == "ALL",
                                    onClick = { formFilterType = "ALL"; formFilterValue = "" },
                                    label = { Text("सभी", fontSize = 11.sp) }
                                )
                            }

                            if (formFilterType == "KEYWORD") {
                                OutlinedTextField(
                                    value = formFilterValue,
                                    onValueChange = { formFilterValue = it },
                                    label = { Text("खोजने का कीवर्ड (Keyword)") },
                                    placeholder = { Text("उदा. Worship, Sermon, गीत, गवाही, Live") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (formFilterType == "CHANNEL") {
                                OutlinedTextField(
                                    value = formFilterValue,
                                    onValueChange = { formFilterValue = it },
                                    label = { Text("चैनल ID / नाम") },
                                    placeholder = { Text("UC92tSCn2I6lwcUyAdyS_MMw या dailymotion") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Quick helper chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SuggestionChip(
                                        onClick = { formFilterValue = PredefinedPlaylists.channelWorship.id },
                                        label = { Text("Worship", fontSize = 10.sp) }
                                    )
                                    SuggestionChip(
                                        onClick = { formFilterValue = PredefinedPlaylists.channelMain.id },
                                        label = { Text("Main AVJ", fontSize = 10.sp) }
                                    )
                                    SuggestionChip(
                                        onClick = { formFilterValue = "dailymotion" },
                                        label = { Text("Dailymotion", fontSize = 10.sp) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = formIsPinned,
                                        onCheckedChange = { formIsPinned = it }
                                    )
                                    Text("📌 सबसे आगे पिन करें", fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = formIsVisible,
                                        onCheckedChange = { formIsVisible = it }
                                    )
                                    Text("दिखाएं (Visible)", fontSize = 12.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = resetForm) {
                                    Text("रद्द करें")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (formLabel.isBlank()) {
                                            Toast.makeText(context, "कृपया बार का नाम दर्ज करें!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val updatedList = items.toMutableList()
                                        val editIdx = editingItemIndex
                                        if (editIdx != null && editIdx in updatedList.indices) {
                                            val oldItem = updatedList[editIdx]
                                            updatedList[editIdx] = oldItem.copy(
                                                label = formLabel.trim(),
                                                filterType = formFilterType,
                                                filterValue = formFilterValue.trim(),
                                                isPinned = formIsPinned,
                                                isVisible = formIsVisible
                                            )
                                        } else {
                                            val newItem = VideoQuickAccessItem(
                                                id = "bar_${System.currentTimeMillis()}",
                                                label = formLabel.trim(),
                                                filterType = formFilterType,
                                                filterValue = formFilterValue.trim(),
                                                isVisible = formIsVisible,
                                                isPinned = formIsPinned,
                                                order = updatedList.size
                                            )
                                            updatedList.add(newItem)
                                        }

                                        val sorted = updatedList.sortedWith(
                                            compareByDescending<VideoQuickAccessItem> { it.isPinned }
                                                .thenBy { it.order }
                                        )
                                        items = sorted
                                        saveConfigToFirebase(isBarVisible, sorted)
                                        resetForm()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (editingItemIndex == null) "जोड़ें" else "अपडेट करें")
                                }
                            }
                        }
                    }
                }

                // List of items
                Text(
                    text = "सभी क्विक बार्स (${items.size}):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(items, key = { _, itm -> itm.id }) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isVisible) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${index + 1}. ${item.label}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (item.isPinned) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = GoldWarm,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "📌 PINNED",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E1B4B),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (!item.isVisible) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "छिपा हुआ",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    val filterDesc = when (item.filterType) {
                                        "ALL" -> "सभी वीडियो"
                                        "CHANNEL" -> "चैनल फ़िल्टर: ${item.filterValue}"
                                        "KEYWORD" -> "खोज कीवर्ड: '${item.filterValue}'"
                                        else -> item.filterType
                                    }
                                    Text(
                                        text = filterDesc,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Pin / Unpin button
                                    IconButton(
                                        onClick = {
                                            val updated = items.toMutableList()
                                            val current = updated[index]
                                            updated[index] = current.copy(isPinned = !current.isPinned)
                                            val sorted = updated.sortedWith(
                                                compareByDescending<VideoQuickAccessItem> { it.isPinned }
                                                    .thenBy { it.order }
                                            )
                                            items = sorted
                                            saveConfigToFirebase(isBarVisible, sorted)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PushPin,
                                            contentDescription = if (item.isPinned) "Unpin" else "Pin",
                                            tint = if (item.isPinned) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Show / Hide toggle button
                                    IconButton(
                                        onClick = {
                                            val updated = items.toMutableList()
                                            val current = updated[index]
                                            updated[index] = current.copy(isVisible = !current.isVisible)
                                            items = updated
                                            saveConfigToFirebase(isBarVisible, updated)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Visibility",
                                            tint = if (item.isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Reorder Up
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val updated = items.toMutableList()
                                                val temp = updated[index]
                                                updated[index] = updated[index - 1]
                                                updated[index - 1] = temp
                                                items = updated
                                                saveConfigToFirebase(isBarVisible, updated)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    }

                                    // Reorder Down
                                    IconButton(
                                        onClick = {
                                            if (index < items.size - 1) {
                                                val updated = items.toMutableList()
                                                val temp = updated[index]
                                                updated[index] = updated[index + 1]
                                                updated[index + 1] = temp
                                                items = updated
                                                saveConfigToFirebase(isBarVisible, updated)
                                            }
                                        },
                                        enabled = index < items.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (index < items.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    }

                                    // Edit
                                    IconButton(
                                        onClick = { openEditForm(index, item) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = {
                                            val updated = items.toMutableList()
                                            updated.removeAt(index)
                                            items = updated
                                            saveConfigToFirebase(isBarVisible, updated)
                                            Toast.makeText(context, "${item.label} हटा दिया गया!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("समाप्त (Done)")
                }
            }
        }
    }
}
