package com.passworddisk.app.cloud

import android.content.Context

/**
 * 本机记住的云端账号：用户名 + 最近一次成功同步的时间戳。
 *
 * 只记用户名与时间，绝不存密码——密码每次操作时由用户输入。
 * 用 SharedPreferences 而不是 Room：清空密码库（wipeAll）时这份记录要不要一起删，
 * 得由删除流程单独决定，不该被数据库清理顺带处理掉。
 */
object CloudAccountStore {

    private const val PREFS_NAME = "cloud_account"
    private const val KEY_USERNAME = "username"
    private const val KEY_LAST_SYNC_AT = "last_sync_at"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun username(context: Context): String? =
        prefs(context).getString(KEY_USERNAME, null)?.takeIf { it.isNotBlank() }

    fun lastSyncAt(context: Context): Long = prefs(context).getLong(KEY_LAST_SYNC_AT, 0L)

    fun hasAccount(context: Context): Boolean = username(context) != null

    /** 注册成功或首次登录成功后记住账号，此时还没有发生过同步 */
    fun remember(context: Context, username: String) {
        val name = username.trim()
        if (name.isEmpty()) return
        prefs(context).edit().putString(KEY_USERNAME, name).apply()
    }

    /** 同步（上传备份或拉取恢复）成功后更新最近同步时间 */
    fun markSynced(context: Context, username: String) {
        val name = username.trim()
        if (name.isEmpty()) return
        prefs(context).edit()
            .putString(KEY_USERNAME, name)
            .putLong(KEY_LAST_SYNC_AT, System.currentTimeMillis())
            .apply()
    }

    /** 云端账号删除成功后调用，避免后续仍提示「已登录 xxx」 */
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
