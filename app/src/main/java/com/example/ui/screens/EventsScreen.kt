package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.BlogPost
import com.example.data.model.FellowshipEvent
import com.example.data.model.appStrings
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    viewModel: MainViewModel,
    onPostClick: (BlogPost) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = appStrings()
    val fellowshipEvents by viewModel.fellowshipEvents.collectAsState()
    val blogEvents by viewModel.upcomingEvents.collectAsState()
    val userRsvps by viewModel.userRsvps.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentAdmin by viewModel.currentAdmin.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val currentUserId = userProfile?.deviceId.takeIf { !it.isNullOrBlank() } ?: userProfile?.phoneNumber.takeIf { !it.isNullOrBlank() } ?: "local_user"
    val currentUserName = userProfile?.displayName.takeIf { !it.isNullOrBlank() } ?: "विश्वासी (Believer)"
    val currentUserContact = userProfile?.phoneNumber ?: "N/A"

    val isMasterOrAdmin = currentAdmin != null || com.example.util.ProfileManager.isVinayProfile()
    val canCreateGlobal = isMasterOrAdmin || settings.delegatedGlobalEventCreators.contains(currentUserId)

    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

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
                        locationString = b.locationString ?: "NCCK Sanctuary",
                        category = "Fellowship",
                        startTimestamp = b.startTimestamp,
                        postUrl = b.post.url
                    )
                )
            }
        }
        combined.filter { it.startTimestamp >= todayStart - 86400000L }.sortedBy { it.startTimestamp }
    }

    val eventDates = remember(allEvents) {
        allEvents.map { ev ->
            val cal = Calendar.getInstance().apply { timeInMillis = ev.startTimestamp }
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.time)
        }.distinct()
    }

    var selectedDateFilter by remember { mutableStateOf<String?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showOnlyRsvped by remember { mutableStateOf(false) }
    val categories = listOf("All", "Sunday Worship", "Prayer Meeting", "Bible Study", "Youth Fellowship")

    val myRsvpCount = remember(allEvents, userRsvps) {
        allEvents.count { userRsvps.containsKey(it.id) }
    }

    val filteredEvents = remember(allEvents, selectedDateFilter, selectedCategoryFilter, showOnlyRsvped, userRsvps) {
        allEvents.filter { ev ->
            val cal = Calendar.getInstance().apply { timeInMillis = ev.startTimestamp }
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.time)
            val matchesDate = selectedDateFilter == null || dateStr == selectedDateFilter
            val matchesCat = selectedCategoryFilter == "All" || ev.category.contains(selectedCategoryFilter, ignoreCase = true)
            val matchesRsvp = !showOnlyRsvped || userRsvps.containsKey(ev.id)
            matchesDate && matchesCat && matchesRsvp
        }
    }

    var logisticsEvent by remember { mutableStateOf<FellowshipEvent?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Event",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "पदानुक्रमित संगति और लाइव RSVP लॉजिस्टिक्स",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 आगामी तिथियां (Dates)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GoldWarm
                        )
                        if (selectedDateFilter != null) {
                            TextButton(onClick = { selectedDateFilter = null }) {
                                Text("सभी दिखाएं (Show All)", fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    val calendarList = remember {
                        val list = mutableListOf<Calendar>()
                        val cal = Calendar.getInstance()
                        for (i in 0 until 14) {
                            list.add(cal.clone() as Calendar)
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        list
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(calendarList) { cal ->
                            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.time)
                            val dayNum = SimpleDateFormat("dd", Locale.ENGLISH).format(cal.time)
                            val dayName = SimpleDateFormat("EEE", Locale.ENGLISH).format(cal.time)
                            val hasEvent = eventDates.contains(dateKey)
                            val isSelected = selectedDateFilter == dateKey

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) GoldWarm else if (hasEvent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .width(54.dp)
                                    .clickable {
                                        selectedDateFilter = if (isSelected) null else dateKey
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = dayNum,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (hasEvent) (if (isSelected) Color.White else GoldWarm) else Color.Transparent)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = !showOnlyRsvped,
                            onClick = { showOnlyRsvped = false },
                            label = { Text("सभी कार्यक्रम", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        FilterChip(
                            selected = showOnlyRsvped,
                            onClick = { showOnlyRsvped = true },
                            label = { Text("मेरे RSVP (${myRsvpCount})", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (showOnlyRsvped) MaterialTheme.colorScheme.primary else GoldWarm
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (filteredEvents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                val emptyMsg = if (showOnlyRsvped) {
                                    "आपने अभी तक किसी कार्यक्रम के लिए RSVP नहीं किया है।"
                                } else {
                                    "इस तिथि या श्रेणी के लिए कोई कार्यक्रम नहीं है।"
                                }
                                Text(emptyMsg, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    items(filteredEvents, key = { it.id }) { event ->
                        val isRsvped = userRsvps.containsKey(event.id)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoldWarm.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = event.category,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldWarm,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.clickable {
                                            logisticsEvent = event
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "RSVP: ${event.rsvpCount.coerceAtLeast(if (isRsvped) 1 else 0)} लोग",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(text = event.dateString, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(12.dp))
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(text = event.timeString, fontSize = 13.sp)
                                }

                                if (event.locationString.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(text = event.locationString, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = event.description,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (isRsvped) {
                                                viewModel.cancelFellowshipRsvp(event.id)
                                                Toast.makeText(context, "RSVP रद्द किया गया", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.submitFellowshipRsvp(
                                                    eventId = event.id,
                                                    userName = currentUserName,
                                                    userContact = currentUserContact,
                                                    status = "GOING",
                                                    attendeesCount = 1,
                                                    onSuccess = { Toast.makeText(context, "RSVP सफल! (मैं आऊंगा)", Toast.LENGTH_SHORT).show() },
                                                    onError = { err -> Toast.makeText(context, "त्रुटि: $err", Toast.LENGTH_SHORT).show() }
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRsvped) MaterialTheme.colorScheme.secondary else GoldWarm
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isRsvped) Icons.Default.Check else Icons.Default.EventAvailable,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isRsvped) "Attending ✓ (मैं आ रहा हूँ)" else "मैं आऊंगा (RSVP GOING)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_INSERT).apply {
                                                        data = CalendarContract.Events.CONTENT_URI
                                                        putExtra(CalendarContract.Events.TITLE, event.title)
                                                        putExtra(CalendarContract.Events.DESCRIPTION, event.description)
                                                        if (event.locationString.isNotBlank()) putExtra(CalendarContract.Events.EVENT_LOCATION, event.locationString)
                                                        if (event.startTimestamp > 0) {
                                                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTimestamp)
                                                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.startTimestamp + 7200000L)
                                                        }
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "कैलेंडर नहीं खुल सका", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = "Add to Calendar", tint = NavyPrimary)
                                        }

                                        IconButton(
                                            onClick = {
                                                val text = "📅 ${event.title}\n🗓 ${event.dateString} at ${event.timeString}\n📍 ${event.locationString}\n\n${event.description}"
                                                val shareIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, text)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share Event"))
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = GoldWarm)
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

    if (logisticsEvent != null) {
        ModalBottomSheet(
            onDismissRequest = { logisticsEvent = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📋 RSVP लॉजिस्टिक्स एनालिटिक्स",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GoldWarm
                        )
                        Text(
                            text = logisticsEvent?.title ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { logisticsEvent = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "पुष्ट उपस्थित विश्वासी (Confirmed Attendees)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ListItem(
                            headlineContent = { Text(currentUserName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("संपर्क: $currentUserContact • स्थिति: GOING (1 व्यक्ति)") },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GoldWarm),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(currentUserName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            trailingContent = {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text("Verified ✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
