package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.UserProfileData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service to generate printable A4 batch ID cards in vector PDF (CR80 standard size)
 * for elderly and non-smartphone members with High-Density QR Codes.
 */
object BadgesPdfGeneratorService {

    private const val PAGE_WIDTH = 595 // A4 standard width (pt)
    private const val PAGE_HEIGHT = 842 // A4 standard height (pt)

    private const val CARDS_PER_ROW = 2
    private const val CARDS_PER_COL = 4 // 8 cards per A4 page with generous lamination margins
    private const val CARD_WIDTH = 270f
    private const val CARD_HEIGHT = 180f
    private const val MARGIN_X = 20f
    private const val MARGIN_Y = 30f
    private const val SPACING_X = 15f
    private const val SPACING_Y = 18f

    fun generateBadgesPdf(
        context: Context,
        members: List<UserProfileData>,
        branchName: String = "न्यू क्रिएशन चर्च (मुख्य कलीसिया)",
        churchPrefix: String = "NCC"
    ): Uri? {
        if (members.isEmpty()) return null

        val pdfDocument = PdfDocument()
        val cardsPerPage = CARDS_PER_ROW * CARDS_PER_COL
        val totalPages = (members.size + cardsPerPage - 1) / cardsPerPage

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        try {
            for (pageIndex in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Background of A4 Sheet
                canvas.drawColor(android.graphics.Color.WHITE)

                // Page Header / Title
                headerPaint.color = android.graphics.Color.rgb(15, 23, 42) // Dark Navy
                headerPaint.textSize = 14f
                headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("⛪ $branchName - भौतिक सदस्यता पहचान पत्र (Physical ID Cards)", MARGIN_X, 20f, headerPaint)

                subTextPaint.color = android.graphics.Color.rgb(100, 116, 139)
                subTextPaint.textSize = 9f
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                canvas.drawText("बैच प्रिंट दिनांक: $dateStr | पृष्ठ ${pageIndex + 1} / $totalPages", PAGE_WIDTH - 220f, 20f, subTextPaint)

                // Draw cutting guidelines
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(226, 232, 240)
                    strokeWidth = 0.5f
                    pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
                }

                val startIndex = pageIndex * cardsPerPage
                val endIndex = (startIndex + cardsPerPage).coerceAtMost(members.size)

                for (i in startIndex until endIndex) {
                    val localIndex = i - startIndex
                    val row = localIndex / CARDS_PER_ROW
                    val col = localIndex % CARDS_PER_ROW

                    val cardLeft = MARGIN_X + col * (CARD_WIDTH + SPACING_X)
                    val cardTop = MARGIN_Y + row * (CARD_HEIGHT + SPACING_Y)
                    val cardRight = cardLeft + CARD_WIDTH
                    val cardBottom = cardTop + CARD_HEIGHT

                    val member = members[i]
                    drawSingleIdCard(
                        canvas = canvas,
                        member = member,
                        cardLeft = cardLeft,
                        cardTop = cardTop,
                        cardRight = cardRight,
                        cardBottom = cardBottom,
                        branchName = branchName,
                        churchPrefix = churchPrefix
                    )
                }

                pdfDocument.finishPage(page)
            }

            // Save PDF to cache directory
            val outputDir = File(context.cacheDir, "pdf_badges").apply { mkdirs() }
            val outputFile = File(outputDir, "Church_ID_Badges_${churchPrefix}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(outputFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            try { pdfDocument.close() } catch (_: Exception) {}
            return null
        }
    }

    private fun drawSingleIdCard(
        canvas: Canvas,
        member: UserProfileData,
        cardLeft: Float,
        cardTop: Float,
        cardRight: Float,
        cardBottom: Float,
        branchName: String,
        churchPrefix: String
    ) {
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        // 1. Card Outer Container (Cardboard effect with rounded corners)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 10f, 10f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(cardRect, 10f, 10f, borderPaint)

        // 2. Header Banner with Gold accent
        val headerHeight = 36f
        val headerRect = RectF(cardLeft, cardTop, cardRight, cardTop + headerHeight)
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(15, 23, 42) // Slate 900
            style = Paint.Style.FILL
        }
        // Draw top rounded only
        val path = Path().apply {
            addRoundRect(
                headerRect,
                floatArrayOf(10f, 10f, 10f, 10f, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        }
        canvas.drawPath(path, headerBgPaint)

        // Gold trim bar
        val trimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(245, 158, 11) // Gold #F59E0B
            strokeWidth = 2f
            style = Paint.Style.FILL
        }
        canvas.drawRect(cardLeft, cardTop + headerHeight - 2f, cardRight, cardTop + headerHeight, trimPaint)

        // Header Church Name & Cross
        val churchTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("⛪ $branchName", cardLeft + 8f, cardTop + 16f, churchTitlePaint)

        val churchSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(226, 232, 240)
            textSize = 7.5f
        }
        canvas.drawText("आधिकारिक डिजिटल सदस्यता पहचान पत्र (Member Identity Pass)", cardLeft + 8f, cardTop + 28f, churchSubtitlePaint)

        // 3. Left Column: Avatar & Role Badge
        val avatarLeft = cardLeft + 10f
        val avatarTop = cardTop + headerHeight + 8f
        val avatarSize = 42f
        val avatarRect = RectF(avatarLeft, avatarTop, avatarLeft + avatarSize, avatarTop + avatarSize)

        val avatarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(avatarRect, 8f, 8f, avatarBgPaint)

        val avatarBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(avatarRect, 8f, 8f, avatarBorderPaint)

        // Avatar silhouette icon placeholder
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(148, 163, 184)
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("👤", avatarRect.centerX(), avatarRect.centerY() + 6f, iconPaint)

        // 4. Center Column: Member Details
        val textLeft = avatarLeft + avatarSize + 10f
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(15, 23, 42)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val memberName = member.fullName.ifBlank { member.displayName.ifBlank { "विश्वासी सदस्य" } }
        canvas.drawText(memberName.take(20), textLeft, avatarTop + 12f, namePaint)

        // Monospace Alphanumeric Serial ID Tag
        val serialId = member.serialNumber.ifBlank { "${churchPrefix}01" }
        val serialBoxRect = RectF(textLeft, avatarTop + 18f, textLeft + 65f, avatarTop + 33f)
        val serialBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(254, 243, 199) // Soft Gold/Amber
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(serialBoxRect, 4f, 4f, serialBgPaint)

        val monoSerialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(180, 83, 9)
            textSize = 9f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(serialId, serialBoxRect.centerX(), serialBoxRect.centerY() + 3.2f, monoSerialPaint)

        // Role Badge
        val roleBadgeLeft = textLeft + 70f
        val roleText = when (member.roleTier.lowercase()) {
            "pastor" -> "पास्टर (Pastor)"
            "elder" -> "एल्डर / सेवक (Elder)"
            "master_admin" -> "मुख्य प्रशासक"
            else -> "विश्वासी (Believer)"
        }
        val roleBoxRect = RectF(roleBadgeLeft, avatarTop + 18f, roleBadgeLeft + 72f, avatarTop + 33f)
        val roleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(224, 231, 255) // Soft Indigo
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(roleBoxRect, 4f, 4f, roleBgPaint)

        val roleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(67, 56, 202)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(roleText, roleBoxRect.centerX(), roleBoxRect.centerY() + 2.8f, roleTextPaint)

        // Extra details: Gender & Phone
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(71, 85, 105)
            textSize = 7.8f
        }
        val phoneStr = member.phoneNumber.ifBlank { member.phone.ifBlank { "N/A" } }
        canvas.drawText("लिंग: ${member.gender} | संपर्क: $phoneStr", textLeft, avatarTop + 43f, detailPaint)

        // 5. Right Column: High-Density QR Code
        val qrSize = 64f
        val qrLeft = cardRight - qrSize - 10f
        val qrTop = cardTop + headerHeight + 6f
        val qrRect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)

        val qrPayload = QrCodeHelper.createActivationPayload(serialId, role = member.roleTier)
        val qrBitmap = QrCodeHelper.generateQrBitmap(qrPayload, sizePx = 180)
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, null, qrRect, null)
            val qrBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            canvas.drawRoundRect(qrRect, 4f, 4f, qrBorderPaint)
        }

        // 6. Security Seal & Barcode simulation stripe
        val footerTop = cardBottom - 26f
        val footerBg = RectF(cardLeft, footerTop, cardRight, cardBottom)
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val footerPath = Path().apply {
            addRoundRect(
                footerBg,
                floatArrayOf(0f, 0f, 0f, 0f, 10f, 10f, 10f, 10f),
                Path.Direction.CW
            )
        }
        canvas.drawPath(footerPath, footerPaint)

        val footerDivider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        canvas.drawLine(cardLeft, footerTop, cardRight, footerTop, footerDivider)

        // Footer validity and emergency text
        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(100, 116, 139)
            textSize = 7.2f
        }
        canvas.drawText("🔒 प्रमाणीकृत कलीसिया पास • वैधता: 2026-2027", cardLeft + 8f, footerTop + 12f, footerTextPaint)
        canvas.drawText("उशर स्कैनिंग हेतु इस कार्ड को प्रवेश द्वार पर दिखाएं", cardLeft + 8f, footerTop + 21f, footerTextPaint)

        // Stamp badge on right
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(16, 185, 129) // Emerald
            textSize = 8f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("✓ VERIFIED", cardRight - 8f, footerTop + 16f, stampPaint)
    }

    /**
     * Triggers Android Native Print or Share chooser
     */
    fun printOrShareBadgesPdf(context: Context, pdfUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "कलीसिया भौतिक सदस्यता पहचान पत्र (A4 ID Cards)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "ID कार्ड PDF प्रिंट या साझा करें"))
    }
}
