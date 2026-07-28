package com.mohamed.playstation.presentation.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.mohamed.playstation.R
import com.mohamed.playstation.core.localization.LocaleManager
import com.mohamed.playstation.core.notifications.NotificationPermissionHelper
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var exactAlarmPermissionDialog: androidx.appcompat.app.AlertDialog? = null

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var localeManager: LocaleManager

    private fun applyDarkMode(isDark: Boolean) {
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        val currentMode = AppCompatDelegate.getDefaultNightMode()

        if (mode == currentMode) {
            return
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationPermissionHelper.registerAndRequest(this)
        checkExactAlarmPermission()

        setupNavigation()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsManager.appConfigFlow.collect { config ->
                    localeManager.applyLanguage(config.language)
                    applyDarkMode(config.isDark)
                }
            }
        }
    }

    private fun setupNavigation() {

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val builder = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setEnterAnim(R.anim.nav_fade_enter)
                .setExitAnim(R.anim.nav_fade_exit)
                .setPopEnterAnim(R.anim.nav_fade_enter)
                .setPopExitAnim(R.anim.nav_fade_exit)

            if (item.order and android.view.Menu.CATEGORY_SECONDARY == 0) {
                builder.setPopUpTo(
                    navController.graph.startDestinationId,
                    inclusive = false,
                    saveState = true
                )
            }
            val options = builder.build()
            try {
                navController.navigate(item.itemId, null, options)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                exactAlarmPermissionDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.exact_alarm_permission_title))
                    .setMessage(getString(R.string.exact_alarm_permission_message))
                    .setPositiveButton(getString(R.string.action_confirm)) { _, _ ->
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        )
                        startActivity(intent)
                    }
                    .setNegativeButton(getString(R.string.action_cancel), null)
                    .create()
                exactAlarmPermissionDialog?.show()
            }
        }
    }

    override fun onDestroy() {
        exactAlarmPermissionDialog?.dismiss()
        exactAlarmPermissionDialog = null
        super.onDestroy()
    }
}
