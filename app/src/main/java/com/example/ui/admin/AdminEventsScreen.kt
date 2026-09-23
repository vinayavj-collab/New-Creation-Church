package com.example.ui.admin

import android.content.Intent
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FellowshipEvent
import com.example.ui.theme.GoldWarm
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEventsScreen(
    events: List<FellowshipEvent>,
    onSaveEvent: (FellowshipEvent) -> Unit,
    onDeleteEvent: (FellowshipEvent) -> Unit,
    delegatedCreators: List<String> = emptyList(),
    onUpdateDelegatedCreators: (List<String>) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<FellowshipEvent?>(null) }
    var eventToDelete by remember { mutableStateOf<FellowshipEvent?>(null) }

    val categories = listOf(
        "ALL" to "सभी",
        "Sunday Worship" to "रविवार आराधना",
        "Prayer Meeting" to "प्रार्थना सभा",
        "Bible Study" to "बाइबल अध्ययन",
        "Youth Fellowship" to "युवा संगति",
        "Special Conference" to "विशेष सम्मेलन"
    )

    val filteredEvents = remember(events, searchQuery, selectedCategoryFilter) {
        events.filter { event ->
            val matchesCategory = if (selectedCategoryFilter == "ALL") true
            else event.category.contains(selectedCategoryFilter, ignoreCase = true)

            val matchesSearch = if (searchQuery.isBlank()) true
            else event.title.contains(searchQuery, ignoreCase = true) ||
                    event.speaker.contains(searchQuery, ignoreCase = true) ||
                    event.locationString.contains(searchQuery, ignoreCase = true) ||
                    event.description.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }.sortedByDescending { it.startTimestamp }
    }

    val onlineCount = remember(events) { events.count { it.isOnline || it.meetingUrl.isNotBlank() } }
    val totalRsvps = remember(events) { events.sumOf { it.rsvpCount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "कार्यक्रम प्रबंधन",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Events",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${events.size} कार्यक्रम रिकॉर्ड • $totalRsvps कुल उपस्थिति",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_events_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingEvent = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier.testTag("admin_events_add_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingEvent = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("नया कार्यक्रम") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("admin_events_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quick Stats Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventStatCard(
                        title = "कुल सभाएं",
                        value = "${events.size}",
                        icon = Icons.Default.Event,
                        bgColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    EventStatCard(
                        title = "ऑनलाइन सभाएं",
                        value = "$onlineCount",
                        icon = Icons.Default.Videocam,
                        bgColor = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                    EventStatCard(
                        title = "कुल RSVPs",
                        value = "$totalRsvps",
                        icon = Icons.Default.Group,
                        bgColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Event Permissions & Delegation Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldWarm)
                            Spacer(Modifier.width(8.dp))
                            Text("इवेंट अधिकार व बहु-उपयोगकर्ता डेलिगेशन", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "मास्टर एडमिन: चयनित पास्टर/बिशप्स को ग्लोबल इवेंट्स बनाने की अनुमति दें",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))

                        val sampleLeaders = listOf(
                            "pastor_john" to "पास्टर जॉन (Pastor John)",
                            "bishop_samuel" to "बिशप सैमुअल (Bishop Samuel)",
                            "elder_david" to "एल्डर डेविड (Elder David)",
                            "pastor_paul" to "पास्टर पॉल (Pastor Paul)"
                        )

                        sampleLeaders.forEach { (id, name) ->
                            val isDelegated = delegatedCreators.contains(id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val updated = if (isDelegated) delegatedCreators - id else delegatedCreators + id
                                        onUpdateDelegatedCreators(updated)
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Checkbox(
                                    checked = isDelegated,
                                    onCheckedChange = { checked ->
                                        val updated = if (checked) delegatedCreators + id else delegatedCreators - id
                                        onUpdateDelegatedCreators(updated)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_events_search_field"),
                    placeholder = { Text("कार्यक्रम, वक्ता या स्थान खोजें...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { (catKey, catLabel) ->
                        val isSelected = selectedCategoryFilter == catKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = catKey },
                            label = { Text(catLabel, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            // Event List Items
            if (filteredEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedCategoryFilter != "ALL") {
                                    "कोई कार्यक्रम नहीं मिला"
                                } else {
                                    "अभी तक कोई संगति कार्यक्रम नहीं जोड़ा गया है"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "नया कार्यक्रम जोड़ने के लिए नीचे दिए गए बटन पर टैप करें।",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredEvents, key = { it.id }) { event ->
                    AdminEventCard(
                        event = event,
                        onEdit = {
                            editingEvent = event
                            showAddEditDialog = true
                        },
                        onDelete = {
                            eventToDelete = event
                        },
                        onShare = {
                            val shareText = """
                                📅 ${event.title}
                                🏷️ श्रेणी: ${event.category}
                                📆 दिनांक: ${event.dateString}
                                ⏰ समय: ${event.timeString}
                                📍 स्थान: ${event.locationString}
                                👤 वक्ता: ${event.speaker.ifBlank { "Pastor Vinay Kumar AVJ" }}
                                ${if (event.meetingUrl.isNotBlank()) "🔗 मीटिंग लिंक: " + event.meetingUrl else ""}
                                
                                ${event.description}
                            """.trimIndent()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "कार्यक्रम साझा करें"))
                        }
                    )
                }
            }

            // Bottom space for FAB
            item {
                Spacer(Modifier.height(72.dp))
            }
        }
    }

    // Add / Edit Event Dialog
    if (showAddEditDialog) {
        AddEditFellowshipEventDialog(
            initialEvent = editingEvent,
            onDismiss = {
                showAddEditDialog = false
                editingEvent = null
            },
            onSave = { savedEvent ->
                onSaveEvent(savedEvent)
                showAddEditDialog = false
                editingEvent = null
            }
        )
    }

    // Delete Confirmation Dialog
    eventToDelete?.let { ev ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = {
                Text("कार्यक्रम हटाएं?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("क्या आप सचमुच '${ev.title}' कार्यक्रम को हटाना चाहते हैं? यह क्रिया पूर्ववत नहीं की जा सकती।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEvent(ev)
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हटाएं")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { eventToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun EventStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = bgColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AdminEventCard(
    event: FellowshipEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_event_card_${event.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category Badge & Status / Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getCategoryBadgeColor(event.category).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = event.category.ifBlank { "General" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = getCategoryBadgeColor(event.category),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (event.isOnline || event.meetingUrl.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF7C3AED).copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Online",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${event.rsvpCount} RSVP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title
            Text(
                text = event.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (event.speaker.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "वक्ता: ${event.speaker}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Date & Time & Venue Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = event.dateString.ifBlank { "तिथि निर्धारित नहीं" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = event.timeString.ifBlank { "समय निर्धारित नहीं" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (event.locationString.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = event.locationString,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (event.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = event.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("admin_event_edit_${event.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("admin_event_delete_${event.id}")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFellowshipEventDialog(
    initialEvent: FellowshipEvent?,
    onDismiss: () -> Unit,
    onSave: (FellowshipEvent) -> Unit
) {
    val isEdit = initialEvent != null
    var title by remember { mutableStateOf(initialEvent?.title.orEmpty()) }
    var category by remember { mutableStateOf(initialEvent?.category?.ifBlank { "Sunday Worship" } ?: "Sunday Worship") }
    var dateString by remember { mutableStateOf(initialEvent?.dateString.orEmpty()) }
    var timeString by remember { mutableStateOf(initialEvent?.timeString.orEmpty()) }
    var locationString by remember { mutableStateOf(initialEvent?.locationString.orEmpty()) }
    var speaker by remember { mutableStateOf(initialEvent?.speaker.orEmpty()) }
    var isOnline by remember { mutableStateOf(initialEvent?.isOnline ?: false) }
    var meetingUrl by remember { mutableStateOf(initialEvent?.meetingUrl.orEmpty()) }
    var imageUrl by remember { mutableStateOf(initialEvent?.imageUrl.orEmpty()) }
    var description by remember { mutableStateOf(initialEvent?.description.orEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetCategories = listOf(
        "Sunday Worship",
        "Prayer Meeting",
        "Bible Study",
        "Youth Fellowship",
        "Special Conference",
        "Fasting Prayer",
        "General"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "कार्यक्रम विवरण संपादित करें" else "नया संगति कार्यक्रम जोड़ें",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("कार्यक्रम का नाम / शीर्षक *") },
                    placeholder = { Text("उदा. पुनरुत्थान सम्मेलन 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Chips
                Text(
                    text = "कार्यक्रम श्रेणी (Category)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presetCategories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Date & Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dateString,
                        onValueChange = { dateString = it },
                        label = { Text("दिनांक (Date) *") },
                        placeholder = { Text("उदा. 25 Oct 2026") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeString,
                        onValueChange = { timeString = it },
                        label = { Text("समय (Time) *") },
                        placeholder = { Text("उदा. 10:00 AM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Speaker
                OutlinedTextField(
                    value = speaker,
                    onValueChange = { speaker = it },
                    label = { Text("प्रचारक / मुख्य वक्ता") },
                    placeholder = { Text("उदा. Pastor Vinay Kumar AVJ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Location / Venue
                OutlinedTextField(
                    value = locationString,
                    onValueChange = { locationString = it },
                    label = { Text("स्थान / चर्च हॉल (Venue)") },
                    placeholder = { Text("उदा. NCCK Main Sanctuary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Online Toggle & Meeting URL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ऑनलाइन / ज़ूम सभा (Online Meeting)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "क्या विश्वासी ऑनलाइन जुड़ सकते हैं?",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { isOnline = it }
                    )
                }

                if (isOnline) {
                    OutlinedTextField(
                        value = meetingUrl,
                        onValueChange = { meetingUrl = it },
                        label = { Text("ज़ूम / मीट / यूट्यूब लिंक (Meeting URL)") },
                        placeholder = { Text("https://zoom.us/j/... या Meet link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Optional Image URL
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("बैनर / पोस्टर इमेज URL (वैकल्पिक)") },
                    placeholder = { Text("https://...poster.jpg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("कार्यक्रम का विवरण व संदेश (Description)") },
                    placeholder = { Text("विशेष आराधना, प्रार्थना के विषय, और दिशानिर्देश...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "कृपया कार्यक्रम का शीर्षक दर्ज करें।"
                        return@Button
                    }
                    if (dateString.isBlank()) {
                        errorMessage = "कृपया कार्यक्रम की दिनांक दर्ज करें।"
                        return@Button
                    }

                    val event = (initialEvent ?: FellowshipEvent()).copy(
                        title = title.trim(),
                        category = category.trim(),
                        dateString = dateString.trim(),
                        timeString = timeString.trim().ifBlank { "10:00 AM" },
                        locationString = locationString.trim().ifBlank { "NCCK Main Sanctuary" },
                        speaker = speaker.trim(),
                        isOnline = isOnline,
                        meetingUrl = meetingUrl.trim(),
                        imageUrl = imageUrl.trim(),
                        description = description.trim()
                    )
                    onSave(event)
                },
                modifier = Modifier.testTag("admin_event_dialog_save_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isEdit) "सेव करें (Update)" else "कार्यक्रम सेव करें (Save)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

private fun getCategoryBadgeColor(category: String): Color {
    return when {
        category.contains("Sunday", ignoreCase = true) || category.contains("Worship", ignoreCase = true) -> Color(0xFF2563EB)
        category.contains("Prayer", ignoreCase = true) || category.contains("प्रार्थना", ignoreCase = true) -> Color(0xFF059669)
        category.contains("Youth", ignoreCase = true) || category.contains("युवा", ignoreCase = true) -> Color(0xFFD97706)
        category.contains("Bible", ignoreCase = true) || category.contains("बाइबल", ignoreCase = true) -> Color(0xFF7C3AED)
        category.contains("Conference", ignoreCase = true) || category.contains("सम्मेलन", ignoreCase = true) -> Color(0xFFDB2777)
        else -> Color(0xFF4B5563)
    }
}
