package com.mediacollector.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ════════════════════════════════════════
// 认证相关
// ════════════════════════════════════════

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    @SerialName("display_name") val displayName: String = "",
    val email: String = ""
)

@Serializable
data class AuthResult(
    @SerialName("user_id") val userId: Int,
    val username: String,
    @SerialName("display_name") val displayName: String = "",
    val token: String,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
data class UserInfo(
    val id: Int,
    val username: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    val email: String = "",
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("history_count") val historyCount: Int = 0,
    @SerialName("created_at") val createdAt: String = ""
)

// ════════════════════════════════════════
// 收藏
// ════════════════════════════════════════

@Serializable
data class FavoriteAction(
    @SerialName("media_id") val mediaId: Int
)

@Serializable
data class FavoriteCheckResponse(
    val data: Map<String, Boolean> // media_id -> is_favorite
)

// ════════════════════════════════════════
// 历史
// ════════════════════════════════════════

@Serializable
data class HistoryRecord(
    @SerialName("media_id") val mediaId: Int,
    @SerialName("media_type") val mediaType: String = "photo",
    @SerialName("watch_position") val watchPosition: Int = 0,
    @SerialName("watched_at") val watchedAt: String = "",
    val media: MediaItem? = null
)

@Serializable
data class HistoryPostRequest(
    @SerialName("media_id") val mediaId: Int,
    @SerialName("media_type") val mediaType: String = "photo",
    @SerialName("watch_position") val watchPosition: Int = 0
)

// ════════════════════════════════════════
// 系统检测
// ════════════════════════════════════════

@Serializable
data class SystemCheck(
    @SerialName("server_name") val serverName: String = "",
    @SerialName("api_version") val apiVersion: String = "",
    val checks: Map<String, CheckGroup> = emptyMap(),
    @SerialName("media_count") val mediaCount: Int = 0,
    val features: Map<String, Boolean> = emptyMap()
)

@Serializable
data class CheckGroup(
    val status: Boolean,
    val endpoints: List<String>
)

// ════════════════════════════════════════
// 聊天
// ════════════════════════════════════════

@Serializable
data class ChatRoom(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0
)

@Serializable
data class ChatMessageDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("sender_user") val senderUser: String,
    @SerialName("sender_name") val senderName: String,
    val type: String = "text", // text / image / system
    val content: String,
    val timestamp: Long
)

@Serializable
data class ChatSyncRequest(
    @SerialName("message_id") val messageId: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("sender_user") val senderUser: String,
    @SerialName("sender_name") val senderName: String,
    val type: String = "text",
    val content: String,
    val timestamp: Long
)

@Serializable
data class ChatHeartbeatRequest(
    @SerialName("room_id") val roomId: String = "lobby"
)

@Serializable
data class ChatOnlineResponse(
    val data: ChatOnlineData
)

@Serializable
data class ChatOnlineData(
    val onlineUsers: List<OnlineUser> = emptyList(),
    @SerialName("online_count") val onlineCount: Int = 0
)

@Serializable
data class OnlineUser(
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("room_id") val roomId: String = ""
)
