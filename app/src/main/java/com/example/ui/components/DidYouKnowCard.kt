package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.BibleFactItem
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.NavyPrimary

@Composable
fun DidYouKnowFactCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fact = remember { BibleFactItem.getFactForToday() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header row with Icon and Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Did You Know Icon",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "💡 क्या आप जानते हैं?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "प्रतिदिन का रोचक बाइबिल तथ्य",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = fact.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fact Text in Hindi
                Text(
                    text = fact.factHindi,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // English translation subtitle
                Text(
                    text = fact.factEnglish,
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Spacer(modifier = Modifier.height(8.dp))

                // Source & Share row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Source",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "स्रोत: ${fact.source}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "💡 क्या आप जानते हैं? (बाइबिल तथ्य):\n\n\"${fact.factHindi}\"\n\nस्रोत: ${fact.source}\n\n- New Creation App से साझा किया गया"
                                )
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "तथ्य साझा करें"))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Fact",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
