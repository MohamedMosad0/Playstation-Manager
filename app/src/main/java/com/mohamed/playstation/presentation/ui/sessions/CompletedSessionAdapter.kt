package com.mohamed.playstation.presentation.ui.sessions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.core.utils.AppFormatters
import com.mohamed.playstation.databinding.ItemCompletedSessionBinding
import com.mohamed.playstation.domain.model.Session
import android.text.BidiFormatter

/**
 * Adapter for completed (ended) sessions.
 *
 * Static list — no timers, no periodic updates.
 * Reuses the existing Session domain model.
 */
class CompletedSessionAdapter(
    private var currency: String
) : ListAdapter<Session, CompletedSessionAdapter.CompletedViewHolder>(CompletedDiffCallback()) {

    fun updateCurrency(newCurrency: String) {
        if (currency != newCurrency) {
            currency = newCurrency
            notifyDataSetChanged()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletedViewHolder {
        val binding = ItemCompletedSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CompletedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompletedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CompletedViewHolder(
        private val binding: ItemCompletedSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: Session) {
            with(binding) {
                // Device name
                val deviceString = buildString {
                    append(session.deviceType)
                    append(" #")
                    append(session.deviceNumber)
                }
                tvCompletedDeviceName.text = BidiFormatter.getInstance().unicodeWrap(deviceString)

                // Start time
                tvCompletedStartTime.text = AppFormatters.formatTwentyFourHourTime(binding.root.context, session.startTime)

                // End time
                tvCompletedEndTime.text = session.endTime?.let {
                    AppFormatters.formatTwentyFourHourTime(binding.root.context, it)
                } ?: "--:--"

                // Duration — use endTime so static, no clock tick needed
                val endMs = session.endTime?.time ?: System.currentTimeMillis()
                tvCompletedDuration.text = AppFormatters.formatDuration(
                    binding.root.context,
                    session.getDurationMinutes(endMs)
                )

                // Total price
                val total = session.calculateTotal(endMs)
                tvCompletedTotal.text = CurrencyUtils.formatAmount(binding.root.context, total, currency)
            }
        }
    }

    class CompletedDiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean =
            oldItem == newItem
    }
}
