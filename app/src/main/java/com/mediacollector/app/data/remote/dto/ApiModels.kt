package com.mediacollector.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ════════════════════════════════════════
// 统一响应包装
// ════════════════════════════════════════

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val code: Int = 200,
    val message: String = "",
    val data: T? = null
)

@Serializable
data class ApiListResponse<T>(
    val success: Boolean,
    val code: Int = 200,
    val message: String = "",
    val data: PaginatedData<T>? = null
)

@Serializable
data class PaginatedData<T>(
    val items: List<T>,
    val pagination: Pagination
)

@Serializable
data class Pagination(
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val total: Int,
    @SerialName("total_pages") val totalPages: Int
)

// ════════════════════════════════════════
// 媒体
// ════════════════════════════════════════

@Serializable
data class MediaItem(
    val id: Int,
    val title: String = "",
    val description: String = "",
    val url: String = "",
    @SerialName("local_path") val localPath: String = "",
    val type: String = "photo", // photo / video / audio / document / other
    @SerialName("mime_type") val mimeType: String = "",
    @SerialName("file_size") val fileSize: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Int = 0,
    @SerialName("thumbnail_url") val thumbnailUrl: String = "",
    val source: String = "",
    @SerialName("source_id") val sourceId: String = "",
    @SerialName("source_url") val sourceUrl: String = "",
    val author: String = "",
    val status: String = "active",
    @SerialName("is_favorite") val isFavorite: Int = 0,
    val rating: Int = 0,
    @SerialName("md5_hash") val md5Hash: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    val isVideo: Boolean get() = type == "video"
    val isPhoto: Boolean get() = type == "photo"
}

@Serializable
data class MediaDetail(
    val id: Int,
    val title: String = "",
    val description: String = "",
    val url: String = "",
    val type: String = "photo",
    @SerialName("thumbnail_url") val thumbnailUrl: String = "",
    val source: String = "",
    val author: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val duration: Int = 0,
    @SerialName("file_size") val fileSize: Long = 0,
    @SerialName("mime_type") val mimeType: String = "",
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    val tags: List<MediaTag> = emptyList(),
    val exif: ExifData? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("collections") val collections: List<CollectionSummary> = emptyList()
)

@Serializable
data class ExifData(
    @SerialName("camera_make") val cameraMake: String = "",
    @SerialName("camera_model") val cameraModel: String = "",
    @SerialName("focal_length") val focalLength: String = "",
    val aperture: String = "",
    @SerialName("shutter_speed") val shutterSpeed: String = "",
    val iso: Int = 0,
    @SerialName("gps_lat") val gpsLat: Double? = null,
    @SerialName("gps_lng") val gpsLng: Double? = null,
    @SerialName("taken_at") val takenAt: String = ""
)

@Serializable
data class CollectionSummary(
    val id: Int,
    val name: String,
    @SerialName("item_count") val itemCount: Int = 0
)

// ════════════════════════════════════════
// 集合
// ════════════════════════════════════════

@Serializable
data class MediaCollection(
    val id: Int,
    val name: String,
    val description: String = "",
    @SerialName("cover_url") val coverUrl: String = "",
    val type: String = "album", // album / folder / playlist / project
    @SerialName("parent_id") val parentId: Int? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("is_public") val isPublic: Int = 1,
    val children: List<MediaCollection>? = null, // 树形结构
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

// ════════════════════════════════════════
// 标签
// ════════════════════════════════════════

@Serializable
data class MediaTag(
    val id: Int,
    val name: String,
    val slug: String = "",
    val description: String = "",
    val type: String = "general",
    val color: String = "#6366f1",
    @SerialName("sort_order") val sortOrder: Int = 0
)

// ════════════════════════════════════════
// 搜索
// ════════════════════════════════════════

@Serializable
data class SearchResult(
    val results: List<MediaItem>,
    val pagination: Pagination
)

// ════════════════════════════════════════
// 统计
// ════════════════════════════════════════

@Serializable
data class Stats(
    @SerialName("total_media") val totalMedia: Int = 0,
    @SerialName("total_photos") val totalPhotos: Int = 0,
    @SerialName("total_videos") val totalVideos: Int = 0,
    @SerialName("total_collections") val totalCollections: Int = 0,
    @SerialName("total_tags") val totalTags: Int = 0
)
