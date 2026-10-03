package com.example.ui.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdminUser
import com.example.data.model.BlogSectionType
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBlogSectionManagerScreen(
    viewModel: MainViewModel,
    currentAdmin: AdminUser?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToAudioMessageManager: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val fellowshipPosts by viewModel.fellowshipPosts.collectAsStateWithLifecycle()
    val personalVlogPosts by viewModel.personalVlogPosts.collectAsStateWithLifecycle()
    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val dailyDevotions by viewModel.dailyDevotions.collectAsStateWithLifecycle()
    val audioMessageConfig by viewModel.audioMessageConfig.collectAsStateWithLifecycle()
    val isPersonalVlogAllowed by viewModel.isPersonalVlogAllowed.collectAsStateWithLifecycle()

    var showSaveSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "ब्लॉग एवं ऑडियो सेक्शन प्रबंधन",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoldWarm.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Master Admin",
                                    color = GoldWarm,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "सेक्शन जोड़ना, हटाना, क्रम बदलना व डिफ़ॉल्ट तय करना",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.resetBlogSectionsToDefault()
                            Toast.makeText(context, "डिफ़ॉल्ट क्रम व सेक्शन बहाल किए गए", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Defaults", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Interactive User Tab Bar Preview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    Icons.Default.Preview,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "यूजर ब्लॉग टैब का लाइव प्रिव्यू (Live Preview)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "डिफ़ॉल्ट: ${settings.defaultBlogSection.titleHindi}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Simulated Tab Bar
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val visibleSections = settings.blogSectionsOrder.filter {
                                settings.enabledBlogSections.contains(it) && (it != BlogSectionType.PERSONAL || isPersonalVlogAllowed)
                            }

                            if (visibleSections.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⚠️ कोई भी सेक्शन सक्रिय नहीं है! कृपया कम से कम 1 सेक्शन चालू करें।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(visibleSections) { sec ->
                                        val isDefault = settings.defaultBlogSection == sec
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDefault) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = if (isDefault) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val icon = when (sec) {
                                                    BlogSectionType.FELLOWSHIP -> Icons.Default.MenuBook
                                                    BlogSectionType.AUDIO_MESSAGES -> Icons.Default.Headphones
                                                    BlogSectionType.PERSONAL -> Icons.Default.Person
                                                    BlogSectionType.ALL -> Icons.Default.AllInbox
                                                }
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = sec.titleHindi,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isDefault) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isDefault) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isDefault) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(6.dp)
                                                    ) {}
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Info Description
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "सेक्शन क्रम एवं नियंत्रण (SECTIONS CONFIGURATION)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "क्रम बदलने के लिए ⬆ ⬇ बटन का उपयोग करें, या 'बाय डिफ़ॉल्ट' रेडियो चुनें",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Blog Sections List
            itemsIndexed(settings.blogSectionsOrder, key = { _, sec -> sec.id }) { index, section ->
                val isEnabled = settings.enabledBlogSections.contains(section)
                val isDefault = settings.defaultBlogSection == section

                val count = when (section) {
                    BlogSectionType.FELLOWSHIP -> "${fellowshipPosts.size} लेख"
                    BlogSectionType.AUDIO_MESSAGES -> "${dailyDevotions.size} ऑडियो संदेश"
                    BlogSectionType.PERSONAL -> "${personalVlogPosts.size} व्लॉग"
                    BlogSectionType.ALL -> "${allPosts.size} कुल लेख"
                }

                val icon = when (section) {
                    BlogSectionType.FELLOWSHIP -> Icons.Default.MenuBook
                    BlogSectionType.AUDIO_MESSAGES -> Icons.Default.Headphones
                    BlogSectionType.PERSONAL -> Icons.Default.Person
                    BlogSectionType.ALL -> Icons.Default.AllInbox
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDefault) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDefault) 2.dp else 1.dp),
                    border = if (isDefault) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Section Icon Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when (section) {
                                    BlogSectionType.FELLOWSHIP -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    BlogSectionType.AUDIO_MESSAGES -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    BlogSectionType.PERSONAL -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    BlogSectionType.ALL -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = section.titleEnglish,
                                        tint = when (section) {
                                            BlogSectionType.FELLOWSHIP -> MaterialTheme.colorScheme.primary
                                            BlogSectionType.AUDIO_MESSAGES -> MaterialTheme.colorScheme.tertiary
                                            BlogSectionType.PERSONAL -> MaterialTheme.colorScheme.secondary
                                            BlogSectionType.ALL -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = section.titleHindi,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "(${section.titleEnglish})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = section.descriptionHindi,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = count,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (section == BlogSectionType.AUDIO_MESSAGES) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (audioMessageConfig.isServiceActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (audioMessageConfig.isServiceActive) "🟢 सर्विस लाइव" else "🔴 सर्विस बंद",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (audioMessageConfig.isServiceActive) Color(0xFF059669) else Color(0xFFDC2626),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Re-order Buttons & Switch
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Move Up
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val list = settings.blogSectionsOrder.toMutableList()
                                            val temp = list[index]
                                            list[index] = list[index - 1]
                                            list[index - 1] = temp
                                            viewModel.updateBlogSectionsOrder(list)
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                                }

                                // Move Down
                                IconButton(
                                    onClick = {
                                        if (index < settings.blogSectionsOrder.size - 1) {
                                            val list = settings.blogSectionsOrder.toMutableList()
                                            val temp = list[index]
                                            list[index] = list[index + 1]
                                            list[index + 1] = temp
                                            viewModel.updateBlogSectionsOrder(list)
                                        }
                                    },
                                    enabled = index < settings.blogSectionsOrder.size - 1,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Enable / Disable Switch
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleBlogSection(section, checked)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottom Actions Row: Set as Default + Quick Shortcut
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "By Default" Selector
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = isEnabled) {
                                        viewModel.setDefaultBlogSection(section)
                                        Toast.makeText(context, "'${section.titleHindi}' को डिफ़ॉल्ट सेक्शन सेट किया गया", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = isDefault,
                                    onClick = {
                                        if (isEnabled) {
                                            viewModel.setDefaultBlogSection(section)
                                            Toast.makeText(context, "'${section.titleHindi}' को डिफ़ॉल्ट सेक्शन सेट किया गया", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = isEnabled,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isDefault) "⭐ यह डिफ़ॉल्ट सेक्शन है (Opens by default)" else "इसे डिफ़ॉल्ट बनाएं (Set as Default)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isDefault) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            // Extra Config Shortcut for Audio Message
                            if (section == BlogSectionType.AUDIO_MESSAGES) {
                                TextButton(
                                    onClick = onNavigateToAudioMessageManager,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("रिकॉर्डिंग / शेड्यूल", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Quick Actions & Server Save Box
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "मास्टर सिंक एवं लेआउट अपडेट",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "आपके द्वारा किए गए बदलाव तुरंत ऐप में लागू हो जाते हैं। जब भी विश्वासी ब्लॉग टैब खोलेंगे, उन्हें आपका सेट किया हुआ क्रम और डिफ़ॉल्ट सेक्शन दिखाई देगा।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    BlogSectionType.entries.forEach { viewModel.toggleBlogSection(it, true) }
                                    Toast.makeText(context, "सभी सेक्शन सक्रिय कर दिए गए", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("सभी सेक्शन चालू करें", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    showSaveSuccessDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("कॉन्फ़िगरेशन सुरक्षित", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSaveSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(36.dp)) },
            title = { Text("ब्लॉग लेआउट सुरक्षित किया गया") },
            text = {
                Text("ब्लॉग टैब की सेटिंग सफलतापूर्वक अपडेट हो गई है। डिफ़ॉल्ट सेक्शन '${settings.defaultBlogSection.titleHindi}' सेट है।")
            },
            confirmButton = {
                TextButton(onClick = { showSaveSuccessDialog = false }) {
                    Text("ठीक है")
                }
            }
        )
    }
}
