package com.mediacollector.app.ui.chat

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MQTT 连接管理器
 *
 * 使用同步 MqttClient + 协程实现更可靠的连接管理。
 * 支持主备 Broker 自动切换，连接失败后指数退避自动重连。
 */
@Singleton
class MqttManager @Inject constructor() {

    companion object {
        private const val TAG = "MqttManager"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private var client: MqttClient? = null
    private var currentRoomId: String = "lobby"
    private var currentUsername: String = ""
    private var currentDisplayName: String = ""
    private var useFallbackBroker = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── 公开状态流 ──

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<ChatMessage> = _messages.asSharedFlow()

    private var reconnectDelay = MqttConfig.RECONNECT_BASE_DELAY
    private var reconnectJob: Job? = null
    private var connectJob: Job? = null

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
    }

    /**
     * 获取当前使用的 Broker URL
     */
    fun getCurrentBrokerUrl(): String {
        return if (useFallbackBroker) MqttConfig.BROKER_URL_FALLBACK
        else MqttConfig.BROKER_URL
    }

    /**
     * 连接 MQTT Broker（同步连接，自动重连，支持主备切换）
     */
    fun connect(
        username: String,
        displayName: String,
        roomId: String = "lobby"
    ) {
        if (connectJob?.isActive == true) return

        currentUsername = username
        currentDisplayName = displayName
        currentRoomId = roomId
        _connectionState.value = ConnectionState.CONNECTING

        connectJob = scope.launch {
            doConnect()
        }
    }

    /**
     * 实际连接逻辑（在协程中运行）
     */
    private suspend fun doConnect() {
        val brokerUrl = getCurrentBrokerUrl()
        try {
            // 关闭旧连接
            disconnectClient()

            val clientId = "${MqttConfig.CLIENT_ID_PREFIX}${UUID.randomUUID().toString().take(8)}"

            val mqttClient = MqttClient(
                brokerUrl,
                clientId,
                MemoryPersistence()
            )

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = MqttConfig.CONNECTION_TIMEOUT
                keepAliveInterval = MqttConfig.HEARTBEAT_INTERVAL.toInt()
                isAutomaticReconnect = false
            }

            // 设置回调（必须在 connect 之前）
            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "连接断开: ${cause?.message}")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    scheduleReconnect()
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    handleMessage(topic, message)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // 消息已送达
                }
            })

            // 同步连接（阻塞当前协程直到连接成功或失败）
            withContext(Dispatchers.IO) {
                mqttClient.connect(options)
            }

            client = mqttClient
            reconnectDelay = MqttConfig.RECONNECT_BASE_DELAY
            _connectionState.value = ConnectionState.CONNECTED

            Log.i(TAG, "连接成功: $brokerUrl")

            // 订阅主题
            subscribeToRoom(currentRoomId)

            // 发送上线通知
            publishSystemMessage("$currentDisplayName 已加入聊天室")

        } catch (e: Exception) {
            Log.e(TAG, "连接失败 ($brokerUrl): ${e.message}")

            // 如果主 Broker 失败，尝试切换到备用
            if (!useFallbackBroker) {
                Log.i(TAG, "尝试切换到备用 Broker: ${MqttConfig.BROKER_URL_FALLBACK}")
                useFallbackBroker = true
                _connectionState.value = ConnectionState.CONNECTING
                doConnect() // 立即尝试备用
                return
            }

            // 备用也失败，切换回主 Broker 等下次重连
            useFallbackBroker = false
            _connectionState.value = ConnectionState.DISCONNECTED
            client = null
            scheduleReconnect()
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        reconnectJob?.cancel()
        connectJob?.cancel()
        disconnectClient()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun disconnectClient() {
        try {
            client?.disconnect()
            client?.close()
        } catch (_: Exception) { }
        client = null
    }

    /**
     * 订阅聊天室主题
     */
    fun subscribeToRoom(roomId: String) {
        currentRoomId = roomId
        val topic = MqttConfig.getTopic(roomId)
        try {
            client?.subscribe(topic, MqttConfig.QOS)
            Log.i(TAG, "已订阅: $topic")
        } catch (e: Exception) {
            Log.e(TAG, "订阅失败: ${e.message}")
        }
    }

    /**
     * 发送文本消息（AES 加密后发送）
     */
    fun sendTextMessage(text: String, senderUser: String, senderName: String, roomId: String = currentRoomId) {
        val encrypted = ChatCrypto.encrypt(text)
        val message = ChatMessage.TextMessage(
            roomId = roomId,
            senderUser = senderUser,
            senderName = senderName,
            encryptedContent = encrypted,
            timestamp = System.currentTimeMillis() / 1000
        )
        publishMessage(message)
    }

    /**
     * 发送图片消息（URL 明文传输）
     */
    fun sendImageMessage(imageUrl: String, senderUser: String, senderName: String, roomId: String = currentRoomId) {
        val message = ChatMessage.ImageMessage(
            roomId = roomId,
            senderUser = senderUser,
            senderName = senderName,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis() / 1000
        )
        publishMessage(message)
    }

    /**
     * 发布消息到 MQTT
     */
    private fun publishMessage(message: ChatMessage) {
        scope.launch {
            try {
                val payload = message.toMqttPayload()
                val payloadStr = json.encodeToString(payload)
                val topic = MqttConfig.getTopic(message.roomId)

                val mqttMessage = MqttMessage(payloadStr.toByteArray()).apply {
                    qos = MqttConfig.QOS
                }

                client?.publish(topic, mqttMessage)
                _messages.tryEmit(message)

            } catch (e: Exception) {
                Log.e(TAG, "发送失败: ${e.message}")
            }
        }
    }

    /**
     * 发送系统消息（上线/下线通知）
     */
    private fun publishSystemMessage(content: String) {
        scope.launch {
            try {
                val message = ChatMessage.SystemMessage(
                    roomId = currentRoomId,
                    content = content,
                    timestamp = System.currentTimeMillis() / 1000
                )
                val payload = message.toMqttPayload()
                val topic = MqttConfig.getTopic(message.roomId)
                val mqttMessage = MqttMessage(json.encodeToString(payload).toByteArray()).apply {
                    qos = MqttConfig.QOS
                }
                client?.publish(topic, mqttMessage)
            } catch (_: Exception) { }
        }
    }

    /**
     * 处理收到的 MQTT 消息
     */
    private fun handleMessage(topic: String, message: MqttMessage) {
        try {
            val payloadStr = String(message.payload)
            val payload = json.decodeFromString<MqttPayload>(payloadStr)
            val chatMessage = payload.toChatMessage()
            _messages.tryEmit(chatMessage)
        } catch (e: Exception) {
            Log.w(TAG, "消息解析失败: ${e.message}")
        }
    }

    /**
     * 指数退避自动重连
     */
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            if (_connectionState.value == ConnectionState.CONNECTED) return@launch

            _connectionState.value = ConnectionState.RECONNECTING
            delay(reconnectDelay * 1000)

            Log.i(TAG, "尝试重连 (延迟 ${reconnectDelay}s)...")
            doConnect()

            reconnectDelay = (reconnectDelay * 2).coerceAtMost(MqttConfig.RECONNECT_MAX_DELAY)
        }
    }

    /** 清理 */
    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
