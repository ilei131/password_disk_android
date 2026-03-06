package com.passworddisk.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.passworddisk.app.data.Category
import com.passworddisk.app.databinding.FragmentCategoriesBinding
import com.passworddisk.app.viewmodel.PasswordViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PasswordViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        observeData()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onEditClick = { category ->
                showEditCategoryDialog(category)
            },
            onDeleteClick = { category ->
                confirmDeleteCategory(category)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddCategoryDialog()
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCategories.collect { categories ->
                    categoryAdapter.submitList(categories.filter { it.id != "1" })
                    binding.emptyView.visibility = if (categories.size <= 1) View.VISIBLE else View.GONE
                }
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(com.passworddisk.app.R.layout.dialog_category, null)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.passworddisk.app.R.id.categoryNameInput)
        val iconButton = dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(com.passworddisk.app.R.id.categoryIconButton)

        var selectedIcon = "📁"

        iconButton.setOnClickListener {
            showIconSelector { icon ->
                selectedIcon = icon
                iconButton.text = icon
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addCategory(name, selectedIcon)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val dialogView = layoutInflater.inflate(com.passworddisk.app.R.layout.dialog_category, null)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.passworddisk.app.R.id.categoryNameInput)
        val iconButton = dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(com.passworddisk.app.R.id.categoryIconButton)

        var selectedIcon = category.icon
        nameInput.setText(category.name)
        iconButton.text = category.icon

        iconButton.setOnClickListener {
            showIconSelector { icon ->
                selectedIcon = icon
                iconButton.text = icon
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Category")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.updateCategory(category.copy(name = name, icon = selectedIcon))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showIconSelector(onIconSelected: (String) -> Unit) {
        val icons = listOf(
            "📁", "👤", "💼", "💰", "📱", "🏠", "🔒", "📧",
            "🌐", "🎮", "📚", "🎨", "🔧", "🚗", "✈️", "⚡"
        )

        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, icons)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Icon")
            .setAdapter(adapter) { dialog, position ->
                onIconSelected(icons[position])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteCategory(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete \"${category.name}\"? All passwords in this category will also be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}