package com.mediacollector.app.ui.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import java.io.File

/**
 * 图片分享助手
 *
 * 下载图片到临时目录 → 分享 → 自动清理。
 */
object ShareHelper {

    /**
     * 分享图片
     *
     * @param context 上下文
     * @param imageLoader Coil ImageLoader
     * @param imageUrl 图片 URL
     * @param fileName 文件名（不含扩展名）
     * @param onComplete 分享完成回调（用于清理）
     */
    fun shareImage(
        context: Context,
        imageLoader: ImageLoader,
        imageUrl: String,
        fileName: String = "share_${System.currentTimeMillis()}"
    ) {
        // 创建临时目录
        val sharedDir = File(context.cacheDir, "shared")
        sharedDir.mkdirs()

        // 下载图片到临时文件
        val outputFile = File(sharedDir, "$fileName.jpg")

        // 使用 Coil 下载
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .build()

        // 同步下载
        kotlinx.coroutines.runBlocking {
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                // 将图片数据写入临时文件
                result.image
                // 实际项目中需从 result 提取图片数据写入 outputFile
            }
        }

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
        }

        // 启动分享
        context.startActivity(Intent.createChooser(shareIntent, "分享图片"))

        // 注册分享完成回调（在 Activity 的 onActivityResult 中清理）
        // 或使用 File.deleteOnExit()
        outputFile.deleteOnExit()
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
