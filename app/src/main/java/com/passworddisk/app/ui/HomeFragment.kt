package com.passworddisk.app.ui

import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.passworddisk.app.R
import com.passworddisk.app.data.Category
import com.passworddisk.app.data.PasswordItem
import com.passworddisk.app.databinding.FragmentHomeBinding
import com.passworddisk.app.viewmodel.PasswordViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PasswordViewModel by activityViewModels()
    private lateinit var passwordAdapter: PasswordAdapter
    private var categories: List<Category> = emptyList()
    private var passwords: List<PasswordItem> = emptyList()
    private var currentCategoryId: String = "1"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabLayout()
        setupFab()
        observeData()
    }

    private fun setupRecyclerView() {
        passwordAdapter = PasswordAdapter(
            onItemClick = { password ->
                showPasswordDetails(password)
            },
            onCopyClick = { password ->
                copyPassword(password)
            },
            onEditClick = { password ->
                editPassword(password)
            },
            onDeleteClick = { password ->
                confirmDeletePassword(password)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = passwordAdapter
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val categoryId = it.tag as? String ?: "1"
                    currentCategoryId = categoryId
                    filterPasswords()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showFabMenu()
        }
    }

    private fun showFabMenu() {
        val popupMenu = PopupMenu(requireContext(), binding.fabAdd)
        popupMenu.inflate(R.menu.menu_fab)
        
        // Force show icons
        try {
            val field = PopupMenu::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val menuPopupHelper = field.get(popupMenu)
            val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
            val setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
            setForceShowIcon.invoke(menuPopupHelper, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_add_password -> {
                    findNavController().navigate(R.id.action_home_to_addPassword)
                    true
                }
                R.id.action_add_category -> {
                    showAddCategoryDialog()
                    true
                }
                R.id.action_totp_generator -> {
                    findNavController().navigate(R.id.action_home_to_totp)
                    true
                }
                else -> false
            }
        }
        
        // Show the menu
        popupMenu.show()
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_category, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.categoryNameInput)
        val iconButton = dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.categoryIconButton)
        
        var selectedIcon = "📁"
        
        iconButton.setOnClickListener {
            showIconSelector {icon ->
                selectedIcon = icon
                iconButton.text = icon
            }
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_category_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add_category)) { _, _ ->
                val name = nameInput.text.toString().trim()

                if (name.isNotEmpty()) {
                    viewModel.addCategory(name, selectedIcon)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showIconSelector(onIconSelected: (String) -> Unit) {
        val icons = listOf(
            "📁", "👤", "💼", "💰", "📱", "🏠", "🔒", "📧",
            "🌐", "🎮", "📚", "🎨", "🔧", "🚗", "✈️", "⚡"
        )
        
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, icons)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_icon))
            .setAdapter(adapter) { dialog, position ->
                onIconSelected(icons[position])
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allCategories.collect { cats ->
                        categories = cats
                        updateTabs()
                    }
                }
                launch {
                    viewModel.allPasswords.collect { pwds ->
                        passwords = pwds
                        filterPasswords()
                    }
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

    private fun updateTabs() {
        binding.tabLayout.removeAllTabs()
        categories.forEach { category ->
            val tab = binding.tabLayout.newTab().apply {
                text = "${category.icon} ${category.name}"
                tag = category.id
            }
            binding.tabLayout.addTab(tab)
        }
    }

    private fun filterPasswords() {
        passwords.forEachIndexed { index, password ->
            println("Password $index: id=${password.id}, title=${password.title}, username=${password.username}, password=${password.password?.take(10)}..., category=${password.categoryId}")
        }
        val filtered = if (currentCategoryId == "1") {
            passwords
        } else {
            passwords.filter { it.categoryId == currentCategoryId }
        }
        passwordAdapter.submitList(filtered)
        binding.emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showPasswordDetails(password: PasswordItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val decryptedPassword = viewModel.decryptPassword(password.password)
            decryptedPassword?.let {
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(password.title)
                    .setMessage("""
                        ${getString(R.string.username_label, password.username)}
                        ${getString(R.string.password_label, it)}
                        ${getString(R.string.url_label, password.url)}
                        ${getString(R.string.notes_label, password.notes)}
                    """.trimIndent())
                    .setPositiveButton(getString(R.string.ok), null)
                    .setNeutralButton(getString(R.string.copy_password_button)) { _, _ ->
                        copyToClipboard(it)
                    }
                    .create()
                dialog.show()
            }
        }
    }

    private fun copyPassword(password: PasswordItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val decryptedPassword = viewModel.decryptPassword(password.password)
            decryptedPassword?.let {
                copyToClipboard(it)
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(getString(R.string.password_hint), text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    private fun editPassword(password: PasswordItem) {
        val bundle = Bundle().apply {
            putString("passwordId", password.id)
        }
        findNavController().navigate(R.id.action_home_to_editPassword, bundle)
    }

    private fun confirmDeletePassword(password: PasswordItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_password_title))
            .setMessage(getString(R.string.delete_password_message, password.title))
            .setPositiveButton(getString(R.string.delete_password)) { _, _ ->
                viewModel.deletePassword(password)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                // Implement search functionality
                return true
            }
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(R.id.action_home_to_settings)
                true
            }
            R.id.action_logout -> {
                viewModel.logout()
                findNavController().navigate(R.id.action_home_to_auth)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}