package com.mohamed.playstation.presentation.ui.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.ItemTopProductBinding

class TopProductsAdapter(
    private val currency: String
) : ListAdapter<TopProductItem, TopProductsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(private val binding: ItemTopProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TopProductItem, rank: Int) {
            val emojiRank = when(rank) {
                1 -> "1️⃣"
                2 -> "2️⃣"
                3 -> "3️⃣"
                4 -> "4️⃣"
                5 -> "5️⃣"
                else -> "$rank"
            }
            binding.tvRank.text = emojiRank
            binding.tvProductName.text = item.name
            
            val isPrepared = item.isPrepared
            val unitName = item.unitLabel
            val pluralUnit = binding.root.context.getString(com.mohamed.playstation.core.utils.UnitFormatUtils.getPluralUnitRes(unitName))
            val icon = if (isPrepared && unitName == binding.root.context.getString(com.mohamed.playstation.R.string.unit_pack)) "🍜" else if (isPrepared) "☕" else "📦"
            
            binding.tvQuantitySold.text = "$icon ${binding.root.context.getString(com.mohamed.playstation.R.string.quantity_sold_label)} ${item.quantitySold} $pluralUnit"
            
            val formattedRevenue = CurrencyUtils.formatAmount(binding.root.context, item.revenue, currency)
            binding.tvProductRevenue.text = binding.root.context.getString(com.mohamed.playstation.R.string.top_product_revenue_format, binding.root.context.getString(com.mohamed.playstation.R.string.revenue_label), formattedRevenue)
            
            itemView.alpha = 0f
            itemView.translationX = -50f
            itemView.animate().alpha(1f).translationX(0f).setDuration(400).setStartDelay((rank * 70).toLong()).start()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TopProductItem>() {
        override fun areItemsTheSame(oldItem: TopProductItem, newItem: TopProductItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: TopProductItem, newItem: TopProductItem): Boolean {
            return oldItem == newItem
        }
    }
}
