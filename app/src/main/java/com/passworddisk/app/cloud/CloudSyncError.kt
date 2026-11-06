package com.passworddisk.app.cloud

sealed class CloudSyncError(val code: String) {
    object RegistrationFailed : CloudSyncError("REGISTRATION_FAILED")
    object LoginFailed : CloudSyncError("LOGIN_FAILED")
    object SyncFailed : CloudSyncError("SYNC_FAILED")
    object GetPasswordsFailed : CloudSyncError("GET_PASSWORDS_FAILED")
    object AccountDeletionFailed : CloudSyncError("ACCOUNT_DELETION_FAILED")

    /** 注册时服务端返回 409：用户名已被占用 */
    object UsernameTaken : CloudSyncError("USERNAME_TAKEN")

    /** 服务端返回 401：用户名不存在或密码错误 */
    object InvalidCredentials : CloudSyncError("INVALID_CREDENTIALS")

    /** 服务端返回 400：缺少必要参数 */
    object BadRequest : CloudSyncError("BAD_REQUEST")

    /** 服务端返回 5xx：数据库或 Worker 异常 */
    object ServerError : CloudSyncError("SERVER_ERROR")

    /** 连接超时（域名被劫持、网络受限时最常见） */
    object ConnectionTimeout : CloudSyncError("CONNECTION_TIMEOUT")

    /** 域名解析失败 / 端口不可达，通常是网络被墙或没有联网 */
    object Unreachable : CloudSyncError("UNREACHABLE")

    /** 注销账号时服务端返回 401：用户名不存在或密码错误 */
    object AccountAuthFailed : CloudSyncError("ACCOUNT_AUTH_FAILED")
    data class NetworkError(val statusCode: Int) : CloudSyncError("NETWORK_ERROR")
    data class UnknownError(val message: String) : CloudSyncError("UNKNOWN_ERROR")
}
