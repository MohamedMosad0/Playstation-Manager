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
    }
}