package com.mediacollector.app.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.remote.dto.MediaDetail
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
    val error: String? = null
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
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
}
