package com.passworddisk.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.passworddisk.app.R
import com.passworddisk.app.data.Category
import com.passworddisk.app.data.PasswordItem
import com.passworddisk.app.databinding.FragmentHomeBinding
import com.passworddisk.app.databinding.SheetActionsBinding
import com.passworddisk.app.viewmodel.PasswordViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PasswordViewModel by activityViewModels()

    private lateinit var passwordAdapter: PasswordAdapter
    private var categories: List<Category> = emptyList()
    private var passwords: List<PasswordItem> = emptyList()
    private var currentCategoryId: String = ALL_CATEGORY_ID
    private var query: String = ""
    private var rebuildingChips = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupActions()
        setupDetailResultListener()
        observeData()
    }

    private fun setupRecyclerView() {
        passwordAdapter = PasswordAdapter(
            onItemClick = { openDetail(it) },
            onCopyClick = { copyPassword(it) },
            onMoreClick = { anchor, item -> showItemMenu(anchor, item) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = passwordAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty()
                binding.clearSearch.isVisible = query.isNotEmpty()
                applyFilter()
            }
        })
        binding.clearSearch.setOnClickListener {
            binding.searchInput.setText("")
            binding.searchInput.clearFocus()
            hideKeyboard()
        }
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }
        binding.lockBadge.setOnClickListener { confirmLock() }
    }

    private fun setupActions() {
        binding.fabAdd.setOnClickListener { showQuickActions() }
    }

    private fun setupDetailResultListener() {
        childFragmentManager.setFragmentResultListener(
            PasswordDetailSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val id = bundle.getString(PasswordDetailSheet.ID_KEY).orEmpty()
            when (bundle.getString(PasswordDetailSheet.ACTION_KEY)) {
                PasswordDetailSheet.ACTION_EDIT -> navigateToEdit(id)
                PasswordDetailSheet.ACTION_DELETE -> passwords.find { it.id == id }?.let { confirmDelete(it) }
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allCategories.collect { cats ->
                        categories = cats
                        rebuildChips()
                        passwordAdapter.setCategoryNames(cats.associate { it.id to it.name })
                        applyFilter()
                    }
                }
                launch {
                    viewModel.allPasswords.collect { items ->
                        passwords = items
                        applyFilter()
                    }
                }
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                snackbar(it, Notify.Kind.SUCCESS)
                viewModel.clearMessages()
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                snackbar(it, Notify.Kind.ERROR)
                viewModel.clearMessages()
            }
        }
    }

    private fun rebuildChips() {
        rebuildingChips = true
        binding.chipGroup.removeAllViews()

        val entries = mutableListOf<Pair<String, String>>()
        entries += ALL_CATEGORY_ID to getString(R.string.all)
        categories.filter { it.id != ALL_CATEGORY_ID }.forEach { entries += it.id to it.name }

        if (entries.none { it.first == currentCategoryId }) {
            currentCategoryId = ALL_CATEGORY_ID
        }

        entries.forEach { (id, label) ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = id == currentCategoryId
                tag = id
                setOnClickListener { /* handled by ChipGroup */ }
            }
            binding.chipGroup.addView(chip)
        }

        binding.chipGroup.setOnCheckedStateChangeListener { group, _ ->
            if (rebuildingChips) return@setOnCheckedStateChangeListener
            val checked = group.checkedChipId
            if (checked == View.NO_ID) return@setOnCheckedStateChangeListener
            val id = group.findViewById<Chip>(checked)?.tag as? String ?: return@setOnCheckedStateChangeListener
            if (id == currentCategoryId) return@setOnCheckedStateChangeListener
            currentCategoryId = id
            applyFilter()
        }

        rebuildingChips = false
    }

    private fun applyFilter() {
        val byCategory = if (currentCategoryId == ALL_CATEGORY_ID) {
            passwords
        } else {
            passwords.filter { it.categoryId == currentCategoryId }
        }

        val keyword = query.trim()
        val filtered = if (keyword.isEmpty()) {
            byCategory
        } else {
            byCategory.filter {
                it.title.contains(keyword, ignoreCase = true) ||
                    it.username.contains(keyword, ignoreCase = true) ||
                    it.url.contains(keyword, ignoreCase = true) ||
                    it.notes.contains(keyword, ignoreCase = true)
            }
        }

        passwordAdapter.submitList(filtered)

        // 空库时标题下就不再显示“共 0 条记录”，画面更干净
        binding.countText.isVisible = passwords.isNotEmpty()
        binding.countText.text = if (keyword.isNotEmpty()) {
            getString(R.string.record_count_label, filtered.size)
        } else {
            getString(R.string.count_records, byCategory.size)
        }

        when {
            filtered.isNotEmpty() -> {
                binding.emptyView.isVisible = false
                binding.recyclerView.isVisible = true
            }
            passwords.isEmpty() -> {
                binding.emptyView.isVisible = true
                binding.recyclerView.isVisible = false
                binding.emptyTitle.text = getString(R.string.empty_vault_title)
                binding.emptyDesc.text = getString(R.string.empty_vault_desc)
            }
            else -> {
                binding.emptyView.isVisible = true
                binding.recyclerView.isVisible = false
                binding.emptyTitle.text = getString(R.string.empty_search_title)
                binding.emptyDesc.text = getString(R.string.empty_search_desc)
            }
        }
    }

    private fun showQuickActions() {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = SheetActionsBinding.inflate(layoutInflater)
        dialog.setContentView(sheet.root)
        Ui.applyNavBarInset(sheet.root, 10)

        sheet.actionAddPassword.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_home_to_addPassword)
        }
        sheet.actionAddCategory.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_home_to_categories)
        }
        sheet.actionTotp.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_home_to_totp)
        }

        dialog.show()
    }

    private fun showItemMenu(anchor: View, item: PasswordItem) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, MENU_COPY, 0, getString(R.string.copy_password))
        popup.menu.add(0, MENU_EDIT, 1, getString(R.string.edit))
        popup.menu.add(0, MENU_DELETE, 2, getString(R.string.delete_password))
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                MENU_COPY -> {
                    copyPassword(item)
                    true
                }
                MENU_EDIT -> {
                    navigateToEdit(item.id)
                    true
                }
                MENU_DELETE -> {
                    confirmDelete(item)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun openDetail(item: PasswordItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val plain = viewModel.decryptPassword(item.password)
            if (plain == null) {
                snackbar(getString(R.string.unknown_error))
                return@launch
            }
            val categoryName = categories.firstOrNull { it.id == item.categoryId }?.name.orEmpty()
            PasswordDetailSheet.newInstance(
                id = item.id,
                title = item.title,
                username = item.username,
                password = plain,
                url = item.url,
                notes = item.notes,
                category = categoryName
            ).show(childFragmentManager, "password_detail")
        }
    }

    private fun copyPassword(item: PasswordItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val plain = viewModel.decryptPassword(item.password)
            if (plain == null) {
                snackbar(getString(R.string.unknown_error))
                return@launch
            }
            SecureUi.copySensitive(
                requireContext(),
                getString(R.string.password_hint),
                plain
            )
            snackbar(getString(R.string.copied_to_clipboard))
        }
    }

    private fun navigateToEdit(id: String) {
        findNavController().navigate(
            R.id.action_home_to_editPassword,
            Bundle().apply { putString("passwordId", id) }
        )
    }

    private fun confirmDelete(item: PasswordItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_password_title))
            .setMessage(getString(R.string.delete_password_message, item.title))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete_password)) { _, _ ->
                viewModel.deletePassword(item)
            }
            .show()
    }

    private fun confirmLock() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.lock_vault))
            .setMessage(getString(R.string.logout_confirm))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.lock_vault)) { _, _ ->
                viewModel.logout()
                findNavController().navigate(R.id.action_settings_to_auth)
            }
            .show()
    }

    private fun snackbar(message: String, kind: Notify.Kind = Notify.Kind.INFO) {
        if (_binding == null) return
        Notify.show(binding.root, message, kind)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val ALL_CATEGORY_ID = "1"
        const val MENU_COPY = 100
        const val MENU_EDIT = 101
        const val MENU_DELETE = 102
    }
}
