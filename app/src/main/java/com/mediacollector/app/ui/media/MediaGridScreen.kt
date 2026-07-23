package com.mediacollector.app.ui.media

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediacollector.app.ui.common.EmptyState
import com.mediacollector.app.ui.common.ErrorState
import com.mediacollector.app.ui.common.LoadMoreIndicator
import com.mediacollector.app.ui.common.LoadingIndicator
import com.mediacollector.app.ui.media.components.MediaCard

/** 媒体网格列表 - 首页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGridScreen(
    onMediaClick: (mediaId: Int, type: String) -> Unit,
    onSearchClick: () -> Unit,
    onCollectionsClick: () -> Unit,
    viewModel: MediaGridViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("媒体") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = onCollectionsClick) {
                        Icon(Icons.Default.Collections, contentDescription = "集合")
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // ── 筛选栏 ──
            FilterBar(
                currentFilter = state.filterType,
                onFilterChange = { viewModel.setFilter(it) }
            )

            // ── 内容区 ──
            when {
                state.isLoading && state.items.isEmpty() -> LoadingIndicator()
                state.error != null && state.items.isEmpty() -> ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.refresh() }
                )
                state.items.isEmpty() -> EmptyState(
                    message = "还没有媒体内容",
                    icon = "📸"
                )
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.items, key = { it.id }) { media ->
                            MediaCard(
                                media = media,
                                onClick = { onMediaClick(media.id, media.type) },
                                isFavorite = state.favorites[media.id] ?: false,
                                onFavoriteClick = { viewModel.toggleFavorite(media.id, state.favorites[media.id] ?: false) }
                            )
                        }

                        // 加载更多
                        if (state.isLoadingMore) {
                            item { LoadMoreIndicator() }
                        }

                        // 滚动到底部触发加载更多
                        if (state.items.isNotEmpty() && state.currentPage < state.totalPages) {
                            item {
                                LaunchedEffect(state.items.size) {
                                    viewModel.loadMore()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    currentFilter: String?,
    onFilterChange: (String?) -> Unit
) {
    val filters = listOf(null to "全部", "photo" to "照片", "video" to "视频")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (value, label) ->
            FilterChip(
                selected = currentFilter == value,
                onClick = { onFilterChange(value) },
                label = { Text(label, style = MaterialTheme.typography.labelLarge) }
            )
        }
    }
}
