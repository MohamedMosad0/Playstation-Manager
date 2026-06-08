package com.mohamed.playstation.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mohamed.playstation.R
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.domain.model.Session
import com.mohamed.playstation.presentation.ui.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates the session notification channel and posts fixed-session warnings / end alerts.
 */
@Singleton
class SessionNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val alertChannel = NotificationChannel(
            AppConstants.NOTIFICATION_CHANNEL_ID,
            AppConstants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(alertChannel)
    }

    /** @deprecated Use [createNotificationChannels]. */
    fun createNotificationChannel() = createNotificationChannels()

    fun canPostNotifications(): Boolean =
        NotificationPermissionHelper.hasNotificationPermission(context)

    /**
     * Warning when a fixed session has <= [warningMinutes] remaining.
     */
    fun showSessionEndingWarning(
        session: Session,
        warningMinutes: Int,
        soundEnabled: Boolean
    ) {
        if (!canPostNotifications()) return

        val title = "${session.deviceType} #${session.deviceNumber}"
        val text = context.getString(R.string.notification_session_ending_warning, warningMinutes)

        val notification = buildNotification(
            title = title,
            text = text,
            notificationId = warningNotificationId(session.id),
            soundEnabled = soundEnabled
        )
        notificationManager.notify(warningNotificationId(session.id), notification)
    }

    /**
     * Sent when a fixed session auto-ends and a receipt was created.
     */
    fun showSessionEnded(
        session: Session,
        receiptId: Long,
        soundEnabled: Boolean
    ) {
        if (!canPostNotifications()) return

        val title = "${session.deviceType} #${session.deviceNumber}"
        val text = context.getString(R.string.notification_session_ended_receipt)

        val notification = buildNotification(
            title = title,
            text = text,
            notificationId = endedNotificationId(session.id),
            soundEnabled = soundEnabled
        )
        notificationManager.notify(endedNotificationId(session.id), notification)
    }

    fun cancelSessionNotifications(sessionId: Long) {
        notificationManager.cancel(warningNotificationId(sessionId))
        notificationManager.cancel(endedNotificationId(sessionId))
    }

    private fun buildNotification(
        title: String,
        text: String,
        notificationId: Int,
        soundEnabled: Boolean
    ): android.app.Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            AppConstants.NOTIFICATION_REQUEST_CODE_OPEN_APP + notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ps_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(!soundEnabled)
            .build()
    }

    companion object {
        fun warningNotificationId(sessionId: Long): Int =
            (sessionId * 2).toInt()

        fun endedNotificationId(sessionId: Long): Int =
            (sessionId * 2 + 1).toInt()
    }
}
