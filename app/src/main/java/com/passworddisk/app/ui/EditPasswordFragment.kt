package com.passworddisk.app.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.passworddisk.app.R
import com.passworddisk.app.data.Category
import com.passworddisk.app.data.PasswordItem
import com.passworddisk.app.databinding.FragmentAddPasswordBinding
import com.passworddisk.app.viewmodel.PasswordViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EditPasswordFragment : Fragment() {

    private var _binding: FragmentAddPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PasswordViewModel by activityViewModels()

    private var categories: List<Category> = emptyList()
    private var currentPassword: PasswordItem? = null
    private var selectedCategoryId: String? = null
    private var loaded = false

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

        binding.titleText.text = getString(R.string.edit_password)
        binding.saveButton.text = getString(R.string.update)

        setupButtons()
        setupGeneratorResult()
        loadPassword()
        observeCategories()
        observeMessages()

        // 键盘弹出时把正在编辑的字段滚回可视区域
        Ui.keepFocusedFieldVisible(binding.contentScroll, binding.formContainer)
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener { close() }
        binding.cancelButton.setOnClickListener { close() }
        binding.generatePasswordButton.setOnClickListener { showGenerator() }
        binding.saveButton.setOnClickListener { update() }
    }

    private fun setupGeneratorResult() {
        childFragmentManager.setFragmentResultListener(
            GeneratorSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            bundle.getString(GeneratorSheet.RESULT_PASSWORD)?.let { generated ->
                binding.passwordInput.setText(generated)
                binding.passwordLayout.error = null
            }
        }
    }

    private fun loadPassword() {
        val passwordId = arguments?.getString("passwordId") ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val item = viewModel.allPasswords.first().firstOrNull { it.id == passwordId } ?: return@launch
            if (_binding == null || loaded) return@launch
            loaded = true
            currentPassword = item
            selectedCategoryId = item.categoryId

            binding.titleInput.setText(item.title)
            binding.usernameInput.setText(item.username)
            binding.urlInput.setText(item.url)
            binding.notesInput.setText(item.notes)
            binding.passwordInput.setText(viewModel.decryptPassword(item.password).orEmpty())

            applyCategorySelection()
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCategories.collect { cats ->
                    categories = cats.filter { it.id != ALL_CATEGORY_ID }
                    if (categories.isEmpty()) {
                        binding.categoryLayout.isEnabled = false
                        binding.categoryDropdown.setText(getString(R.string.no_categories), false)
                        return@collect
                    }
                    binding.categoryLayout.isEnabled = true
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        categories.map { it.name }
                    )
                    binding.categoryDropdown.setAdapter(adapter)
                    binding.categoryDropdown.setOnItemClickListener { _, _, position, _ ->
                        selectedCategoryId = categories.getOrNull(position)?.id
                    }
                    applyCategorySelection()
                }
            }
        }
    }

    private fun applyCategorySelection() {
        if (_binding == null || categories.isEmpty()) return
        val index = categories.indexOfFirst { it.id == selectedCategoryId }
        val resolved = if (index >= 0) index else 0
        selectedCategoryId = categories[resolved].id
        binding.categoryDropdown.setText(categories[resolved].name, false)
    }

    private fun observeMessages() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                if (_binding != null) {
                    Notify.error(binding.root, it)
                }
                viewModel.clearMessages()
            }
        }
    }

    private fun showGenerator() {
        hideKeyboard()
        GeneratorSheet().show(childFragmentManager, "generator")
    }

    private fun update() {
        val item = currentPassword ?: return

        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val username = binding.usernameInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val url = binding.urlInput.text?.toString()?.trim().orEmpty()
        val notes = binding.notesInput.text?.toString()?.trim().orEmpty()

        var valid = true
        if (title.isEmpty()) {
            binding.titleLayout.error = getString(R.string.title_required)
            valid = false
        } else {
            binding.titleLayout.error = null
        }
        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.password_required_field)
            valid = false
        } else {
            binding.passwordLayout.error = null
        }
        if (!valid) return

        val updated = item.copy(
            title = title,
            username = username,
            url = url,
            notes = notes,
            categoryId = selectedCategoryId ?: item.categoryId
        )
        hideKeyboard()
        viewModel.updatePassword(updated, password)
        findNavController().navigateUp()
    }

    private fun close() {
        hideKeyboard()
        findNavController().navigateUp()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val ALL_CATEGORY_ID = "1"
    }
}
