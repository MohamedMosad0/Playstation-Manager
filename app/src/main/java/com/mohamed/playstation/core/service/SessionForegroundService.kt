package com.mohamed.playstation.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.notifications.SessionNotificationHelper
import com.mohamed.playstation.core.utils.SessionTicker
import com.mohamed.playstation.data.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Display-only foreground service showing active/paused session timers.
 * Observes [SessionRepository] directly — not the ViewModel.
 */
@AndroidEntryPoint
class SessionForegroundService : Service() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var sessionTicker: SessionTicker

    @Inject
    lateinit var sessionNotificationHelper: SessionNotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("SessionForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val placeholder = sessionNotificationHelper.buildForegroundPlaceholderNotification()
        startForeground(AppConstants.FOREGROUND_SESSION_NOTIFICATION_ID, placeholder)

        observeJob?.cancel()
        observeJob = serviceScope.launch {
            combine(
                sessionRepository.getActiveSessions(),
                sessionRepository.getPausedSessions(),
                sessionTicker.tickerFlow
            ) { active, paused, tick ->
                Triple(active + paused, tick, Unit)
            }.collect { (sessions, tick, _) ->
                if (sessions.isEmpty()) {
                    Timber.d("No active/paused sessions — stopping foreground service")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val notification = sessionNotificationHelper
                        .buildForegroundSessionsNotification(sessions, tick)
                    startForeground(AppConstants.FOREGROUND_SESSION_NOTIFICATION_ID, notification)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        observeJob?.cancel()
        serviceScope.cancel()
        Timber.d("SessionForegroundService destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
