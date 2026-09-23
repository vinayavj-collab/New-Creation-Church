package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.FeaturedBannerItem

@Composable
fun FeaturedBannerCarousel(
    banners: List<FeaturedBannerItem>,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return

    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(banners, key = { it.id }) { banner ->
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .height(140.dp)
                        .clickable {
                            if (banner.actionUrl.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.actionUrl)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (banner.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = banner.imageUrl,
                                contentDescription = banner.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.secondaryContainer
                                            )
                                        )
                                    )
                            )
                        }

                        // Gradient shadow for readable text overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                        )

                        if (banner.badgeText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = banner.badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            if (banner.title.isNotBlank()) {
                                Text(
                                    text = banner.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1
                                )
                            }
                            if (banner.subtitle.isNotBlank()) {
                                Text(
                                    text = banner.subtitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.85f)
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
