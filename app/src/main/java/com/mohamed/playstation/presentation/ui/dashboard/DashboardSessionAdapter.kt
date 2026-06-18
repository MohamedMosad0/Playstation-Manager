package com.mohamed.playstation.presentation.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.R
import com.mohamed.playstation.databinding.ItemSessionCardBinding
import com.mohamed.playstation.domain.model.Session
import java.text.SimpleDateFormat
import java.util.Locale

class DashboardSessionAdapter(
    private val onItemClick: (Session) -> Unit
) : ListAdapter<Session, DashboardSessionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSessionCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        fun bind(session: Session) {
            with(binding) {
                root.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClick(getItem(pos))
                    }
                }
                root.isClickable = true
                root.isFocusable = true

                tvDeviceName.text = buildString {
                    append(session.deviceType)
                    append(" #")
                    append(session.deviceNumber)
                }

                tvSessionMode.text = if (session.isFixed()) {
                    root.context.getString(R.string.session_mode_fixed)
                } else {
                    root.context.getString(R.string.session_mode_open)
                }

                when {
                    session.isActive() -> {
                        tvStatus.text = root.context.getString(R.string.status_running)
                        tvStatus.setTextColor(root.context.getColor(R.color.status_active))
                        viewStatusStrip.setBackgroundColor(root.context.getColor(R.color.status_active))
                        viewStatusDot.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(
                                root.context.getColor(R.color.status_active)
                            )
                        tvTimer.text = timeFormat.format(session.startTime)
                        tvTimer.textSize = 14f
                        tvTimer.setTextColor(root.context.getColor(R.color.text_secondary))
                        tvTimer.visibility = View.VISIBLE
                    }
                    session.isPaused() -> {
                        tvStatus.text = root.context.getString(R.string.status_paused)
                        tvStatus.setTextColor(root.context.getColor(R.color.status_paused))
                        viewStatusStrip.setBackgroundColor(root.context.getColor(R.color.status_paused))
                        viewStatusDot.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(
                                root.context.getColor(R.color.status_paused)
                            )
                        tvTimer.text = timeFormat.format(session.startTime)
                        tvTimer.textSize = 14f
                        tvTimer.setTextColor(root.context.getColor(R.color.text_secondary))
                        tvTimer.visibility = View.VISIBLE
                    }
                    else -> {
                        tvStatus.text = root.context.getString(R.string.status_available)
                        tvStatus.setTextColor(root.context.getColor(R.color.text_secondary))
                        viewStatusStrip.setBackgroundColor(root.context.getColor(R.color.divider))
                        viewStatusDot.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(
                                root.context.getColor(R.color.divider)
                            )
                        tvTimer.visibility = View.GONE
                    }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean =
            oldItem == newItem
    }
}
