package com.passworddisk.app.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnticipateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.passworddisk.app.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val appIcon = findViewById<ImageView>(R.id.app_icon)
        val appName = findViewById<TextView>(R.id.app_name)

        // 初始状态：完全透明并稍微缩小
        appIcon.alpha = 0f
        appIcon.scaleX = 0.8f
        appIcon.scaleY = 0.8f
        appName.alpha = 0f
        appName.translationY = 30f

        // 创建图标动画：淡入 + 缩放
        val iconAlpha = ObjectAnimator.ofFloat(appIcon, "alpha", 0f, 1f)
        val iconScaleX = ObjectAnimator.ofFloat(appIcon, "scaleX", 0.8f, 1f)
        val iconScaleY = ObjectAnimator.ofFloat(appIcon, "scaleY", 0.8f, 1f)

        // 创建文字动画：淡入 + 上移
        val nameAlpha = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f)
        val nameTranslation = ObjectAnimator.ofFloat(appName, "translationY", 30f, 0f)

        // 组合动画
        val animatorSet = AnimatorSet().apply {
            playTogether(iconAlpha, iconScaleX, iconScaleY)
            playTogether(nameAlpha, nameTranslation)
            nameAlpha.startDelay = 200 // 文字动画延迟200ms
            duration = 800
            interpolator = AnticipateInterpolator()
            doOnEnd {
                // 动画结束后延迟1秒跳转到主页面
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }, 800)
            }
        }

        animatorSet.start()
    }
}
