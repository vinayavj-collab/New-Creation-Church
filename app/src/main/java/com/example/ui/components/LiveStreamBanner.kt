package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.LiveStreamInfo

@Composable
fun LiveStreamBanner(
    liveStreamInfo: LiveStreamInfo,
    modifier: Modifier = Modifier
) {
    if (!liveStreamInfo.isLive || liveStreamInfo.url.isBlank()) return

    val context = LocalContext.current

    // Pulsing animation for the red LIVE dot
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(liveStreamInfo.url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFB71C1C), // Deep crimson red
                            Color(0xFFE53935), // Vibrant red
                            Color(0xFF880E4F)  // Deep rose
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Pulsing Red Dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = alpha))
                        )

                        Text(
                            text = "LIVE • अभी लाइव प्रसारण",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )

                        if (liveStreamInfo.platform.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.3f),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    text = liveStreamInfo.platform.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = liveStreamInfo.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1
                    )

                    if (liveStreamInfo.subtitle.isNotBlank()) {
                        Text(
                            text = liveStreamInfo.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f)
                            ),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(liveStreamInfo.url)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFB71C1C)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "देखें",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
