---
name: reto-codigo
description: "Propone al estudiante un cambio pequeño y concreto sobre su app existente (una validación, un botón, un indicador de carga, etc.), descrito en términos de pantalla/comportamiento y no de archivos o clases, para que él lo implemente. Luego evalúa el resultado leyendo el código. Úsalo cuando un estudiante quiera practicar modificando código ya existente."
trigger: /reto-codigo
---

# /reto-codigo

Le pides al estudiante que haga un cambio pequeño sobre su app ya existente (agregar una validación, un botón, un indicador de carga, un mensaje de error, etc.), describiendo el pedido en términos de lo que se ve/pasa en la app — nunca en términos de archivos, clases o funciones. Él lo implementa; tú después evalúas si quedó bien.

## Paso 0 — Elegir el reto

Si el estudiante da un tema (ej. "quiero practicar validaciones", "algo con estados de carga"), úsalo como guía.

Si no da nada, lee el código del proyecto (Read/Grep) para encontrar una pantalla o funcionalidad real donde tenga sentido agregar algo pequeño que hoy falte (ej. no hay indicador mientras carga una API, un campo no valida vacío, falta un botón de limpiar formulario).

## Paso 1 — Plantear el cambio

Redacta el pedido en una sola frase, en términos de pantalla y comportamiento visible, **nunca** nombrando archivos, clases, funciones o composables técnicos. Ejemplos:

- "En la pantalla donde se registran usuarios, agrega un indicador de carga mientras se espera la respuesta de la API."
- "En la lista de productos, agrega una validación para que no se pueda guardar un producto con el nombre vacío."

El cambio debe ser **pequeño y acotado** — una sola cosa a la vez, nunca una funcionalidad completa nueva. No expliques cómo implementarlo salvo que el estudiante pregunte.

## Paso 2 — Si se traba

Mismas reglas que en `guia-codigo`: puedes leer su código y logs/errores para diagnosticar y explicarle qué está pasando, y si sigue sin lograrlo darle un fragmento breve en el chat con explicación de uso. Nunca uses Edit/Write para aplicarlo tú.

## Paso 3 — Evaluar el cambio

Cuando el estudiante diga que terminó, lee el código relevante (Read/Grep, y logs si aplica) para verificar si el cambio quedó bien hecho y en el lugar correcto.

- Si está bien: confírmalo en 1-2 frases, señalando específicamente qué hizo bien.
- Si falta algo o está mal: dile con precisión qué encontraste y por qué no cumple el pedido (aquí sí puedes citar archivo/línea, ya que es evaluación, no el planteo inicial), pero deja que él lo corrija — no lo corrijas tú.
- Si tras un par de correcciones sigue sin quedar bien, sé más directo explicándole el punto exacto a cambiar, sin reescribirle el código.

## Paso 4 — Siguiente paso

Pregúntale si quiere otro reto pequeño o prefiere cerrar. Si cierra, un resumen breve (1-2 líneas) de qué practicó.

## Qué NO hacer

- No uses Edit/Write para aplicar el cambio — lo escribe el estudiante.
- No menciones archivos, clases o funciones al plantear el pedido inicial — solo pantalla/comportamiento.
- No propongas cambios grandes ni funcionalidades completas, solo cambios pequeños y acotados.
- No des el código de la solución de entrada, solo si se traba (Paso 2).