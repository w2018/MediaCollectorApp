package com.mediacollector.app.ui.download

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 前台下载服务
 *
 * 管理文件下载进度，同时更新 DownloadTracker 供 UI 展示。
 */
@AndroidEntryPoint
class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DOWNLOAD = "com.mediacollector.download.DOWNLOAD"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILENAME = "extra_filename"
        const val EXTRA_MEDIA_ID = "extra_media_id"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val fileName = intent.getStringExtra(EXTRA_FILENAME)
                    ?: "download_${System.currentTimeMillis()}"
                val mediaId = intent.getIntExtra(EXTRA_MEDIA_ID, 0)

                startForeground(NOTIFICATION_ID, createNotification("准备下载...", 0))
                downloadFile(url, fileName, mediaId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "下载进度",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "媒体文件下载通知"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("下载中")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .build()
    }

    private fun downloadFile(urlStr: String, fileName: String, mediaId: Int) {
        Thread {
            try {
                val downloadDir = File(getExternalFilesDir(null), "downloads")
                downloadDir.mkdirs()

                // 文件名去重
                val ext = if (urlStr.contains(".m3u8")) "ts" else fileName.substringAfterLast(".", "mp4")
                val baseName = fileName.substringBeforeLast(".")
                var outputFile = File(downloadDir, "$baseName.$ext")
                var suffix = 1
                while (outputFile.exists()) {
                    outputFile = File(downloadDir, "${baseName}_$suffix.$ext")
                    suffix++
                }

                val isHls = urlStr.contains(".m3u8") || urlStr.contains("m3u8")
                val success: Boolean
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (isHls) {
                    // HLS 流下载：下载播放列表 → 解析分片 → 下载合并
                    nm.notify(NOTIFICATION_ID, createNotification("解析 HLS 流...", 0))
                    success = HlsDownloader.download(urlStr, outputFile) { downloaded, total ->
                        val progress = (downloaded * 100 / total).coerceAtMost(99)
                        DownloadTracker.updateProgress(mediaId, progress)
                        nm.notify(NOTIFICATION_ID, createNotification("$baseName ($downloaded/$total 分片)", progress))
                    }
                } else {
                    // 直接文件下载
                    val url = URL(urlStr)
                    val connection = url.openConnection().apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                    }
                    val totalSize = connection.contentLengthLong
                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(outputFile)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastProgress = -1

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalSize > 0) {
                            val progress = (totalRead * 100 / totalSize).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                DownloadTracker.updateProgress(mediaId, progress)
                                nm.notify(NOTIFICATION_ID, createNotification("$baseName ($progress%)", progress))
                            }
                        }
                    }
                    outputStream.close()
                    inputStream.close()
                    success = true
                }

                if (success) {
                    DownloadTracker.markCompleted(mediaId)
                    nm.notify(
                        NOTIFICATION_ID,
                        NotificationCompat.Builder(this, CHANNEL_ID)
                            .setContentTitle("下载完成")
                            .setContentText(outputFile.name)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                            .build()
                    )
                } else {
                    DownloadTracker.markFailed(mediaId)
                    outputFile.delete()
                    nm.notify(
                        NOTIFICATION_ID,
                        NotificationCompat.Builder(this, CHANNEL_ID)
                            .setContentTitle("下载失败")
                            .setContentText("视频下载失败，请重试")
                            .setSmallIcon(android.R.drawable.stat_notify_error)
                            .build()
                    )
                }

            } catch (e: Exception) {
                DownloadTracker.markFailed(mediaId)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(
                    NOTIFICATION_ID,
                    NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("下载失败")
                        .setContentText(e.message ?: "未知错误")
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .build()
                )
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
    }
}
