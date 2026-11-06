package com.passworddisk.app.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.passworddisk.app.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureUi.protectScreen(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_splash)

        val appIcon = findViewById<ImageView>(R.id.app_icon)
        val appName = findViewById<TextView>(R.id.app_name)
        WindowInsetsControllerCompat(window, appIcon).isAppearanceLightStatusBars = false

        appIcon.alpha = 0f
        appIcon.scaleX = 0.86f
        appIcon.scaleY = 0.86f
        appName.alpha = 0f
        appName.translationY = 24f

        val iconAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(appIcon, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(appIcon, "scaleX", 0.86f, 1f),
                ObjectAnimator.ofFloat(appIcon, "scaleY", 0.86f, 1f)
            )
            duration = 520
            interpolator = DecelerateInterpolator()
        }

        val textAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(appName, "translationY", 24f, 0f)
            )
            duration = 420
            startDelay = 160
            interpolator = DecelerateInterpolator()
            doOnEnd {
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }, 240)
            }
        }

        iconAnim.start()
        textAnim.start()
    }
}
