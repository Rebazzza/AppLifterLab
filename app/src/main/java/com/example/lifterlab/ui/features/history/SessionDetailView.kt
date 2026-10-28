package com.example.lifterlab.ui.features.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.data.repository.WarmupRepository
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.CardTitle
import com.example.lifterlab.ui.components.ChippedTag
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.components.SecondaryButton
import com.example.lifterlab.ui.components.bottomBarClearance
import com.example.lifterlab.ui.formatKg
import com.example.lifterlab.ui.formatSessionDate
import com.example.lifterlab.ui.formatShortDate
import com.example.lifterlab.ui.formatVolumeKg

/**
 * RF17 — Flujo de detalle de una sesión ya hecha.
 *
 * Al entrar se muestra la sesión en **modo solo lectura**: resumen y el detalle de cada
 * ejercicio (series, peso, repeticiones, RPE, volumen y 1RM estimado) sin ningún control
 * editable. Recién cuando el atleta toca "Editar Sesión" se abre el editor, que arranca
 * con todos los datos de la sesión cargados y las series marcadas según lo registrado.
 */
@Composable
fun SessionDetailFlow(
    session: WorkoutSession,
    userId: String,
    warmupRepository: WarmupRepository,
    onBack: () -> Unit,
    onChanged: () -> Unit
) {
    var editando by remember(session.id) { mutableStateOf(false) }

    // Botón atrás: del editor vuelve al detalle; del detalle, a la lista.
    BackHandler(enabled = editando) { editando = false }

    if (editando) {
        SessionEditor(
            session = session,
            userId = userId,
            warmupRepository = warmupRepository,
            onBack = { editando = false },
            onChanged = onChanged
        )
    } else {
        SessionDetailView(
            session = session,
            onBack = onBack,
            onEdit = { editando = true }
        )
    }
}

/** Detalle de una sesión finalizada en modo solo lectura, con la salida hacia el editor. */
@Composable
fun SessionDetailView(
    session: WorkoutSession,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val ejercicios = session.exercises.sortedBy { it.executionOrder }

    // RF04/RF29 — el resumen cuenta lo ejecutado: las series omitidas quedan en el
    // registro (para saber qué se saltó) pero no suman ni series ni tonelaje.
    val totalSeries = ejercicios.sumOf { ex -> ex.sets.count { it.isCompleted } }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = bottomBarClearance())
        ) {
            Text(
                text = "← Volver",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onBack() }
            )
            ScreenTitle(session.routineName.ifBlank { "Sesión" }, Modifier.padding(top = 8.dp))
            Text(
                text = "${formatSessionDate(session.date)} • ${formatShortDate(session.date)} • " +
                    "${session.durationMins} min",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )

            LifterCard(Modifier.padding(top = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardTitle("Resumen", Modifier.weight(1f))
                    ChippedTag(if (session.isFreeSession) "SESIÓN EXPRESS" else "SESIÓN DE RUTINA")
                }
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChippedTag(formatVolumeKg(session.totalVolumeKg))
                    ChippedTag("$totalSeries SERIES")
                    ChippedTag("${ejercicios.size} EJERCICIOS")
                }
                CardCaption(
                    "Vista de solo lectura: así quedó guardada la sesión. " +
                        "Para corregir algo, usa el botón de editar.",
                    Modifier.padding(top = 12.dp)
                )
            }

            PrimaryButton(
                text = "Editar Sesión",
                onClick = onEdit,
                modifier = Modifier.padding(top = 16.dp)
            )

            ejercicios.forEachIndexed { index, exercise ->
                SessionExerciseCard(numero = index + 1, exercise = exercise)
            }

            Spacer(Modifier.height(20.dp))
            SecondaryButton(
                text = "Volver",
                onClick = onBack,
                minHeight = 48.dp
            )
        }
    }
}

/** Tarjeta de solo lectura de un ejercicio: objetivo, detalle de series y totales. */
@Composable
private fun SessionExerciseCard(numero: Int, exercise: RoutineExercise) {
    val sets = exercise.sets
    val ejecutadas = sets.count { it.isCompleted }
    val volumen = sets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps }
    val mejor1rm = sets.maxOfOrNull { it.est1RM } ?: 0.0
    val hayRecord = sets.any { it.isPR }

    LifterCard(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle("$numero. ${exercise.name}", Modifier.weight(1f))
            ChippedTag("$ejecutadas/${sets.size} ${if (sets.size == 1) "SERIE" else "SERIES"}")
        }
        if (hayRecord) {
            Row(Modifier.padding(top = 6.dp)) {
                ChippedTag(
                    "RÉCORD PERSONAL",
                    textColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        CardCaption(
            exercise.targetMuscles.ifEmpty { "Objetivo: General" },
            Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(12.dp))
        sets.forEachIndexed { index, set ->
            SetDetailRow(set = set, mostrarDivisor = index < sets.lastIndex)
        }

        if (sets.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardCaption("VOLUMEN ${formatVolumeKg(volumen)}", Modifier.weight(1f), mono = true)
                if (mejor1rm > 0) {
                    CardCaption("1RM MÁX ${formatKg(mejor1rm)} KG", mono = true)
                }
            }
        }
    }
}

/**
 * Fila de una serie en el detalle: número, peso × repeticiones, esfuerzo percibido (RF05)
 * y sello de Récord Personal (RF29). Una serie omitida (isCompleted = false) se muestra
 * atenuada y tachada: queda en el registro, pero no cuenta como ejecutada.
 */
@Composable
private fun SetDetailRow(set: ExerciseSet, mostrarDivisor: Boolean) {
    val omitida = !set.isCompleted
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (omitida) "SERIE ${set.setNumber} · OMITIDA" else "SERIE ${set.setNumber}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1.4f)
            )
            Text(
                text = "${formatKg(set.weightKg)} KG × ${set.reps}",
                color = if (omitida) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                textDecoration = if (omitida) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(2.2f)
            )
            Text(
                text = if (set.rpe > 0) "@${formatKg(set.rpe)}" else "—",
                color = if (set.rpe > 0) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.2f)
            )
            Box(Modifier.width(26.dp)) {
                if (set.isPR) {
                    Text(
                        text = "PR",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        if (mostrarDivisor) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    }
}
