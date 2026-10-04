package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
  )

fun parseHexColor(hexString: String?): androidx.compose.ui.graphics.Color? {
    if (hexString.isNullOrBlank()) return null
    return try {
        val cleanHex = hexString.trim().removePrefix("#")
        val colorInt = when (cleanHex.length) {
            6 -> (0xFF000000 or cleanHex.toLong(16)).toInt()
            8 -> cleanHex.toLong(16).toInt()
            else -> null
        }
        colorInt?.let { androidx.compose.ui.graphics.Color(it) }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  customPrimaryHex: String? = null,
  customSecondaryHex: String? = null,
  content: @Composable () -> Unit,
) {
  val customPrimary = androidx.compose.runtime.remember(customPrimaryHex) { parseHexColor(customPrimaryHex) }
  val customSecondary = androidx.compose.runtime.remember(customSecondaryHex) { parseHexColor(customSecondaryHex) }

  val isSunday = androidx.compose.runtime.remember {
      java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY
  }

  val baseColorScheme =
    when {
      customPrimary != null -> if (darkTheme) DarkColorScheme else LightColorScheme
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val finalColorScheme = androidx.compose.runtime.remember(baseColorScheme, customPrimary, customSecondary, darkTheme, isSunday, dynamicColor) {
      var scheme = baseColorScheme

      // When Dynamic Theme is enabled and today is Sunday, apply a special Sabbath Worship Gold accent
      if (dynamicColor && isSunday && customPrimary == null) {
          val sundayGold = if (darkTheme) androidx.compose.ui.graphics.Color(0xFFFFD700) else androidx.compose.ui.graphics.Color(0xFFC59B27)
          scheme = scheme.copy(
              primary = sundayGold,
              primaryContainer = sundayGold.copy(alpha = if (darkTheme) 0.35f else 0.2f)
          )
      }

      customPrimary?.let { p ->
          scheme = scheme.copy(
              primary = p,
              primaryContainer = if (darkTheme) p.copy(alpha = 0.35f) else p.copy(alpha = 0.15f),
              onPrimaryContainer = if (darkTheme) androidx.compose.ui.graphics.Color.White else p
          )
      }
      customSecondary?.let { s ->
          scheme = scheme.copy(
              secondary = s,
              secondaryContainer = if (darkTheme) s.copy(alpha = 0.35f) else s.copy(alpha = 0.15f),
              onSecondaryContainer = if (darkTheme) androidx.compose.ui.graphics.Color.White else s
          )
      }
      scheme
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val insetsController = WindowCompat.getInsetsController(window, view)
        // Automatic Luminance-based contrast:
        // Background luminance > 0.5f means bright background -> dark status bar icons (true)
        // Background luminance <= 0.5f means dark background -> white status bar icons (false)
        val bgLuminance = finalColorScheme.background.luminance()
        val isLightBg = bgLuminance > 0.5f
        insetsController.isAppearanceLightStatusBars = isLightBg

        val navBgLuminance = finalColorScheme.surface.luminance()
        insetsController.isAppearanceLightNavigationBars = navBgLuminance > 0.5f
      }
    }
  }

  MaterialTheme(colorScheme = finalColorScheme, typography = Typography, content = content)
}
