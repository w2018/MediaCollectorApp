package com.mediacollector.app.ui.download

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class DownloadViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    /** 从全局 DownloadTracker 获取实时下载状态 */
    val state: StateFlow<List<DownloadItem>> = DownloadTracker.downloads

    fun startDownload(mediaId: Int, title: String, url: String) {
        // 避免重复下载
        val current = DownloadTracker.downloads.value
        if (current.any { it.mediaId == mediaId }) return

        // 写入全局跟踪器
        DownloadTracker.addDownload(mediaId, title, url)

        // 启动前台下载服务
        val intent = Intent(application, DownloadService::class.java).apply {
            action = DownloadService.ACTION_DOWNLOAD
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_FILENAME, sanitizeFileName(title))
            putExtra(DownloadService.EXTRA_MEDIA_ID, mediaId)
        }
        application.startForegroundService(intent)
    }

    fun cancelDownload(mediaId: Int) {
        DownloadTracker.removeDownload(mediaId)
    }

    fun deleteDownloaded(mediaId: Int) {
        DownloadTracker.removeDownload(mediaId)
    }

    private fun sanitizeFileName(title: String): String {
        return title.replace(Regex("[/\\\\:*?\"<>|]"), "_")
            .take(80)
            .ifEmpty { "download_${System.currentTimeMillis()}" }
    }
}
