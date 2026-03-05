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
import com.passworddisk.app.databinding.FragmentSettingsBinding
import com.passworddisk.app.viewmodel.PasswordViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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

        setupButtons()
        observeData()
    }

    private fun setupButtons() {
        binding.manageCategoriesButton.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_categories)
        }

        binding.cloudSyncButton.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_cloudSync)
        }

        binding.backupButton.setOnClickListener {
            showBackupDialog()
        }

        binding.restoreButton.setOnClickListener {
            showRestoreDialog()
        }

        binding.totpButton.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_totp)
        }

        binding.changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun showBackupDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Backup Vault")
            .setMessage("This feature will export your vault data. Make sure to store it securely.")
            .setPositiveButton("Export") { _, _ ->
                viewModel.backupVault()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRestoreDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restore Vault")
            .setMessage("This will replace your current vault with the backup data.")
            .setPositiveButton("Import") { _, _ ->
                // In a real app, you would open a file picker to select the backup file
                // For now, we'll just show a success message
                val dummyBackup = "{\"master_password_hash\":\"dummy\",\"salt\":\"dummy\",\"passwords\":[],\"categories\":[]}"
                viewModel.restoreVault(dummyBackup)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        Toast.makeText(requireContext(), "Change password feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun observeData() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}