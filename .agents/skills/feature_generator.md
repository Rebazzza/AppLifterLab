---
name: feature-generator
description: "Genera módulos o pantallas nuevas para LifterLab en Jetpack Compose (flujo reto-codigo)."
trigger: /feature-generator
---

# Skill: Generador de Módulos Compose para LifterLab

Genera nuevos módulos o pantallas para LifterLab siguiendo la arquitectura ACTUAL (Jetpack Compose + Material 3 + repositorios con Result<T>), usando el flujo pedagógico de la skill reto-codigo: propone el cambio en términos de pantalla/comportamiento, el estudiante lo implementa y después lo evalúa. Úsalo cuando un estudiante quiera añadir un módulo o funcionalidad a la app.

## Propósito
Cuando se pida un módulo nuevo (ej. "Implementa el RF06: Calculadora de fuerza", "agrega el módulo de competencias"), **no generes el código completo de una vez**. Sigue el flujo de la skill `reto-codigo`: propón la pantalla como un cambio pequeño y concreto descrito en términos de lo que se ve y lo que pasa al tocar, deja que el estudiante lo implemente y luego evalúalo leyendo su código. Toda implementación debe respetar la arquitectura Compose actual descrita abajo.

> Lee y aplica `reto_codigo.md` para el flujo (plantear → si se traba dar fragmento breve → evaluar). Este skill solo define **qué** módulos pedir y **bajo qué arquitectura** deben implementarse.

## Arquitectura ACTUAL (a respetar — NO la anterior)

Antes el proyecto usaba Views programativas (LinearLayout/TextView/EditText, FormView, Activities). Eso ya fue eliminado. Ahora:

- **UI:** Jetpack Compose + Material 3. Sin XML, sin ViewBinding, sin ViewModel, sin Activities separadas.
- **Una pantalla = un archivo:** `app/src/main/java/com/example/lifterlab/ui/features/<feature>/<Feature>Screen.kt` con un composable `@Composable fun <Feature>Screen(...)`.
- **Componentes compartidos:** reutilizar `ui/components/Components.kt` (`LifterCard`, `PrimaryButton`, `SecondaryButton`, `LifterTextField`, `ScreenTitle`, `ScreenSubtitle`, `CardTitle`, `CardCaption`, `FieldLabel`, `ErrorText`) — no crear estilos propios.
- **Tema:** `ui/theme/Theme.kt` (`LifterLabTheme`, dark "Titanio y Acero"). Usar `MaterialTheme.colorScheme.*`.
- **Datos:** `data/model/*.kt` y `data/repository/*.kt`. Las pantallas usan repositorios (`AuthRepository`, `ProfileRepository`, `RoutineRepository`) que devuelven `Result<T>` con funciones `suspend`.
- **Asincronía:** `rememberCoroutineScope()` + `launch { }` + `withContext(Dispatchers.IO) { repo.algo() }`; nunca listeners directos de Firestore dentro de la pantalla.
- **Toast:** helper `com.example.lifterlab.toast` (importar `com.example.lifterlab.toast`).
- **Navegación:** `MainActivity.kt` con un `sealed class AppScreen` + `when` en `AppNavigator()`. Un módulo nuevo entra como una pantalla más: agregar el objeto al sealed class, su ruta en el `when`, y desde dónde se accede (botón/menú).

## Paso 1 — Plantear el módulo como reto

Describe la pantalla en UNA frase de comportamiento, sin nombres de archivos/clases/funciones. Ajusta el tamaño a UN solo cambio acotado; si el módulo es grande, fragméntalo en varios retos secuenciales.

Ejemplos:
- "En la pantalla de inicio, agrega una tarjeta 'Calculadora de 1RM' que estime tu repetición máxima al ingresar peso y repeticiones."
- "Agrega una pantalla 'Competencias' a la que se llegue desde la barra inferior, que muestre tus últimos intentos de Squat, Bench y Deadlift."

Termina siempre con: "Implementalo; cuando lo tengas, dime 'listo' y lo evalúo."

## Paso 2 — Si se traba

Igual que `reto-codigo`: lee su código/logs, diagnostica y da un fragmento breve en el chat. **Nunca** apliques el cambio con Edit/Write.

## Paso 3 — Evaluar

Lee el archivo de la pantalla (Read/Grep). Verifica contra la arquitectura actual:
- ¿Es un único composable `*Screen.kt` bajo `ui/features/<feature>/`? ¿Usa los componentes de `Components.kt` y el tema?
- ¿Las llamadas a datos pasan por el repositorio con `Result<T>` + coroutines en hilo adecuado?
- ¿Está registrada la navegación en `MainActivity`?
- ¿Compila? Comando: `.\gradlew.bat :app:assembleDebug --console=plain --no-daemon`

Confirma lo bueno en 1-2 frases o señala con precisión (archivo:línea) qué falta, dejando que él lo corrija.

## Plantilla base (referencia para el estudiante)

```kotlin
package com.example.lifterlab.ui.features.<feature>

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.<X>Repository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.ScreenTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun <Feature>Screen(
    onBack: () -> Unit = {},
    <x>Repository: <X>Repository = remember { <X>Repository() }
) {
    val userId = remember { AuthRepository().getCurrentUser()?.uid.orEmpty() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var cargando by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf("") }
    var dato by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        // Cargar dato inicial desde el repositorio
        if (userId.isNotEmpty()) {
            val result = withContext(Dispatchers.IO) { <x>Repository.obtener(userId) }
            result.onSuccess { dato = it }.onFailure { e ->
                mensajeError = e.message ?: "No se pudo cargar."
            }
        }
        cargando = false
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "← Volver",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onBack() }
            )
            ScreenTitle("<Feature>", Modifier.padding(top = 8.dp))

            LifterCard(Modifier.padding(top = 16.dp)) {
                // contenido del módulo (inputs, métricas, etc.)
            }

            if (mensajeError.isNotEmpty()) {
                ErrorText(mensajeError, Modifier.padding(top = 12.dp))
            }

            PrimaryButton(
                text = if (cargando) "Cargando..." else "Guardar",
                onClick = {
                    if (userId.isEmpty()) {
                        mensajeError = "No hay una sesión iniciada."
                        return@PrimaryButton
                    }
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            <x>Repository.guardar(userId, dato)
                        }
                        result
                            .onSuccess { context.toast("Guardado correctamente") }
                            .onFailure { e -> mensajeError = e.message ?: "Error al guardar." }
                    }
                }
            )
        }
    }
}
```

## Reglas de oro

- **Un módulo = un archivo por pantalla** en `ui/features/<feature>/`. No añadir dependencias al Gradle si el módulo no lo exige.
- La lógica de datos va en `data/repository/*.kt` (nuevo repo si hace falta), nunca inline en la pantalla con Firestore directo.
- La barra inferior del dashboard conserva solo **Home, Routines, Profile**; un módulo nuevo se agrega por otro punto de entrada (botón/tarjeta) salvo que el estudiante pida cambiarla.
- No uses Edit/Write para resolver el reto tú mismo; el código lo escribe el estudiante.