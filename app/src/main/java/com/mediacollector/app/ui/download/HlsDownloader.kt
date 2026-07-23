package com.mediacollector.app.ui.download

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * HLS 视频下载器
 *
 * 支持：
 * - 变种播放列表（自动跟随到子播放列表）
 * - 绝对路径 / 相对路径 / 完整 URL 分片
 * - 分片合并为单个 .ts 文件
 */
object HlsDownloader {

    /**
     * 下载 HLS 视频
     * @param m3u8Url 播放列表 URL
     * @param outputFile 输出文件 (.ts)
     * @param onProgress 进度回调 (downloadedSegments, totalSegments)
     */
    fun download(
        m3u8Url: String,
        outputFile: File,
        onProgress: (downloadedSegments: Int, totalSegments: Int) -> Unit = { _, _ -> }
    ): Boolean {
        return try {
            // 1. 下载播放列表
            val playlistContent = downloadUrl(m3u8Url)

            // 2. 解析分片 URL
            val baseUrl = m3u8Url.substringBeforeLast("/")
            val lines = playlistContent.lines()

            // 检查是否为变种播放列表（包含 STREAM-INF 但没有 #EXTINF）
            val isVariant = lines.any { it.contains("STREAM-INF") }

            val segmentUrls: List<String>

            if (isVariant) {
                // 变种播放列表 → 提取子播放列表 URL 并下载它
                val subPlaylistPath = lines.firstOrNull { line ->
                    line.isNotBlank() && !line.startsWith("#")
                } ?: return false

                val subUrl = resolveUrl(baseUrl, subPlaylistPath)
                val subContent = downloadUrl(subUrl)
                val subBase = subUrl.substringBeforeLast("/")

                // 从子播放列表解析 ts 分片
                segmentUrls = subContent.lines()
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .map { resolveUrl(subBase, it) }

                if (segmentUrls.isEmpty()) return false
            } else {
                // 直接是分片列表
                segmentUrls = lines
                    .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("#") }
                    .map { resolveUrl(baseUrl, it) }

                if (segmentUrls.isEmpty()) {
                    // 不是 HLS，可能直接是视频文件
                    return downloadDirect(m3u8Url, outputFile)
                }
            }

            // 3. 下载所有分片并合并
            val total = segmentUrls.size
            val outputStream = FileOutputStream(outputFile)
            var downloaded = 0

            for (segUrl in segmentUrls) {
                try {
                    val bytes = downloadUrlBytes(segUrl)
                    if (bytes != null) {
                        outputStream.write(bytes)
                    }
                    downloaded++
                    onProgress(downloaded, total)
                } catch (e: Exception) {
                    downloaded++
                    onProgress(downloaded, total)
                }
            }

            outputStream.close()
            total > 0 && downloaded > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** 解析 URL（支持绝对路径 / 相对路径 / 完整 URL） */
    private fun resolveUrl(baseUrl: String, path: String): String {
        return when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            path.startsWith("/") -> {
                // 绝对路径：基于域名根目录
                val scheme = baseUrl.substringBefore("://") + "://"
                val domain = baseUrl.substringAfter("://").substringBefore("/")
                "$scheme$domain$path"
            }
            else -> {
                // 相对路径：基于当前目录
                val dir = baseUrl.substringBeforeLast("/")
                "$dir/$path"
            }
        }
    }

    /** 创建 HTTP 连接（带反盗链头） */
    private fun createConnection(urlStr: String): HttpURLConnection {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Android 14; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
        conn.setRequestProperty("Referer", urlStr.substringBefore("/", urlStr)
            .let { if (it.contains("://")) it.substringBefore("/", "").let { r -> "$r/" } else "" })
        conn.instanceFollowRedirects = true
        return conn
    }

    /** 下载 URL 内容为字符串 */
    private fun downloadUrl(urlStr: String): String {
        val conn = createConnection(urlStr)
        return conn.inputStream.bufferedReader().readText()
    }

    /** 下载 URL 内容为字节数组 */
    private fun downloadUrlBytes(urlStr: String): ByteArray? {
        return try {
            createConnection(urlStr).inputStream.readBytes()
        } catch (e: Exception) {
            null
        }
    }

    /** 直接下载单个文件 */
    private fun downloadDirect(url: String, outputFile: File): Boolean {
        try {
            val conn = createConnection(url)
            val inputStream = conn.inputStream
            val outputStream = FileOutputStream(outputFile)
            inputStream.copyTo(outputStream)
            outputStream.close()
            inputStream.close()
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
