package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.MainViewModel

@Composable
fun WelcomeCustomizationDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    var nameInput by remember { mutableStateOf(settings.userName) }
    var welcomeSpeechEnabled by remember { mutableStateOf(settings.enableWelcomeSpeech) }
    var verseSpeechEnabled by remember { mutableStateOf(settings.enableVerseSpeechOnLaunch) }
    var welcomeOnceDay by remember { mutableStateOf(settings.welcomeSpeechOncePerDay) }
    var verseOnceDay by remember { mutableStateOf(settings.verseSpeechOncePerDay) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Icon & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "स्वागत एवं नाम कस्टमाइज़ेशन",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Welcome Speech & Scripture TTS",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name Input Section
                Text(
                    text = "अपना नाम दर्ज करें (अभिवादन हेतु):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("उदाहरण: विनय") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (nameInput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🔊 Voice Sample: \"${nameInput.trim()} जी, जय मसीह की\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚠️ नाम खाली होने पर केवल 'आज का वचन है' TTS द्वारा बोला जाएगा.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Option 1: Welcome Speech Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "नाम के साथ अभिवादन सुनें",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "ऐप खोलने पर: '[नाम] जी, जय मसीह की'",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = welcomeSpeechEnabled,
                        onCheckedChange = { welcomeSpeechEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Today's Scripture Speech Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "अभिवादन के बाद 'आज का वचन' सुनें",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "'आज का वचन है' कहकर आज का वचन पढ़ेगा",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = verseSpeechEnabled,
                        onCheckedChange = { verseSpeechEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 3: Welcome Once Per Day
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "हर दिन केवल एक बार अभिवादन",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "दिन में बार-बार ऐप खोलने पर दोहराएगा नहीं",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = welcomeOnceDay,
                        onCheckedChange = { welcomeOnceDay = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 4: Verse Once Per Day
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "दिन में केवल एक ही बार वचन सुनें",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "आज का वचन दिन में सिर्फ पहली बार बोला जाएगा",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = verseOnceDay,
                        onCheckedChange = { verseOnceDay = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Test Button
                    OutlinedButton(
                        onClick = {
                            viewModel.updateUserName(nameInput)
                            viewModel.testWelcomeSpeech()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ध्वनि टेस्ट")
                    }

                    // Save Button
                    Button(
                        onClick = {
                            viewModel.updateUserName(nameInput)
                            viewModel.updateEnableWelcomeSpeech(welcomeSpeechEnabled)
                            viewModel.updateEnableVerseSpeechOnLaunch(verseSpeechEnabled)
                            viewModel.updateWelcomeSpeechOncePerDay(welcomeOnceDay)
                            viewModel.updateVerseSpeechOncePerDay(verseOnceDay)
                            viewModel.updateWelcomeDialogDismissed(true)

                            // Trigger speech
                            viewModel.triggerWelcomeSpeechOnLaunch()

                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("सहेजें (Save)")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = {
                        viewModel.updateWelcomeDialogDismissed(true)
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("बाद में (Later / Skip)")
                }
            }
        }
    }
}
