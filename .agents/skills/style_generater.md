# Skill: Estilo Visual y Sistema de Diseño UI/UX (UI Style Skill)

## Propósito
Esta skill establece los tokens de diseño, paleta de colores, reglas de componentes y guía estérica oficial de la aplicación **LifterLab** ("Titanio y Acero"). El objetivo es garantizar que cualquier vista, layout o componente generado mantenga consistencia gráfica mobile-first e identidad visual.

---

## 🎨 1. Paleta de Colores y Tokens Oficiales

Toda interfaz gráfica debe utilizar los siguientes colores e identificadores de recursos:

### A. Modo Oscuro Base ("Titanio y Acero")
- **Fondo Principal (`bg_background`):** `#000000` / `#131315` (Oscuro puro)
- **Superficies y Tarjetas (`bg_surface_container_low`):** `#121212` / `#1C1B1D`
- **Contenedores Destacados (`bg_surface_container_high`):** `#2A2A2C`
- **Bordes y Divisores (`outline_variant`):** `#2A2A2A` / `#464554` (Sutiles de 1dp)

### B. Acentos de Marca
- **Acento Principal (`primary_accent`):** `#6567E3` (Azul / Violeta - Acciones clave y navegación)
- **Acento Secundario (`secondary_accent`):** `#FF38FA` / `#FFB77D` (Rosa/Fucsia - Récords y alertas)
- **Éxito y Racha (`tertiary_accent`):** `#4EDEA3` (Verde Neón - Check de series completadas)
- **Alerta / Sentadilla (`error_accent`):** `#FFB4AB` (Rojo)

### C. Codificación Oficial IPF (Visualizador de Barra y Discos)
- **25 kg:** `#E53935` (Rojo)
- **20 kg:** `#1E88E5` (Azul)
- **15 kg:** `#FDD835` (Amarillo)
- **10 kg:** `#43A047` (Verde)
- **5 kg:** `#FFB77D` (Naranja / Ambar)
- **2.5 kg:** `#212121` (Negro / Gris oscuro)
- **1.25 kg / Seguros (2.5kg):** `#FAFAFA` / Metálico

---

## 📐 2. Reglas de Layout y Tipografía

1. **Diseño Mobile-First:** Ancho máximo de contenedor centrado de **480px** con `BottomNavigationBar` fija.
2. **Jerarquía Tipográfica:**
    - Textos generales y títulos: `sans-serif-medium` de alto contraste (`#E5E1E4`).
    - Métricas numéricas (kilos, reps, RPE, 1RM): `monospace` o peso `bold` para máxima legibilidad durante el entrenamiento.
3. **Redondeo y Formas:**
    - Tarjetas y contenedores de métricas: Bordes redondeados de **12dp** con borde `outline_variant` de 1dp.
    - Botones primarios de acción: Radio de **12dp** o **Pill shape** con relleno `#6567E3`.

---

## 🛠️ 3. Directivas para el Agente de IA

Cuando se solicite crear una nueva vista o componente visual:
1. Aplica el fondo `#000000` o `@color/bg_background` para evitar fondos blancos o parpadeos.
2. Usa tarjetas sobrepuestas `#121212` con bordes sutiles en `#2A2A2A` para separar bloques de datos.
3. Reserva el color de acento `#6567E3` exclusivamente para elementos interactivos primarios (botones principales, pestañas activas, contadores en tiempo real).
4. Asigna los colores correspondientes de la IPF a cualquier representación de la barra olímpica o listas de discos.