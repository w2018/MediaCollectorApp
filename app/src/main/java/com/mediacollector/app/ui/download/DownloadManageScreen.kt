package com.mediacollector.app.ui.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediacollector.app.ui.common.EmptyState

/** 下载管理页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManageScreen(
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val allItems by viewModel.state.collectAsState()
    val downloadingItems = allItems.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
    val completedItems = allItems.filter { it.status == DownloadStatus.COMPLETED }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("下载管理") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tab
            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf("下载中", "已完成")

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> { // 下载中
                    if (downloadingItems.isEmpty()) {
                        EmptyState("没有正在下载的任务", "📥")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(downloadingItems, key = { it.mediaId }) { item ->
                                DownloadingItem(
                                    item = item,
                                    onCancel = { viewModel.cancelDownload(item.mediaId) }
                                )
                            }
                        }
                    }
                }
                1 -> { // 已完成
                    if (completedItems.isEmpty()) {
                        EmptyState("还没有下载完成的内容", "✅")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(completedItems, key = { it.mediaId }) { item ->
                                CompletedItem(
                                    item = item,
                                    onDelete = { viewModel.deleteDownloaded(item.mediaId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadingItem(item: DownloadItem, onCancel: () -> Unit) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (item.title.contains("video", true)) Icons.Default.VideoFile else Icons.Default.Image,
                    null
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    LinearProgressIndicator(
                        progress = { item.progress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, "取消")
                }
            }
        }
    }
}

@Composable
private fun CompletedItem(item: DownloadItem, onDelete: () -> Unit) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text("下载完成", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "删除")
            }
        }
    }
}
