package com.passworddisk.app.cloud

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
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
}

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