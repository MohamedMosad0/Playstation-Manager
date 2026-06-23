package com.mohamed.playstation

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.mohamed.playstation.core.notifications.SessionAlarmScheduler
import com.mohamed.playstation.core.notifications.SessionNotificationHelper
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.data.repository.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PlayStationApplication : Application() {

    @Inject
    lateinit var sessionNotificationHelper: SessionNotificationHelper

    @Inject
    lateinit var sessionAlarmScheduler: SessionAlarmScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var settingsManager: SettingsManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun applyDarkMode(isDark: Boolean, source: String) {
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        android.util.Log.d("SettingsAudit", "[SET_NIGHT_MODE] Source: $source, Thread: ${Thread.currentThread().name}, Timestamp: ${System.currentTimeMillis()}, Requested: $mode, CurrentUI: ${resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK}")
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        // Keep watching for runtime changes (user toggles the switch while the app is running)
        // This will also asynchronously apply the initial dark mode state upon app launch.
        applicationScope.launch {
            settingsRepository.darkModeFlow.collect { isDark ->
                applyDarkMode(isDark, "Application.collect")
            }
        }

        sessionNotificationHelper.createNotificationChannels()
        sessionAlarmScheduler.initialize()

        Timber.d("PlayStation Application Started")
    }
}
