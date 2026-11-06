package com.passworddisk.app.cloud

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Query

interface CloudApiService {
    @POST("/api/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse>

    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse>

    @POST("/api/sync")
    suspend fun sync(@Body request: SyncRequest): Response<ApiResponse>

    @GET("/api/passwords")
    suspend fun getPasswords(@Query("user_id") userId: String): Response<ApiResponse>

    /**
     * 注销云端账号：校验用户名密码后，删除 users / passwords / password 三张表里的关联数据。
     * 见 API.md「3. 删除账号」，路径是连字符形式。
     *
     * 注意不能用 `@DELETE` + `@Body`：Retrofit 把 DELETE 视作「无请求体」的方法，
     * 编译期不报错，运行到第一次调用时才抛
     * `IllegalArgumentException: Non-body HTTP method cannot contain @Body`。
     * API.md 要求凭据放在 JSON 请求体里，所以这里用 `@HTTP(hasBody = true)` 显式声明可以带 body。
     */
    @HTTP(method = "DELETE", path = "/api/delete-account", hasBody = true)
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<ApiResponse>
}

data class DeleteAccountRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String,
    val backup: String? = null
)

data class SyncRequest(
    val username: String,
    val password: String
)

data class ApiResponse(
    val success: Boolean,
    val id: String? = null,
    val error: String? = null,
    val passwords: List<String>? = null,
    val backup: String? = null
)