package com.mediacollector.app.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * API 路由重写拦截器
 *
 * 将标准 Retrofit URL（如 /media-api/api/v1/media）重写为
 * route.php 查询参数格式（如 /media-api/route.php?_url=/api/v1/media）。
 *
 * 这样做的原因是服务器 Nginx 不支持 PHP PATH_INFO（.php/xxx 格式路径），
 * 改用查询参数方式传递 API 路径。
 */
class RouteRewriteInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // 不重写已经指向 route.php 的请求
        if (originalUrl.encodedPath.contains("route.php")) {
            return chain.proceed(originalRequest)
        }

        val encodedPath = originalUrl.encodedPath

        // 提取 /media-api/ 后面的 API 路径
        val apiPath = encodedPath.substringAfter("/media-api", "")
            .substringBefore("?")

        // 只重写 API 路径
        if (apiPath.isBlank() || !apiPath.startsWith("/api/")) {
            return chain.proceed(originalRequest)
        }

        // 构建新的 URL：/media-api/route.php?_url=/api/v1/xxx
        val newUrl = originalUrl.newBuilder()
            .encodedPath("/media-api/route.php")
            .addQueryParameter("_url", apiPath)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
