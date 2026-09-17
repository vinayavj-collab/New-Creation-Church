package com.example.ui.bible

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
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
import com.example.data.bible.model.*
import com.example.util.DevotionalBgmManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BibleSettingsDialog(
    settings: BibleReadingSettings,
    selectedTranslation: BibleTranslation,
    onFontSizeChange: (BibleFontSize) -> Unit,
    onLineSpacingChange: (BibleLineSpacing) -> Unit,
    onFontStyleChange: (BibleFontFamilyType) -> Unit = {},
    onShowVerseNumbersChange: (Boolean) -> Unit,
    onVerseNumberSizeChange: (VerseNumberSize) -> Unit = {},
    onShowSubheadingsChange: (Boolean) -> Unit = {},
    onShowChapterOutlineChange: (Boolean) -> Unit = {},
    onShowHeadingVerseRangesChange: (Boolean) -> Unit = {},
    onShowParagraphAndIndentsChange: (Boolean) -> Unit = {},
    onOriginalFormatModeChange: (Boolean) -> Unit = {},
    onShowJesusWordsInRedChange: (Boolean) -> Unit = {},
    onJesusWordsColorChange: (String) -> Unit = {},
    onCustomTextColorChange: (String?) -> Unit = {},
    onCustomHeadingColorChange: (String?) -> Unit = {},
    onCustomSubHeadingColorChange: (String?) -> Unit = {},
    onDualBibleConfigChange: (enabled: Boolean, hindiId: String, englishId: String, mode: DualViewMode) -> Unit = { _, _, _, _ -> },
    onShowFavoritesHintChange: (Boolean) -> Unit = {},
    onShowBookmarkHintChange: (Boolean) -> Unit = {},
    onShowHighlightsChange: (Boolean) -> Unit = {},
    onShowNoteHintChange: (Boolean) -> Unit = {},
    onJustifyBibleTextChange: (Boolean) -> Unit = {},
    onSuggestVerseSelectionChange: (Boolean) -> Unit = {},
    onShowAudioPlayerChange: (Boolean) -> Unit = {},
    onTtsSmartStartChange: (Boolean) -> Unit = {},
    onTtsBackgroundPlayChange: (Boolean) -> Unit = {},
    onTtsSleepTimerMinutesChange: (Int) -> Unit = {},
    onTtsSleepChapterCountChange: (Int) -> Unit = {},
    onEnableDevotionalBgmChange: (Boolean) -> Unit = {},
    onTtsVolumeChange: (Float) -> Unit = {},
    onBgmVolumeChange: (Float) -> Unit = {},
    onSelectedBgmTrackChange: (String) -> Unit = {},
    onCustomBgmFileSelected: (uri: String, fileName: String) -> Unit = { _, _ -> },
    onCustomBgmUrlSubmitted: (url: String) -> Unit = {},
    onScreenTimeoutChange: (Int) -> Unit = {},
    onRememberPositionChange: (Boolean) -> Unit = {},
    onResetToDefault: () -> Unit = {},
    onThemeChange: (BibleTheme) -> Unit,
    onTranslationChange: (BibleTranslation) -> Unit,
    onTranslationToggleBehaviorChange: (TranslationToggleBehavior) -> Unit = {},
    onVerseTapSelectionModeChange: (VerseTapSelectionMode) -> Unit = {},
    onShowTodaysScriptureOnHomeChange: (Boolean) -> Unit = {},
    onShowActivatedPlansOnHomeChange: (Boolean) -> Unit = {},
    onOpenReadingPlan: () -> Unit = {},
    onExternalFolderSelected: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showLicenseInfo by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf(if (settings.selectedBgmTrackId == "custom_url") settings.customBgmUri else "") }
    val previewingTrackId by DevotionalBgmManager.previewingTrackId.collectAsState()

    // SAF Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {}

            try {
                val rootDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
                rootDoc?.let { root ->
                    if (root.findFile("Bibles") == null) root.createDirectory("Bibles")
                    if (root.findFile("Commentaries") == null) root.createDirectory("Commentaries")
                }
            } catch (e: Exception) {}

            onExternalFolderSelected(uri.toString())
        }
    }

    // Launcher for user to pick an audio file from device storage
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Some providers do not support persistable permissions
            }

            var displayName = "डिवाइस ऑडियो (Device Audio)"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                // Fallback
            }

            onCustomBgmFileSelected(uri.toString(), displayName)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
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
                            text = "पठन, थीम एवं अनुवाद अनुकूलन",
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• English WEB & BBE: Public Domain English Bibles.",
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
                    // SECTION: SAF EXTERNAL FOLDER & MODULES
                    SettingsSectionHeader("बाहरी स्टोरेज एवं मॉड्यूल फोल्डर (SAF External Storage)")
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "SAF स्टोरेज एक्सेस फ्रेमवर्क (Persistent Access)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (settings.externalFolderPath.isNotBlank())
                                    "चयनित फोल्डर (Selected): ${settings.externalFolderPath.substringAfterLast("%3A")}"
                                else
                                    "कोई फोल्डर चयनित नहीं है। 'Bibles' और 'Commentaries' उप-फोल्डर स्वतः बनाए जाएंगे।",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { folderPickerLauncher.launch(null) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("फोल्डर चुनें और सिंक करें (Select Folder)")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "समर्थित प्रारूप (Supported): SQLite3, .bblx, .bbli, .mybible, .topx, .dctx, .dct.mybible, .bok.mybible, .jor.mynmbible, .zip",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 1: TRANSLATION & DUAL BIBLE
                    SettingsSectionHeader("अनुवाद एवं द्विभाषी (Translation & Dual Bible)")

                    // Translation selector
                    Text(
                        text = "अनुवाद चुनें (Select Translation)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
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
                                            BibleTranslation.HIOV.id -> "हिन्दी (HIOV)"
                                            BibleTranslation.ENGLISH_ESV.id -> "English (ESV)"
                                            BibleTranslation.PARALLEL_HI_EN.id -> "द्विभाषी (HIOV + ESV)"
                                            else -> translation.nameHindi
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

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "अनुवाद बटन टैप मोड (Translation Toggle Mode)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TranslationToggleBehavior.entries.forEach { behavior ->
                            val isSelected = settings.translationToggleBehavior == behavior
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTranslationToggleBehaviorChange(behavior) },
                                label = { Text(behavior.titleHindi, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "वचन चयन / करंट वर्स हाइलाइट मोड (Verse Tap Target):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VerseTapSelectionMode.entries.forEach { mode ->
                            val isSelected = settings.verseTapSelectionMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onVerseTapSelectionModeChange(mode) },
                                label = { Text(mode.titleHindi, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "डिफ़ॉल्ट नेविगेटर शैली (Default Navigation Style):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    var currentNavMode by remember { mutableStateOf(getSavedNavigatorMode(context)) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = currentNavMode == BibleNavigatorMode.GRID,
                            onClick = {
                                currentNavMode = BibleNavigatorMode.GRID
                                saveNavigatorMode(context, BibleNavigatorMode.GRID)
                            },
                            label = { Text("ग्रिड (3-Column Grid)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = currentNavMode == BibleNavigatorMode.LIST,
                            onClick = {
                                currentNavMode = BibleNavigatorMode.LIST
                                saveNavigatorMode(context, BibleNavigatorMode.LIST)
                            },
                            label = { Text("सूची (Step-by-Step List)", fontSize = 11.sp) }
                        )
                    }

                    // Dual Bible Customization Card
                    val isDual = selectedTranslation.id == BibleTranslation.PARALLEL_HI_EN.id || settings.isDualBibleEnabled
                    if (isDual) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "द्विभाषी बाइबल चयन (Choose Hindi + English Versions)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Hindi Version Choice
                                Text("हिन्दी संस्करण चुनें (Select Hindi):", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    BibleTranslation.HINDI_TRANSLATIONS.forEach { hTrans ->
                                        val isHSelected = settings.dualHindiVersionId == hTrans.id
                                        FilterChip(
                                            selected = isHSelected,
                                            onClick = {
                                                onDualBibleConfigChange(true, hTrans.id, settings.dualEnglishVersionId, settings.dualViewMode)
                                            },
                                            label = { Text(hTrans.nameHindi.substringBefore(" ("), fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // English Version Choice
                                Text("अंग्रेज़ी संस्करण चुनें (Select English):", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    BibleTranslation.ENGLISH_TRANSLATIONS.forEach { eTrans ->
                                        val isESelected = settings.dualEnglishVersionId == eTrans.id
                                        FilterChip(
                                            selected = isESelected,
                                            onClick = {
                                                onDualBibleConfigChange(true, settings.dualHindiVersionId, eTrans.id, settings.dualViewMode)
                                            },
                                            label = { Text(eTrans.nameEnglish.substringBefore(" ("), fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // View Mode Choice: Interleaved vs Side by Side
                                Text("प्रदर्शन प्रारूप (Display Mode):", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DualViewMode.entries.forEach { mode ->
                                        val isModeSelected = settings.dualViewMode == mode
                                        FilterChip(
                                            selected = isModeSelected,
                                            onClick = {
                                                onDualBibleConfigChange(true, settings.dualHindiVersionId, settings.dualEnglishVersionId, mode)
                                            },
                                            label = { Text(mode.titleHindi, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 2: THEMES & COLORS
                    SettingsSectionHeader("थीम्स एवं रंग (Themes & Colors)")

                    // Canvas Theme
                    Text(
                        text = "रीडिंग बैकग्राउंड थीम (Reading Background)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionBox(
                            title = "कागज़",
                            subtitle = "Paper",
                            bgColor = Color(0xFFFBF6EE),
                            textColor = Color(0xFF2C241E),
                            isSelected = settings.theme == BibleTheme.PAPER,
                            onClick = { onThemeChange(BibleTheme.PAPER) }
                        )
                        ThemeOptionBox(
                            title = "काष्ठ",
                            subtitle = "Wood",
                            bgColor = Color(0xFFF3E8D3),
                            textColor = Color(0xFF3B2B20),
                            isSelected = settings.theme == BibleTheme.WOOD,
                            onClick = { onThemeChange(BibleTheme.WOOD) }
                        )
                        ThemeOptionBox(
                            title = "आई केयर",
                            subtitle = "Eye Care",
                            bgColor = Color(0xFFFEF3E2),
                            textColor = Color(0xFF2E2519),
                            isSelected = settings.theme == BibleTheme.EYE_PROTECTION || settings.theme == BibleTheme.SEPIA,
                            onClick = { onThemeChange(BibleTheme.EYE_PROTECTION) }
                        )
                        ThemeOptionBox(
                            title = "उजला",
                            subtitle = "Light",
                            bgColor = Color(0xFFFFFFFF),
                            textColor = Color(0xFF1E293B),
                            isSelected = settings.theme == BibleTheme.LIGHT,
                            onClick = { onThemeChange(BibleTheme.LIGHT) }
                        )
                        ThemeOptionBox(
                            title = "रात्रि",
                            subtitle = "Night",
                            bgColor = Color(0xFF1F2937),
                            textColor = Color(0xFFF3F4F6),
                            isSelected = settings.theme == BibleTheme.NIGHT,
                            onClick = { onThemeChange(BibleTheme.NIGHT) }
                        )
                        ThemeOptionBox(
                            title = "डार्क",
                            subtitle = "Dark",
                            bgColor = Color(0xFF0F172A),
                            textColor = Color(0xFFE2E8F0),
                            isSelected = settings.theme == BibleTheme.DARK,
                            onClick = { onThemeChange(BibleTheme.DARK) }
                        )
                        ThemeOptionBox(
                            title = "शुद्ध काला",
                            subtitle = "AMOLED",
                            bgColor = Color(0xFF000000),
                            textColor = Color(0xFFFFFFFF),
                            isSelected = settings.theme == BibleTheme.AMOLED,
                            onClick = { onThemeChange(BibleTheme.AMOLED) }
                        )
                        ThemeOptionBox(
                            title = "शांत हरा",
                            subtitle = "Emerald",
                            bgColor = Color(0xFFEBF2EC),
                            textColor = Color(0xFF143522),
                            isSelected = settings.theme == BibleTheme.EMERALD,
                            onClick = { onThemeChange(BibleTheme.EMERALD) }
                        )
                        ThemeOptionBox(
                            title = "सिस्टम",
                            subtitle = "Dynamic",
                            bgColor = MaterialTheme.colorScheme.surfaceVariant,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            isSelected = settings.theme == BibleTheme.SYSTEM,
                            onClick = { onThemeChange(BibleTheme.SYSTEM) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Text, Heading & Subheading Color Palette Customization
                    Text(
                        text = "कस्टम रंग चयन (Custom Colors)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // 1. Text Color
                    SettingsColorRow(
                        title = "वचन पाठ का रंग (Bible Text Color)",
                        currentColorHex = settings.customTextColorHex,
                        presetColors = listOf(
                            "#1E293B" to "Dark Slate",
                            "#2C241E" to "Parchment Brown",
                            "#000000" to "Pure Black",
                            "#1E3A8A" to "Deep Navy",
                            "#14532D" to "Dark Pine"
                        ),
                        onSelect = onCustomTextColorChange
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. Heading Color
                    SettingsColorRow(
                        title = "अध्याय शीर्षक का रंग (Heading Color)",
                        currentColorHex = settings.customHeadingColorHex,
                        presetColors = listOf(
                            "#0284C7" to "Ocean Blue",
                            "#991B1B" to "Deep Crimson",
                            "#065F46" to "Emerald",
                            "#7C2D12" to "Warm Amber",
                            "#581C87" to "Royal Purple"
                        ),
                        onSelect = onCustomHeadingColorChange
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Subheading Color
                    SettingsColorRow(
                        title = "उपशीर्षक का रंग (Subheading Color)",
                        currentColorHex = settings.customSubHeadingColorHex,
                        presetColors = listOf(
                            "#0369A1" to "Sky Teal",
                            "#B45309" to "Bronze",
                            "#4338CA" to "Indigo",
                            "#0F766E" to "Deep Teal",
                            "#9F1239" to "Rose Wine"
                        ),
                        onSelect = onCustomSubHeadingColorChange
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 3: TYPOGRAPHY & FONTS
                    SettingsSectionHeader("फॉन्ट एवं लेआउट (Typography & Layout)")

                    // Stylish Fonts Selector
                    Text(
                        text = "फॉन्ट शैली (Stylish Fonts)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BibleFontFamilyType.entries.forEach { fontType ->
                            val isSelected = settings.fontStyle == fontType
                            FilterChip(
                                selected = isSelected,
                                onClick = { onFontStyleChange(fontType) },
                                label = { Text(fontType.titleHindi, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 4: SHOW IN BIBLE WINDOW OPTIONS
                    SettingsSectionHeader("बाइबल नेविगेशन एवं होम विकल्प (Main Navigation & Home Options)")

                    // Today's Scripture Option (Default OFF)
                    SettingsToggleRow(
                        title = "आज का वचन मुख्य पृष्ठ पर दिखाएं (Today's Scripture)",
                        subtitle = "बाइबल मुख्य पृष्ठ पर 'आज का वचन' प्रदर्शित करें (डिफ़ॉल्ट बंद)",
                        checked = settings.showTodaysScriptureOnHome,
                        onCheckedChange = onShowTodaysScriptureOnHomeChange
                    )

                    // Activated Plans Option (Default ON)
                    SettingsToggleRow(
                        title = "सक्रिय रीडिंग प्लान्स होमपेज पर दिखाएं (Activated Reading Plans)",
                        subtitle = "बाइबल मुख्य पृष्ठ पर केवल सक्रिय रीडिंग प्लान्स ही प्रदर्शित करें",
                        checked = settings.showActivatedPlansOnHome,
                        onCheckedChange = onShowActivatedPlansOnHomeChange
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Direct shortcut button to open Reading Plan screen
                    Card(
                        onClick = {
                            onDismiss()
                            onOpenReadingPlan()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "📖 बाइबल रीडिंग प्लान्स खोलें (Open Reading Plans)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "दैनिक पठन योजनाएं देखें एवं प्रबंधित करें",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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

                    // Chapter Outline / Verse Groupings
                    SettingsToggleRow(
                        title = "अध्याय रूपरेखा कार्ड (Chapter Outline & Groupings)",
                        subtitle = "अध्याय के शीर्ष पर वचन समूहों की रूपरेखा व त्वरित जंप दिखाएँ",
                        checked = settings.showChapterOutline,
                        onCheckedChange = onShowChapterOutlineChange
                    )

                    // Heading Verse Ranges
                    SettingsToggleRow(
                        title = "शीर्षक में वचन सीमा (Verse Ranges in Headings)",
                        subtitle = "प्रत्येक शीर्षक के साथ वचन सीमा टैग (जैसे: वचन 1–12) दिखाएँ",
                        checked = settings.showHeadingVerseRanges,
                        onCheckedChange = onShowHeadingVerseRangesChange
                    )

                    // Verse Numbers Option
                    SettingsToggleRow(
                        title = "वचन संख्या (Verse Numbers)",
                        subtitle = "प्रत्येक वचन के आगे संख्या प्रदर्शित करें",
                        checked = settings.showVerseNumbers,
                        onCheckedChange = onShowVerseNumbersChange
                    )

                    // Verse Number Size (Superscript vs Normal)
                    AnimatedVisibility(visible = settings.showVerseNumbers) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, bottom = 12.dp)
                        ) {
                            Text(
                                text = "वचन संख्या का आकार (Verse Number Size):",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VerseNumberSize.entries.forEach { vSize ->
                                    val isSelected = settings.verseNumberSize == vSize
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onVerseNumberSizeChange(vSize) },
                                        label = {
                                            Text(
                                                text = vSize.titleHindi,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

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

                    // Jesus Words in Red Option (Default OFF)
                    SettingsToggleRow(
                        title = "प्रभु यीशु के वचन रंगीन (Jesus Words)",
                        subtitle = "प्रभु यीशु मसीह के कथनों को विशिष्ट रंग में प्रदर्शित करें (डिफ़ॉल्ट बंद)",
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // ADVANCED AUDIO CUSTOMISATION IN BIBLE CUSTOMISATION
                    SettingsSectionHeader("एडवांस्ड ऑडियो अनुकूलन (Advanced Audio Customisation)")

                    // Smart Start
                    SettingsToggleRow(
                        title = "स्मार्ट स्टार्ट (Smart Start from Active Verse)",
                        subtitle = "TTS वाचन ठीक वर्तमान हाइलाइट या सक्रिय वचन से प्रारंभ करें",
                        checked = settings.ttsSmartStartActiveVerse,
                        onCheckedChange = onTtsSmartStartChange
                    )

                    // Background Play
                    SettingsToggleRow(
                        title = "बैकग्राउंड प्ले (Background Audio Playback)",
                        subtitle = "ऐप मिनिमाइज़ होने पर भी वॉइस वाचन निरंतर जारी रखें",
                        checked = settings.ttsBackgroundPlay,
                        onCheckedChange = onTtsBackgroundPlayChange
                    )

                    // Sleep Timer Limits
                    Text(
                        text = "स्लीप टाइमर सीमा (TTS Sleep Timer):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val timerLimits = listOf(
                        0 to "अनलिमिटेड (Unlimited)",
                        15 to "15 मिनट",
                        30 to "30 मिनट",
                        60 to "1 घंटा",
                        120 to "2 घंटे"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timerLimits.forEach { (mins, label) ->
                            val isSelected = settings.ttsSleepTimerMinutes == mins
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTtsSleepTimerMinutesChange(mins) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "अध्याय अनुसार स्वतः बंद (Stop after Chapters):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val chapterLimits = listOf(
                        0 to "मैन्युअल (Manual)",
                        1 to "वर्तमान अध्याय के बाद",
                        2 to "2 अध्याय बाद",
                        5 to "5 अध्याय बाद",
                        10 to "10 अध्याय बाद"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chapterLimits.forEach { (chCount, label) ->
                            val isSelected = settings.ttsSleepChapterCount == chCount
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTtsSleepChapterCountChange(chCount) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Devotional Background Music System
                    SettingsToggleRow(
                        title = "भक्तिमय बैकग्राउंड संगीत (Devotional Background Music)",
                        subtitle = "वचन वाचन (TTS) के साथ शांत एवं आत्मिक संगीत बजाएं",
                        checked = settings.enableDevotionalBgm,
                        onCheckedChange = onEnableDevotionalBgmChange
                    )

                    if (settings.enableDevotionalBgm) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "डुअल ऑडियो मिक्सर (Dual Audio Volume Mixer)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // TTS Volume
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "वॉइस TTS: ${(settings.ttsVolume * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(110.dp)
                                    )
                                    Slider(
                                        value = settings.ttsVolume,
                                        onValueChange = onTtsVolumeChange,
                                        valueRange = 0.0f..1.0f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // BGM Volume
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "बैकग्राउंड संगीत: ${(settings.bgmVolume * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(110.dp)
                                    )
                                    Slider(
                                        value = settings.bgmVolume,
                                        onValueChange = onBgmVolumeChange,
                                        valueRange = 0.0f..1.0f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "भक्ति संगीत स्रोत (Devotional Background Music Sources):",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val groupedTracks = remember {
                                    DevotionalBgmManager.availableTracks.groupBy { it.categoryHindi }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    groupedTracks.forEach { (category, trackList) ->
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                        )

                                        trackList.forEach { track ->
                                            val isTrackSelected = settings.selectedBgmTrackId == track.id
                                            val isPreviewing = previewingTrackId == track.id

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isTrackSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                border = if (isTrackSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onSelectedBgmTrackChange(track.id) }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isTrackSelected,
                                                        onClick = { onSelectedBgmTrackChange(track.id) }
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = track.nameHindi,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontWeight = if (isTrackSelected) FontWeight.Bold else FontWeight.Medium
                                                            )
                                                        )
                                                        Text(
                                                            text = track.descriptionHindi,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontSize = 10.sp
                                                            )
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = {
                                                            DevotionalBgmManager.togglePreview(
                                                                context = context,
                                                                trackId = track.id,
                                                                volume = settings.bgmVolume
                                                            )
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                            contentDescription = "Preview",
                                                            tint = if (isPreviewing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // Option 1: Custom Music from Phone Storage
                                    val isCustomFileSelected = settings.selectedBgmTrackId == "custom_file"
                                    val isFilePreviewing = previewingTrackId == "custom_file"

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCustomFileSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = if (isCustomFileSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (settings.customBgmUri.isNotBlank()) {
                                                            onSelectedBgmTrackChange("custom_file")
                                                        } else {
                                                            audioPickerLauncher.launch(arrayOf("audio/*"))
                                                        }
                                                    },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isCustomFileSelected,
                                                    onClick = {
                                                        if (settings.customBgmUri.isNotBlank()) {
                                                            onSelectedBgmTrackChange("custom_file")
                                                        } else {
                                                            audioPickerLauncher.launch(arrayOf("audio/*"))
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (settings.customBgmFileName.isNotBlank() && isCustomFileSelected) {
                                                            "📁 फ़ोन स्टोरेज से: ${settings.customBgmFileName}"
                                                        } else {
                                                            "📁 फ़ोन स्टोरेज से अपना MP3 गाना चुनें"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = if (isCustomFileSelected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    )
                                                }
                                                if (settings.customBgmUri.isNotBlank()) {
                                                    IconButton(
                                                        onClick = {
                                                            DevotionalBgmManager.togglePreview(
                                                                context = context,
                                                                trackId = "custom_file",
                                                                customUri = settings.customBgmUri,
                                                                volume = settings.bgmVolume
                                                            )
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isFilePreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                            contentDescription = "Preview Custom File",
                                                            tint = if (isFilePreviewing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    audioPickerLauncher.launch(arrayOf("audio/*"))
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (settings.customBgmUri.isNotBlank()) "फ़ोन से दूसरा गाना चुनें (Change File)" else "फ़ोन से MP3/ऑडियो फ़ाइल चुनें (Select Local Audio)",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }

                                    // Option 2: Custom Online Web Audio Stream Link
                                    val isCustomUrlSelected = settings.selectedBgmTrackId == "custom_url"
                                    val isUrlPreviewing = previewingTrackId == "custom_url"

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCustomUrlSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = if (isCustomUrlSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (customUrlInput.isNotBlank()) {
                                                            onCustomBgmUrlSubmitted(customUrlInput)
                                                        }
                                                    },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isCustomUrlSelected,
                                                    onClick = {
                                                        if (customUrlInput.isNotBlank()) {
                                                            onCustomBgmUrlSubmitted(customUrlInput)
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "🌐 ऑनलाइन वेब स्ट्रिम URL (Custom Live Audio Stream)",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = if (isCustomUrlSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                )
                                            }

                                            OutlinedTextField(
                                                value = customUrlInput,
                                                onValueChange = { customUrlInput = it },
                                                placeholder = { Text("https://example.com/devotional_stream.mp3", fontSize = 11.sp) },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                                },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (customUrlInput.isNotBlank()) {
                                                            onCustomBgmUrlSubmitted(customUrlInput.trim())
                                                        }
                                                    },
                                                    enabled = customUrlInput.isNotBlank(),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("सेव करें एवं लागू करें", style = MaterialTheme.typography.labelSmall)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        if (customUrlInput.isNotBlank()) {
                                                            DevotionalBgmManager.togglePreview(
                                                                context = context,
                                                                trackId = "custom_url",
                                                                customUri = customUrlInput.trim(),
                                                                volume = settings.bgmVolume
                                                            )
                                                        }
                                                    },
                                                    enabled = customUrlInput.isNotBlank(),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isUrlPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                        contentDescription = "Test Stream",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isUrlPreviewing) "स्टॉप" else "टेस्ट करें", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 5: SCREEN & TIMEOUT
                    SettingsSectionHeader("स्क्रीन एवं टाइमआउट (Screen & Timeout)")

                    Text(
                        text = "स्क्रीन स्वतः बंद समय (Automatic Screen Off / Timeout):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val timeoutOptions = listOf(
                        -1 to "☀️ हमेशा चालू (Always On)",
                        2 to "⏱️ 2 मिनट",
                        5 to "⏱️ 5 मिनट",
                        10 to "⏱️ 10 मिनट",
                        15 to "⏱️ 15 मिनट",
                        0 to "📱 सिस्टम डिफ़ॉल्ट"
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
                        onRememberPositionChange
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 6: RESET TO DEFAULTS
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
            title = { Text("रीसेट की पुष्टि करें (Reset Settings)") },
            text = { Text("क्या आप सभी पठन, रंग एवं अनुवाद प्राथमिकताओं को डिफ़ॉल्ट पर रीसेट करना चाहते हैं?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetToDefault()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हाँ, रीसेट करें")
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
private fun SettingsColorRow(
    title: String,
    currentColorHex: String?,
    presetColors: List<Pair<String, String>>,
    onSelect: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (currentColorHex == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clickable { onSelect(null) }
                    .border(
                        width = if (currentColorHex == null) 2.dp else 0.dp,
                        color = if (currentColorHex == null) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Text(
                    text = "थीम अनुसार",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (currentColorHex == null) FontWeight.Bold else FontWeight.Normal,
                        color = if (currentColorHex == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }

            presetColors.forEach { (hex, name) ->
                val isSelected = currentColorHex.equals(hex, ignoreCase = true)
                val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.DarkGray }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = name,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
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
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
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
    subtitle: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier
            .width(68.dp)
            .height(52.dp)
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
            modifier = Modifier.padding(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 11.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = textColor.copy(alpha = 0.75f),
                    fontSize = 9.sp
                )
            )
        }
    }
}
