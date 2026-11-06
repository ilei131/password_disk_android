package com.passworddisk.app.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.passworddisk.app.R
import com.passworddisk.app.data.PasswordItem
import com.passworddisk.app.databinding.ItemPasswordBinding

class PasswordAdapter(
    private val onItemClick: (PasswordItem) -> Unit,
    private val onCopyClick: (PasswordItem) -> Unit,
    private val onMoreClick: (View, PasswordItem) -> Unit
) : ListAdapter<PasswordItem, PasswordAdapter.PasswordViewHolder>(DiffCallback()) {

    private var categoryNames: Map<String, String> = emptyMap()

    fun setCategoryNames(names: Map<String, String>) {
        if (names == categoryNames) return
        categoryNames = names
        notifyItemRangeChanged(0, itemCount, PAYLOAD_CATEGORY)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
        val binding = ItemPasswordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PasswordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
        holder.bind(getItem(position), categoryNames)
    }

    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.bindCategory(getItem(position), categoryNames)
        }
    }

    inner class PasswordViewHolder(
        private val binding: ItemPasswordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(password: PasswordItem, names: Map<String, String>) {
            val context = binding.root.context

            binding.titleText.text = password.title
            val secondary = password.username.ifBlank { password.url }
            binding.usernameText.text = secondary
            binding.usernameText.visibility = if (secondary.isBlank()) View.GONE else View.VISIBLE
            binding.avatarText.text = Ui.initialOf(password.title)
            binding.avatarBox.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, Ui.avatarTintFor(password.id))
            )
            binding.avatarText.setTextColor(
                Ui.themeColor(context, com.google.android.material.R.attr.colorOnSurface)
            )

            bindCategory(password, names)

            binding.root.contentDescription = password.title
            binding.root.setOnClickListener { onItemClick(password) }
            binding.copyButton.setOnClickListener { onCopyClick(password) }
            binding.moreButton.setOnClickListener { onMoreClick(it, password) }
        }

        fun bindCategory(password: PasswordItem, names: Map<String, String>) {
            val name = names[password.categoryId]
            if (name.isNullOrBlank()) {
                binding.categoryText.visibility = View.GONE
            } else {
                binding.categoryText.visibility = View.VISIBLE
                binding.categoryText.text = name
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PasswordItem>() {
        override fun areItemsTheSame(oldItem: PasswordItem, newItem: PasswordItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PasswordItem, newItem: PasswordItem): Boolean =
            oldItem == newItem
    }

    private companion object {
        const val PAYLOAD_CATEGORY = "category"
    }
}
