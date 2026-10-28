package com.example.lifterlab

import com.example.lifterlab.data.model.ExerciseSet
import com.google.firebase.firestore.util.CustomClassMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regresión de RF25 (indicador de récord personal).
 *
 * Firestore deserializa clases de Kotlin a través de getters/setters. Al declarar un
 * booleano como `val isCompleted`, Kotlin genera el getter `isCompleted()` y Firestore
 * deriva el nombre de campo `completed`, pero al leer el documento busca un
 * setter/field llamado `completed` que no existe (el campo real se llama `isCompleted`),
 * así que el valor vuelve siempre en `false`. Eso impedía detectar récords al leer el
 * historial. La corrección es declararlos `var` (genera `setCompleted()`, etc.).
 *
 * Si este test falla, revisa que `isPR`, `isCompleted` y `isWarmup` en [ExerciseSet]
 * sigan siendo `var` y no se hayan vuelto a declarar como `val`.
 */
class FirestoreExerciseSetMappingTest {

    @Test
    fun `los indicadores booleanos sobreviven el ida y vuelta por Firestore`() {
        val original = ExerciseSet(
            setNumber = 1,
            weightKg = 100.0,
            reps = 5,
            rpe = 8.0,
            est1RM = 112.5,
            isPR = true,
            isCompleted = true,
            isWarmup = true
        )

        @Suppress("UNCHECKED_CAST")
        val plain = CustomClassMapper.convertToPlainJavaTypes(original) as Map<String, Any?>

        // Nombres de campo estables: son los que ya existen en el historial guardado,
        // así que el arreglo recupera los datos previos sin migraciones.
        assertEquals(true, plain["pr"])
        assertEquals(true, plain["completed"])
        assertEquals(true, plain["warmup"])

        val back = CustomClassMapper.convertToCustomClass(plain, ExerciseSet::class.java, null)

        assertTrue("isPR debe conservarse", back.isPR)
        assertTrue("isCompleted debe conservarse", back.isCompleted)
        assertTrue("isWarmup debe conservarse", back.isWarmup)
    }
}
