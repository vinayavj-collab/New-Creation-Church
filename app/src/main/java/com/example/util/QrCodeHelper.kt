package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.EnumMap

object QrCodeHelper {

    /**
     * Generates standard QR Code Bitmap from given text.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 2)
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            }
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix.get(x, y)) fgColor else bgColor
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates structured activation payload containing Serial ID and optional P2 OTP.
     */
    fun createActivationPayload(serialNumber: String, p2Otp: String? = null, role: String? = null): String {
        return try {
            val json = JSONObject().apply {
                put("type", "ncck_activation")
                put("serial", serialNumber.trim().uppercase())
                if (!p2Otp.isNullOrBlank()) {
                    put("p2", p2Otp.trim())
                }
                if (!role.isNullOrBlank()) {
                    put("role", role.trim())
                }
            }
            json.toString()
        } catch (_: Exception) {
            serialNumber.trim().uppercase()
        }
    }

    /**
     * Parses scanned text from QR code. Returns Pair(serialNumber, p2Otp?).
     */
    fun parseSerialFromQr(scannedText: String): Pair<String, String?> {
        val raw = scannedText.trim()
        if (raw.isBlank()) return Pair("", null)

        // Try JSON parsing (handles both "serial"/"p2" and "sn"/"otp")
        if (raw.startsWith("{") && raw.endsWith("}")) {
            try {
                val json = JSONObject(raw)
                val serial = (json.optString("serial", "").ifBlank { json.optString("sn", "") }).trim().uppercase()
                val p2 = (json.optString("p2", "").ifBlank { json.optString("otp", "") }).trim().takeIf { it.isNotBlank() }
                if (serial.isNotBlank() || p2 != null) {
                    return Pair(serial, p2)
                }
            } catch (_: Exception) {}
        }

        // Try URL / deep link parsing (e.g. ncck://activate?serial=ADMIN1&p2=5678 or sn=ADMIN1&otp=5678)
        if (raw.contains("serial=", ignoreCase = true) || raw.contains("sn=", ignoreCase = true)) {
            try {
                val uri = Uri.parse(raw)
                val serial = (uri.getQueryParameter("serial") ?: uri.getQueryParameter("sn"))?.trim()?.uppercase().orEmpty()
                val p2 = (uri.getQueryParameter("p2") ?: uri.getQueryParameter("otp"))?.trim()
                if (serial.isNotBlank() || !p2.isNullOrBlank()) {
                    return Pair(serial, p2)
                }
            } catch (_: Exception) {}
        }

        // Try delimiter separation (e.g. ADMIN1:123456 or ADMIN1|123456 or ADMIN1,123456)
        for (sep in listOf(":", "|", ",", ";")) {
            if (raw.contains(sep)) {
                val parts = raw.split(sep).map { it.trim() }
                if (parts.size >= 2 && parts[0].isNotBlank()) {
                    return Pair(parts[0].uppercase(), parts[1].takeIf { it.isNotBlank() })
                }
            }
        }

        // Direct serial code format (e.g., ADMIN1 or NCCK-1234)
        return Pair(raw.uppercase(), null)
    }

    /**
     * Decodes a QR code directly from a Bitmap.
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates structured onboarding payload for zero-burden member addition.
     * Encodes: { "type": "MEMBER_ONBOARDING", "sn": "NCC1" }
     */
    fun createMemberOnboardingPayload(sn: String): String {
        return try {
            val json = JSONObject().apply {
                put("type", "MEMBER_ONBOARDING")
                put("sn", sn.trim().uppercase())
            }
            json.toString()
        } catch (_: Exception) {
            sn.trim().uppercase()
        }
    }

    /**
     * Creates a high-res branded QR Badge Bitmap suitable for printing or saving to device gallery.
     */
    fun createBrandedQrBadgeBitmap(
        context: Context,
        qrBitmap: Bitmap,
        serialNumber: String,
        churchTitle: String = "नई सृष्टि कलीसिया",
        roleTitle: String = "सदस्य (Member)"
    ): Bitmap {
        val width = 720
        val height = 980
        val badge = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(badge)

        // Background
        val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header Background Banner
        val headerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A") // Deep slate
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 150f, headerPaint)

        // Gold Accent Line
        val goldPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#D4AF37")
            strokeWidth = 6f
        }
        canvas.drawLine(0f, 150f, width.toFloat(), 150f, goldPaint)

        // Header Church Title
        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("✝ $churchTitle", width / 2f, 70f, textPaint)

        // Subtitle / Tagline
        textPaint.textSize = 22f
        textPaint.color = android.graphics.Color.parseColor("#E2E8F0")
        textPaint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("आधिकारिक डिजिटल सदस्यता पास (Official Member Pass)", width / 2f, 115f, textPaint)

        // Role Badge Pill
        val pillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#F1F5F9")
        }
        val pillRect = android.graphics.RectF(160f, 175f, (width - 160).toFloat(), 230f)
        canvas.drawRoundRect(pillRect, 28f, 28f, pillPaint)

        val rolePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 24f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText(roleTitle, width / 2f, 212f, rolePaint)

        // Draw QR Code
        val qrSize = 440
        val qrLeft = (width - qrSize) / 2f
        val qrTop = 250f
        val scaledQr = Bitmap.createScaledBitmap(qrBitmap, qrSize, qrSize, true)
        canvas.drawBitmap(scaledQr, qrLeft, qrTop, null)

        // Serial Number Display Box
        val snBoxPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FEF3C7") // warm light amber
        }
        val snBorderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#D97706")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
        }
        val snRect = android.graphics.RectF(120f, 715f, (width - 120).toFloat(), 810f)
        canvas.drawRoundRect(snRect, 20f, 20f, snBoxPaint)
        canvas.drawRoundRect(snRect, 20f, 20f, snBorderPaint)

        val snTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#92400E")
            textSize = 42f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("SN: $serialNumber", width / 2f, 778f, snTextPaint)

        // Instructions Footer
        val footPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#64748B")
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("सदस्य प्रोफ़ाइल सक्रिय करने हेतु इस कोड को स्कैन करें", width / 2f, 855f, footPaint)
        canvas.drawText("Scan to complete membership verification & onboarding", width / 2f, 890f, footPaint)

        // Outer Border
        val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(2f, 2f, (width - 2).toFloat(), (height - 2).toFloat(), borderPaint)

        return badge
    }

    /**
     * Saves high-res branded QR badge PNG directly to device Pictures gallery.
     */
    fun saveQrBadgeToGallery(
        context: Context,
        bitmap: Bitmap,
        serialNumber: String
    ): Boolean {
        val cleanSn = serialNumber.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val filename = "Church_Badge_${cleanSn}_${System.currentTimeMillis()}.png"
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/ChurchBadges")
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri).use { stream ->
                        if (stream != null) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            return true
                        }
                    }
                }
            } else {
                val dir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "ChurchBadges")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                return true
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Shares member onboarding WhatsApp/OS message with serial number & QR image.
     */
    fun shareOnboardingBadge(
        context: Context,
        serialNumber: String,
        badgeBitmap: Bitmap
    ) {
        try {
            val shareUri = saveQrBitmapToFile(context, badgeBitmap, serialNumber)
            val shareText = "✝️ नई सृष्टि कलीसिया (New Creation Church)\n" +
                    "सदस्य सीरियल नंबर: $serialNumber\n\n" +
                    "आपके लिए डिजिटल सदस्य पास जारी कर दिया गया है।\n" +
                    "ऑनबोर्डिंग लिंक: https://newcreationchurch.app/onboarding?sn=$serialNumber\n\n" +
                    "कृपया ऐप में अपना सीरियल नंबर ($serialNumber) दर्ज करें या संलग्न QR कोड स्कैन करके अपनी प्रोफ़ाइल एक्टिवेट करें।"

            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "कलीसिया सदस्य पास - $serialNumber")
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                if (shareUri != null) {
                    putExtra(android.content.Intent.EXTRA_STREAM, shareUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(android.content.Intent.createChooser(intent, "सदस्य पास शेयर करें (Share Pass)"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "शेयर करने में त्रुटि: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves a generated QR Bitmap to a temporary file for sharing.
     */
    fun saveQrBitmapToFile(context: Context, bitmap: Bitmap, serialNumber: String): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "qr_shares")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "qr_${serialNumber.replace("[^a-zA-Z0-9]".toRegex(), "_")}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
