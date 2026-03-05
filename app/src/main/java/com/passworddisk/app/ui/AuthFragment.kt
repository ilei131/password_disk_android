package com.passworddisk.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.passworddisk.app.R
import com.passworddisk.app.databinding.FragmentAuthBinding
import com.passworddisk.app.viewmodel.PasswordViewModel

class AuthFragment : Fragment() {
    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PasswordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.isVaultInitialized.observe(viewLifecycleOwner) { initialized ->
            if (initialized) {
                showLoginMode()
            } else {
                showRegisterMode()
            }
        }

        viewModel.isAuthenticated.observe(viewLifecycleOwner) { authenticated ->
            if (authenticated) {
                findNavController().navigate(R.id.action_auth_to_home)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        binding.toggleModeButton.setOnClickListener {
            toggleMode()
        }

        binding.submitButton.setOnClickListener {
            submitForm()
        }
    }

    private fun showLoginMode() {
        binding.titleText.text = getString(R.string.login_title)
        binding.submitButton.text = getString(R.string.login_button)
        binding.confirmPasswordLayout.visibility = View.GONE
        binding.toggleModeButton.text = getString(R.string.switch_to_register)
        binding.toggleModeButton.visibility = View.VISIBLE
    }

    private fun showRegisterMode() {
        binding.titleText.text = getString(R.string.register_title)
        binding.submitButton.text = getString(R.string.register_button)
        binding.confirmPasswordLayout.visibility = View.VISIBLE
        binding.toggleModeButton.visibility = View.GONE
    }

    private fun toggleMode() {
        val isLoginMode = binding.confirmPasswordLayout.visibility == View.GONE
        if (isLoginMode) {
            showRegisterMode()
        } else {
            showLoginMode()
        }
    }

    private fun submitForm() {
        val password = binding.passwordInput.text.toString()

        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.password_required)
            return
        }
        binding.passwordLayout.error = null

        if (binding.confirmPasswordLayout.visibility == View.VISIBLE) {
            val confirmPassword = binding.confirmPasswordInput.text.toString()
            viewModel.initializeVault(password, confirmPassword)
        } else {
            viewModel.login(password)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}