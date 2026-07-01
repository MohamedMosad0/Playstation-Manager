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
            binding.tvRank.text = "$rank"
            binding.tvProductName.text = item.name
            
            val isPrepared = item.isPrepared
            val unitName = item.unitLabel
            val pluralUnit = binding.root.context.getString(com.mohamed.playstation.core.utils.UnitFormatUtils.getPluralUnitRes(unitName))
            
            binding.tvQuantitySold.text = "${binding.root.context.getString(com.mohamed.playstation.R.string.quantity_sold_label)} ${item.quantitySold} $pluralUnit"
            
            val formattedRevenue = CurrencyUtils.formatAmount(binding.root.context, item.revenue, currency)
            binding.tvProductRevenue.text = formattedRevenue
            
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
