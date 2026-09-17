package com.example.ui.prayer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.prayer.model.DailyPrayerVerse
import com.example.data.prayer.repository.FirebaseDailyPrayerManager

@Composable
fun CustomizeDailyPrayerDialog(
    prayer: DailyPrayerVerse,
    prayerManager: FirebaseDailyPrayerManager,
    onDismissRequest: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var topicText by remember { mutableStateOf(prayer.displayTopic) }
    var sermonNotesText by remember { mutableStateOf(prayer.sermonNotes) }
    var announcementsText by remember { mutableStateOf(prayer.announcements) }

    // List of intercession names/points
    val namesList = remember {
        mutableStateListOf<String>().apply {
            addAll(prayer.allIntercessionNames)
        }
    }

    var newNameInput by remember { mutableStateOf("") }
    var syncToFirebaseOnline by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
                .testTag("customize_prayer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "दैनिक प्रार्थना कस्टमाइज़ करें",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Customize Topic, Notes & Prayer List",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Topic
                    Text(
                        text = "1. प्रार्थना का विषय (Topic):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = topicText,
                        onValueChange = { topicText = it },
                        placeholder = { Text("उदा. शांति, चंगाई व अनुग्रह") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayer_topic_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Intercession List / इनके लिए प्रार्थना करें:
                    Text(
                        text = "2. इनके लिए प्रार्थना करें (Prayer List / Names):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "परिवार, मित्रों या विशेष प्रार्थना विषयों के नाम जोड़ें:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Input to add a new name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newNameInput,
                            onValueChange = { newNameInput = it },
                            placeholder = { Text("नाम या विषय लिखें (* उनके नाम)...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("prayer_name_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (newNameInput.isNotBlank()) {
                                    namesList.add(newNameInput.trim())
                                    newNameInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Name")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display list of names with bullet points and delete option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (namesList.isEmpty()) {
                                Text(
                                    text = "अभी कोई नाम नहीं जोड़ा गया है। ऊपर से जोड़ें।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                namesList.forEachIndexed { index, name ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "• ",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        IconButton(
                                            onClick = { namesList.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Sermon Notes (प्रवचन / संदेश नोट्स)
                    Text(
                        text = "3. प्रवचन / संदेश नोट्स (Sermon Notes):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = sermonNotesText,
                        onValueChange = { sermonNotesText = it },
                        placeholder = { Text("आज के वचन का मुख्य संदेश या आध्यात्मिक सीख...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sermon_notes_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Announcements (घोषणाएं / विशेष सूचनाएं)
                    Text(
                        text = "4. घोषणाएं व सूचनाएं (Announcements):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = announcementsText,
                        onValueChange = { announcementsText = it },
                        placeholder = { Text("विशेष प्रार्थना सभा, उपवास प्रार्थना या चर्च सूचनाएं...") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("announcements_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Firebase Online Sync Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = syncToFirebaseOnline,
                            onCheckedChange = { syncToFirebaseOnline = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Firebase क्लाउड पर सिंक करें (Sync to Firebase)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "सभी यूज़र्स के लिए रियल-टाइम अपडेट उपलब्ध होगा",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            prayerManager.resetPrayerCustomization(prayer.id)
                            Toast.makeText(context, "डिफ़ॉल्ट रीसेट किया गया", Toast.LENGTH_SHORT).show()
                            onSaved()
                            onDismissRequest()
                        }
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("डिफ़ॉल्ट (Reset)")
                    }

                    Row {
                        TextButton(onClick = onDismissRequest) {
                            Text("रद्द करें")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                prayerManager.updateCustomPrayer(
                                    prayerId = prayer.id,
                                    topic = topicText,
                                    sermonNotes = sermonNotesText,
                                    announcements = announcementsText,
                                    customNames = namesList.toList(),
                                    syncToFirebase = syncToFirebaseOnline
                                )
                                Toast.makeText(context, "प्रार्थना सफलतापूर्वक सेव की गई!", Toast.LENGTH_SHORT).show()
                                onSaved()
                                onDismissRequest()
                            },
                            modifier = Modifier.testTag("save_prayer_customization_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("सेव करें (Save)")
                        }
                    }
                }
            }
        }
    }
}
