package com.passworddisk.app.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.view.WindowManager
import com.passworddisk.app.BuildConfig

/**
 * 密码类应用的安全相关 UI 处理：
 * - 正式包窗口禁止截屏 / 录屏 / 在最近任务中生成缩略图；
 * - 复制到剪贴板的密码标记为敏感内容，并在超时后自动清空。
 */
object SecureUi {

    private const val CLIPBOARD_CLEAR_DELAY_MS = 60_000L

    /** 在 Activity.onCreate 中调用，正式包生效。 */
    fun protectScreen(activity: Activity) {
        if (!BuildConfig.ENABLE_SECURE_WINDOW) return
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 最近任务列表里也不生成缩略图
            activity.setRecentsScreenshotEnabled(false)
        }
    }

    /**
     * 复制敏感文本到剪贴板。
     * Android 13+ 会向输入法等读取方标记为敏感内容，不会进入剪贴板历史预览；
     * 60 秒后若剪贴板内容仍是这段文本，则自动清空。
     */
    fun copySensitive(
        context: Context,
        label: String,
        value: String,
        clearAfterMs: Long = CLIPBOARD_CLEAR_DELAY_MS
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        val clip = ClipData.newPlainText(label, value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)

        Handler(Looper.getMainLooper()).postDelayed({
            val current = clipboard.primaryClip
            val stillTheSame = current != null &&
                current.itemCount > 0 &&
                current.getItemAt(0).text?.toString() == value
            if (stillTheSame) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        }, clearAfterMs)
    }
}
