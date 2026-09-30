package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.VideoUrlParser
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVideoPlaylistManagerScreen(
    viewModel: MainViewModel,
    currentAdmin: AdminUser?,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val allVideos by viewModel.youtubeVideos.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.youtubePlaylists.collectAsStateWithLifecycle()
    val pinnedVideoId by viewModel.pinnedVideoId.collectAsStateWithLifecycle()
    val quickAccessConfig by viewModel.videoQuickAccessConfig.collectAsStateWithLifecycle()

    var selectedSection by remember { mutableIntStateOf(0) }
    val sections = listOf("📺 वीडियो लिंक", "📑 प्लेलिस्ट प्रबंधन", "⚡ क्विक एक्सेस टैग्स")

    var searchQuery by remember { mutableStateOf("") }

    // Dialog States for Video
    var showAddVideoDialog by remember { mutableStateOf(false) }
    var editingVideo by remember { mutableStateOf<YouTubeVideo?>(null) }
    var videoToDelete by remember { mutableStateOf<YouTubeVideo?>(null) }

    // Dialog States for Playlist
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var showCreateNamedPlaylistDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<YouTubePlaylist?>(null) }
    var playlistToManageVideos by remember { mutableStateOf<YouTubePlaylist?>(null) }
    var playlistToDelete by remember { mutableStateOf<YouTubePlaylist?>(null) }

    // Filtered Videos
    val filteredVideos = remember(allVideos, searchQuery) {
        if (searchQuery.isBlank()) allVideos
        else allVideos.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.channelTitle.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    // Filtered Playlists
    val filteredPlaylists = remember(allPlaylists, searchQuery) {
        if (searchQuery.isBlank()) allPlaylists
        else allPlaylists.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.channelTitle.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Horizontal Section Tabs
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedSection,
                containerColor = Color.Transparent,
                contentColor = GoldWarm
            ) {
                sections.forEachIndexed { index, title ->
                    val isSelected = selectedSection == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedSection = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }

        when (selectedSection) {
            0 -> {
                // Video Links Management
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    // Header Action Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.VideoLibrary,
                                            contentDescription = null,
                                            tint = GoldWarm,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "वीडियो लिंक प्रबंधन",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "कुल ${allVideos.size} वीडियो • यूट्यूब, लाइव व डायरेक्ट लिंक",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            editingVideo = null
                                            showAddVideoDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("नया वीडियो जोड़ें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Search Bar
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("वीडियो का शीर्षक या चैनल खोजें...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Video List
                    if (filteredVideos.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "कोई वीडियो नहीं मिला" else "कोई वीडियो उपलब्ध नहीं है। ऊपर दिए गए बटन से नया वीडियो लिंक जोड़ें।",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredVideos, key = { it.id }) { video ->
                            val isPinned = video.id == pinnedVideoId || video.isPinned
                            AdminVideoCard(
                                video = video,
                                isPinned = isPinned,
                                onTogglePin = {
                                    viewModel.togglePinVideo(video.id) { success, nowPinned ->
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                if (nowPinned) "वीडियो पिन किया गया 📌" else "वीडियो अनपिन किया गया",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onEdit = {
                                    editingVideo = video
                                    showAddVideoDialog = true
                                },
                                onDelete = {
                                    videoToDelete = video
                                },
                                onCopyUrl = {
                                    clipboardManager.setText(AnnotatedString(video.videoUrl))
                                    Toast.makeText(context, "वीडियो लिंक कॉपी किया गया! 📋", Toast.LENGTH_SHORT).show()
                                },
                                onPreview = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.videoUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "लिंक खोलने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Video Playlist Management
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    // Header Action Card for Playlists
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.FeaturedPlayList,
                                            contentDescription = null,
                                            tint = GoldWarm,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "प्लेलिस्ट प्रबंधन (Playlists)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "कुल ${allPlaylists.size} प्लेलिस्ट • वीडियो मैपिंग व संग्रह",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            showCreateNamedPlaylistDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("नई प्लेलिस्ट बनाएं", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            editingPlaylist = null
                                            showAddPlaylistDialog = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("यूट्यूब प्लेलिस्ट लिंक", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Search Bar
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("प्लेलिस्ट का नाम या चैनल खोजें...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Playlist List
                    if (filteredPlaylists.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "कोई प्लेलिस्ट नहीं मिली" else "कोई प्लेलिस्ट उपलब्ध नहीं है। ऊपर से नई प्लेलिस्ट बनाएं।",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredPlaylists, key = { it.id }) { playlist ->
                            AdminPlaylistCard(
                                playlist = playlist,
                                onManageVideos = {
                                    playlistToManageVideos = playlist
                                },
                                onEdit = {
                                    editingPlaylist = playlist
                                    showAddPlaylistDialog = true
                                },
                                onDelete = {
                                    playlistToDelete = playlist
                                },
                                onCopyUrl = {
                                    clipboardManager.setText(AnnotatedString(playlist.playlistUrl))
                                    Toast.makeText(context, "प्लेलिस्ट लिंक कॉपी किया गया! 📋", Toast.LENGTH_SHORT).show()
                                },
                                onPreview = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playlist.playlistUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "लिंक खोलने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            2 -> {
                // Quick Access Bar & Tags Customizer Tab
                AdminQuickAccessCustomizerSection(
                    viewModel = viewModel,
                    config = quickAccessConfig
                )
            }
        }
    }

    // Add/Edit Video Dialog
    if (showAddVideoDialog) {
        AddEditVideoDialog(
            existingVideo = editingVideo,
            onDismiss = {
                showAddVideoDialog = false
                editingVideo = null
            },
            onSave = { video ->
                viewModel.addOrUpdateCustomVideo(video) { success ->
                    if (success) {
                        Toast.makeText(
                            context,
                            if (editingVideo != null) "वीडियो अपडेट किया गया! ✅" else "नया वीडियो सफलतापूर्वक जोड़ा गया! 🎉",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(context, "सहेजने में विफल!", Toast.LENGTH_SHORT).show()
                    }
                }
                showAddVideoDialog = false
                editingVideo = null
            }
        )
    }

    // Add/Edit Playlist Dialog
    if (showAddPlaylistDialog) {
        AddEditPlaylistDialog(
            existingPlaylist = editingPlaylist,
            onDismiss = {
                showAddPlaylistDialog = false
                editingPlaylist = null
            },
            onSave = { playlist ->
                viewModel.addOrUpdateYouTubePlaylist(playlist) { success ->
                    if (success) {
                        Toast.makeText(
                            context,
                            if (editingPlaylist != null) "प्लेलिस्ट अपडेट की गई! ✅" else "प्लेलिस्ट सफलतापूर्वक जोड़ी गई! 🎉",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(context, "सहेजने में विफल!", Toast.LENGTH_SHORT).show()
                    }
                }
                showAddPlaylistDialog = false
                editingPlaylist = null
            }
        )
    }

    // Create Named Playlist Dialog with initial video URLs
    if (showCreateNamedPlaylistDialog) {
        CreateNamedPlaylistDialog(
            allVideos = allVideos,
            onDismiss = { showCreateNamedPlaylistDialog = false },
            onSave = { playlist, newVideosList ->
                viewModel.addOrUpdateYouTubePlaylist(playlist) { plSuccess ->
                    if (plSuccess) {
                        newVideosList.forEach { vid ->
                            viewModel.addOrUpdateCustomVideo(vid)
                        }
                        Toast.makeText(context, "प्लेलिस्ट '${playlist.title}' सहेजी गई! 🎉", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "प्लेलिस्ट सहेजने में विफल!", Toast.LENGTH_SHORT).show()
                    }
                }
                showCreateNamedPlaylistDialog = false
            }
        )
    }

    // Manage Videos inside a Playlist Dialog
    if (playlistToManageVideos != null) {
        val activePlaylist = allPlaylists.find { it.id == playlistToManageVideos!!.id } ?: playlistToManageVideos!!
        PlaylistVideoManagerDialog(
            playlist = activePlaylist,
            onDismiss = { playlistToManageVideos = null },
            onAddVideoUrl = { url ->
                viewModel.addVideoUrlToPlaylist(activePlaylist.id, url) { success ->
                    if (success) {
                        Toast.makeText(context, "वीडियो लिंक प्लेलिस्ट में जोड़ा गया! ✅", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onRemoveVideoUrl = { url ->
                viewModel.removeVideoUrlFromPlaylist(activePlaylist.id, url) { success ->
                    if (success) {
                        Toast.makeText(context, "वीडियो लिंक प्लेलिस्ट से हटाया गया", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Delete Video Confirmation
    if (videoToDelete != null) {
        val vid = videoToDelete!!
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("वीडियो हटाएं?") },
            text = { Text("क्या आप '${vid.title}' को वीडियो सूची से हटाना चाहते हैं?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomVideo(vid.id) { success ->
                            if (success) {
                                Toast.makeText(context, "वीडियो हटाया गया", Toast.LENGTH_SHORT).show()
                            }
                        }
                        videoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हटाएं")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Delete Playlist Confirmation
    if (playlistToDelete != null) {
        val pl = playlistToDelete!!
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("प्लेलिस्ट हटाएं?") },
            text = { Text("क्या आप '${pl.title}' प्लेलिस्ट को सूची से हटाना चाहते हैं?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteYouTubePlaylist(pl.id) { success ->
                            if (success) {
                                Toast.makeText(context, "प्लेलिस्ट हटाई गई", Toast.LENGTH_SHORT).show()
                            }
                        }
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हटाएं")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun AdminVideoCard(
    video: YouTubeVideo,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyUrl: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) GoldWarm.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (isPinned) 1.5.dp else 0.8.dp,
            if (isPinned) GoldWarm else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.1f))
                        .clickable { onPreview() }
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl.ifBlank { "https://img.youtube.com/vi/${video.id}/hqdefault.jpg" },
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (isPinned) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GoldWarm,
                            contentColor = Color.Black
                        ) {
                            Text(
                                text = "📌 शीर्ष पर पिन (Pinned)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = video.title.ifBlank { "बिना शीर्षक का वीडियो" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "चैनल: ${video.channelTitle.ifBlank { "Vinay Kumar AVJ" }}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "ID: ${video.id}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(6.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pin button
                IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.PushPin else Icons.Default.PinInvoke,
                        contentDescription = "Pin",
                        tint = if (isPinned) GoldWarm else MaterialTheme.colorScheme.outline
                    )
                }

                // Copy URL button
                IconButton(onClick = onCopyUrl, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }

                // Preview Link button
                IconButton(onClick = onPreview, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }

                // Edit button
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }

                // Delete button
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AdminPlaylistCard(
    playlist: YouTubePlaylist,
    onManageVideos: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyUrl: () -> Unit,
    onPreview: () -> Unit
) {
    val totalCount = if (playlist.videoUrls.isNotEmpty()) playlist.videoUrls.size else (playlist.videoCountEstimate ?: 0)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Playlist Cover Box
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyPrimary.copy(alpha = 0.15f))
                        .clickable { onPreview() }
                ) {
                    if (!playlist.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = playlist.thumbnailUrl,
                            contentDescription = playlist.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.FeaturedPlayList,
                            contentDescription = null,
                            tint = GoldWarm,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center)
                        )
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(topStart = 6.dp)
                    ) {
                        Text(
                            text = "$totalCount वीडियो",
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Playlist Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = playlist.title.ifBlank { "बिना नाम की प्लेलिस्ट" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "चैनल: ${playlist.channelTitle.ifBlank { "New Creation Church" }}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (playlist.videoUrls.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GoldWarm.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, GoldWarm.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "✨ ${playlist.videoUrls.size} वीडियो मैप किए गए",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldWarm,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "ID: ${playlist.id}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(6.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Manage Videos inside playlist button
                Button(
                    onClick = onManageVideos,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm.copy(alpha = 0.2f), contentColor = GoldWarm),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("वीडियो लिंक प्रबंधित करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Copy URL
                    IconButton(onClick = onCopyUrl, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    // Preview
                    IconButton(onClick = onPreview, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    // Edit
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    // Delete
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Dialog to view, add and remove individual video URLs inside a specific Playlist!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistVideoManagerDialog(
    playlist: YouTubePlaylist,
    onDismiss: () -> Unit,
    onAddVideoUrl: (String) -> Unit,
    onRemoveVideoUrl: (String) -> Unit
) {
    var newUrlInput by remember { mutableStateOf("") }
    val currentUrls = playlist.videoUrls

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "प्लेलिस्ट वीडियो संपादक",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = playlist.title,
                    fontSize = 12.sp,
                    color = GoldWarm,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Add new URL Input Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = newUrlInput,
                        onValueChange = { newUrlInput = it },
                        placeholder = { Text("वीडियो URL पेस्ट करें...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            if (newUrlInput.isNotBlank()) {
                                onAddVideoUrl(newUrlInput.trim())
                                newUrlInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        Text("जोड़ें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text(
                    text = "मैप किए गए वीडियो लिंक्स (${currentUrls.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (currentUrls.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "इस प्लेलिस्ट में अभी कोई कस्टम वीडियो लिंक नहीं है। ऊपर दिए गए बॉक्स से वीडियो URL जोड़ें।",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(currentUrls) { index, url ->
                            val parsed = VideoUrlParser.parse(url)
                            val thumb = if (parsed.thumbnailUrl.isNotBlank()) parsed.thumbnailUrl else "https://img.youtube.com/vi/${parsed.videoId}/hqdefault.jpg"

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 50.dp, height = 32.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Black.copy(alpha = 0.2f))
                                        ) {
                                            AsyncImage(
                                                model = thumb,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "वीडियो #${index + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = url,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onRemoveVideoUrl(url) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black)
            ) {
                Text("पूर्ण (Done)", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVideoDialog(
    existingVideo: YouTubeVideo?,
    onDismiss: () -> Unit,
    onSave: (YouTubeVideo) -> Unit
) {
    var urlInput by remember { mutableStateOf(existingVideo?.videoUrl ?: "") }
    var titleInput by remember { mutableStateOf(existingVideo?.title ?: "") }
    var channelTitleInput by remember { mutableStateOf(existingVideo?.channelTitle ?: "Vinay Kumar AVJ") }
    var channelIdInput by remember { mutableStateOf(existingVideo?.channelId ?: "UClFK75L0wsDMf10Tj77hlsg") }
    var descriptionInput by remember { mutableStateOf(existingVideo?.description ?: "") }
    var thumbnailInput by remember { mutableStateOf(existingVideo?.thumbnailUrl ?: "") }
    var isPinnedInput by remember { mutableStateOf(existingVideo?.isPinned ?: false) }

    // Auto parse when URL is typed/pasted
    LaunchedEffect(urlInput) {
        if (urlInput.isNotBlank()) {
            val parsed = VideoUrlParser.parse(urlInput)
            if (parsed.videoId.isNotBlank() && thumbnailInput.isBlank()) {
                thumbnailInput = if (parsed.thumbnailUrl.isNotBlank()) parsed.thumbnailUrl else "https://img.youtube.com/vi/${parsed.videoId}/hqdefault.jpg"
            }
        }
    }

    val channelSuggestions = listOf(
        "Vinay Kumar AVJ" to "UClFK75L0wsDMf10Tj77hlsg",
        "Worship New Creation Church" to "UC92tSCn2I6lwcUyAdyS_MMw",
        "New Creation Church Official" to "UC4lEaYq9Wp_l5jXgU_1_1gg",
        "रविवार आराधना (Sunday Service)" to "sunday_service",
        "गवाही व संदेश (Testimonies)" to "testimonies",
        "दैनिक प्रार्थना (Daily Prayer)" to "prayer"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingVideo != null) "वीडियो लिंक संपादित करें" else "नया वीडियो लिंक जोड़ें",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("वीडियो URL / YouTube Link *") },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("वीडियो का शीर्षक (Title) *") },
                        placeholder = { Text("उदा. सामर्थी वचन व आराधना...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Text("चैनल / श्रेणी का चयन करें:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        channelSuggestions.take(3).forEach { (name, id) ->
                            FilterChip(
                                selected = channelTitleInput == name,
                                onClick = {
                                    channelTitleInput = name
                                    channelIdInput = id
                                },
                                label = { Text(name, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = channelTitleInput,
                        onValueChange = { channelTitleInput = it },
                        label = { Text("कस्टम चैनल या श्रेणी नाम") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("विवरण (Description - वैकल्पिक)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📌 ऐप के शीर्ष पर पिन करें", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = isPinnedInput,
                            onCheckedChange = { isPinnedInput = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlInput.isBlank() && titleInput.isBlank()) return@Button
                    val parsed = VideoUrlParser.parse(urlInput)
                    val vidId = if (parsed.videoId.isNotBlank()) parsed.videoId else existingVideo?.id ?: ("custom_" + System.currentTimeMillis())
                    val thumb = if (thumbnailInput.isNotBlank()) thumbnailInput else if (parsed.thumbnailUrl.isNotBlank()) parsed.thumbnailUrl else "https://img.youtube.com/vi/$vidId/hqdefault.jpg"

                    val video = YouTubeVideo(
                        id = vidId,
                        title = titleInput.ifBlank { "नया वीडियो ($vidId)" },
                        channelId = channelIdInput.ifBlank { "vinay_channel" },
                        channelTitle = channelTitleInput.ifBlank { "Vinay Kumar AVJ" },
                        thumbnailUrl = thumb,
                        publishedAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        publishedTimestamp = System.currentTimeMillis(),
                        description = descriptionInput,
                        videoUrl = urlInput.ifBlank { "https://www.youtube.com/watch?v=$vidId" },
                        isRemote = true,
                        isPinned = isPinnedInput
                    )
                    onSave(video)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black)
            ) {
                Text("सहेजें (Save)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlaylistDialog(
    existingPlaylist: YouTubePlaylist?,
    onDismiss: () -> Unit,
    onSave: (YouTubePlaylist) -> Unit
) {
    var urlInput by remember { mutableStateOf(existingPlaylist?.playlistUrl ?: "") }
    var titleInput by remember { mutableStateOf(existingPlaylist?.title ?: "") }
    var channelTitleInput by remember { mutableStateOf(existingPlaylist?.channelTitle ?: "New Creation Church") }
    var thumbnailInput by remember { mutableStateOf(existingPlaylist?.thumbnailUrl ?: "") }
    var descriptionInput by remember { mutableStateOf(existingPlaylist?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingPlaylist != null) "यूट्यूब प्लेलिस्ट संपादित करें" else "यूट्यूब प्लेलिस्ट लिंक जोड़ें",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("प्लेलिस्ट URL / Playlist Link *") },
                        placeholder = { Text("https://www.youtube.com/playlist?list=PL...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("प्लेलिस्ट का शीर्षक (Title) *") },
                        placeholder = { Text("उदा. आत्मिक गीत व आराधना संग्रह...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = channelTitleInput,
                        onValueChange = { channelTitleInput = it },
                        label = { Text("चैनल या मंत्रालय का नाम") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("विवरण (Description - वैकल्पिक)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = thumbnailInput,
                        onValueChange = { thumbnailInput = it },
                        label = { Text("कस्टम थंबनेल URL (वैकल्पिक)") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleInput.isBlank() && urlInput.isBlank()) return@Button
                    var playlistId = existingPlaylist?.id ?: ""
                    if (playlistId.isBlank() || playlistId.startsWith("http")) {
                        val uri = Uri.parse(urlInput)
                        playlistId = uri.getQueryParameter("list") ?: ("pl_" + System.currentTimeMillis())
                    }
                    val playlist = YouTubePlaylist(
                        id = playlistId,
                        title = titleInput.ifBlank { "प्लेलिस्ट ($playlistId)" },
                        channelTitle = channelTitleInput.ifBlank { "New Creation Church" },
                        playlistUrl = urlInput.ifBlank { "https://youtube.com/playlist?list=$playlistId" },
                        videoCountEstimate = existingPlaylist?.videoCountEstimate ?: 10,
                        thumbnailUrl = thumbnailInput.ifBlank { existingPlaylist?.thumbnailUrl },
                        description = descriptionInput,
                        videoUrls = existingPlaylist?.videoUrls ?: emptyList(),
                        videoIds = existingPlaylist?.videoIds ?: emptyList(),
                        isCustom = existingPlaylist?.isCustom ?: false
                    )
                    onSave(playlist)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black)
            ) {
                Text("सहेजें (Save)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun CreateNamedPlaylistDialog(
    allVideos: List<YouTubeVideo>,
    onDismiss: () -> Unit,
    onSave: (YouTubePlaylist, List<YouTubeVideo>) -> Unit
) {
    var playlistTitle by remember { mutableStateOf("") }
    var channelTitle by remember { mutableStateOf("Vinay Kumar AVJ") }
    var descriptionInput by remember { mutableStateOf("") }
    var videoLinksText by remember { mutableStateOf("") }
    val selectedExistingVideoIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "नई प्लेलिस्ट बनाएं (Create Named Playlist)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "यहाँ आप एक नई प्लेलिस्ट का नाम दर्ज करें और वीडियो लिंक जोड़कर नया संग्रह तैयार करें।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    OutlinedTextField(
                        value = playlistTitle,
                        onValueChange = { playlistTitle = it },
                        label = { Text("प्लेलिस्ट का नाम *") },
                        placeholder = { Text("उदा. प्रार्थना संदेश श्रृंखला 2026") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = channelTitle,
                        onValueChange = { channelTitle = it },
                        label = { Text("मंत्रालय / चैनल का नाम") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("विवरण (Description)") },
                        placeholder = { Text("इस प्लेलिस्ट का संक्षिप्त विवरण...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = videoLinksText,
                        onValueChange = { videoLinksText = it },
                        label = { Text("वीडियो लिंक पेस्ट करें (प्रत्येक लिंक नई पंक्ति में)") },
                        placeholder = { Text("https://youtu.be/abc123\nhttps://www.youtube.com/watch?v=xyz456") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                if (allVideos.isNotEmpty()) {
                    item {
                        Text(
                            text = "मौजूदा वीडियो में से भी जोड़ें (${selectedExistingVideoIds.size} चयनित):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    items(allVideos.take(6), key = { it.id }) { vid ->
                        val isSelected = selectedExistingVideoIds.contains(vid.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GoldWarm.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    if (isSelected) selectedExistingVideoIds.remove(vid.id)
                                    else selectedExistingVideoIds.add(vid.id)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedExistingVideoIds.add(vid.id)
                                    else selectedExistingVideoIds.remove(vid.id)
                                }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = vid.title,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (playlistTitle.isBlank()) return@Button

                    val parsedNewVideos = mutableListOf<YouTubeVideo>()
                    val collectedUrls = mutableListOf<String>()
                    val lines = videoLinksText.lines().map { it.trim() }.filter { it.isNotBlank() }
                    lines.forEachIndexed { index, line ->
                        val parsed = VideoUrlParser.parse(line)
                        val vidId = if (parsed.videoId.isNotBlank()) parsed.videoId else "custom_${System.currentTimeMillis()}_$index"
                        collectedUrls.add(line)
                        parsedNewVideos.add(
                            YouTubeVideo(
                                id = vidId,
                                title = "$playlistTitle - भाग ${index + 1}",
                                channelId = "custom_channel",
                                channelTitle = channelTitle,
                                thumbnailUrl = if (parsed.thumbnailUrl.isNotBlank()) parsed.thumbnailUrl else "https://img.youtube.com/vi/$vidId/hqdefault.jpg",
                                publishedAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                publishedTimestamp = System.currentTimeMillis(),
                                description = "प्लेलिस्ट: $playlistTitle",
                                videoUrl = line,
                                isRemote = true
                            )
                        )
                    }

                    val existingSelectedVideos = allVideos.filter { it.id in selectedExistingVideoIds }
                    existingSelectedVideos.forEach {
                        if (!collectedUrls.contains(it.videoUrl)) {
                            collectedUrls.add(it.videoUrl)
                        }
                    }

                    val playlistId = "custom_pl_" + System.currentTimeMillis()
                    val firstThumb = parsedNewVideos.firstOrNull()?.thumbnailUrl
                        ?: existingSelectedVideos.firstOrNull()?.thumbnailUrl

                    val playlist = YouTubePlaylist(
                        id = playlistId,
                        title = playlistTitle,
                        channelTitle = channelTitle,
                        playlistUrl = "https://youtube.com/playlist?list=$playlistId",
                        videoCountEstimate = collectedUrls.size.coerceAtLeast(1),
                        thumbnailUrl = firstThumb,
                        description = descriptionInput,
                        videoUrls = collectedUrls,
                        videoIds = parsedNewVideos.map { it.id } + selectedExistingVideoIds,
                        isCustom = true
                    )

                    onSave(playlist, parsedNewVideos)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black)
            ) {
                Text("प्लेलिस्ट बनाएं", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun AdminQuickAccessCustomizerSection(
    viewModel: MainViewModel,
    config: VideoQuickAccessConfig
) {
    val context = LocalContext.current
    var isBarVisible by remember(config) { mutableStateOf(config.isBarVisible) }
    var itemsList by remember(config) {
        mutableStateOf(config.items.sortedWith(compareByDescending<VideoQuickAccessItem> { it.isPinned }.thenBy { it.order }))
    }

    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagLabel by remember { mutableStateOf("") }
    var newTagFilterType by remember { mutableStateOf("KEYWORD") }
    var newTagFilterValue by remember { mutableStateOf("") }

    val saveChanges = {
        val updated = VideoQuickAccessConfig(
            isBarVisible = isBarVisible,
            items = itemsList.mapIndexed { idx, item -> item.copy(order = idx) }
        )
        viewModel.updateVideoQuickAccessConfig(updated) { success ->
            if (success) {
                Toast.makeText(context, "क्विक एक्सेस टैग्स सेटिंग्स सहेजी गईं! ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "सेटिंग्स सहेजने में विफल!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Toggle Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ वीडियो स्क्रीन पर क्विक एक्सेस बार",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "उपयोगकर्ताओं के लिए ऊपर श्रेणी व सर्च पिल्स प्रदर्शित करें",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isBarVisible,
                            onCheckedChange = {
                                isBarVisible = it
                                saveChanges()
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("सक्रिय टैग्स सूची (${itemsList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Button(
                            onClick = {
                                newTagLabel = ""
                                newTagFilterType = "KEYWORD"
                                newTagFilterValue = ""
                                showAddTagDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("नया टैग जोड़ें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // List of Tags
        items(itemsList, key = { it.id }) { tag ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (tag.isPinned) GoldWarm.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(0.5.dp, if (tag.isPinned) GoldWarm else Color.Transparent)
                        ) {
                            Text(
                                text = tag.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tag.isPinned) GoldWarm else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "फ़िल्टर: ${tag.filterType} • ${tag.filterValue.ifBlank { "All" }}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle Visible
                        IconButton(
                            onClick = {
                                itemsList = itemsList.map { if (it.id == tag.id) it.copy(isVisible = !it.isVisible) else it }
                                saveChanges()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (tag.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Visibility",
                                tint = if (tag.isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Toggle Pin
                        IconButton(
                            onClick = {
                                itemsList = itemsList.map { if (it.id == tag.id) it.copy(isPinned = !it.isPinned) else it }
                                saveChanges()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (tag.isPinned) Icons.Default.PushPin else Icons.Default.PinInvoke,
                                contentDescription = "Pin",
                                tint = if (tag.isPinned) GoldWarm else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Delete
                        IconButton(
                            onClick = {
                                itemsList = itemsList.filter { it.id != tag.id }
                                saveChanges()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("नया क्विक एक्सेस टैग जोड़ें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTagLabel,
                        onValueChange = { newTagLabel = it },
                        label = { Text("टैग का नाम *") },
                        placeholder = { Text("उदा. Sunday Service, आराधना...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = newTagFilterValue,
                        onValueChange = { newTagFilterValue = it },
                        label = { Text("फ़िल्टर कीवर्ड / चैनल ID") },
                        placeholder = { Text("उदा. worship, sermon, vinay...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagLabel.isBlank()) return@Button
                        val newTag = VideoQuickAccessItem(
                            id = "tag_" + System.currentTimeMillis(),
                            label = newTagLabel.trim(),
                            filterType = "KEYWORD",
                            filterValue = newTagFilterValue.trim(),
                            isVisible = true,
                            isPinned = false,
                            order = itemsList.size
                        )
                        itemsList = itemsList + newTag
                        saveChanges()
                        showAddTagDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black)
                ) {
                    Text("जोड़ें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
