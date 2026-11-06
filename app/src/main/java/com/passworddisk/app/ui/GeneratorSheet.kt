package com.passworddisk.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.passworddisk.app.R
import com.passworddisk.app.crypto.CryptoManager
import com.passworddisk.app.databinding.SheetGeneratorBinding
import kotlin.math.ln
import kotlin.math.roundToInt

class GeneratorSheet : BottomSheetDialogFragment() {

    private var _binding: SheetGeneratorBinding? = null
    private val binding get() = _binding!!

    private var currentPassword: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetGeneratorBinding.inflate(inflater, container, false)
        Ui.applyNavBarInset(binding.root, 8)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lengthSlider.addOnChangeListener { _, value, _ ->
            binding.lengthValue.text = value.roundToInt().toString()
            regenerate()
        }
        listOf(binding.uppercaseCheck, binding.lowercaseCheck, binding.numbersCheck, binding.symbolsCheck)
            .forEach { it.setOnCheckedChangeListener { _, _ -> regenerate() } }

        binding.refreshButton.setOnClickListener { regenerate() }
        binding.copyGeneratedButton.setOnClickListener {
            if (currentPassword.isBlank()) return@setOnClickListener
            SecureUi.copySensitive(
                requireContext(),
                getString(R.string.password_hint),
                currentPassword
            )
            Notify.success(binding.root, getString(R.string.copied_to_clipboard))
        }
        binding.generateButton.setOnClickListener {
            if (currentPassword.isBlank()) regenerate()
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_PASSWORD to currentPassword))
            dismiss()
        }

        regenerate()
    }

    private fun regenerate() {
        val length = binding.lengthSlider.value.roundToInt()
        val upper = binding.uppercaseCheck.isChecked
        val lower = binding.lowercaseCheck.isChecked
        val numbers = binding.numbersCheck.isChecked
        val symbols = binding.symbolsCheck.isChecked

        if (!upper && !lower && !numbers && !symbols) {
            binding.generatedPasswordText.text = getString(R.string.select_at_least_one_type)
            currentPassword = ""
            renderStrength(0.0)
            return
        }

        currentPassword = runCatching {
            CryptoManager.generatePassword(length, upper, lower, numbers, symbols)
        }.getOrElse { "" }

        binding.generatedPasswordText.text = currentPassword
        val charsetSize = (if (upper) 26 else 0) + (if (lower) 26 else 0) +
            (if (numbers) 10 else 0) + (if (symbols) 25 else 0)
        val entropy = if (charsetSize <= 1) 0.0 else length * ln(charsetSize.toDouble()) / ln(2.0)
        renderStrength(entropy)
    }

    private fun renderStrength(entropy: Double) {
        val (level, labelRes, colorRes) = when {
            entropy <= 0.0 -> Triple(0, R.string.strength_unknown, R.color.color_outline)
            entropy < 40 -> Triple(1, R.string.strength_weak, R.color.strength_weak)
            entropy < 60 -> Triple(2, R.string.strength_fair, R.color.strength_fair)
            entropy < 80 -> Triple(3, R.string.strength_good, R.color.strength_good)
            else -> Triple(4, R.string.strength_strong, R.color.strength_strong)
        }

        binding.strengthLabel.text = getString(labelRes)
        val segments = listOf(
            binding.strengthSeg1,
            binding.strengthSeg2,
            binding.strengthSeg3,
            binding.strengthSeg4
        )
        val activeColor = ContextCompat.getColor(requireContext(), colorRes)
        val idleColor = Ui.themeColor(requireContext(), com.google.android.material.R.attr.colorSurfaceVariant)
        segments.forEachIndexed { index, segment ->
            segment.setBackgroundColor(if (index < level) activeColor else idleColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "generator_result"
        const val RESULT_PASSWORD = "password"
    }
}
