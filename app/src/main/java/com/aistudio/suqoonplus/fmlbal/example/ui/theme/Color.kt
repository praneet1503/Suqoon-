package com.aistudio.suqoonplus.fmlbal.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

object ThemeConfig {
    var isDarkTheme by mutableStateOf(false)
}

val SoftNeutralBackground: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF121414) else Color(0xFFF4F7F6)

val AccentBlue = Color(0xFF3B82F6)

val AccentBlueSoft: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF1E293B) else Color(0xFFE1EFF9)

val AccentGreen = Color(0xFF10B981)

val AccentGreenSoft: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF064E3B) else Color(0xFFE2F7EA)

val DarkSlate: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFFE2E3E3) else Color(0xFF2C3E50)

val MutedGray: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF9E9E9E) else Color(0xFF7F8C8D)

// Additional support colors
val AmberBurnout = Color(0xFFF59E0B)

val LightAmber: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF451A03) else Color(0xFFFEF3C7)

val SoftRed = Color(0xFFEF4444)

val OffWhite: Color
  get() = if (ThemeConfig.isDarkTheme) Color(0xFF1C1E1E) else Color(0xFFFFFFFF)

// Keep standard dark-mode colors for fallback or components if needed
val DarkPrimary = Color(0xFF90CAF9)
val DarkSecondary = Color(0xFFA5D6A7)
val DarkBackground = Color(0xFF121414)
val DarkSurface = Color(0xFF1C1E1E)
val DarkOnBackground = Color(0xFFE2E3E3)
val DarkOnSurface = Color(0xFFE2E3E3)
