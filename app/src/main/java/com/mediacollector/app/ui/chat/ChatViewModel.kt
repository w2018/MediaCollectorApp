package com.mediacollector.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.local.entity.ChatMessageEntity
import com.mediacollector.app.data.remote.dto.ChatSyncRequest
import com.mediacollector.app.data.remote.dto.OnlineUser
import com.mediacollector.app.data.repository.ChatRepository
import com.mediacollector.app.data.settings.AuthStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val connectionState: MqttManager.ConnectionState = MqttManager.ConnectionState.DISCONNECTED,
    val onlineCount: Int = 0,
    val onlineUsers: List<OnlineUser> = emptyList(),
    val currentRoomId: String = "lobby",
    val username: String = "",
    val displayName: String = "",
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val mqttManager: MqttManager,
    private val chatRepository: ChatRepository,
    private val authStore: AuthStore
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var heartbeatJob: Job? = null
    private var onlinePollJob: Job? = null
    private var messagePollJob: Job? = null

    init {
        loadUserInfo()
        observeMqttMessages()
        observeConnectionState()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val username = authStore.username.first()
            val displayName = authStore.displayName.first()
            val isLoggedIn = authStore.isLoggedIn()
            _state.value = _state.value.copy(
                username = username,
                displayName = displayName,
                isLoggedIn = isLoggedIn
            )

            // 加载本地消息
            loadLocalMessages()

            // 连接 MQTT
            if (isLoggedIn) {
                connectMqtt()
                // 从服务端拉取历史消息
                loadRemoteMessages()
                startHeartbeat()
                startOnlinePolling()
                startMessagePolling()
            }
        }
    }

    private fun loadLocalMessages() {
        viewModelScope.launch {
            chatRepository.getLocalMessages(_state.value.currentRoomId).collect { entities ->
                val messages = entities.map { entity ->
                    when (entity.type) {
                        "image" -> ChatMessage.ImageMessage(
                            messageId = entity.message_id,
                            roomId = entity.room_id,
                            senderUser = entity.sender_user,
                            senderName = entity.sender_name,
                            imageUrl = entity.content,
                            timestamp = entity.timestamp
                        )
                        "system" -> ChatMessage.SystemMessage(
                            messageId = entity.message_id,
                            roomId = entity.room_id,
                            content = entity.content,
                            timestamp = entity.timestamp
                        )
                        else -> ChatMessage.TextMessage(
                            messageId = entity.message_id,
                            roomId = entity.room_id,
                            senderUser = entity.sender_user,
                            senderName = entity.sender_name,
                            encryptedContent = entity.content,
                            timestamp = entity.timestamp
                        )
                    }
                }
                _state.value = _state.value.copy(messages = messages)
            }
        }
    }

    private fun observeMqttMessages() {
        viewModelScope.launch {
            mqttManager.messages.collect { message ->
                // 保存到本地
                saveMessageToLocal(message)
                // 同步到 API 服务端
                syncMessageToApi(message)
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            mqttManager.connectionState.collect { state ->
                _state.value = _state.value.copy(connectionState = state)
            }
        }
    }

    fun connectMqtt() {
        val s = _state.value
        if (s.username.isNotBlank()) {
            mqttManager.connect(s.username, s.displayName, s.currentRoomId)
        }
    }

    fun sendTextMessage() {
        val s = _state.value
        val text = s.inputText.trim()
        if (text.isEmpty()) return

        mqttManager.sendTextMessage(text, s.username, s.displayName, s.currentRoomId)
        _state.value = s.copy(inputText = "")
    }

    fun updateInputText(text: String) {
        _state.value = _state.value.copy(inputText = text)
    }

    fun sendImage(imageUrl: String) {
        val s = _state.value
        mqttManager.sendImageMessage(imageUrl, s.username, s.displayName, s.currentRoomId)
    }

    /** 从服务端拉取历史消息并合并到本地 */
    private fun loadRemoteMessages() {
        viewModelScope.launch {
            val s = _state.value
            val result = chatRepository.getRemoteMessages(s.currentRoomId)
            result.onSuccess { paginated ->
                val entities = paginated.items.map { dto ->
                    ChatMessageEntity(
                        message_id = dto.messageId,
                        room_id = dto.roomId,
                        sender_user = dto.senderUser,
                        sender_name = dto.senderName,
                        type = dto.type,
                        content = dto.content,
                        timestamp = dto.timestamp
                    )
                }
                if (entities.isNotEmpty()) {
                    chatRepository.saveMessages(entities)
                }
            }
        }
    }

    fun changeRoom(roomId: String) {
        _state.value = _state.value.copy(currentRoomId = roomId)
        mqttManager.subscribeToRoom(roomId)
        loadLocalMessages()
    }

    /** 心跳：每 30 秒通知 API 自己在线 */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(MqttConfig.HEARTBEAT_INTERVAL * 1000)
                val s = _state.value
                if (s.isLoggedIn) {
                    chatRepository.heartbeat(s.currentRoomId)
                }
            }
        }
    }

    /** 在线用户轮询 */
    private fun startOnlinePolling() {
        onlinePollJob?.cancel()
        onlinePollJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000) // 每 15 秒刷新在线列表
                val s = _state.value
                if (s.isLoggedIn) {
                    chatRepository.getOnlineUsers(s.currentRoomId).fold(
                        onSuccess = { users ->
                            _state.value = _state.value.copy(onlineUsers = users)
                        },
                        onFailure = { }
                    )
                    chatRepository.getOnlineCount(s.currentRoomId).fold(
                        onSuccess = { count ->
                            _state.value = _state.value.copy(onlineCount = count)
                        },
                        onFailure = { }
                    )
                }
            }
        }
    }

    /** 保存消息到本地 Room */
    private fun saveMessageToLocal(message: ChatMessage) {
        viewModelScope.launch {
            val entity = when (message) {
                is ChatMessage.TextMessage -> ChatMessageEntity(
                    message_id = message.messageId,
                    room_id = message.roomId,
                    sender_user = message.senderUser,
                    sender_name = message.senderName,
                    type = "text",
                    content = message.encryptedContent,
                    timestamp = message.timestamp
                )
                is ChatMessage.ImageMessage -> ChatMessageEntity(
                    message_id = message.messageId,
                    room_id = message.roomId,
                    sender_user = message.senderUser,
                    sender_name = message.senderName,
                    type = "image",
                    content = message.imageUrl,
                    timestamp = message.timestamp
                )
                is ChatMessage.SystemMessage -> ChatMessageEntity(
                    message_id = message.messageId,
                    room_id = message.roomId,
                    sender_user = "system",
                    sender_name = "系统",
                    type = "system",
                    content = message.content,
                    timestamp = message.timestamp
                )
            }
            chatRepository.saveMessage(entity)
        }
    }

    /** 同步消息到 API 服务端 */
    private fun syncMessageToApi(message: ChatMessage) {
        viewModelScope.launch {
            val s = _state.value
            if (!s.isLoggedIn) return@launch

            val request = when (message) {
                is ChatMessage.TextMessage -> ChatSyncRequest(
                    messageId = message.messageId,
                    roomId = message.roomId,
                    senderUser = message.senderUser,
                    senderName = message.senderName,
                    type = "text",
                    content = message.encryptedContent,
                    timestamp = message.timestamp
                )
                is ChatMessage.ImageMessage -> ChatSyncRequest(
                    messageId = message.messageId,
                    roomId = message.roomId,
                    senderUser = message.senderUser,
                    senderName = message.senderName,
                    type = "image",
                    content = message.imageUrl,
                    timestamp = message.timestamp
                )
                is ChatMessage.SystemMessage -> return@launch // 系统消息不同步到 API
            }
            chatRepository.syncMessage(request)
        }
    }

    /** 定时轮询服务端消息（确保即使 MQTT 断开也能同步） */
    private fun startMessagePolling() {
        messagePollJob?.cancel()
        messagePollJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000) // 每 30 秒
                val s = _state.value
                if (s.isLoggedIn) {
                    loadRemoteMessages()
                }
            }
        }
    }

    /** 退出登录时清除聊天数据 */
    fun onLogout() {
        viewModelScope.launch {
            mqttManager.disconnect()
            // 不清除服务端消息，以便下次登录能恢复聊天记录
            // 只清本地缓存，下次登录会从 API 重新拉取
            chatRepository.clearAllLocalMessages()
            heartbeatJob?.cancel()
            onlinePollJob?.cancel()
            messagePollJob?.cancel()
            _state.value = ChatUiState()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mqttManager.destroy()
        heartbeatJob?.cancel()
        onlinePollJob?.cancel()
        messagePollJob?.cancel()
    }
}
