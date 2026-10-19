package com.example.lifterlab.ui.features.tools

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** RF06 — Estimación de 1RM. Con 1 repetición ambas fórmulas devuelven el peso levantado. */
object OneRepMax {
    const val MAX_WEIGHT_KG = 1000.0
    /** Por encima de ~12 repeticiones las fórmulas pierden precisión. */
    const val MAX_REPS = 12

    /** Porcentajes de aproximación del 1RM (agent.md, RF06/RF08), incluido el 100% (el propio 1RM). */
    val APPROACH_PERCENTS = listOf(70, 75, 80, 85, 90, 95, 100)

    fun brzycki(weightKg: Double, reps: Int): Double = weightKg * 36.0 / (37.0 - reps)

    fun epley(weightKg: Double, reps: Int): Double =
        if (reps == 1) weightKg else weightKg * (1.0 + reps / 30.0)

    /** Devuelve el mensaje de error de validación, o null si peso y repeticiones son válidos. */
    fun validate(weightKg: Double?, reps: Int?): String? = when {
        weightKg == null || weightKg <= 0.0 -> "Ingresa un peso mayor a 0 kg."
        weightKg > MAX_WEIGHT_KG -> "El peso máximo permitido es ${MAX_WEIGHT_KG.toInt()} kg."
        reps == null || reps < 1 -> "Ingresa al menos 1 repetición."
        reps > MAX_REPS -> "Usa entre 1 y $MAX_REPS repeticiones: con más, la estimación pierde precisión."
        else -> null
    }
}

/** RF07 — Discos IPF disponibles, de mayor a menor, con su color oficial. */
enum class IpfPlate(val kg: Double, val argb: Long) {
    RED_25(25.0, 0xFFD32F2F),
    BLUE_20(20.0, 0xFF1976D2),
    YELLOW_15(15.0, 0xFFFBC02D),
    GREEN_10(10.0, 0xFF388E3C),
    WHITE_5(5.0, 0xFFF5F5F5),
    BLACK_2_5(2.5, 0xFF1A1A1A),
    SILVER_1_25(1.25, 0xFFC0C0C0)
}

/**
 * Resultado de armar un peso objetivo en la barra.
 * [perSide] va del disco más cercano al centro hacia el extremo de la barra.
 */
data class PlateLoad(
    val targetKg: Double,
    val barKg: Double,
    val collarsTotalKg: Double,
    val perSide: List<IpfPlate>,
    val loadedKg: Double,
    val isExact: Boolean,
    /** Peso armable inmediatamente inferior / superior cuando [isExact] es false. */
    val nearestLowerKg: Double?,
    val nearestUpperKg: Double?
)

object PlateCalculator {
    const val BAR_KG = 20.0
    const val COLLARS_TOTAL_KG = 5.0
    const val MAX_TARGET_KG = 1000.0

    /** Unidad mínima por lado: el disco más pequeño (2.5 kg). */
    private val smallestPlateKg = IpfPlate.entries.minOf { it.kg }

    fun validate(targetKg: Double?): String? = when {
        targetKg == null || targetKg <= 0.0 -> "Ingresa un peso objetivo mayor a 0 kg."
        targetKg > MAX_TARGET_KG -> "El peso máximo permitido es ${MAX_TARGET_KG.toInt()} kg."
        else -> null
    }

    fun compute(targetKg: Double, withCollars: Boolean, barKg: Double = BAR_KG): PlateLoad {
        val collars = if (withCollars) COLLARS_TOTAL_KG else 0.0
        val base = barKg + collars
        val sideKg = (targetKg - base) / 2.0
        val steps = sideKg / smallestPlateKg

        val exact = steps >= 0.0 && abs(steps - steps.roundToInt()) < 1e-6
        val lowerSteps = if (steps < 0.0) null else floor(steps + 1e-6).toInt()
        val upperSteps = if (steps < 0.0) 0 else ceil(steps - 1e-6).toInt()

        fun totalOf(sideSteps: Int) = base + 2.0 * sideSteps * smallestPlateKg

        val loadSteps = if (exact) steps.roundToInt() else lowerSteps ?: 0
        val perSide = platesFor(loadSteps)
        return PlateLoad(
            targetKg = targetKg,
            barKg = barKg,
            collarsTotalKg = collars,
            perSide = perSide,
            loadedKg = totalOf(loadSteps),
            isExact = exact,
            nearestLowerKg = if (exact) null else lowerSteps?.let(::totalOf),
            nearestUpperKg = if (exact) null else totalOf(upperSteps)
        )
    }

    /** Descomposición voraz en unidades de 2.5 kg (válida porque 2.5 kg es el disco mínimo). */
    private fun platesFor(sideSteps: Int): List<IpfPlate> {
        var remaining = sideSteps
        val result = mutableListOf<IpfPlate>()
        for (plate in IpfPlate.entries) {
            val units = (plate.kg / smallestPlateKg).roundToInt()
            while (remaining >= units) {
                result.add(plate)
                remaining -= units
            }
        }
        return result
    }
}
