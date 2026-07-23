package com.mediacollector.app.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mediacollector.app.ui.common.LoadingIndicator
import com.mediacollector.app.util.DateUtils

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
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 播放器区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            when {
                state.isLoading -> LoadingIndicator(message = "")
                state.videoUrl.isNotEmpty() -> {
                    // ExoPlayer
                    DisposableEffect(context) {
                        val player = ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(state.videoUrl))
                            prepare()
                            playWhenReady = true
                        }

                        val playerView = PlayerView(context).apply {
                            this.player = player
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }

                        onDispose { player.release() }
                    }

                    // 简化的 PlayerView 集成（实际需用 AndroidView）
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = ExoPlayer.Builder(ctx).build().apply {
                                    setMediaItem(MediaItem.fromUri(state.videoUrl))
                                    prepare()
                                    playWhenReady = true
                                }
                                useController = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("无法加载视频", color = Color.White)
                    }
                }
            }
        }

        // 视频信息
        if (state.mediaDetail != null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = state.mediaDetail!!.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.mediaDetail!!.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    // 元信息
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("时长", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${state.mediaDetail!!.duration / 60}:${(state.mediaDetail!!.duration % 60).toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column {
                            Text("来源", style = MaterialTheme.typography.labelSmall)
                            Text(
                                state.mediaDetail!!.source.ifEmpty { "未知" },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // 顶部返回按钮
    Box(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { /* TODO: 投屏 */ }) {
                    Icon(Icons.Default.Cast, "投屏", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
