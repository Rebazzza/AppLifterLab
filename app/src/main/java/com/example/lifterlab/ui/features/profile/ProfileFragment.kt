package com.example.lifterlab.ui.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.appcompat.widget.PopupMenu
import com.example.lifterlab.R
import com.example.lifterlab.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Fragment del módulo de Perfil del Atleta.
 *
 * Responsabilidades:
 * - Mostrar datos del perfil cargados desde Firestore via [ProfileViewModel].
 * - Permitir editar peso corporal y peso de barra base.
 * - Gestionar el cierre de sesión navegando de vuelta al loginFragment.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    /** UID del usuario autenticado actualmente. Nunca null en este Fragment. */
    private val currentUserId: String by lazy {
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeUiState()

        // Disparar carga del perfil al entrar a la pantalla
        if (currentUserId.isNotEmpty()) {
            viewModel.loadProfile(currentUserId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private fun setupListeners() {
        // Guardar los cambios editables (peso corporal y peso de barra)
        binding.btnSaveProfile.setOnClickListener {
            val weightStr = binding.etBodyWeight.text.toString()
            val barStr = binding.etBarWeight.text.toString()
            viewModel.saveProfileChanges(currentUserId, weightStr, barStr)
        }

        // Cerrar sesión: limpiar back stack y navegar a Login
        binding.btnSignOut.setOnClickListener {
            viewModel.signOut()
            findNavController().navigate(
                R.id.action_profileFragment_to_loginFragment
            )
        }

        // Toggle de unidad de peso (visual únicamente en esta versión)
        binding.switchWeightUnit.setOnCheckedChangeListener { _, isChecked ->
            val label = if (isChecked) "Libras (lb)" else "Kilogramos (kg)"
            // El TextView de descripción de la fila se encuentra dentro del LinearLayout padre
            // Se accede por posición ya que el switch y su label comparten el mismo contenedor
            Toast.makeText(
                requireContext(),
                "Unidad cambiada a $label (próximamente)",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnOptions.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            popup.menuInflater.inflate(R.menu.menu_main, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_profile -> {
                        true
                    }
                    R.id.action_warmup -> {
                        findNavController().navigate(R.id.action_profileFragment_to_WarmupFragment)
                        true
                    }
                    R.id.action_sign_out -> {
                        viewModel.signOut()
                        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    // -------------------------------------------------------------------------
    // State Observation
    // -------------------------------------------------------------------------

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    /**
     * Aplica el [ProfileUiState] completo a las vistas del layout.
     * Actualiza los campos solo cuando el usuario no está editando activamente
     * para no interrumpir su escritura (se usa isEmpty() como heurística simple).
     */
    private fun renderState(state: ProfileUiState) {

        // --- Loading ---
        binding.progressBarProfile.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.btnSaveProfile.isEnabled = !state.isLoading
        binding.btnSignOut.isEnabled = !state.isLoading

        // --- Header ---
        binding.tvAthleteFullName.text = state.name.ifEmpty { "Atleta" }
        binding.tvAthleteEmail.text = state.email
        val initial = state.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        binding.tvAvatarInitial.text = initial

        // --- Datos fijos (solo lectura) ---
        binding.tvGenderValue.text = when (state.gender.lowercase()) {
            "male", "masculino", "m" -> "Masculino"
            "female", "femenino", "f" -> "Femenino"
            else -> state.gender.ifEmpty { "—" }
        }
        binding.tvBirthYearValue.text = if (state.birthYear > 0) state.birthYear.toString() else "—"

        // --- Campos editables (solo rellenar si están vacíos para no pisar la edición del usuario) ---
        if (binding.etBodyWeight.text.isNullOrEmpty()) {
            val weightText = if (state.currentWeightKg > 0.0)
                state.currentWeightKg.toBigDecimal().stripTrailingZeros().toPlainString()
            else ""
            binding.etBodyWeight.setText(weightText)
        }

        if (binding.etBarWeight.text.isNullOrEmpty()) {
            val barText = if (state.baseBarWeightKg >= 0.0)
                state.baseBarWeightKg.toBigDecimal().stripTrailingZeros().toPlainString()
            else ""
            binding.etBarWeight.setText(barText)
        }

        // --- Mensajes transitorios ---
        state.errorMessage?.let { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            viewModel.resetTransientState()
        }

        if (state.isSavedSuccessfully) {
            Toast.makeText(requireContext(), "Perfil actualizado correctamente ✓", Toast.LENGTH_SHORT).show()
            viewModel.resetTransientState()
        }
    }
}
