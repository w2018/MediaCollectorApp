package com.mediacollector.app.data.repository

import com.mediacollector.app.data.local.dao.MediaDao
import com.mediacollector.app.data.local.entity.ChatMessageEntity
import com.mediacollector.app.data.remote.api.ChatApi
import com.mediacollector.app.data.remote.dto.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val mediaDao: MediaDao
) {
    /** 本地聊天消息流 */
    fun getLocalMessages(roomId: String): Flow<List<ChatMessageEntity>> =
        mediaDao.getChatMessages(roomId)

    /** 从服务器拉取历史消息 */
    suspend fun getRemoteMessages(
        roomId: String,
        page: Int = 1,
        pageSize: Int = 50
    ): Result<PaginatedData<ChatMessageDto>> = runCatching {
        val response = chatApi.getMessages(roomId, page, pageSize)
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    /** 保存消息到本地 */
    suspend fun saveMessage(message: ChatMessageEntity) {
        mediaDao.insertChatMessage(message)
    }

    /** 批量保存消息到本地 */
    suspend fun saveMessages(messages: List<ChatMessageEntity>) {
        messages.forEach { mediaDao.insertChatMessage(it) }
    }

    /** 同步消息到服务端 */
    suspend fun syncMessage(request: ChatSyncRequest): Result<Unit> = runCatching {
        val response = chatApi.syncMessage(request)
        if (!response.success) throw Exception(response.message)
    }

    /** 获取聊天室列表 */
    suspend fun getRooms(): Result<List<ChatRoom>> = runCatching {
        val response = chatApi.getRooms()
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    /** 心跳 */
    suspend fun heartbeat(roomId: String): Result<Int> = runCatching {
        val response = chatApi.heartbeat(ChatHeartbeatRequest(roomId))
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    /** 在线用户 */
    suspend fun getOnlineUsers(roomId: String): Result<List<OnlineUser>> = runCatching {
        val response = chatApi.getOnlineUsers(roomId)
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    /** 在线人数 */
    suspend fun getOnlineCount(roomId: String): Result<Int> = runCatching {
        val response = chatApi.getOnlineCount(roomId)
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    /** 清空本地聊天消息 */
    suspend fun clearLocalMessages(roomId: String) {
        mediaDao.clearChatMessages(roomId)
    }

    /** 清空所有本地聊天 */
    suspend fun clearAllLocalMessages() {
        mediaDao.clearAllChatMessages()
    }

    /** 退出时清空服务端消息 */
    suspend fun clearRemoteMessages(): Result<Unit> = runCatching {
        val response = chatApi.clearMessages()
        if (!response.success) throw Exception(response.message)
    }
}
