package com.mohamed.playstation.presentation.ui.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.mohamed.playstation.R
import com.mohamed.playstation.databinding.ActivitySplashBinding
import com.mohamed.playstation.presentation.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Splash Screen API
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Setup ViewBinding
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.d("Splash Screen Started")

        // Start animations and sound
        startAnimations()
        playStartupSound()
        navigateToMainAfterDelay()
    }

    /**
     * تشغيل الأنيميشن للأيقونة والنص
     */
    private fun startAnimations() {
        // Icon Animation - Scale and Fade In
        binding.ivPlayStationIcon.apply {
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Text Animation - Fade In (delayed)
        binding.tvAppName.apply {
            alpha = 0f
            translationY = 20f

            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(300)
                .setDuration(600)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Progress Bar Animation - Fade In (delayed)
        binding.progressBar.apply {
            alpha = 0f

            animate()
                .alpha(1f)
                .setStartDelay(600)
                .setDuration(400)
                .start()
        }

        // Loading Text Animation - Fade In (delayed)
        binding.tvLoading.apply {
            alpha = 0f

            animate()
                .alpha(1f)
                .setStartDelay(800)
                .setDuration(400)
                .start()
        }
    }

    /**
     * تشغيل صوت البداية
     */
    private fun playStartupSound() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.ps4_startup_sound)
            mediaPlayer?.apply {
                setOnCompletionListener {
                    release()
                }
                start()
            }
            Timber.d("Startup sound playing")
        } catch (e: Exception) {
            Timber.e(e, "Error playing startup sound")
        }
    }

    /**
     * الانتقال للشاشة الرئيسية بعد 2.5 ثانية
     */
    private fun navigateToMainAfterDelay() {
        lifecycleScope.launch {
            delay(4000) // 2.5 seconds

            // Fade out animation before navigation
            binding.root.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        navigateToMain()
                    }
                })
                .start()
        }
    }

    /**
     * الانتقال للشاشة الرئيسية
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

        // Smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Release MediaPlayer
        mediaPlayer?.release()
        mediaPlayer = null

        Timber.d("Splash Screen Destroyed")
    }
}