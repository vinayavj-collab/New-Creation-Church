package com.example.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.bible.model.*

@Composable
fun BibleSettingsDialog(
    settings: BibleReadingSettings,
    selectedTranslation: BibleTranslation,
    onFontSizeChange: (BibleFontSize) -> Unit,
    onLineSpacingChange: (BibleLineSpacing) -> Unit,
    onShowVerseNumbersChange: (Boolean) -> Unit,
    onThemeChange: (BibleTheme) -> Unit,
    onTranslationChange: (BibleTranslation) -> Unit,
    onDismiss: () -> Unit
) {
    var showLicenseInfo by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
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
                    Text(
                        text = "बाइबल सेटिंग्स (Settings)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { showLicenseInfo = !showLicenseInfo }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Licensing Information",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (showLicenseInfo) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Legal & Translation Attribution",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Hindi IRV: Licensed under Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0). Courtesy: Bridge Connectivity Solutions & Free Bibles India.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• English WEB: World English Bible is 100% in the Public Domain, freely distributed.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Translation selector
                Text(
                    text = "अनुवाद (Translation)",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BibleTranslation.ALL.forEach { translation ->
                        val isSelected = selectedTranslation.id == translation.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTranslationChange(translation) },
                            label = {
                                Text(
                                    when (translation.id) {
                                        BibleTranslation.HINDI_IRV.id -> "हिन्दी (IRV)"
                                        BibleTranslation.ENGLISH_KJV.id -> "English (KJV)"
                                        else -> "एक साथ (HI + EN)"
                                    },
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Font Size
                Text(
                    text = "फॉन्ट का आकार (Text Size)",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BibleFontSize.entries.forEach { size ->
                        val isSelected = settings.fontSize == size
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFontSizeChange(size) },
                            label = {
                                Text(
                                    when (size) {
                                        BibleFontSize.SMALL -> "S"
                                        BibleFontSize.NORMAL -> "M"
                                        BibleFontSize.LARGE -> "L"
                                        BibleFontSize.EXTRA_LARGE -> "XL"
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Line spacing
                Text(
                    text = "पंक्ति अंतर (Line Spacing)",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BibleLineSpacing.entries.forEach { spacing ->
                        val isSelected = settings.lineSpacing == spacing
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLineSpacingChange(spacing) },
                            label = {
                                Text(
                                    when (spacing) {
                                        BibleLineSpacing.COMPACT -> "सघन"
                                        BibleLineSpacing.NORMAL -> "सामान्य"
                                        BibleLineSpacing.COMFORTABLE -> "खुला"
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Canvas Theme
                Text(
                    text = "रीडिंग बैकग्राउंड (Reading Canvas)",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeOptionBox(
                        title = "उजला",
                        bgColor = Color(0xFFFAFAFA),
                        textColor = Color.Black,
                        isSelected = settings.theme == BibleTheme.LIGHT,
                        onClick = { onThemeChange(BibleTheme.LIGHT) }
                    )
                    ThemeOptionBox(
                        title = "गहरा",
                        bgColor = Color(0xFF1E293B),
                        textColor = Color.White,
                        isSelected = settings.theme == BibleTheme.DARK,
                        onClick = { onThemeChange(BibleTheme.DARK) }
                    )
                    ThemeOptionBox(
                        title = "सहज",
                        bgColor = Color(0xFFFBF0D9),
                        textColor = Color(0xFF4A3B2C),
                        isSelected = settings.theme == BibleTheme.SEPIA,
                        onClick = { onThemeChange(BibleTheme.SEPIA) }
                    )
                    ThemeOptionBox(
                        title = "सिस्टम",
                        bgColor = MaterialTheme.colorScheme.surfaceVariant,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        isSelected = settings.theme == BibleTheme.SYSTEM,
                        onClick = { onThemeChange(BibleTheme.SYSTEM) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verse Numbers toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "वचन संख्या दिखाएँ (Show Verse Numbers)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = settings.showVerseNumbers,
                        onCheckedChange = onShowVerseNumbersChange
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("सम्पन्न (Done)")
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionBox(
    title: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )
            }
        }
    }
}
