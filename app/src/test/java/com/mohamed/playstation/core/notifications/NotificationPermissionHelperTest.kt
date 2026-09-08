package com.mohamed.playstation.core.notifications

import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class NotificationPermissionHelperTest {

    @Test
    fun hasNotificationPermission_preTiramisu_returnsTrue() {
        // JVM test environment has Build.VERSION.SDK_INT = 0 (< TIRAMISU 33),
        // so hasNotificationPermission must return true without checking ContextCompat.
        val context: Context = mock()

        assertTrue(NotificationPermissionHelper.hasNotificationPermission(context))
    }
}
