package com.mediacollector.app.data.remote.interceptor

import com.mediacollector.app.data.settings.ServerUrlProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 服务器 URL 动态重写拦截器
 *
 * 将 Retrofit 请求中的主机部分替换为 DataStore 中保存的服务器 URL。
 * 这样修改 API 服务器地址后立即生效，无需重启应用。
 *
 * 职责：只修改主机（scheme + host + port），不修改路径。
 * 路径重写由 [RouteRewriteInterceptor] 处理。
 */
@Singleton
class ServerUrlInterceptor @Inject constructor(
    private val serverUrlProvider: ServerUrlProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val originalUrl = original.url

        // 如果主机已匹配，无需重写
        if (originalUrl.host == serverUrlProvider.host &&
            originalUrl.port == serverUrlProvider.port &&
            originalUrl.scheme == serverUrlProvider.scheme
        ) {
            return chain.proceed(original)
        }

        // 只替换主机部分，路径和查询参数保持不变
        val newUrl = originalUrl.newBuilder()
            .scheme(serverUrlProvider.scheme)
            .host(serverUrlProvider.host)
            .port(serverUrlProvider.port)
            .build()

        val newRequest = original.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
