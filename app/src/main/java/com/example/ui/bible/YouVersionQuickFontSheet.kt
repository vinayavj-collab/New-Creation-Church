package com.example.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.BibleFontSize
import com.example.data.bible.model.BibleLineSpacing
import com.example.data.bible.model.BibleReadingSettings
import com.example.data.bible.model.BibleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouVersionQuickFontSheet(
    settings: BibleReadingSettings,
    onFontSizeChange: (BibleFontSize) -> Unit,
    onLineSpacingChange: (BibleLineSpacing) -> Unit,
    onThemeChange: (BibleTheme) -> Unit,
    onToggleSerif: (Boolean) -> Unit,
    onToggleOriginalFormat: (Boolean) -> Unit = {},
    onToggleJesusWordsInRed: (Boolean) -> Unit,
    onToggleJustify: (Boolean) -> Unit,
    onOpenFullSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "पठन विकल्प (Reading Display)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Font Size (YouVersion A- to A+ selector)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("फ़ॉन्ट आकार", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BibleFontSize.entries.forEach { size ->
                        val isSelected = settings.fontSize == size
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { onFontSizeChange(size) }
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = when (size) {
                                    BibleFontSize.SMALL -> "A"
                                    BibleFontSize.NORMAL -> "A+"
                                    BibleFontSize.LARGE -> "A++"
                                    BibleFontSize.EXTRA_LARGE -> "A+++"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. YouVersion Themes (Light, Sepia, Dark, AMOLED)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeColorPill(
                    name = "Light",
                    bgColor = Color(0xFFFCFCFC),
                    textColor = Color(0xFF1E293B),
                    isSelected = settings.theme == BibleTheme.LIGHT,
                    onClick = { onThemeChange(BibleTheme.LIGHT) }
                )
                ThemeColorPill(
                    name = "Sepia",
                    bgColor = Color(0xFFFBF0D9),
                    textColor = Color(0xFF382D20),
                    isSelected = settings.theme == BibleTheme.SEPIA,
                    onClick = { onThemeChange(BibleTheme.SEPIA) }
                )
                ThemeColorPill(
                    name = "Dark",
                    bgColor = Color(0xFF1E293B),
                    textColor = Color(0xFFF1F5F9),
                    isSelected = settings.theme == BibleTheme.DARK,
                    onClick = { onThemeChange(BibleTheme.DARK) }
                )
                ThemeColorPill(
                    name = "Black",
                    bgColor = Color(0xFF000000),
                    textColor = Color(0xFFE2E8F0),
                    isSelected = settings.theme == BibleTheme.AMOLED,
                    onClick = { onThemeChange(BibleTheme.AMOLED) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Font Style: Sans-serif (Modern) vs Serif (Classic YouVersion)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("फ़ॉन्ट शैली", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !settings.useSerifFont,
                        onClick = { onToggleSerif(false) },
                        label = { Text("Modern Sans", fontFamily = FontFamily.SansSerif) }
                    )
                    FilterChip(
                        selected = settings.useSerifFont,
                        onClick = { onToggleSerif(true) },
                        label = { Text("Classic Serif", fontFamily = FontFamily.Serif) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Original Format Mode (पारंपरिक मूल मुद्रित बाइबिल प्रारूप)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.originalFormatMode) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "📖 ओरिजिनल फॉर्मेट (Original Print Format)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "पारंपरिक प्रिंटेड बाइबिल लेआउट: सेरिफ़ फॉन्ट, समरेखण एवं पैराग्राफ प्रवाह",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Switch(
                        checked = settings.originalFormatMode,
                        onCheckedChange = onToggleOriginalFormat
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Quick Toggles (Jesus Words in Red, Justify)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("यीशु के वचन लाल रंग में (Red Letter)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.showJesusWordsInRed,
                    onCheckedChange = onToggleJesusWordsInRed
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("समान संरेखण (Justify Text)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.justifyBibleText,
                    onCheckedChange = onToggleJustify
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Open Full Settings button
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenFullSettings()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("सभी विस्तृत सेटिंग्स (Full Bible Settings)")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeColorPill(
    name: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                shape = CircleShape
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
        }
    }
}
