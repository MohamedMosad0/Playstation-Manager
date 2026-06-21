package com.mohamed.playstation.presentation.ui.sessions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.ItemSessionProductBinding
import com.mohamed.playstation.domain.model.SessionProduct

class ProductAdapter(
    private var currencyCode: String
) : ListAdapter<SessionProduct, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    fun updateCurrency(newCurrencyCode: String) {
        if (currencyCode == newCurrencyCode) return
        currencyCode = newCurrencyCode
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemSessionProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemSessionProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: SessionProduct) {
            binding.tvProductName.text = product.nameSnapshot
            val unitPrice = CurrencyUtils.formatAmount(product.sellPriceSnapshot, currencyCode)
            val lineTotal = CurrencyUtils.formatAmount(product.getLineTotal(), currencyCode)
            binding.tvProductQuantity.text = "$unitPrice × ${product.quantitySold}"
            binding.tvProductPrice.text = lineTotal
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<SessionProduct>() {
        override fun areItemsTheSame(oldItem: SessionProduct, newItem: SessionProduct): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SessionProduct, newItem: SessionProduct): Boolean {
            return oldItem == newItem
        }
    }
}
