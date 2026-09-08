package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.EventExtractor
import com.example.data.model.UpcomingEvent
import com.example.data.model.appStrings
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.util.ReminderScheduler

@Composable
fun EventCard(
    event: UpcomingEvent,
    onViewDetails: () -> Unit,
    onScheduleReminder: ((ReminderScheduler.ReminderOffset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = appStrings()
    var showReminderDialog by remember { mutableStateOf(false) }

    val openCalendar = {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, event.title)
                putExtra(
                    CalendarContract.Events.DESCRIPTION,
                    "Vinay Kumar AVJ Fellowship Event\n\nRead more: ${event.post.url}"
                )
                if (!event.locationString.isNullOrBlank()) {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, event.locationString)
                }
                if (event.startTimestamp > 0) {
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTimestamp)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.startTimestamp + (2 * 60 * 60 * 1000L))
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open calendar app", Toast.LENGTH_SHORT).show()
        }
    }

    val openLocation = {
        val query = event.locationString ?: event.title
        try {
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open map app", Toast.LENGTH_SHORT).show()
        }
    }

    val shareEvent = {
        val text = EventExtractor.generateEventShareText(event)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Event"))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Event image
            if (!event.post.featuredImageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(event.post.featuredImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = event.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        color = NavyPrimary.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(bottomEnd = 12.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "📅 UPCOMING EVENT",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.dateString,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Time row if present
                if (!event.timeString.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = GoldWarm,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = event.timeString,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Location row if present
                if (!event.locationString.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = event.locationString,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: View Details, Add to Calendar, Open Location, Remind Me
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onViewDetails,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text(strings.viewDetails, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = openCalendar,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.addToCalendar, fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!event.locationString.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = openLocation,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.openLocation, fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = { showReminderDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.remindMe, fontSize = 11.sp, maxLines = 1)
                    }

                    IconButton(
                        onClick = shareEvent,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = strings.share
                        )
                    }
                }
            }
        }
    }

    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Set Event Reminder") },
            text = {
                Column {
                    Text(
                        text = "Choose when you would like to be reminded for \"${event.title}\":",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ReminderScheduler.ReminderOffset.entries.forEach { offset ->
                        OutlinedButton(
                            onClick = {
                                onScheduleReminder?.invoke(offset)
                                val success = ReminderScheduler.scheduleReminder(
                                    context = context,
                                    postId = event.post.id,
                                    eventTitle = event.title,
                                    eventDate = event.dateString,
                                    eventTimestamp = event.startTimestamp,
                                    offset = offset
                                )
                                showReminderDialog = false
                                if (success) {
                                    Toast.makeText(context, "Reminder set for ${offset.label}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Event is too soon or already passed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(offset.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
