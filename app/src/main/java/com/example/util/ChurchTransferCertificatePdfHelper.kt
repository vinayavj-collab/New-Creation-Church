package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.UserProfileData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates official Church Recommendation & Transfer Letters (TC / प्रमाण पत्र) as high-resolution PDF documents.
 */
object ChurchTransferCertificatePdfHelper {
    private const val PAGE_WIDTH = 595 // A4 standard width (points)
    private const val PAGE_HEIGHT = 842 // A4 standard height (points)

    fun generateTransferCertificatePdf(
        context: Context,
        member: UserProfileData,
        destinationChurchOrCity: String,
        transferType: String = "MARRIED_OUT", // "MARRIED_OUT", "EXTERNAL_RELOCATION", "INTERNAL_BRANCH"
        pastorName: String = "रेव. विनय कुमार",
        pastorDesignation: String = "मुख्य पास्टर (Senior Pastor)",
        branchName: String = "न्यू क्रिएशन चर्च (मुख्य कलीसिया • भिलाई)"
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        try {
            // Background
            canvas.drawColor(android.graphics.Color.WHITE)

            // Outer Decorative Border
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(180, 83, 9) // Amber/Gold #B45309
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRect(24f, 24f, PAGE_WIDTH - 24f, PAGE_HEIGHT - 24f, borderPaint)

            // Inner Fine Border
            val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(217, 119, 6) // Warm Gold
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRect(30f, 30f, PAGE_WIDTH - 30f, PAGE_HEIGHT - 30f, innerBorder)

            // Top Header Emblem & Cross
            val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(180, 83, 9)
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("✝", PAGE_WIDTH / 2f, 75f, crossPaint)

            // Header Title
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(15, 23, 42) // Dark Navy
                textSize = 20f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("न्यू क्रिएशन चर्च (NEW CREATION CHURCH)", PAGE_WIDTH / 2f, 105f, titlePaint)

            val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(71, 85, 105) // Slate
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("$branchName • छत्तीसगढ़ (भारत)", PAGE_WIDTH / 2f, 122f, subTitlePaint)

            // Divider Line
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(203, 213, 225)
                strokeWidth = 1.2f
            }
            canvas.drawLine(50f, 138f, PAGE_WIDTH - 50f, 138f, dividerPaint)

            // Document Heading Box
            val certTitleBox = RectF(PAGE_WIDTH / 2f - 170f, 155f, PAGE_WIDTH / 2f + 170f, 190f)
            val certBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(254, 243, 199) // Light Amber Tint
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(certTitleBox, 8f, 8f, certBoxBg)

            val certHeadingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(146, 64, 14) // Amber 800
                textSize = 13.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val headingText = if (transferType.contains("MARRIED", ignoreCase = true)) {
                "वैवाहिक कलीसियाई स्थानांतरण व अनुशंसा पत्र"
            } else {
                "कलीसियाई स्थानांतरण व सदस्यता अनुशंसा पत्र (TC)"
            }
            canvas.drawText(headingText, PAGE_WIDTH / 2f, 177f, certHeadingPaint)

            // Metadata Row: Ref No & Date
            val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale("hi", "IN")).format(Date())
            val refNo = "NCC/TC/${member.serialNumber}/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}"
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(51, 65, 85)
                textSize = 10f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText("पत्र क्रमांक: $refNo", 50f, 215f, metaPaint)

            val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(51, 65, 85)
                textSize = 10f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("दिनांक: $todayStr", PAGE_WIDTH - 50f, 215f, datePaint)

            // Salutation
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(15, 23, 42)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("प्रिय प्रभु के सेवक / कलीसियाई नेतृत्व में,", 50f, 250f, bodyPaint)
            canvas.drawText("मसीह यीशु के अनुग्रहकारी नाम में सादर अभिवादन।", 50f, 270f, bodyPaint)

            // Introduction Paragraph
            val p1 = "यह प्रमाणित किया जाता है कि नीचे उल्लिखित विश्वासी सदस्य न्यू क्रिएशन कलीसिया के एक सम्मानित व पंजीकृत सदस्य रहे हैं:"
            canvas.drawText(p1, 50f, 305f, bodyPaint)

            // Member Profile Details Card (Structured Box)
            val detailsBox = RectF(50f, 325f, PAGE_WIDTH - 50f, 480f)
            val detailsBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(248, 250, 252) // Slate 50
                style = Paint.Style.FILL
            }
            val detailsBoxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(226, 232, 240)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(detailsBox, 8f, 8f, detailsBoxBg)
            canvas.drawRoundRect(detailsBox, 8f, 8f, detailsBoxBorder)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(100, 116, 139)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(15, 23, 42)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // Row 1: Name & Permanent SN
            canvas.drawText("सदस्य का पूरा नाम:", 66f, 355f, labelPaint)
            canvas.drawText(member.fullName.ifBlank { member.displayName.ifBlank { "कलीसिया सदस्य" } }, 180f, 355f, valuePaint)

            canvas.drawText("स्थायी सदस्यता ID (SN):", 340f, 355f, labelPaint)
            canvas.drawText(member.serialNumber.ifBlank { "NCC-MEM" }, 475f, 355f, valuePaint)

            // Row 2: Gender & DOB
            canvas.drawText("लिंग (Gender):", 66f, 385f, labelPaint)
            canvas.drawText(member.gender, 180f, 385f, valuePaint)

            canvas.drawText("जन्म तिथि (DOB):", 340f, 385f, labelPaint)
            canvas.drawText(member.dateOfBirth.ifBlank { "अभिलेख में दर्ज" }, 475f, 385f, valuePaint)

            // Row 3: Holy Baptism & Date
            canvas.drawText("पवित्र जल बपतिस्मा:", 66f, 415f, labelPaint)
            val bapStatus = if (member.isBaptized || member.baptismStatus) "✓ बपतिस्मा प्राप्त" else "सत्यापित विश्वासी"
            canvas.drawText(bapStatus, 180f, 415f, valuePaint)

            canvas.drawText("बपतिस्मा तिथि:", 340f, 415f, labelPaint)
            canvas.drawText(member.baptismDate.ifBlank { "कलीसिया रिकॉर्ड में प्रमाणित" }, 475f, 415f, valuePaint)

            // Row 4: Family ID & Contact
            canvas.drawText("मूल परिवार ID:", 66f, 445f, labelPaint)
            canvas.drawText(member.familyId.ifBlank { "${member.serialNumber}-F" }, 180f, 445f, valuePaint)

            canvas.drawText("संपर्क फोन नंबर:", 340f, 445f, labelPaint)
            canvas.drawText(member.phoneNumber.ifBlank { member.phone.ifBlank { "—" } }, 475f, 445f, valuePaint)

            // Transfer Statement
            val reasonText = if (transferType.contains("MARRIED", ignoreCase = true)) {
                "इन्होंने हाल ही में विवाह संपन्न किया है और अपने पति के परिवार व नई कलीसिया/नगर ($destinationChurchOrCity) में सम्मिलित हो रहे हैं।"
            } else {
                "ये कार्य/निवास परिवर्तन के कारण नई कलीसिया ($destinationChurchOrCity) में स्थानांतरित हो रहे हैं।"
            }
            canvas.drawText(reasonText, 50f, 515f, bodyPaint)

            val p2 = "हमारी कलीसिया में इनकी संगति, आराधना और आत्मिक सहभागिता निष्ठावान रही है। इनके विरुद्ध कोई भी कलीसियाई अनुशासनात्मक मामला लंबित नहीं है।"
            canvas.drawText(p2, 50f, 540f, bodyPaint)

            val p3 = "हम इन्हें आपकी सम्मानित कलीसिया में पूर्ण आत्मिक प्रेम व स्वीकार्यता के साथ अनुशंसित करते हैं। प्रभु इनके नवीन पारिवारिक व कलीसियाई जीवन को बहुतायत से आशीष प्रदान करें।"
            canvas.drawText(p3, 50f, 565f, bodyPaint)

            // Scripture Verse at bottom
            val verseBox = RectF(50f, 600f, PAGE_WIDTH - 50f, 640f)
            val verseBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(verseBox, 6f, 6f, verseBoxBg)

            val versePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(71, 85, 105)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("\"यहोवा तुझे आशीष दे और तेरी रक्षा करे; यहोवा अपना मुख तुझ पर चमकाए और तुझ पर अनुग्रह करे।\" (गिनती 6:24-25)", PAGE_WIDTH / 2f, 624f, versePaint)

            // Signature & Seal Section
            val sealBox = RectF(70f, 680f, 170f, 750f)
            val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(180, 83, 9)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRoundRect(sealBox, 10f, 10f, sealPaint)

            val sealText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(180, 83, 9)
                textSize = 9f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("★ आधिकारिक मुहर ★", 120f, 705f, sealText)
            canvas.drawText("CHURCH SEAL", 120f, 720f, sealText)
            canvas.drawText("VERIFIED & SEALED", 120f, 735f, sealText)

            // Pastor Signature Right Side
            val sigLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(15, 23, 42)
                strokeWidth = 1f
            }
            canvas.drawLine(PAGE_WIDTH - 240f, 720f, PAGE_WIDTH - 60f, 720f, sigLine)

            val sigText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(15, 23, 42)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(pastorName, PAGE_WIDTH - 150f, 738f, sigText)

            val sigSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(100, 116, 139)
                textSize = 9.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(pastorDesignation, PAGE_WIDTH - 150f, 752f, sigSub)
            canvas.drawText("न्यू क्रिएशन चर्च मुख्यालय", PAGE_WIDTH - 150f, 765f, sigSub)

            // Document Footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(148, 163, 184)
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("यह एक डिजिटल रूप से सत्यापित आधिकारिक कलीसिया स्थानांतरण पत्र है। स्थायी सीरियल नंबर: ${member.serialNumber}", PAGE_WIDTH / 2f, 800f, footerPaint)

            pdfDocument.finishPage(page)

            // Save PDF to cache
            val outputDir = File(context.cacheDir, "transfer_letters")
            if (!outputDir.exists()) outputDir.mkdirs()

            val cleanName = member.serialNumber.ifBlank { member.userId.take(6) }
            val outputFile = File(outputDir, "Transfer_Certificate_${cleanName}.pdf")
            val fos = FileOutputStream(outputFile)
            pdfDocument.writeTo(fos)
            fos.flush()
            fos.close()
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

    fun shareTransferCertificate(context: Context, pdfUri: Uri, memberName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "कलीसिया स्थानांतरण पत्र (TC) - $memberName")
            putExtra(Intent.EXTRA_TEXT, "$memberName का आधिकारिक कलीसिया स्थानांतरण व अनुशंसा पत्र (Transfer Certificate) संलग्न है।")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "स्थानांतरण पत्र (TC) साझा करें"))
    }
}
