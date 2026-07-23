package com.mediacollector.app.ui.video

import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mediacollector.app.ui.cast.CastHelper
import com.mediacollector.app.ui.common.LoadingIndicator

/** 视频播放器页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    mediaId: Int,
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    LaunchedEffect(mediaId) { viewModel.loadMedia(mediaId) }

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf(false) }

    fun toggleFullscreen() {
        val activity = context as? android.app.Activity ?: return
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            (context as? android.app.Activity)?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                act.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 顶部栏（全屏时隐藏）
        if (!isFullscreen) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 播放器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (isFullscreen) it.fillMaxHeight() else it.aspectRatio(16f / 9f) }
        ) {
            when {
                state.isLoading -> LoadingIndicator(message = "")
                playerError -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PlayDisabled,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("视频加载失败", color = Color.Gray)
                            Text("链接可能已失效", color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                state.videoUrl.isNotEmpty() -> {
                    val exoPlayer = remember {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(state.videoUrl))
                            prepare()
                            playWhenReady = true
                            addListener(object : Player.Listener {
                                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                    playerError = true
                                }
                            })
                        }
                    }

                    DisposableEffect(exoPlayer) {
                        onDispose { exoPlayer.release() }
                    }

                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 全屏按钮
                    IconButton(
                        onClick = { toggleFullscreen() },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(
                            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            "全屏",
                            tint = Color.White
                        )
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("无法加载视频", color = Color.Gray)
                    }
                }
            }
        }

        // 视频信息（全屏时隐藏）
        if (!isFullscreen && state.mediaDetail != null) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(state.mediaDetail!!.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    if (state.mediaDetail!!.description.isNotEmpty()) {
                        Text(state.mediaDetail!!.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("时长", style = MaterialTheme.typography.labelSmall)
                            Text("${state.mediaDetail!!.duration / 60}:${(state.mediaDetail!!.duration % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("来源", style = MaterialTheme.typography.labelSmall)
                            Text(state.mediaDetail!!.source.ifEmpty { "未知" }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (state.isFavorite) Icons.Default.Favorite
                                    else Icons.Default.FavoriteBorder,
                                    "收藏",
                                    tint = if (state.isFavorite) Color(0xFFFF4081) else Color.Unspecified
                                )
                                Text("收藏", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(onClick = {
                            val videoUrl = state.videoUrl
                            val title = state.mediaDetail?.title ?: "video_$mediaId"
                            if (videoUrl.isNotEmpty()) {
                                // 加入下载跟踪
                                com.mediacollector.app.ui.download.DownloadTracker.addDownload(mediaId, title, videoUrl)
                                // 启动前台下载服务
                                val intent = Intent(context, com.mediacollector.app.ui.download.DownloadService::class.java).apply {
                                    action = com.mediacollector.app.ui.download.DownloadService.ACTION_DOWNLOAD
                                    putExtra(com.mediacollector.app.ui.download.DownloadService.EXTRA_URL, videoUrl)
                                    putExtra(com.mediacollector.app.ui.download.DownloadService.EXTRA_FILENAME, title.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(80))
                                    putExtra(com.mediacollector.app.ui.download.DownloadService.EXTRA_MEDIA_ID, mediaId)
                                }
                                context.startForegroundService(intent)
                                android.widget.Toast.makeText(context, "开始下载", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Download, "下载")
                                Text("下载", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(onClick = {
                            val ctx = context
                            val available = CastHelper.isCastAvailable(ctx)
                            if (available) {
                                android.widget.Toast.makeText(ctx, "已连接投屏设备", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(ctx, "未发现投屏设备", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Cast, "投屏")
                                Text("投屏", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
