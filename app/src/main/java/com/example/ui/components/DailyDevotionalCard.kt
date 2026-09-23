package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.bible.model.DailyDevotionalItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

@Composable
fun DailyDevotionalCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var devotional by remember { mutableStateOf(DailyDevotionalItem.getDevotionalForToday()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch from Firestore online if available, fallback to offline
    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("daily_devotionals")
                .whereEqualTo("dayOfYear", Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val doc = snapshot.documents[0]
                val remote = doc.toObject(DailyDevotionalItem::class.java)
                if (remote != null) {
                    devotional = remote
                }
            }
        } catch (e: Exception) {
            // Offline fallback already loaded via remember
        } finally {
            isLoading = false
        }
    }

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
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header row
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
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = "Devotional Icon",
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "📖 दैनिक मनन (Daily Devotional)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = devotional.titleHindi.ifEmpty { "आज का विशेष आत्मिक संदेश" },
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
                            text = devotional.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Verse reference & Text box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = "Verse Quote",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = devotional.verseReference,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${devotional.verseTextHindi}\"",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Devotional thought
                Text(
                    text = devotional.devotionalThoughtHindi,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                // Author & Share row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "लेखक/स्रोत: ${devotional.author}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "📖 दैनिक मनन (${devotional.titleHindi}):\n\n\"${devotional.verseTextHindi}\"\n(${devotional.verseReference})\n\nचिंतन: ${devotional.devotionalThoughtHindi}\n\n- New Creation App से साझा किया गया"
                                )
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "दैनिक मनन साझा करें"))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Devotional",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
