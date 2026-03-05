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
        
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_add_password -> {
                    findNavController().navigate(R.id.action_home_to_addPassword)
                    true
                }
                R.id.action_add_category -> {
                    findNavController().navigate(R.id.action_home_to_settings)
                    // In a real app, we would navigate directly to the add category screen
                    true
                }
                R.id.action_totp_generator -> {
                    findNavController().navigate(R.id.action_home_to_settings)
                    // In a real app, we would navigate directly to the TOTP fragment
                    true
                }
                R.id.action_backup_vault -> {
                    findNavController().navigate(R.id.action_home_to_settings)
                    // In a real app, we would show the backup dialog directly
                    true
                }
                else -> false
            }
        }
        
        // Show the menu
        popupMenu.show()
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
            val decryptedPassword = viewModel.decryptPassword(password.encryptedPassword)
            decryptedPassword?.let {
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(password.title)
                    .setMessage("""
                        Username: ${password.username}
                        Password: $it
                        URL: ${password.url}
                        Notes: ${password.notes}
                    """.trimIndent())
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Copy Password") { _, _ ->
                        copyToClipboard(it)
                    }
                    .create()
                dialog.show()
            }
        }
    }

    private fun copyPassword(password: PasswordItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val decryptedPassword = viewModel.decryptPassword(password.encryptedPassword)
            decryptedPassword?.let {
                copyToClipboard(it)
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("password", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun editPassword(password: PasswordItem) {
        val bundle = Bundle().apply {
            putString("passwordId", password.id)
        }
        findNavController().navigate(R.id.action_home_to_editPassword, bundle)
    }

    private fun confirmDeletePassword(password: PasswordItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Password")
            .setMessage("Are you sure you want to delete \"${password.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePassword(password)
            }
            .setNegativeButton("Cancel", null)
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