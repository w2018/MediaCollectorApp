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
 * 异步从 DataStore 加载已保存的 URL，缓存到内存中，
 * 提供同步访问方法供拦截器使用。
 * 修改 URL 后立即生效，无需重启应用。
 */
@Singleton
class ServerUrlProvider @Inject constructor(
    private val settingsStore: SettingsStore
) {
    companion object {
        private const val TAG = "ServerUrlProvider"
        private const val DEFAULT_URL = "http://192.168.1.100/media-api/"
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

    @Volatile
    var currentUrl: String = DEFAULT_URL
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // 异步监听 DataStore 变化，及时更新缓存
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
            Log.d(TAG, "URL 已更新: $scheme://$host:$port")
        } catch (e: Exception) {
            Log.w(TAG, "URL 解析失败: ${e.message}, 使用默认值")
            scheme = "http"
            host = "192.168.1.100"
            port = 80
        }
    }
}
