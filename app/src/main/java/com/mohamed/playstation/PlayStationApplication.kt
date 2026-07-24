package com.mohamed.playstation

import android.app.Application

import androidx.appcompat.app.AppCompatDelegate
import com.mohamed.playstation.core.localization.LocaleManager
import com.mohamed.playstation.core.notifications.SessionAlarmScheduler
import com.mohamed.playstation.core.notifications.SessionNotificationHelper
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.data.repository.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
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

    @Inject
    lateinit var localeManager: LocaleManager

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Configuration collection moved to MainActivity for experiment


        sessionNotificationHelper.createNotificationChannels()
    }
}
