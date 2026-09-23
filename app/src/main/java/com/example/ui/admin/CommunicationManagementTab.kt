package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminUser
import com.example.data.model.UserSettings
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel

@Composable
fun CommunicationManagementTabContent(
    viewModel: MainViewModel,
    currentAdmin: AdminUser?,
    settings: UserSettings
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isChatEnabled by remember(settings.isChatEnabled) { mutableStateOf(settings.isChatEnabled) }
    var chatAllowOnlyVerified by remember(settings.chatAllowOnlyVerified) { mutableStateOf(settings.chatAllowOnlyVerified) }
    
    var whitelistedInput by remember { mutableStateOf("") }
    var whitelistedList by remember(settings.chatWhitelistedUserIds) { mutableStateOf(settings.chatWhitelistedUserIds) }

    var broadcastText by remember { mutableStateOf("") }
    var broadcastTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "कम्यूनिकेशन मैनेजमेंट (Chat & Broadcast)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GoldWarm
                        )
                        Text(
                            text = "मास्टर एडमिन सुपर-कंट्रोल्स: एपफेमेरल चैट, ग्लोबल किल-स्विच, वेरिफिकेशन गेट और ब्रॉडकास्ट।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Global Kill Switch & Verification Gate
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "🌐 वैश्विक चैट सुरक्षा और नियंत्रण (Global Access Rules)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ग्लोबल चैट सिस्टम (Global Chat Kill Switch)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("चालू होने पर ही सभी उपयोगकर्ताओं के लिए 'चैट' टैब दिखाई देगा।", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isChatEnabled,
                        onCheckedChange = { isChatEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldWarm, checkedTrackColor = GoldWarm.copy(alpha = 0.5f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("केवल वेरिफ़ाइड उपयोगकर्ताओं की अनुमति", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("अनवेरिफ़ाइड उपयोगकर्ता चैट नहीं देख सकेंगे।", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = chatAllowOnlyVerified,
                        onCheckedChange = { chatAllowOnlyVerified = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldWarm, checkedTrackColor = GoldWarm.copy(alpha = 0.5f))
                    )
                }

                Button(
                    onClick = {
                        viewModel.updateChatSettings(
                            isChatEnabled = isChatEnabled,
                            chatAllowOnlyVerified = chatAllowOnlyVerified,
                            chatWhitelistedUserIds = whitelistedList,
                            chatAllowedRoles = settings.chatAllowedRoles
                        )
                        Toast.makeText(context, "कम्यूनिकेशन सेटिंग्स सुरक्षित रूप से सहेज ली गईं!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("सेटिंग्स सहेजें (Save Chat Config)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Whitelist Management
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🛡️ विशेष छूट / व्हाइटलिस्ट (Whitelisted User IDs)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "जिन उपयोगकर्ताओं की यूज़र आईडी यहाँ जोड़ी जाएगी, वे वेरिफिकेशन या सामान्य प्रतिबंधों के बिना चैट एक्सेस कर सकेंगे।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = whitelistedInput,
                        onValueChange = { whitelistedInput = it },
                        label = { Text("User ID दर्ज करें") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (whitelistedInput.isNotBlank() && !whitelistedList.contains(whitelistedInput.trim())) {
                                whitelistedList = whitelistedList + whitelistedInput.trim()
                                whitelistedInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black)
                    ) {
                        Text("जोड़ें")
                    }
                }

                if (whitelistedList.isNotEmpty()) {
                    Text("वर्तमान व्हाइटलिस्ट:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    whitelistedList.forEach { uid ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(uid, fontSize = 12.sp)
                            IconButton(onClick = { whitelistedList = whitelistedList.filter { it != uid } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Pastor's Announcement Mode / Global Broadcast Sender
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "📢 पास्टर / मास्टर एडमिन ब्रॉडकास्ट (Announcement Mode)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GoldWarm
                )
                Text(
                    text = "यह संदेश सभी चैट कमरों के शीर्ष पर विशेष हाइलाइटेड घोषणा के रूप में प्रसारित किया जाएगा।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = broadcastTitle,
                    onValueChange = { broadcastTitle = it },
                    label = { Text("घोषणा शीर्षक (Title)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = broadcastText,
                    onValueChange = { broadcastText = it },
                    label = { Text("घोषणा संदेश (Message Body)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Button(
                    onClick = {
                        if (broadcastTitle.isNotBlank() && broadcastText.isNotBlank()) {
                            // Send broadcast via viewModel announcement method
                            viewModel.postSpecialAnnouncement(broadcastTitle, broadcastText) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, "ब्रॉडकास्ट सफलतापूर्वक प्रसारित किया गया!", Toast.LENGTH_SHORT).show()
                                    broadcastTitle = ""
                                    broadcastText = ""
                                } else {
                                    Toast.makeText(context, "त्रुटि: $msg", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "कृपया शीर्षक और संदेश दोनों भरें", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("वैश्विक घोषणा प्रसारित करें (Broadcast Now)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
