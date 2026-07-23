package com.mediacollector.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** 设置主页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onServerConfig: () -> Unit,
    onCacheManage: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // API 服务器配置
            SettingsItem(
                icon = Icons.Default.Dns,
                title = "API 服务器配置",
                onClick = onServerConfig
            )
            // 缓存管理
            SettingsItem(
                icon = Icons.Default.Storage,
                title = "缓存管理",
                onClick = onCacheManage
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // 深色模式
            val settingsState by viewModel.settingsState.collectAsState()
            SettingsItem(
                icon = if (settingsState.darkMode.name == "DARK") Icons.Default.DarkMode
                       else Icons.Default.LightMode,
                title = "深色模式",
                subtitle = when (settingsState.darkMode) {
                    com.mediacollector.app.data.settings.DarkMode.SYSTEM -> "跟随系统"
                    com.mediacollector.app.data.settings.DarkMode.LIGHT -> "始终亮色"
                    com.mediacollector.app.data.settings.DarkMode.DARK -> "始终深色"
                },
                onClick = {
                    val next = when (settingsState.darkMode) {
                        com.mediacollector.app.data.settings.DarkMode.SYSTEM -> com.mediacollector.app.data.settings.DarkMode.DARK
                        com.mediacollector.app.data.settings.DarkMode.DARK -> com.mediacollector.app.data.settings.DarkMode.LIGHT
                        com.mediacollector.app.data.settings.DarkMode.LIGHT -> com.mediacollector.app.data.settings.DarkMode.SYSTEM
                    }
                    viewModel.saveDarkMode(next)
                }
            )

            // 仅 WiFi 下载
            SettingsSwitchItem(
                icon = Icons.Default.Wifi,
                title = "仅 WiFi 下载",
                checked = settingsState.downloadOnlyWifi,
                onCheckedChange = { viewModel.saveDownloadOnlyWifi(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // 关于
            val versionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (_: Exception) { "2.0.0" }
            SettingsItem(
                icon = Icons.Default.Info,
                title = "关于",
                subtitle = "v$versionName",
                onClick = onAbout
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    Surface(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
