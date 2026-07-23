package com.mediacollector.app.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mediacollector.app.ui.common.EmptyState
import com.mediacollector.app.util.DateUtils

/** 聊天室页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // 自动滚到底部
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("聊天室", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 连接状态指示器
                            ConnectionStatusDot(state.connectionState)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                when (state.connectionState) {
                                    MqttManager.ConnectionState.CONNECTED -> "已连接"
                                    MqttManager.ConnectionState.CONNECTING -> "连接中..."
                                    MqttManager.ConnectionState.RECONNECTING -> "重连中..."
                                    MqttManager.ConnectionState.DISCONNECTED -> "未连接"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.onlineCount > 0) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "在线: ${state.onlineCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (state.connectionState != MqttManager.ConnectionState.CONNECTED) {
                        IconButton(onClick = { viewModel.connectMqtt() }) {
                            Icon(Icons.Default.Refresh, "重连")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // ── 消息列表 ──
            if (state.messages.isEmpty()) {
                EmptyState(
                    message = if (state.connectionState == MqttManager.ConnectionState.CONNECTED)
                        "开始聊天吧！" else "连接中...",
                    icon = if (state.connectionState == MqttManager.ConnectionState.CONNECTED) "💬" else "🔄",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.messages, key = { it.messageId }) { message ->
                        ChatBubble(
                            message = message,
                            isMe = message.senderUser == state.username
                        )
                    }
                }
            }

            // ── 输入区 ──
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = { viewModel.updateInputText(it) },
                        placeholder = { Text("输入消息...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    FilledIconButton(
                        onClick = { viewModel.sendTextMessage() },
                        enabled = state.inputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, "发送")
                    }
                }
            }
        }
    }
}

/** 连接状态圆点 */
@Composable
private fun ConnectionStatusDot(state: MqttManager.ConnectionState) {
    val color by animateColorAsState(
        targetValue = when (state) {
            MqttManager.ConnectionState.CONNECTED -> Color(0xFF4CAF50)
            MqttManager.ConnectionState.CONNECTING -> Color(0xFFFFC107)
            MqttManager.ConnectionState.RECONNECTING -> Color(0xFFFF9800)
            MqttManager.ConnectionState.DISCONNECTED -> Color(0xFFF44336)
        },
        label = "status"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/** 聊天气泡 */
@Composable
private fun ChatBubble(
    message: ChatMessage,
    isMe: Boolean
) {
    when (message) {
        is ChatMessage.SystemMessage -> {
            // 系统消息居中显示
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Text(
                        message.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        is ChatMessage.TextMessage -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                // 发送者名字
                Text(
                    message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    ),
                    color = if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            message.decryptedContent,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            DateUtils.formatRelative(message.timestamp * 1000),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        is ChatMessage.ImageMessage -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                Text(
                    message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "图片消息",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .heightIn(max = 240.dp)
                    )
                }
            }
        }
    }
}
