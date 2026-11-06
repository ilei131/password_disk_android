package com.passworddisk.app.ui

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.passworddisk.app.R

/** 轻量 UI 工具：主题取色、头像字母、掩码等。 */
object Ui {

    /**
     * 给底部面板 / 全屏页面补上导航栏（手势条）高度的内边距，
     * 避免最后一行内容被系统手势区压住。
     */
    fun applyNavBarInset(view: View, extraBottomDp: Int = 0) {
        val extra = (view.resources.displayMetrics.density * extraBottomDp).toInt()
        val initialPadding = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = initialPadding + bottom + extra)
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun themeColor(context: Context, @AttrRes attr: Int, fallback: Int = 0): Int {
        val value = TypedValue()
        if (!context.theme.resolveAttribute(attr, value, true)) return fallback
        return if (value.resourceId != 0) ContextCompat.getColor(context, value.resourceId) else value.data
    }

    @ColorRes
    fun avatarTintFor(seed: String): Int {
        val palette = intArrayOf(
            R.color.avatar_tint_1,
            R.color.avatar_tint_2,
            R.color.avatar_tint_3,
            R.color.avatar_tint_4
        )
        val index = (seed.hashCode().let { if (it == Int.MIN_VALUE) 0 else it } and Int.MAX_VALUE) % palette.size
        return palette[index]
    }

    fun initialOf(value: String?, fallback: String = "?"): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return fallback
        return trimmed.substring(0, 1).uppercase()
    }

    fun maskOf(length: Int = 8): String = "•".repeat(length.coerceIn(4, 16))

    /**
     * 键盘弹出导致可视区域变小时，把当前光标所在的输入框滚回可见范围内，
     * 避免表单底部字段被键盘遮挡。
     */
    fun keepFocusedFieldVisible(scroll: ScrollView, container: ViewGroup) {
        var lastHeight = 0
        scroll.viewTreeObserver.addOnGlobalLayoutListener {
            val height = scroll.height
            if (height <= 0 || height == lastHeight) return@addOnGlobalLayoutListener
            lastHeight = height

            val focused = scroll.findFocus() ?: return@addOnGlobalLayoutListener
            val rect = Rect().also { focused.getDrawingRect(it) }
            container.offsetDescendantRectToMyCoords(focused, rect)

            val margin = (16 * scroll.resources.displayMetrics.density).toInt()
            if (rect.bottom > height - margin || rect.top < margin) {
                scroll.smoothScrollTo(0, (rect.top - margin).coerceAtLeast(0))
            }
        }
    }
}

/** 分类图标：把历史 emoji 数据映射到矢量图标，新数据直接存 key。 */
object CategoryIcons {

    val keys = listOf(
        "folder", "person", "work", "finance", "social", "home",
        "lock", "mail", "web", "media", "tool", "key"
    )

    private val legacy = mapOf(
        "📁" to "folder", "👤" to "person", "💼" to "work", "💰" to "finance",
        "📱" to "social", "🏠" to "home", "🔒" to "lock", "📧" to "mail",
        "🌐" to "web", "🎮" to "media", "📚" to "media", "🎨" to "media",
        "🔧" to "tool", "🚗" to "tool", "✈️" to "web", "⚡" to "key"
    )

    fun keyOf(raw: String?): String {
        if (raw.isNullOrBlank()) return "folder"
        if (keys.contains(raw)) return raw
        return legacy[raw] ?: "folder"
    }

    @DrawableRes
    fun drawable(key: String): Int = when (keyOf(key)) {
        "person" -> R.drawable.ic_cat_person
        "work" -> R.drawable.ic_cat_work
        "finance" -> R.drawable.ic_cat_finance
        "social" -> R.drawable.ic_cat_social
        "home" -> R.drawable.ic_cat_home
        "lock" -> R.drawable.ic_cat_lock
        "mail" -> R.drawable.ic_cat_mail
        "web" -> R.drawable.ic_cat_web
        "media" -> R.drawable.ic_cat_media
        "tool" -> R.drawable.ic_cat_tool
        "key" -> R.drawable.ic_cat_key
        else -> R.drawable.ic_cat_folder
    }
}
