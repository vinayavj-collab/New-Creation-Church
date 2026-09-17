package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NotificationEntity
import com.example.ui.theme.GoldWarm
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifications by viewModel.allNotifications.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Requirement 5: Automatically update database to mark all as read upon opening screen, clearing red badge
    LaunchedEffect(Unit) {
        viewModel.markAllNotificationsAsRead()
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("सूचनाएं साफ़ करें (Clear All)") },
            text = { Text("क्या आप वास्तव में सभी सहेजी गई सूचनाओं को हटाना चाहते हैं?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllNotifications()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हाँ, हटाएं (Delete All)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("रद्द करें (Cancel)")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "सूचनाएं (Notifications)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (notifications.isEmpty()) "कोई सूचना नहीं" else "${notifications.size} सूचनाएं सुरक्षित हैं",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("notification_history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldWarm
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_all_notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All Notifications",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (notifications.isEmpty()) {
                EmptyNotificationsView(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notification_history_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = notifications,
                        key = { it.id }
                    ) { item ->
                        NotificationItemCard(
                            notification = item,
                            onDelete = { viewModel.deleteNotification(item.id) },
                            onOpenLink = { url ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: NotificationEntity,
    onDelete: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTime = remember(notification.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(notification.timestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("notification_item_${notification.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(GoldWarm.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (notification.type) {
                                "admin_notice", "admin" -> Icons.Default.Campaign
                                else -> Icons.Default.Notifications
                            },
                            contentDescription = null,
                            tint = GoldWarm,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (notification.type == "admin_notice") "प्रशासनिक सूचना" else "घोषणा",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GoldWarm,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $formattedTime",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete Notification",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            if (notification.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                )
            }

            if (!notification.linkUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onOpenLink(notification.linkUrl) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "लिंक खोलें (Open Link)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(GoldWarm.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = GoldWarm,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "कोई सूचना नहीं है",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "यहाँ आपको प्रशासनिक संदेश, महत्वपूर्ण घोषणाएं और आने वाले अपडेट की सूचनाएं प्राप्त होंगी।",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
