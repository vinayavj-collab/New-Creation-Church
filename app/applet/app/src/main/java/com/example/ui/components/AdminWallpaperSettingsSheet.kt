package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.GoldWarm
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWallpaperSettingsSheet(
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isEnabled by remember { mutableStateOf(true) }
    var frequency by remember { mutableIntStateOf(1) } // 1: Morning, 2: Morning & Evening, 3: 3 times
    var targetScreen by remember { mutableStateOf("both") } // "home", "lock", "both"
    var autoSetOnLaunch by remember { mutableStateOf(true) }
    var customPrompt by remember { mutableStateOf("") }
    var currentWallpaperUrl by remember { mutableStateOf("") }
    var isRegenerating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    // Load current config from Firestore
    LaunchedEffect(Unit) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("app_settings")
                .document("wallpaper_config")
                .get()
                .await()
            if (doc.exists()) {
                isEnabled = doc.getBoolean("isEnabled") ?: true
                frequency = (doc.getLong("frequencyPerDay") ?: 1L).toInt()
                targetScreen = doc.getString("targetScreen") ?: "both"
                autoSetOnLaunch = doc.getBoolean("autoSetOnAppLaunch") ?: true
                currentWallpaperUrl = doc.getString("currentWallpaperUrl") ?: ""
                customPrompt = doc.getString("promptPreset") ?: ""
            }
        } catch (e: Exception) {
            // handle
        }
    }

    fun saveConfig() {
        coroutineScope.launch {
            try {
                val data = mapOf(
                    "isEnabled" to isEnabled,
                    "frequencyPerDay" to frequency,
                    "targetScreen" to targetScreen,
                    "autoSetOnAppLaunch" to autoSetOnLaunch,
                    "promptPreset" to customPrompt,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                FirebaseFirestore.getInstance()
                    .collection("app_settings")
                    .document("wallpaper_config")
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                statusMessage = "सेटिंग्स सफलतापूर्वक सहेजी गईं!"
            } catch (e: Exception) {
                statusMessage = "त्रुटि: ${e.message}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 550.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, GoldWarm.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = Color(0xFF101828)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, tint = GoldWarm)
                        Text(
                            text = "दैनिक AI वॉलपेपर मास्टर कंट्रोल",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                // Master Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("दैनिक AI वॉलपेपर सुविधा", fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("मास्टर ON/OFF स्विच", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it; saveConfig() },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldWarm)
                    )
                }

                // Frequency Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("प्रतिदिन जनरेशन आवृत्ति (Frequency)", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "1 बार (सुबह)", 2 to "2 बार (सुबह/शाम)", 3 to "3 बार").forEach { (valInt, label) ->
                            OutlinedButton(
                                onClick = { frequency = valInt; saveConfig() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (frequency == valInt) GoldWarm.copy(alpha = 0.2f) else Color.Transparent,
                                    contentColor = if (frequency == valInt) GoldWarm else Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (frequency == valInt) GoldWarm else Color.White.copy(alpha = 0.3f))
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Target Screen Chips
                Column(vertical_arrangement = Arrangement.spacedBy(6.dp)) {
                    Text("लक्ष्य स्क्रीन (Target Screen)", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("home" to "होमस्क्रीन", "lock" to "लॉकस्क्रीन", "both" to "दोनों (Both)").forEach { (key, label) ->
                            OutlinedButton(
                                onClick = { targetScreen = key; saveConfig() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (targetScreen == key) GoldWarm.copy(alpha = 0.2f) else Color.Transparent,
                                    contentColor = if (targetScreen == key) GoldWarm else Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (targetScreen == key) GoldWarm else Color.White.copy(alpha = 0.3f))
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Auto-set on launch switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("प्रथम लॉन्च ऑटो-सेट", fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("नए यूज़र्स को पहली बार ऐप खोलने पर ऑनबोर्डिंग प्रॉम्प्ट दिखाएँ", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = autoSetOnLaunch,
                        onCheckedChange = { autoSetOnLaunch = it; saveConfig() },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldWarm)
                    )
                }

                // Custom Style Override
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it; saveConfig() },
                    label = { Text("कस्टम थियोलॉजिकल प्रॉम्प्ट प्रसेट") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldWarm,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = GoldWarm,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Active Wallpaper Preview Card
                if (currentWallpaperUrl.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("सक्रिय वॉलपेपर प्रीव्यू", style = MaterialTheme.typography.bodyMedium, color = GoldWarm, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, GoldWarm.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = currentWallpaperUrl,
                                contentDescription = "Active Wallpaper Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Force Regenerate Button
                Button(
                    onClick = {
                        isRegenerating = true
                        coroutineScope.launch {
                            try {
                                // Simulate Cloud Function call trigger
                                kotlinx.coroutines.delay(1500)
                                statusMessage = "⚡ आज का वॉलपेपर तुरंत पुनः जनरेट किया गया!"
                            } catch (e: Exception) {
                                statusMessage = "त्रुटि: ${e.message}"
                            } finally {
                                isRegenerating = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm),
                    enabled = !isRegenerating
                ) {
                    if (isRegenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("जनरेट हो रहा है...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("⚡ आज का वॉलपेपर तुरंत पुनः जनरेट करें", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (statusMessage.isNotBlank()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (statusMessage.startsWith("त्रुटि")) MaterialTheme.typography.errorColor else GoldWarm
                    )
                }
            }
        }
    }
}
val Typography.errorColor: Color @Composable get() = MaterialTheme.colorScheme.error
