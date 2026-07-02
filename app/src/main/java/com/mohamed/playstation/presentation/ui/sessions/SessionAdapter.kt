package com.mohamed.playstation.presentation.ui.sessions

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.SessionTimer
import com.mohamed.playstation.databinding.ItemSessionCardBinding
import com.mohamed.playstation.domain.model.Session

class SessionAdapter(
    private var tick: Long = 0L,
    private val onCardClick: (Session) -> Unit
) : ListAdapter<Session, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    // Cached resources to prevent allocations in bind() and bindTimerOnly()
    private var isInitialized = false
    private var colorStatusPaused = 0
    private var colorStatusActive = 0
    private var colorTextSecondary = 0
    private var colorPsBluePrimary = 0
    private var bgStatusPaused: ColorStateList? = null
    private var bgStatusActive: ColorStateList? = null
    private var bgStatusAvailable: ColorStateList? = null
    private var strFinishingProgress: String = ""
    private var strStatusRunning: String = ""
    private var strStatusPaused: String = ""
    private var strStatusAvailable: String = ""
    private var strModeFixed: String = ""
    private var strModeOpen: String = ""
    private var strMultiplayer: String = ""
    private var strSinglePlayer: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        if (!isInitialized) {
            val context = parent.context
            colorStatusPaused = context.getColor(R.color.status_paused)
            colorStatusActive = context.getColor(R.color.status_active)
            colorTextSecondary = context.getColor(R.color.text_secondary)
            colorPsBluePrimary = context.getColor(R.color.ps_blue_primary)
            
            bgStatusPaused = ColorStateList.valueOf(ColorUtils.setAlphaComponent(colorStatusPaused, 38))
            bgStatusActive = ColorStateList.valueOf(ColorUtils.setAlphaComponent(colorStatusActive, 38))
            bgStatusAvailable = ColorStateList.valueOf(ColorUtils.setAlphaComponent(colorTextSecondary, 25))
            
            strFinishingProgress = context.getString(R.string.finishing_progress)
            strStatusRunning = context.getString(R.string.status_running)
            strStatusPaused = context.getString(R.string.status_paused)
            strStatusAvailable = context.getString(R.string.status_available)
            strModeFixed = context.getString(R.string.session_mode_fixed)
            strModeOpen = context.getString(R.string.session_mode_open)
            strMultiplayer = context.getString(R.string.multiplayer)
            strSinglePlayer = context.getString(R.string.single_player)
            isInitialized = true
        }

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

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("TIMER_TICK")) {
            holder.bindTimerOnly(getItem(position), tick)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun updateTick(newTick: Long) {
        tick = newTick
        currentList.forEachIndexed { index, session ->
            if (session.isActive() || session.isPaused()) {
                notifyItemChanged(index, "TIMER_TICK")
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

                val modeText = if (session.isFixed()) strModeFixed else strModeOpen
                val playerText = if (session.isMultiPlayer) strMultiplayer else strSinglePlayer
                tvSessionMode.text = "$modeText • $playerText"

                when {
                    session.isActive() -> {
                        val isAutoEnding = session.isFixed() && (SessionTimer.getRemainingMs(session, currentTick) ?: 0L) <= 0L
                        if (isAutoEnding) {
                            tvStatus.text = strFinishingProgress
                            tvStatus.setTextColor(colorStatusPaused)
                            tvStatus.backgroundTintList = bgStatusPaused
                            tvTimer.visibility = View.VISIBLE
                        } else {
                            tvStatus.text = strStatusRunning
                            tvStatus.setTextColor(colorStatusActive)
                            tvStatus.backgroundTintList = bgStatusActive
                            tvTimer.visibility = View.VISIBLE
                        }
                    }
                    session.isPaused() -> {
                        tvStatus.text = strStatusPaused
                        tvStatus.setTextColor(colorStatusPaused)
                        tvStatus.backgroundTintList = bgStatusPaused
                        tvTimer.visibility = View.VISIBLE
                    }
                    else -> {
                        tvStatus.text = strStatusAvailable
                        tvStatus.setTextColor(colorTextSecondary)
                        tvStatus.backgroundTintList = bgStatusAvailable
                        tvTimer.visibility = View.GONE
                    }
                }

                if (tvTimer.visibility == View.VISIBLE) {
                    val isAutoEnding = session.isActive() && session.isFixed() && (SessionTimer.getRemainingMs(session, currentTick) ?: 0L) <= 0L
                    if (isAutoEnding) {
                        tvTimer.text = "00:00:00"
                        tvTimer.setTextColor(colorStatusPaused)
                    } else {
                        tvTimer.text = SessionTimer.formatForSession(session, currentTick)
                        if (session.isFixed() && session.isActive()) {
                            val remaining = SessionTimer.getRemainingMs(session, currentTick) ?: 0L
                            tvTimer.setTextColor(if (remaining <= 5 * 60_000) colorStatusPaused else colorPsBluePrimary)
                        } else {
                            tvTimer.setTextColor(colorPsBluePrimary)
                        }
                    }
                }
            }
        }

        fun bindTimerOnly(session: Session, currentTick: Long) {
            with(binding) {
                if (session.isActive()) {
                    val isAutoEnding = session.isFixed() && (SessionTimer.getRemainingMs(session, currentTick) ?: 0L) <= 0L
                    if (isAutoEnding) {
                        tvStatus.text = strFinishingProgress
                        tvStatus.setTextColor(colorStatusPaused)
                        tvStatus.backgroundTintList = bgStatusPaused
                    } else {
                        tvStatus.text = strStatusRunning
                        tvStatus.setTextColor(colorStatusActive)
                        tvStatus.backgroundTintList = bgStatusActive
                    }
                }

                if (tvTimer.visibility == View.VISIBLE) {
                    val isAutoEnding = session.isActive() && session.isFixed() && (SessionTimer.getRemainingMs(session, currentTick) ?: 0L) <= 0L
                    if (isAutoEnding) {
                        tvTimer.text = "00:00:00"
                        tvTimer.setTextColor(colorStatusPaused)
                    } else {
                        tvTimer.text = SessionTimer.formatForSession(session, currentTick)
                        if (session.isFixed() && session.isActive()) {
                            val remaining = SessionTimer.getRemainingMs(session, currentTick) ?: 0L
                            tvTimer.setTextColor(if (remaining <= 5 * 60_000) colorStatusPaused else colorPsBluePrimary)
                        } else {
                            tvTimer.setTextColor(colorPsBluePrimary)
                        }
                    }
                }
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean =
            oldItem == newItem
    }
}
