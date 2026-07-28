package com.mohamed.playstation.presentation.ui.dashboard

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.AppFormatters
import com.mohamed.playstation.databinding.ItemSessionCardBinding
import com.mohamed.playstation.domain.model.Session
import android.text.BidiFormatter

class DashboardSessionAdapter(
    private val onItemClick: (Session) -> Unit
) : ListAdapter<Session, DashboardSessionAdapter.ViewHolder>(DiffCallback()) {

    // Cached resources to prevent allocations in bind()
    private var isInitialized = false
    private var colorStatusPaused = 0
    private var colorStatusActive = 0
    private var colorTextSecondary = 0
    private var bgStatusPaused: ColorStateList? = null
    private var bgStatusActive: ColorStateList? = null
    private var bgStatusAvailable: ColorStateList? = null
    private var strStatusRunning: String = ""
    private var strStatusPaused: String = ""
    private var strStatusAvailable: String = ""
    private var strModeFixed: String = ""
    private var strModeOpen: String = ""
    private var strMultiplayer: String = ""
    private var strSinglePlayer: String = ""
    

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (!isInitialized) {
            val context = parent.context
            colorStatusPaused = context.getColor(R.color.status_paused)
            colorStatusActive = context.getColor(R.color.status_active)
            colorTextSecondary = context.getColor(R.color.text_secondary)
            
            bgStatusPaused = ColorStateList.valueOf(ColorUtils.setAlphaComponent(colorStatusPaused, 38))
            bgStatusActive = ColorStateList.valueOf(ColorUtils.setAlphaComponent(colorStatusActive, 38))
            bgStatusAvailable = ColorStateList.valueOf(ColorUtils.setAlphaComponent(colorTextSecondary, 25))
            
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
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSessionCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

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
                
                val deviceString = buildString {
                    append(session.deviceType)
                    append(" #")
                    append(session.deviceNumber)
                }
                tvDeviceName.text = BidiFormatter.getInstance().unicodeWrap(deviceString)

                val modeText = if (session.isFixed()) strModeFixed else strModeOpen
                val playerText = if (session.isMultiPlayer) strMultiplayer else strSinglePlayer
                tvSessionMode.text = buildString {
        append(modeText)
        append(" • ")
        append(playerText)
    }

                when {
                    session.isActive() -> {
                        tvStatus.text = strStatusRunning
                        tvStatus.setTextColor(colorStatusActive)
                        tvStatus.backgroundTintList = bgStatusActive
                        tvTimer.text = AppFormatters.formatTime(itemView.context, session.startTime)
                        tvTimer.textSize = 14f
                        tvTimer.setTextColor(colorTextSecondary)
                        tvTimer.visibility = View.VISIBLE
                    }
                    session.isPaused() -> {
                        tvStatus.text = strStatusPaused
                        tvStatus.setTextColor(colorStatusPaused)
                        tvStatus.backgroundTintList = bgStatusPaused
                        tvTimer.text = AppFormatters.formatTime(itemView.context, session.startTime)
                        tvTimer.textSize = 14f
                        tvTimer.setTextColor(colorTextSecondary)
                        tvTimer.visibility = View.VISIBLE
                    }
                    else -> {
                        tvStatus.text = strStatusAvailable
                        tvStatus.setTextColor(colorTextSecondary)
                        tvStatus.backgroundTintList = bgStatusAvailable
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
