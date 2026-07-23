package com.mediacollector.app.data.repository

import com.mediacollector.app.data.local.dao.MediaDao
import com.mediacollector.app.data.local.entity.LocalFavorite
import com.mediacollector.app.data.remote.api.UserApi
import com.mediacollector.app.data.remote.dto.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val userApi: UserApi,
    private val mediaDao: MediaDao
) {
    /** 本地收藏流（即时 UI 响应） */
    val localFavorites: Flow<List<LocalFavorite>> = mediaDao.getLocalFavorites()

    /** 从服务器拉取收藏列表 */
    suspend fun getRemoteFavorites(page: Int = 1, pageSize: Int = 100): Result<PaginatedData<MediaItem>> =
        runCatching {
            val response = userApi.getFavorites(page, pageSize)
            if (response.success && response.data != null) response.data
            else throw Exception(response.message)
        }

    /** 添加收藏 */
    suspend fun addFavorite(mediaId: Int) {
        // 先存本地
        mediaDao.insertFavorite(LocalFavorite(media_id = mediaId, synced = false))
        // 同步到服务器
        runCatching {
            userApi.addFavorite(FavoriteAction(mediaId))
            mediaDao.markFavoriteSynced(mediaId)
        }
    }

    /** 取消收藏 */
    suspend fun removeFavorite(mediaId: Int) {
        // 先删本地
        mediaDao.deleteFavoriteByMediaId(mediaId)
        // 同步到服务器
        runCatching { userApi.removeFavorite(mediaId) }
    }

    /** 同步本地未同步收藏到服务器 */
    suspend fun syncPending() {
        mediaDao.getUnsyncedFavorites().forEach { fav ->
            runCatching {
                userApi.addFavorite(FavoriteAction(fav.media_id))
                mediaDao.markFavoriteSynced(fav.media_id)
            }
        }
    }

    /** 拉取服务器数据并替换本地 */
    suspend fun syncFromServer() {
        val remoteResult = getRemoteFavorites()
        remoteResult.getOrNull()?.let { data ->
            mediaDao.clearAllFavorites()
            data.items.forEach { media ->
                mediaDao.insertFavorite(
                    LocalFavorite(media_id = media.id, synced = true)
                )
            }
        }
    }

    /** 批量检查收藏状态 */
    suspend fun checkFavorites(mediaIds: List<Int>): Result<Map<String, Boolean>> = runCatching {
        val response = userApi.checkFavorites(mediaIds.joinToString(","))
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }
}
