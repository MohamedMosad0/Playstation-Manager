package com.mohamed.playstation.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionAlarmScheduler: SessionAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val job = sessionAlarmScheduler.initialize()
            
            if (job != null) {
                job.invokeOnCompletion {
                    pendingResult.finish()
                }
            } else {
                pendingResult.finish()
            }
        }
    }
}
