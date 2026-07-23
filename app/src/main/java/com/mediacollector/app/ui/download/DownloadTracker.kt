package com.mediacollector.app.ui.download

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局下载跟踪器 — 跨屏幕共享下载任务状态。
 *
 * VideoPlayerScreen 启动下载时写入，DownloadManageScreen 读取展示。
 */
object DownloadTracker {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    fun addDownload(mediaId: Int, title: String, url: String) {
        val current = _downloads.value.toMutableList()
        // 去重
        if (current.any { it.mediaId == mediaId }) return
        current.add(DownloadItem(mediaId, title, url, status = DownloadStatus.DOWNLOADING))
        _downloads.value = current
    }

    fun updateProgress(mediaId: Int, progress: Int) {
        val current = _downloads.value.toMutableList()
        val idx = current.indexOfFirst { it.mediaId == mediaId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(progress = progress)
            _downloads.value = current
        }
    }

    fun markCompleted(mediaId: Int) {
        val current = _downloads.value.toMutableList()
        val idx = current.indexOfFirst { it.mediaId == mediaId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(status = DownloadStatus.COMPLETED, progress = 100)
            _downloads.value = current
        }
    }

    fun markFailed(mediaId: Int) {
        val current = _downloads.value.toMutableList()
        val idx = current.indexOfFirst { it.mediaId == mediaId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(status = DownloadStatus.FAILED)
            _downloads.value = current
        }
    }

    fun removeDownload(mediaId: Int) {
        _downloads.value = _downloads.value.filter { it.mediaId != mediaId }
    }
}
