package com.mediacollector.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.mediacollector.app.ui.common.LoadingIndicator
import com.mediacollector.app.ui.media.components.MediaCard

/** 搜索页面 */
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

            // 搜索结果
            when {
                state.isLoading -> LoadingIndicator()
                state.error != null -> ErrorState(state.error!!)
                state.hasSearched && state.results.isEmpty() -> EmptyState("没有找到相关内容", "🔍")
                state.results.isNotEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.results, key = { it.id }) { media ->
                            MediaCard(
                                media = media,
                                onClick = { onMediaClick(media.id, media.type) }
                            )
                        }
                    }
                }
                else -> EmptyState("输入关键词开始搜索", "🔍")
            }
        }
    }
}
