package com.example.ui.bible

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.bible.model.*

@Composable
fun BibleSettingsDialog(
    settings: BibleReadingSettings,
    selectedTranslation: BibleTranslation,
    onFontSizeChange: (BibleFontSize) -> Unit,
    onLineSpacingChange: (BibleLineSpacing) -> Unit,
    onShowVerseNumbersChange: (Boolean) -> Unit,
    onShowSubheadingsChange: (Boolean) -> Unit = {},
    onShowParagraphAndIndentsChange: (Boolean) -> Unit = {},
    onOriginalFormatModeChange: (Boolean) -> Unit = {},
    onShowJesusWordsInRedChange: (Boolean) -> Unit = {},
    onJesusWordsColorChange: (String) -> Unit = {},
    onShowFavoritesHintChange: (Boolean) -> Unit = {},
    onShowBookmarkHintChange: (Boolean) -> Unit = {},
    onShowHighlightsChange: (Boolean) -> Unit = {},
    onShowNoteHintChange: (Boolean) -> Unit = {},
    onJustifyBibleTextChange: (Boolean) -> Unit = {},
    onSuggestVerseSelectionChange: (Boolean) -> Unit = {},
    onShowAudioPlayerChange: (Boolean) -> Unit = {},
    onScreenTimeoutChange: (Int) -> Unit = {},
    onRememberPositionChange: (Boolean) -> Unit = {},
    onResetToDefault: () -> Unit = {},
    onThemeChange: (BibleTheme) -> Unit,
    onTranslationChange: (BibleTranslation) -> Unit,
    onDismiss: () -> Unit
) {
    var showLicenseInfo by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "बाइबल सेटिंग्स (Settings)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "संस्करण 1.4 (Version 1.4)",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
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
                                text = "• English KJV: King James Version is in the Public Domain.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // SECTION 1: TRANSLATION & DISPLAY
                    SettingsSectionHeader("अनुवाद एवं रूपरेखा (Translation & Typography)")

                    // Translation selector
                    Text(
                        text = "अनुवाद चुनें (Select Translation)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                            BibleTranslation.HINDI_BSI_OV.id -> "हिन्दी (BSI Old)"
                                            BibleTranslation.HINDI_ERV.id -> "हिन्दी (ERV सरल)"
                                            BibleTranslation.HINDI_ULB.id -> "हिन्दी (ULB मूलनिष्ठ)"
                                            BibleTranslation.ENGLISH_KJV.id -> "English (KJV)"
                                            BibleTranslation.ENGLISH_WEB.id -> "English (WEB)"
                                            else -> "एक साथ (HI+EN)"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Font Size
                    Text(
                        text = "फॉन्ट का आकार (Text Size)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
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
                                            BibleFontSize.SMALL -> "S (छोटा)"
                                            BibleFontSize.NORMAL -> "M (सामान्य)"
                                            BibleFontSize.LARGE -> "L (बड़ा)"
                                            BibleFontSize.EXTRA_LARGE -> "XL (विशाल)"
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Line Spacing
                    Text(
                        text = "पंक्ति अंतर (Line Spacing)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
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
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Canvas Theme
                    Text(
                        text = "रीडिंग बैकग्राउंड (Reading Theme)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionBox(
                            title = "उजला",
                            bgColor = Color(0xFFFAFAFA),
                            textColor = Color.Black,
                            isSelected = settings.theme == BibleTheme.LIGHT,
                            onClick = { onThemeChange(BibleTheme.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionBox(
                            title = "गहरा",
                            bgColor = Color(0xFF1E293B),
                            textColor = Color.White,
                            isSelected = settings.theme == BibleTheme.DARK,
                            onClick = { onThemeChange(BibleTheme.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionBox(
                            title = "सहज",
                            bgColor = Color(0xFFFBF0D9),
                            textColor = Color(0xFF4A3B2C),
                            isSelected = settings.theme == BibleTheme.SEPIA,
                            onClick = { onThemeChange(BibleTheme.SEPIA) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionBox(
                            title = "सिस्टम",
                            bgColor = MaterialTheme.colorScheme.surfaceVariant,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            isSelected = settings.theme == BibleTheme.SYSTEM,
                            onClick = { onThemeChange(BibleTheme.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 2: SHOW IN BIBLE WINDOW OPTIONS (Version 1.4 Toggles)
                    SettingsSectionHeader("बाइबल विंडो विकल्प (Bible Window Options)")

                    // Original Format Mode Option
                    SettingsToggleRow(
                        title = "मूल मुद्रित प्रारूप (Original Print Format)",
                        subtitle = "पारंपरिक प्रिंटेड बाइबिल की तरह सेरिफ़ फॉन्ट, समरेखण एवं पैराग्राफ प्रवाह",
                        checked = settings.originalFormatMode,
                        onCheckedChange = onOriginalFormatModeChange
                    )

                    // Subheadings Option
                    SettingsToggleRow(
                        title = "उपशीर्षक दिखाएँ (Subheadings)",
                        subtitle = "अध्याय में खंड शीर्षक और संदर्भ दिखाएँ",
                        checked = settings.showSubheadings,
                        onCheckedChange = onShowSubheadingsChange
                    )

                    // Verse Numbers Option
                    SettingsToggleRow(
                        title = "वचन संख्या (Verse Numbers)",
                        subtitle = "प्रत्येक वचन के आगे संख्या प्रदर्शित करें",
                        checked = settings.showVerseNumbers,
                        onCheckedChange = onShowVerseNumbersChange
                    )

                    // Paragraphs and Indents Option
                    SettingsToggleRow(
                        title = "अनुच्छेद एवं इंडेंट (Paragraphs & Indents)",
                        subtitle = "काव्य व वार्तालाप के अनुसार संरचित रूप में पाठ दिखाएँ",
                        checked = settings.showParagraphAndIndents,
                        onCheckedChange = onShowParagraphAndIndentsChange
                    )

                    // Justify Bible Text Option
                    SettingsToggleRow(
                        title = "टेक्स्ट समरेखण (Justify Bible Text)",
                        subtitle = "दोनों किनारों से समान संरेखण (Justified alignment)",
                        checked = settings.justifyBibleText,
                        onCheckedChange = onJustifyBibleTextChange
                    )

                    // Jesus Words in Red Option
                    SettingsToggleRow(
                        title = "प्रभु यीशु के वचन रंगीन (Jesus Words)",
                        subtitle = "प्रभु यीशु मसीह के कथनों को विशिष्ट रंग में प्रदर्शित करें",
                        checked = settings.showJesusWordsInRed,
                        onCheckedChange = onShowJesusWordsInRedChange
                    )

                    // Color picker for Jesus Words
                    AnimatedVisibility(visible = settings.showJesusWordsInRed) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, bottom = 12.dp)
                        ) {
                            Text(
                                text = "यीशु के वचनों का रंग चुनें (Choose Color):",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val colors = listOf(
                                "#D32F2F" to "लाल (Red)",
                                "#C2185B" to "गुलाबी गहरा (Crimson)",
                                "#880E4F" to "मैरून (Maroon)",
                                "#E65100" to "नारंगी (Amber)",
                                "#F57F17" to "स्वर्ण (Gold)"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                colors.forEach { (hex, name) ->
                                    val isColorSelected = settings.jesusWordsColorHex.equals(hex, ignoreCase = true)
                                    val parsedColor = try {
                                        Color(android.graphics.Color.parseColor(hex))
                                    } catch (e: Exception) {
                                        Color.Red
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                            .border(
                                                width = if (isColorSelected) 3.dp else 1.dp,
                                                color = if (isColorSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable { onJesusWordsColorChange(hex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isColorSelected) {
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

                    // Favorite Verse Hint Option
                    SettingsToggleRow(
                        title = "पसंदीदा वचन संकेत (Favorite Verse Hint)",
                        subtitle = "पसंदीदा वचनों के पास स्टार चिह्न दिखाएँ",
                        checked = settings.showFavoritesHint,
                        onCheckedChange = onShowFavoritesHintChange
                    )

                    // Bookmark Hint Option
                    SettingsToggleRow(
                        title = "बुकमार्क संकेत (Bookmark Hint)",
                        subtitle = "बुकमार्क किए गए वचनों पर रिबन बैज दिखाएँ",
                        checked = settings.showBookmarkHint,
                        onCheckedChange = onShowBookmarkHintChange
                    )

                    // Highlights Option
                    SettingsToggleRow(
                        title = "हाइलाइट्स (Highlights)",
                        subtitle = "रंगे हुए वचनों का हाइलाइट बैकग्राउंड दिखाएँ",
                        checked = settings.showHighlights,
                        onCheckedChange = onShowHighlightsChange
                    )

                    // Notes Hint Option
                    SettingsToggleRow(
                        title = "नोट्स संकेत (Notes Hint)",
                        subtitle = "वचन के पास व्यक्तिगत नोट का चिह्न दिखाएँ",
                        checked = settings.showNoteHint,
                        onCheckedChange = onShowNoteHintChange
                    )

                    // Suggest Verse Selection Option
                    SettingsToggleRow(
                        title = "अध्याय के बाद वचन सुझाव (Suggest Verse)",
                        subtitle = "अध्याय चयन के तुरंत बाद वचन चुनने का सुझाव दें",
                        checked = settings.suggestVerseSelection,
                        onCheckedChange = onSuggestVerseSelectionChange
                    )

                    // Audio Player Bar Option
                    SettingsToggleRow(
                        title = "ऑडियो प्लेयर पट्टी (Audio Player Bar)",
                        subtitle = "स्क्रीन के नीचे बाइबल ऑडियो प्लेयर पट्टी दिखाएँ",
                        checked = settings.showAudioPlayer,
                        onCheckedChange = onShowAudioPlayerChange
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 3: SCREEN & TIMEOUT
                    SettingsSectionHeader("स्क्रीन एवं टाइमआउट (Screen & Timeout)")

                    Text(
                        text = "स्क्रीन स्वतः बंद समय (Automatic Screen Off / Timeout):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val timeoutOptions = listOf(
                        0 to "सिस्टम डिफ़ॉल्ट",
                        2 to "2 मिनट",
                        5 to "5 मिनट",
                        10 to "10 मिनट",
                        15 to "15 मिनट",
                        -1 to "हमेशा चालू (Always On)"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        timeoutOptions.chunked(3).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                chunk.forEach { (mins, label) ->
                                    val isSelected = settings.screenTimeoutMinutes == mins
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onScreenTimeoutChange(mins) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsToggleRow(
                        title = "पिछली पढ़ने की स्थिति याद रखें",
                        subtitle = "ऐप दोबारा खोलने पर उसी पुस्तक व अध्याय पर आएँ",
                        checked = settings.rememberLastReadingPosition,
                        onCheckedChange = onRememberPositionChange
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 4: RESET TO DEFAULTS
                    SettingsSectionHeader("रीसेट (Reset)")

                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("डिफ़ॉल्ट सेटिंग्स रीसेट करें (Reset to Default)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Done Button
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

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("सेटिंग्स रीसेट करें?") },
            text = { Text("क्या आप सभी बाइबल सेटिंग्स को डिफ़ॉल्ट मानों पर रीसेट करना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetToDefault()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("हाँ, रीसेट करें", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ThemeOptionBox(
    title: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )
            }
        }
    }
}
