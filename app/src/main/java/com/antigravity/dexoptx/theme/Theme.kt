package com.antigravity.dexoptx.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LiquidGlassColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    secondary = ElectricViolet,
    onSecondary = Color.White,
    tertiary = RadiantFuchsia,
    background = DarkMidnight,
    onBackground = TextPrimary,
    surface = DarkMidnight,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

@Composable
fun DexOptXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LiquidGlassColorScheme,
        typography = Typography,
        content = content
    )
}
