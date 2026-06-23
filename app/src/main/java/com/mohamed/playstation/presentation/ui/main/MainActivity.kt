package com.mohamed.playstation.presentation.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.mohamed.playstation.R
import com.mohamed.playstation.core.notifications.NotificationPermissionHelper
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("SettingsAudit", "MainActivity.onCreate: savedInstanceState is ${if (savedInstanceState == null) "null" else "NOT null"}")
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationPermissionHelper.registerAndRequest(this)
        checkExactAlarmPermission()

        setupNavigation()

        Timber.d("Main Activity Started")
    }

    override fun onStart() {
        android.util.Log.d("SettingsAudit", "MainActivity.onStart")
        super.onStart()
    }

    override fun onResume() {
        android.util.Log.d("SettingsAudit", "MainActivity.onResume")
        super.onResume()
    }

    override fun onDestroy() {
        android.util.Log.d("SettingsAudit", "MainActivity.onDestroy")
        super.onDestroy()
    }

    private fun setupNavigation() {

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Timber.d(
                "DESTINATION = ${resources.getResourceEntryName(destination.id)}"
            )
        }
    }

    private fun checkExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(getString(com.mohamed.playstation.R.string.exact_alarm_permission_title))
                    .setMessage(getString(com.mohamed.playstation.R.string.exact_alarm_permission_message))
                    .setPositiveButton(getString(com.mohamed.playstation.R.string.action_confirm)) { _, _ ->
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        )
                        startActivity(intent)
                    }
                    .setNegativeButton(getString(com.mohamed.playstation.R.string.action_cancel), null)
                    .show()
            }
        }
    }
}
