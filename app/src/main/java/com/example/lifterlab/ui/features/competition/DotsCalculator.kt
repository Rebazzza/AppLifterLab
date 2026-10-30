package com.example.lifterlab.ui.features.competition

import com.example.lifterlab.data.model.CompetitionAttempt
import kotlin.math.pow

/** RF10 — Sexo usado para seleccionar los coeficientes oficiales del puntaje DOTS. */
enum class CompetitionSex { MALE, FEMALE }

/**
 * RF09 / RF10 — Reglas de negocio del módulo de competencia.
 *
 * Calcula el total levantado (suma del mejor intento válido de cada básico) y el puntaje
 * oficial DOTS cruzando ese total con el peso corporal y el sexo del atleta. La fórmula
 * DOTS ajusta el total por el peso corporal mediante un polinomio de cuarto grado cuyos
 * coeficientes dependen del sexo (RNF05: sin errores de redondeo).
 */
object DotsCalculator {

    /** Básicos de la competencia, en orden de ejecución, con la llave usada en Firestore. */
    val LIFTS = listOf("squat", "bench", "deadlift")

    val LIFT_LABELS = mapOf(
        "squat" to "Sentadilla",
        "bench" to "Press Banca",
        "deadlift" to "Peso Muerto"
    )

    const val ATTEMPTS_PER_LIFT = 3
    const val MIN_BODYWEIGHT_KG = 35.0
    const val MAX_BODYWEIGHT_KG = 250.0
    const val MAX_ATTEMPT_KG = 600.0

    // Coeficientes oficiales del polinomio DOTS por sexo: a·BW⁴ + b·BW³ + c·BW² + d·BW + e.
    private val MALE = doubleArrayOf(
        -0.000001093, 0.0007391293, -0.1918759221, 24.0900756, -307.75076
    )
    private val FEMALE = doubleArrayOf(
        -0.0000010706, 0.0005158568, -0.1126655495, 13.6175032, -57.96288
    )

    /** Interpreta el sexo guardado en el perfil; cualquier valor no femenino se toma como masculino. */
    fun sexFromProfile(gender: String): CompetitionSex =
        if (gender.trim().startsWith("fem", ignoreCase = true)) CompetitionSex.FEMALE
        else CompetitionSex.MALE

    /** Mejor intento válido (marcado válido y con peso cargado) de una lista, o null. */
    fun bestSuccessful(attempts: List<CompetitionAttempt>): Double? =
        attempts.filter { it.isSuccessful && it.weightKg > 0.0 }
            .maxOfOrNull { it.weightKg }

    /** Total SBD: suma del mejor intento válido de cada básico. */
    fun total(attemptsByLift: Map<String, List<CompetitionAttempt>>): Double =
        LIFTS.sumOf { bestSuccessful(attemptsByLift[it].orEmpty()) ?: 0.0 }

    /**
     * Puntaje DOTS = total × 500 ÷ polinomio(BW, sexo). Devuelve 0.0 si faltan datos
     * válidos (total o peso corporal no positivos).
     */
    fun dotsScore(totalKg: Double, bodyweightKg: Double, sex: CompetitionSex): Double {
        if (totalKg <= 0.0 || bodyweightKg <= 0.0) return 0.0
        val c = if (sex == CompetitionSex.MALE) MALE else FEMALE
        val bw = bodyweightKg
        val denom = c[0] * bw.pow(4) + c[1] * bw.pow(3) + c[2] * bw.pow(2) + c[3] * bw + c[4]
        if (denom <= 0.0) return 0.0
        return totalKg * 500.0 / denom
    }

    /** Validación del peso de un intento; null si es válido o está vacío (aún sin cargar). */
    fun validateAttemptWeight(weightKg: Double?): String? = when {
        weightKg == null -> null
        weightKg <= 0.0 -> "El peso del intento debe ser mayor a 0 kg."
        weightKg > MAX_ATTEMPT_KG -> "El peso máximo por intento es ${MAX_ATTEMPT_KG.toInt()} kg."
        else -> null
    }

    /** Validación del peso corporal para calcular DOTS. */
    fun validateBodyweight(bodyweightKg: Double?): String? = when {
        bodyweightKg == null || bodyweightKg <= 0.0 -> "Ingresa tu peso corporal en kg."
        bodyweightKg < MIN_BODYWEIGHT_KG -> "El peso corporal mínimo es ${MIN_BODYWEIGHT_KG.toInt()} kg."
        bodyweightKg > MAX_BODYWEIGHT_KG -> "El peso corporal máximo es ${MAX_BODYWEIGHT_KG.toInt()} kg."
        else -> null
    }
}
