package com.mediacollector.app.data.remote.api

import com.mediacollector.app.data.remote.dto.*
import retrofit2.http.*

/** 用户数据 API（收藏/历史） */
interface UserApi {
    // ── 收藏 ──
    @GET("api/v1/user/favorites")
    suspend fun getFavorites(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiListResponse<MediaItem>

    @POST("api/v1/user/favorites")
    suspend fun addFavorite(@Body body: FavoriteAction): ApiResponse<Unit>

    @DELETE("api/v1/user/favorites/{mediaId}")
    suspend fun removeFavorite(@Path("mediaId") mediaId: Int): ApiResponse<Unit>

    @GET("api/v1/user/favorites/check")
    suspend fun checkFavorites(
        @Query("media_ids") mediaIds: String
    ): ApiResponse<Map<String, Boolean>>

    // ── 历史 ──
    @GET("api/v1/user/history")
    suspend fun getHistory(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiListResponse<HistoryRecord>

    @POST("api/v1/user/history")
    suspend fun addHistory(@Body body: HistoryPostRequest): ApiResponse<Unit>

    @DELETE("api/v1/user/history/{mediaId}")
    suspend fun removeHistory(@Path("mediaId") mediaId: Int): ApiResponse<Unit>

    @DELETE("api/v1/user/history")
    suspend fun clearHistory(): ApiResponse<Unit>
}
