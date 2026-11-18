package com.replock.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// True Liquid Glass color system
private val LiquidGlassColors = darkColorScheme(
    primary = Color(0xFF7C9CFF),
    onPrimary = Color(0xFF0A0A12),
    primaryContainer = Color(0xFF1A1F3A),
    onPrimaryContainer = Color(0xFFD6E0FF),
    secondary = Color(0xFF64D2FF),
    onSecondary = Color(0xFF001F2A),
    tertiary = Color(0xFFFF8A9B),
    background = Color(0xFF05050A),
    surface = Color(0xFF0C0C14),
    surfaceVariant = Color(0xFF161622),
    onBackground = Color(0xFFE8E8F0),
    onSurface = Color(0xFFE8E8F0),
    onSurfaceVariant = Color(0xFFA0A0B8),
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1AFFFFFF)
)

@Composable
fun RepLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LiquidGlassColors,
        content = content
    )
}
