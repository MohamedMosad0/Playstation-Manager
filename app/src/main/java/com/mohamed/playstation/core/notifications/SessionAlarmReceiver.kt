package com.mohamed.playstation.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.utils.SessionTimer
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.usecase.SessionUseCases
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SessionAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionUseCases: SessionUseCases

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var sessionNotificationHelper: SessionNotificationHelper

    @Inject
    lateinit var sessionAlarmScheduler: SessionAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra(AppConstants.EXTRA_SESSION_ID, -1L)
        if (sessionId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    AppConstants.ACTION_SESSION_WARNING_ALARM -> handleWarningAlarm(sessionId)
                    AppConstants.ACTION_SESSION_END_ALARM -> handleEndAlarm(sessionId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle session alarm for session $sessionId")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleWarningAlarm(sessionId: Long) {
        val session = sessionUseCases.getSessionById(sessionId)
        if (session == null || !session.isFixed()) {
            sessionAlarmScheduler.cancelSessionAlarms(sessionId)
            return
        }

        if (!session.isActive()) {
            sessionAlarmScheduler.cancelSessionAlarms(sessionId)
            return
        }

        val warningSettings = settingsManager.getWarningNotificationSettings()
        if (!warningSettings.shouldScheduleWarning()) return

        val now = System.currentTimeMillis()
        val remainingMs = SessionTimer.getRemainingMs(session, now) ?: return
        val thresholdMs = warningSettings.warningMinutes * 60_000L

        if (remainingMs in 1..thresholdMs) {
            sessionNotificationHelper.showSessionEndingWarning(
                session = session,
                warningMinutes = warningSettings.warningMinutes,
                soundEnabled = warningSettings.soundEnabled
            )
            return
        }

        if (remainingMs > thresholdMs) {
            sessionAlarmScheduler.syncSession(sessionId)
        }
    }

    private suspend fun handleEndAlarm(sessionId: Long) {
        val session = sessionUseCases.getSessionById(sessionId)
        if (session == null || !session.isFixed()) {
            sessionAlarmScheduler.cancelSessionAlarms(sessionId)
            return
        }

        if (!session.isActive()) {
            sessionAlarmScheduler.cancelSessionAlarms(sessionId)
            return
        }

        if (!SessionTimer.isFixedExpired(session, System.currentTimeMillis())) {
            sessionAlarmScheduler.syncSession(sessionId)
            return
        }

        try {
            val receiptId = sessionUseCases.endSessionAndCreateReceipt(
                session = session,
                currencyCode = settingsManager.getCurrencyCode(),
                pricing = settingsManager.getPricingSettings()
            )
            val warningSettings = settingsManager.getWarningNotificationSettings()

            sessionAlarmScheduler.cancelSessionAlarms(sessionId)
            sessionNotificationHelper.cancelSessionNotifications(sessionId)
            sessionNotificationHelper.showSessionEnded(
                session = session,
                receiptId = receiptId,
                soundEnabled = warningSettings.soundEnabled
            )
        } catch (e: Exception) {
            sessionAlarmScheduler.syncSession(sessionId)
            Timber.e(e, "Failed to end session ${session.id}")
            return
        }
    }
}
