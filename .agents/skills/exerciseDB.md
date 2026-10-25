# Skill: Integración de Catálogo de Ejercicios con ExerciseDB (RapidAPI) y Firestore

## Propósito
Esta skill establece el flujo de consumo de la API de **ExerciseDB (RapidAPI)**, la transformación de su estructura JSON a los modelos internos de **LifterLab**, y las reglas para enriquecer la base de datos de Cloud Firestore con demostraciones animadas (GIFs) respetando los criterios de rendimiento e identidad visual.

---

## 🌐 1. Parámetros de Configuración de la API (RapidAPI)

- **API Base URL:** `https://exercisedb.p.rapidapi.com`
- **Endpoint Principal:** `/exercises` (obtiene el catálogo general)
- **Headers Requeridos:**
    - `X-RapidAPI-Key`: `1b006addc4msh8fe81ee8f6a5a6cp1cf327jsnf622de8b3f96`[cite: 1]
    - `X-RapidAPI-Host`: `exercisedb.p.rapidapi.com`

---

## 🗄️ 2. Mapeo de Datos: ExerciseDB API -> Firestore Spec

Al sincronizar o consultar la API, la IA o el backend debe mapear la respuesta JSON de RapidAPI a la estructura del documento en Firestore (`/exercises/{exerciseId}`):

| Campo JSON (ExerciseDB) | Campo Documento Firestore | Tipo de Dato | Transformación / Mapeo para LifterLab |
| :--- | :--- | :--- | :--- |
| `id` | `exerciseId` | String | ID único de referencia de la API. |
| `name` | `name` | String | Nombre del ejercicio (se sugiere formatear a Title Case). |
| `bodyPart` / `target` | `primaryMuscle` | String | Músculo principal (ej. "quadriceps", "chest", "back"). |
| `equipment` | `equipment` | String | Equipamiento necesario (ej. "barbell", "dumbbell"). |
| `gifUrl` | `gifUrl` | String | URL pública alojada del GIF de demostración. |
| *(Calculado)* | `isSBD` | Boolean | `true` si el nombre contiene "squat", "bench press" o "deadlift". |

---

## 🛠️ 3. Reglas de Implementación en Android Studio

### A. Almacenamiento Eficiente (Buenas Prácticas)
1. **No guardar binarios Base64 en Firestore:** Almacenar únicamente la cadena con la URL pública (`gifUrl`).
2. **Caché de Imágenes y GIFs con Glide:** Para renderizar la demostración animación en las vistas sin sobrecargar la memoria ni re-descargar recursos:
   ```kotlin
   Glide.with(context)
       .asGif()
       .load(exercise.gifUrl)
       .placeholder(R.drawable.bg_surface_card) // Respetar tokens del ui-design-system.md
       .into(imageView)