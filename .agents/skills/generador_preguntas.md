# Skill: Generador de Preguntas de Descubrimiento (Project Discovery Skill)

## Propósito
Esta skill define el comportamiento del agente para analizar el repositorio (código, archivos XML, configuración de Gradle y reglas de agent.md) y generar un cuestionario directo agrupado por categorías. El objetivo es que el desarrollador o el equipo responda dónde se define cada parte o qué hace cada componente clave del proyecto.

---

## Directivas para el Agente de IA

Cuando se active esta skill (ej. "Genera preguntas básicas sobre el estado del proyecto"), debes responder estructurando el cuestionario en 4 bloques:

### 1. Interfaz y Componentes de UI (¿Qué hace este botón / pantalla?)
- Identificar botones o elementos de la interfaz en los archivos XML / Jetpack Compose cuyo comportamiento no sea obvio.
- Preguntar la función exacta de acciones clave (ej. "¿Qué debe ocurrir exactamente al presionar el botón de 'Sumar Seguros' en la calculadora de barra?").

### 2. Configuración e Infraestructura (¿Dónde se define X?)
- Indagar sobre la ubicación de servicios de backend y configuraciones globales (ej. Firebase Auth, Firestore, Gradle, mapas de navegación).
- Formular preguntas de ubicación técnica (ej. "¿En qué archivo se inicializa la persistencia offline de Firestore?").

### 3. Modelo de Datos y Estado (¿Dónde y cómo se guarda?)
- Preguntar sobre las estructuras de datos y la persistencia local/remota.
- Cuestionar la responsabilidad de los ViewModels y UiStates (ej. "¿Dónde se calcula el 1RM antes de enviarlo a la base de datos?").

### 4. Reglas de Negocio del Dominio Powerlifting
- Consultar detalles sobre algoritmos o lógica de la aplicación (ej. RPE, fórmulas de 1RM, puntaje DOTS).   