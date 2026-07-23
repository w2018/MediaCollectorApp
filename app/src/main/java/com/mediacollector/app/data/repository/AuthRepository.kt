package com.mediacollector.app.data.repository

import com.mediacollector.app.data.remote.api.AuthApi
import com.mediacollector.app.data.remote.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi
) {
    suspend fun register(
        username: String,
        password: String,
        displayName: String = "",
        email: String = ""
    ): Result<AuthResult> = runCatching {
        val response = authApi.register(RegisterRequest(username, password, displayName, email))
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    suspend fun login(username: String, password: String): Result<AuthResult> = runCatching {
        val response = authApi.login(LoginRequest(username, password))
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    suspend fun logout(): Result<Unit> = runCatching {
        val response = authApi.logout()
        if (!response.success) throw Exception(response.message)
    }

    suspend fun getMe(): Result<UserInfo> = runCatching {
        val response = authApi.getMe()
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }
}
