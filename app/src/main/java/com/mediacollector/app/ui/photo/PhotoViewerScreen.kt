package com.mediacollector.app.ui.photo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediacollector.app.ui.common.LoadingIndicator
import com.mediacollector.app.ui.common.ZoomableImage

/** 全屏照片查看器 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    mediaId: Int,
    onBack: () -> Unit,
    viewModel: PhotoViewerViewModel = hiltViewModel()
) {
    LaunchedEffect(mediaId) { viewModel.loadMedia(mediaId) }

    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { viewModel.toggleControls() }
    ) {
        when {
            state.isLoading -> LoadingIndicator(message = "")
            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.error!!, color = Color.White)
                }
            }
            state.mediaDetail != null -> {
                ZoomableImage(
                    model = state.mediaDetail!!.url,
                    contentDescription = state.mediaDetail!!.title,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 顶部控制栏（点击切换显示/隐藏）
        AnimatedVisibility(visible = state.showControls) {
            TopAppBar(
                title = { Text(state.mediaDetail?.title ?: "", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: 分享 */ }) {
                        Icon(Icons.Default.Share, "分享", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }
    }
}
