package com.example.lifterlab.ui.features.routines

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Reordenamiento de ejercicios por arrastre (drag-and-drop).
 *
 * Equivalente en Compose del par `ItemTouchHelper` + `ItemTouchHelper.Callback`: mide la
 * altura de cada tarjeta, sigue al dedo sobre el "drag handle" y, al cruzar media altura
 * del vecino, intercambia los elementos invocando [onMove] y corrige el desplazamiento
 * para que la tarjeta arrastrada no salte. Las alturas viajan junto a sus tarjetas para
 * que el cálculo siga siendo correcto después de cada intercambio.
 */
class ExerciseDragState {

    /** Callback de intercambio: se actualiza en cada recomposición desde la pantalla. */
    var onMove: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }

    var draggingIndex by mutableIntStateOf(-1)
        private set

    var dragDeltaY by mutableFloatStateOf(0f)
        private set

    /** Altura de cada tarjeta (px), alineada con la lista en pantalla. */
    private val heights = mutableStateListOf<Float>()

    /** Borde superior de cada tarjeta en la ventana (px), solo posición de layout. */
    private val topsY = mutableStateListOf<Float>()

    /** Borde superior de la tarjeta arrastrada al iniciar el gesto (px en ventana). */
    private var dragStartTopY = 0f

    /** Región visible del contenedor con scroll (px en ventana). */
    var viewportTopY by mutableFloatStateOf(0f)
        private set

    var viewportHeightPx by mutableFloatStateOf(0f)
        private set

    fun isDragging(index: Int): Boolean = draggingIndex == index

    /** Mantiene las listas alineadas con la cantidad de ejercicios en pantalla. */
    fun syncSize(count: Int) {
        while (heights.size < count) {
            heights.add(0f)
            topsY.add(0f)
        }
        while (heights.size > count) {
            heights.removeAt(heights.lastIndex)
            topsY.removeAt(topsY.lastIndex)
        }
    }

    /**
     * Mide una tarjeta. Crece las listas si hace falta: así una medición que llega antes
     * que [syncSize] nunca se descarta (si se descartara, las alturas quedarían en 0 y el
     * arrastre seguiría al dedo sin reordenar nada).
     */
    fun setBounds(index: Int, topY: Float, heightPx: Float) {
        while (heights.size <= index) {
            heights.add(0f)
            topsY.add(0f)
        }
        heights[index] = heightPx
        topsY[index] = topY
    }

    /** Región visible del contenedor con scroll, para el auto-scroll durante el arrastre. */
    fun setViewport(topY: Float, heightPx: Float) {
        viewportTopY = topY
        viewportHeightPx = heightPx
    }

    /** Traslación total de la tarjeta en [index]: la arrastrada sigue al dedo y las
     *  vecinas se corren para abrirle el hueco. */
    fun translationY(index: Int): Float {
        val dragged = draggingIndex
        if (dragged < 0) return 0f
        if (index == dragged) return dragDeltaY

        val alturaArrastrada = heights.getOrNull(dragged) ?: return 0f
        val destino = targetIndex()
        return when {
            index > dragged && index <= destino -> -alturaArrastrada
            index < dragged && index >= destino -> alturaArrastrada
            else -> 0f
        }
    }

    internal fun start(index: Int) {
        if (index !in heights.indices) return
        draggingIndex = index
        dragDeltaY = 0f
        dragStartTopY = topsY[index]
    }

    internal fun drag(deltaY: Float) {
        val dragged = draggingIndex
        if (dragged < 0) return
        dragDeltaY += deltaY

        val destino = targetIndex()
        if (destino == dragged) return

        val alturaDesplazada = heights[destino]
        onMove(dragged, destino)

        // Las alturas se intercambian igual que las tarjetas.
        heights[destino] = heights[dragged]
        heights[dragged] = alturaDesplazada

        // La tarjeta arrastrada ahora ocupa otra posición: se corrige el delta para
        // que permanezca pegada al dedo (ItemTouchHelper hace lo mismo con dX).
        dragDeltaY -= if (destino > dragged) alturaDesplazada else -alturaDesplazada
        draggingIndex = destino
    }

    internal fun end() {
        draggingIndex = -1
        dragDeltaY = 0f
    }

    /** Índice destino según los cruces de media altura con cada vecino consecutivo. */
    private fun targetIndex(): Int {
        val dragged = draggingIndex
        if (dragged < 0) return dragged

        var destino = dragged
        var acumulado = 0f

        if (dragDeltaY > 0f) {
            for (i in dragged + 1 until heights.size) {
                val altura = heights[i]
                if (altura <= 0f) break
                acumulado += altura / 2f
                if (dragDeltaY >= acumulado) destino = i else break
            }
        } else if (dragDeltaY < 0f) {
            for (i in dragged - 1 downTo 0) {
                val altura = heights[i]
                if (altura <= 0f) break
                acumulado += altura / 2f
                if (-dragDeltaY >= acumulado) destino = i else break
            }
        }
        return destino
    }

    /**
     * Desplazamiento de scroll (px con signo, positivo = hacia abajo) para mantener la
     * tarjeta arrastrada dentro de la zona sensible [thresholdPx] de los bordes de la
     * pantalla visible. Limita la velocidad a [maxSpeedPx] por frame. Devuelve 0 cuando
     * la tarjeta está en zona segura o no hay arrastre en curso.
     */
    fun autoScrollDeltaPx(thresholdPx: Float, maxSpeedPx: Float): Float {
        val dragged = draggingIndex
        if (dragged < 0 || dragged !in heights.indices || viewportHeightPx <= 0f) return 0f

        // Posición visual de la tarjeta = su posición de layout al iniciar + lo arrastrado.
        val top = dragStartTopY + dragDeltaY
        val bottom = top + heights[dragged]

        val excedeAbajo = bottom - (viewportTopY + viewportHeightPx - thresholdPx)
        if (excedeAbajo > 0f) return minOf(excedeAbajo, maxSpeedPx)

        val excedeArriba = (viewportTopY + thresholdPx) - top
        if (excedeArriba > 0f) return -minOf(excedeArriba, maxSpeedPx)

        return 0f
    }
}

@Composable
fun rememberExerciseDragState(onMove: (fromIndex: Int, toIndex: Int) -> Unit): ExerciseDragState {
    val state = remember { ExerciseDragState() }
    state.onMove = onMove
    return state
}

/**
 * Arranca el arrastre únicamente al mantener presionado el ícono de agarre, de modo que
 * no haya conflictos con el desplazamiento vertical de la pantalla.
 */
fun Modifier.dragHandleFor(state: ExerciseDragState, index: Int): Modifier = this
    .pointerInput(state, index) {
        detectDragGesturesAfterLongPress(
            onDragStart = { state.start(index) },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount.y)
            },
            onDragEnd = { state.end() },
            onDragCancel = { state.end() }
        )
    }

/**
 * Contenedor de una tarjeta dentro de la lista reordenable: mide su altura y aplica la
 * traslación en vivo mientras el atleta arrastra otro ejercicio.
 */
@Composable
fun ReorderableExerciseItem(
    state: ExerciseDragState,
    index: Int,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val top = coords.positionInWindow()
                state.setBounds(index, top.y, coords.size.height.toFloat())
            }
            .graphicsLayer { translationY = state.translationY(index) }
    ) {
        content()
    }
}

/**
 * Auto-scroll durante el arrastre: mientras la tarjeta arrastrada se acerca al borde
 * superior o inferior de la pantalla, la lista se desplaza sola (y el arrastre sigue
 * acumulando deltas para que la tarjeta no se despegue del dedo ni se salten cruces).
 */
@Composable
fun AutoScrollWhileDragging(state: ExerciseDragState, scrollState: ScrollState) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 96.dp.toPx() }
    val maxSpeedPx = with(density) { 16.dp.toPx() }

    LaunchedEffect(state.draggingIndex) {
        if (state.draggingIndex < 0) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            val delta = state.autoScrollDeltaPx(thresholdPx, maxSpeedPx)
            if (delta != 0f) {
                scrollState.dispatchRawDelta(delta)
                state.drag(delta)
            }
        }
    }
}
