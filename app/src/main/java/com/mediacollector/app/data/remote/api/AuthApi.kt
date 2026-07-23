package com.mediacollector.app.data.remote.api

import com.mediacollector.app.data.remote.dto.*
import retrofit2.http.*

/** 认证相关 API */
interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResult>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResult>

    @POST("api/v1/auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    @GET("api/v1/auth/me")
    suspend fun getMe(): ApiResponse<UserInfo>
}
