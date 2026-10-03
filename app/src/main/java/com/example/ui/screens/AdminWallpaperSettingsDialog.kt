package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.MainViewModel

/**
 * AdminWallpaperSettingsDialog: Tablet-optimized Admin Master Control Panel for the
 * Dual-Screen Daily Scripture AI Wallpaper System (MaxWidth 550.dp constraint).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWallpaperSettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val adminConfig by viewModel.adminWallpaperConfig.collectAsState()

    var isEnabled by remember(adminConfig) { mutableStateOf(adminConfig.isEnabled) }
    var frequencyPerDay by remember(adminConfig) { mutableIntStateOf(adminConfig.frequencyPerDay) } // 1, 2, 3
    var targetScreen by remember(adminConfig) { mutableStateOf(adminConfig.targetScreen) } // "home" | "lock" | "both"
    var showOnboardingPrompt by remember(adminConfig) { mutableStateOf(adminConfig.showOnboardingPrompt) }
    var customPromptPreset by remember(adminConfig) { mutableStateOf(adminConfig.customPromptPreset) }
    var selectedStyleCode by remember(adminConfig) { mutableStateOf(adminConfig.selectedThematicStyle) }
    var isRegenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val promptTemplates = listOf(
        Pair("🌿 हरी चराइयां व जल", "Cinematic serene biblical painting of lush emerald green pastures and quiet still waters, golden sunrise rays breaking through morning mist, sacred peaceful mood, 8k wallpaper"),
        Pair("✨ स्वर्गीय दिव्य प्रकाश", "Heavenly golden glory clouds with majestic crepuscular sunbeams from heaven, celestial purple and gold atmosphere, divine light of hope, 8k spiritual art"),
        Pair("✝ पवित्र क्रूस व गोधूलि", "Sacred wooden cross on mountain hill against warm crimson-gold sunset twilight, radiant divine aura with halo, atmospheric reverent mood"),
        Pair("🕊️ जीवन का जल व शांति", "Crystal clear living waters stream reflecting golden heavenly light, white dove of Holy Spirit silhouette, blooming serene lilies"),
        Pair("⛰️ दृढ़ शरणस्थान व चट्टान", "Majestic mountain peak and stone fortress illuminated by golden dawn sunlight, the Lord is my rock and fortress, divine protection")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 550.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AI वॉलपेपर मास्टर कंट्रोल",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Master ON/OFF Switch
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "दैनिक AI वॉलपेपर सुविधा",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "मास्टर स्विच चालू / बंद करें",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Frequency Selector with Today's Verse First Priority Rule
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "दैनिक आवृत्ति (Frequency Per Day)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "प्राथमिकता: आज का वचन (Slot 1)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "नोट: आवृत्ति चाहे कितनी भी सेट हो (1x, 2x या 3x), दिन का सबसे पहला और मुख्य वॉलपेपर हमेशा 'आज का वचन' (Today's Verse) ही रहेगा।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1 to "1 बार (सुबह: आज का वचन)", 2 to "2 बार (सुबह + शाम)", 3 to "3 बार (सुबह + 2x)").forEach { (valInt, label) ->
                        FilterChip(
                            selected = frequencyPerDay == valInt,
                            onClick = { frequencyPerDay = valInt },
                            label = { Text(label, fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target Screen Chips
                Text(
                    text = "लक्ष्य स्क्रीन (Target Screen)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("home" to "होमस्क्रीन", "lock" to "लॉकस्क्रीन", "both" to "दोनों").forEach { (code, label) ->
                        FilterChip(
                            selected = targetScreen == code,
                            onClick = { targetScreen = code },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Thematic Style Selector
                Text(
                    text = "🎨 AI स्पिरिचुअल आर्ट थीम (Thematic Style)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "AUTO" to "✨ ऑटो (वचन अनुसार)",
                        "BOTANICAL" to "🌿 हरी चराइयां",
                        "PASTEL" to "✨ स्वर्गीय किरणें"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = selectedStyleCode == code,
                            onClick = { selectedStyleCode = code },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "CINEMATIC" to "✝ क्रूस व गोधूलि",
                        "LIVING_WATER" to "🕊️ जीवन का जल",
                        "ROCK" to "⛰️ दृढ़ चट्टान"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = selectedStyleCode == code,
                            onClick = { selectedStyleCode = code },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Prompt Preset Chips
                Text(
                    text = "⚡ त्वरित AI प्रॉम्प्ट प्रीसेट (Quick Templates)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    promptTemplates.take(3).forEach { (label, prompt) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.clickable {
                                customPromptPreset = prompt
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    promptTemplates.drop(3).forEach { (label, prompt) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.clickable {
                                customPromptPreset = prompt
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Prompt Override Text Field
                OutlinedTextField(
                    value = customPromptPreset,
                    onValueChange = { customPromptPreset = it },
                    label = { Text("AI आध्यात्मिक सीन प्रॉम्प्ट (Custom AI Prompt)") },
                    placeholder = { Text("वचन के अनुसार AI सीन के लिए दृश्य प्रॉम्प्ट यहाँ लिखें...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Onboarding Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "नए यूज़र्स ऑनबोर्डिंग प्रॉम्प्ट",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "पहली बार ऐप खोलने पर ऑटो-सेट वॉलपेपर की अनुमति मांगें",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Switch(
                        checked = showOnboardingPrompt,
                        onCheckedChange = { showOnboardingPrompt = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Force Regenerate Action Button - Prioritizes Today's Scripture
                val selectedStyleEnum = when (selectedStyleCode) {
                    "BOTANICAL" -> com.example.util.DailyWallpaperService.TheologicalStyle.SOFT_BOTANICAL_WATERCOLOR
                    "PASTEL" -> com.example.util.DailyWallpaperService.TheologicalStyle.ETHEREAL_DREAMY_PASTEL
                    "LIVING_WATER" -> com.example.util.DailyWallpaperService.TheologicalStyle.VIBRANT_SPIRITUAL_STORYBOOK
                    "ROCK" -> com.example.util.DailyWallpaperService.TheologicalStyle.ROCK_OF_AGES
                    "CINEMATIC" -> com.example.util.DailyWallpaperService.TheologicalStyle.CINEMATIC_BIBLICAL_HISTORICAL
                    else -> null
                }

                Button(
                    onClick = {
                        isRegenerating = true
                        Toast.makeText(context, "⚡ 'आज का वचन' AI वॉलपेपर तैयार किया जा रहा है...", Toast.LENGTH_SHORT).show()
                        viewModel.applyDailyWallpaperNow(
                            context = context,
                            targetScreen = targetScreen,
                            selectedStyle = selectedStyleEnum
                        ) { success ->
                            isRegenerating = false
                            if (success) {
                                Toast.makeText(context, "✅ 'आज का वचन' AI वॉलपेपर स्क्रीन पर लागू हो गया!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "❌ वॉलपेपर सेट करने में त्रुटि हुई, कृपया पुनः प्रयास करें।", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isRegenerating && !isSaving
                ) {
                    if (isRegenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("आज का वचन वॉलपेपर लागू हो रहा है...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("⚡ 'आज का वचन' AI वॉलपेपर फोन पर लागू करें")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save & Close Button (Persists to Firebase Realtime Database)
                OutlinedButton(
                    onClick = {
                        isSaving = true
                        val newConfig = com.example.data.model.AdminWallpaperConfig(
                            isEnabled = isEnabled,
                            frequencyPerDay = frequencyPerDay,
                            targetScreen = targetScreen,
                            showOnboardingPrompt = showOnboardingPrompt,
                            customPromptPreset = customPromptPreset.trim(),
                            selectedThematicStyle = selectedStyleCode,
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        )
                        viewModel.updateAdminWallpaperConfig(newConfig) { success ->
                            isSaving = false
                            Toast.makeText(context, "✅ AI वॉलपेपर प्रॉम्प्ट व सेटिंग्स सहेजी गईं!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("सेटिंग्स सहेजी जा रही हैं...")
                    } else {
                        Text("💾 प्रॉम्प्ट व सेटिंग्स सहेजें (Save & Close)")
                    }
                }
            }
        }
    }
}
