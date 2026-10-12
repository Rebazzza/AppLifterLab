package com.example.lifterlab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Tokens oficiales según la skill `ui-design-system.md` ("Titanio y Acero"):
 * fondo casi negro OLED (#09090B), superficies #121212 con borde #2A2A2A,
 * acento de marca #6366F1 y texto principal #E5E1E4.
 */
val BgBackground = Color(0xFF09090B)       // bg_background
val BgSurfaceLow = Color(0xFF121212)       // bg_surface_container (tarjetas)
val BgSurfaceHigh = Color(0xFF2A2A2C)      // contenedores destacados
val BgSurfaceContainer = Color(0xFF1C1B1D) // chips y barra de navegación
val OutlineVariant = Color(0xFF2A2A2A)     // outline_variant (bordes 1dp)
val PrimaryAccent = Color(0xFF6366F1)      // primary_accent (brand core)
val PrimaryContainer = Color(0xFF8083FF)
val SecondaryAccent = Color(0xFFFFB77D)
val TertiaryAccent = Color(0xFF4EDEA3)
val ErrorAccent = Color(0xFFFFB4AB)
val TextOnBackground = Color(0xFFE5E1E4)
val TextOnSurfaceVariant = Color(0xFFC7C4D7)

// Asignación cromática de los 3 básicos (SBD) — ui-design-system.md §3.
val ColorSquat = Color(0xFF10B981)    // Sentadilla - Verde Neón
val ColorBench = Color(0xFFD97706)    // Press Banca - Naranja / Ámbar
val ColorDeadlift = Color(0xFFEF4444) // Peso Muerto - Rojo Coral

// Barra de navegación inferior (pill/cápsula).
val NavBarSurface = Color(0xFF1C1B1D)
val NavIconActive = Color(0xFF0B0B0F)
val NavIconInactive = Color(0xFF8A8A93)

private val LifterLabColors = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color(0xFF0B0B2E),
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
    outlineVariant = OutlineVariant,
    surfaceContainerLowest = Color(0xFF0B0B0D),
    surfaceContainerLow = BgSurfaceLow,
    surfaceContainer = BgSurfaceContainer,
    surfaceContainerHigh = BgSurfaceHigh,
    surfaceContainerHighest = Color(0xFF353437)
)

@Composable
fun LifterLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LifterLabColors,
        content = content
    )
}
