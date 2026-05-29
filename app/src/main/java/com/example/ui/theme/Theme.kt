package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ElegantPurple,
    secondary = DeepPurple,
    tertiary = CrimsonFlame,
    background = SlateDark,
    surface = CardDark,
    onPrimary = DeepPurple,
    onSecondary = ElegantPurple,
    onBackground = TextParchment,
    onSurface = TextParchment,
    outline = DarkBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DeepPurple,
    secondary = ElegantPurple,
    tertiary = CrimsonFlame,
    background = Color(0xFFFFFBFA), // Parchment White
    surface = Color(0xFFF6F8FA),
    onPrimary = Color.White,
    onSecondary = DeepPurple,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    outline = Color(0xFF49454F)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default so the Elegant Dark custom palette shines
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
