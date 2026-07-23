package com.mediacollector.app.ui.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 图片分享助手
 *
 * 下载图片到临时目录 → 分享 → 自动清理。
 */
object ShareHelper {

    /**
     * 分享图片（挂起函数，需要在协程中调用）
     *
     * @param context 上下文
     * @param imageUrl 图片 URL
     * @return true 分享成功
     */
    suspend fun shareImage(
        context: Context,
        imageUrl: String
    ): Boolean {
        return try {
            // 创建临时目录
            val sharedDir = File(context.cacheDir, "shared")
            sharedDir.mkdirs()
            val outputFile = File(sharedDir, "share_${System.currentTimeMillis()}.jpg")

            // 下载图片到临时文件
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = true
            connection.inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()

            // 创建 FileProvider URI
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            // 创建分享 Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // 启动分享（非 Activity 上下文需要 NEW_TASK flag）
            context.startActivity(Intent.createChooser(shareIntent, "分享图片").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            outputFile.deleteOnExit()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 清理所有分享临时文件
     */
    fun cleanupSharedFiles(context: Context) {
        val sharedDir = File(context.cacheDir, "shared")
        if (sharedDir.exists()) {
            sharedDir.listFiles()?.forEach { it.delete() }
        }
    }
}
