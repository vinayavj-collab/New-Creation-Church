package com.example.ui.prayer

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.prayer.model.PersonalPrayerItem
import com.example.data.prayer.repository.PersonalPrayerJournalRepository
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalPrayerJournalDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val journalRepo = remember { PersonalPrayerJournalRepository.getInstance(context) }
    val allPrayers by journalRepo.prayers.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active, 1: Answered
    var showAddDialog by remember { mutableStateOf(false) }
    var answeringPrayerItem by remember { mutableStateOf<PersonalPrayerItem?>(null) }

    val activePrayers = remember(allPrayers) { allPrayers.filter { !it.isAnswered } }
    val answeredPrayers = remember(allPrayers) { allPrayers.filter { it.isAnswered } }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = GoldWarm,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "मेरी व्यक्तिगत प्रार्थना डायरी",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Personal Prayer Journal & Testimonies",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "बंद करें")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Active vs Answered
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "सक्रिय विनती (${activePrayers.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "उत्तर मिला! गवाही (${answeredPrayers.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.Celebration, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add button for Active Prayers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "अपनी व्यक्तिगत प्रार्थना सूची:" else "प्रभु द्वारा सुनी गई प्रार्थनाएं व गवाहियां:",
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.outline)
                    )
                    Button(
                        onClick = { showAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("नया विषय", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List content
                val currentList = if (selectedTab == 0) activePrayers else answeredPrayers

                if (currentList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Default.NoteAdd else Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (selectedTab == 0)
                                    "अभी कोई सक्रिय प्रार्थना विषय नहीं है."
                                else
                                    "अभी तक कोई उत्तरित प्रार्थना दर्ज नहीं है.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = if (selectedTab == 0)
                                    "ऊपर दिए '+ नया विषय' बटन पर टैप करके अपनी विनती लिखें."
                                else
                                    "जब आपकी प्रार्थना का उत्तर मिले, तो सक्रिय सूची में 'उत्तर मिला 🙌' पर टैप करें!",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(currentList, key = { it.id }) { item ->
                            PersonalPrayerCard(
                                item = item,
                                onMarkAnswered = { answeringPrayerItem = item },
                                onDelete = {
                                    journalRepo.deletePrayer(item.id)
                                    Toast.makeText(context, "प्रार्थना विषय हटाया गया", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add New Prayer Request Sub-Dialog
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newDetails by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = GoldWarm)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("नया प्रार्थना विषय लिखें")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("प्रार्थना का शीर्षक (जैसे: पिताजी के स्वास्थ्य के लिए)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newDetails,
                        onValueChange = { newDetails = it },
                        label = { Text("विस्तार / वचन (वैकल्पिक)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            journalRepo.addPrayer(newTitle, newDetails)
                            showAddDialog = false
                            Toast.makeText(context, "प्रार्थना विषय डायरी में जोड़ा गया! 🙏", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = newTitle.isNotBlank()
                ) {
                    Text("जोड़ें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Answered Prayer Testimony Input Sub-Dialog
    answeringPrayerItem?.let { item ->
        var testimonyNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { answeringPrayerItem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = GoldWarm)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("प्रभु की स्तुति हो! 🙌")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "विषय: ${item.title}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "प्रभु ने आपकी प्रार्थना कैसे सुनी? अपनी गवाही या तारीख लिखें:",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    OutlinedTextField(
                        value = testimonyNotes,
                        onValueChange = { testimonyNotes = it },
                        placeholder = { Text("उदा. प्रभु ने डॉक्टर की रिपोर्ट सामान्य कर दी और चंगाई दी!") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        journalRepo.markPrayerAnswered(item.id, testimonyNotes)
                        answeringPrayerItem = null
                        selectedTab = 1 // Switch to Answered tab to celebrate!
                        Toast.makeText(context, "गवाही दर्ज की गई! प्रभु को महिमा मिले! 🙌", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm)
                ) {
                    Text("गवाही दर्ज करें (आमीन)")
                }
            },
            dismissButton = {
                TextButton(onClick = { answeringPrayerItem = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun PersonalPrayerCard(
    item: PersonalPrayerItem,
    onMarkAnswered: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isAnswered)
                GoldWarm.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (item.isAnswered) GoldWarm else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isAnswered) Icons.Default.Check else Icons.Default.VolunteerActivism,
                            contentDescription = null,
                            tint = if (item.isAnswered) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "हटाएं",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (item.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.details,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            // Answered Testimony Section
            if (item.isAnswered && !item.testimonyNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldWarm.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                        Text(text = "🎉 ", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                        Text(
                            text = "गवाही: ${item.testimonyNotes} (${item.answeredDate ?: ""})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "दिनांक: ${item.createdDate}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                )

                if (!item.isAnswered) {
                    Button(
                        onClick = onMarkAnswered,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldWarm)
                    ) {
                        Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "उत्तर मिला! 🙌",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "✓ उत्तरित प्रार्थना",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
