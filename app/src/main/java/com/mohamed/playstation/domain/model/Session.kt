package com.mohamed.playstation.domain.model

import com.mohamed.playstation.core.constants.AppConstants
import java.util.Date
import kotlin.math.ceil

/**
 * Domain Model للجلسة
 */
data class Session(
    val id: Long = 0,
    val deviceType: String,
    val deviceNumber: Int,
    val sessionType: String,
    val sessionMode: String = AppConstants.SESSION_MODE_OPEN,
    val isMultiPlayer: Boolean = false,
    val fixedDurationMinutes: Int? = null,
    val startTime: Date,
    val endTime: Date? = null,
    val pausedAt: Date? = null,
    val totalPausedMinutes: Long = 0,
    val status: String,
    val pricePerHour: Double,
    val notes: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    fun isOpen(): Boolean = sessionMode == AppConstants.SESSION_MODE_OPEN

    fun isFixed(): Boolean = sessionMode == AppConstants.SESSION_MODE_FIXED

    /**
     * Elapsed active play time in minutes (derived from timestamps).
     * Hardening: Partial minute counts as a full minute (Ceil strategy).
     */
    fun getElapsedMinutes(nowMs: Long = System.currentTimeMillis()): Long {
        val endMs = endTime?.time ?: pausedAt?.time ?: nowMs
        val grossMs = endMs - startTime.time
        val netMs = grossMs - (totalPausedMinutes * 60000L)
        return ceil(netMs / 60000.0).toLong().coerceAtLeast(0)
    }

    /**
     * Billable duration for pricing and receipts.
     */
    fun getDurationMinutes(nowMs: Long = System.currentTimeMillis()): Long {
        val elapsed = getElapsedMinutes(nowMs)
        if (isFixed() && fixedDurationMinutes != null) {
            return minOf(elapsed, fixedDurationMinutes.toLong())
        }
        return elapsed
    }

    fun calculateTotal(nowMs: Long = System.currentTimeMillis()): Double {
        if (isFixed() && fixedDurationMinutes != null && isEnded()) {
            val hours = fixedDurationMinutes / 60.0
            return hours * pricePerHour
        }
        val hours = getDurationMinutes(nowMs) / 60.0
        return hours * pricePerHour
    }

    fun getFormattedDuration(nowMs: Long = System.currentTimeMillis()): String {
        val totalMinutes = getDurationMinutes(nowMs)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    fun isActive(): Boolean = status == AppConstants.SESSION_STATUS_ACTIVE

    fun isPaused(): Boolean = status == AppConstants.SESSION_STATUS_PAUSED

    fun isEnded(): Boolean = status == AppConstants.SESSION_STATUS_ENDED
}
