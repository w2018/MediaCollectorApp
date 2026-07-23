package com.mediacollector.app.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.remote.dto.MediaDetail
import com.mediacollector.app.data.repository.FavoriteRepository
import com.mediacollector.app.data.repository.HistoryRepository
import com.mediacollector.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoPlayerUiState(
    val mediaDetail: MediaDetail? = null,
    val videoUrl: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val isFavorite: Boolean = false
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VideoPlayerUiState())
    val state: StateFlow<VideoPlayerUiState> = _state.asStateFlow()

    fun loadMedia(mediaId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = mediaRepository.getMediaDetail(mediaId)
            result.fold(
                onSuccess = { detail ->
                    _state.value = _state.value.copy(
                        mediaDetail = detail,
                        videoUrl = detail.url,
                        isLoading = false
                    )
                    // 检查收藏状态
                    checkFavoriteStatus(mediaId)
                    // 记录浏览历史
                    recordHistory(mediaId, detail.type)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            )
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val detail = _state.value.mediaDetail ?: return@launch
            if (_state.value.isFavorite) {
                favoriteRepository.removeFavorite(detail.id)
            } else {
                favoriteRepository.addFavorite(detail.id)
            }
            _state.value = _state.value.copy(isFavorite = !_state.value.isFavorite)
        }
    }

    private suspend fun checkFavoriteStatus(mediaId: Int) {
        try {
            val result = favoriteRepository.checkFavorites(listOf(mediaId))
            val map = result.getOrNull() ?: return
            _state.value = _state.value.copy(isFavorite = map[mediaId.toString()] ?: false)
        } catch (_: Exception) { }
    }

    /** 记录浏览历史 */
    private fun recordHistory(mediaId: Int, type: String) {
        viewModelScope.launch {
            try {
                historyRepository.addHistory(mediaId, type)
            } catch (_: Exception) { }
        }
    }
}
