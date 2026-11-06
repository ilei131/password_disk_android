package com.passworddisk.app.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
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

    private var registerMode = false

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
            applyMode(registerMode = initialized == false)
        }

        viewModel.isAuthenticated.observe(viewLifecycleOwner) { authenticated ->
            if (authenticated) {
                hideLoading()
                binding.errorText.isVisible = false
                findNavController().navigate(R.id.action_auth_to_home)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                hideLoading()
                showError(it)
                viewModel.clearMessages()
            }
        }

        binding.submitButton.setOnClickListener { submit() }
        binding.legalNote.setOnClickListener {
            if (!ExternalLinks.open(requireContext(), getString(R.string.privacy_policy_url))) {
                showError(getString(R.string.link_open_failed))
            }
        }
        binding.toggleModeButton.setOnClickListener {
            applyMode(registerMode = !registerMode)
        }
        binding.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                submit()
                true
            } else {
                false
            }
        }
        binding.confirmPasswordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                submit()
                true
            } else {
                false
            }
        }
        binding.passwordInput.setOnFocusChangeListener { _, _ -> binding.errorText.isVisible = false }
        binding.confirmPasswordInput.setOnFocusChangeListener { _, _ -> binding.errorText.isVisible = false }
    }

    private fun applyMode(registerMode: Boolean) {
        this.registerMode = registerMode
        binding.titleText.text = getString(
            if (registerMode) R.string.register_title else R.string.login_title
        )
        binding.authSubtitle.text = getString(
            if (registerMode) R.string.register_subtitle else R.string.login_subtitle
        )
        binding.submitButton.text = getString(
            if (registerMode) R.string.register_button else R.string.login_button
        )
        binding.submitButton.setIconResource(
            if (registerMode) R.drawable.ic_shield else R.drawable.ic_lock
        )
        binding.confirmPasswordLayout.isVisible = registerMode
        binding.toggleModeButton.isVisible = false
    }

    private fun submit() {
        val password = binding.passwordInput.text?.toString().orEmpty()

        if (password.isEmpty()) {
            showError(getString(R.string.password_required))
            binding.passwordLayout.error = getString(R.string.password_required)
            return
        }
        binding.passwordLayout.error = null

        if (registerMode) {
            val confirm = binding.confirmPasswordInput.text?.toString().orEmpty()
            if (confirm.isEmpty()) {
                binding.confirmPasswordLayout.error = getString(R.string.password_required)
                return
            }
            binding.confirmPasswordLayout.error = null
            if (password != confirm) {
                showError(getString(R.string.passwords_do_not_match))
                return
            }
            if (password.length < 6) {
                showError(getString(R.string.password_min_length))
                return
            }
        }

        hideKeyboard()
        showLoading()
        if (registerMode) {
            viewModel.initializeVault(password, binding.confirmPasswordInput.text?.toString().orEmpty())
        } else {
            viewModel.login(password)
        }
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.isVisible = true
    }

    private fun showLoading() {
        binding.submitButton.isEnabled = false
        binding.toggleModeButton.isEnabled = false
        binding.loadingIndicator.isVisible = true
        binding.submitButton.alpha = 0.75f
    }

    private fun hideLoading() {
        if (_binding == null) return
        binding.submitButton.isEnabled = true
        binding.toggleModeButton.isEnabled = true
        binding.loadingIndicator.isVisible = false
        binding.submitButton.alpha = 1f
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.passwordInput.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
