package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.prayer.model.DailyPrayerVerse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PrayerCardImageGenerator {

    suspend fun generateAndShareCard(
        context: Context,
        prayer: DailyPrayerVerse
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val width = 1080
            val height = 1440
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Background Gradient (Navy Dark to Deep Indigo)
            val bgPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.rgb(10, 25, 47), // Deep Navy
                    Color.rgb(23, 42, 69), // Indigo
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // 2. Decorative Gold Borders
            val borderPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(212, 175, 55) // GoldWarm
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            val inset = 36f
            val cornerRadius = 40f
            canvas.drawRoundRect(inset, inset, width - inset, height - inset, cornerRadius, cornerRadius, borderPaint)

            // Inner thin line
            borderPaint.strokeWidth = 1.5f
            borderPaint.alpha = 130
            canvas.drawRoundRect(inset + 14f, inset + 14f, width - inset - 14f, height - inset - 14f, cornerRadius - 8f, cornerRadius - 8f, borderPaint)

            // 3. Header: Church Branding & Category Badge
            val headerPaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.rgb(212, 175, 55) // Gold
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("✝ न्यू क्रिएशन चर्च • दैनिक प्रार्थना", width / 2f, 130f, headerPaint)

            val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("hi", "IN")).format(Date())
            val datePaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.rgb(200, 210, 230)
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(dateStr, width / 2f, 180f, datePaint)

            // Day & Theme badge box
            val themeBoxPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(30, 58, 100)
                style = Paint.Style.FILL
            }
            val themeBoxRect = RectF(120f, 220f, width - 120f, 310f)
            canvas.drawRoundRect(themeBoxRect, 20f, 20f, themeBoxPaint)

            val themeBorderPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(212, 175, 55)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(themeBoxRect, 20f, 20f, themeBorderPaint)

            val themeTitlePaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("दिवस ${prayer.dayNumber} • ${prayer.themeTitleHindi}", width / 2f, 278f, themeTitlePaint)

            // 4. Large Gold Quote Icon
            val quoteMarkPaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.rgb(212, 175, 55)
                textSize = 110f
                alpha = 80
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("❝", width / 2f, 400f, quoteMarkPaint)

            // 5. Scripture Text (Multi-line StaticLayout)
            val versePaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.rgb(255, 255, 255)
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textWidth = width - 200
            val verseLayout = StaticLayout.Builder.obtain(
                prayer.verseTextHindi,
                0,
                prayer.verseTextHindi.length,
                versePaint,
                textWidth
            ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

            canvas.save()
            canvas.translate(100f, 420f)
            verseLayout.draw(canvas)
            canvas.restore()

            val verseHeight = verseLayout.height

            // 6. Scripture Reference in Gold
            val refPaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.rgb(245, 195, 65) // Bright Gold
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val refY = 440f + verseHeight + 20f
            canvas.drawText("— ${prayer.verseReferenceHindi} —", width / 2f, refY, refPaint)

            // 7. Divider Line with Cross
            val lineY = refY + 40f
            val dividerPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(212, 175, 55)
                strokeWidth = 2f
                alpha = 150
            }
            canvas.drawLine(200f, lineY, (width / 2f) - 40f, lineY, dividerPaint)
            canvas.drawLine((width / 2f) + 40f, lineY, width - 200f, lineY, dividerPaint)

            val crossPaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.rgb(212, 175, 55)
                textSize = 32f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("✝", width / 2f, lineY + 10f, crossPaint)

            // 8. Daily Prayer Excerpt Box
            val prayerBoxTop = lineY + 50f
            val prayerPaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.rgb(220, 230, 245)
                textSize = 32f
            }
            val shortPrayer = prayer.prayerHindi.take(240).let { if (prayer.prayerHindi.length > 240) "$it..." else it }
            val prayerLayout = StaticLayout.Builder.obtain(
                "🙏 $shortPrayer",
                0,
                shortPrayer.length + 3,
                prayerPaint,
                textWidth
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

            canvas.save()
            canvas.translate(100f, prayerBoxTop)
            prayerLayout.draw(canvas)
            canvas.restore()

            // 9. Faith Declaration Highlight Card at Bottom
            val declBoxTop = (height - 280f)
            val declBoxRect = RectF(90f, declBoxTop, width - 90f, height - 120f)
            val declBgPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(20, 40, 75)
            }
            canvas.drawRoundRect(declBoxRect, 24f, 24f, declBgPaint)

            val declBorderPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(212, 175, 55)
                style = Paint.Style.STROKE
                strokeWidth = 2f
                alpha = 180
            }
            canvas.drawRoundRect(declBoxRect, 24f, 24f, declBorderPaint)

            val declTitlePaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.rgb(245, 195, 65)
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("✨ आत्मिक अंगीकार (Faith Declaration)", width / 2f, declBoxTop + 45f, declTitlePaint)

            val declTextPaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 30f
            }
            val shortDecl = prayer.declarationHindi.take(130).let { if (prayer.declarationHindi.length > 130) "$it..." else it }
            val declLayout = StaticLayout.Builder.obtain(
                "\"$shortDecl\"",
                0,
                shortDecl.length + 2,
                declTextPaint,
                textWidth - 40
            ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

            canvas.save()
            canvas.translate(120f, declBoxTop + 65f)
            declLayout.draw(canvas)
            canvas.restore()

            // 10. Footer App Branding
            val footerPaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.rgb(180, 190, 210)
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("📲 New Creation Church App • प्रभु आपको आशीष दे", width / 2f, height - 60f, footerPaint)

            // Save Bitmap to Cache
            val cacheFolder = File(context.cacheDir, "prayer_cards").apply { mkdirs() }
            val imageFile = File(cacheFolder, "daily_prayer_day_${prayer.dayNumber}.png")
            val fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            // Launch Share Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "✨ आज का प्रेरक वचन व प्रार्थना (${prayer.verseReferenceHindi})\n📲 न्यू क्रिएशन चर्च ऐप")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "दैनिक प्रार्थना पोस्टर शेयर करें"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
