package com.passworddisk.app.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.R as MaterialR
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.passworddisk.app.R
import com.passworddisk.app.data.Category
import com.passworddisk.app.data.PasswordItem
import com.passworddisk.app.databinding.DialogCategoryBinding
import com.passworddisk.app.databinding.FragmentCategoriesBinding
import com.passworddisk.app.viewmodel.PasswordViewModel
import kotlinx.coroutines.launch

class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PasswordViewModel by activityViewModels()

    private lateinit var categoryAdapter: CategoryAdapter

    private var categories: List<Category> = emptyList()
    private var passwords: List<PasswordItem> = emptyList()

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

        categoryAdapter = CategoryAdapter(
            onItemClick = { showCategoryDialog(it) },
            onMoreClick = { anchor, category -> showCategoryMenu(anchor, category) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }

        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.fabAdd.setOnClickListener { showCategoryDialog(null) }

        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allCategories.collect { cats ->
                        categories = cats.filter { it.id != ALL_CATEGORY_ID }
                        categoryAdapter.submitList(categories)
                        renderEmpty()
                    }
                }
                launch {
                    viewModel.allPasswords.collect { items ->
                        passwords = items
                        categoryAdapter.setCounts(items.groupingBy { it.categoryId }.eachCount())
                        renderEmpty()
                    }
                }
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                if (_binding != null) Notify.success(binding.root, it)
                viewModel.clearMessages()
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                if (_binding != null) Notify.error(binding.root, it)
                viewModel.clearMessages()
            }
        }
    }

    private fun renderEmpty() {
        if (_binding == null) return
        val empty = categories.isEmpty()
        binding.emptyView.isVisible = empty
        binding.recyclerView.isVisible = !empty
    }

    private fun showCategoryMenu(anchor: View, category: Category) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, MENU_EDIT, 0, getString(R.string.edit))
        popup.menu.add(0, MENU_DELETE, 1, getString(R.string.delete_category))
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                MENU_EDIT -> {
                    showCategoryDialog(category)
                    true
                }
                MENU_DELETE -> {
                    confirmDelete(category)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showCategoryDialog(existing: Category?) {
        val dialogBinding = DialogCategoryBinding.inflate(layoutInflater)
        var selectedIcon = CategoryIcons.keyOf(existing?.icon)

        dialogBinding.categoryNameInput.setText(existing?.name.orEmpty())
        buildIconPicker(dialogBinding.iconContainer, selectedIcon) { selectedIcon = it }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                if (existing == null) R.string.add_category_title else R.string.edit_category_title
            )
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(
                if (existing == null) R.string.add_category else R.string.save,
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.categoryNameInput.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    dialogBinding.categoryNameLayout.error = getString(R.string.category_name_required)
                    return@setOnClickListener
                }
                if (existing == null) {
                    viewModel.addCategory(name, selectedIcon)
                } else {
                    viewModel.updateCategory(existing.copy(name = name, icon = selectedIcon))
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun buildIconPicker(
        container: GridLayout,
        initial: String,
        onChanged: (String) -> Unit
    ) {
        container.removeAllViews()
        val context = requireContext()
        val size = dp(42)
        val margin = dp(5)
        var current = CategoryIcons.keyOf(initial)
        val tiles = mutableListOf<Pair<ImageView, String>>()

        val onPrimaryContainer = Ui.themeColor(context, MaterialR.attr.colorOnPrimaryContainer)
        val surfaceVariant = Ui.themeColor(context, MaterialR.attr.colorSurfaceVariant)
        val onSurfaceVariant = Ui.themeColor(context, MaterialR.attr.colorOnSurfaceVariant)

        fun refresh() {
            tiles.forEach { (view, key) ->
                val selected = key == current
                view.backgroundTintList = ColorStateList.valueOf(
                    if (selected) Ui.themeColor(context, MaterialR.attr.colorPrimaryContainer) else surfaceVariant
                )
                view.imageTintList = ColorStateList.valueOf(
                    if (selected) onPrimaryContainer else onSurfaceVariant
                )
                view.alpha = if (selected) 1f else 0.9f
            }
        }

        CategoryIcons.keys.forEach { key ->
            val view = ImageView(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                }
                setBackgroundResource(R.drawable.bg_icon_tile)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = key
                setImageResource(CategoryIcons.drawable(key))
                setOnClickListener {
                    current = key
                    refresh()
                    onChanged(key)
                }
            }
            container.addView(view)
            tiles.add(view to key)
        }
        refresh()
    }

    private fun confirmDelete(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_category_title))
            .setMessage(getString(R.string.category_delete_confirm, category.name))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete_category)) { _, _ ->
                viewModel.deleteCategory(category)
            }
            .show()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val ALL_CATEGORY_ID = "1"
        const val MENU_EDIT = 200
        const val MENU_DELETE = 201
    }
}
