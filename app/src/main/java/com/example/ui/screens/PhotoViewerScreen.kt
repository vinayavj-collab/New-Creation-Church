package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.GalleryPhoto
import com.example.ui.components.SourceBadge
import com.example.util.BloggerImageUtils
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun PhotoViewerScreen(
    photos: List<GalleryPhoto>,
    initialIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)),
        pageCount = { photos.size }
    )

    val currentPhoto = photos.getOrNull(pagerState.currentPage)

    // Swipe up/down dismissal offset
    val dismissOffsetY = remember { Animatable(0f) }

    val sharePhoto = {
        if (currentPhoto != null) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "${currentPhoto.postTitle}\n${currentPhoto.imageUrl}")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Photo"))
        }
    }

    val openOriginal = {
        if (currentPhoto != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentPhoto.imageUrl))
                context.startActivity(intent)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (photos.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = dismissOffsetY.value
                        val progress = (abs(dismissOffsetY.value) / 600f).coerceIn(0f, 1f)
                        alpha = 1f - (progress * 0.5f)
                    }
            ) { page ->
                val photo = photos[page]
                ZoomableImageWithSwipeDismiss(
                    photo = photo,
                    onDismiss = onBack,
                    onDragY = { dy ->
                        coroutineScope.launch {
                            dismissOffsetY.snapTo(dismissOffsetY.value + dy)
                        }
                    },
                    onReleaseY = {
                        coroutineScope.launch {
                            if (abs(dismissOffsetY.value) > 180f) {
                                onBack()
                            } else {
                                dismissOffsetY.animateTo(0f, tween(200))
                            }
                        }
                    }
                )
            }
        }

        // Top Navigation & Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "${pagerState.currentPage + 1} / ${photos.size}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Row {
                IconButton(onClick = sharePhoto) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
                IconButton(onClick = openOriginal) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = "Open Original",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Info Bar
        if (currentPhoto != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SourceBadge(source = currentPhoto.source)
                        Text(
                            text = currentPhoto.publishedDate,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentPhoto.postTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ZoomableImageWithSwipeDismiss(
    photo: GalleryPhoto,
    onDismiss: () -> Unit,
    onDragY: (Float) -> Unit,
    onReleaseY: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(scale) {
                if (scale > 1.05f) {
                    // When zoomed in, allow panning and zooming
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                } else {
                    // When not zoomed, allow pinch to zoom AND vertical drag to dismiss
                    detectDragGestures(
                        onDragEnd = { onReleaseY() },
                        onDragCancel = { onReleaseY() },
                        onDrag = { change, dragAmount ->
                            if (abs(dragAmount.y) > abs(dragAmount.x) * 0.8f) {
                                change.consume()
                                onDragY(dragAmount.y)
                            }
                        }
                    )
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f) {
                        scale = (scale * zoom).coerceIn(1f, 4f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(BloggerImageUtils.sanitizeUrl(photo.imageUrl))
                .crossfade(true)
                .build(),
            contentDescription = photo.postTitle,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

