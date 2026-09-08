package com.mohamed.playstation.core.notifications

import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class SessionNotificationHelperTest {

    @Test
    fun canPostNotifications_preTiramisu_returnsTrue() {
        // On JVM, Build.VERSION.SDK_INT = 0 (< TIRAMISU 33).
        // hasNotificationPermission returns true for pre-TIRAMISU,
        // so canPostNotifications delegates correctly and returns true.
        val context: Context = mock()
        val helper = SessionNotificationHelper(context)

        assertTrue(helper.canPostNotifications())
    }
}
