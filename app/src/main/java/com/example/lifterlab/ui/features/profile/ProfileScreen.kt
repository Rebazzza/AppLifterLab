package com.example.lifterlab.ui.features.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifterlab.data.model.UserProfile
import com.example.lifterlab.data.repository.AuthRepository
import com.example.lifterlab.data.repository.ProfileRepository
import com.example.lifterlab.toast
import com.example.lifterlab.ui.components.CardTitle
import com.example.lifterlab.ui.components.ErrorText
import com.example.lifterlab.ui.components.FieldLabel
import com.example.lifterlab.ui.components.LifterCard
import com.example.lifterlab.ui.components.LifterTextField
import com.example.lifterlab.ui.components.PrimaryButton
import com.example.lifterlab.ui.components.ScreenSubtitle
import com.example.lifterlab.ui.components.ScreenTitle
import com.example.lifterlab.ui.components.SecondaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    authRepository: AuthRepository = remember { AuthRepository() },
    profileRepository: ProfileRepository = remember { ProfileRepository() }
) {
    val userId = remember { authRepository.getCurrentUser()?.uid.orEmpty() }
    val email = remember { authRepository.getCurrentUser()?.email.orEmpty() }

    var cargando by remember { mutableStateOf(true) }
    var cargaError by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }

    var perfil by remember { mutableStateOf<UserProfile?>(null) }
    var altura by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(userId) {
        if (userId.isEmpty()) {
            cargando = false
            cargaError = true
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.IO) { profileRepository.getProfile(userId) }
        result.onSuccess { p ->
            perfil = p
            altura = p.height.takeIf { it > 0 }?.toInt().toString()
            peso = p.currentWeightKg.takeIf { it > 0 }?.toString().orEmpty()
            cargando = false
        }.onFailure {
            cargando = false
            cargaError = true
            mensajeError = it.message ?: "Error de conexión o perfil inexistente."
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "← Volver",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onBack() }
            )

            ScreenTitle("Perfil", Modifier.padding(top = 8.dp))

            if (cargando) {
                ScreenSubtitle(
                    text = if (cargaError) "No se pudo cargar el perfil." else "Cargando perfil...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }

            val current = perfil
            LifterCard(Modifier.padding(top = 16.dp)) {
                InfoRow("Nombre", current?.name?.ifEmpty { "Sin nombre" } ?: "")
                InfoRow("Correo", email.ifEmpty { "—" })
                InfoRow("Año de nacimiento", if (current?.birthYear ?: 0 > 0) current!!.birthYear.toString() else "—")
                InfoRow("Sexo", current?.gender?.ifEmpty { "—" } ?: "—")
            }

            LifterCard(Modifier.padding(top = 16.dp)) {
                CardTitle("Medidas (editable)")

                FieldLabel("Altura (cm)", Modifier.padding(top = 16.dp))
                Spacer(Modifier.height(4.dp))
                LifterTextField(
                    value = altura,
                    onValueChange = { altura = it },
                    label = "Ej. 175 cm",
                    keyboardType = KeyboardType.Decimal
                )

                FieldLabel("Peso corporal (kg)", Modifier.padding(top = 16.dp))
                Spacer(Modifier.height(4.dp))
                LifterTextField(
                    value = peso,
                    onValueChange = { peso = it },
                    label = "Ej. 80 kg",
                    keyboardType = KeyboardType.Decimal
                )
            }

            if (mensajeError.isNotEmpty() && !cargando) {
                Spacer(Modifier.height(12.dp))
                ErrorText(mensajeError, Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = if (guardando) "Guardando..." else "Guardar Cambios",
                onClick = {
                    guardando = true
                    mensajeError = ""
                    postGuardar(
                        userId = userId,
                        altura = altura,
                        peso = peso,
                        scope = scope,
                        context = context,
                        profileRepository = profileRepository,
                        onError = { msg ->
                            guardando = false
                            mensajeError = msg
                        },
                        onSuccess = {
                            guardando = false
                            mensajeError = ""
                            context.toast("Perfil actualizado")
                        }
                    )
                },
                enabled = !guardando
            )

            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = "Cerrar Sesión",
                onClick = { onSignOut() },
                minHeight = 52.dp
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, labelTopGap: androidx.compose.ui.unit.Dp = 16.dp) {
    FieldLabel(label, Modifier.padding(top = labelTopGap))
    Spacer(Modifier.height(4.dp))
    Text(
        text = value,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
}

private fun postGuardar(
    userId: String,
    altura: String,
    peso: String,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    profileRepository: ProfileRepository,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    val height = altura.trim().toDoubleOrNull()
    val weight = peso.trim().toDoubleOrNull()
    if (height == null || height <= 0 || weight == null || weight <= 0) {
        onError("Ingresa una altura y un peso válidos.")
        return
    }
    if (userId.isEmpty()) {
        onError("No hay una sesión iniciada.")
        return
    }
    scope.launch {
        val current = withContext(Dispatchers.IO) {
            profileRepository.getProfile(userId).getOrNull()
        }
        val updated = UserProfile(
            name = current?.name.orEmpty(),
            birthYear = current?.birthYear ?: 0,
            gender = current?.gender.orEmpty(),
            height = height,
            currentWeightKg = weight,
            baseBarWeightKg = current?.baseBarWeightKg ?: 20.0
        )
        val result = withContext(Dispatchers.IO) {
            profileRepository.updateProfile(userId, updated)
        }
        result
            .onSuccess { onSuccess() }
            .onFailure { e -> onError(e.message ?: "No se pudo guardar los cambios.") }
    }
}