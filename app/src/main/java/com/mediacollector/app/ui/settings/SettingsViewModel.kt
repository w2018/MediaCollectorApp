package com.mediacollector.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.settings.CacheStrategy
import com.mediacollector.app.data.settings.DarkMode
import com.mediacollector.app.data.settings.ServerUrlProvider
import com.mediacollector.app.data.settings.SettingsStore
import com.mediacollector.app.domain.ApiCompatChecker
import com.mediacollector.app.domain.CacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val serverUrl: String = "",
    val cacheStrategy: CacheStrategy = CacheStrategy.WIFI_ONLY,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val pageSize: Int = 20,
    val downloadOnlyWifi: Boolean = true
)

data class CacheUiState(
    val cacheInfo: CacheManager.CacheInfo = CacheManager.CacheInfo(),
    val isClearing: Boolean = false,
    val clearedMessage: String? = null
)

data class ApiCheckUiState(
    val isLoading: Boolean = false,
    val result: ApiCompatChecker.ApiCheckResult? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val serverUrlProvider: ServerUrlProvider,
    private val cacheManager: CacheManager,
    private val apiCompatChecker: ApiCompatChecker
) : ViewModel() {

    // ── 设置状态 ──
    val settingsState: StateFlow<SettingsUiState> = combine(
        settingsStore.serverUrl,
        settingsStore.cacheStrategy,
        settingsStore.darkMode,
        settingsStore.pageSize,
        settingsStore.downloadOnlyWifi
    ) { url, strategy, mode, size, wifi ->
        SettingsUiState(url, strategy, mode, size, wifi)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    // ── 缓存状态 ──
    private val _cacheState = MutableStateFlow(CacheUiState())
    val cacheState: StateFlow<CacheUiState> = _cacheState.asStateFlow()

    // ── API 检测状态 ──
    private val _apiCheckState = MutableStateFlow(ApiCheckUiState())
    val apiCheckState: StateFlow<ApiCheckUiState> = _apiCheckState.asStateFlow()

    init { refreshCache() }

    /**
     * 保存服务器 URL，同时立即更新内存缓存（拦截器即刻生效）
     */
    fun saveServerUrl(url: String) {
        serverUrlProvider.updateUrl(url) // 立即生效
        viewModelScope.launch { settingsStore.saveServerUrl(url) } // 持久化
    }

    fun saveCacheStrategy(strategy: CacheStrategy) {
        viewModelScope.launch { settingsStore.saveCacheStrategy(strategy) }
    }

    fun saveDarkMode(mode: DarkMode) {
        viewModelScope.launch { settingsStore.saveDarkMode(mode) }
    }

    fun savePageSize(size: Int) {
        viewModelScope.launch { settingsStore.savePageSize(size) }
    }

    fun saveDownloadOnlyWifi(value: Boolean) {
        viewModelScope.launch { settingsStore.saveDownloadOnlyWifi(value) }
    }

    // ── 缓存管理 ──

    fun refreshCache() {
        val info = cacheManager.getCacheInfo()
        _cacheState.value = _cacheState.value.copy(cacheInfo = info)
    }

    fun clearAllCache() {
        viewModelScope.launch {
            _cacheState.value = _cacheState.value.copy(isClearing = true)
            cacheManager.clearAllCache()
            refreshCache()
            _cacheState.value = _cacheState.value.copy(
                isClearing = false,
                clearedMessage = "缓存已清理"
            )
        }
    }

    fun clearClearedMessage() {
        _cacheState.value = _cacheState.value.copy(clearedMessage = null)
    }

    // ── API 检测 ──

    fun checkApi() {
        viewModelScope.launch {
            _apiCheckState.value = ApiCheckUiState(isLoading = true)
            val result = apiCompatChecker.check()
            _apiCheckState.value = ApiCheckUiState(result = result)
        }
    }
}
