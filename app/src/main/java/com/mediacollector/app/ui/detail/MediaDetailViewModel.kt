package com.mediacollector.app.ui.detail

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.remote.dto.MediaDetail
import com.mediacollector.app.data.repository.FavoriteRepository
import com.mediacollector.app.data.repository.MediaRepository
import com.mediacollector.app.ui.download.DownloadService
import com.mediacollector.app.ui.download.DownloadTracker
import com.mediacollector.app.ui.share.ShareHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MediaDetailUiState(
    val detail: MediaDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isFavorite: Boolean = false
)

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val favoriteRepository: FavoriteRepository,
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(MediaDetailUiState())
    val state: StateFlow<MediaDetailUiState> = _state.asStateFlow()

    fun loadMedia(id: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            mediaRepository.getMediaDetail(id).fold(
                onSuccess = {
                    _state.value = _state.value.copy(detail = it, isLoading = false)
                    checkFavorite(id)
                },
                onFailure = { _state.value = _state.value.copy(error = it.message, isLoading = false) }
            )
        }
    }

    private suspend fun checkFavorite(mediaId: Int) {
        try {
            val result = favoriteRepository.checkFavorites(listOf(mediaId))
            val map = result.getOrNull() ?: return
            _state.value = _state.value.copy(isFavorite = map[mediaId.toString()] ?: false)
        } catch (_: Exception) { }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val mediaId = _state.value.detail?.id ?: return@launch
            if (_state.value.isFavorite) {
                favoriteRepository.removeFavorite(mediaId)
            } else {
                favoriteRepository.addFavorite(mediaId)
            }
            _state.value = _state.value.copy(isFavorite = !_state.value.isFavorite)
        }
    }

    fun shareImage() {
        val imageUrl = _state.value.detail?.url ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ShareHelper.shareImage(application, imageUrl)
            }
        }
    }

    fun downloadMedia() {
        val detail = _state.value.detail ?: return
        val url = detail.url
        if (url.isEmpty()) return
        val title = detail.title.ifEmpty { "download_${detail.id}" }

        // 加入下载跟踪
        DownloadTracker.addDownload(detail.id, title, url)

        val intent = Intent(application, DownloadService::class.java).apply {
            action = DownloadService.ACTION_DOWNLOAD
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_FILENAME, title.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(80))
            putExtra(DownloadService.EXTRA_MEDIA_ID, detail.id)
        }
        application.startForegroundService(intent)
    }
}
