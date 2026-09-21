package com.example.lifterlab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgBackground = Color(0xFF131315)
val BgSurfaceLow = Color(0xFF1C1B1D)
val BgSurfaceHigh = Color(0xFF2A2A2C)
val OutlineVariant = Color(0xFF464554)
val PrimaryAccent = Color(0xFF6567E3)
val PrimaryContainer = Color(0xFF8083FF)
val SecondaryAccent = Color(0xFFFFB77D)
val TertiaryAccent = Color(0xFF4EDEA3)
val ErrorAccent = Color(0xFFFFB4AB)
val TextOnBackground = Color(0xFFE5E1E4)
val TextOnSurfaceVariant = Color(0xFFC7C4D7)

private val LifterLabColors = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color(0xFF003824),
    secondary = SecondaryAccent,
    tertiary = TertiaryAccent,
    error = ErrorAccent,
    background = BgBackground,
    onBackground = TextOnBackground,
    surface = BgSurfaceLow,
    onSurface = TextOnBackground,
    surfaceVariant = BgSurfaceHigh,
    onSurfaceVariant = TextOnSurfaceVariant,
    outline = OutlineVariant,
    outlineVariant = OutlineVariant
)

@Composable
fun LifterLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LifterLabColors,
        content = content
    )
}