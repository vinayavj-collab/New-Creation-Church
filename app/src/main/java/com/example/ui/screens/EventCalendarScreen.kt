package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlogPost
import com.example.data.model.FellowshipEvent
import com.example.data.model.UpcomingEvent
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCalendarScreen(
    viewModel: MainViewModel,
    onPostClick: (BlogPost) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fellowshipEvents by viewModel.fellowshipEvents.collectAsState()
    val blogEvents by viewModel.upcomingEvents.collectAsState()
    val userRsvps by viewModel.userRsvps.collectAsState()

    // Combine Firebase fellowship events and blog events
    val allEvents = remember(fellowshipEvents, blogEvents) {
        val combined = mutableListOf<FellowshipEvent>()
        combined.addAll(fellowshipEvents)
        for (b in blogEvents) {
            if (combined.none { it.title.equals(b.title, ignoreCase = true) || it.id == b.post.id }) {
                combined.add(
                    FellowshipEvent(
                        id = b.post.id,
                        title = b.title,
                        description = b.post.plainTextExcerpt,
                        dateString = b.dateString,
                        timeString = b.timeString ?: "10:00 AM",
                        locationString = b.locationString ?: "NCCK Main Sanctuary",
                        category = "Fellowship",
                        startTimestamp = b.startTimestamp,
                        postUrl = b.post.url
                    )
                )
            }
        }
        combined.sortedBy { it.startTimestamp }
    }

    val categories = listOf("All", "Sunday Worship", "Prayer Meeting", "Bible Study", "Youth Fellowship")
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredEvents = remember(allEvents, selectedCategory) {
        if (selectedCategory == "All") {
            allEvents
        } else {
            allEvents.filter { it.category.contains(selectedCategory, ignoreCase = true) }
        }
    }

    var selectedEventForRsvp by remember { mutableStateOf<FellowshipEvent?>(null) }

    val openGoogleCalendar = { event: FellowshipEvent ->
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, event.title)
                putExtra(
                    CalendarContract.Events.DESCRIPTION,
                    "${event.description}\n\nPastor/Speaker: ${event.speaker.ifBlank { "Vinay Kumar AVJ" }}\n${event.postUrl.ifBlank { event.meetingUrl }}"
                )
                if (event.locationString.isNotBlank()) {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, event.locationString)
                }
                if (event.startTimestamp > 0) {
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTimestamp)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.startTimestamp + (2 * 60 * 60 * 1000L))
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch calendar app", Toast.LENGTH_SHORT).show()
        }
    }

    val openLocation = { loc: String ->
        try {
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(loc))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open map", Toast.LENGTH_SHORT).show()
        }
    }

    val openMeeting = { url: String ->
        try {
            val targetUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open meeting link", Toast.LENGTH_SHORT).show()
        }
    }

    val shareEvent = { event: FellowshipEvent ->
        try {
            val text = """
📅 FELLOWSHIP EVENT

📌 ${event.title}
🗓 Date: ${event.dateString}
⏰ Time: ${event.timeString}
📍 Venue: ${event.locationString}
👤 Speaker: ${event.speaker.ifBlank { "Pastor Vinay Kumar AVJ" }}

${event.description.take(200)}

${if (event.meetingUrl.isNotBlank()) "🔗 Join Online: ${event.meetingUrl}\n" else ""}${if (event.postUrl.isNotBlank()) "📖 Details: ${event.postUrl}" else ""}
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "Share Fellowship Event"))
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "📅 Fellowship Calendar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Live Events & RSVP (संगति कार्यक्रम)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Filter Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 13.sp) },
                            leadingIcon = if (selectedCategory == cat) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = NavyPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No events in this category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Check back soon or select another category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredEvents, key = { it.id }) { event ->
                        val userRsvp = userRsvps[event.id]

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Header: Category chip & RSVP count badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = NavyPrimary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = event.category.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    if (event.rsvpCount > 0 || userRsvp != null) {
                                        Surface(
                                            color = GoldWarm.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.People,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = GoldWarm
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${event.rsvpCount} Attending",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NavyPrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Title
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (event.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = event.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Date & Time
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = event.dateString,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (event.timeString.isNotBlank()) {
                                        Text(
                                            text = " • ${event.timeString}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Location / Online
                                if (event.locationString.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { openLocation(event.locationString) }
                                    ) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = event.locationString,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Speaker
                                if (event.speaker.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Speaker: ${event.speaker}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // User RSVP Status Banner
                                if (userRsvp != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = if (userRsvp.status == "GOING") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    if (userRsvp.status == "GOING") Icons.Default.CheckCircle else Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = if (userRsvp.status == "GOING") Color(0xFF2E7D32) else Color(0xFFE65100),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (userRsvp.status == "GOING") "You are attending (${userRsvp.attendeesCount} seat${if (userRsvp.attendeesCount > 1) "s" else ""})" else "RSVP: ${userRsvp.status}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (userRsvp.status == "GOING") Color(0xFF2E7D32) else Color(0xFFE65100)
                                                )
                                            }

                                            TextButton(
                                                onClick = { viewModel.cancelFellowshipRsvp(event.id) },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                            ) {
                                                Text("Change", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Actions: RSVP + Calendar + Join/Share
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { selectedEventForRsvp = event },
                                        modifier = Modifier.weight(1.2f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (userRsvp != null) Color(0xFF2E7D32) else NavyPrimary
                                        ),
                                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
                                    ) {
                                        Icon(
                                            if (userRsvp != null) Icons.Default.Check else Icons.Default.HowToReg,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (userRsvp != null) "Attending ✅" else "RSVP Now",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { openGoogleCalendar(event) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Calendar", fontSize = 12.sp, maxLines = 1)
                                    }

                                    if (event.meetingUrl.isNotBlank() && event.isOnline) {
                                        FilledTonalButton(
                                            onClick = { openMeeting(event.meetingUrl) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)
                                        ) {
                                            Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Join Live", fontSize = 12.sp, maxLines = 1)
                                        }
                                    } else {
                                        IconButton(onClick = { shareEvent(event) }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = NavyPrimary)
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

    // RSVP Dialog / Sheet
    selectedEventForRsvp?.let { event ->
        RsvpSubmissionDialog(
            event = event,
            currentRsvp = userRsvps[event.id],
            onDismiss = { selectedEventForRsvp = null },
            onSubmit = { name, contact, status, count, notes ->
                viewModel.submitFellowshipRsvp(
                    eventId = event.id,
                    userName = name,
                    userContact = contact,
                    status = status,
                    attendeesCount = count,
                    notes = notes,
                    onSuccess = {
                        Toast.makeText(context, "RSVP confirmed! See you at the fellowship.", Toast.LENGTH_SHORT).show()
                        selectedEventForRsvp = null
                    },
                    onError = { err ->
                        Toast.makeText(context, "RSVP updated locally ($err)", Toast.LENGTH_SHORT).show()
                        selectedEventForRsvp = null
                    }
                )
            }
        )
    }
}

@Composable
fun RsvpSubmissionDialog(
    event: FellowshipEvent,
    currentRsvp: com.example.data.model.EventRsvp?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, contact: String, status: String, count: Int, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(currentRsvp?.userName.orEmpty()) }
    var contact by remember { mutableStateOf(currentRsvp?.userContact.orEmpty()) }
    var selectedStatus by remember { mutableStateOf(currentRsvp?.status ?: "GOING") }
    var count by remember { mutableIntStateOf(currentRsvp?.attendeesCount ?: 1) }
    var notes by remember { mutableStateOf(currentRsvp?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Confirm Your Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("GOING" to "Going ✅", "INTERESTED" to "Interested", "NOT_GOING" to "Can't Go").forEach { (st, label) ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (st == "GOING") Color(0xFF2E7D32) else NavyPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name (आपका नाम)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Contact / Phone Input
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Phone / WhatsApp (फ़ोन नंबर)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                // Attendees Count (if going)
                if (selectedStatus == "GOING") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Family Members / Seats:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (count > 1) count-- },
                                enabled = count > 1
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                            }

                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = { if (count < 10) count++ },
                                enabled = count < 10
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                            }
                        }
                    }
                }

                // Optional Prayer Request / Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Prayer Request (वैकल्पिक)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = name.trim().ifBlank { "Guest Attendee" }
                    val finalContact = contact.trim().ifBlank { "N/A" }
                    onSubmit(finalName, finalContact, selectedStatus, count, notes.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Submit RSVP")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
