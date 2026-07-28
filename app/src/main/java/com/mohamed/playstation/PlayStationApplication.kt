package com.mohamed.playstation

import android.app.Application
import com.mohamed.playstation.core.localization.LocaleManager
import com.mohamed.playstation.core.notifications.SessionAlarmScheduler
import com.mohamed.playstation.core.notifications.SessionNotificationHelper
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.data.repository.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import timber.log.Timber

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
        val language = runBlocking { settingsManager.getLanguage() }
        localeManager.applyLanguage(language)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        sessionNotificationHelper.createNotificationChannels()
    }
}
