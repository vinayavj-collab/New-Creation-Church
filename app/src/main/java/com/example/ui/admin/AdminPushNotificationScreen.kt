package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminPushNotification
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPushNotificationScreen(
    notifications: List<AdminPushNotification>,
    onSendNotification: (AdminPushNotification) -> Unit,
    onDeleteNotification: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var targetAudience by remember { mutableStateOf("सभी विश्वासी (All)") }
    var urgency by remember { mutableStateOf("सामान्य (Normal)") }
    var actionUrl by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val audienceOptions = listOf(
        "सभी विश्वासी (All)",
        "युवा संगति (Youth)",
        "महिला संगति (Women)",
        "संडे स्कूल (Sunday School)",
        "प्रार्थना योद्धा (Prayer Warriors)",
        "प्रशासक मंडल (Admins)"
    )

    val urgencyOptions = listOf(
        "सामान्य (Normal)",
        "महत्वपूर्ण (Important)",
        "आपातकालीन प्रार्थना (Urgent Prayer)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "पुश नोटिफिकेशन व संदेश प्रसारण",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "विश्वासियों तक तुरंत सूचना पहुंचाएं",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("push_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val uri = com.example.util.CsvExportHelper.exportPushNotificationsToCsv(context, notifications)
                            if (uri != null) {
                                com.example.util.CsvExportHelper.shareCsvFile(
                                    context,
                                    uri,
                                    "पुश नोटिफिकेशन इतिहास CSV रिपोर्ट"
                                )
                            } else {
                                Toast.makeText(context, "CSV फ़ाइल बनाने में त्रुटि हुई", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("export_push_csv_button")
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "CSV डाउनलोड (Export CSV)",
                            tint = com.example.ui.theme.GoldWarm
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "नया प्रसारण संदेश बनाएं",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Target Audience selector
                        Text("लक्षित वर्ग (Target Audience):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(audienceOptions) { aud ->
                                FilterChip(
                                    selected = targetAudience == aud,
                                    onClick = { targetAudience = aud },
                                    label = { Text(aud.substringBefore(" ("), fontSize = 12.sp) },
                                    leadingIcon = if (targetAudience == aud) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }

                        // Urgency selector
                        Text("प्राथमिकता (Priority):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(urgencyOptions) { urg ->
                                FilterChip(
                                    selected = urgency == urg,
                                    onClick = { urgency = urg },
                                    label = { Text(urg.substringBefore(" ("), fontSize = 12.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("शीर्षक (Title) *") },
                            placeholder = { Text("उदा. विशेष प्रार्थना सभा आज शाम 6:00 बजे") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("push_title_input")
                        )

                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = { Text("संदेश का विवरण (Message Body) *") },
                            placeholder = { Text("सभी भाई-बहन समय पर उपस्थित हों...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("push_body_input"),
                            minLines = 3
                        )

                        OutlinedTextField(
                            value = actionUrl,
                            onValueChange = { actionUrl = it },
                            label = { Text("लिंक / यूट्यूब URL (वैकल्पिक)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Live Preview Box
                        if (title.isNotBlank() || body.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("पूर्वावलोकन (Notification Preview)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = title.ifBlank { "शीर्षक" }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = body.ifBlank { "संदेश का विवरण" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank() && body.isNotBlank()) {
                                    isSending = true
                                    val notif = AdminPushNotification(
                                        title = title.trim(),
                                        body = body.trim(),
                                        targetAudience = targetAudience,
                                        urgency = urgency,
                                        actionUrl = actionUrl.trim()
                                    )
                                    onSendNotification(notif)
                                    title = ""
                                    body = ""
                                    actionUrl = ""
                                    isSending = false
                                    Toast.makeText(context, "सूचना सफलतापूर्वक प्रसारित की गई!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = title.isNotBlank() && body.isNotBlank() && !isSending,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("send_push_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("अभी प्रसारित करें (Broadcast Now)")
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "प्रसारित सूचनाओं का इतिहास",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${notifications.size} भेजे गए",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (notifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "अभी तक कोई नोटिफिकेशन प्रसारित नहीं किया गया है",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(notifications, key = { it.id }) { notif ->
                    SentPushCard(
                        notification = notif,
                        onDelete = { onDeleteNotification(notif.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SentPushCard(
    notification: AdminPushNotification,
    onDelete: () -> Unit
) {
    val dateStr = remember(notification.timestamp) {
        SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(notification.timestamp))
    }

    val urgencyColor = when {
        notification.urgency.contains("Urgent") || notification.urgency.contains("आपातकालीन") -> Color(0xFFD32F2F)
        notification.urgency.contains("Important") || notification.urgency.contains("महत्वपूर्ण") -> Color(0xFFF57C00)
        else -> Color(0xFF1976D2)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(urgencyColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = urgencyColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notification.body,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(notification.targetAudience, fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(notification.urgency, fontSize = 11.sp) }
                )
            }

            if (notification.sentByAdmin.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "प्रेषक: ${notification.sentByAdmin}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
