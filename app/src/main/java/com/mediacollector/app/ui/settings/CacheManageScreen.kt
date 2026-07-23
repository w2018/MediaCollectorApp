package com.mediacollector.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediacollector.app.domain.CacheManager

/** 缓存管理页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheManageScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val cacheState by viewModel.cacheState.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshCache() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("缓存管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        },
        snackbarHost = {
            cacheState.clearedMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearClearedMessage() }) { Text("关闭") }
                    }
                ) { Text(msg) }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            // 存储占用
            Text("存储占用", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            val info = cacheState.cacheInfo

            CacheSizeRow("图片缓存", info.coilCacheSize, info.totalSize)
            CacheSizeRow("本地缩略图", info.thumbnailCacheSize, info.totalSize)
            CacheSizeRow("数据库", info.databaseSize, info.totalSize)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            CacheSizeRow("总计", info.totalSize, info.totalSize, bold = true)

            Spacer(Modifier.height(24.dp))

            // 清理按钮
            Button(
                onClick = { showClearDialog = true },
                enabled = info.totalSize > 0 && !cacheState.isClearing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (cacheState.isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Icon(Icons.Default.DeleteSweep, null)
                }
                Spacer(Modifier.width(8.dp))
                Text("清理所有缓存")
            }

            Spacer(Modifier.height(24.dp))

            // 缓存策略
            Text("缓存策略", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            listOf(
                com.mediacollector.app.data.settings.CacheStrategy.WIFI_ONLY to "仅 WiFi 下缓存",
                com.mediacollector.app.data.settings.CacheStrategy.ALWAYS to "始终缓存",
                com.mediacollector.app.data.settings.CacheStrategy.DISABLED to "不缓存"
            ).forEach { (strategy, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settingsState.cacheStrategy == strategy,
                        onClick = { viewModel.saveCacheStrategy(strategy) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // 确认清理弹窗
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清理缓存") },
            text = { Text("确定要清理所有缓存吗？这将清除图片缓存和缩略图，但不会影响收藏和历史记录。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllCache()
                    showClearDialog = false
                }) { Text("清理") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun CacheSizeRow(label: String, size: Long, total: Long, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(100.dp)
        )
        // 进度条
        LinearProgressIndicator(
            progress = { if (total > 0) size.toFloat() / total else 0f },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(8.dp),
        )
        Text(
            formatSize(size),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(70.dp)
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}
