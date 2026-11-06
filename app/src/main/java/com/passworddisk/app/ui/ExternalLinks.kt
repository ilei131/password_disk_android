package com.passworddisk.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** 统一打开外部链接（隐私政策、服务条款），失败时返回 false 由调用方提示。 */
object ExternalLinks {

    fun open(context: Context, url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
