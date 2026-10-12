package com.example.lifterlab.data.model

import kotlin.math.roundToInt

/** Fase de cada serie dentro del plan de aproximación. */
enum class WarmupPhase(val label: String) {
    ACTIVACION("ACTIVACIÓN"),
    APROXIMACION("APROXIMACIÓN"),
    TRABAJO("TRABAJO")
}

/** Serie generada por la Calculadora de Aproximación. */
data class WarmupStep(
    val stepNumber: Int,
    val phase: WarmupPhase,
    val weightKg: Double,
    val reps: Int,
    val pctOfTop: Double,
    val suggestedRpe: Double? = null
)

/** Incremento mínimo de carga: 2.5 kg (1.25 kg por lado). */
private const val PLATE_INCREMENT_KG = 2.5

/** Discos de la barra olímpica, del más pesado al más liviano (codificación IPF). */
val ipfPlateSizesKg: List<Double> = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25, 0.5, 0.25)

/** Redondea al múltiplo de 2.5 kg más cercano y nunca baja de la barra base. */
fun roundToPlateStep(weightKg: Double, barKg: Double = 20.0): Double {
    val rounded = (weightKg / PLATE_INCREMENT_KG).roundToInt() * PLATE_INCREMENT_KG
    return maxOf(rounded, barKg)
}

/**
 * Genera la progresión escalonada desde la barra vacía hasta la carga top:
 * activación con la barra (x12), aproximaciones al 45%, 70% y 80% del top y la serie
 * de trabajo principal con las repeticiones y el RPE objetivo.
 */
fun buildWarmupPlan(
    topKg: Double,
    targetReps: Int,
    targetRpe: Double,
    barKg: Double = 20.0
): List<WarmupStep> {
    if (topKg <= 0.0 || targetReps <= 0) return emptyList()

    if (topKg <= barKg + PLATE_INCREMENT_KG) {
        return listOf(
            WarmupStep(
                stepNumber = 1,
                phase = WarmupPhase.TRABAJO,
                weightKg = roundToPlateStep(topKg, barKg),
                reps = targetReps,
                pctOfTop = 1.0,
                suggestedRpe = targetRpe
            )
        )
    }

    val steps = mutableListOf<WarmupStep>()
    steps.add(
        WarmupStep(
            stepNumber = 1,
            phase = WarmupPhase.ACTIVACION,
            weightKg = barKg,
            reps = 12,
            pctOfTop = barKg / topKg
        )
    )

    // Escalonado de intensidad: prepara SNC y articulaciones sin fatiga prematura.
    val ladder = listOf(
        Triple(0.45, 8, WarmupPhase.APROXIMACION),
        Triple(0.70, 6, WarmupPhase.APROXIMACION),
        Triple(0.80, targetReps.coerceAtMost(6), WarmupPhase.APROXIMACION)
    )

    var lastWeight = barKg
    for ((fraction, reps, phase) in ladder) {
        val weight = roundToPlateStep(topKg * fraction, barKg)
        if (weight <= lastWeight) continue
        steps.add(
            WarmupStep(
                stepNumber = steps.size + 1,
                phase = phase,
                weightKg = weight,
                reps = reps,
                pctOfTop = fraction
            )
        )
        lastWeight = weight
    }

    steps.add(
        WarmupStep(
            stepNumber = steps.size + 1,
            phase = WarmupPhase.TRABAJO,
            weightKg = roundToPlateStep(topKg, barKg),
            reps = targetReps,
            pctOfTop = 1.0,
            suggestedRpe = targetRpe
        )
    )
    return steps
}

/** Tabla de intensidades de referencia del 75% al 95% del top (pasos de 5%). */
fun intensityTable(topKg: Double): List<WarmupStep> {
    if (topKg <= 0.0) return emptyList()
    return listOf(0.75, 0.80, 0.85, 0.90, 0.95).mapIndexed { index, fraction ->
        WarmupStep(
            stepNumber = index + 1,
            phase = WarmupPhase.APROXIMACION,
            weightKg = roundToPlateStep(topKg * fraction),
            // Reps máximas estimadas con Epley (1RM = w · (1 + reps/30)).
            reps = (30.0 * (1.0 / fraction - 1.0)).roundToInt().coerceAtLeast(1),
            pctOfTop = fraction
        )
    }
}

/**
 * Desglose de discos por lado para cargar la barra (integración con el
 * visualizador IPF). Devuelve pares (kg del disco, cantidad por lado).
 */
fun plateBreakdown(totalKg: Double, barKg: Double = 20.0): List<Pair<Double, Int>> {
    var perSide = (totalKg - barKg) / 2.0
    if (perSide <= 0.0) return emptyList()

    val breakdown = mutableListOf<Pair<Double, Int>>()
    for (plate in ipfPlateSizesKg) {
        var count = 0
        while (perSide >= plate - 0.001) {
            count++
            perSide -= plate
        }
        if (count > 0) breakdown.add(plate to count)
    }
    return breakdown
}
