package com.passworddisk.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.passworddisk.app.R
import com.passworddisk.app.cloud.CloudAccountStore
import com.passworddisk.app.databinding.FragmentBackupToCloudBinding
import com.passworddisk.app.viewmodel.PasswordViewModel
import java.text.DateFormat
import java.util.Date

class BackupToCloudFragment : Fragment() {

    private var _binding: FragmentBackupToCloudBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PasswordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupToCloudBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.registerHint.setOnClickListener { toggleMode() }
        binding.registerButton.setOnClickListener { register() }
        binding.backupButton.setOnClickListener { backup() }
        binding.switchAccountButton.setOnClickListener { switchAccount() }

        observeData()
    }

    override fun onResume() {
        super.onResume()
        applySavedAccount()
    }

    /**
     * 本机记住过云端账号时：隐藏用户名输入框，只留密码框，并显示最近一次同步时间。
     */
    private fun applySavedAccount() {
        if (_binding == null) return
        val saved = CloudAccountStore.username(requireContext())
        if (saved == null) {
            binding.accountLabel.isVisible = false
            binding.lastSyncText.isVisible = false
            binding.switchAccountButton.isVisible = false
            binding.usernameLayout.isVisible = true
            binding.registerHint.isVisible = true
            return
        }
        binding.usernameInput.setText(saved)
        binding.usernameLayout.isVisible = false
        binding.accountLabel.isVisible = true
        binding.accountLabel.text = getString(R.string.signed_in_as, saved)
        binding.lastSyncText.isVisible = true
        binding.lastSyncText.text = formatLastSync(CloudAccountStore.lastSyncAt(requireContext()))
        binding.switchAccountButton.isVisible = true
        binding.authButtons.isVisible = false
        binding.backupLayout.isVisible = true
        // 已经记住账号时不再显示「没有账号？」，避免和「切换账号」重复
        binding.registerHint.isVisible = false
    }

    private fun switchAccount() {
        binding.usernameLayout.isVisible = true
        binding.usernameInput.setText("")
        binding.passwordInput.setText("")
        binding.usernameInput.requestFocus()
        binding.accountLabel.isVisible = false
        binding.lastSyncText.isVisible = false
        binding.switchAccountButton.isVisible = false
        binding.registerHint.isVisible = true
    }

    private fun formatLastSync(timestamp: Long): String {
        if (timestamp <= 0L) return getString(R.string.never_synced)
        val formatted = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(timestamp))
        return getString(R.string.last_sync_label, formatted)
    }

    private fun toggleMode() {
        val toRegister = !binding.authButtons.isVisible
        binding.authButtons.isVisible = toRegister
        binding.backupLayout.isVisible = !toRegister
        binding.registerHint.text = getString(
            if (toRegister) R.string.have_account else R.string.no_account
        )
        if (toRegister) {
            binding.usernameLayout.isVisible = true
            binding.accountLabel.isVisible = false
            binding.lastSyncText.isVisible = false
            binding.switchAccountButton.isVisible = false
        } else {
            applySavedAccount()
        }
    }

    private fun validate(): Boolean {
        val username = binding.usernameInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        binding.usernameLayout.error = null
        binding.passwordLayout.error = null
        if (username.isEmpty()) {
            binding.usernameLayout.error = getString(R.string.please_fill_all_fields)
            return false
        }
        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.please_fill_all_fields)
            return false
        }
        return true
    }

    private fun register() {
        if (!validate()) return
        binding.registerButton.isEnabled = false
        viewModel.registerCloud(
            binding.usernameInput.text?.toString()?.trim().orEmpty(),
            binding.passwordInput.text?.toString().orEmpty()
        )
    }

    private fun backup() {
        if (!validate()) return
        binding.backupButton.isEnabled = false
        binding.backupButton.text = getString(R.string.backing_up)
        binding.backupButton.icon = null
        viewModel.backupToCloud(
            binding.usernameInput.text?.toString()?.trim().orEmpty(),
            binding.passwordInput.text?.toString().orEmpty()
        )
    }

    private fun resetButton() {
        binding.backupButton.isEnabled = true
        binding.backupButton.text = getString(R.string.backup_to_cloud)
        binding.backupButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cloud)
        binding.registerButton.isEnabled = true
    }

    private fun observeData() {
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                resetButton()
                snackbar(it, Notify.Kind.SUCCESS)
                viewModel.clearMessages()

                when {
                    it == getString(R.string.registered_successfully) -> applySavedAccount()
                    it == getString(R.string.backup_successful) -> {
                        findNavController().navigateUp()
                    }
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                if (_binding != null) resetButton()
                snackbar(it, Notify.Kind.ERROR)
                viewModel.clearMessages()
            }
        }
    }

    private fun snackbar(message: String, kind: Notify.Kind = Notify.Kind.INFO) {
        if (_binding != null) {
            Notify.show(binding.root, message, kind)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
