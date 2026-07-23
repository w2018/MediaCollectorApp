package com.mediacollector.app.data.settings

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 服务器 URL 动态提供器
 *
 * 保存用户自定义的 API 服务器地址，提供同步访问方法供拦截器使用。
 * 支持用户输入任意格式的 URL，自动解析各组成部分。
 */
@Singleton
class ServerUrlProvider @Inject constructor(
    private val settingsStore: SettingsStore
) {
    companion object {
        private const val TAG = "ServerUrlProvider"
        private const val DEFAULT_URL = "http://localhost/media-api/"
    }

    @Volatile
    var scheme: String = "http"
        private set

    @Volatile
    var host: String = "192.168.1.100"
        private set

    @Volatile
    var port: Int = 80
        private set

    /** 用户 URL 中的路径部分（如 /media-api、/api/v1、空字符串等） */
    @Volatile
    var basePath: String = "/media-api"
        private set

    @Volatile
    var currentUrl: String = DEFAULT_URL
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            settingsStore.serverUrl.collect { savedUrl ->
                if (savedUrl.isNotBlank()) {
                    val normalized = if (!savedUrl.endsWith("/")) "$savedUrl/" else savedUrl
                    applyUrl(normalized)
                } else {
                    applyUrl(DEFAULT_URL)
                }
            }
        }
    }

    /**
     * 立即更新 URL（由 ViewModel 在保存时调用，不需等 DataStore 回写）
     */
    fun updateUrl(url: String) {
        val normalized = if (!url.endsWith("/")) "$url/" else url
        applyUrl(normalized)
    }

    /**
     * 获取默认 URL
     */
    fun getDefaultUrl(): String = DEFAULT_URL

    /**
     * 解析并应用 URL 的各部分
     */
    private fun applyUrl(url: String) {
        currentUrl = url
        try {
            val httpUrl = url.toHttpUrl()
            scheme = httpUrl.scheme
            host = httpUrl.host
            port = httpUrl.port

            // 提取路径部分，去掉末尾的 /
            val rawPath = httpUrl.encodedPath
            basePath = rawPath.trimEnd('/').let { if (it.isEmpty()) "" else it }

            Log.d(TAG, "URL 已更新: $scheme://$host:$port, basePath=$basePath")
        } catch (e: Exception) {
            Log.w(TAG, "URL 解析失败: ${e.message}, 使用默认值")
            scheme = "http"
            host = "192.168.1.100"
            port = 80
            basePath = "/media-api"
        }
    }
}
