package com.mediacollector.app.domain

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缓存管理器
 *
 * 负责读取各缓存大小、执行清理操作。
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader
) {
    data class CacheInfo(
        val coilCacheSize: Long = 0,      // Coil 磁盘缓存
        val thumbnailCacheSize: Long = 0,  // 本地缩略图缓存
        val databaseSize: Long = 0,        // Room 数据库
        val totalSize: Long = 0
    )

    /** 获取所有缓存大小 */
    fun getCacheInfo(): CacheInfo {
        val coilSize = imageLoader.diskCache?.size ?: 0L
        val thumbDir = getThumbnailDir()
        val thumbSize = if (thumbDir.exists()) {
            thumbDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
        val dbSize = context.getDatabasePath("media_collector.db").let {
            if (it.exists()) it.length() else 0L
        }
        val total = coilSize + thumbSize + dbSize
        return CacheInfo(coilSize, thumbSize, dbSize, total)
    }

    /** 清理所有缓存 */
    fun clearAllCache() {
        imageLoader.diskCache?.clear()
        getThumbnailDir().deleteRecursively()
    }

    /** 清理缩略图缓存 */
    fun clearThumbnailCache() {
        getThumbnailDir().deleteRecursively()
    }

    /** 清理 Coil 磁盘缓存 */
    fun clearCoilCache() {
        imageLoader.diskCache?.clear()
    }

    private fun getThumbnailDir(): File {
        return File(context.cacheDir, "thumbnails").also { it.mkdirs() }
    }

    /** 格式化文件大小 */
    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }
}
