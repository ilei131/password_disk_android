package com.passworddisk.app.ui

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.passworddisk.app.R
import com.passworddisk.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var lastInsets: WindowInsetsCompat? = null
    private var destinationWantsNav: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 密码类应用：正式包禁止截屏 / 录屏，也不在最近任务中留缩略图
        SecureUi.protectScreen(this)

        // 全屏绘制，内容延伸到状态栏/导航栏后再用 insets 让出安全区
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // API 30+ 由本页自己处理键盘高度，避免系统把窗口压缩、底栏被顶到键盘上方
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val darkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            lastInsets = insets
            applyInsets()
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            destinationWantsNav = destination.id in TOP_LEVEL_DESTINATIONS
            applyInsets()
        }
    }

    /**
     * 计算安全区：
     * - 键盘弹出时不显示底栏，并把键盘高度让给当前页面，输入框与底部按钮不会被遮挡；
     * - 底栏可见时由底栏让出导航栏高度，页面内容顶到导航栏上方。
     */
    private fun applyInsets() {
        val insets = lastInsets ?: return
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        val imeVisible = ime.bottom > bars.bottom

        val showNav = destinationWantsNav && !imeVisible
        binding.bottomNavView.visibility = if (showNav) View.VISIBLE else View.GONE
        binding.navDivider.visibility = if (showNav) View.VISIBLE else View.GONE

        val bottomSpace = maxOf(bars.bottom, ime.bottom)
        binding.navHostFragment.updatePadding(
            top = bars.top,
            bottom = if (showNav) 0 else bottomSpace
        )
        binding.bottomNavView.updatePadding(bottom = if (showNav) bars.bottom else 0)
    }

    private companion object {
        val TOP_LEVEL_DESTINATIONS = setOf(
            R.id.homeFragment,
            R.id.totpFragment,
            R.id.settingsFragment
        )
    }
}
