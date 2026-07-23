package com.mediacollector.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.local.dao.MediaDao
import com.mediacollector.app.data.repository.AuthRepository
import com.mediacollector.app.data.settings.AuthStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val username: String = "",
    val displayName: String = "",
    val userId: Int = 0,
    val favoriteCount: Int = 0,
    val historyCount: Int = 0,
    val createdAt: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authStore: AuthStore,
    private val mediaDao: MediaDao
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init { checkLoginStatus() }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val loggedIn = authStore.isLoggedIn()
            if (loggedIn) {
                val username = authStore.username.first()
                val displayName = authStore.displayName.first()
                val userId = authStore.userId.first()
                _state.value = _state.value.copy(
                    isLoggedIn = true,
                    username = username,
                    displayName = displayName,
                    userId = userId
                )
                // 顺便拉取最新用户信息
                loadUserInfo()
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = authRepository.login(username, password)
            result.fold(
                onSuccess = { authResult ->
                    authStore.saveAuth(
                        tokenStr = authResult.token,
                        userId = authResult.userId,
                        username = authResult.username,
                        displayName = authResult.displayName
                    )
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        username = authResult.username,
                        displayName = authResult.displayName,
                        userId = authResult.userId
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "登录失败"
                    )
                }
            )
        }
    }

    fun register(username: String, password: String, displayName: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = authRepository.register(username, password, displayName)
            result.fold(
                onSuccess = { authResult ->
                    authStore.saveAuth(
                        tokenStr = authResult.token,
                        userId = authResult.userId,
                        username = authResult.username,
                        displayName = authResult.displayName
                    )
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        username = authResult.username,
                        displayName = authResult.displayName,
                        userId = authResult.userId
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "注册失败"
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            // 先尝试调用服务端退出（忽略失败）
            authRepository.logout()
            // 清除本地认证信息
            authStore.clear()

            // 清空本地 Room 数据（NonCancellable 确保 ViewModel 销毁时不会被中断）
            withContext(NonCancellable) {
                try { mediaDao.clearAllFavorites() } catch (_: Exception) { }
                try { mediaDao.clearAllHistory() } catch (_: Exception) { }
                try { mediaDao.clearAllChatMessages() } catch (_: Exception) { }
            }

            _state.value = AuthUiState()
        }
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            authRepository.getMe().fold(
                onSuccess = { user ->
                    _state.value = _state.value.copy(
                        displayName = user.displayName,
                        favoriteCount = user.favoriteCount,
                        historyCount = user.historyCount,
                        createdAt = user.createdAt
                    )
                },
                onFailure = { /* 静默失败，使用缓存数据 */ }
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
