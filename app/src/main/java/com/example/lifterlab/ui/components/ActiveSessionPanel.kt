package com.example.lifterlab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.ActiveSessionTracker
import com.example.lifterlab.data.model.ActiveSession
import com.example.lifterlab.data.repository.ActiveSessionRepository
import com.example.lifterlab.ui.theme.NavBarSurface
import com.example.lifterlab.ui.theme.NavIconInactive
import com.example.lifterlab.ui.theme.OutlineVariant
import com.example.lifterlab.ui.theme.PrimaryAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Panel flotante de la sesión de entrenamiento en curso.
 *
 * Se monta una sola vez desde MainActivity, APILADO ENCIMA de la barra de navegación
 * flotante, y se ve en cualquier pantalla mientras haya una rutina iniciada. El
 * cronómetro que muestra corre segundo a segundo desde que arrancó la sesión y el
 * panel desaparece en cuanto la sesión se termina o se limpia (el repositorio
 * actualiza [ActiveSessionTracker] en esos momentos). Tocarlo regresa a la sesión.
 */
@Composable
fun ActiveSessionPanel(
    onOpenSession: (ActiveSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) { ActiveSessionRepository(context) }

    // Si la app se reinició con la sesión guardada, el espejo en memoria arranca vacío:
    // se hidrata una única vez desde el almacenamiento local.
    LaunchedEffect(Unit) {
        if (ActiveSessionTracker.sesion == null) {
            val guardada = withContext(Dispatchers.IO) { repository.getActiveSession() }
            guardada.onSuccess { sesion ->
                if (sesion != null && ActiveSessionTracker.sesion == null) {
                    ActiveSessionTracker.actualizar(sesion)
                }
            }
        }
    }

    val sesion = ActiveSessionTracker.sesion ?: return

    var ahora by remember(sesion.startedAtMillis) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sesion.startedAtMillis) {
        while (true) {
            delay(1000)
            ahora = System.currentTimeMillis()
        }
    }

    val transcurrido = ((ahora - sesion.startedAtMillis) / 1000L).coerceAtLeast(0L)
    val tiempo = String.format(
        java.util.Locale.ROOT,
        "%02d:%02d:%02d",
        transcurrido / 3600,
        (transcurrido % 3600) / 60,
        transcurrido % 60
    )
    val nombreRutina = if (sesion.isFreeSession) "Rutina Express" else sesion.routineName

    val forma = RoundedCornerShape(18.dp)
    Surface(
        shape = forma,
        color = NavBarSurface,
        border = BorderStroke(1.dp, OutlineVariant),
        shadowElevation = 14.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 102.dp)
            .fillMaxWidth()
            .clickable { onOpenSession(sesion) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PrimaryAccent.copy(alpha = 0.16f))
                    .border(BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.55f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = "ENTRENAMIENTO EN CURSO",
                    color = NavIconInactive,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Text(
                    text = nombreRutina,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text = tiempo,
                color = PrimaryAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
