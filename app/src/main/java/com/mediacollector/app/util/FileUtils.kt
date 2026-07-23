package com.mediacollector.app.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** 文件操作工具类 */
object FileUtils {
    /** 缓存目录：分享临时文件 */
    fun getSharedDir(context: Context): File {
        return File(context.cacheDir, "shared").also { it.mkdirs() }
    }

    /** 缓存目录：缩略图 */
    fun getThumbnailDir(context: Context): File {
        return File(context.cacheDir, "thumbnails").also { it.mkdirs() }
    }

    /** 下载目录：外部 Download 目录 */
    fun getDownloadDir(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }

    /** 保存到系统相册（Android Q+ 使用 MediaStore） */
    fun saveToGallery(context: Context, sourceFile: File, mimeType: String = "image/jpeg"): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return false

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().use { input -> input.copyTo(output) }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                true
            } else {
                // Android 9 及以下直接复制到相册目录
                val destDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
                destDir.mkdirs()
                val destFile = File(destDir, sourceFile.name)
                sourceFile.copyTo(destFile, overwrite = true)
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** 获取文件扩展名 */
    fun getExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    /** 安全删除文件 */
    fun safeDelete(file: File) {
        try {
            if (file.exists()) file.delete()
        } catch (_: Exception) { }
    }
}
