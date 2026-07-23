package com.mediacollector.app.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.remote.dto.MediaItem
import com.mediacollector.app.data.repository.FavoriteRepository
import com.mediacollector.app.data.repository.MediaRepository
import com.mediacollector.app.ui.photo.BrowsingContext
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
    val filterType: String? = null, // null = 全部
    val favorites: Map<Int, Boolean> = emptyMap() // mediaId -> isFavorite
)

@HiltViewModel
class MediaGridViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val favoriteRepository: FavoriteRepository
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
                        totalPages = data.pagination?.totalPages ?: 1,
                        total = data.pagination?.total ?: data.items.size
                    )
                    // 更新浏览上下文（供全屏查看器左右滑动导航）
                    BrowsingContext.mediaIds = data.items.map { it.id }
                    loadFavoriteStatus(data.items.map { it.id })
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
                    // 随机排序下分页可能有重复，按 id 去重
                    val existingIds = _state.value.items.map { it.id }.toSet()
                    val newItems = data.items.filter { it.id !in existingIds }
                    _state.value = _state.value.copy(
                        items = _state.value.items + newItems,
                        isLoadingMore = false,
                        currentPage = data.pagination?.page ?: 1,
                        totalPages = data.pagination?.totalPages ?: 1,
                        total = data.pagination?.total ?: _state.value.items.size
                    )
                    // 更新浏览上下文（包含所有已加载的 ID）
                    BrowsingContext.mediaIds = _state.value.items.map { it.id }
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

    /** 切换收藏状态 */
    fun toggleFavorite(mediaId: Int, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            // 立即更新 UI
            val newFav = _state.value.favorites.toMutableMap()
            newFav[mediaId] = !isCurrentlyFavorite
            _state.value = _state.value.copy(favorites = newFav)

            // 异步同步到服务器
            if (isCurrentlyFavorite) {
                favoriteRepository.removeFavorite(mediaId)
            } else {
                favoriteRepository.addFavorite(mediaId)
            }
        }
    }

    /** 加载收藏状态 */
    private suspend fun loadFavoriteStatus(mediaIds: List<Int>) {
        if (mediaIds.isEmpty()) return
        try {
            val result = favoriteRepository.checkFavorites(mediaIds)
            result.getOrNull()?.let { map ->
                val favorites = map.mapKeys { it.key.toIntOrNull() ?: return }
                _state.value = _state.value.copy(favorites = favorites)
            }
        } catch (_: Exception) { }
    }
}
