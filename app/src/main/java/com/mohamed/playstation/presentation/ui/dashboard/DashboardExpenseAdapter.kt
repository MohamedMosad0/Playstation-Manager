package com.mohamed.playstation.presentation.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.AppFormatters
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.ItemExpenseBinding
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.ExpenseCategory
import android.text.BidiFormatter

class DashboardExpenseAdapter(
    private var currency: String
) : ListAdapter<Expense, DashboardExpenseAdapter.ViewHolder>(DiffCallback()) {

    fun updateCurrency(newCurrency: String) {
        if (currency != newCurrency) {
            currency = newCurrency
            notifyDataSetChanged()
        }
    }

    private var isInitialized = false
    private var strCategoryElectricity = ""
    private var strCategoryInternet = ""
    private var strCategoryMaintenance = ""
    private var strCategoryPurchases = ""
    private var strCategoryWater = ""
    private var strCategoryOther = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (!isInitialized) {
            val context = parent.context
            strCategoryElectricity = context.getString(R.string.category_electricity)
            strCategoryInternet = context.getString(R.string.category_internet)
            strCategoryMaintenance = context.getString(R.string.category_maintenance)
            strCategoryPurchases = context.getString(R.string.category_purchases)
            strCategoryWater = context.getString(R.string.category_water)
            strCategoryOther = context.getString(R.string.category_other)
            isInitialized = true
        }
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

            binding.tvCategoryName.text = BidiFormatter.getInstance().unicodeWrap(getCategoryName(expense.category))
            binding.tvAmount.text = CurrencyUtils.formatAmount(binding.root.context, expense.amount, currency)
            binding.tvDescription.text = BidiFormatter.getInstance().unicodeWrap(expense.description)
            binding.tvDate.text = AppFormatters.formatDate(binding.root.context, expense.expenseDate)
            binding.ivCategory.setImageResource(getCategoryIcon(expense.category))
        }

        private fun getCategoryName(category: ExpenseCategory): String {
            return when (category) {
                ExpenseCategory.ELECTRICITY -> strCategoryElectricity
                ExpenseCategory.INTERNET -> strCategoryInternet
                ExpenseCategory.MAINTENANCE -> strCategoryMaintenance
                ExpenseCategory.PURCHASES -> strCategoryPurchases
                ExpenseCategory.WATER -> strCategoryWater
                ExpenseCategory.OTHER -> strCategoryOther
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
