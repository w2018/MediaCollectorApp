package com.mediacollector.app.ui.photo

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.remote.dto.MediaDetail
import com.mediacollector.app.data.repository.FavoriteRepository
import com.mediacollector.app.data.repository.HistoryRepository
import com.mediacollector.app.data.repository.MediaRepository
import com.mediacollector.app.ui.share.ShareHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PhotoViewerUiState(
    val mediaDetail: MediaDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showControls: Boolean = true,
    val isFavorite: Boolean = false,
    val currentIndex: Int = 0,
    val totalCount: Int = 0
)

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository,
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(PhotoViewerUiState())
    val state: StateFlow<PhotoViewerUiState> = _state.asStateFlow()

    /** URL 缓存（mediaId -> url），供 HorizontalPager 各页直接使用 */
    private val urlCache = mutableMapOf<Int, String>()

    /** 当前媒体列表（从 BrowsingContext 读取，由 MediaGridScreen 维护） */
    private val mediaIds: List<Int> get() = BrowsingContext.mediaIds

    /** 供 HorizontalPager 获取总页数 */
    fun getMediaIdList(): List<Int> = mediaIds

    /** 供 HorizontalPager 各页获取图片 URL */
    fun getUrlForMedia(mediaId: Int): String = urlCache[mediaId] ?: ""

    /** 供 AsyncImage 获取 Application Context */
    fun getApplicationContext(): Context = application

    fun loadMedia(mediaId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = mediaRepository.getMediaDetail(mediaId)
            result.fold(
                onSuccess = { detail ->
                    val idx = mediaIds.indexOf(mediaId)
                    // 缓存 URL 供 pager 各页使用
                    urlCache[mediaId] = detail.url
                    _state.value = _state.value.copy(
                        mediaDetail = detail,
                        isLoading = false,
                        currentIndex = if (idx >= 0) idx else 0,
                        totalCount = mediaIds.size
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

    /** 记录浏览历史 */
    private fun recordHistory(mediaId: Int, type: String) {
        viewModelScope.launch {
            try {
                historyRepository.addHistory(mediaId, type)
            } catch (_: Exception) { }
        }
    }

    private suspend fun checkFavoriteStatus(mediaId: Int) {
        try {
            val result = favoriteRepository.checkFavorites(listOf(mediaId))
            val map = result.getOrNull() ?: return
            _state.value = _state.value.copy(isFavorite = map[mediaId.toString()] ?: false)
        } catch (_: Exception) { }
    }

    fun navigateTo(id: Int) {
        if (id > 0) loadMedia(id)
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

    fun shareImage() {
        val imageUrl = _state.value.mediaDetail?.url ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ShareHelper.shareImage(application, imageUrl)
            }
        }
    }

    fun toggleControls() {
        _state.value = _state.value.copy(
            showControls = !_state.value.showControls
        )
    }
}
