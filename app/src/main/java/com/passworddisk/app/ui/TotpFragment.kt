package com.passworddisk.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.passworddisk.app.R
import com.passworddisk.app.databinding.FragmentTotpBinding
import com.passworddisk.app.viewmodel.PasswordViewModel

class TotpFragment : Fragment() {

    private var _binding: FragmentTotpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PasswordViewModel by activityViewModels()

    private var countDownTimer: CountDownTimer? = null
    private var rawCode: String = ""
    private var currentSecret: String = ""

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents.isNullOrBlank()) return@registerForActivityResult
        applyScanned(contents)
    }

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchScanner()
        } else {
            snackbar(getString(R.string.camera_permission_needed), Notify.Kind.ERROR)
        }
    }

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

        binding.generateButton.setOnClickListener {
            val secret = binding.secretInput.text?.toString()?.trim().orEmpty()
            if (secret.isEmpty()) {
                binding.secretLayout.error = getString(R.string.please_enter_secret)
                return@setOnClickListener
            }
            binding.secretLayout.error = null
            binding.issuerText.isVisible = false
            currentSecret = secret
            viewModel.generateTotpCode(secret)
        }

        // 扫码入口做成输入框右侧的小图标按钮：和「生成验证码」并列时不再抢视觉重心
        binding.secretLayout.setEndIconOnClickListener { startScan() }

        binding.copyButton.setOnClickListener {
            if (rawCode.isEmpty()) return@setOnClickListener
            SecureUi.copySensitive(requireContext(), getString(R.string.totp_generator), rawCode)
            snackbar(getString(R.string.code_copied), Notify.Kind.SUCCESS)
        }

        observeData()
    }

    private fun observeData() {
        viewModel.totpCode.observe(viewLifecycleOwner) { code ->
            if (code == null) return@observe
            rawCode = code
            binding.codeCard.isVisible = true
            binding.codeText.text = formatCode(code)
            startTimer()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                if (_binding == null) return@observe
                binding.secretLayout.error = it
                snackbar(it, Notify.Kind.ERROR)
                viewModel.clearMessages()
            }
        }
    }

    private fun startScan() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) launchScanner() else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchScanner() {
        if (_binding == null) return
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(getString(R.string.scan_qr_prompt))
            setBeepEnabled(false)
            // 竖屏固定；具体界面由自定义的 QrScanActivity 渲染
            setOrientationLocked(true)
            setCaptureActivity(QrScanActivity::class.java)
        }
        scanLauncher.launch(options)
    }

    /** 支持 otpauth:// 链接，也支持直接扫到一串 Base32 密钥 */
    private fun applyScanned(contents: String) {
        val account = parseScanned(contents)
        if (account == null) {
            snackbar(getString(R.string.invalid_qr_code), Notify.Kind.ERROR)
            return
        }
        binding.secretLayout.error = null
        binding.secretInput.setText(account.secret)
        currentSecret = account.secret
        binding.issuerText.isVisible = account.label.isNotBlank()
        if (account.label.isNotBlank()) {
            binding.issuerText.text = getString(R.string.scanned_from, account.label)
        }
        viewModel.generateTotpCode(account.secret)
    }

    private fun parseScanned(raw: String): ScannedAccount? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        if (text.startsWith("otpauth://", ignoreCase = true)) {
            val uri = Uri.parse(text)
            val secret = uri.getQueryParameter("secret")
                ?.let { normalizeSecret(it) }
                ?.takeIf { it.isNotEmpty() }
                ?: return null
            val issuer = uri.getQueryParameter("issuer").orEmpty().trim()
            val path = uri.pathSegments.firstOrNull().orEmpty()
            val account = path.substringAfter(":", "").trim().ifBlank {
                if (path == "totp" || path == "hotp") "" else path
            }
            val label = listOf(issuer, account)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" · ")
            return ScannedAccount(secret, label)
        }

        val secret = normalizeSecret(text)
        if (!secret.matches(Regex("^[A-Z2-7]{8,}=*$"))) return null
        return ScannedAccount(secret, "")
    }

    private fun normalizeSecret(value: String): String =
        value.uppercase().replace(" ", "").replace("-", "").trimEnd('=')

    private fun formatCode(code: String): String =
        if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code

    private fun startTimer() {
        countDownTimer?.cancel()
        binding.progressRing.setProgressCompat(PERIOD_SECONDS, true)

        countDownTimer = object : CountDownTimer(PERIOD_SECONDS * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                binding.progressRing.setProgressCompat(seconds, true)
                binding.timerText.text = getString(R.string.code_expires_in, seconds)
            }

            override fun onFinish() {
                if (_binding == null) return
                // 30 秒窗口结束，自动用同一密钥生成下一个验证码
                if (currentSecret.isNotEmpty()) {
                    viewModel.generateTotpCode(currentSecret)
                } else {
                    binding.progressRing.setProgressCompat(0, true)
                    binding.codeText.text = getString(R.string.code_placeholder)
                    binding.timerText.text = getString(R.string.code_expired)
                    rawCode = ""
                }
            }
        }.start()
    }

    private fun snackbar(message: String, kind: Notify.Kind = Notify.Kind.INFO) {
        if (_binding == null) return
        Notify.show(binding.root, message, kind)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        countDownTimer = null
        _binding = null
    }

    private data class ScannedAccount(val secret: String, val label: String)

    private companion object {
        const val PERIOD_SECONDS = 30
    }
}
