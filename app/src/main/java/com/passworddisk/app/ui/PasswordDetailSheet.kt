package com.passworddisk.app.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.passworddisk.app.R
import com.passworddisk.app.databinding.SheetPasswordDetailBinding

class PasswordDetailSheet : BottomSheetDialogFragment() {

    private var _binding: SheetPasswordDetailBinding? = null
    private val binding get() = _binding!!

    private var plainPassword: String = ""
    private var passwordVisible: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetPasswordDetailBinding.inflate(inflater, container, false)
        Ui.applyNavBarInset(binding.root, 8)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val id = args.getString(ARG_ID).orEmpty()
        val title = args.getString(ARG_TITLE).orEmpty()
        val username = args.getString(ARG_USERNAME).orEmpty()
        val url = args.getString(ARG_URL).orEmpty()
        val notes = args.getString(ARG_NOTES).orEmpty()
        val category = args.getString(ARG_CATEGORY).orEmpty()
        plainPassword = args.getString(ARG_PASSWORD).orEmpty()

        val context = requireContext()
        binding.detailTitle.text = title
        binding.detailAvatarText.text = Ui.initialOf(title)
        binding.detailAvatarBox.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, Ui.avatarTintFor(id))
        )
        binding.detailAvatarText.setTextColor(
            Ui.themeColor(context, com.google.android.material.R.attr.colorOnSurface)
        )
        binding.detailCategory.text = category
        binding.detailCategory.visibility = if (category.isBlank()) View.GONE else View.VISIBLE

        binding.usernameValue.text = username.ifBlank { getString(R.string.no_notes) }
        binding.usernameValue.alpha = if (username.isBlank()) 0.6f else 1f
        binding.copyUsernameButton.isEnabled = username.isNotBlank()
        binding.copyUsernameButton.alpha = if (username.isNotBlank()) 1f else 0.4f

        binding.passwordValue.text = Ui.maskOf(plainPassword.length.coerceAtLeast(6))
        binding.togglePasswordButton.setOnClickListener { togglePassword() }
        binding.copyPasswordButton.setOnClickListener { copyPassword() }

        val hasUrl = url.isNotBlank()
        binding.urlRow.visibility = if (hasUrl) View.VISIBLE else View.GONE
        binding.urlDivider.visibility = if (hasUrl) View.VISIBLE else View.GONE
        binding.urlValue.text = url
        binding.copyUrlButton.setOnClickListener { copyToClipboard(getString(R.string.url_hint), url) }
        binding.urlValue.setOnClickListener {
            if (!hasUrl) return@setOnClickListener
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizeUrl(url))))
            }.onFailure {
                toast(getString(R.string.unknown_error))
            }
        }

        val hasNotes = notes.isNotBlank()
        binding.notesRow.visibility = if (hasNotes) View.VISIBLE else View.GONE
        binding.notesDivider.visibility = if (hasNotes) View.VISIBLE else View.GONE
        binding.notesValue.text = notes

        binding.closeButton.setOnClickListener { dismiss() }
        binding.editButton.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(ACTION_KEY to ACTION_EDIT, ID_KEY to id))
            dismiss()
        }
        binding.deleteButton.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(ACTION_KEY to ACTION_DELETE, ID_KEY to id))
            dismiss()
        }
    }

    private fun togglePassword() {
        passwordVisible = !passwordVisible
        if (passwordVisible) {
            binding.passwordValue.text = plainPassword
            binding.passwordValue.setTextColor(
                Ui.themeColor(requireContext(), com.google.android.material.R.attr.colorPrimary)
            )
            binding.togglePasswordButton.setImageResource(R.drawable.ic_visibility_off)
            binding.togglePasswordButton.contentDescription = getString(R.string.hide_password)
        } else {
            binding.passwordValue.text = Ui.maskOf(plainPassword.length.coerceAtLeast(6))
            binding.passwordValue.setTextColor(
                Ui.themeColor(requireContext(), com.google.android.material.R.attr.colorOnSurface)
            )
            binding.togglePasswordButton.setImageResource(R.drawable.ic_visibility)
            binding.togglePasswordButton.contentDescription = getString(R.string.show_password)
        }
    }

    private fun copyPassword() {
        copyToClipboard(getString(R.string.password_hint), plainPassword)
    }

    private fun copyToClipboard(label: String, value: String) {
        if (value.isBlank()) return
        // 剪贴板内容标记为敏感，60 秒后自动清空
        SecureUi.copySensitive(requireContext(), label, value)
        toast(getString(R.string.copied_to_clipboard))
    }

    private fun toast(message: String) {
        if (_binding == null) return
        Notify.success(binding.root, message)
    }

    private fun normalizeUrl(url: String): String =
        if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "password_detail_action"
        const val ACTION_KEY = "action"
        const val ID_KEY = "id"
        const val ACTION_EDIT = "edit"
        const val ACTION_DELETE = "delete"

        private const val ARG_ID = "id"
        private const val ARG_TITLE = "title"
        private const val ARG_USERNAME = "username"
        private const val ARG_PASSWORD = "password"
        private const val ARG_URL = "url"
        private const val ARG_NOTES = "notes"
        private const val ARG_CATEGORY = "category"

        fun newInstance(
            id: String,
            title: String,
            username: String,
            password: String,
            url: String,
            notes: String,
            category: String
        ): PasswordDetailSheet = PasswordDetailSheet().apply {
            arguments = bundleOf(
                ARG_ID to id,
                ARG_TITLE to title,
                ARG_USERNAME to username,
                ARG_PASSWORD to password,
                ARG_URL to url,
                ARG_NOTES to notes,
                ARG_CATEGORY to category
            )
        }
    }
}
