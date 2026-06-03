package com.mohamed.playstation.presentation.ui.receipts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.ItemReceiptBinding
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.SessionProductSummary
import java.util.Locale

/**
 * Adapter لعرض قائمة الفواتير
 */
class ReceiptAdapter(
    private val onReceiptClick: (Receipt) -> Unit
) : ListAdapter<Receipt, ReceiptAdapter.ReceiptViewHolder>(ReceiptDiffCallback()) {

    private var productSummaries: Map<Long, SessionProductSummary> = emptyMap()

    fun submitProductSummaries(summaries: Map<Long, SessionProductSummary>) {
        productSummaries = summaries
        notifyItemRangeChanged(0, itemCount)
    }

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

        fun bind(receipt: Receipt) {
            with(binding) {
                // Receipt Number
                tvReceiptNumber.text = root.context.getString(
                    R.string.receipt_number,
                    receipt.receiptNumber
                )

                // Device Info
                val sessionTypeText = when (receipt.sessionType) {
                    "single" -> root.context.getString(R.string.single_player)
                    "multi" -> root.context.getString(R.string.multiplayer)
                    else -> receipt.sessionType
                }

                tvDeviceInfo.text = "${receipt.deviceType} #${receipt.deviceNumber} • $sessionTypeText"

                // Duration and Time
                val timeRange = "${receipt.getFormattedStartTime()} - ${receipt.getFormattedEndTime()}"
                tvDuration.text = "${receipt.getFormattedDuration()} • $timeRange"

                // Total Amount
                val currencySymbol = CurrencyUtils.getCurrencySymbol(receipt.currencyCode)
                // استخدم Locale عند التنسيق لتجنب lint warnings
                tvTotal.text = "${String.format(Locale.getDefault(), "%.2f", receipt.totalAmount)} $currencySymbol"

                // Payment Method
                chipPaymentMethod.text = when (receipt.paymentMethod) {
                    "cash" -> root.context.getString(R.string.cash)
                    "card" -> root.context.getString(R.string.card)
                    else -> receipt.paymentMethod
                }

                val summary = productSummaries[receipt.sessionId]
                if (summary != null && summary.totalQuantity > 0) {
                    tvProductsSummary.text = root.context.getString(
                        R.string.receipt_products_summary,
                        summary.totalQuantity,
                        CurrencyUtils.formatAmount(summary.totalAmount, receipt.currencyCode)
                    )
                    tvProductsSummary.visibility = android.view.View.VISIBLE
                } else {
                    tvProductsSummary.visibility = android.view.View.GONE
                }

                // Click listener
                root.setOnClickListener {
                    onReceiptClick(receipt)
                }
            }
        }
    }

    private class ReceiptDiffCallback : DiffUtil.ItemCallback<Receipt>() {
        override fun areItemsTheSame(oldItem: Receipt, newItem: Receipt): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Receipt, newItem: Receipt): Boolean {
            return oldItem == newItem
        }
    }
}
