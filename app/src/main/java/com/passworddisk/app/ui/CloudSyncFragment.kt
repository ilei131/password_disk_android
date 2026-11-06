package com.passworddisk.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.passworddisk.app.R
import com.passworddisk.app.cloud.CloudAccountStore
import com.passworddisk.app.databinding.FragmentCloudSyncBinding
import com.passworddisk.app.viewmodel.PasswordViewModel
import java.text.DateFormat
import java.util.Date

class CloudSyncFragment : Fragment() {

    private var _binding: FragmentCloudSyncBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PasswordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCloudSyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.registerHint.setOnClickListener { toggleMode() }
        binding.registerButton.setOnClickListener { register() }
        binding.syncButton.setOnClickListener { sync() }
        binding.switchAccountButton.setOnClickListener { switchAccount() }

        observeData()
    }

    override fun onResume() {
        super.onResume()
        applySavedAccount()
    }

    /**
     * 本机记住过云端账号时：隐藏用户名输入框，只留密码框，并显示最近一次同步时间。
     * 没记住过就保持原来的两个输入框。
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
        binding.syncLayout.isVisible = true
        // 已经记住账号时，「切换账号」和「没有账号？」语义冲突：后者只服务于新用户
        binding.registerHint.isVisible = false
    }

    /** 换一个账号：把用户名输入框放出来并清空，密码也一并清掉 */
    private fun switchAccount() {
        binding.usernameLayout.isVisible = true
        binding.usernameInput.setText("")
        binding.passwordInput.setText("")
        binding.usernameInput.requestFocus()
        binding.accountLabel.isVisible = false
        binding.lastSyncText.isVisible = false
        binding.switchAccountButton.isVisible = false
        // 手动切换账号后可能就是要在别处新注册，此时把入口放回来
        binding.registerHint.isVisible = true
    }

    private fun formatLastSync(timestamp: Long): String {
        if (timestamp <= 0L) return getString(R.string.never_synced)
        val formatted = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(timestamp))
        return getString(R.string.last_sync_label, formatted)
    }

    private fun isRegisterMode(): Boolean = binding.authButtons.isVisible

    private fun toggleMode() {
        val toRegister = !isRegisterMode()
        binding.authButtons.isVisible = toRegister
        binding.syncLayout.isVisible = !toRegister
        binding.registerHint.text = getString(
            if (toRegister) R.string.have_account else R.string.no_account
        )
        binding.registerHint.isVisible = true
        if (toRegister) {
            // 注册新账号时用户名必须能编辑，别被「已记住的账号」挡住
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
        setLoading(binding.registerButton, true, getString(R.string.register))
        viewModel.registerCloud(
            binding.usernameInput.text?.toString()?.trim().orEmpty(),
            binding.passwordInput.text?.toString().orEmpty()
        )
    }

    private fun sync() {
        if (!validate()) return
        setLoading(binding.syncButton, true, getString(R.string.syncing))
        viewModel.syncCloud(
            binding.usernameInput.text?.toString()?.trim().orEmpty(),
            binding.passwordInput.text?.toString().orEmpty()
        )
    }

    private fun setLoading(view: View, loading: Boolean, idleText: String) {
        (view as? com.google.android.material.button.MaterialButton)?.apply {
            isEnabled = !loading
            text = idleText
        }
        if (view === binding.syncButton) {
            binding.syncButton.icon = if (loading) null else androidx.core.content.ContextCompat
                .getDrawable(requireContext(), R.drawable.ic_refresh)
        }
    }

    private fun observeData() {
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                setLoading(binding.syncButton, false, getString(R.string.sync_now))
                setLoading(binding.registerButton, false, getString(R.string.register))
                snackbar(it, Notify.Kind.SUCCESS)
                viewModel.clearMessages()

                when (it) {
                    getString(R.string.registered_successfully) -> applySavedAccount()
                    getString(R.string.please_login_again),
                    getString(R.string.synced_successfully) ->
                        findNavController().navigate(R.id.action_cloudSync_to_auth)
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                setLoading(binding.syncButton, false, getString(R.string.sync_now))
                setLoading(binding.registerButton, false, getString(R.string.register))
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
