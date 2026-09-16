package com.example.lifterlab.ui.features.auth

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
import com.example.lifterlab.R
import com.example.lifterlab.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()
            val bodyweight = binding.etWeight.text.toString()
            val barWeight = binding.etBarWeight.text.toString()

            viewModel.register(name, email, pass, confirmPass, bodyweight, barWeight)
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.btnRegister.isEnabled = !state.isLoading
                    binding.tvGoToLogin.isEnabled = !state.isLoading

                    if (state.errorMessage != null) {
                        Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }

                    if (state.isSuccess && state.isUserLoggedIn) {
                        Toast.makeText(requireContext(), "¡Registro exitoso! Bienvenido a LifterLab.", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registerFragment_to_warmupFragment)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
