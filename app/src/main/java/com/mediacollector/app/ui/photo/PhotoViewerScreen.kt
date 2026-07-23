package com.mediacollector.app.ui.photo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediacollector.app.ui.common.LoadingIndicator
import com.mediacollector.app.ui.common.ZoomableImage

/** 全屏照片查看器（支持左右滑动切换） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    mediaId: Int,
    onBack: () -> Unit,
    viewModel: PhotoViewerViewModel = hiltViewModel()
) {
    // 加载初始媒体
    LaunchedEffect(mediaId) { viewModel.loadMedia(mediaId) }

    val state by viewModel.state.collectAsState()
    val mediaIds = viewModel.getMediaIdList()
    val totalCount = mediaIds.size

    // 计算当前页索引
    val initialPage = maxOf(0, mediaIds.indexOf(mediaId))

    // 分页状态 — 用 mediaIds 列表驱动页数
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { maxOf(1, totalCount) }
    )

    // 页面切换时加载对应的媒体
    LaunchedEffect(pagerState.currentPage) {
        if (totalCount > 0 && pagerState.currentPage < totalCount) {
            viewModel.loadMedia(mediaIds[pagerState.currentPage])
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            state.isLoading && state.mediaDetail == null -> LoadingIndicator(message = "")
            state.error != null && state.mediaDetail == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BrokenImage, contentDescription = null,
                            tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(state.error!!, color = Color.Gray)
                    }
                }
            }
            else -> {
                // HorizontalPager 实现左右滑动切换
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    // 当前页加载图片，相邻页直接显示 URL
                    val isCurrentPage = page == pagerState.currentPage
                    val pageMediaId = if (page < totalCount) mediaIds[page] else 0
                    val pageUrl = viewModel.getUrlForMedia(pageMediaId)

                    val url = if (pageUrl.isNotEmpty()) pageUrl
                              else if (isCurrentPage && state.mediaDetail != null) state.mediaDetail!!.url
                              else ""
                    if (url.isNotEmpty()) {
                        ZoomableImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 控制栏（点击切换显隐）
                if (state.showControls) {
                    // 顶部栏
                    TopAppBar(
                        title = {
                            Text(
                                state.mediaDetail?.title ?: "",
                                color = Color.White,
                                maxLines = 1
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.toggleFavorite() }) {
                                Icon(
                                    if (state.isFavorite) Icons.Default.Favorite
                                    else Icons.Default.FavoriteBorder,
                                    "收藏",
                                    tint = if (state.isFavorite) Color(0xFFFF4081) else Color.White
                                )
                            }
                            IconButton(onClick = { viewModel.shareImage() }) {
                                Icon(Icons.Default.Share, "分享", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        )
                    )
                }

                // 页码指示器
                if (state.showControls && totalCount > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "${pagerState.currentPage + 1} / $totalCount",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
