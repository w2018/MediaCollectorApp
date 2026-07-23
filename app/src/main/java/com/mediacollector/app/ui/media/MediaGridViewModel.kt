package com.mediacollector.app.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.remote.dto.MediaItem
import com.mediacollector.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaGridUiState(
    val items: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val filterType: String? = null // null = 全部
)

@HiltViewModel
class MediaGridViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MediaGridUiState())
    val state: StateFlow<MediaGridUiState> = _state.asStateFlow()

    init { loadMedia() }

    fun loadMedia() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = mediaRepository.getMediaList(
                page = 1,
                type = _state.value.filterType
            )
            result.fold(
                onSuccess = { data ->
                    _state.value = _state.value.copy(
                        items = data.items,
                        isLoading = false,
                        currentPage = 1,
                        totalPages = data.pagination.totalPages,
                        total = data.pagination.total
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

    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || current.currentPage >= current.totalPages) return

        viewModelScope.launch {
            _state.value = current.copy(isLoadingMore = true)
            val result = mediaRepository.getMediaList(
                page = current.currentPage + 1,
                type = current.filterType
            )
            result.fold(
                onSuccess = { data ->
                    _state.value = _state.value.copy(
                        items = _state.value.items + data.items,
                        isLoadingMore = false,
                        currentPage = data.pagination.page,
                        totalPages = data.pagination.totalPages,
                        total = data.pagination.total
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(isLoadingMore = false)
                }
            )
        }
    }

    fun setFilter(type: String?) {
        if (_state.value.filterType != type) {
            _state.value = _state.value.copy(filterType = type)
            loadMedia()
        }
    }

    fun refresh() { loadMedia() }
}
