package com.mohamed.playstation.core.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.utils.SessionTimer
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.data.repository.SessionRepository
import com.mohamed.playstation.domain.model.Session
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val settingsManager: SettingsManager
) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var initialized = false

    @Volatile
    private var initJob: kotlinx.coroutines.Job? = null

    fun initialize(): kotlinx.coroutines.Job? {
        if (initialized) return initJob
        initialized = true

        initJob = scope.launch {
            reconcileExistingFixedSessions()
        }
        return initJob
    }

    suspend fun syncSession(sessionId: Long, allowImmediateWarning: Boolean = true) {
        val session = sessionRepository.getSessionById(sessionId)
        if (session == null) {
            cancelSessionAlarms(sessionId)
            return
        }

        if (!session.isFixed() || !session.isActive()) {
            cancelSessionAlarms(sessionId)
            return
        }

        scheduleSessionAlarms(session, allowImmediateWarning)
    }

    fun cancelSessionAlarms(sessionId: Long) {
        alarmManager.cancel(buildWarningPendingIntent(sessionId))
        alarmManager.cancel(buildEndPendingIntent(sessionId))
    }

    private suspend fun reconcileExistingFixedSessions() {
        val activeSessions = sessionRepository.getActiveSessions().first()
        val pausedSessions = sessionRepository.getPausedSessions().first()

        activeSessions
            .filter { it.isFixed() }
            .forEach { session ->
                scheduleSessionAlarms(session, allowImmediateWarning = false)
            }

        pausedSessions
            .filter { it.isFixed() }
            .forEach { session ->
                cancelSessionAlarms(session.id)
            }
    }

    private suspend fun scheduleSessionAlarms(
        session: Session,
        allowImmediateWarning: Boolean
    ) {
        cancelSessionAlarms(session.id)

        val endTimeMs = SessionTimer.getFixedEndTimeMs(session) ?: return
        val now = System.currentTimeMillis()
        val warningSettings = settingsManager.getWarningNotificationSettings()

        if (warningSettings.shouldScheduleWarning()) {
            val warningTimeMs = endTimeMs - (warningSettings.warningMinutes * 60_000L)
            when {
                warningTimeMs > now -> {
                    setAlarm(warningTimeMs, buildWarningPendingIntent(session.id))
                }
                allowImmediateWarning && endTimeMs > now -> {
                    setAlarm(now, buildWarningPendingIntent(session.id))
                }
            }
        }

        setAlarm(maxOf(endTimeMs, now), buildEndPendingIntent(session.id))
        Timber.d("Scheduled fixed-session alarms for session ${session.id}")
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun setAlarm(
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Timber.e("canScheduleExactAlarms = ${alarmManager.canScheduleExactAlarms()}")
        }
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Timber.w("Exact alarms unavailable; using inexact alarm fallback")
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }

            else -> {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun buildWarningPendingIntent(sessionId: Long): PendingIntent {
        val intent = Intent(context, SessionAlarmReceiver::class.java).apply {
            action = AppConstants.ACTION_SESSION_WARNING_ALARM
            putExtra(AppConstants.EXTRA_SESSION_ID, sessionId)
        }
        return PendingIntent.getBroadcast(
            context,
            warningRequestCode(sessionId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildEndPendingIntent(sessionId: Long): PendingIntent {
        val intent = Intent(context, SessionAlarmReceiver::class.java).apply {
            action = AppConstants.ACTION_SESSION_END_ALARM
            putExtra(AppConstants.EXTRA_SESSION_ID, sessionId)
        }
        return PendingIntent.getBroadcast(
            context,
            endRequestCode(sessionId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val ALARM_REQUEST_CODE_BASE = 10_000

        private fun warningRequestCode(sessionId: Long): Int =
            ALARM_REQUEST_CODE_BASE + (sessionId * 2).toInt()

        private fun endRequestCode(sessionId: Long): Int =
            ALARM_REQUEST_CODE_BASE + (sessionId * 2 + 1).toInt()
    }
}
