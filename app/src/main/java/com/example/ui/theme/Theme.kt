package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color

val LocalAppFont = compositionLocalOf<FontFamily> { FontFamily.Monospace }
val LocalCardBackgroundColor = compositionLocalOf { Color.Black }
val LocalCardBorderColor = compositionLocalOf { Color.White.copy(alpha = 0.08f) }
val LocalCardTextColor = compositionLocalOf { Color.White }
val LocalCardSecondaryTextColor = compositionLocalOf { Color.Gray }

fun getAppFontFamily(fontSetting: String?): FontFamily {
    return FontFamily.Monospace
}

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  appFont: FontFamily = FontFamily.Monospace,
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

  androidx.compose.runtime.CompositionLocalProvider(
    LocalAppFont provides appFont
  ) {
    MaterialTheme(colorScheme = colorScheme, typography = getTypography(appFont), content = content)
  }
}
