package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.util.BackgroundMusicManager
import com.example.util.BackgroundMusicTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBackgroundMusicScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        BackgroundMusicManager.init(context)
    }

    val tracks by BackgroundMusicManager.tracks.collectAsState()
    val selectedPrayerTrackId by BackgroundMusicManager.selectedPrayerTrackId.collectAsState()
    val isPlaying by BackgroundMusicManager.isPlaying.collectAsState()
    val currentlyPlayingTrack by BackgroundMusicManager.currentlyPlayingTrack.collectAsState()

    var showAddEditDialog by remember { mutableStateOf<BackgroundMusicTrack?>(null) }
    var isNewTrack by remember { mutableStateOf(false) }
    var trackToDelete by remember { mutableStateOf<BackgroundMusicTrack?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("सभी") }

    val categories = listOf("सभी", "प्रार्थना", "आराधना", "अन्य")

    val filteredTracks = remember(tracks, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "प्रार्थना" -> tracks.filter { it.isPrayerSpecific || it.category.contains("प्रार्थना") }
            "आराधना" -> tracks.filter { it.category.contains("आराधना") }
            "अन्य" -> tracks.filter { !it.isPrayerSpecific && !it.category.contains("प्रार्थना") && !it.category.contains("आराधना") }
            else -> tracks
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "बैकग्राउंड म्यूजिक सेटिंग",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "मास्टर एडमिन: म्यूजिक व प्रेयर लाइब्रेरी कॉन्फ़िगरेशन",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        BackgroundMusicManager.stopMusic()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isPlaying) {
                        IconButton(onClick = { BackgroundMusicManager.stopMusic() }) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop BGM", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    isNewTrack = true
                    showAddEditDialog = BackgroundMusicTrack(
                        title = "",
                        url = "",
                        category = "प्रार्थना (Prayer)",
                        isPrayerSpecific = true
                    )
                },
                containerColor = GoldWarm,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("नया लिंक / म्यूजिक जोड़ें", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "प्रेयर व बैकग्राउंड म्यूजिक नियंत्रण",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "यहाँ मास्टर एडमिन किसी भी ऑडियो लिंक या प्लेलिस्ट स्रोत को जोड़, संपादित अथवा हटा सकते हैं। चयनित म्यूजिक दैनिक प्रार्थना में उपयोगकर्ताओं के लिए बजेगा।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category Filter Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldWarm.copy(alpha = 0.25f),
                                selectedLabelColor = GoldWarm
                            )
                        )
                    }
                }
            }

            // List of Music Tracks
            items(filteredTracks, key = { it.id }) { track ->
                val isSelectedForPrayer = track.id == selectedPrayerTrackId
                val isCurrentlyPlaying = isPlaying && currentlyPlayingTrack?.id == track.id

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelectedForPrayer) {
                            GoldWarm.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (isSelectedForPrayer) BorderStroke(1.5.dp, GoldWarm) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrentlyPlaying) GoldWarm else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentlyPlaying) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isCurrentlyPlaying) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = track.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = track.category,
                                        fontSize = 11.sp,
                                        color = GoldWarm,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (isSelectedForPrayer) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = GoldWarm
                                ) {
                                    Text(
                                        text = "✓ प्रार्थना सक्रिय",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (track.url.isNotBlank() && track.url != BackgroundMusicManager.BUILTIN_SYNTH_URL) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "स्रोत: ${track.url}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Play / Pause Button
                                FilledTonalButton(
                                    onClick = {
                                        if (isCurrentlyPlaying) {
                                            BackgroundMusicManager.stopMusic()
                                        } else {
                                            BackgroundMusicManager.playTrack(context, track)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isCurrentlyPlaying) "रोकें" else "सुनें", fontSize = 12.sp)
                                }

                                if (!isSelectedForPrayer) {
                                    OutlinedButton(
                                        onClick = {
                                            BackgroundMusicManager.setSelectedPrayerTrack(context, track.id)
                                            Toast.makeText(context, "प्रार्थना हेतु सेट किया गया", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("प्रार्थना हेतु चुनें", fontSize = 11.sp)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        isNewTrack = false
                                        showAddEditDialog = track
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary)
                                }

                                if (!track.isDefault) {
                                    IconButton(
                                        onClick = { trackToDelete = track },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // Add / Edit Track Dialog
    showAddEditDialog?.let { currentEditing ->
        var editTitle by remember { mutableStateOf(currentEditing.title) }
        var editUrl by remember { mutableStateOf(currentEditing.url) }
        var editCategory by remember { mutableStateOf(currentEditing.category) }
        var editIsPrayerSpecific by remember { mutableStateOf(currentEditing.isPrayerSpecific) }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = null },
            title = {
                Text(
                    text = if (isNewTrack) "नया म्यूजिक / प्लेलिस्ट लिंक जोड़ें" else "म्यूजिक ट्रैक संपादित करें",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("शीर्षक (Title)") },
                        placeholder = { Text("उदा. शांतिदायक आराधना धुन") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("ऑडियो / प्लेलिस्ट लिंक (Stream URL)") },
                        placeholder = { Text("https://example.com/audio.mp3") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("श्रेणी (Category)") },
                        placeholder = { Text("प्रार्थना, आराधना, ध्यान") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = editIsPrayerSpecific,
                            onCheckedChange = { editIsPrayerSpecific = it }
                        )
                        Text(
                            text = "प्रार्थना लाइब्रेरी में जोड़ें",
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isBlank()) {
                            Toast.makeText(context, "कृपया शीर्षक दर्ज करें", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val updated = currentEditing.copy(
                            title = editTitle.trim(),
                            url = editUrl.trim(),
                            category = editCategory.trim().ifBlank { "प्रार्थना (Prayer)" },
                            isPrayerSpecific = editIsPrayerSpecific
                        )
                        BackgroundMusicManager.addOrUpdateTrack(context, updated)
                        Toast.makeText(context, if (isNewTrack) "म्यूजिक लिंक सफलतापूर्वक जोड़ा गया" else "अपडेट हो गया", Toast.LENGTH_SHORT).show()
                        showAddEditDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("सहेजें (Save Track)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    trackToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            title = { Text("ट्रैक हटाएं?") },
            text = { Text("क्या आप \"${target.title}\" को लाइब्रेरी से हटाना चाहते हैं?") },
            confirmButton = {
                Button(
                    onClick = {
                        BackgroundMusicManager.deleteTrack(context, target.id)
                        Toast.makeText(context, "हटा दिया गया", Toast.LENGTH_SHORT).show()
                        trackToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हटाएं")
                }
            },
            dismissButton = {
                TextButton(onClick = { trackToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
