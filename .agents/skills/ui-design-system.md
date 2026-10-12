# Skill: Estilo Visual, Sistema de Diseño UI/UX e Identidad Cromática

## Propósito
Esta skill define la guía estético-visual, la paleta cromática de alto contraste, los tokens de diseño y la asignación cromática para los movimientos básicos de Powerlifting (SBD) en la aplicación **LifterLab**. Cualquier vista, layout o componente generado por la IA debe cumplir con estos lineamientos para asegurar coherencia en el modo oscuro (*Dark Mode*).

---

## 🎨 1. Concepto Visual y Estética General

- **Estilo:** Dark Mode de Alto Contraste Moderno / Neón ("Titanio y Acero").
- **Filosofía UI/UX:** Interfaz limpia orientada a entornos de baja/alta iluminación (gimnasios). Los bloques oscuros destacan las métricas numéricas pesadas (kilos, reps, RPE, 1RM).
- **Layout:** Mobile-first con ancho máximo de contenedor centrado de **480px** y `BottomNavigationBar` fija.

---

## 🖤 2. Paleta de Colores Base (Tokens de Diseño)

| Token de Recurso | Código Hexadecimal | Propósito y Uso en Interfaz |
| :--- | :--- | :--- |
| **`bg_background`** | `#09090B` | Fondo general negro puro / antracita (pantalla OLED). |
| **`bg_surface_container`** | `#121212` | Tarjetas, contenedores de ejercicios y secciones secundarias. |
| **`outline_variant`** | `#2A2A2A` | Bordes sutiles de 1dp en tarjetas y separadores. |
| **`primary_accent`** | `#6366F1` | **Azul Claro / Violeta (Brand Core):** Botones principales, navegación, switches y estados globales. |
| **`text_primary`** | `#E5E1E4` | Títulos y métricas de alto contraste. |

---

## 🏋️‍♂️ 3. Asignación Cromática para los 3 Básicos (SBD)

Para diferenciar visualmente los tres movimientos principales del Powerlifting dentro de tarjetas, badges, gráficos de progreso y etiquetas:

| Movimiento / Básico | Identificador | Código Hex | Justificación Visual y Uso |
| :--- | :--- | :--- | :--- |
| **Squat (Sentadilla)** | `color_squat` | `#10B981` | **Verde Neón:** Representa estabilidad, empuje de tren inferior y energía. |
| **Bench Press (Press de Banca)** | `color_bench` | `#D97706` | **Naranja / Ámbar:** Aporta calidez y contraste para aislar el bloque de torso. |
| **Deadlift (Peso Muerto)** | `color_deadlift` | `#EF4444` *(o `#FF38FA`)* | **Rojo Coral / Fucsia Neón:** Simboliza esfuerzo pico, máxima tensión y potencia en el movimiento final. |

---

## 🔤 4. Tipografía y Jerarquía Visual

- **Headline / Títulos (`Chivo`):** Encabezados de pantallas y tarjetas principales.
- **Body / Textos (`Inter`):** Párrafos, instrucciones, etiquetas e interfaz general.
- **Label / Métricas (`JetBrains Mono` / `monospace`):** Datos numéricos (kilos, repeticiones, RPE, 1RM y temporizadores) para evitar saltos de texto durante la edicion en vivo.

---

## 🛠️ 5. Directivas Estrictas para el Agente de IA

1. **Aislamiento del Azul Principal (`#6366F1`):** Usar el azul claro exclusivamente en la identidad de marca, navegación principal y acciones clave del sistema (Botón "Train", Guardar, Switches).
2. **Aplicación Cromática SBD:** En pantallas de rutina, historiales o analíticas, aplica el color correspondiente (`#10B981` para Sentadilla, `#D97706` para Banca, `#EF4444` para Peso Muerto) en los bordes, etiquetas o indicadores de progreso.
3. **Contenedores y Bordes:** Las tarjetas de métricas deben usar `#121212` de fondo con un borde de 1dp en `#2A2A2A` y un radio de esquina de **12dp**.