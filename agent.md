# Contexto Técnico del Proyecto: LifterLab

Este documento (`agent.md`) contiene el contexto técnico, la arquitectura, el modelo de datos y las reglas de negocio de **LifterLab** para guiar a los agentes de IA (Gemini Code Assist / Antigravity) en la generación de código nativo preciso en Android Studio.

---

## 1. Información General
- **Nombre del Proyecto:** LifterLab (`com.example.lifterlab`)
- **Tipo de App:** Android Nativa (Móvil)
- **Lenguaje Principal:** Kotlin
- **JDK / Compatibilidad:** Java 11 / 17
- **SDK Target / Min:** Compile 37 / Target 37 / Min SDK 24 (Android 7.0)

---

## 2. Arquitectura y Tecnologías Clave
- **Patrón de Arquitectura:** MVVM (Model-View-ViewModel) + Clean Architecture orientada a Unidirectional Data Flow (UDF).
- **Interfaz de Usuario (UI):** XML Clásico con `ViewBinding` y componentes de Material Design 3 (`ConstraintLayout`). *(Nota: Si migras a Jetpack Compose, actualiza este apartado).*
- **Navegación:** Android Navigation Component (`nav_graph.xml`) bajo el patrón Single-Activity (`MainActivity`).
- **Persistencia y Backend:** Firebase Cloud Firestore (NoSQL) con persistencia offline habilitada (`Offline-First`).
- **Autenticación:** Firebase Auth (Email/Password & Google Sign-In).
- **Asincronía:** Kotlin Coroutines + `StateFlow` / `SharedFlow`.

---

## 3. Modelo de Datos (Firestore Spec)
Toda la información del usuario se almacena aislada bajo la ruta raíz `/users/{userId}`:

- `/users/{userId}`: Perfil del atleta (nombre, peso corporal, peso de barra predeterminado).
- `/users/{userId}/routines/{routineId}`: Plantillas de rutinas y lista de ejercicios organizados.
- `/users/{userId}/workout_sessions/{sessionId}`: Historial de entrenamientos registrados (peso, reps, RPE, 1RM estimado, marcas de PR).
- `/users/{userId}/competitions/{competitionId}`: Módulo de competencia (intentos de Squat, Bench, Deadlift y puntaje DOTS).
- `/users/{userId}/equipment/{equipmentId}`: Monitoreo de desgaste e indumentaria personal (ODS 12).

---

## 4. Especificaciones y Reglas de Negocio (Business Specs)
1. **Cálculo de Carga IPF (RF07, RF27):**
   - Peso Total = (Peso de Barra Base) + (Suma de Discos por Lado × 2) + (Seguros/Collars si están activos: 5.0 kg total).
   - Barras base soportadas: 20 kg (Estándar), 15 kg (Femenina), 0 kg (Máquinas/Especial).
2. **Calculadora de Fuerza (RF06, RF08):**
   - Estimación de 1RM usando la fórmula de Brzycki: `1RM = Peso / (1.0278 - (0.0278 × Reps))`.
   - Generación de porcentajes de aproximación: 70%, 75%, 80%, 85%, 90%, 95%.
3. **Puntuación Competitiva DOTS (RF10):**
   - Cálculo automático del puntaje oficial DOTS cruzando el total levantado (SBD en kg) con el peso corporal del atleta y su sexo.
4. **Sostenibilidad y Mantenimiento de Equipamiento (ODS 12):**
   - Monitoreo del ciclo de vida de accesorios (cinturón, rodilleras, calzado). Notificar mantenimiento preventivo al alcanzar cada 50 sesiones de uso.

---

## 5. Reglas y Directivas para el Agente de IA
1. **Sintaxis y Código:** Escribir código exclusivamente en **Kotlin** idiomático y limpio.
2. **ViewBinding:** Al crear o modificar vistas en Fragments, inicializar siempre el `ViewBinding` y limpiar la referencia en `onDestroyView()` para evitar memory leaks.
3. **Firebase & Asincronía:**
   - Usar siempre Kotlin Coroutines (`suspend` functions / `await()`) o `callbackFlow` para consultar Firestore; evitar callbacks anidados.
   - Envolver las operaciones en `Result<T>` para manejar estados de éxito y fallos de red/offline.
4. **Respetar Gradle DSL:** Toda inclusión de dependencias debe realizarse en sintaxis Kotlin DSL (`build.gradle.kts`) respetando el Version Catalog (`libs.versions.toml`) y el Firebase BOM.
5. **Comprobación de Seguridad:** Todo documento o consulta escrita para Firestore debe asumir la restricción `request.auth.uid == userId`.
# Directivas de Diseño y UI/UX para el Agente (Android Studio)

## 1. Sistema Visual y Reglas Estéticas
- **Tema:** Dark Mode Puro ("Titanio y Acero"). Fondo `@color/bg_background` (#131315), Tarjetas `@color/bg_surface_container_low` (#1C1B1D) con borde de 1dp `@color/outline_variant` (#464554).
- **Acentos de Color:**
  - Principal (Acciones clave, Botón "Train"): `@color/primary_accent` (#6567E3).
  - Acento Secundario (Weight / Records / DL): `@color/secondary_accent` (#FFB77D).
  - Racha y Éxito (Sesiones, Checks completados): `@color/tertiary_accent` (#4EDEA3).
  - Alerta / Sentadilla (Squat): `@color/error_accent` (#FFB4AB).
- **Tipografía:** Usar fuentes limpias con peso `fontFamily="sans-serif-medium"` para encabezados y `fontFamily="monospace"` para números/métricas (RPE, Kilos, Reps).

## 2. Construcción de Pantallas en XML
1. Todos los layouts deben ser creados usando `ConstraintLayout` como contenedor principal o `LinearLayout` para pilas verticales simples.
2. Usar componentes `com.google.android.material.card.MaterialCardView` o aplicar el background `@drawable/bg_surface_card` para los bloques de métricas.
3. Los botones principales deben implementar `MaterialButton` estilizados con bordes redondeados (12dp) y colores de la paleta.
4. La barra de navegación inferior debe ser un `com.google.android.material.bottomnavigation.BottomNavigationView` con íconos vectoriales minimalistas (`MaterialSymbols`).

## 3. Discos de Pesas IPF (Reglas de Dibujo/UI)
Cuando se genere el visualizador 2D de la barra olímpica en un Custom View o Canvas XML:
- 25 kg -> `@color/ipf_red_25kg`
- 20 kg -> `@color/ipf_blue_20kg`
- 15 kg -> `@color/ipf_yellow_15kg`
- 10 kg -> `@color/ipf_green_10kg`
- 5 kg -> `@color/ipf_white_5kg`
- 2.5 kg -> `@color/ipf_black_2_5kg`
- Seguros/Collars (2.5 kg) -> Representar con bloque metálico/gris angosto al final de los discos.