package com.passworddisk.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.passworddisk.app.data.PasswordItem
import com.passworddisk.app.databinding.ItemPasswordBinding

class PasswordAdapter(
    private val onItemClick: (PasswordItem) -> Unit,
    private val onCopyClick: (PasswordItem) -> Unit,
    private val onEditClick: (PasswordItem) -> Unit,
    private val onDeleteClick: (PasswordItem) -> Unit
) : ListAdapter<PasswordItem, PasswordAdapter.PasswordViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
        val binding = ItemPasswordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PasswordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PasswordViewHolder(
        private val binding: ItemPasswordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(password: PasswordItem) {
            binding.apply {
                titleText.text = password.title
                usernameText.text = password.username
                urlText.text = password.url

                root.setOnClickListener { onItemClick(password) }
                copyButton.setOnClickListener { onCopyClick(password) }
                editButton.setOnClickListener { onEditClick(password) }
                deleteButton.setOnClickListener { onDeleteClick(password) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PasswordItem>() {
        override fun areItemsTheSame(oldItem: PasswordItem, newItem: PasswordItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PasswordItem, newItem: PasswordItem): Boolean {
            return oldItem == newItem
        }
    }
}