package com.mohamed.playstation.presentation.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.ItemExpenseBinding
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.ExpenseCategory
import java.text.SimpleDateFormat
import java.util.Locale

class DashboardExpenseAdapter(
    private val currency: String
) : ListAdapter<Expense, DashboardExpenseAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: Expense) {
            // Remove interactive long clicks for dashboard
            binding.root.setOnLongClickListener(null)
            binding.root.isLongClickable = false

            binding.tvCategoryName.text = getCategoryName(expense.category)
            binding.tvAmount.text = CurrencyUtils.formatAmount(expense.amount, currency)
            binding.tvDescription.text = expense.description
            binding.tvDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expense.expenseDate)
            binding.ivCategory.setImageResource(getCategoryIcon(expense.category))
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

    class DiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean = oldItem == newItem
    }
}
