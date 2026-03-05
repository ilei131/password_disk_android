package com.passworddisk.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.passworddisk.app.databinding.FragmentBackupToCloudBinding
import com.passworddisk.app.viewmodel.PasswordViewModel

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

        setupButtons()
        observeData()
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.registerHint.setOnClickListener {
            if (binding.registerHint.text.toString() == "没有账号？") {
                // 切换到注册界面
                binding.backupLayout.visibility = View.GONE
                binding.authButtons.visibility = View.VISIBLE
                binding.registerHint.text = "已有账号？"
            } else {
                // 切换回备份界面
                binding.authButtons.visibility = View.GONE
                binding.backupLayout.visibility = View.VISIBLE
                binding.registerHint.text = "没有账号？"
            }
        }

        binding.registerButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.registerCloud(username, password)
        }

        binding.backupButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 显示加载状态
            binding.backupButton.isEnabled = false
            binding.backupButton.text = "Backing up..."

            // 先获取本地备份数据，然后上传到云端
            viewModel.backupToCloud(username, password)
        }
    }

    private fun observeData() {
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                // 重置按钮状态
                binding.backupButton.isEnabled = true
                binding.backupButton.text = "备份至云端"
                
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
                
                // 注册成功后返回备份界面
                if (it.contains("Registered")) {
                    binding.authButtons.visibility = View.GONE
                    binding.backupLayout.visibility = View.VISIBLE
                    binding.registerHint.text = "没有账号？"
                } 
                // 备份成功后关闭当前页面
                else if (it.contains("Backup")) {
                    // 关闭当前页面，返回上一页
                    findNavController().navigateUp()
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                // 重置按钮状态
                binding.backupButton.isEnabled = true
                binding.backupButton.text = "备份至云端"
                
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