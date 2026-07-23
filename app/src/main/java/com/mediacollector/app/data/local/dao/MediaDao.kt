package com.mediacollector.app.data.local.dao

import androidx.room.*
import com.mediacollector.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    // ── 收藏 ──
    @Query("SELECT * FROM local_favorites ORDER BY created_at DESC")
    fun getLocalFavorites(): Flow<List<LocalFavorite>>

    @Query("SELECT * FROM local_favorites WHERE media_id = :mediaId LIMIT 1")
    suspend fun getFavoriteByMediaId(mediaId: Int): LocalFavorite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(fav: LocalFavorite)

    @Delete
    suspend fun deleteFavorite(fav: LocalFavorite)

    @Query("DELETE FROM local_favorites WHERE media_id = :mediaId")
    suspend fun deleteFavoriteByMediaId(mediaId: Int)

    @Query("DELETE FROM local_favorites")
    suspend fun clearAllFavorites()

    @Query("SELECT * FROM local_favorites WHERE synced = 0")
    suspend fun getUnsyncedFavorites(): List<LocalFavorite>

    @Query("UPDATE local_favorites SET synced = 1 WHERE media_id = :mediaId")
    suspend fun markFavoriteSynced(mediaId: Int)

    // ── 历史 ──
    @Query("SELECT * FROM local_history ORDER BY watched_at DESC")
    fun getLocalHistory(): Flow<List<LocalHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: LocalHistory)

    @Query("DELETE FROM local_history WHERE media_id = :mediaId")
    suspend fun deleteHistoryByMediaId(mediaId: Int)

    @Query("DELETE FROM local_history")
    suspend fun clearAllHistory()

    @Query("SELECT * FROM local_history WHERE synced = 0")
    suspend fun getUnsyncedHistory(): List<LocalHistory>

    @Query("UPDATE local_history SET synced = 1 WHERE media_id = :mediaId")
    suspend fun markHistorySynced(mediaId: Int)

    // ── 缓存媒体 ──
    @Query("SELECT * FROM cached_media ORDER BY cached_at DESC")
    fun getCachedMedia(): Flow<List<CachedMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedMedia(media: CachedMedia)

    @Query("DELETE FROM cached_media WHERE id = :mediaId")
    suspend fun deleteCachedMedia(mediaId: Int)

    // ── 聊天消息 ──
    @Query("SELECT * FROM chat_messages WHERE room_id = :roomId ORDER BY timestamp ASC")
    fun getChatMessages(roomId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE room_id = :roomId")
    suspend fun clearChatMessages(roomId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllChatMessages()

    @Query("SELECT COUNT(*) FROM chat_messages WHERE room_id = :roomId")
    suspend fun getChatMessageCount(roomId: String): Int
}
