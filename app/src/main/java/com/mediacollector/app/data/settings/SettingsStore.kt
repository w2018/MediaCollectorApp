package com.mediacollector.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore 扩展，用于应用设置持久化 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** 缓存策略枚举 */
enum class CacheStrategy {
    WIFI_ONLY,   // 仅 WiFi 缓存
    ALWAYS,      // 始终缓存（流量下也缓存）
    DISABLED     // 不缓存
}

/** 深色模式枚举 */
enum class DarkMode {
    SYSTEM,      // 跟随系统
    LIGHT,       // 始终亮色
    DARK         // 始终深色
}

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_CACHE_STRATEGY = stringPreferencesKey("cache_strategy")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_PAGE_SIZE = intPreferencesKey("page_size")
        private val KEY_DOWNLOAD_ONLY_WIFI = booleanPreferencesKey("download_only_wifi")
    }

    // ── 服务器地址 ──
    val serverUrl: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL] ?: ""
    }

    suspend fun saveServerUrl(url: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = url
        }
    }

    // ── 缓存策略 ──
    val cacheStrategy: Flow<CacheStrategy> = context.settingsDataStore.data.map { prefs ->
        when (prefs[KEY_CACHE_STRATEGY]) {
            "always" -> CacheStrategy.ALWAYS
            "disabled" -> CacheStrategy.DISABLED
            else -> CacheStrategy.WIFI_ONLY
        }
    }

    suspend fun saveCacheStrategy(strategy: CacheStrategy) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_CACHE_STRATEGY] = when (strategy) {
                CacheStrategy.ALWAYS -> "always"
                CacheStrategy.DISABLED -> "disabled"
                CacheStrategy.WIFI_ONLY -> "wifi_only"
            }
        }
    }

    // ── 深色模式 ──
    val darkMode: Flow<DarkMode> = context.settingsDataStore.data.map { prefs ->
        when (prefs[KEY_DARK_MODE]) {
            "light" -> DarkMode.LIGHT
            "dark" -> DarkMode.DARK
            else -> DarkMode.SYSTEM
        }
    }

    suspend fun saveDarkMode(mode: DarkMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = when (mode) {
                DarkMode.SYSTEM -> "system"
                DarkMode.LIGHT -> "light"
                DarkMode.DARK -> "dark"
            }
        }
    }

    // ── 每页数量 ──
    val pageSize: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_PAGE_SIZE] ?: 20
    }

    suspend fun savePageSize(size: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_PAGE_SIZE] = size
        }
    }

    // ── 仅 WiFi 下载 ──
    val downloadOnlyWifi: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DOWNLOAD_ONLY_WIFI] ?: true
    }

    suspend fun saveDownloadOnlyWifi(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_DOWNLOAD_ONLY_WIFI] = value
        }
    }
}
