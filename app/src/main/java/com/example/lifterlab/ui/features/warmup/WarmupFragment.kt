package com.example.lifterlab.ui.features.warmup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.appcompat.widget.PopupMenu
import com.example.lifterlab.R
import com.example.lifterlab.databinding.FragmentWarmupBinding
import com.example.lifterlab.databinding.ItemWarmupSetBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class WarmupFragment : Fragment() {

    private var _binding: FragmentWarmupBinding? = null
    private val binding get() = _binding!!
    
    // Using default ViewModelProvider, assuming no special Factory for now.
    private val viewModel: WarmupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarmupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {
        binding.etExerciseName.doAfterTextChanged { 
            viewModel.updateExerciseName(it.toString())
        }
        
        binding.etWeight.doAfterTextChanged {
            viewModel.updateWeight(it.toString())
        }
        
        binding.etReps.doAfterTextChanged {
            viewModel.updateReps(it.toString())
        }
        
        binding.btnCalculate.setOnClickListener {
            viewModel.calculate1RM()
        }
        
        binding.btnSaveSession.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
            viewModel.saveSession(userId)
        }
        
        binding.btnOptions.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            popup.menuInflater.inflate(R.menu.menu_main, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_profile -> {
                        findNavController().navigate(R.id.action_WarmupFragment_to_profileFragment)
                        true
                    }
                    R.id.action_warmup -> {
                        true
                    }
                    R.id.action_sign_out -> {
                        FirebaseAuth.getInstance().signOut()
                        findNavController().navigate(R.id.loginFragment)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.btnCalculate.isEnabled = !state.isLoading
                    binding.btnSaveSession.isEnabled = !state.isLoading && state.estimated1RM != null
                    
                    if (state.estimated1RM != null) {
                        binding.tv1RMResult.text = "${state.estimated1RM} kg"
                        renderPercentages(state.percentages)
                    } else {
                        binding.tv1RMResult.text = "-- kg"
                        binding.llPercentagesContainer.removeAllViews()
                    }
                    
                    if (state.errorMessage != null) {
                        Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
                        viewModel.resetState() // Clear error after showing
                    }
                    
                    if (state.isSavedSuccessfully) {
                        Toast.makeText(requireContext(), "Sesión guardada exitosamente", Toast.LENGTH_SHORT).show()
                        binding.etWeight.text?.clear()
                        binding.etReps.text?.clear()
                        binding.etExerciseName.text?.clear()
                        viewModel.resetState()
                    }
                }
            }
        }
    }
    
    private fun renderPercentages(percentages: List<Pair<Int, Double>>) {
        binding.llPercentagesContainer.removeAllViews()
        
        for ((percent, weight) in percentages) {
            val itemBinding = ItemWarmupSetBinding.inflate(layoutInflater, binding.llPercentagesContainer, false)
            
            itemBinding.tvPercentageBadge.text = "$percent%"
            itemBinding.tvSetWeight.text = "$weight kg"
            
            // Progressive powerlifting warmup reps recommendation
            val suggestedReps = when {
                percent <= 70 -> "5 reps"
                percent <= 75 -> "3-4 reps"
                percent <= 80 -> "3 reps"
                percent <= 85 -> "2 reps"
                percent <= 90 -> "1 rep"
                else -> "1 rep"
            }
            itemBinding.tvRecommendedReps.text = suggestedReps
            
            // Accent highlight by intensity tier
            when {
                percent >= 90 -> {
                    itemBinding.tvPercentageBadge.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.error_accent)
                    )
                }
                percent >= 80 -> {
                    itemBinding.tvPercentageBadge.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.secondary_accent)
                    )
                }
                else -> {
                    itemBinding.tvPercentageBadge.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.primary_container)
                    )
                }
            }
            
            binding.llPercentagesContainer.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
