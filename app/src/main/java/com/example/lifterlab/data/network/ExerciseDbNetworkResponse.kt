package com.example.lifterlab.data.network

import com.google.gson.annotations.SerializedName

/**
 * Data class que mapea la respuesta JSON de la API externa ExerciseDB (RapidAPI).
 * Los nombres de campo coinciden exactamente con los del payload remoto.
 */
data class ExerciseDbNetworkResponse(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("bodyPart")
    val bodyPart: String = "",

    @SerializedName("equipment")
    val equipment: String = "",

    @SerializedName("target")
    val target: String = "",

    @SerializedName("gifUrl")
    val gifUrl: String = "",

    @SerializedName("secondaryMuscles")
    val secondaryMuscles: List<String> = emptyList(),

    @SerializedName("instructions")
    val instructions: List<String> = emptyList()
)
