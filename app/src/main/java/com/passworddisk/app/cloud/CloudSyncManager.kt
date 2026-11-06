package com.passworddisk.app.cloud

import android.util.Log
import com.passworddisk.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object CloudSyncManager {

    private const val TAG = "CloudSync"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // 正式包绝不输出请求体：里面含主密码与备份密文
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(normalizedBaseUrl())
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: CloudApiService = retrofit.create(CloudApiService::class.java)

    /**
     * Retrofit 要求 baseUrl 以 `/` 结尾，否则相对路径拼接会丢掉最后一段。
     * 这里统一兜底，避免以后改地址时漏掉斜杠。
     */
    private fun normalizedBaseUrl(): String {
        val raw = BuildConfig.CLOUD_BASE_URL
        return if (raw.endsWith("/")) raw else "$raw/"
    }

    suspend fun register(username: String, password: String): Result<ApiResponse> {
        return runCall(
            businessError = CloudSyncError.RegistrationFailed,
            request = { apiService.register(RegisterRequest(username, password)) },
            conflictMeansTaken = true
        )
    }

    suspend fun login(username: String, password: String, backup: String? = null): Result<ApiResponse> {
        return runCall(
            businessError = CloudSyncError.LoginFailed,
            request = { apiService.login(LoginRequest(username, password, backup)) }
        )
    }

    suspend fun sync(username: String, password: String): Result<ApiResponse> {
        return runCall(
            businessError = CloudSyncError.SyncFailed,
            request = { apiService.sync(SyncRequest(username, password)) }
        )
    }

    suspend fun getCloudPasswords(userId: String): Result<ApiResponse> {
        return runCall(
            businessError = CloudSyncError.GetPasswordsFailed,
            request = { apiService.getPasswords(userId) }
        )
    }

    /**
     * 注销云端账号并删除服务端备份。
     * Google Play 要求提供「允许创建账号的应用必须支持在应用内删除账号」。
     *
     * 服务端返回码见 API.md：400 缺少参数、401 用户名不存在或密码错误、500 数据库异常。
     */
    suspend fun deleteAccount(username: String, password: String): Result<ApiResponse> {
        return runCall(
            businessError = CloudSyncError.AccountDeletionFailed,
            request = { apiService.deleteAccount(DeleteAccountRequest(username, password)) },
            authMeansAccountAuthFailed = true
        )
    }

    /**
     * 统一处理一次云端请求：
     * - 2xx 且 `success=true` → 成功；
     * - 2xx 但 `success=false` → 业务失败（服务端已明确告知失败原因）；
     * - 非 2xx → 按状态码区分「用户名被占用 / 凭据错误 / 参数缺失 / 服务端异常 / 其他」；
     * - 抛异常 → 按异常类型区分「超时 / 域名不可达 / 未知」。
     *
     * 之前所有非 2xx 都落到「网络错误：<code>」，用户只会看到「网络错误：409」这种
     * 既不是网络问题、也没告诉他怎么办的提示。
     */
    private suspend fun runCall(
        businessError: CloudSyncError,
        request: suspend () -> Response<ApiResponse>,
        conflictMeansTaken: Boolean = false,
        authMeansAccountAuthFailed: Boolean = false
    ): Result<ApiResponse> {
        return try {
            val response = request()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Result.success(body)
                } else {
                    Result.failure(CloudSyncErrorException(businessError, body?.error))
                }
            } else {
                val rawBody = response.errorBody()?.string()
                Log.w(TAG, "HTTP ${response.code()} ${response.raw().request.url}: $rawBody")
                Result.failure(
                    CloudSyncErrorException(
                        mapHttpError(
                            code = response.code(),
                            conflictMeansTaken = conflictMeansTaken,
                            authMeansAccountAuthFailed = authMeansAccountAuthFailed
                        ),
                        rawBody
                    )
                )
            }
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "请求超时", e)
            Result.failure(CloudSyncErrorException(CloudSyncError.ConnectionTimeout, e.message))
        } catch (e: UnknownHostException) {
            // 域名被 DNS 污染 / 被墙时通常在这里失败
            Log.w(TAG, "域名解析失败", e)
            Result.failure(CloudSyncErrorException(CloudSyncError.Unreachable, e.message))
        } catch (e: ConnectException) {
            Log.w(TAG, "无法建立连接", e)
            Result.failure(CloudSyncErrorException(CloudSyncError.Unreachable, e.message))
        } catch (e: IOException) {
            Log.w(TAG, "网络异常", e)
            Result.failure(CloudSyncErrorException(CloudSyncError.Unreachable, e.message))
        } catch (e: Exception) {
            Log.w(TAG, "未知异常", e)
            Result.failure(
                CloudSyncErrorException(CloudSyncError.UnknownError(e.message ?: "Unknown error"))
            )
        }
    }

    private fun mapHttpError(
        code: Int,
        conflictMeansTaken: Boolean,
        authMeansAccountAuthFailed: Boolean
    ): CloudSyncError = when {
        code == 409 && conflictMeansTaken -> CloudSyncError.UsernameTaken
        code == 401 && authMeansAccountAuthFailed -> CloudSyncError.AccountAuthFailed
        code == 401 -> CloudSyncError.InvalidCredentials
        code == 400 -> CloudSyncError.BadRequest
        code in 500..599 -> CloudSyncError.ServerError
        else -> CloudSyncError.NetworkError(code)
    }
}

class CloudSyncErrorException(
    val error: CloudSyncError,
    val serverMessage: String? = null
) : Exception(serverMessage ?: error.code)
