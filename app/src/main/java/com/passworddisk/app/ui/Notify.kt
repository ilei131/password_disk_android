package com.passworddisk.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.MaterialColors
import com.passworddisk.app.R

/**
 * 应用内统一提示条。
 *
 * 取代两种表现不一致的系统提示：
 * - `Toast`：系统渲染的黑底白字，位置由系统决定（有的贴底、有的偏上），且看不到图标语义；
 * - `Snackbar`：默认深色，需要在布局里有 CoordinatorLayout 才有理想表现。
 *
 * 这里统一成「轻卡片 + 彩色图标」的浮层：
 * - 深色文字写在 `colorSurface` 上，右下角投影 + 细描边，和页面卡片同一套视觉；
 * - 始终固定在窗口底部居中，自动避开导航栏、底部 Tab 与键盘；
 * - 通过 [Kind] 区分成功 / 失败 / 普通，图标底色跟着变，不用读文字也能判断结果。
 *
 * 浮层挂在当前窗口的内容容器上，所以底部面板、对话框里也能正常显示。
 */
object Notify {

    enum class Kind { SUCCESS, ERROR, INFO }

    private const val TAG = "PasswordDisk.Notify"
    private const val VISIBLE_MS = 2800L
    private const val ENTER_MS = 180L
    private const val EXIT_MS = 140L

    private val handler = Handler(Looper.getMainLooper())

    fun success(anchor: View, message: String) = show(anchor, message, Kind.SUCCESS)

    fun error(anchor: View, message: String) = show(anchor, message, Kind.ERROR)

    fun info(anchor: View, message: String) = show(anchor, message, Kind.INFO)

    fun show(anchor: View, message: String, kind: Kind = Kind.SUCCESS) {
        if (message.isBlank() || !ViewCompat.isAttachedToWindow(anchor)) return
        val windowRoot = anchor.rootView as? ViewGroup ?: return
        val container = windowRoot.findViewById<ViewGroup>(android.R.id.content) ?: windowRoot
        val context = anchor.context
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        // 同一窗口只保留一条：新提示直接顶掉旧的，避免叠成一片
        container.findViewWithTag<View>(TAG)?.let { old ->
            handler.removeCallbacksAndMessages(old)
            (old.parent as? ViewGroup)?.removeView(old)
        }

        val palette = paletteFor(context, kind)

        val icon = ImageView(context).apply {
            setImageResource(kind.iconRes())
            imageTintList = ColorStateList.valueOf(palette.iconFg)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val size = dp(26)
            setPadding(dp(5), dp(5), dp(5), dp(5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(palette.iconBg)
            }
            layoutParams = LinearLayout.LayoutParams(size, size)
        }

        val label = TextView(context).apply {
            text = message
            setTextColor(palette.text)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 3
            setLineSpacing(dp(2).toFloat(), 1f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(10) }
        }

        val pill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(9), dp(16), dp(9))
            elevation = dp(10).toFloat()
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(palette.background)
                setStroke(dp(1), palette.stroke)
            }
            addView(icon)
            addView(label)
        }

        val overlay = FrameLayout(context).apply {
            tag = TAG
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        overlay.addView(
            pill,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                marginStart = dp(28)
                marginEnd = dp(28)
                bottomMargin = bottomOffset(anchor, context, dp(18))
            }
        )
        ViewCompat.setAccessibilityLiveRegion(pill, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)

        container.addView(
            overlay,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        pill.alpha = 0f
        pill.translationY = dp(16).toFloat()
        pill.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(ENTER_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        handler.postAtTime(
            { dismiss(overlay, pill, dp(10).toFloat()) },
            overlay,
            SystemClock.uptimeMillis() + VISIBLE_MS
        )
    }

    private fun dismiss(overlay: FrameLayout, pill: View, distance: Float) {
        if (!ViewCompat.isAttachedToWindow(pill)) {
            (overlay.parent as? ViewGroup)?.removeView(overlay)
            return
        }
        pill.animate()
            .alpha(0f)
            .translationY(distance)
            .setDuration(EXIT_MS)
            .withEndAction {
                (overlay.parent as? ViewGroup)?.removeView(overlay)
            }
            .start()
    }

    /**
     * 提示条离窗口底部的高度：
     * - 底部 Tab 可见时贴着 Tab 上方（Tab 自身已包含导航栏高度，不能再叠加一次）；
     * - 其余情况让出导航栏 / 键盘高度。
     */
    private fun bottomOffset(anchor: View, context: Context, gap: Int): Int {
        val activity = context.findActivity()
        val isActivityWindow = activity != null && anchor.rootView === activity.window?.decorView
        if (isActivityWindow) {
            val navBar = activity!!.findViewById<View>(R.id.bottom_nav_view)
            if (navBar != null && navBar.isShown && navBar.height > 0) return navBar.height + gap
        }
        val insets = ViewCompat.getRootWindowInsets(anchor)
        val systemBars = insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
        val ime = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        return maxOf(systemBars, ime) + gap
    }

    private fun paletteFor(context: Context, kind: Kind): Palette = when (kind) {
        Kind.SUCCESS -> Palette(
            background = surface(context),
            stroke = context.getColor(R.color.color_outline_variant),
            text = onSurface(context),
            iconBg = context.getColor(R.color.color_secondary_container),
            iconFg = context.getColor(R.color.color_secondary)
        )
        Kind.ERROR -> Palette(
            background = surface(context),
            stroke = context.getColor(R.color.color_outline_variant),
            text = onSurface(context),
            iconBg = context.getColor(R.color.color_error_container),
            iconFg = context.getColor(R.color.color_error)
        )
        Kind.INFO -> Palette(
            background = surface(context),
            stroke = context.getColor(R.color.color_outline_variant),
            text = onSurface(context),
            iconBg = context.getColor(R.color.color_primary_container),
            iconFg = context.getColor(R.color.color_primary)
        )
    }

    private fun surface(context: Context): Int =
        MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, 0)

    private fun onSurface(context: Context): Int =
        MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, 0)

    @DrawableRes
    private fun Kind.iconRes(): Int = when (this) {
        Kind.SUCCESS -> R.drawable.ic_check
        Kind.ERROR -> R.drawable.ic_warning
        Kind.INFO -> R.drawable.ic_shield
    }

    private data class Palette(
        val background: Int,
        val stroke: Int,
        val text: Int,
        val iconBg: Int,
        val iconFg: Int
    )

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
