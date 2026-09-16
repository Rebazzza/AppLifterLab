#!/bin/bash

# Generar y enviar los 30 requerimientos uno por uno
gh issue create --title "RF01: Identificación y acceso del usuario" --body "### Descripción
Apenas se ingresa a la aplicación se indicará al usuario identificarse a través de un inicio de sesión; en caso de no contar con una cuenta, se le permitirá crear una nueva.

### Entregable
Módulo de Autenticación"

gh issue create --title "RF02: Perfil del atleta" --body "### Descripción
La interfaz de perfil contará con una sección donde el usuario ingresará información como: nombres, año de nacimiento, sexo, altura y peso corporal actual.

### Entregable
Registro de perfil"

gh issue create --title "RF03: Creación de plantillas de rutinas" --body "### Descripción
A través de la interfaz de rutinas, el usuario podrá registrar una nueva rutina especificando nombre, categoría y los ejercicios asociados en orden de ejecución.

### Entregable
Gestión de rutinas"

gh issue create --title "RF04: Registro de series, peso y repeticiones" --body "### Descripción
Durante la sesión activa ('Modo Gimnasio'), el usuario podrá marcar cada serie completada ingresando el peso levantado en kilogramos y las repeticiones ejecutadas.

### Entregable
Registro de entrenamiento"

gh issue create --title "RF05: Registro de esfuerzo percibido (RPE)" --body "### Descripción
En cada serie completada del entrenamiento en vivo, el usuario podrá seleccionar un valor de RPE o marca de esfuerzo (de @6 a @10) para medir la fatiga.

### Entregable
Registro de entrenamiento"

gh issue create --title "RF06: Cálculo de 1RM estimado" --body "### Descripción
A través de la calculadora de fuerza, el usuario ingresará el peso y repeticiones para que el sistema calcule el 1RM estimado usando algoritmos como Brzycki y Epley.

### Entregable
Calculadora de Fuerza"

gh issue create --title "RF07: Visualizador de carga de barra IPF" --body "### Descripción
El sistema contará con un visualizador gráfico que mostrará los discos exactos que se deben colocar en cada lado de la barra respetando la codificación de colores IPF.

### Entregable
Visualizador de discos"

gh issue create --title "RF08: Series de aproximación e intensidades" --body "### Descripción
A partir del 1RM del usuario o peso base ingresado, el sistema generará automáticamente la tabla de porcentajes (del 75% al 95%) y series de calentamiento progresivas.

### Entregable
Calculadora de aproximación"

gh issue create --title "RF09: Gestión de intentos en competencia" --body "### Descripción
El usuario contará con un módulo de competencia donde podrá planificar y registrar sus 3 intentos oficiales para Sentadilla, Press de Banca y Peso Muerto.

### Entregable
Módulo de competencia"

gh issue create --title "RF10: Cálculo de puntuación oficial DOTS" --body "### Descripción
Calculadora DOTS"

gh issue create --title "RF11: Calculadora de aproximación" --body "### Descripción

Calculadora de aproximación"

gh issue create --title "RF12: Temporizador por ejercicio" --body "### Descripción
Permitir configurar tiempos de descanso automáticos e independientes por tipo de ejercicio (ej. 3-5 min en básicos SBD vs 1.5 min en accesorios).
### Entregable

gh issue create --title "RF13: Notas visuales y multimedia" --body "### Descripción

### Entregable
Historial de sesión"

Recomendar incrementos automáticos de carga o repeticiones para la siguiente semana aplicando sobrecarga progresiva según el RPE de la sesión anterior.
### Entregable
Gestión de rutinas"

gh issue create --title "RF15: Comparador de sesiones históricas" --body "### Descripción
Permitir seleccionar una rutina activa y mostrar una vista superpuesta con la sesión previa para evaluar progresos de tonelaje y récords personales (PRs).

### Entregable
Módulo de analítica"

gh issue create --title "RF16: Entrenamiento rápido (Sesión libre)" --body "### Descripción
Iniciar un entrenamiento sin plantilla predeterminada, agregando ejercicios sobre la marcha y registrando la carga en el historial sin crear una rutina fija.

### Entregable
Registro de entrenamiento"

gh issue create --title "RF17: Edición de sesiones registradas" --body "### Descripción
Permitir editar o corregir un entrenamiento ya finalizado y guardado en el historial para modificar peso, repeticiones, RPE o ejercicios realizados.

### Entregable
Historial de sesión"

gh issue create --title "RF18: Dashboard inicial" --body "### Descripción
Permitir ver la información general más importante, como mis últimas sesiones, mis mayores pesos levantados, sesiones próximas, etc.

### Entregable
Historial de sesión"

gh issue create --title "RF19: Buscador y filtros en catálogo de ejercicios" --body "### Descripción
Permitir filtrar el catálogo de ejercicios por nombre, grupo muscular principal (pecho, espalda, piernas), equipamiento (barra, mancuerna, máquina) o tipo de movimiento (SBD).

### Entregable
Catálogo de ejercicios"

gh issue create --title "RF20: Creación de ejercicios personalizados" --body "### Descripción
Permitir al usuario dar de alta un nuevo ejercicio ingresando nombre, categoría muscular y tipo de medición (peso + reps, tiempo o peso corporal).

### Entregable
Catálogo de ejercicios"

gh issue create --title "RF21: Buscador global de rutinas" --body "### Descripción
Incluir una barra de búsqueda en la pantalla de rutinas para filtrar plantillas por nombre o etiquetas de enfoque (ej. 'Hipertrofia', 'Peaking', 'Fuerza 4 días').

### Entregable
Gestión de rutinas"

gh issue create --title "RF22: Duplicación de plantillas de rutina" --body "### Descripción
Permitir clonar una rutina existente con un clic para crear variaciones o bloques de entrenamiento nuevos sin tener que armarla desde cero.

### Entregable
Gestión de rutinas"

gh issue create --title "RF23: Reordenamiento drag-and-drop de ejercicios" --body "### Descripción
Permitir cambiar el orden de los ejercicios dentro de una rutina activa o plantilla arrastrándolos hacia arriba o abajo en la interfaz.

### Entregable
Gestión de rutinas / Entrenamiento"

gh issue create --title "RF24: Reemplazo de ejercicio en vivo" --body "### Descripción
Permitir sustituir un ejercicio por una variante durante la sesión (ej. cambiar Banca con Barra por Banca con Mancuernas si la barra está ocupada).

### Entregable
Registro de entrenamiento"

gh issue create --title "RF25: Indicador visual de Récord Personal (PR)" --body "### Descripción
Mostrar una animación o distintivo (badge de 'PR') en tiempo real dentro del entrenamiento cuando el peso o reps ingresados superen el récord histórico en ese ejercicio.

### Entregable
Registro de entrenamiento"

gh issue create --title "RF14: Sugerencia de progresión semanal" --body "### Descripción
Permitir adjuntar notas de texto, observaciones técnicas o enlaces de video a series específicas para revisar la ejecución en levantamientos pesados.
Registro de entrenamiento"

gh issue create --title "RF26: Configuración de peso de barra base" --body "### Descripción
Permitir definir en la configuración global o por sesión el peso predeterminado de la barra olímpica (20 kg, 15 kg, barra especializada o 0 kg para máquinas).


### Entregable
### Entregable
Visualizador de discos"

gh issue create --title "RF27: Toggle de cálculo de seguros/collars" --body "### Descripción
Incluir un interruptor en el visualizador de discos para sumar o restar los 2.5 kg estándar de los seguros de competencia al total del peso en barra.
Generar un desglose progresivo de series de calentamiento con pesos y repeticiones porcentuales (40%, 60%, 80%) antes de la serie principal sin fatigar al atleta.

### Entregable
### Entregable
Visualizador de discos"


gh issue create --title "RF28: Desglose de tonelaje e intensidad de la sesión" --body "### Descripción
Mostrar en la cabecera del entrenamiento activo un contador en tiempo real del tonelaje acumulado (sumatoria de peso x reps) y total de series completadas.

### Entregable
Registro de entrenamiento"


gh issue create --title "RF29: Historial detallado por ejercicio específico" --body "### Descripción
Incluir una vista directa desde la ficha de cada ejercicio que muestre la curva de progreso de 1RM estimado, el peso máximo histórico y el total de repeticiones registradas.


### Entregable
Módulo de analítica"

gh issue create --title "RF30: Exportación de entrenamientos (CSV / PDF)" --body "### Descripción
Permitir descargar el historial de sesiones o el resumen de una competencia en archivo CSV o PDF formateado para compartir con un entrenador o guardar como respaldo.

### Entregable
Historial de sesión"

echo "¡Completado! Se crearon los 30 requerimientos correctamente."
