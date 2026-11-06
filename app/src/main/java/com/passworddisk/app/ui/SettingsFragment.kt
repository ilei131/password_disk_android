package com.passworddisk.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.passworddisk.app.BuildConfig
import com.passworddisk.app.R
import com.passworddisk.app.databinding.FragmentSettingsBinding
import com.passworddisk.app.viewmodel.PasswordViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PasswordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.categoriesRow.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_categories)
        }
        binding.cloudSyncRow.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_cloudSync)
        }
        binding.backupRow.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_backupToCloud)
        }
        binding.privacyRow.setOnClickListener { openUrl(getString(R.string.privacy_policy_url)) }
        binding.termsRow.setOnClickListener { openUrl(getString(R.string.terms_url)) }
        binding.deleteDataRow.setOnClickListener { showDeleteDataDialog(viewModel) }
        binding.logoutRow.setOnClickListener { confirmLock() }
        binding.versionText.text = getString(R.string.version_label, BuildConfig.VERSION_NAME)

        observeMessages()
    }

    private fun confirmLock() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.lock_vault))
            .setMessage(getString(R.string.logout_confirm))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.lock_vault)) { _, _ ->
                viewModel.logout()
                findNavController().navigate(R.id.action_settings_to_auth)
            }
            .show()
    }

    private fun openUrl(url: String) {
        if (!ExternalLinks.open(requireContext(), url)) {
            snackbar(getString(R.string.link_open_failed))
        }
    }

    private fun snackbar(message: String, kind: Notify.Kind = Notify.Kind.INFO) {
        if (_binding == null) return
        Notify.show(binding.root, message, kind)
    }

    private fun observeMessages() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                snackbar(it, Notify.Kind.ERROR)
                viewModel.clearMessages()
            }
        }
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                snackbar(it, Notify.Kind.SUCCESS)
                viewModel.clearMessages()
            }
        }
        viewModel.vaultWiped.observe(viewLifecycleOwner) { wiped ->
            if (wiped) {
                viewModel.consumeVaultWiped()
                findNavController().navigate(R.id.action_settings_to_auth)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
