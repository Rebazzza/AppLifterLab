package com.example.lifterlab.ui.features.routines

import com.example.lifterlab.data.model.ExerciseSet
import com.example.lifterlab.data.model.Routine
import com.example.lifterlab.data.model.RoutineExercise
import com.example.lifterlab.data.model.WorkoutSession
import com.example.lifterlab.data.model.WarmupStep
import com.example.lifterlab.ui.features.tools.OneRepMax
import java.util.Locale

/**
 * Ejercicios FIJOS del selector de rutinas: no dependen de la BD, así que los
 * básicos (Sentadilla con Barra, Press de Banca, Peso Muerto…) siempre están
 * disponibles para armar rutinas. ExerciseRepository inyecta además esos
 * mismos nombres en `/exercises` (SBD_BASICS) para que también aparezcan en
 * la pantalla de Catálogo con detalle y foto; el selector detecta el nombre
 * exacto y no los duplica.
 */
val basicExercises = listOf(
    Pair("Sentadilla con Barra", "Objetivo: Cuádriceps, Glúteos"),
    Pair("Press de Banca", "Objetivo: Pecho, Tríceps"),
    Pair("Peso Muerto", "Objetivo: Espalda, Isquiotibiales"),
    Pair("Peso Muerto Rumano", "Objetivo: Isquiotibiales, Glúteos"),
    Pair("Press Militar", "Objetivo: Hombros, Tríceps"),
    Pair("Remo con Barra", "Objetivo: Dorsales, Espalda Alta"),
    Pair("Press Inclinado con Mancuernas", "Objetivo: Pecho Superior, Hombros"),
    Pair("Dominadas", "Objetivo: Dorsales, Bíceps"),
    Pair("Fondos", "Objetivo: Pecho, Tríceps"),
    Pair("Prensa de Piernas", "Objetivo: Cuádriceps"),
    Pair("Curl de Piernas", "Objetivo: Isquiotibiales"),
    Pair("Elevación de Talones", "Objetivo: Pantorrillas")
)

fun addExercise(routine: Routine, name: String, targetMuscles: String): Routine {
    val exercises = routine.exercises.toMutableList()
    exercises.add(
        RoutineExercise(
            name = name,
            targetMuscles = targetMuscles,
            executionOrder = exercises.size + 1,
            sets = listOf(
                ExerciseSet(setNumber = 1, weightKg = 100.0, reps = 5, rpe = 8.0),
                ExerciseSet(setNumber = 2, weightKg = 100.0, reps = 5, rpe = 8.5)
            )
        )
    )
    return routine.copy(exercises = exercises)
}

fun removeExercise(routine: Routine, exIndex: Int): Routine {
    if (exIndex !in routine.exercises.indices) return routine
    val exercises = routine.exercises.toMutableList()
    exercises.removeAt(exIndex)
    return routine.copy(exercises = exercises.renumberExecutionOrder())
}

/**
 * Reordenamiento de ejercicios (drag-and-drop o flechas Subir/Bajar).
 * Mueve el ejercicio de [fromIndex] a [toIndex] dentro de la rutina o sesión activa y
 * recalcula la numeración de ejecución secuencial (1..n) de todos los ejercicios.
 */
fun moveExercise(routine: Routine, fromIndex: Int, toIndex: Int): Routine {
    if (fromIndex == toIndex) return routine
    if (fromIndex !in routine.exercises.indices || toIndex !in routine.exercises.indices) return routine
    val exercises = routine.exercises.toMutableList()
    val movido = exercises.removeAt(fromIndex)
    exercises.add(toIndex, movido)
    return routine.copy(exercises = exercises.renumberExecutionOrder())
}

/** Recalcula executionOrder (1, 2, 3…) según la posición actual de cada ejercicio. */
private fun List<RoutineExercise>.renumberExecutionOrder(): List<RoutineExercise> =
    mapIndexed { index, exercise ->
        if (exercise.executionOrder == index + 1) exercise
        else exercise.copy(executionOrder = index + 1)
    }

/**
 * Transfiere las series de aproximación calculadas al ejercicio indicado,
 * insertándolas ANTES de las series existentes para respetar el orden de ejecución
 * (calentamiento → trabajo). Las series quedan pendientes de marcar como completadas.
 */
fun applyWarmupPlan(routine: Routine, exIndex: Int, steps: List<WarmupStep>): Routine {
    if (steps.isEmpty()) return routine
    if (exIndex !in routine.exercises.indices) return routine

    val exercises = routine.exercises.toMutableList()
    val targetEx = exercises[exIndex]

    val warmupSets = steps.map { step ->
        ExerciseSet(
            setNumber = 0,
            weightKg = step.weightKg,
            reps = step.reps,
            rpe = step.suggestedRpe ?: 0.0,
            isCompleted = false,
            isWarmup = true
        )
    }

    val sets = (warmupSets + targetEx.sets).mapIndexed { index, set ->
        if (set.setNumber == index + 1) set else set.copy(setNumber = index + 1)
    }

    exercises[exIndex] = targetEx.copy(sets = sets)
    return routine.copy(exercises = exercises)
}

fun updateSetData(
    routine: Routine,
    exIndex: Int,
    setIndex: Int,
    weightKg: Double? = null,
    reps: Int? = null,
    rpe: Double? = null,
    isCompleted: Boolean? = null
): Routine {
    val exercises = routine.exercises.toMutableList()
    if (exIndex !in exercises.indices) return routine
    val targetEx = exercises[exIndex]
    val sets = targetEx.sets.toMutableList()
    if (setIndex !in sets.indices) return routine

    val currentSet = sets[setIndex]
    val updatedSet = currentSet.copy(
        weightKg = weightKg ?: currentSet.weightKg,
        reps = reps ?: currentSet.reps,
        rpe = rpe ?: currentSet.rpe,
        isCompleted = isCompleted ?: currentSet.isCompleted
    )
    sets[setIndex] = updatedSet
    exercises[exIndex] = targetEx.copy(sets = sets)
    return routine.copy(exercises = exercises)
}

/**
 * Traslada a la plantilla los pesos y repeticiones que el atleta usó en la sesión, para
 * que la próxima vez que arranque esa rutina aparezcan las cargas reales (120 kg en
 * lugar de los 100 kg con los que fue creada). Se aplica al terminar el entrenamiento.
 *
 * No toca la estructura de la rutina: ni ejercicios añadidos o quitados durante la
 * sesión, ni las series de aproximación (que solo existen dentro del entrenamiento).
 * Un ejercicio de la plantilla que no aparezca en la sesión se queda como estaba, y un
 * valor vacío o en cero de la sesión tampoco pisa lo que la rutina ya tenía.
 */
fun applySessionValues(plantilla: Routine, sesion: Routine): Routine {
    fun clave(ejercicio: RoutineExercise): String =
        if (ejercicio.exerciseId.isNotEmpty()) ejercicio.exerciseId else ejercicio.name

    // Agrupa por clave y recorre ambas listas en orden de aparición: si la rutina repite
    // un mismo ejercicio, cada copia se empareja con su contraparte de la sesión.
    val porClave = sesion.exercises.groupBy { clave(it) }
    val ocupadas = mutableMapOf<String, Int>()

    val exercises = plantilla.exercises.map { ejercicio ->
        val k = clave(ejercicio)
        val posicion = ocupadas[k] ?: 0
        ocupadas[k] = posicion + 1
        val sesionEjercicio = porClave[k]?.getOrNull(posicion) ?: return@map ejercicio

        val cargas = sesionEjercicio.sets.filter { !it.isWarmup }
        val sets = ejercicio.sets.mapIndexed { index, set ->
            val carga = cargas.getOrNull(index) ?: return@mapIndexed set
            if (carga.weightKg > 0 && carga.reps > 0) {
                set.copy(weightKg = carga.weightKg, reps = carga.reps)
            } else {
                set
            }
        }
        ejercicio.copy(sets = sets)
    }
    return plantilla.copy(exercises = exercises)
}

fun addSetToExercise(routine: Routine, exIndex: Int): Routine {
    val exercises = routine.exercises.toMutableList()
    if (exIndex !in exercises.indices) return routine

    val targetEx = exercises[exIndex]
    val sets = targetEx.sets.toMutableList()
    val lastSet = sets.lastOrNull()

    sets.add(
        ExerciseSet(
            setNumber = sets.size + 1,
            weightKg = lastSet?.weightKg ?: 100.0,
            reps = lastSet?.reps ?: 5,
            rpe = lastSet?.rpe ?: 8.0,
            isCompleted = false
        )
    )
    exercises[exIndex] = targetEx.copy(sets = sets)
    return routine.copy(exercises = exercises)
}

fun calculateVolume(routine: Routine): String {
    var totalVolume = 0.0
    for (ex in routine.exercises) {
        for (set in ex.sets) {
            totalVolume += (set.weightKg * set.reps)
        }
    }
    return formatVolume(totalVolume)
}

/**
 * Volumen en vivo: solo cuenta el tonelaje de las series ya verificadas
 * (peso x repeticiones), que es el que se consolida al cerrar el entrenamiento.
 */
fun calculateCompletedVolume(routine: Routine): String {
    var totalVolume = 0.0
    for (ex in routine.exercises) {
        for (set in ex.sets) {
            if (set.isCompleted) totalVolume += (set.weightKg * set.reps)
        }
    }
    return formatVolume(totalVolume)
}

private fun formatVolume(totalVolume: Double): String {
    return if (totalVolume >= 1000) {
        String.format(Locale.ROOT, "%.1fk kg", totalVolume / 1000.0)
    } else {
        String.format(Locale.ROOT, "%.0f kg", totalVolume)
    }
}

/**
 * Referencia de la última sesión: mapea nombre de ejercicio -> series
 * registradas, para mostrarlas como referencia en las tarjetas del Modo Gimnasio.
 */
fun previousSessionReference(sessions: List<WorkoutSession>): Map<String, List<ExerciseSet>> {
    val lastSession = sessions.firstOrNull { it.exercises.isNotEmpty() } ?: return emptyMap()
    return lastSession.exercises.associate { exercise -> exercise.name to exercise.sets }
}

fun buildWorkoutSession(
    routine: Routine,
    isExpress: Boolean,
    durationMins: Int,
    records: Map<String, MarcaDeEjercicio> = emptyMap()
): WorkoutSession {
    // RF04 — serie por serie: se conservan TODAS las series de trabajo (ejecutadas y
    // omitidas) con su isCompleted, numeradas de forma secuencial 1..n. Las series de
    // aproximación quedan fuera: son calentamiento, no series del ejercicio, y su
    // numeración rompería la secuencia. Un ejercicio del que no se completó NINGUNA
    // serie no entra en la sesión (no hubo desempeño que registrar).
    val exercises = routine.exercises.mapNotNull { ex ->
        val sets = ex.sets.filter { !it.isWarmup }
        if (sets.isEmpty() || sets.none { it.isCompleted }) return@mapNotNull null
        ex.copy(sets = sets.mapIndexed { index, set -> set.copy(setNumber = index + 1) })
    }

    // Volumen consolidado: solo lo ejecutado (RF04 + acumulado de carga del RF29).
    val volume = exercises.sumOf { ex ->
        ex.sets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps }
    }

    // RF05 (RPE normalizado) y RF29 (1RM por serie con Brzycki + récord automático).
    val conMetricas = exercises.map { ex ->
        val record = records[ex.name]
        ex.copy(
            sets = ex.sets.map { set ->
                val ejecutada = set.isCompleted && set.weightKg > 0 && set.reps > 0
                set.copy(
                    rpe = normalizeRpe(set.rpe),
                    est1RM = if (ejecutada && set.reps <= OneRepMax.MAX_REPS) {
                        OneRepMax.brzycki(set.weightKg, set.reps)
                    } else {
                        0.0
                    },
                    isPR = esRecordPersonal(set, record)
                )
            }
        )
    }

    return WorkoutSession(
        routineName = if (isExpress) "Rutina Express" else routine.name,
        totalVolumeKg = volume,
        durationMins = durationMins,
        isFreeSession = isExpress,
        exercises = conMetricas
    )
}

/**
 * RF05 — Esfuerzo percibido: obligatorio en el rango @6.0 a @10.0 con incrementos de
 * 0.5. 0.0 significa "no registrado" y se deja tal cual. Cualquier otra entrada se
 * redondea al paso de 0.5 más cercano dentro del rango (p. ej. 7.3 → 7.5).
 */
fun normalizeRpe(rpe: Double): Double {
    if (rpe <= 0.0) return 0.0
    val dentroDeRango = rpe.coerceIn(6.0, 10.0)
    return Math.round(dentroDeRango * 2.0) / 2.0
}

/** RF29 — Máximos históricos de un movimiento: mayor peso de serie y mayor tonelaje de serie. */
data class MarcaDeEjercicio(
    val maxWeightKg: Double = 0.0,
    val maxSetVolumeKg: Double = 0.0
) {
    /** El récord más alto registrado; 0.0 significa que el movimiento aún no tiene historial. */
    val hasHistory: Boolean get() = maxWeightKg > 0.0 || maxSetVolumeKg > 0.0
}

/**
 * RF25 / RF29 — ¿Esta serie supera el récord histórico del movimiento?
 *
 * Es la MISMA condición con la que al cerrar el entrenamiento se marca [ExerciseSet.isPR],
 * de modo que la fila amarilla que ve el atleta en el Modo Gimnasio coincide con lo que
 * queda guardado en el historial. También se usa para encender el indicador en vivo.
 *
 * Una serie sin completar (o sin peso/repeticiones) nunca es récord, y un movimiento sin
 * historial tampoco: no hay marca que superar.
 */
fun esRecordPersonal(set: ExerciseSet, marca: MarcaDeEjercicio?): Boolean {
    if (marca == null || !marca.hasHistory) return false
    if (!set.isCompleted || set.weightKg <= 0 || set.reps <= 0) return false
    return set.weightKg > marca.maxWeightKg || set.weightKg * set.reps > marca.maxSetVolumeKg
}

/**
 * RF29 — Recorre el historial y devuelve, por nombre de ejercicio, el récord vigente a
 * partir del cual se decide si una serie nueva es Récord Personal.
 */
fun marcasHistoricas(sesiones: List<WorkoutSession>): Map<String, MarcaDeEjercicio> {
    val marcas = mutableMapOf<String, MarcaDeEjercicio>()
    sesiones.forEach { sesion ->
        sesion.exercises.forEach { ejercicio ->
            ejercicio.sets.filter { it.isCompleted && it.weightKg > 0 && it.reps > 0 }
                .forEach { set ->
                    val tonelaje = set.weightKg * set.reps
                    val actual = marcas[ejercicio.name] ?: MarcaDeEjercicio()
                    marcas[ejercicio.name] = MarcaDeEjercicio(
                        maxWeightKg = maxOf(actual.maxWeightKg, set.weightKg),
                        maxSetVolumeKg = maxOf(actual.maxSetVolumeKg, tonelaje)
                    )
                }
        }
    }
    return marcas
}

/** RF25/RF29 — Los tres movimientos básicos (SBD) que resume el Dashboard. */
enum class MovimientoBasico { SENTADILLA, PRESS_BANCA, PESO_MUERTO }

/** Palabras clave por movimiento; mismas que usa el catálogo para detectar los SBD. */
private val CLAVES_MOVIMIENTO_BASICO = mapOf(
    MovimientoBasico.SENTADILLA to listOf("squat", "sentadilla"),
    MovimientoBasico.PRESS_BANCA to listOf("bench press", "press de banca", "press banca"),
    MovimientoBasico.PESO_MUERTO to listOf("deadlift", "peso muerto")
)

/** Clasifica el nombre de un ejercicio en un movimiento básico; null si no es SBD. */
fun movimientoBasicoDe(nombre: String): MovimientoBasico? {
    val nombreNormalizado = nombre.lowercase(Locale.ROOT)
    return CLAVES_MOVIMIENTO_BASICO.entries
        .firstOrNull { (_, claves) -> claves.any { nombreNormalizado.contains(it) } }
        ?.key
}

/**
 * RF25/RF29 — Mejor peso realmente levantado por movimiento básico, tomado del
 * historial (solo series ejecutadas, vía [marcasHistoricas]). Un movimiento sin
 * registros no aparece en el mapa.
 */
fun mejoresPesosPorMovimiento(sesiones: List<WorkoutSession>): Map<MovimientoBasico, Double> {
    val resultado = mutableMapOf<MovimientoBasico, Double>()
    marcasHistoricas(sesiones).forEach { (nombre, marca) ->
        val movimiento = movimientoBasicoDe(nombre) ?: return@forEach
        resultado[movimiento] = maxOf(resultado[movimiento] ?: 0.0, marca.maxWeightKg)
    }
    return resultado
}