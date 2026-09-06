package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ForensicDarkScheme = darkColorScheme(
  primary = CyberCyan,
  onPrimary = ForensicNavyDark,
  primaryContainer = Color(0xFF0C2442),
  onPrimaryContainer = CyberCyan,
  secondary = DeepCobalt,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF0F325E),
  onSecondaryContainer = Color(0xFFBAE6FD),
  tertiary = WarningAmber,
  onTertiary = Color.Black,
  background = ForensicNavyDark,
  onBackground = TextPrimaryDark,
  surface = ForensicSurface,
  onSurface = TextPrimaryDark,
  surfaceVariant = Color(0xFF16233B),
  onSurfaceVariant = TextSecondaryDark,
  outline = ForensicCardBorder,
  error = CorruptedRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = ForensicDarkScheme,
    typography = Typography,
    content = content
  )
}

