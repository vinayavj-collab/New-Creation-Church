package com.example.ui.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.MainViewModel

data class ChristianThemePreset(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String,
    val primaryHex: String,
    val secondaryHex: String,
    val description: String,
    val icon: String
)

val christianThemePresets = listOf(
    ChristianThemePreset(
        id = "royal_purple",
        nameHindi = "🟣 शाही बैंगनी व स्वर्णिम (Liturgical Purple & Gold)",
        nameEnglish = "Royal Gold & Liturgical Purple",
        primaryHex = "#6A1B9A",
        secondaryHex = "#D4AF37",
        description = "मसीह की शाही महिमा, पश्चाताप, प्रार्थना और स्वर्गीय मुकुट का प्रतीक",
        icon = "👑"
    ),
    ChristianThemePreset(
        id = "celestial_navy",
        nameHindi = "🔵 स्वर्गीय नीलिमा व अनुग्रह (Celestial Navy & Gold)",
        nameEnglish = "Celestial Navy & Grace Gold",
        primaryHex = "#0F2B52",
        secondaryHex = "#C59E3F",
        description = "स्वर्गीय शांति, ईश्वर की अगाध बुद्धि और असीम अनुग्रह का अहसास",
        icon = "🌌"
    ),
    ChristianThemePreset(
        id = "sacred_crimson",
        nameHindi = "🔴 पवित्र बलिदान व अनुग्रह (Sacred Crimson & Gold)",
        nameEnglish = "Sacred Crimson & Ivory",
        primaryHex = "#8B1E22",
        secondaryHex = "#DAA520",
        description = "क्रूस के पवित्र बलिदान, यीशु के असीम प्रेम और पिन्तेकुस्त की सामर्थ्य का रंग",
        icon = "✝️"
    ),
    ChristianThemePreset(
        id = "peaceful_olive",
        nameHindi = "🟢 जैतून व आत्मिक वृद्धि (Peaceful Olive Green & Gold)",
        nameEnglish = "Peaceful Olive & Renewal",
        primaryHex = "#1E4D3A",
        secondaryHex = "#D4AF37",
        description = "जैतून का पहाड़, पवित्र आत्मा का कपोत, शांति और आत्मिक तरक्की",
        icon = "🕊️"
    ),
    ChristianThemePreset(
        id = "vintage_parchment",
        nameHindi = "📜 प्राचीन चर्मपत्र बाइबल (Vintage Parchment & Leather)",
        nameEnglish = "Vintage Bible Parchment",
        primaryHex = "#4A2E12",
        secondaryHex = "#C29241",
        description = "प्राचीन चमड़े की बाइबल, चर्मपत्र पांडुलिपि और गंभीर बाइबल अध्ययन",
        icon = "📖"
    ),
    ChristianThemePreset(
        id = "emerald_light",
        nameHindi = "💚 स्वर्गीय पन्ना व दिव्य प्रकाश (Emerald & Celestial Gold)",
        nameEnglish = "Heavenly Emerald & Light",
        primaryHex = "#004D40",
        secondaryHex = "#FFD700",
        description = "दिव्य प्रकाश, नया जीवन और स्वर्गीय सिंहारसन का पन्ना रंग",
        icon = "✨"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminThemeManagerDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentPrimary by viewModel.themePrimaryColor.collectAsState()
    val currentSecondary by viewModel.themeSecondaryColor.collectAsState()

    var selectedPrimaryHex by remember(currentPrimary) { mutableStateOf(currentPrimary.ifBlank { "#0F2B52" }) }
    var selectedSecondaryHex by remember(currentSecondary) { mutableStateOf(currentSecondary.ifBlank { "#C59E3F" }) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyPrimary)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "👑 मास्टर एडमिन: क्रिश्चियन थीम प्रबन्धक",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "सभी विश्वासी प्रयोगकर्ताओं के लिए ग्लोबल ऐप थीम कस्टमाइज़ करें",
                            fontSize = 11.sp,
                            color = GoldWarm
                        )
                    }
                }

                // Active Theme Live Preview Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🎨 चयनित थीम का लाइव पूर्वावलोकन (Live Preview):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Sample Preview Card
                        val primaryColor = parseHexColor(selectedPrimaryHex) ?: Color(0xFF0F2B52)
                        val secondaryColor = parseHexColor(selectedSecondaryHex) ?: Color(0xFFC59E3F)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = primaryColor)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "✝️ New Creation Church App",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = secondaryColor
                                    ) {
                                        Text(
                                            text = "प्राइमरी + एक्सीन्ट",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\"तेरा वचन मेरे पांवों के लिये दीपक, और मेरे मार्ग के लिये उजियाला है।\" (भजन संहिता 119:105)",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Presets List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "✝️ विशेष क्रिश्चियन थीम प्रीसेट्स (Christian Presets):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(christianThemePresets) { preset ->
                        val isSelected = selectedPrimaryHex.equals(preset.primaryHex, ignoreCase = true)
                        val presetPrimary = parseHexColor(preset.primaryHex) ?: Color.Gray
                        val presetSecondary = parseHexColor(preset.secondaryHex) ?: Color.Yellow

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPrimaryHex = preset.primaryHex
                                    selectedSecondaryHex = preset.secondaryHex
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, GoldWarm) else null,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Color swatches
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(presetPrimary)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(presetSecondary)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = preset.nameHindi,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = preset.description,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedPrimaryHex = preset.primaryHex
                                        selectedSecondaryHex = preset.secondaryHex
                                    }
                                )
                            }
                        }
                    }

                    // Custom Color Input Box
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚙️ कस्टम हेक्स कलर दर्ज करें (Custom Hex Codes):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = selectedPrimaryHex,
                                        onValueChange = { selectedPrimaryHex = it },
                                        label = { Text("Primary Hex") },
                                        placeholder = { Text("#0F2B52") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = selectedSecondaryHex,
                                        onValueChange = { selectedSecondaryHex = it },
                                        label = { Text("Accent Hex") },
                                        placeholder = { Text("#C59E3F") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Save Action Bar
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("रद्द करें")
                        }

                        Button(
                            onClick = {
                                isSaving = true
                                viewModel.updateGlobalThemeColors(selectedPrimaryHex, selectedSecondaryHex) { success ->
                                    isSaving = false
                                    if (success) {
                                        Toast.makeText(context, "✅ सर्व-कलीसिया थीम सफलता से लागू की गई!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "⚠️ थीम अपडेट करने में विफल", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "👑 सर्व-कलीसिया लागू करें",
                                color = GoldWarm,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
