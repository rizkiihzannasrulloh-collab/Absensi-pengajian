package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkEmeraldPrimary,
    secondary = DarkGoldAccent,
    tertiary = GoldAccent,
    background = DarkEmeraldBackground,
    surface = DarkEmeraldCard,
    surfaceVariant = DarkEmeraldSurfaceVar,
    onPrimary = Color.White,
    onSecondary = DarkEmeraldBackground,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkGoldAccent.copy(alpha = 0.3f),
    error = StatusError
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    secondary = GoldPrimary,
    tertiary = GoldAccent,
    background = CreamIvoryBackground,
    surface = WhiteCard,
    surfaceVariant = SurfaceVariantCream,
    onPrimary = Color.White,
    onSecondary = TextDarkCharcoal,
    onBackground = TextDarkCharcoal,
    onSurface = TextDarkCharcoal,
    onSurfaceVariant = TextMutedGrey,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldDark,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = EmeraldPrimary,
    outline = GoldBorder,
    error = StatusError
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set dynamicColor to false to enforce our signature Islamic Emerald & Cream Gold design
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
