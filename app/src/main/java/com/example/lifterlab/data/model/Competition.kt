package com.example.lifterlab.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.Timestamp

/**
 * RF09 / RF10 — Competencia de powerlifting.
 *
 * Guarda los intentos de Sentadilla, Press Banca y Peso Muerto, el total levantado y el
 * puntaje DOTS calculado con el peso corporal del atleta.
 *
 * [attempts] usa como llaves "squat", "bench" y "deadlift" y cada valor es la lista de
 * los 3 intentos oficiales en orden.
 */
data class Competition(
    /** ID del documento en Firestore; no se persiste dentro del documento. */
    @get:Exclude val id: String = "",
    val name: String = "",
    val date: Timestamp = Timestamp.now(),
    val athleteWeightKg: Double = 0.0,
    /** Total SBD calculado al registrar, para conservar el resultado de la competencia. */
    val totalKg: Double = 0.0,
    val dotsScore: Double = 0.0,
    val attempts: Map<String, List<CompetitionAttempt>> = emptyMap()
)
