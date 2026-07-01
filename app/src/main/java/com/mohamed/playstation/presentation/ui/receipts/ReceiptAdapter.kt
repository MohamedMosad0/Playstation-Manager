package com.mohamed.playstation.presentation.ui.receipts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.databinding.ItemReceiptBinding
import com.mohamed.playstation.presentation.ui.receipts.model.ReceiptListItemUiModel

/**
 * Adapter for displaying a list of receipts.
 * Consumes pre-formatted [ReceiptListItemUiModel] to ensure consistency and zero logic duplication.
 */
class ReceiptAdapter(
    private val onReceiptClick: (Long) -> Unit
) : ListAdapter<ReceiptListItemUiModel, ReceiptAdapter.ReceiptViewHolder>(ReceiptDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val binding = ItemReceiptBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReceiptViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReceiptViewHolder(
        private val binding: ItemReceiptBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: ReceiptListItemUiModel) {
            with(binding) {
                tvReceiptNumber.text = model.receiptNumber
                tvDeviceInfo.text = model.deviceInfo
                tvDuration.text = model.durationAndRange
                tvTotal.text = model.totalAmount
                chipPaymentMethod.text = model.paymentMethod
                chipPaymentMethod.isVisible = model.paymentMethod.isNotEmpty()
                
                tvProductsSummary.isVisible = model.hasProducts
                tvProductsSummary.text = model.productsSummary

                root.setOnClickListener {
                    onReceiptClick(model.id)
                }
            }
        }
    }

    private class ReceiptDiffCallback : DiffUtil.ItemCallback<ReceiptListItemUiModel>() {
        override fun areItemsTheSame(oldItem: ReceiptListItemUiModel, newItem: ReceiptListItemUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReceiptListItemUiModel, newItem: ReceiptListItemUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
