package com.mohamed.playstation.presentation.ui.sessions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.databinding.ItemCompletedSessionBinding
import com.mohamed.playstation.domain.model.Session
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for completed (ended) sessions.
 *
 * Static list — no timers, no periodic updates.
 * Reuses the existing Session domain model.
 */
class CompletedSessionAdapter(
    private val currency: String
) : ListAdapter<Session, CompletedSessionAdapter.CompletedViewHolder>(CompletedDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

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
                tvCompletedDeviceName.text = buildString {
                    append(session.deviceType)
                    append(" #")
                    append(session.deviceNumber)
                }

                // Start time
                tvCompletedStartTime.text = timeFormat.format(session.startTime)

                // End time
                tvCompletedEndTime.text = session.endTime?.let { timeFormat.format(it) } ?: "--:--"

                // Duration — use endTime so static, no clock tick needed
                val endMs = session.endTime?.time ?: System.currentTimeMillis()
                tvCompletedDuration.text = session.getFormattedDuration(endMs)

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
