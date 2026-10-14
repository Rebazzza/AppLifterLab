package com.example.lifterlab.ui.features.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.formatKg

/**
 * RF07 — Visualizador de carga de barra IPF. Dado un peso objetivo dibuja los discos exactos
 * por lado (barra de 20 kg, colores IPF) y avisa si el peso no se puede armar con los discos
 * estándar, sugiriendo los pesos armables más cercanos.
 */
@Composable
fun PlateLoaderScreen(onBack: () -> Unit) {
    var targetText by remember { mutableStateOf("") }
    var withCollars by remember { mutableStateOf(false) }

    val target = targetText.replace(',', '.').toDoubleOrNull()
    val error = PlateCalculator.validate(target)
    val load = if (error == null && target != null) PlateCalculator.compute(target, withCollars) else null

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 104.dp)
        ) {
            Text(
                text = "← Volver",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onBack() }
            )
            ScreenTitle("Carga de Barra", Modifier.padding(top = 8.dp))
            ScreenSubtitle(
                "Discos IPF por lado con barra de ${formatKg(PlateCalculator.BAR_KG)} kg.",
                Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            LifterCard {
                ToolField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = "Peso objetivo (kg)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { withCollars = !withCollars }
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = withCollars, onCheckedChange = { withCollars = it })
                    Text(
                        text = "Usar seguros (${formatKg(PlateCalculator.COLLARS_TOTAL_KG)} kg en total)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            if (targetText.isNotBlank() && error != null) {
                ErrorText(error, Modifier.fillMaxWidth().padding(top = 12.dp))
            }

            if (load != null) {
                if (!load.isExact) {
                    NotLoadableBanner(
                        load = load,
                        onSelectTarget = { targetText = formatKg(it) }
                    )
                }

                LifterCard(Modifier.padding(top = 16.dp)) {
                    CardCaption(if (load.isExact) "CARGA EXACTA" else "CARGA MÁS CERCANA POR DEBAJO", mono = true)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = formatKg(load.loadedKg),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = " KG",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    BarbellCanvas(load, Modifier.padding(top = 16.dp))

                    if (load.perSide.isEmpty()) {
                        Text(
                            text = "Solo la barra: sin discos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    } else {
                        CardCaption("POR LADO", Modifier.padding(top = 16.dp), mono = true)
                        load.perSide.groupingBy { it }.eachCount().forEach { (plate, count) ->
                            PlateLegendRow(plate, count)
                        }
                    }
                    if (load.collarsTotalKg > 0) {
                        CardCaption(
                            "+ Seguros: ${formatKg(load.collarsTotalKg / 2)} kg por lado",
                            Modifier.padding(top = 10.dp),
                            mono = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotLoadableBanner(
    load: PlateLoad,
    onSelectTarget: (Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(Color(0xFF201F22), RoundedCornerShape(12.dp))
            .border(BorderStroke(2.dp, Color(0xFFFFB4AB)), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Alerta",
                tint = Color(0xFFFFB4AB),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "PESO NO ARMABLE",
                    color = Color(0xFFFFB4AB),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatKg(load.targetKg)} kg no se puede armar con los discos estándar (el disco más pequeño es de 1.25 kg por lado).",
                    color = Color(0xFFE6E1E6),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            load.nearestLowerKg?.let { lower ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C1B1D))
                        .border(1.dp, Color(0xFF2A2A2C), RoundedCornerShape(8.dp))
                        .clickable { onSelectTarget(lower) }
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Usar ${formatKg(lower)} kg",
                        color = Color(0xFFC1C1FB),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            load.nearestUpperKg?.let { upper ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C1B1D))
                        .border(1.dp, Color(0xFF2A2A2C), RoundedCornerShape(8.dp))
                        .clickable { onSelectTarget(upper) }
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Usar ${formatKg(upper)} kg",
                        color = Color(0xFFC1C1FB),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlateLegendRow(plate: IpfPlate, count: Int) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(Color(plate.argb), RoundedCornerShape(4.dp))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(4.dp))
        )
        Text(
            text = "$count × ${formatKg(plate.kg)} kg",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

/** Vista lateral de un extremo de la barra: manguito, discos (del centro hacia afuera) y seguro. */
@Composable
private fun BarbellCanvas(load: PlateLoad, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val barColor = Color(0xFF9E9EA6)
    val collarColor = Color(0xFF6E6E76)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val centerY = size.height / 2f
        val fullPlateHeight = size.height * 0.95f
        val gap = 3.dp.toPx()

        // Barra: el trazo cruza todo el ancho; el disco de tope (collar interior) marca el inicio del manguito.
        drawRect(barColor, Offset(0f, centerY - 5.dp.toPx()), Size(size.width, 10.dp.toPx()))
        drawRoundRect(
            collarColor,
            Offset(0f, centerY - 16.dp.toPx()),
            Size(10.dp.toPx(), 32.dp.toPx()),
            CornerRadius(3.dp.toPx())
        )

        // Con muchos discos el conjunto se comprime para caber en el ancho disponible.
        val startX = 16.dp.toPx()
        val collarSpace = if (load.collarsTotalKg > 0) 12.dp.toPx() else 0f
        val naturalWidth = load.perSide.sumOf { (plateWidthDp(it).dp.toPx() + gap).toDouble() }.toFloat()
        val available = size.width - startX - collarSpace
        val scale = if (naturalWidth > available && naturalWidth > 0f) available / naturalWidth else 1f

        var x = startX
        load.perSide.forEach { plate ->
            val w = plateWidthDp(plate).dp.toPx() * scale
            val h = fullPlateHeight * plateHeightFactor(plate)
            val topLeft = Offset(x, centerY - h / 2f)
            drawRoundRect(Color(plate.argb), topLeft, Size(w, h), CornerRadius(3.dp.toPx()))
            drawRoundRect(
                outline,
                topLeft,
                Size(w, h),
                CornerRadius(3.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
            x += w + gap * scale
        }

        if (load.collarsTotalKg > 0) {
            drawRoundRect(
                collarColor,
                Offset(x, centerY - 14.dp.toPx()),
                Size(8.dp.toPx(), 28.dp.toPx()),
                CornerRadius(2.dp.toPx())
            )
        }
    }
}

/** Los discos IPF de 10 kg o más miden lo mismo de diámetro; los de 5, 2.5 y 1.25 kg son menores. */
private fun plateHeightFactor(plate: IpfPlate): Float = when (plate) {
    IpfPlate.WHITE_5 -> 0.62f
    IpfPlate.BLACK_2_5 -> 0.5f
    IpfPlate.SILVER_1_25 -> 0.4f
    else -> 1f
}

private fun plateWidthDp(plate: IpfPlate): Float = when (plate) {
    IpfPlate.RED_25 -> 20f
    IpfPlate.BLUE_20 -> 18f
    IpfPlate.YELLOW_15 -> 16f
    IpfPlate.GREEN_10 -> 14f
    IpfPlate.WHITE_5 -> 12f
    IpfPlate.BLACK_2_5 -> 10f
    IpfPlate.SILVER_1_25 -> 8f
}
