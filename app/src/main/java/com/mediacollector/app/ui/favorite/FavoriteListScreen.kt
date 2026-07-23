package com.mediacollector.app.ui.favorite

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mediacollector.app.ui.common.EmptyState
import com.mediacollector.app.ui.common.ErrorState
import com.mediacollector.app.ui.common.LoadingIndicator
import com.mediacollector.app.ui.common.SwipeToDeleteItem
import com.mediacollector.app.util.DateUtils

/** 收藏列表页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteListScreen(
    onMediaClick: (mediaId: Int, type: String) -> Unit,
    viewModel: FavoriteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("我的收藏") })
        }
    ) { padding ->
        when {
            state.isLoading && state.items.isEmpty() -> LoadingIndicator()
            state.error != null && state.items.isEmpty() -> ErrorState(
                state.error!!, onRetry = { viewModel.loadFavorites() }
            )
            state.items.isEmpty() -> EmptyState(
                message = "还没有收藏",
                icon = "❤️"
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.id }) { media ->
                        SwipeToDeleteItem(
                            onDelete = { viewModel.removeFavorite(media.id) }
                        ) {
                            FavoriteItem(
                                media = media,
                                onClick = { onMediaClick(media.id, media.type) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteItem(
    media: com.mediacollector.app.data.remote.dto.MediaItem,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图
            AsyncImage(
                model = media.thumbnailUrl.ifEmpty { media.url },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .padding(4.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    media.title.ifEmpty { "无标题" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    media.author.ifEmpty { "未知作者" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 收藏图标
            Icon(
                Icons.Default.Favorite,
                contentDescription = "收藏",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
