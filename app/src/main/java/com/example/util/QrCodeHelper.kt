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

        // Try JSON parsing
        if (raw.startsWith("{") && raw.endsWith("}")) {
            try {
                val json = JSONObject(raw)
                val serial = json.optString("serial", "").trim().uppercase()
                val p2 = json.optString("p2", "").takeIf { it.isNotBlank() }
                if (serial.isNotBlank()) {
                    return Pair(serial, p2)
                }
            } catch (_: Exception) {}
        }

        // Try URL / deep link parsing (e.g. ncck://activate?serial=NCCK-1234&p2=5678)
        if (raw.contains("serial=", ignoreCase = true)) {
            try {
                val uri = Uri.parse(raw)
                val serial = uri.getQueryParameter("serial")?.trim()?.uppercase()
                val p2 = uri.getQueryParameter("p2")?.trim()
                if (!serial.isNullOrBlank()) {
                    return Pair(serial, p2)
                }
            } catch (_: Exception) {}
        }

        // Direct serial code format (e.g., NCCK-B-1002 or NCCK-1234)
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
