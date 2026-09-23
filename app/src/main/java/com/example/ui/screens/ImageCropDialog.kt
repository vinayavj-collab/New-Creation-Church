package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldWarm
import java.io.InputStream

@Composable
fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(imageUri) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bmp = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bmp != null) {
                val maxDim = 1024
                val width = bmp.width
                val height = bmp.height
                val scaledBmp = if (width > maxDim || height > maxDim) {
                    val ratio = maxDim.toFloat() / maxOf(width, height)
                    Bitmap.createScaledBitmap(bmp, (width * ratio).toInt(), (height * ratio).toInt(), true)
                } else {
                    bmp
                }
                originalBitmap = scaledBmp
            }
        } catch (e: Exception) {
            // handle error
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = GoldWarm)
                Spacer(Modifier.width(8.dp))
                Text("प्रोफ़ाइल फोटो क्रॉप व रिसाइज करें", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val bmp = originalBitmap
                if (bmp == null) {
                    CircularProgressIndicator(color = GoldWarm)
                    Spacer(Modifier.height(12.dp))
                    Text("फोटो लोड हो रही है...", fontSize = 12.sp)
                } else {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val matrix = Matrix().apply {
                            postRotate(rotationAngle)
                        }
                        val rotatedBmp = remember(bmp, rotationAngle) {
                            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                        }

                        Image(
                            bitmap = rotatedBmp.asImageBitmap(),
                            contentDescription = "Crop preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val boxSize = minOf(canvasWidth, canvasHeight) * 0.75f
                            val left = (canvasWidth - boxSize) / 2f
                            val top = (canvasHeight - boxSize) / 2f

                            drawRect(color = Color.Black.copy(alpha = 0.5f))
                            drawRect(
                                color = Color.Transparent,
                                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(boxSize, boxSize)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Rotate", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("घुमाएं (Rotate)", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                                rotationAngle = 0f
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("रीसेट", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bmp = originalBitmap
                    if (bmp != null) {
                        try {
                            val minDim = minOf(bmp.width, bmp.height)
                            val startX = (bmp.width - minDim) / 2
                            val startY = (bmp.height - minDim) / 2
                            val cropped = Bitmap.createBitmap(bmp, startX, startY, minDim, minDim)
                            val finalBmp = Bitmap.createScaledBitmap(cropped, 400, 400, true)
                            onCropped(finalBmp)
                        } catch (e: Exception) {
                            onCropped(bmp)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldWarm),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("क्रॉप व सेव करें", fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
