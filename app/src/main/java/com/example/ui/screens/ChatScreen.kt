package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentAdmin by viewModel.currentAdmin.collectAsState()

    val assignedRoomId = "general_congregation"
    var selectedRoomId by remember(assignedRoomId) { mutableStateOf(assignedRoomId) }

    val isMasterOrAdmin = currentAdmin != null || com.example.util.ProfileManager.isVinayProfile()
    var isAnnouncementMode by remember { mutableStateOf(false) }
    var isRoomLocked by remember { mutableStateOf(false) }

    var messageInput by remember { mutableStateOf("") }
    var messagesList by remember { mutableStateOf(listOf<ChatMessage>()) }

    val currentUserId = userProfile?.deviceId.takeIf { !it.isNullOrBlank() } ?: userProfile?.phoneNumber.takeIf { !it.isNullOrBlank() } ?: "my_id"
    val currentUserName = userProfile?.displayName.takeIf { !it.isNullOrBlank() } ?: "विश्वासी (Believer)"
    val currentUserRole = userProfile?.role ?: "Member"

    LaunchedEffect(selectedRoomId) {
        messagesList = listOf(
            ChatMessage(
                id = "msg_1",
                roomId = selectedRoomId,
                senderId = "pastor_01",
                senderName = "पास्टर जॉन (Pastor John)",
                senderDesignation = "Senior Pastor",
                text = "प्रभु में प्रिय भाई-बहनों, इस सप्ताह की प्रार्थना सभा में आप सबका स्वागत है। कृपया अपने प्रार्थना विषय साझा करें।",
                timestamp = System.currentTimeMillis() - 3600000L,
                isAnnouncement = true
            ),
            ChatMessage(
                id = "msg_2",
                roomId = selectedRoomId,
                senderId = currentUserId,
                senderName = currentUserName,
                senderDesignation = currentUserRole,
                text = "जय मसीह की! हमारे परिवार के लिए विशेष प्रार्थना करें।",
                timestamp = System.currentTimeMillis() - 1800000L
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "💬 कलीसिया संगति चैट (Scoped Group)",
                            fontWeight = FontWeight.Bold,
                            color = GoldWarm,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "कक्ष (Room): $selectedRoomId • (केवल समूह चैट - No P2P)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isRoomLocked) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "यह चैट कक्ष मास्टर एडमिन द्वारा लॉक कर दिया गया है।",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (isAnnouncementMode && !isMasterOrAdmin) {
                Surface(
                    color = GoldWarm.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = GoldWarm)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "📢 घोषणा मोड सक्रिय: केवल पास्टर/एडमिन संदेश भेज सकते हैं।",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messagesList) { msg ->
                    val isMe = msg.senderId == currentUserId
                    val canDelete = isMasterOrAdmin || isMe

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isAnnouncement) GoldWarm.copy(alpha = 0.15f)
                            else if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (msg.isAnnouncement) BorderStroke(1.dp, GoldWarm) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(GoldWarm),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = msg.senderName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = msg.senderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = msg.senderDesignation,
                                            fontSize = 10.sp,
                                            color = GoldWarm
                                        )
                                    }
                                }
                                if (canDelete) {
                                    IconButton(
                                        onClick = {
                                            messagesList = messagesList.filterNot { it.id == msg.id }
                                            Toast.makeText(context, "संदेश हटाया गया", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = msg.text,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (!isRoomLocked && (!isAnnouncementMode || isMasterOrAdmin)) {
                Surface(
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            placeholder = { Text("संदेश टाइप करें (Group Message)...") },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (messageInput.isNotBlank()) {
                                    val newMsg = ChatMessage(
                                        id = "msg_${System.currentTimeMillis()}",
                                        roomId = selectedRoomId,
                                        senderId = currentUserId,
                                        senderName = currentUserName,
                                        senderDesignation = currentUserRole,
                                        text = messageInput.trim(),
                                        timestamp = System.currentTimeMillis()
                                    )
                                    messagesList = messagesList + newMsg
                                    messageInput = ""
                                    Toast.makeText(context, "संदेश भेजा गया", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GoldWarm)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
