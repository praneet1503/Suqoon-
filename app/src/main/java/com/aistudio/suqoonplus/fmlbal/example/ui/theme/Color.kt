package com.aistudio.suqoonplus.fmlbal.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

object ThemeConfig {
    var isDarkTheme by mutableStateOf(false)
}

val SoftNeutralBackground: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF121414) else Color(0xFFFDFBF7)

val AccentBlue = Color(0xFF8E9AAF)

val AccentBlueSoft: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF2D3142) else Color(0xFFE8EBF0)

val AccentGreen = Color(0xFF8FB9A8)

val AccentGreenSoft: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF1B2E26) else Color(0xFFEBF2EF)

val DarkSlate: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFFE2E3E3) else Color(0xFF4A5568)

val MutedGray: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF94A3B8)

// Additional support colors
val AmberBurnout = Color(0xFFD97706)

val LightAmber: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF451A03) else Color(0xFFFEF3C7)

val SoftRed = Color(0xFFE57373)

val OffWhite: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF1C1E1E) else Color(0xFFFAFAFA)

// Keep standard dark-mode colors for fallback or components if needed
val DarkPrimary = Color(0xFF90CAF9)
val DarkSecondary = Color(0xFFA5D6A7)
val DarkBackground = Color(0xFF121414)
val DarkSurface = Color(0xFF1C1E1E)
val DarkOnBackground = Color(0xFFE2E3E3)
val DarkOnSurface = Color(0xFFE2E3E3)
