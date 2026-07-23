package com.mediacollector.app.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mediacollector.app.ui.common.ErrorState
import com.mediacollector.app.ui.common.LoadingIndicator

/** 媒体详情页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    mediaId: Int,
    onBack: () -> Unit,
    onPlayVideo: () -> Unit,
    viewModel: MediaDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(mediaId) { viewModel.loadMedia(mediaId) }

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.title ?: "详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingIndicator()
            state.error != null -> ErrorState(state.error!!, onRetry = { viewModel.loadMedia(mediaId) })
            state.detail != null -> {
                val detail = state.detail!!
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 封面图
                    AsyncImage(
                        model = detail.thumbnailUrl.ifEmpty { detail.url },
                        contentDescription = detail.title,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── 操作栏 ──
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 收藏
                            IconButton(onClick = { viewModel.toggleFavorite() }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        if (state.isFavorite) Icons.Default.Favorite
                                        else Icons.Default.FavoriteBorder,
                                        "收藏",
                                        tint = if (state.isFavorite) Color(0xFFFF4081)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text("收藏", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            // 分享
                            IconButton(onClick = { viewModel.shareImage() }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Share, "分享", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("分享", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            // 下载
                            IconButton(onClick = { viewModel.downloadMedia() }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Download, "下载", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("下载", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    if (detail.type == "video") {
                        Button(
                            onClick = onPlayVideo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("播放视频")
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        // 标题
                        Text(detail.title, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))

                        // 描述
                        if (detail.description.isNotEmpty()) {
                            Text(
                                detail.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                        }

                        // 元信息
                        DetailInfoRow("类型", detail.type)
                        DetailInfoRow("作者", detail.author.ifEmpty { "未知" })
                        DetailInfoRow("来源", detail.source.ifEmpty { "未知" })
                        if (detail.width > 0) DetailInfoRow("分辨率", "${detail.width}×${detail.height}")
                        if (detail.fileSize > 0) DetailInfoRow("大小", formatFileSize(detail.fileSize))

                        // EXIF
                        detail.exif?.let { exif ->
                            Spacer(Modifier.height(16.dp))
                            Text("拍摄信息", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            if (exif.cameraMake.isNotEmpty()) DetailInfoRow("相机", "${exif.cameraMake} ${exif.cameraModel}")
                            if (exif.focalLength.isNotEmpty()) DetailInfoRow("焦距", exif.focalLength)
                            if (exif.aperture.isNotEmpty()) DetailInfoRow("光圈", exif.aperture)
                            if (exif.iso > 0) DetailInfoRow("ISO", exif.iso.toString())
                            if (exif.takenAt.isNotEmpty()) DetailInfoRow("拍摄时间", exif.takenAt)
                        }

                        // 标签
                        if (detail.tags.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text("标签", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            // FlowRow 显示标签
                            detail.tags.forEach { tag ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(tag.name) }
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}
