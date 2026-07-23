package com.mediacollector.app.data.remote.api

import com.mediacollector.app.data.remote.dto.*
import retrofit2.http.*

/** 媒体相关 API */
interface MediaApi {
    @GET("api/v1/media")
    suspend fun getMediaList(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("type") type: String? = null,
        @Query("source") source: String? = null,
        @Query("tag") tag: String? = null
    ): ApiListResponse<MediaItem>

    @GET("api/v1/media/{id}")
    suspend fun getMediaDetail(@Path("id") id: Int): ApiResponse<MediaDetail>

    @GET("api/v1/collections")
    suspend fun getCollections(): ApiResponse<List<MediaCollection>>

    @GET("api/v1/collections/tree")
    suspend fun getCollectionTree(): ApiResponse<List<MediaCollection>>

    @GET("api/v1/collections/{id}")
    suspend fun getCollectionDetail(@Path("id") id: Int): ApiResponse<MediaCollection>

    @GET("api/v1/tags")
    suspend fun getTags(): ApiResponse<List<MediaTag>>

    @GET("api/v1/search")
    suspend fun search(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<SearchResult>

    @GET("api/v1/stats")
    suspend fun getStats(): ApiResponse<Stats>
}
