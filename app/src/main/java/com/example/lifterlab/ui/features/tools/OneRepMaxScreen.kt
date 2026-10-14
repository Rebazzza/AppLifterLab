package com.example.lifterlab.ui.features.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.FieldCornerRadius
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.ui.components.ScreenTitle
import java.util.Locale

/**
 * RF06 — Calculadora de 1RM estimado. Con peso y repeticiones validados muestra el 1RM
 * por Brzycki y por Epley, más la tabla de porcentajes de aproximación.
 */
@Composable
fun OneRepMaxScreen(onBack: () -> Unit) {
    var weightText by remember { mutableStateOf("") }
    var repsText by remember { mutableStateOf("") }

    val weight = weightText.replace(',', '.').toDoubleOrNull()
    val reps = repsText.toIntOrNull()
    val hasInput = weightText.isNotBlank() || repsText.isNotBlank()
    val error = OneRepMax.validate(weight, reps)

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
            ScreenTitle("Calculadora 1RM", Modifier.padding(top = 8.dp))
            ScreenSubtitle(
                "Estima tu repetición máxima a partir de una serie.",
                Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            LifterCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = "Peso (kg)",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    ToolField(
                        value = repsText,
                        onValueChange = { repsText = it.filter(Char::isDigit) },
                        label = "Repeticiones",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
                CardCaption(
                    "Válido de 1 a ${OneRepMax.MAX_REPS} repeticiones.",
                    Modifier.padding(top = 8.dp)
                )
            }

            if (hasInput && error != null) {
                ErrorText(error, Modifier.fillMaxWidth().padding(top = 12.dp))
            }

            if (error == null && weight != null && reps != null) {
                val brzycki = OneRepMax.brzycki(weight, reps)
                val epley = OneRepMax.epley(weight, reps)

                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResultCard("BRZYCKI", brzycki, "peso × 36 ÷ (37 − reps)", Modifier.weight(1f))
                    ResultCard("EPLEY", epley, "peso × (1 + reps ÷ 30)", Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF201F22))
                        .border(1.dp, Color(0xFF2A2A2C), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = Color(0xFFC1C1FB),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Con 1 repetición, el 1RM es el mismo peso levantado. Para estimaciones con alta precisión técnica se recomienda testear entre 2 y 5 reps.",
                        color = Color(0xFFC9C4CA),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                LifterCard(Modifier.padding(top = 16.dp)) {
                    CardCaption("ZONAS DE CARGA (BRZYCKI) · 1RM BASE ${formatOneRm(brzycki)} KG")
                    OneRepMax.APPROACH_PERCENTS.forEach { percent ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = "$percent%",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${formatOneRm(brzycki * percent / 100.0)} kg",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatOneRm(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

@Composable
private fun ResultCard(
    title: String,
    valueKg: Double,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    LifterCard(modifier) {
        CardCaption(title, mono = true)
        Text(
            text = formatOneRm(valueKg),
            color = MaterialTheme.colorScheme.primaryContainer,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "KG",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** Campo numérico con el estilo de los campos de la app (reutilizado por la calculadora de discos). */
@Composable
internal fun ToolField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(FieldCornerRadius),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
        )
    )
}
