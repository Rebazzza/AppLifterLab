# 🏋️‍♂️ LifterLab - Matriz Completa de Requerimientos del Proyecto

Este documento consolida la totalidad de los Requerimientos Funcionales (RF) y No Funcionales (RNF) definidos para el desarrollo de la aplicación móvil **LifterLab** en Android Studio con Firebase.

---

## ⚙️ 1. Requerimientos Funcionales (RF)

| ID | Nombre del Requerimiento | Descripción / Proceso | Prioridad | Entregable / Módulo |
| :--- | :--- | :--- | :--- | :--- |
| **RF01** | Identificación y acceso del usuario | Autenticación inicial mediante Firebase Auth (correo y contraseña) con opción de creación de cuentas nuevas. | Alta | Módulo de Autenticación |
| **RF02** | Perfil del atleta | Registro de datos antropométricos y de configuración: nombres, año de nacimiento, sexo, altura y peso corporal actual. | Alta | Registro de perfil |
| **RF03** | Creación de plantillas de rutinas | Registro de plantillas de entrenamiento especificando nombre, categoría y ejercicios asociados en orden de ejecución. | Alta | Gestión de rutinas |
| **RF04** | Registro de series, peso y repeticiones | Sesión en vivo ("Modo Gimnasio") para marcar series completadas ingresando kilogramos y repeticiones ejecutadas. | Alta | Registro de entrenamiento |
| **RF05** | Registro de esfuerzo percibido (RPE) | Selector de fatiga por serie completada utilizando la escala RPE / RIR (valores de @6.0 a @10.0). | Media | Registro de entrenamiento |
| **RF06** | Cálculo de 1RM estimado | Estimación de la repetición máxima mediante fórmulas múltiples (Brzycki, Epley, Lander, Wathan, Lombardi). | Media | Calculadora de Fuerza |
| **RF07** | Visualizador de carga de barra IPF | Representación gráfica 2D de la barra olímpica con codificación oficial de colores de discos IPF. | Alta | Visualizador de discos |
| **RF08** | Series de aproximación e intensidades | Generación automática de tablas de porcentajes (del 75% al 95%) y series de calentamiento progresivas. | Media | Calculadora de aproximación |
| **RF09** | Gestión de intentos en competencia | Módulo de tarima para planificar y registrar los 3 intentos oficiales (Sentadilla, Banca y Peso Muerto). | Alta | Módulo de competencia |
| **RF10** | Cálculo de puntuación oficial DOTS | Procesamiento matemático automático de la puntuación DOTS cruzando el total levantado y el peso corporal. | Media | Calculadora DOTS |
| **RF11** | Calculadora de aproximación avanzada | Desglose progresivo de calentamiento porcentual (40%, 60%, 80%) previo a la serie de trabajo principal. | Media | Calculadora de aproximación |
| **RF12** | Temporizador por ejercicio | Configuración de tiempos de descanso automáticos e independientes según el patrón de movimiento. | Media | Registro de entrenamiento |
| **RF13** | Notas visuales y multimedia | Adición de notas de texto o enlaces de video vinculados a series específicas para revisión técnica. | Baja | Historial de sesión |
| **RF14** | Sugerencia de progresión semanal | Recomendación automática de sobrecarga progresiva (+2.5 kg / +1 rep) basada en el RPE previo. | Media | Gestión de rutinas |
| **RF15** | Comparador de sesiones históricas | Vista superpuesta de entrenamientos para evaluar la evolución del tonelaje y récords personales (PRs). | Media | Módulo de analítica |
| **RF16** | Entrenamiento rápido (Sesión libre) | Ejecución de sesión sin plantilla fija, agregando ejercicios al instante y registrando el historial. | Alta | Registro de entrenamiento |
| **RF17** | Edición de sesiones registradas | Modificación de valores en entrenamientos finalizados (peso, repeticiones, RPE o ejercicios). | Alta | Historial de sesión |
| **RF18** | Dashboard inicial | Panel principal con resumen de últimas sesiones, marcas SBD máximas y métricas globales. | Alta | Pantalla de Inicio |
| **RF19** | Buscador y filtros en catálogo | Filtrado del catálogo de ejercicios por nombre, grupo muscular, equipamiento o patrón de movimiento. | Alta | Catálogo de ejercicios |
| **RF20** | Creación de ejercicios personalizados | Alta de ejercicios propios por parte del atleta (nombre, categoría, tipo de medición). | Media | Catálogo de ejercicios |
| **RF21** | Buscador global de rutinas | Búsqueda de plantillas en la biblioteca por nombre o etiquetas de enfoque (Hipertrofia, Peaking). | Baja | Gestión de rutinas |
| **RF22** | Duplicación de plantillas de rutina | Clonación de rutinas existentes con un solo toque para crear nuevos bloques de entrenamiento. | Media | Gestión de rutinas |
| **RF23** | Reordenamiento drag-and-drop | Cambio de posición de ejercicios dentro de una rutina o plantilla mediante arrastre visual. | Media | Gestión de rutinas / Entrenamiento |
| **RF24** | Reemplazo de ejercicio en vivo | Sustitución de un ejercicio por una variante durante la ejecución de la sesión activa. | Media | Registro de entrenamiento |
| **RF25** | Indicador visual de Récord Personal | Distintivo o animación en tiempo real (badge "PR") al superar marcas históricas de peso o repetición. | Alta | Registro de entrenamiento |
| **RF26** | Configuración de peso de barra base | Definición global o por sesión del peso de la barra (20 kg, 15 kg, máquinas o 0 kg). | Alta | Visualizador de discos |
| **RF27** | Toggle de seguros / collars | Interruptor para sumar o restar los 2.5 kg estándar de los seguros de competencia al total de la barra. | Baja | Visualizador de discos |
| **RF28** | Desglose de tonelaje e intensidad | Contador en tiempo real del tonelaje acumulado ($\sum \text{peso} \times \text{reps}$) en la sesión activa. | Media | Registro de entrenamiento |
| **RF29** | Historial detallado por ejercicio | Ficha técnica individual por movimiento con curva de progreso de 1RM y récords. | Alta | Módulo de analítica |
| **RF30** | Exportación de entrenamientos | Descarga de historiales o reportes de competencia en formatos estructurados (CSV / PDF). | Baja | Historial de sesión |

---

## 🛠️ 2. Requerimientos No Funcionales (RNF)

| ID | Nombre del Requerimiento | Descripción / Proceso | Prioridad | Criterio (FURPS+) |
| :--- | :--- | :--- | :--- | :--- |
| **RNF01** | Persistencia dual offline-online | Operatividad sin internet mediante caché local en Firestore (`isPersistenceEnabled = true`) y sincronización en la nube. | Alta | Confiabilidad / Arquitectura |
| **RNF02** | Diseño mobile-first y tema oscuro | Interfaz adaptada a pantallas móviles (max-width 480px) con paleta "Titanio y Acero" y acentos violeta `#6567E3`. | Alta | Usabilidad / Interfaz |
| **RNF03** | Persistencia de sesión activa | Recuperación automática del progreso de entrenamiento si la aplicación se cierra de forma inesperada. | Alta | Usabilidad / Recuperación |
| **RNF04** | Temporizador de descanso interactivo | Cronómetro flotante en pantalla con botones de ajuste rápido (+30s / -30s). | Media | Usabilidad / Eficiencia |
| **RNF05** | Precisión matemática en cálculos | Algoritmos de 1RM, desglose de discos y puntaje DOTS exentos de errores de redondeo. | Alta | Confiabilidad / Funcional |
| **RNF06** | Optimización para una sola mano | Ubicación ergonómica de botones de acción y barra de navegación inferior para el pulgar. | Media | Usabilidad / Accesibilidad |
| **RNF07** | Catálogo precargado de ejercicios | Base de datos inicial con más de 50 ejercicios estándar organizados por categorías SBD. | Media | Rendimiento / Datos |
| **RNF08** | Registro de ejercicios personalizados | Soporte para almacenar y gestionar extensiones de ejercicios definidos por el usuario localmente. | Media | Soporte / Extensibilidad |
| **RNF09** | Gráficos de tendencia de peso | Visualización temporal interactiva de la evolución del peso corporal del atleta mes a mes. | Media | Usabilidad / Visualización |
| **RNF10** | Módulo social de clanes (Beta) | Vista informativa de tablas de posiciones globales y rendimiento comparativo en equipo. | Baja | Extensibilidad / Social |
| **RNF11** | Respuesta háptica al finalizar timer | Vibración distintiva en el dispositivo al finalizar el tiempo de descanso sin requerir mirar la pantalla. | Media | Usabilidad / Accesibilidad |
| **RNF12** | Tiempos de carga ultrarrápidos | Respuesta menor a 100 ms en la interfaz de "Modo Gimnasio" mediante almacenamiento local en IndexedDB/Room. | Alta | Rendimiento / Confiabilidad |
| **RNF13** | Accesibilidad de alto contraste (WCAG) | Cumplimiento del estándar WCAG AA en modo oscuro para lectura de métricas a distancia en el suelo. | Media | Usabilidad / Interfaz |
| **RNF14** | Modo Horizontal (Landscape) | Adaptación automática del visualizador 2D de discos IPF al girar el dispositivo a posición horizontal. | Baja | Usabilidad / Adaptabilidad |
| **RNF15** | Sincronización silenciosa en segundo plano | Envío de datos locales a Firebase sin bloquear la navegación ni mostrar modales bloqueantes. | Alta | Confiabilidad / Rendimiento |