package com.mediacollector.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediacollector.app.domain.ApiCompatChecker
import com.mediacollector.app.domain.ApiCompatChecker.OverallStatus
import com.mediacollector.app.domain.ApiCompatChecker.CheckStatus

/** API 兼容性检测页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiCheckScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { viewModel.checkApi() }
    val state by viewModel.apiCheckState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 检测") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Text(state.error!!)
                        }
                    }
                }
                state.result != null -> {
                    val result = state.result!!

                    // 总体状态
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (result.overallStatus) {
                                OverallStatus.FULLY_COMPATIBLE -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                OverallStatus.PARTIALLY_COMPATIBLE -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                OverallStatus.INCOMPATIBLE -> Color(0xFFF44336).copy(alpha = 0.15f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (result.overallStatus) {
                                    OverallStatus.FULLY_COMPATIBLE -> Icons.Default.CheckCircle
                                    OverallStatus.PARTIALLY_COMPATIBLE -> Icons.Default.Warning
                                    OverallStatus.INCOMPATIBLE -> Icons.Default.Error
                                },
                                null,
                                tint = when (result.overallStatus) {
                                    OverallStatus.FULLY_COMPATIBLE -> Color(0xFF4CAF50)
                                    OverallStatus.PARTIALLY_COMPATIBLE -> Color(0xFFFF9800)
                                    OverallStatus.INCOMPATIBLE -> Color(0xFFF44336)
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    when (result.overallStatus) {
                                        OverallStatus.FULLY_COMPATIBLE -> "完全兼容"
                                        OverallStatus.PARTIALLY_COMPATIBLE -> "基本兼容"
                                        OverallStatus.INCOMPATIBLE -> "无法连接"
                                    }
                                )
                                if (result.serverName.isNotEmpty()) {
                                    Text(result.serverName, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("媒体总数: ${result.mediaCount}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 各端点状态
                    Text("端点检测", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    result.checks.forEach { check ->
                        CheckItemRow(check)
                        Spacer(Modifier.height(4.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(onClick = { viewModel.checkApi() }, modifier = Modifier.fillMaxWidth()) {
                        Text("重新检测")
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckItemRow(check: ApiCompatChecker.EndpointCheck) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (check.status) {
                    CheckStatus.OK -> Icons.Default.CheckCircle
                    CheckStatus.WARN -> Icons.Default.Warning
                    CheckStatus.FAIL -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (check.status) {
                    CheckStatus.OK -> Color(0xFF4CAF50)
                    CheckStatus.WARN -> Color(0xFFFF9800)
                    CheckStatus.FAIL -> Color(0xFFF44336)
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(check.name, style = MaterialTheme.typography.bodyMedium)
                if (check.error.isNotEmpty()) {
                    Text(check.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            if (check.statusCode > 0) {
                Text("${check.statusCode}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
