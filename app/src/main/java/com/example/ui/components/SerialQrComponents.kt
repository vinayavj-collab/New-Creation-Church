package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.QrCodeHelper

private val GoldWarm = Color(0xFFD4AF37)
private val DeepEmerald = Color(0xFF0F5132)

/**
 * Embedded QR Code Card showing QR preview and quick actions.
 */
@Composable
fun SerialQrCard(
    serialNumber: String,
    p2Otp: String? = null,
    roleTitle: String? = null,
    modifier: Modifier = Modifier,
    onExpandFullscreen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val payload = remember(serialNumber, p2Otp, roleTitle) {
        QrCodeHelper.createActivationPayload(serialNumber, p2Otp, roleTitle)
    }

    val qrBitmap = remember(payload) {
        QrCodeHelper.generateQrBitmap(payload, sizePx = 400)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = GoldWarm,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "एक्टिवेशन QR कोड (Scan to Activate)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (onExpandFullscreen != null) {
                    IconButton(
                        onClick = onExpandFullscreen,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "बड़ा करें",
                            tint = GoldWarm,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // QR Code Container
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, GoldWarm),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(170.dp)
                    .clickable { onExpandFullscreen?.invoke() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code for $serialNumber",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "QR लोड हो रहा है...",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = serialNumber,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = GoldWarm,
                letterSpacing = 1.sp
            )

            if (!p2Otp.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "OTP: $p2Otp",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF10B981)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val textToCopy = buildString {
                            append("सीरियल आईडी: $serialNumber")
                            if (!p2Otp.isNullOrBlank()) append("\nP2 OTP: $p2Otp")
                            if (!roleTitle.isNullOrBlank()) append("\nपद: $roleTitle")
                        }
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        Toast.makeText(context, "सीरियल विवरण कॉपी किया गया!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("कॉपी", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        if (qrBitmap != null) {
                            val uri = QrCodeHelper.saveQrBitmapToFile(context, qrBitmap, serialNumber)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                if (uri != null) {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                } else {
                                    type = "text/plain"
                                }
                                val extraText = buildString {
                                    append("✝️ न्यू क्रिएशन चर्च - सदस्य एक्टिवेशन\n\n")
                                    append("🆔 सीरियल आईडी: $serialNumber\n")
                                    if (!p2Otp.isNullOrBlank()) append("🔑 P2 OTP: $p2Otp\n")
                                    if (!roleTitle.isNullOrBlank()) append("👤 पदनाम: $roleTitle\n")
                                    append("\nऐप में 'First-Time Activation' पर जाकर QR कोड स्कैन करें या सीरियल आईडी दर्ज करके प्रोफाइल एक्टिवेट करें।")
                                }
                                putExtra(Intent.EXTRA_TEXT, extraText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "QR कोड और सीरियल साझा करें"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("शेयर करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Fullscreen / Expanded Dialog for showing the QR code clearly so other users can scan it directly.
 */
@Composable
fun SerialQrDisplayDialog(
    serialNumber: String,
    p2Otp: String? = null,
    roleTitle: String? = null,
    remainingSeconds: Long = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val payload = remember(serialNumber, p2Otp, roleTitle) {
        QrCodeHelper.createActivationPayload(serialNumber, p2Otp, roleTitle)
    }

    val qrBitmap = remember(payload) {
        QrCodeHelper.generateQrBitmap(payload, sizePx = 600)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.5.dp, GoldWarm),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GoldWarm.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = GoldWarm,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "सदस्य एक्टिवेशन QR कोड",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "New Member Activation Pass",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "बंद करें",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // High-Contrast QR Code Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, GoldWarm),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(240.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("QR कोड जनरेट हो रहा है...", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Serial ID Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldWarm.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "सीरियल आईडी (Serial ID)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = serialNumber,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = GoldWarm,
                            letterSpacing = 1.2.sp
                        )
                        if (!roleTitle.isNullOrBlank()) {
                            Text(
                                text = "पद: $roleTitle",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (!p2Otp.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("P2 OTP: $p2Otp", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            if (remainingSeconds > 0) {
                                Text(
                                    text = "वैधता: %02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "📱 नया सदस्य अपने ऐप में 'QR कोड स्कैन करें' बटन दबाकर सीधे इस कोड को स्कैन कर सकता है।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val textToCopy = buildString {
                                append("सीरियल आईडी: $serialNumber")
                                if (!p2Otp.isNullOrBlank()) append("\nP2 OTP: $p2Otp")
                                if (!roleTitle.isNullOrBlank()) append("\nपद: $roleTitle")
                            }
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            Toast.makeText(context, "विवरण कॉपी किया गया!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("कॉपी", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (qrBitmap != null) {
                                val uri = QrCodeHelper.saveQrBitmapToFile(context, qrBitmap, serialNumber)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    if (uri != null) {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    } else {
                                        type = "text/plain"
                                    }
                                    val extraText = buildString {
                                        append("✝️ न्यू क्रिएशन चर्च - सदस्य एक्टिवेशन पास\n\n")
                                        append("🆔 सीरियल आईडी: $serialNumber\n")
                                        if (!p2Otp.isNullOrBlank()) append("🔑 P2 OTP: $p2Otp\n")
                                        if (!roleTitle.isNullOrBlank()) append("👤 पदनाम: $roleTitle\n")
                                        append("\nऐप में 'First-Time Activation' पर जाकर QR कोड स्कैन करें या सीरियल आईडी दर्ज करके प्रोफाइल एक्टिवेट करें।")
                                    }
                                    putExtra(Intent.EXTRA_TEXT, extraText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "QR कोड साझा करें"))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("शेयर पास", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * QR Code Scanner / Image Decoder Dialog.
 * Allows members to scan or pick a QR code image to auto-fill their Serial ID and P2 OTP.
 */
@Composable
fun QrScannerDialog(
    onSerialScanned: (serial: String, p2Otp: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isDecoding by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isDecoding = true
            scanError = null
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val decodedText = QrCodeHelper.decodeQrFromBitmap(bitmap)
                        if (!decodedText.isNullOrBlank()) {
                            val (serial, p2) = QrCodeHelper.parseSerialFromQr(decodedText)
                            if (serial.isNotBlank()) {
                                Toast.makeText(context, "✅ QR कोड सफलतापूर्वक स्कैन हुआ!", Toast.LENGTH_SHORT).show()
                                onSerialScanned(serial, p2)
                                onDismiss()
                                return@use
                            }
                        }
                    }
                }
                scanError = "चित्र में कोई मान्य सीरियल QR कोड नहीं मिला। कृपया स्पष्ट QR चित्र चुनें।"
            } catch (e: Exception) {
                scanError = "चित्र पढ़ने में त्रुटि: ${e.localizedMessage}"
            } finally {
                isDecoding = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.5.dp, GoldWarm),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GoldWarm.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = GoldWarm,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "QR कोड स्कैन करें",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Scan Activation QR Code",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "बंद करें")
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Viewfinder / Instructions Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .border(2.dp, GoldWarm, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = GoldWarm,
                                modifier = Modifier.size(90.dp)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "पास्टर या एडमिन द्वारा दिए गए एक्टिवेशन QR कोड की फोटो गैलरी से चुनें या स्कैन करें।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }

                if (scanError != null) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = scanError ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Gallery Button
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldWarm, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isDecoding) "स्कैन हो रहा है..." else "🖼️ गैलरी / स्क्रीनशॉट से QR चुनें",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("हाथ से टाइप करें (Type Manually)", fontSize = 12.sp)
                }
            }
        }
    }
}
