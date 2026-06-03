package com.mohamed.playstation.core.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Runtime POST_NOTIFICATIONS handling for API 33+.
 */
object NotificationPermissionHelper {

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Registers a permission launcher on the activity and requests permission if needed.
     * Safe to call on every cold start; only prompts when not yet granted.
     */
    fun registerAndRequest(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* result handled by system; notifications simply won't show if denied */ }

        if (!hasNotificationPermission(activity)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
