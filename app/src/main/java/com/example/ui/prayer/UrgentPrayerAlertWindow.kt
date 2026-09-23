package com.example.ui.prayer

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PrayerRequestItem
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UrgentPrayerAlertWindow(
    item: PrayerRequestItem,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOpenPrayerList: () -> Unit = {}
) {
    val context = LocalContext.current
    var hasPrayed by remember { mutableStateOf(false) }

    val formattedDate = remember(item.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Emergency Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "🚨 तत्काल प्रार्थना अलर्ट",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "अति-आवश्यक • तुरंत प्रार्थना करें",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "बंद करें",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Body content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Serial & Identity Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "क्रमांक #${item.serialNumber}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Requester Info Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                // User Role Badge
                                val roleLabel = when (item.userRole) {
                                    "पास्टर" -> "✝️ पास्टर"
                                    "अन्य" -> "👥 अन्य"
                                    else -> "👤 विश्वासी"
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = roleLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (item.city.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "स्थान: ${item.city}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Urgent Request Notice & Full Message
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "⚠️ तत्काल प्रार्थना की ज़रूरत (Emergency Request):",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.requestText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    AnimatedVisibility(visible = hasPrayed) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "आमीन! आपकी प्रार्थना दर्ज हुई। प्रभु अद्भुत चंगाई व सहायता दें! 🙏",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Primary Pray Button
                    Button(
                        onClick = {
                            viewModel.incrementPrayingCountWithLimit(context, item.id) { success, msg ->
                                if (success) {
                                    hasPrayed = true
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasPrayed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasPrayed) "प्रार्थना की गई 🙏 (${item.prayingCount + 1})" else "मैंने अभी प्रार्थना की 🙏 (${item.prayingCount})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Share and Other options Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val shareText = buildString {
                                    append("🚨 अति-आवश्यक / तत्काल प्रार्थना अलर्ट 🚨\n\n")
                                    append("📌 क्रमांक: #${item.serialNumber}\n")
                                    append("👤 नाम: ${item.name} (${item.userRole})\n")
                                    if (item.city.isNotBlank()) append("📍 स्थान: ${item.city}\n")
                                    append("\n❓ तत्काल प्रार्थना निवेदन:\n${item.requestText}\n\n")
                                    append("📖 \"धर्मी जन की प्रार्थना के प्रभाव से बहुत कुछ हो सकता है।\" (याकूब 5:16)\n")
                                    append("कृपया तुरंत प्रभु के चरणों में मध्यस्थता करें! 🙏")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "तत्काल प्रार्थना शेयर करें"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("शेयर करें 📲", style = MaterialTheme.typography.labelSmall)
                        }

                        FilledTonalButton(
                            onClick = {
                                onDismiss()
                                onOpenPrayerList()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("सभी निवेदन 📜", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Close Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "स्वीकार किया / विंडो बंद करें",
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }
                }
            }
        }
    }
}
