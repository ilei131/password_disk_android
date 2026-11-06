package com.passworddisk.app.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import com.passworddisk.app.R
import com.passworddisk.app.data.Category
import com.passworddisk.app.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onItemClick: (Category) -> Unit,
    private val onMoreClick: (View, Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(DiffCallback()) {

    private var counts: Map<String, Int> = emptyMap()

    fun setCounts(value: Map<String, Int>) {
        if (value == counts) return
        counts = value
        notifyItemRangeChanged(0, itemCount, PAYLOAD_COUNT)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), counts)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.bindCount(getItem(position), counts)
        }
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category, counts: Map<String, Int>) {
            val context = binding.root.context
            binding.nameText.text = category.name
            binding.iconImage.setImageResource(CategoryIcons.drawable(category.icon))
            binding.iconImage.imageTintList = ColorStateList.valueOf(
                Ui.themeColor(context, MaterialR.attr.colorPrimary)
            )
            binding.root.setOnClickListener { onItemClick(category) }
            binding.moreButton.setOnClickListener { onMoreClick(it, category) }
            bindCount(category, counts)
        }

        fun bindCount(category: Category, counts: Map<String, Int>) {
            val count = counts[category.id] ?: 0
            binding.countText.text = binding.root.context.getString(R.string.record_count_label, count)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean =
            oldItem == newItem
    }

    private companion object {
        const val PAYLOAD_COUNT = "count"
    }
}
