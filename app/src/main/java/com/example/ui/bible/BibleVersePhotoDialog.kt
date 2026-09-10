package com.example.ui.bible

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.bible.model.BibleVerse

data class PhotoThemeBackground(
    val id: String,
    val title: String,
    val imageUrl: String,
    val gradientColors: List<Color>,
    val textColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleVersePhotoDialog(
    verse: BibleVerse,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val photoThemes = remember {
        listOf(
            PhotoThemeBackground(
                id = "sunrise",
                title = "Sunrise Grace",
                imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80",
                gradientColors = listOf(Color(0xFF0F172A).copy(alpha = 0.6f), Color(0xFF1E293B).copy(alpha = 0.8f)),
                textColor = Color.White
            ),
            PhotoThemeBackground(
                id = "mountains",
                title = "Mountain Peace",
                imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=800&q=80",
                gradientColors = listOf(Color(0xFF064E3B).copy(alpha = 0.7f), Color(0xFF022C22).copy(alpha = 0.85f)),
                textColor = Color.White
            ),
            PhotoThemeBackground(
                id = "sky",
                title = "Heavenly Blue",
                imageUrl = "https://images.unsplash.com/photo-1517824806704-9040b037703b?auto=format&fit=crop&w=800&q=80",
                gradientColors = listOf(Color(0xFF1E3A8A).copy(alpha = 0.65f), Color(0xFF172554).copy(alpha = 0.85f)),
                textColor = Color.White
            ),
            PhotoThemeBackground(
                id = "sunset",
                title = "Golden Sunset",
                imageUrl = "https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?auto=format&fit=crop&w=800&q=80",
                gradientColors = listOf(Color(0xFF7C2D12).copy(alpha = 0.65f), Color(0xFF431407).copy(alpha = 0.85f)),
                textColor = Color(0xFFFEF3C7)
            ),
            PhotoThemeBackground(
                id = "calm_water",
                title = "Still Waters",
                imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=800&q=80",
                gradientColors = listOf(Color(0xFF134E4A).copy(alpha = 0.65f), Color(0xFF042F2E).copy(alpha = 0.85f)),
                textColor = Color.White
            ),
            PhotoThemeBackground(
                id = "minimal_dark",
                title = "Minimal Dark",
                imageUrl = "",
                gradientColors = listOf(Color(0xFF18181B), Color(0xFF09090B)),
                textColor = Color(0xFFF4F4F5)
            ),
            PhotoThemeBackground(
                id = "royal_purple",
                title = "Royal Grace",
                imageUrl = "",
                gradientColors = listOf(Color(0xFF581C87), Color(0xFF3B0764)),
                textColor = Color(0xFFFAF5FF)
            )
        )
    }

    var selectedTheme by remember { mutableStateOf(photoThemes[0]) }
    var textAlign by remember { mutableStateOf(TextAlign.Center) }
    var fontSize by remember { mutableFloatStateOf(18f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "फोटो के साथ वचन (Verse with Photo)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Photo Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(selectedTheme.gradientColors))
                ) {
                    if (selectedTheme.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = selectedTheme.imageUrl,
                            contentDescription = "Background Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.45f),
                                            Color.Black.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )
                    }

                    // Verse Text Content on Photo
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = when (textAlign) {
                            TextAlign.Center -> Alignment.CenterHorizontally
                            TextAlign.End -> Alignment.End
                            else -> Alignment.Start
                        },
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = selectedTheme.textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = verse.text,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = fontSize.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = selectedTheme.textColor,
                                textAlign = textAlign,
                                lineHeight = (fontSize * 1.4f).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "— ${verse.bookName} ${verse.chapter}:${verse.verseNumber}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = selectedTheme.textColor,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Photo Theme Selector Carousel
                Text(
                    text = "बैकग्राउंड फ़ोटो चुनें (Select Background):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    photoThemes.forEach { theme ->
                        val isSelected = theme.id == selectedTheme.id
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .width(90.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTheme = theme }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(theme.gradientColors)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = theme.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Sharing & Saving Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                fontSize = (fontSize - 2).coerceAtLeast(14f)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "Font Smaller")
                        }
                        IconButton(
                            onClick = {
                                fontSize = (fontSize + 2).coerceAtMost(26f)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "Font Larger")
                        }
                    }

                    Button(
                        onClick = {
                            val shareText = "✝ \"${verse.text}\"\n\n— ${verse.bookName} ${verse.chapter}:${verse.verseNumber}\n\nसच्चा मसीही जीवन (Hindi Bible App)"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "वचन शेयर करें"))
                            Toast.makeText(context, "वचन साझा किया जा रहा है...", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("फोटो वचन शेयर करें")
                    }
                }
            }
        }
    }
}
