package com.mediacollector.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.repository.AuthRepository
import com.mediacollector.app.data.settings.AuthStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val authStore: AuthStore
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
            authRepository.logout()
            authStore.clear()
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
