package com.mediacollector.app.data.repository

import com.mediacollector.app.data.remote.api.MediaApi
import com.mediacollector.app.data.remote.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val mediaApi: MediaApi
) {
    suspend fun getMediaList(
        page: Int = 1,
        pageSize: Int = 20,
        type: String? = null,
        source: String? = null,
        tag: String? = null
    ): Result<PaginatedData<MediaItem>> = runCatching {
        val response = mediaApi.getMediaList(page, pageSize, type, source, tag)
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    suspend fun getMediaDetail(id: Int): Result<MediaDetail> = runCatching {
        val response = mediaApi.getMediaDetail(id)
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    suspend fun getCollections(): Result<List<MediaCollection>> = runCatching {
        val response = mediaApi.getCollections()
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    suspend fun getCollectionTree(): Result<List<MediaCollection>> = runCatching {
        val response = mediaApi.getCollectionTree()
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    suspend fun getCollectionDetail(id: Int): Result<MediaCollection> = runCatching {
        val response = mediaApi.getCollectionDetail(id)
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    suspend fun getTags(): Result<List<MediaTag>> = runCatching {
        val response = mediaApi.getTags()
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    suspend fun search(
        keyword: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<SearchResult> = runCatching {
        val response = mediaApi.search(keyword, page, pageSize)
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }

    suspend fun getStats(): Result<Stats> = runCatching {
        val response = mediaApi.getStats()
        if (response.success && response.data != null) response.data
        else throw Exception(response.message)
    }
}
