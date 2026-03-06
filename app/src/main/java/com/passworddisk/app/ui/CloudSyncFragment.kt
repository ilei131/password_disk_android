package com.passworddisk.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.passworddisk.app.R
import com.passworddisk.app.databinding.FragmentCloudSyncBinding
import com.passworddisk.app.viewmodel.PasswordViewModel

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

        setupButtons()
        observeData()
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.registerHint.setOnClickListener {
            if (binding.registerHint.text.toString() == getString(R.string.no_account)) {
                // 显示注册界面
                binding.syncLayout.visibility = View.GONE
                binding.authButtons.visibility = View.VISIBLE
                binding.registerHint.text = getString(R.string.have_account)
            } else {
                // 返回同步界面
                binding.authButtons.visibility = View.GONE
                binding.syncLayout.visibility = View.VISIBLE
                binding.registerHint.text = getString(R.string.no_account)
            }
        }

        binding.registerButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.registerCloud(username, password)
        }

//        binding.loginButton.setOnClickListener {
//            val username = binding.usernameInput.text.toString().trim()
//            val password = binding.passwordInput.text.toString()
//
//            if (username.isEmpty() || password.isEmpty()) {
//                Toast.makeText(requireContext(), getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            viewModel.loginCloud(username, password)
//        }

        binding.syncButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 显示加载状态
            binding.syncButton.isEnabled = false
            binding.syncButton.text = getString(R.string.syncing)
            binding.syncButton.icon = null // 移除图标

            viewModel.syncCloud(username, password)
        }
    }

    private fun observeData() {
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                // 重置按钮状态
                binding.syncButton.isEnabled = true
                binding.syncButton.text = getString(R.string.sync_now)
                binding.syncButton.icon = null // 恢复图标
                
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
                
                // 注册成功后返回同步界面
                if (it.contains(getString(R.string.registered_successfully))) {
                    binding.authButtons.visibility = View.GONE
                    binding.syncLayout.visibility = View.VISIBLE
                } 
                // 同步成功后跳转到登录页面
                else if (it.contains("Synced successfully", ignoreCase = true) || it.contains("同步成功")) {
                    // 跳转到登录页面，让用户重新登录
                    findNavController().navigate(R.id.action_cloudSync_to_auth)
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                // 重置按钮状态
                binding.syncButton.isEnabled = true
                binding.syncButton.text = getString(R.string.sync_now)
                binding.syncButton.icon = null // 恢复图标
                
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