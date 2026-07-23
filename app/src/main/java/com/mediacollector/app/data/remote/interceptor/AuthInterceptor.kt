package com.mediacollector.app.data.remote.interceptor

import com.mediacollector.app.data.settings.AuthStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp 拦截器，自动为需要认证的请求添加 Authorization 头。
 *
 * DataStore 流需要协程，此处用 runBlocking 同步读取 Token。
 * Token 通常在内存中已缓存，不会阻塞 UI。
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authStore: AuthStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authStore.token.first() }
        val request = chain.request().newBuilder()

        // 非空 Token 自动注入 Authorization 头
        if (!token.isNullOrBlank()) {
            request.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(request.build())
    }
}
