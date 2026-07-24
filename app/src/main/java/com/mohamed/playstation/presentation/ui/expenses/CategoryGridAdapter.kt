package com.mohamed.playstation.presentation.ui.expenses

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.R
import com.mohamed.playstation.databinding.ItemExpenseCategoryBinding
import com.mohamed.playstation.domain.model.ExpenseCategory

class CategoryGridAdapter(
    private val onCategorySelected: (ExpenseCategory) -> Unit
) : ListAdapter<ExpenseCategory, CategoryGridAdapter.ViewHolder>(DiffCallback()) {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    inner class ViewHolder(private val binding: ItemExpenseCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: ExpenseCategory, isSelected: Boolean) {
            binding.tvCategoryName.text = getCategoryName(category)
            binding.ivCategory.setImageResource(getCategoryIcon(category))
            
            binding.container.isSelected = isSelected

            binding.root.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onCategorySelected(category)
            }
        }

        private fun getCategoryName(category: ExpenseCategory): String {
            return when (category) {
                ExpenseCategory.ELECTRICITY -> binding.root.context.getString(R.string.category_electricity)
                ExpenseCategory.INTERNET -> binding.root.context.getString(R.string.category_internet)
                ExpenseCategory.MAINTENANCE -> binding.root.context.getString(R.string.category_maintenance)
                ExpenseCategory.PURCHASES -> binding.root.context.getString(R.string.category_purchases)
                ExpenseCategory.WATER -> binding.root.context.getString(R.string.category_water)
                ExpenseCategory.OTHER -> binding.root.context.getString(R.string.category_other)
            }
        }

        private fun getCategoryIcon(category: ExpenseCategory): Int {
            return when (category) {
                ExpenseCategory.ELECTRICITY -> R.drawable.ic_electricity
                ExpenseCategory.INTERNET -> R.drawable.ic_internet
                ExpenseCategory.MAINTENANCE -> R.drawable.ic_maintenance
                ExpenseCategory.PURCHASES -> R.drawable.ic_purchases
                ExpenseCategory.WATER -> R.drawable.ic_water
                ExpenseCategory.OTHER -> R.drawable.ic_other
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ExpenseCategory>() {
        override fun areItemsTheSame(oldItem: ExpenseCategory, newItem: ExpenseCategory): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: ExpenseCategory, newItem: ExpenseCategory): Boolean = oldItem == newItem
    }
}
