package com.mediacollector.app.data.remote.api

import com.mediacollector.app.data.remote.dto.*
import retrofit2.http.*

/** 聊天 API */
interface ChatApi {
    @GET("api/v1/chat/rooms")
    suspend fun getRooms(): ApiResponse<List<ChatRoom>>

    @GET("api/v1/chat/messages")
    suspend fun getMessages(
        @Query("room_id") roomId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): ApiListResponse<ChatMessageDto>

    @POST("api/v1/chat/messages")
    suspend fun syncMessage(@Body body: ChatSyncRequest): ApiResponse<Unit>

    @DELETE("api/v1/chat/messages")
    suspend fun clearMessages(): ApiResponse<Unit>

    @POST("api/v1/chat/heartbeat")
    suspend fun heartbeat(@Body body: ChatHeartbeatRequest): ApiResponse<Int>

    @GET("api/v1/chat/online")
    suspend fun getOnlineUsers(
        @Query("room_id") roomId: String = "lobby"
    ): ApiResponse<List<OnlineUser>>

    @GET("api/v1/chat/online/count")
    suspend fun getOnlineCount(
        @Query("room_id") roomId: String = "lobby"
    ): ApiResponse<Int>
}
