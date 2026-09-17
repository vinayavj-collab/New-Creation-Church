package com.example.ui.bible

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouVersionQuickFontSheet(
    settings: BibleReadingSettings,
    onFontSizeChange: (BibleFontSize) -> Unit,
    onLineSpacingChange: (BibleLineSpacing) -> Unit,
    onFontStyleChange: (BibleFontFamilyType) -> Unit,
    onVerseNumberSizeChange: (VerseNumberSize) -> Unit = {},
    onToggleChapterOutline: (Boolean) -> Unit = {},
    onToggleHeadingVerseRanges: (Boolean) -> Unit = {},
    onThemeChange: (BibleTheme) -> Unit,
    onScreenTimeoutChange: (Int) -> Unit,
    onCustomTextColorChange: (String?) -> Unit,
    onCustomHeadingColorChange: (String?) -> Unit,
    onCustomSubHeadingColorChange: (String?) -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "पठन अनुकूलन (Display & Customization)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Font Size (YouVersion A- to A+ selector)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("फ़ॉन्ट आकार", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BibleFontSize.entries.forEach { size ->
                        val isSelected = settings.fontSize == size
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { onFontSizeChange(size) }
                                .padding(horizontal = 1.dp)
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

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Themes Customization (Paper, Wood, Eye Care/Sepia, Day, Night, Dark, OLED, Emerald)
            Text(
                text = "पृष्ठभूमि थीम (Background Themes)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeColorPill(
                    name = "Paper",
                    hindiSubtitle = "कागज़",
                    bgColor = Color(0xFFFBF6EE),
                    textColor = Color(0xFF2C241E),
                    isSelected = settings.theme == BibleTheme.PAPER,
                    onClick = { onThemeChange(BibleTheme.PAPER) }
                )
                ThemeColorPill(
                    name = "Wood",
                    hindiSubtitle = "काष्ठ",
                    bgColor = Color(0xFFF3E8D3),
                    textColor = Color(0xFF3B2B20),
                    isSelected = settings.theme == BibleTheme.WOOD,
                    onClick = { onThemeChange(BibleTheme.WOOD) }
                )
                ThemeColorPill(
                    name = "Eye Care",
                    hindiSubtitle = "सुरक्षा",
                    bgColor = Color(0xFFFEF3E2),
                    textColor = Color(0xFF2E2519),
                    isSelected = settings.theme == BibleTheme.EYE_PROTECTION || settings.theme == BibleTheme.SEPIA,
                    onClick = { onThemeChange(BibleTheme.EYE_PROTECTION) }
                )
                ThemeColorPill(
                    name = "Light",
                    hindiSubtitle = "दिन",
                    bgColor = Color(0xFFFFFFFF),
                    textColor = Color(0xFF1E293B),
                    isSelected = settings.theme == BibleTheme.LIGHT,
                    onClick = { onThemeChange(BibleTheme.LIGHT) }
                )
                ThemeColorPill(
                    name = "Night",
                    hindiSubtitle = "रात्रि",
                    bgColor = Color(0xFF1F2937),
                    textColor = Color(0xFFF3F4F6),
                    isSelected = settings.theme == BibleTheme.NIGHT,
                    onClick = { onThemeChange(BibleTheme.NIGHT) }
                )
                ThemeColorPill(
                    name = "Dark",
                    hindiSubtitle = "डार्क",
                    bgColor = Color(0xFF0F172A),
                    textColor = Color(0xFFE2E8F0),
                    isSelected = settings.theme == BibleTheme.DARK,
                    onClick = { onThemeChange(BibleTheme.DARK) }
                )
                ThemeColorPill(
                    name = "AMOLED",
                    hindiSubtitle = "काला",
                    bgColor = Color(0xFF000000),
                    textColor = Color(0xFFFFFFFF),
                    isSelected = settings.theme == BibleTheme.AMOLED,
                    onClick = { onThemeChange(BibleTheme.AMOLED) }
                )
                ThemeColorPill(
                    name = "Emerald",
                    hindiSubtitle = "हरा",
                    bgColor = Color(0xFFEBF2EC),
                    textColor = Color(0xFF143522),
                    isSelected = settings.theme == BibleTheme.EMERALD,
                    onClick = { onThemeChange(BibleTheme.EMERALD) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Stylish Fonts Selector
            Text(
                text = "फ़ॉन्ट चयन (Stylish Fonts)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BibleFontFamilyType.entries.forEach { fontType ->
                    val isSelected = settings.fontStyle == fontType
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFontStyleChange(fontType) },
                        label = {
                            Text(
                                text = fontType.titleHindi,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Screen Off Customization (Screen Timeout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ScreenLockPortrait,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "स्क्रीन बंद समय (Screen Off):",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val timeoutOptions = listOf(
                    -1 to "☀️ हमेशा चालू (Always On)",
                    2 to "⏱️ 2 मिनट",
                    5 to "⏱️ 5 मिनट",
                    10 to "⏱️ 10 मिनट",
                    15 to "⏱️ 15 मिनट",
                    0 to "📱 सिस्टम डिफ़ॉल्ट"
                )
                timeoutOptions.forEach { (minutes, label) ->
                    val isSelected = settings.screenTimeoutMinutes == minutes
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onScreenTimeoutChange(minutes) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Text / Heading / Subheading Colors Customization
            Text(
                text = "टेक्स्ट एवं शीर्षक रंग अनुकूलन (Text & Heading Colors)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Text Color Row
            ColorPickerSelectorRow(
                title = "वचन पाठ रंग (Bible Text Color)",
                currentColorHex = settings.customTextColorHex,
                defaultName = "थीम अनुसार",
                presetColors = listOf(
                    "#1E293B" to "Dark Slate",
                    "#2C241E" to "Parchment Brown",
                    "#000000" to "Pure Black",
                    "#1E3A8A" to "Deep Navy",
                    "#14532D" to "Dark Pine"
                ),
                onColorSelected = onCustomTextColorChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Heading Color Row
            ColorPickerSelectorRow(
                title = "अध्याय शीर्षक रंग (Heading Color)",
                currentColorHex = settings.customHeadingColorHex,
                defaultName = "थीम अनुसार",
                presetColors = listOf(
                    "#0284C7" to "Ocean Blue",
                    "#991B1B" to "Deep Crimson",
                    "#065F46" to "Emerald",
                    "#7C2D12" to "Warm Amber",
                    "#581C87" to "Royal Purple"
                ),
                onColorSelected = onCustomHeadingColorChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subheading Color Row
            ColorPickerSelectorRow(
                title = "उपशीर्षक रंग (Subheading Color)",
                currentColorHex = settings.customSubHeadingColorHex,
                defaultName = "थीम अनुसार",
                presetColors = listOf(
                    "#0369A1" to "Sky Teal",
                    "#B45309" to "Bronze",
                    "#4338CA" to "Indigo",
                    "#0F766E" to "Deep Teal",
                    "#9F1239" to "Rose Wine"
                ),
                onColorSelected = onCustomSubHeadingColorChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Quick Toggles: Verse Number Size, Jesus words in Red (Default OFF), Justify
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "वचन संख्या सामान्य आकार (Normal Verse Numbers)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = if (settings.verseNumberSize == VerseNumberSize.NORMAL) "वचन संख्या सामान्य आकार में है" else "वचन संख्या छोटा/ऊपर (Superscript) है",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Switch(
                    checked = settings.verseNumberSize == VerseNumberSize.NORMAL,
                    onCheckedChange = { isNormal ->
                        onVerseNumberSizeChange(if (isNormal) VerseNumberSize.NORMAL else VerseNumberSize.SUPERSCRIPT)
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "प्रभु यीशु के वचन लाल रंग में",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "प्रभु यीशु मसीह के कथनों को लाल रंग में दिखाना (डिफ़ॉल्ट बंद)",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
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
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "अध्याय रूपरेखा कार्ड (Chapter Outline)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "वचन समूहों की संक्षिप्त सूची व त्वरित नेविगेशन",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Switch(
                    checked = settings.showChapterOutline,
                    onCheckedChange = onToggleChapterOutline
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "शीर्षक में वचन सीमा (Verse Ranges)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "शीर्षक के साथ [वचन 1–12] सीमा प्रदर्शित करें",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Switch(
                    checked = settings.showHeadingVerseRanges,
                    onCheckedChange = onToggleHeadingVerseRanges
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Open Full Settings button
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
private fun ColorPickerSelectorRow(
    title: String,
    currentColorHex: String?,
    defaultName: String,
    presetColors: List<Pair<String, String>>,
    onColorSelected: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset / Default Chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (currentColorHex == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clickable { onColorSelected(null) }
                    .border(
                        width = if (currentColorHex == null) 2.dp else 0.dp,
                        color = if (currentColorHex == null) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Text(
                    text = defaultName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (currentColorHex == null) FontWeight.Bold else FontWeight.Normal,
                        color = if (currentColorHex == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // Preset Colors
            presetColors.forEach { (hex, name) ->
                val isSelected = currentColorHex.equals(hex, ignoreCase = true)
                val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.DarkGray }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = name,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeColorPill(
    name: String,
    hindiSubtitle: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier
            .width(72.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 11.sp
                )
            )
            Text(
                text = hindiSubtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = textColor.copy(alpha = 0.75f),
                    fontSize = 9.sp
                )
            )
        }
    }
}
