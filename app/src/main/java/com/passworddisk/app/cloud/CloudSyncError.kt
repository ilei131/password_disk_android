package com.passworddisk.app.cloud

sealed class CloudSyncError(val code: String) {
    object RegistrationFailed : CloudSyncError("REGISTRATION_FAILED")
    object LoginFailed : CloudSyncError("LOGIN_FAILED")
    object SyncFailed : CloudSyncError("SYNC_FAILED")
    object GetPasswordsFailed : CloudSyncError("GET_PASSWORDS_FAILED")
    data class NetworkError(val statusCode: Int) : CloudSyncError("NETWORK_ERROR")
    data class UnknownError(val message: String) : CloudSyncError("UNKNOWN_ERROR")
}
