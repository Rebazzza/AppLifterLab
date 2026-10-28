package com.example.lifterlab.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.lifterlab.data.model.ActiveSession

/**
 * Espejo en memoria de la sesión de entrenamiento en curso.
 *
 * [com.example.lifterlab.data.repository.ActiveSessionRepository] lo mantiene al día en
 * cada guardado y en cada limpieza, así el panel flotante global (montado desde
 * MainActivity) aparece y desaparece en el instante exacto en que el atleta inicia,
 * termina o descarta un entrenamiento, sin tener que estar leyendo el almacenamiento
 * local todo el tiempo.
 */
object ActiveSessionTracker {

    var sesion: ActiveSession? by mutableStateOf(null)
        private set

    fun actualizar(nueva: ActiveSession?) {
        sesion = nueva
    }
}
