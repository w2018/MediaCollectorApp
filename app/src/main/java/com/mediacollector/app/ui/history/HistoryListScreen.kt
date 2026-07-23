package com.mediacollector.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
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

/** 浏览历史页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryListScreen(
    onMediaClick: (mediaId: Int, type: String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("浏览记录") },
                actions = {
                    if (state.items.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "清空全部")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading && state.items.isEmpty() -> LoadingIndicator()
            state.error != null && state.items.isEmpty() -> ErrorState(
                state.error!!, onRetry = { viewModel.loadHistory() }
            )
            state.items.isEmpty() -> EmptyState(
                message = "还没有浏览记录",
                icon = "🕐"
            )
            else -> {
                // 按日期分组
                val grouped = state.items.groupBy { record ->
                    com.mediacollector.app.util.DateUtils.fromDbString(record.watchedAt)
                        .let { DateUtils.groupDate(it) }
                }

                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (date, records) ->
                        item {
                            Text(
                                date,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(records, key = { it.mediaId }) { record ->
                            SwipeToDeleteItem(
                                onDelete = { viewModel.removeHistory(record.mediaId) }
                            ) {
                                HistoryItem(
                                    record = record,
                                    onClick = {
                                        onMediaClick(record.mediaId, record.mediaType)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 清空确认弹窗
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空浏览记录") },
            text = { Text("确定要清空所有浏览记录吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showClearDialog = false
                }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryItem(
    record: com.mediacollector.app.data.remote.dto.HistoryRecord,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = record.media?.thumbnailUrl ?: "",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .padding(4.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    record.media?.title ?: "未知",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    DateUtils.formatRelative(DateUtils.fromDbString(record.watchedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 类型标签
            if (record.mediaType == "video") {
                AssistChip(
                    onClick = {},
                    label = { Text("视频", style = MaterialTheme.typography.labelSmall) }
                )
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}
