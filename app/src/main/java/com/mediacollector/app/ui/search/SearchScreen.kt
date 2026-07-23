package com.mediacollector.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediacollector.app.ui.common.EmptyState
import com.mediacollector.app.ui.common.ErrorState
import com.mediacollector.app.ui.common.LoadMoreIndicator
import com.mediacollector.app.ui.common.LoadingIndicator
import com.mediacollector.app.ui.media.components.MediaCard
import com.mediacollector.app.ui.photo.BrowsingContext

/** 搜索页面（支持图片/视频分类切换） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMediaClick: (mediaId: Int, type: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 搜索框
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text("搜索标题、描述...") },
                singleLine = true,
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, "清除")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            // 分类选项卡
            if (state.hasSearched && state.results.isNotEmpty()) {
                TabRow(
                    selectedTabIndex = when (state.filterType) {
                        "photo" -> 1
                        "video" -> 2
                        else -> 0
                    }
                ) {
                    Tab(
                        selected = state.filterType == "all",
                        onClick = { viewModel.setFilter("all") },
                        text = { Text("全部 (${state.results.size})") }
                    )
                    Tab(
                        selected = state.filterType == "photo",
                        onClick = { viewModel.setFilter("photo") },
                        text = { Text("图片") }
                    )
                    Tab(
                        selected = state.filterType == "video",
                        onClick = { viewModel.setFilter("video") },
                        text = { Text("视频") }
                    )
                }
            }

            // 搜索结果
            when {
                state.isLoading -> LoadingIndicator()
                state.error != null -> ErrorState(state.error!!)
                state.hasSearched && state.results.isEmpty() -> EmptyState("没有找到相关内容", "🔍")
                state.filteredResults.isNotEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.filteredResults, key = { it.id }) { media ->
                            MediaCard(
                                media = media,
                                onClick = {
                                    BrowsingContext.mediaIds = state.filteredResults.map { it.id }
                                    onMediaClick(media.id, media.type)
                                }
                            )
                        }

                        // 加载更多
                        if (state.isLoadingMore) {
                            item { LoadMoreIndicator() }
                        }
                        if (state.results.isNotEmpty() && state.currentPage < state.totalPages) {
                            item { LaunchedEffect(state.results.size) { viewModel.loadMore() } }
                        }
                    }
                }
                state.hasSearched && state.filteredResults.isEmpty() && state.results.isNotEmpty() ->
                    EmptyState("当前分类没有结果", "🔍")
                else -> EmptyState("输入关键词开始搜索", "🔍")
            }
        }
    }
}
