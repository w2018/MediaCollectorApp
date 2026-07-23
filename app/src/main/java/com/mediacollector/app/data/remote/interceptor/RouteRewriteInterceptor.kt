package com.mediacollector.app.data.remote.interceptor

import android.util.Log
import com.mediacollector.app.data.settings.ServerUrlProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * API 路由重写拦截器
 *
 * 将标准 Retrofit URL（如 /media-api/api/v1/media）重写为
 * route.php 查询参数格式（如 /media-api/route.php?_url=/api/v1/media）。
 *
 * basePath 来自用户自定义的 API 服务器地址，支持任意路径配置。
 */
class RouteRewriteInterceptor @Inject constructor(
    private val serverUrlProvider: ServerUrlProvider
) : Interceptor {

    companion object {
        private const val TAG = "NET_RouteRewrite"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        val method = originalRequest.method

        Log.d(TAG, "▶ 原始请求: $method $originalUrl")

        // 不重写已经指向 route.php 的请求
        if (originalUrl.encodedPath.contains("route.php")) {
            Log.d(TAG, "  → 已含 route.php，跳过重写")
            return chain.proceed(originalRequest)
        }

        val basePath = serverUrlProvider.basePath
        if (basePath.isEmpty()) {
            Log.d(TAG, "  → basePath 为空，跳过重写")
            return chain.proceed(originalRequest)
        }

        val encodedPath = originalUrl.encodedPath

        // 提取 basePath 后面的 API 路径
        val apiPath = encodedPath.substringAfter(basePath, "")
            .substringBefore("?")

        Log.d(TAG, "  basePath=$basePath, encodedPath=$encodedPath, apiPath=$apiPath")

        // 只重写 API 路径
        if (apiPath.isBlank() || !apiPath.startsWith("/api/")) {
            Log.d(TAG, "  → 非 API 路径，跳过重写")
            return chain.proceed(originalRequest)
        }

        // 构建新的 URL：{basePath}/route.php?_url=/api/v1/xxx
        val urlBuilder = originalUrl.newBuilder()
            .encodedPath("${basePath}/route.php")
            .addQueryParameter("_url", apiPath)

        // 保留原始查询参数（如 keyword=test, page=1 等）
        for (param in originalUrl.queryParameterNames) {
            if (param == "_url") continue
            for (value in originalUrl.queryParameterValues(param)) {
                urlBuilder.addQueryParameter(param, value)
            }
        }

        val newUrl = urlBuilder.build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        Log.d(TAG, "  ✓ 重写为: $newUrl")
        return chain.proceed(newRequest)
    }
}
