package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

  val finalColorScheme = androidx.compose.runtime.remember(baseColorScheme, customPrimary, customSecondary, darkTheme) {
      var scheme = baseColorScheme
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

  MaterialTheme(colorScheme = finalColorScheme, typography = Typography, content = content)
}
