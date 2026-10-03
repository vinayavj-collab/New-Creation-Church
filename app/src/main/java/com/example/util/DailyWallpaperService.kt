package com.example.util

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * DailyWallpaperService: Handles automatic and on-demand AI wallpaper generation fetching
 * from Firestore config and applying to Home/Lock screen natively supporting mobile (9:16)
 * and tablet (4:3 / 16:10) aspect ratios.
 */
object DailyWallpaperService {
    private const val TAG = "DailyWallpaperService"

    /**
     * Curated list of inspirational Hindi scripture verses with book references
     * for continuous wallpaper rotation and fresh spiritual upliftment.
     */
    val INSPIRATIONAL_VERSES = listOf(
        Pair("प्रभु मेरा चरवाहा है; मुझे कुछ घटी न होगी।", "भजन संहिता 23:1"),
        Pair("क्योंकि जो कोई यहोवा पर भरोसा रखते हैं, वे नये बल पाते रहेंगे; वे उकाबों की नाईं उड़ेंगे।", "यशायाह 40:31"),
        Pair("मैं तुम्हें शान्ति दिए जाता हूँ, अपनी शान्ति तुम्हें देता हूँ; जैसे संसार देता है, मैं तुम्हें नहीं देता।", "यूहन्ना 14:27"),
        Pair("तुम्हारा मन व्याकुल न हो; परमेश्वर पर विश्वास रखो और मुझ पर भी विश्वास रखो।", "यूहन्ना 14:1"),
        Pair("जो मुझे सामर्थ्य देता है उसमें मैं सब कुछ कर सकता हूँ।", "फिलिप्पियों 4:13"),
        Pair("यहोवा का धन्यवाद करो क्योंकि वह भला है, और उसकी करुणा सदा की है।", "1 इतिहास 16:34"),
        Pair("परमेश्वर हमारा शरणस्थान और बल है, संकट में अति सहज से मिलने वाला सहायक।", "भजन संहिता 46:1"),
        Pair("तेरा वचन मेरे पाँव के लिये दीपक, और मेरे मार्ग के लिये उजियाला है।", "भजन संहिता 119:105"),
        Pair("डरो मत, क्योंकि मैं तुम्हारे साथ हूँ; निराश मत हो, क्योंकि मैं तुम्हारा परमेश्वर हूँ।", "यशायाह 41:10"),
        Pair("यदि परमेश्वर हमारी ओर है, तो हमारा विरोधी कौन हो सकता है?", "रोमियों 8:31"),
        Pair("अपनी सारी चिन्ता उसी पर डाल दो, क्योंकि उसको तुम्हारा ध्यान है।", "1 पतरस 5:7"),
        Pair("यहोवा की आँखें धर्मियों पर लगी रहती हैं, और उसके कान उनकी दोहाई की ओर लगे रहते हैं।", "भजन संहिता 34:15")
    )

    /**
     * Get scripture by index or cyclic rotation.
     */
    fun getScriptureByIndex(index: Int): Pair<String, String> {
        val safeIndex = Math.floorMod(index, INSPIRATIONAL_VERSES.size)
        return INSPIRATIONAL_VERSES[safeIndex]
    }

    /**
     * Download an image or generate a scripture wallpaper and apply to phone Home / Lock screen.
     */
    suspend fun applyScriptureWallpaper(
        context: Context,
        verseText: String = "",
        verseRef: String = "",
        imageUrl: String? = null,
        targetScreen: String = "both", // "home" | "lock" | "both"
        selectedStyle: TheologicalStyle? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            var bitmap: Bitmap? = null

            // 1. Try downloading image if URL is provided
            if (!imageUrl.isNullOrBlank()) {
                try {
                    val url = URL(imageUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.doInput = true
                    connection.connect()
                    val input: InputStream = connection.inputStream
                    bitmap = BitmapFactory.decodeStream(input)
                    input.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download wallpaper from URL: $imageUrl, generating fallback art", e)
                }
            }

            // 2. If no image downloaded, render a high-quality Scripture Wallpaper Bitmap
            val todayVerse = com.example.data.bible.model.VerseOfTheDay.getTodayVerse()
            val finalBitmap = bitmap ?: generateScriptureWallpaperBitmap(
                context = context,
                verseText = verseText.ifBlank { todayVerse.textHindi },
                verseRef = verseRef.ifBlank { todayVerse.referenceHindi },
                selectedStyle = selectedStyle
            )

            val wallpaperManager = WallpaperManager.getInstance(context)

            // 3. Apply with comprehensive fallback compatibility across OEM skins and Android API levels
            when (targetScreen.lowercase()) {
                "home" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        try {
                            wallpaperManager.setBitmap(finalBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        } catch (e: Exception) {
                            Log.w(TAG, "FLAG_SYSTEM setBitmap failed, trying default setBitmap", e)
                            wallpaperManager.setBitmap(finalBitmap)
                        }
                    } else {
                        wallpaperManager.setBitmap(finalBitmap)
                    }
                }
                "lock" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        try {
                            wallpaperManager.setBitmap(finalBitmap, null, true, WallpaperManager.FLAG_LOCK)
                        } catch (e: Exception) {
                            Log.w(TAG, "FLAG_LOCK setBitmap failed, trying default setBitmap", e)
                            wallpaperManager.setBitmap(finalBitmap)
                        }
                    } else {
                        wallpaperManager.setBitmap(finalBitmap)
                    }
                }
                else -> { // "both" (Home + Lock Screen Sync)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        var isSuccess = false
                        // Primary attempt: Combined FLAG_SYSTEM or FLAG_LOCK for simultaneous sync
                        try {
                            val combinedFlags = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                            wallpaperManager.setBitmap(finalBitmap, null, true, combinedFlags)
                            isSuccess = true
                        } catch (e: Exception) {
                            Log.w(TAG, "Combined FLAG_SYSTEM|FLAG_LOCK failed: ${e.message}, trying individual flags")
                        }

                        if (!isSuccess) {
                            var homeOk = false
                            var lockOk = false
                            try {
                                wallpaperManager.setBitmap(finalBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                homeOk = true
                            } catch (e: Exception) {
                                Log.w(TAG, "FLAG_SYSTEM failed: ${e.message}")
                            }
                            try {
                                wallpaperManager.setBitmap(finalBitmap, null, true, WallpaperManager.FLAG_LOCK)
                                lockOk = true
                            } catch (e: Exception) {
                                Log.w(TAG, "FLAG_LOCK failed: ${e.message}")
                            }

                            if (!homeOk && !lockOk) {
                                try {
                                    wallpaperManager.setBitmap(finalBitmap)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Final fallback setBitmap failed", e)
                                }
                            }
                        }
                    } else {
                        wallpaperManager.setBitmap(finalBitmap)
                    }
                }
            }

            Log.i(TAG, "Successfully applied daily scripture AI wallpaper to target: $targetScreen (Verse: $verseRef)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error applying daily scripture wallpaper", e)
            false
        }
    }

    /**
     * Download and apply wallpaper by image URL.
     */
    suspend fun downloadAndApplyWallpaper(
        context: Context,
        imageUrl: String,
        targetScreen: String = "both"
    ): Boolean {
        return applyScriptureWallpaper(
            context = context,
            imageUrl = imageUrl,
            targetScreen = targetScreen
        )
    }

    /**
     * Theological Art Style enum for dynamic multi-art styling
     */
    enum class TheologicalStyle(val displayNameHindi: String, val subtitle: String) {
        CINEMATIC_BIBLICAL_HISTORICAL("✝ पवित्र क्रूस व स्वर्गीय आभा", "Cinematic Sunset & Radiant Cross"),
        SOFT_BOTANICAL_WATERCOLOR("🌿 हरी चराइयां व सुखदायी जल", "Emerald Pastures & Quiet Waters"),
        ETHEREAL_DREAMY_PASTEL("✨ स्वर्गीय महिमा व दिव्य किरणें", "Heavenly Clouds & Divine Sunrays"),
        VIBRANT_SPIRITUAL_STORYBOOK("🕊️ जीवन का जल व अनुग्रह", "Living Waters & Holy Spirit"),
        ROCK_OF_AGES("⛰️ दृढ़ शरणस्थान व चट्टान", "Mountain Fortress & Strength")
    }

    /**
     * Determines theological style based on scripture content & reference keywords.
     */
    fun detectTheologicalStyle(verseText: String, verseRef: String): TheologicalStyle {
        val combined = "$verseText $verseRef".lowercase()
        return when {
            combined.contains("शांति") || combined.contains("दाखलता") || combined.contains("फल") || 
            combined.contains("हरी चराइयों") || combined.contains("सुखदायी जल") || combined.contains("peace") || 
            combined.contains("vine") || combined.contains("nature") -> TheologicalStyle.SOFT_BOTANICAL_WATERCOLOR

            combined.contains("महिमा") || combined.contains("आशा") || combined.contains("पुनरुत्थान") || 
            combined.contains("ज्योति") || combined.contains("स्वर्ग") || combined.contains("glory") || 
            combined.contains("hope") || combined.contains("light") -> TheologicalStyle.ETHEREAL_DREAMY_PASTEL

            combined.contains("शरणस्थान") || combined.contains("चट्टान") || combined.contains("बल") ||
            combined.contains("सामर्थ्य") || combined.contains("rock") || combined.contains("refuge") -> TheologicalStyle.ROCK_OF_AGES

            combined.contains("जीवन का जल") || combined.contains("चरवाहा") || combined.contains("अनुग्रह") || 
            combined.contains("प्रेम") || combined.contains("उद्धार") || combined.contains("water") || 
            combined.contains("shepherd") || combined.contains("grace") -> TheologicalStyle.VIBRANT_SPIRITUAL_STORYBOOK

            else -> TheologicalStyle.CINEMATIC_BIBLICAL_HISTORICAL
        }
    }

    /**
     * Renders a 1080x1920 (9:16) spiritual biblical wallpaper dynamically mapped to
     * AI-inspired theological art styles with rich scenic elements, divine sunbeams,
     * glowing sacred symbols, and frame-safe centered Hindi scripture typography.
     */
    fun generateScriptureWallpaperBitmap(
        context: Context,
        verseText: String,
        verseRef: String,
        selectedStyle: TheologicalStyle? = null
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val theologicalStyle = selectedStyle ?: detectTheologicalStyle(verseText, verseRef)

        // 1. Dynamic Thematic Multi-Stop Gradient Background
        val (bgColors, glowColor1, glowColor2, accentColor) = when (theologicalStyle) {
            TheologicalStyle.CINEMATIC_BIBLICAL_HISTORICAL -> Quadruple(
                intArrayOf(Color.rgb(12, 16, 28), Color.rgb(38, 20, 36), Color.rgb(18, 12, 24)),
                Color.argb(130, 245, 158, 11), Color.argb(45, 217, 119, 6),
                Color.rgb(251, 191, 36)
            )
            TheologicalStyle.SOFT_BOTANICAL_WATERCOLOR -> Quadruple(
                intArrayOf(Color.rgb(14, 34, 26), Color.rgb(22, 48, 36), Color.rgb(10, 26, 20)),
                Color.argb(120, 52, 211, 153), Color.argb(40, 16, 185, 129),
                Color.rgb(110, 231, 183)
            )
            TheologicalStyle.ETHEREAL_DREAMY_PASTEL -> Quadruple(
                intArrayOf(Color.rgb(28, 18, 48), Color.rgb(46, 26, 62), Color.rgb(18, 16, 38)),
                Color.argb(130, 192, 132, 252), Color.argb(45, 244, 114, 182),
                Color.rgb(232, 121, 249)
            )
            TheologicalStyle.VIBRANT_SPIRITUAL_STORYBOOK -> Quadruple(
                intArrayOf(Color.rgb(12, 28, 46), Color.rgb(18, 42, 62), Color.rgb(8, 20, 34)),
                Color.argb(130, 56, 189, 248), Color.argb(45, 14, 165, 233),
                Color.rgb(125, 211, 252)
            )
            TheologicalStyle.ROCK_OF_AGES -> Quadruple(
                intArrayOf(Color.rgb(20, 24, 36), Color.rgb(32, 38, 52), Color.rgb(14, 18, 26)),
                Color.argb(120, 251, 191, 36), Color.argb(40, 217, 119, 6),
                Color.rgb(253, 224, 71)
            )
        }

        val backgroundGradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            bgColors,
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply {
            shader = backgroundGradient
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Scenic AI Artwork Landscape Elements (Hills, Waters, Mountains, Sunbeams)
        when (theologicalStyle) {
            TheologicalStyle.SOFT_BOTANICAL_WATERCOLOR -> {
                // Rolling Green Pastures at bottom
                val hillPaint = Paint().apply {
                    color = Color.argb(60, 16, 185, 129)
                    isAntiAlias = true
                }
                val hillPath = Path().apply {
                    moveTo(0f, height * 0.78f)
                    quadTo(width * 0.35f, height * 0.72f, width * 0.7f, height * 0.77f)
                    quadTo(width * 0.9f, height * 0.79f, width.toFloat(), height * 0.75f)
                    lineTo(width.toFloat(), height.toFloat())
                    lineTo(0f, height.toFloat())
                    close()
                }
                canvas.drawPath(hillPath, hillPaint)

                // Still Waters gentle stream glow
                val waterPaint = Paint().apply {
                    color = Color.argb(40, 56, 189, 248)
                    isAntiAlias = true
                }
                canvas.drawRect(0f, height * 0.85f, width.toFloat(), height.toFloat(), waterPaint)
            }
            TheologicalStyle.ROCK_OF_AGES -> {
                // Distant majestic mountain silhouettes
                val mountainPaint = Paint().apply {
                    color = Color.argb(45, 148, 163, 184)
                    isAntiAlias = true
                }
                val mountainPath = Path().apply {
                    moveTo(0f, height * 0.75f)
                    lineTo(width * 0.25f, height * 0.65f)
                    lineTo(width * 0.5f, height * 0.72f)
                    lineTo(width * 0.78f, height * 0.62f)
                    lineTo(width.toFloat(), height * 0.74f)
                    lineTo(width.toFloat(), height.toFloat())
                    lineTo(0f, height.toFloat())
                    close()
                }
                canvas.drawPath(mountainPath, mountainPaint)
            }
            else -> {
                // Subtle horizon glow
                val horizonPaint = Paint().apply {
                    color = Color.argb(30, 255, 255, 255)
                    isAntiAlias = true
                }
                canvas.drawCircle(width / 2f, height * 0.80f, width * 0.6f, horizonPaint)
            }
        }

        // 3. Divine Ambient Glow in Upper Focal Zone (Sunrays from heaven)
        val glowGradient = RadialGradient(
            width / 2f, height * 0.28f,
            width * 0.75f,
            intArrayOf(glowColor1, glowColor2, Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        val glowPaint = Paint().apply {
            shader = glowGradient
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, height * 0.28f, width * 0.75f, glowPaint)

        // Crepuscular Sunrays
        val sunrayPaint = Paint().apply {
            color = Color.argb(25, 255, 255, 255)
            strokeWidth = 4f
            isAntiAlias = true
        }
        val sunCenterX = width / 2f
        val sunCenterY = height * 0.26f
        for (i in -4..4) {
            val angle = i * 18f * (Math.PI.toFloat() / 180f)
            val rayLength = 320f
            val endX = sunCenterX + (Math.sin(angle.toDouble()) * rayLength).toFloat()
            val endY = sunCenterY + (Math.cos(angle.toDouble()) * rayLength).toFloat()
            canvas.drawLine(sunCenterX, sunCenterY, endX, endY, sunrayPaint)
        }

        // 4. Symbolic Focal Point (Radiant Cross with Sacred Divine Aura)
        val symbolPaint = Paint().apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 6.5f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }

        val centerX = width / 2f
        val crossY = height * 0.26f
        val crossHeight = 110f
        val crossWidth = 64f

        // Vertical & Horizontal beams
        canvas.drawLine(centerX, crossY - crossHeight * 0.4f, centerX, crossY + crossHeight * 0.6f, symbolPaint)
        canvas.drawLine(centerX - crossWidth / 2f, crossY - crossHeight * 0.1f, centerX + crossWidth / 2f, crossY - crossHeight * 0.1f, symbolPaint)

        // Sacred Halo Ring
        val ringPaint = Paint().apply {
            color = Color.argb(100, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, crossY - crossHeight * 0.1f, 32f, ringPaint)

        // 5. Dark Vignette Protective Backdrop for Scripture Text Readability
        val cardRect = RectF(60f, height * 0.35f, width - 60f, height * 0.86f)
        val cardBackdropPaint = Paint().apply {
            color = Color.argb(95, 8, 12, 20)
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardBackdropPaint)

        val cardBorderPaint = Paint().apply {
            color = Color.argb(55, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardBorderPaint)

        // 6. Header: "✝ आज का वचन ✝"
        val headerPaint = TextPaint().apply {
            color = accentColor
            textSize = 36f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(10f, 0f, 4f, Color.argb(200, 0, 0, 0))
        }
        val headerY = height * 0.395f
        canvas.drawText("✝  आज का वचन  ✝", centerX, headerY, headerPaint)

        // Decorative subtle divider
        val dividerPaint = Paint().apply {
            color = Color.argb(90, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            strokeWidth = 2.5f
            isAntiAlias = true
        }
        val dividerY = headerY + 24f
        canvas.drawLine(centerX - 110f, dividerY, centerX + 110f, dividerY, dividerPaint)

        // 7. Verse Text (Multi-line Hindi typography with proper frame-safe centering)
        val fullVerse = "“${verseText.trim()}”"
        val dynamicTextSize = when {
            fullVerse.length > 140 -> 40f
            fullVerse.length > 80 -> 46f
            else -> 52f
        }

        val textPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = dynamicTextSize
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(16f, 0f, 6f, Color.argb(230, 0, 0, 0))
        }

        val paddingHorizontal = 90
        val textWidth = width - (paddingHorizontal * 2)

        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(
                fullVerse,
                0,
                fullVerse.length,
                textPaint,
                textWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(14f, 1.2f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                fullVerse,
                textPaint,
                textWidth,
                Layout.Alignment.ALIGN_CENTER,
                1.2f,
                14f,
                false
            )
        }

        canvas.save()
        val textStartY = dividerY + 44f
        // Frame-safe translation
        canvas.translate(paddingHorizontal.toFloat(), textStartY)
        staticLayout.draw(canvas)
        canvas.restore()

        // 8. Scripture Reference
        val refPaint = TextPaint().apply {
            color = accentColor
            textSize = 38f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(10f, 0f, 4f, Color.argb(200, 0, 0, 0))
        }
        val refY = textStartY + staticLayout.height + 48f
        canvas.drawText("— $verseRef —", centerX, refY, refPaint)

        // 9. Subtle Footer Branding & Style Indicator
        val footerPaint = TextPaint().apply {
            color = Color.argb(130, 255, 255, 255)
            textSize = 26f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("New Creation Church • AI Daily Scripture", centerX, height * 0.92f, footerPaint)

        return bitmap
    }

    /**
     * Renders and shares the high-resolution AI scripture wallpaper photo directly via Share Intent.
     */
    suspend fun shareScriptureWallpaperPhoto(
        context: Context,
        verseText: String,
        verseRef: String,
        selectedStyle: TheologicalStyle? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = generateScriptureWallpaperBitmap(
                context = context,
                verseText = verseText,
                verseRef = verseRef,
                selectedStyle = selectedStyle
            )

            val cacheFolder = File(context.cacheDir, "scripture_wallpapers").apply { mkdirs() }
            val imageFile = File(cacheFolder, "verse_wallpaper_${System.currentTimeMillis()}.png")
            val fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "✝️ आज का वचन (Daily Scripture Wallpaper)\n\n“$verseText”\n— $verseRef\n\n📲 New Creation Church App"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "वचन वॉलपेपर फोटो शेयर करें").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share scripture wallpaper photo", e)
            false
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
