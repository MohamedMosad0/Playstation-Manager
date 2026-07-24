package com.mohamed.playstation.core.utils

import android.content.Context
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.domain.model.Session
import java.util.concurrent.TimeUnit

/**
 * Computes elapsed and remaining time from session timestamps and pause data.
 * Countdown values are never persisted — derived at display time only.
 */
object SessionTimer {

    fun getFixedEndTimeMs(session: Session): Long? {
        val fixedMinutes = session.fixedDurationMinutes ?: return null
        return session.startTime.time +
            ((session.totalPausedMinutes + fixedMinutes.toLong()) * 60_000L)
    }

    fun getElapsedMs(session: Session, nowMs: Long): Long {
        val endMs = session.endTime?.time ?: session.pausedAt?.time ?: nowMs
        val grossMs = endMs - session.startTime.time
        val pausedMs = session.totalPausedMinutes * 60_000L
        return (grossMs - pausedMs).coerceAtLeast(0L)
    }

    fun getRemainingMs(session: Session, nowMs: Long): Long? {
        val fixedMinutes = session.fixedDurationMinutes ?: return null
        val fixedMs = fixedMinutes * 60_000L
        return (fixedMs - getElapsedMs(session, nowMs)).coerceAtLeast(0L)
    }

    fun isFixedExpired(session: Session, nowMs: Long): Boolean {
        if (!session.isFixed() || !session.isActive()) return false
        val remaining = getRemainingMs(session, nowMs) ?: return false
        
        // Safety buffer: only consider expired if remaining time is practically zero or negative.
        // This prevents early termination due to minor clock drift or scheduling jitter.
        return remaining < 100L
    }

    fun formatDurationMs(context: Context, durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        return AppFormatters.formatTimer(context, hours, minutes, seconds)
    }

    fun formatForSession(context: Context, session: Session, nowMs: Long): String {
        return if (session.sessionMode == AppConstants.SESSION_MODE_FIXED) {
            formatDurationMs(context, getRemainingMs(session, nowMs) ?: 0L)
        } else {
            formatDurationMs(context, getElapsedMs(session, nowMs))
        }
    }
}
