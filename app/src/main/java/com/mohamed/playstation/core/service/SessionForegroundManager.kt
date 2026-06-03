package com.mohamed.playstation.core.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.mohamed.playstation.data.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts the foreground service when at least one session is active or paused.
 * The service stops itself when the session list becomes empty.
 */
@Singleton
class SessionForegroundManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        scope.launch {
            combine(
                sessionRepository.getActiveSessions(),
                sessionRepository.getPausedSessions()
            ) { active, paused -> active.size + paused.size }
                .map { count -> count > 0 }
                .distinctUntilChanged()
                .collect { hasSessions ->
                    if (hasSessions) {
                        startService()
                    }
                }
        }

        Timber.d("SessionForegroundManager initialized")
    }

    private fun startService() {
        val intent = Intent(context, SessionForegroundService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Timber.d("SessionForegroundService start requested")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start SessionForegroundService")
        }
    }
}
