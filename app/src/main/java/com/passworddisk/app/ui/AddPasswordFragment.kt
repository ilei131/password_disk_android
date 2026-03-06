package com.passworddisk.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.passworddisk.app.R
import com.passworddisk.app.data.Category
import com.passworddisk.app.databinding.FragmentAddPasswordBinding
import com.passworddisk.app.viewmodel.PasswordViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AddPasswordFragment : Fragment() {
    private var _binding: FragmentAddPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PasswordViewModel by activityViewModels()
    private var categories: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategorySpinner()
        setupButtons()
        observeData()
    }

    private fun setupCategorySpinner() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCategories.collect { cats ->
                    categories = cats.filter { it.id != "1" }
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        categories.map { it.name }
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.categorySpinner.adapter = adapter
                }
            }
        }
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.generatePasswordButton.setOnClickListener {
            showPasswordGeneratorDialog()
        }

        binding.saveButton.setOnClickListener {
            savePassword()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showPasswordGeneratorDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password_generator, null)
        val lengthInput = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.lengthSlider)
        val lengthValue = dialogView.findViewById<android.widget.TextView>(R.id.lengthValue)
        val uppercaseCheck = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.uppercaseCheck)
        val lowercaseCheck = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.lowercaseCheck)
        val numbersCheck = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.numbersCheck)
        val symbolsCheck = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.symbolsCheck)
        val resultText = dialogView.findViewById<android.widget.TextView>(R.id.generatedPasswordText)

        // 监听滑块变化，更新长度值
        lengthInput.addOnChangeListener { _, value, _ ->
            lengthValue.text = value.toInt().toString()
        }

        uppercaseCheck.isChecked = true
        lowercaseCheck.isChecked = true
        numbersCheck.isChecked = true
        symbolsCheck.isChecked = true

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.generate_password_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.use)) { _, _ ->
                binding.passwordInput.setText(resultText.text)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialogView.findViewById<android.widget.Button>(R.id.generateButton).setOnClickListener {
            viewModel.generatePassword(
                lengthInput.value.toInt(),
                uppercaseCheck.isChecked,
                lowercaseCheck.isChecked,
                numbersCheck.isChecked,
                symbolsCheck.isChecked
            )
        }

        viewModel.generatedPassword.observe(viewLifecycleOwner) { password ->
            password?.let {
                resultText.text = it
            }
        }

        dialog.show()
    }

    private fun savePassword() {
        val title = binding.titleInput.text.toString().trim()
        val username = binding.usernameInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val url = binding.urlInput.text.toString().trim()
        val notes = binding.notesInput.text.toString().trim()

        if (title.isEmpty()) {
            binding.titleLayout.error = getString(R.string.title_required)
            return
        }
        binding.titleLayout.error = null

        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.password_required_field)
            return
        }
        binding.passwordLayout.error = null

        val selectedPosition = binding.categorySpinner.selectedItemPosition
        val categoryId = if (selectedPosition >= 0 && selectedPosition < categories.size) {
            categories[selectedPosition].id
        } else {
            "2"
        }

        viewModel.addPassword(title, username, password, url, notes, categoryId)
        Toast.makeText(requireContext(), getString(R.string.password_saved), Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun observeData() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
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