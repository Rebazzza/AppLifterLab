package com.example.lifterlab.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

/** Azul titanio sobre superficies claras para toda la app. */
val BgBackground = Color(0xFFF8F9FD)
val FeaturedSurface = Color(0xFFEEF0FF)
val BgSurfaceLow = Color.White
val BgSurfaceHigh = Color(0xFFF1F3FC)
val BgSurfaceContainer = Color.White
val OutlineVariant = Color(0xFFE5E8F2)
val PrimaryAccent = Color(0xFF6366F1)
val PrimaryContainer = Color(0xFF6366F1)
val SecondaryAccent = Color(0xFF365FBB)
val TertiaryAccent = Color(0xFF16815E)
val ErrorAccent = Color(0xFFB42332)
val TextOnBackground = Color(0xFF171B2C)
val TextOnSurfaceVariant = Color(0xFF60667A)

val ColorSquat = Color(0xFF16815E)
val ColorBench = PrimaryAccent
val ColorDeadlift = Color(0xFFC64646)

val NavBarSurface = Color.White
val NavIconActive = TextOnBackground
val NavIconInactive = TextOnSurfaceVariant

private val LifterLabColors = lightColorScheme(
    primary = PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = SecondaryAccent,
    secondaryContainer = Color(0xFFEEF0FF),
    onSecondaryContainer = TextOnBackground,
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
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = BgSurfaceLow,
    surfaceContainer = BgSurfaceContainer,
    surfaceContainerHigh = BgSurfaceHigh,
    surfaceContainerHighest = Color(0xFFE8EBF6)
)

@Composable
fun LifterLabTheme(content: @Composable () -> Unit) {
    val window = (LocalContext.current as? Activity)?.window
    SideEffect {
        window?.let {
            val controller = WindowCompat.getInsetsController(it, it.decorView)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }
    MaterialTheme(colorScheme = LifterLabColors, content = content)
}
