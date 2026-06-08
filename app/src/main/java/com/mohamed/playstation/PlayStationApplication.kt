package com.mohamed.playstation

import android.app.Application
import com.mohamed.playstation.core.notifications.SessionAlarmScheduler
import com.mohamed.playstation.core.notifications.SessionNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PlayStationApplication : Application() {

    @Inject
    lateinit var sessionNotificationHelper: SessionNotificationHelper

    @Inject
    lateinit var sessionAlarmScheduler: SessionAlarmScheduler

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        sessionNotificationHelper.createNotificationChannels()
        sessionAlarmScheduler.initialize()

        Timber.d("PlayStation Application Started")
    }
}
