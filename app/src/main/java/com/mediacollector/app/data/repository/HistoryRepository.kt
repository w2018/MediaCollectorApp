package com.mediacollector.app.data.repository

import com.mediacollector.app.data.local.dao.MediaDao
import com.mediacollector.app.data.local.entity.LocalHistory
import com.mediacollector.app.data.remote.api.UserApi
import com.mediacollector.app.data.remote.dto.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val userApi: UserApi,
    private val mediaDao: MediaDao
) {
    /** 本地历史流 */
    val localHistory: Flow<List<LocalHistory>> = mediaDao.getLocalHistory()

    /** 从服务器拉取历史 */
    suspend fun getRemoteHistory(page: Int = 1, pageSize: Int = 100): Result<PaginatedData<HistoryRecord>> =
        runCatching {
            val response = userApi.getHistory(page, pageSize)
            if (response.success && response.data != null) response.data
            else throw Exception(response.message)
        }

    /** 添加浏览记录 */
    suspend fun addHistory(mediaId: Int, mediaType: String = "photo", watchPosition: Int = 0) {
        mediaDao.insertHistory(
            LocalHistory(
                media_id = mediaId,
                media_type = mediaType,
                watch_position = watchPosition,
                synced = false
            )
        )
        // 同步到服务器
        runCatching {
            userApi.addHistory(HistoryPostRequest(mediaId, mediaType, watchPosition))
            mediaDao.markHistorySynced(mediaId)
        }
    }

    /** 删除单条历史 */
    suspend fun removeHistory(mediaId: Int) {
        mediaDao.deleteHistoryByMediaId(mediaId)
        runCatching { userApi.removeHistory(mediaId) }
    }

    /** 清空全部历史 */
    suspend fun clearAll() {
        mediaDao.clearAllHistory()
        runCatching { userApi.clearHistory() }
    }

    /** 同步本地未同步历史到服务器 */
    suspend fun syncPending() {
        mediaDao.getUnsyncedHistory().forEach { h ->
            runCatching {
                userApi.addHistory(HistoryPostRequest(h.media_id, h.media_type, h.watch_position))
                mediaDao.markHistorySynced(h.media_id)
            }
        }
    }

    /** 拉取服务器数据并替换本地 */
    suspend fun syncFromServer() {
        val remoteResult = getRemoteHistory()
        remoteResult.getOrNull()?.let { data ->
            mediaDao.clearAllHistory()
            data.items.forEach { record ->
                mediaDao.insertHistory(
                    LocalHistory(
                        media_id = record.mediaId,
                        media_type = record.mediaType,
                        watch_position = record.watchPosition,
                        synced = true,
                        watched_at = parseTimestamp(record.watchedAt)
                    )
                )
            }
        }
    }

    private fun parseTimestamp(dateStr: String): Long {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
