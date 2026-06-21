package com.mohamed.playstation.presentation.ui.sessions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.R
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.utils.SessionTimer
import com.mohamed.playstation.databinding.ItemSessionCardBinding
import com.mohamed.playstation.domain.model.Session

class SessionAdapter(
    private var tick: Long = 0L,
    private val onCardClick: (Session) -> Unit
) : ListAdapter<Session, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position), tick)
    }

    fun updateTick(newTick: Long) {
        tick = newTick
        currentList.forEachIndexed { index, session ->
            if (session.isActive() || session.isPaused()) {
                notifyItemChanged(index)
            }
        }
    }

    inner class SessionViewHolder(
        private val binding: ItemSessionCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onCardClick(getItem(pos))
                }
            }
        }

        fun bind(session: Session, currentTick: Long) {
            with(binding) {
                tvDeviceName.text = buildString {
                    append(session.deviceType)
                    append(" #")
                    append(session.deviceNumber)
                }

                val modeText = if (session.isFixed()) {
                    root.context.getString(R.string.session_mode_fixed)
                } else {
                    root.context.getString(R.string.session_mode_open)
                }
                
                val playerText = if (session.isMultiPlayer) root.context.getString(R.string.multiplayer) else root.context.getString(R.string.single_player)
                tvSessionMode.text = "$modeText • $playerText"

                when {
                    session.isActive() -> {
                        val isAutoEnding = session.isFixed() && (SessionTimer.getRemainingMs(session, currentTick) ?: 0L) <= 0L
                        if (isAutoEnding) {
                            tvStatus.text = "جاري الإنهاء..."
                            tvStatus.setTextColor(root.context.getColor(R.color.status_paused))
                            tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                androidx.core.graphics.ColorUtils.setAlphaComponent(root.context.getColor(R.color.status_paused), 38) // ~15% alpha
                            )
                            tvTimer.visibility = View.VISIBLE
                        } else {
                            tvStatus.text = root.context.getString(R.string.status_running)
                            tvStatus.setTextColor(root.context.getColor(R.color.status_active))
                            tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                androidx.core.graphics.ColorUtils.setAlphaComponent(root.context.getColor(R.color.status_active), 38)
                            )
                            tvTimer.visibility = View.VISIBLE
                        }
                    }
                    session.isPaused() -> {
                        tvStatus.text = root.context.getString(R.string.status_paused)
                        tvStatus.setTextColor(root.context.getColor(R.color.status_paused))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            androidx.core.graphics.ColorUtils.setAlphaComponent(root.context.getColor(R.color.status_paused), 38)
                        )
                        tvTimer.visibility = View.VISIBLE
                    }
                    else -> {
                        tvStatus.text = root.context.getString(R.string.status_available)
                        tvStatus.setTextColor(root.context.getColor(R.color.text_secondary))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            androidx.core.graphics.ColorUtils.setAlphaComponent(root.context.getColor(R.color.text_secondary), 25)
                        )
                        tvTimer.visibility = View.GONE
                    }
                }

                if (tvTimer.visibility == View.VISIBLE) {
                    val isAutoEnding = session.isActive() && session.isFixed() && (SessionTimer.getRemainingMs(session, currentTick) ?: 0L) <= 0L
                    if (isAutoEnding) {
                        tvTimer.text = "00:00:00"
                        tvTimer.setTextColor(root.context.getColor(R.color.status_paused))
                    } else {
                        tvTimer.text = SessionTimer.formatForSession(session, currentTick)
                        if (session.isFixed() && session.isActive()) {
                            val remaining = SessionTimer.getRemainingMs(session, currentTick) ?: 0L
                            tvTimer.setTextColor(
                                root.context.getColor(
                                    if (remaining <= 5 * 60_000) R.color.status_paused
                                    else R.color.ps_blue_primary
                                )
                            )
                        } else {
                            tvTimer.setTextColor(root.context.getColor(R.color.ps_blue_primary))
                        }
                    }
                }
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
            if (newItem.isActive() || newItem.isPaused()) return false
            return oldItem == newItem
        }
    }
}
