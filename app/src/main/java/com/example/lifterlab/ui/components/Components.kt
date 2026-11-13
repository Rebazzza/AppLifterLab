package com.example.lifterlab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Hardware
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.ActiveSessionTracker
import com.example.lifterlab.ui.theme.NavBarSurface
import com.example.lifterlab.ui.theme.NavIconActive
import com.example.lifterlab.ui.theme.NavIconInactive
import com.example.lifterlab.ui.theme.OutlineVariant
import com.example.lifterlab.ui.theme.PrimaryAccent
import com.example.lifterlab.ui.theme.FeaturedSurface

val CardCornerRadius = 12.dp
val FieldCornerRadius = 12.dp

/**
 * Cola reservada al final de una vista con scroll para que su último contenido nunca
 * quede tapado por la barra de navegación flotante (que se monta encima del contenido
 * desde MainActivity): la altura real del menú (inset de navegación + ~90 dp) más un
 * margen de 104 dp.
 *
 * Si hay un entrenamiento en curso, el panel flotante de la sesión se apila encima del
 * menú: se reserva además su altura para que el último contenido con scroll tampoco
 * quede tapado por ese panel.
 */
@Composable
fun bottomBarClearance(): Dp {
    val extraSesionActiva = if (ActiveSessionTracker.sesion != null) 76.dp else 0.dp
    return WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        104.dp + extraSesionActiva
}

/** Pestañas de la barra de navegación global. */
enum class LifterTab {
    Inicio,
    Rutinas,
    Herramientas,
    Perfil
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ScreenSubtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp
    )
}

@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun CardCaption(text: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
    )
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier,
        color = MaterialTheme.colorScheme.error,
        fontSize = 14.sp
    )
}

@Composable
fun SuccessText(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiary,
        fontSize = 14.sp
    )
}

@Composable
fun LifterCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    featured: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (featured) FeaturedSurface else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(CardCornerRadius)
            )
            .border(
                border = BorderStroke(1.dp, if (featured) PrimaryAccent.copy(alpha = 0.65f)
                    else accent?.copy(alpha = 0.45f) ?: MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(CardCornerRadius)
            )
            .padding(20.dp),
        content = content
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(CardCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 48.dp
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        shape = RoundedCornerShape(CardCornerRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LifterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(FieldCornerRadius),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = keyboardType
        ),
        visualTransformation = if (isPassword && !passwordVisible) {
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun RowScope.ChippedTag(
    text: String,
    textColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeColor: Color = MaterialTheme.colorScheme.outlineVariant,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, strokeColor), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}

/**
 * Tarjeta de estadística de un ejercicio básico (Sentadilla / Press Banca / Peso Muerto):
 * peso actual en monoespaciada, barra de progreso y texto de meta. El color de acento
 * sigue la codificación SBD de ui-design-system.md §3.
 */
@Composable
fun SbdStatCard(
    title: String,
    badgeText: String,
    badgeNeutral: Boolean,
    weightText: String,
    goalText: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    LifterCard(modifier, accent = accentColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardTitle(title, Modifier.weight(1f))
            val badgeColor =
                if (badgeNeutral) MaterialTheme.colorScheme.surfaceVariant else accentColor
            val badgeTextColor =
                if (badgeNeutral) MaterialTheme.colorScheme.onSurfaceVariant else NavIconActive
            Text(
                text = badgeText,
                modifier = Modifier
                    .background(color = badgeColor, shape = RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, badgeColor), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                color = badgeTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = weightText,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 38.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = " KG",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
        LifterProgressBar(fraction, color = accentColor)

        Text(
            text = goalText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

/** Barra de progreso segmentada 0f..1f, reutilizada por las tarjetas de estadísticas. */
@Composable
fun LifterProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val fillWeight = clamped.coerceAtLeast(0.001f)
    val emptyWeight = (1f - clamped).coerceAtLeast(0.001f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Box(
            Modifier
                .weight(fillWeight)
                .fillMaxSize()
                .background(
                    color = color,
                    shape = RoundedCornerShape(4.dp)
                )
        )
        Box(
            Modifier
                .weight(emptyWeight)
                .fillMaxSize()
        )
    }
}

/** Navegación global clara con azul titanio para la pestaña activa. */
@Composable
fun LifterBottomNavBar(
    currentTab: LifterTab?,
    onNavigateToDashboard: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
    lightAppearance: Boolean = false
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (lightAppearance) Color.White else MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        NavItem("Inicio", Icons.Outlined.Home, currentTab == LifterTab.Inicio, onNavigateToDashboard, lightAppearance)
        NavItem("Rutinas", Icons.Outlined.FitnessCenter, currentTab == LifterTab.Rutinas, onNavigateToRoutines, lightAppearance)
        NavItem("Herramientas", Icons.Outlined.Hardware, currentTab == LifterTab.Herramientas, onNavigateToTools, lightAppearance)
        NavItem("Perfil", Icons.Outlined.Person, currentTab == LifterTab.Perfil, onNavigateToProfile, lightAppearance)
    }
}

/** Barra lateral para pantallas anchas; conserva las mismas pestañas del menú inferior. */
@Composable
fun LifterNavigationRail(
    currentTab: LifterTab?,
    onNavigateToDashboard: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.padding(start = 12.dp),
        containerColor = NavBarSurface,
        contentColor = NavIconInactive
    ) {
        RailItem("Inicio", Icons.Outlined.Home, currentTab == LifterTab.Inicio, onNavigateToDashboard)
        RailItem("Rutinas", Icons.Outlined.FitnessCenter, currentTab == LifterTab.Rutinas, onNavigateToRoutines)
        RailItem("Herramientas", Icons.Outlined.Hardware, currentTab == LifterTab.Herramientas, onNavigateToTools)
        RailItem("Perfil", Icons.Outlined.Person, currentTab == LifterTab.Perfil, onNavigateToProfile)
    }
}

@Composable
private fun RailItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    NavigationRailItem(
        selected = isActive,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 10.sp) },
        alwaysShowLabel = false,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = PrimaryAccent,
            indicatorColor = PrimaryAccent,
            unselectedIconColor = NavIconInactive,
            unselectedTextColor = NavIconInactive
        )
    )
}

@Composable
private fun RowScope.NavItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    lightAppearance: Boolean
) {
    NavigationBarItem(
        selected = isActive,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label, maxLines = 1) },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = if (lightAppearance) PrimaryAccent else MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = if (lightAppearance) PrimaryAccent else MaterialTheme.colorScheme.onSurface,
            indicatorColor = if (lightAppearance) Color(0xFFEEF0FF) else MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = if (lightAppearance) Color(0xFF60667A) else MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = if (lightAppearance) Color(0xFF60667A) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
