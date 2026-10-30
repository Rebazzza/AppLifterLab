package com.example.lifterlab.ui.features.competition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.Competition
import com.example.lifterlab.data.model.CompetitionAttempt
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.CompetitionRepository
import com.example.lifterlab.data.repository.ProfileRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.CardCaption
import com.example.lifterlab.ui.components.CardTitle
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.FieldLabel
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.LifterTextField
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.components.bottomBarClearance
import com.example.lifterlab.ui.formatKg
import com.example.lifterlab.ui.theme.ColorBench
import com.example.lifterlab.ui.theme.ColorDeadlift
import com.example.lifterlab.ui.theme.ColorSquat
import com.example.lifterlab.ui.theme.TertiaryAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Estado editable de un intento: texto de peso y si fue válido o nulo. */
private data class AttemptInput(
    val weightText: String = "",
    val isSuccessful: Boolean = false
)

private fun AttemptInput.toAttempt(): CompetitionAttempt = CompetitionAttempt(
    weightKg = weightText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0,
    isSuccessful = isSuccessful
)

private val LIFT_ACCENTS = mapOf(
    "squat" to ColorSquat,
    "bench" to ColorBench,
    "deadlift" to ColorDeadlift
)

/**
 * RF09 / RF10 — Módulo de competencia: planifica y registra los 3 intentos oficiales de
 * Sentadilla, Press Banca y Peso Muerto (cada uno válido o nulo), calcula el total SBD y el
 * puntaje DOTS con el peso corporal del atleta, y guarda la competencia.
 */
@Composable
fun CompetitionScreen(
    onBack: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    competitionRepository: CompetitionRepository = remember { CompetitionRepository() },
    profileRepository: ProfileRepository = remember { ProfileRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var bodyweightText by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(CompetitionSex.MALE) }

    // Tres intentos por básico, en el mismo orden que Firestore ("squat"/"bench"/"deadlift").
    var intentos by remember {
        mutableStateOf(DotsCalculator.LIFTS.associateWith { List(3) { AttemptInput() } })
    }

    var guardando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val perfil = withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
            perfil.onSuccess { profile ->
                if (profile.currentWeightKg > 0) {
                    bodyweightText = formatKg(profile.currentWeightKg)
                }
                sex = DotsCalculator.sexFromProfile(profile.gender)
            }
        }
    }

    val attemptsByLift = intentos.mapValues { (_, list) -> list.map { it.toAttempt() } }
    val total = DotsCalculator.total(attemptsByLift)
    val bodyweight = bodyweightText.replace(',', '.').trim().toDoubleOrNull()
    val bodyweightError = DotsCalculator.validateBodyweight(bodyweight)
    val dots = if (bodyweightError == null) {
        DotsCalculator.dotsScore(total, bodyweight ?: 0.0, sex)
    } else {
        0.0
    }

    fun setWeight(lift: String, index: Int, value: String) {
        intentos = intentos.mapValues { (key, list) ->
            if (key != lift) list
            else list.toMutableList().also { it[index] = it[index].copy(weightText = value) }
        }
    }

    fun toggleValid(lift: String, index: Int) {
        intentos = intentos.mapValues { (key, list) ->
            if (key != lift) list
            else list.toMutableList().also {
                it[index] = it[index].copy(isSuccessful = !it[index].isSuccessful)
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = bottomBarClearance())
        ) {
            Text(
                text = "← Volver",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onBack() }
            )
            ScreenTitle("Competencia", Modifier.padding(top = 8.dp))
            ScreenSubtitle(
                "Planifica y registra tus 3 intentos de SBD. El total y el DOTS se calculan solos.",
                Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            LifterCard {
                LifterTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre de la competencia (opcional)"
                )
                Spacer(Modifier.height(12.dp))
                LifterTextField(
                    value = bodyweightText,
                    onValueChange = { bodyweightText = it },
                    label = "Peso corporal (kg)",
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(Modifier.height(16.dp))
                FieldLabel("SEXO PARA EL DOTS")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SexChip(
                        text = "Masculino",
                        selected = sex == CompetitionSex.MALE,
                        onClick = { sex = CompetitionSex.MALE },
                        modifier = Modifier.weight(1f)
                    )
                    SexChip(
                        text = "Femenino",
                        selected = sex == CompetitionSex.FEMALE,
                        onClick = { sex = CompetitionSex.FEMALE },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (bodyweightText.isNotBlank() && bodyweightError != null) {
                    ErrorText(bodyweightError, Modifier.padding(top = 10.dp))
                }
            }

            DotsCalculator.LIFTS.forEach { lift ->
                LiftAttemptsCard(
                    title = DotsCalculator.LIFT_LABELS.getValue(lift),
                    accentColor = LIFT_ACCENTS.getValue(lift),
                    attempts = intentos.getValue(lift),
                    onWeightChange = { index, value -> setWeight(lift, index, value) },
                    onToggleValid = { index -> toggleValid(lift, index) },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            TotalCard(total = total, dots = dots, hasBodyweight = bodyweightError == null)

            if (mensajeError.isNotEmpty()) {
                ErrorText(mensajeError, Modifier.padding(top = 12.dp))
            }

            PrimaryButton(
                text = if (guardando) "Guardando..." else "GUARDAR COMPETENCIA",
                onClick = {
                    mensajeError = ""
                    if (userId.isEmpty()) {
                        mensajeError = "No hay una sesión iniciada."
                        return@PrimaryButton
                    }
                    if (bodyweightError != null) {
                        mensajeError = bodyweightError
                        return@PrimaryButton
                    }
                    if (total <= 0.0) {
                        mensajeError = "Marca al menos un intento válido para registrar la competencia."
                        return@PrimaryButton
                    }
                    val attemptError = attemptsByLift.values.flatten()
                        .mapNotNull { DotsCalculator.validateAttemptWeight(it.weightKg.takeIf { w -> w > 0 }) }
                        .firstOrNull()
                    if (attemptError != null) {
                        mensajeError = attemptError
                        return@PrimaryButton
                    }

                    val competition = Competition(
                        name = name.trim().ifEmpty { "Competencia" },
                        athleteWeightKg = bodyweight ?: 0.0,
                        dotsScore = dots,
                        attempts = attemptsByLift
                    )

                    guardando = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            competitionRepository.saveCompetition(userId, competition)
                        }
                        guardando = false
                        result
                            .onSuccess { context.toast("Competencia guardada") }
                            .onFailure { e -> mensajeError = e.message ?: "No se pudo guardar la competencia." }
                    }
                },
                modifier = Modifier.padding(top = 20.dp),
                enabled = !guardando
            )
        }
    }
}

@Composable
private fun SexChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(46.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = shape
            )
            .border(
                BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                shape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LiftAttemptsCard(
    title: String,
    accentColor: Color,
    attempts: List<AttemptInput>,
    onWeightChange: (Int, String) -> Unit,
    onToggleValid: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LifterCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardTitle(title, Modifier.weight(1f))
            val best = DotsCalculator.bestSuccessful(attempts.map { it.toAttempt() })
            Text(
                text = best?.let { "MEJOR ${formatKg(it)} KG" } ?: "SIN MARCA",
                color = if (best != null) TertiaryAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }

        attempts.forEachIndexed { index, attempt ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "I${index + 1}",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(28.dp)
                )
                LifterTextField(
                    value = attempt.weightText,
                    onValueChange = { onWeightChange(index, it) },
                    label = "kg",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                ValidToggle(
                    isSuccessful = attempt.isSuccessful,
                    onClick = { onToggleValid(index) }
                )
            }
        }
    }
}

@Composable
private fun ValidToggle(isSuccessful: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val color = if (isSuccessful) TertiaryAccent else MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier
            .width(76.dp)
            .height(52.dp)
            .background(color.copy(alpha = 0.16f), shape)
            .border(BorderStroke(1.dp, color), shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isSuccessful) "VÁLIDO" else "NULO",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TotalCard(total: Double, dots: Double, hasBodyweight: Boolean) {
    LifterCard(Modifier.padding(top = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                CardCaption("TOTAL SBD", mono = true)
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = formatKg(total),
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
            }
            Column(horizontalAlignment = Alignment.End) {
                CardCaption("PUNTAJE DOTS", mono = true)
                Text(
                    text = if (hasBodyweight) String.format(Locale.ROOT, "%.2f", dots) else "—",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Text(
            text = "El DOTS cruza el total con tu peso corporal y tu sexo (RNF05: sin errores de redondeo).",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
