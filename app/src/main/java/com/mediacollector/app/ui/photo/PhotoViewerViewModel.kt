package com.mediacollector.app.ui.photo

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

data class PhotoViewerUiState(
    val mediaDetail: MediaDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showControls: Boolean = true
)

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PhotoViewerUiState())
    val state: StateFlow<PhotoViewerUiState> = _state.asStateFlow()

    fun loadMedia(mediaId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = mediaRepository.getMediaDetail(mediaId)
            result.fold(
                onSuccess = { detail ->
                    _state.value = _state.value.copy(
                        mediaDetail = detail,
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

    fun toggleControls() {
        _state.value = _state.value.copy(
            showControls = !_state.value.showControls
        )
    }
}
