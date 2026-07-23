package com.mediacollector.app.ui.download

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class DownloadItem(
    val mediaId: Int,
    val title: String,
    val url: String,
    val progress: Int = 0,
    val status: DownloadStatus = DownloadStatus.PENDING
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, COMPLETED, FAILED
}

data class DownloadUiState(
    val downloadingItems: List<DownloadItem> = emptyList(),
    val completedItems: List<DownloadItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class DownloadViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(DownloadUiState())
    val state: StateFlow<DownloadUiState> = _state.asStateFlow()

    fun startDownload(mediaId: Int, title: String, url: String) {
        val existing = _state.value.downloadingItems.find { it.mediaId == mediaId }
        if (existing != null && existing.status == DownloadStatus.DOWNLOADING) return

        val item = DownloadItem(mediaId, title, url, status = DownloadStatus.DOWNLOADING)
        _state.value = _state.value.copy(
            downloadingItems = _state.value.downloadingItems + item
        )

        // TODO: 启动 DownloadService
        // 实际调用: startForegroundService with DownloadService
    }

    fun cancelDownload(mediaId: Int) {
        _state.value = _state.value.copy(
            downloadingItems = _state.value.downloadingItems.filter { it.mediaId != mediaId }
        )
    }

    fun deleteDownloaded(mediaId: Int) {
        _state.value = _state.value.copy(
            completedItems = _state.value.completedItems.filter { it.mediaId != mediaId }
        )
    }
}
