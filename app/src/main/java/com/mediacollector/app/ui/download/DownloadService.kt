package com.mediacollector.app.ui.download

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 前台下载服务
 *
 * 管理文件下载进度，显示通知栏进度。
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
                val fileName = intent.getStringExtra(EXTRA_FILENAME) ?: "download_${System.currentTimeMillis()}"
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
                val url = URL(urlStr)
                val connection = url.openConnection()
                val totalSize = connection.contentLengthLong
                val inputStream = connection.getInputStream()

                val downloadDir = File(getExternalFilesDir(null), "downloads")
                downloadDir.mkdirs()
                val outputFile = File(downloadDir, fileName)
                val outputStream = FileOutputStream(outputFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    if (totalSize > 0) {
                        val progress = (totalRead * 100 / totalSize).toInt()
                        val notification = createNotification(
                            "${fileName} (${progress}%)",
                            progress
                        )
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIFICATION_ID, notification)
                    }
                }

                outputStream.close()
                inputStream.close()

                // 下载完成通知
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(
                    NOTIFICATION_ID,
                    NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("下载完成")
                        .setContentText(fileName)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .build()
                )

            } catch (e: Exception) {
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(
                    NOTIFICATION_ID,
                    NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("下载失败")
                        .setContentText(e.message)
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
