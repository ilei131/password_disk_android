package com.passworddisk.app.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.passworddisk.app.R
import com.passworddisk.app.databinding.FragmentTotpBinding
import com.passworddisk.app.viewmodel.PasswordViewModel

class TotpFragment : Fragment() {
    private var _binding: FragmentTotpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PasswordViewModel by activityViewModels()
    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTotpBinding.inflate(inflater, container, false)
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

        binding.generateButton.setOnClickListener {
            val secret = binding.secretInput.text.toString().trim()
            if (secret.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.please_enter_secret), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.generateTotpCode(secret)
        }

        binding.codeText.setOnClickListener {
            val code = binding.codeText.text.toString()
            if (code != "----" && code.isNotEmpty()) {
                copyToClipboard(code)
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(getString(R.string.totp_generator), text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.code_copied), Toast.LENGTH_SHORT).show()
    }

    private fun observeData() {
        viewModel.totpCode.observe(viewLifecycleOwner) { code ->
            code?.let {
                binding.codeText.text = it
                binding.codeText.visibility = View.VISIBLE
                startTimer()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.timerText.text = getString(R.string.code_expires_in, seconds)
            }

            override fun onFinish() {
                binding.codeText.text = getString(R.string.code_placeholder)
                binding.timerText.text = getString(R.string.code_expired)
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}