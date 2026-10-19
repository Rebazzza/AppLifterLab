package com.example.lifterlab.ui.features.routines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tokens visuales del módulo de rutinas: conservan la estructura "steel" de
 * Ejemplo.html (tarjetas oscuras con borde sutil, campos "input-dark" y halo de
 * foco) pero alineados a la paleta oficial de `ui-design-system.md`: fondo #09090B,
 * superficies #121212 con borde #2A2A2A y acento de marca #6366F1. Se aíslan aquí
 * para que Crear, Editar e Iniciar compartan la misma estética sin tocar el tema global.
 */
object RoutineSkin {
    val Background = Color(0xFF09090B)          // bg_background
    val SteelSurface = Color(0xFF121212)        // bg_surface_container
    val SteelBorder = Color(0xFF2A2A2A)         // outline_variant
    val InputBackground = Color(0xFF0B0B0D)     // surface_container_lowest
    val SurfaceContainerLowest = Color(0xFF0B0B0D)
    val SurfaceContainerLow = Color(0xFF121212)
    val SurfaceContainer = Color(0xFF1C1B1D)
    val SurfaceContainerHigh = Color(0xFF2A2A2C)
    val SurfaceContainerHighest = Color(0xFF353437)
    val Primary = Color(0xFF6366F1)             // primary_accent (rellenos)
    val PrimaryText = Color(0xFFA5B4FC)         // acento legible sobre fondos oscuros
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFF8083FF)
    val InversePrimary = Color(0xFF6366F1)      // botones principales
    val Tertiary = Color(0xFF4EDEA3)
    val Secondary = Color(0xFFFFB77D)
    val OutlineVariant = Color(0xFF2A2A2A)
    val OnSurface = Color(0xFFE5E1E4)
    val OnSurfaceVariant = Color(0xFFC7C4D7)
    val Error = Color(0xFFFFB4AB)
    val RowAlt = Color(0x14FFFFFF)
}

/** Halo suave del Ejemplo.html (`box-shadow: 0 0 15px`). */
fun Modifier.routineGlow(
    shape: Shape,
    color: Color = RoutineSkin.PrimaryText,
    elevation: Dp = 10.dp
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = color,
    spotColor = color
)

/** Contenedor "steel-card": superficie oscura con borde sutil y esquinas redondeadas. */
@Composable
fun SteelCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RoutineSkin.SteelSurface)
            .border(BorderStroke(1.dp, RoutineSkin.SteelBorder), shape)
            .padding(contentPadding),
        content = content
    )
}

/** Etiqueta monoespaciada en mayúsculas (label-mono del Ejemplo.html). */
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = RoutineSkin.OnSurfaceVariant,
    fontSize: Int = 11,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        textAlign = textAlign
    )
}

/** Botón circular de acción (info / swap_horiz / editar / borrar del Ejemplo.html). */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = RoutineSkin.OnSurface,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(RoutineSkin.SurfaceContainer)
            .border(BorderStroke(1.dp, RoutineSkin.OutlineVariant), CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Campo numérico "input-dark" del Ejemplo.html: fondo casi negro, borde sutil y halo
 * violeta cuando recibe el foco.
 */
@Composable
fun RowScope.DarkNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    weight: Float,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    textColor: Color = RoutineSkin.OnSurface,
    focusColor: Color = RoutineSkin.Primary,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(6.dp)
    val border = if (focused) focusColor else RoutineSkin.SteelBorder

    Box(
        modifier = Modifier
            .weight(weight)
            .then(if (focused) Modifier.routineGlow(shape, focusColor, 8.dp) else Modifier)
            .clip(shape)
            .background(RoutineSkin.InputBackground)
            .border(BorderStroke(1.dp, border), shape)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            enabled = enabled,
            interactionSource = interactionSource,
            textStyle = TextStyle(
                color = if (focused && enabled) focusColor else textColor,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            color = RoutineSkin.OnSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/** Valor no editable dentro de una caja "input-dark" (referencia rápida de la serie). */
@Composable
fun RowScope.DarkValueBox(
    text: String,
    weight: Float,
    textColor: Color = RoutineSkin.OnSurfaceVariant
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .weight(weight)
            .clip(shape)
            .background(RoutineSkin.InputBackground)
            .border(BorderStroke(1.dp, RoutineSkin.SteelBorder), shape)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

/** Botón ancho de acción principal (Finalizar / Guardar) con fondo inverse-primary. */
@Composable
fun AccentWideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.routineGlow(shape, RoutineSkin.InversePrimary, 12.dp)
                else Modifier
            )
            .clip(shape)
            .background(
                if (enabled) RoutineSkin.InversePrimary
                else RoutineSkin.InversePrimary.copy(alpha = 0.4f)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = RoutineSkin.OnPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
        )
        if (trailingIcon != null) {
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = RoutineSkin.OnPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Botón secundario "steel" (mismo lenguaje que Add Set / Skip Rest del Ejemplo.html). */
@Composable
fun SteelOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = RoutineSkin.PrimaryText,
    leadingIcon: ImageVector? = null
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RoutineSkin.SurfaceContainer)
            .border(BorderStroke(1.dp, RoutineSkin.OutlineVariant), shape)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Barra superior del módulo: marca "LifterLab" con filete inferior, replicada en todas
 * las vistas de la app.
 *
 * El botón de regreso solo se muestra en los módulos que se abren desde la vista de un
 * botón del menú (p. ej. Calculadora de Aproximación o Editar Perfil). Las vistas raíz
 * de la barra flotante (Inicio, Rutinas, Herramientas, Perfil) se invocan sin [onBack]
 * y muestran únicamente la marca.
 */
@Composable
fun RoutineTopBar(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RoutineSkin.Background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    onClick = onBack
                )
                Spacer(Modifier.size(10.dp))
            }
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = RoutineSkin.PrimaryText,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "LIFTERLAB",
                color = RoutineSkin.PrimaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RoutineSkin.OutlineVariant)
        )
    }
}
