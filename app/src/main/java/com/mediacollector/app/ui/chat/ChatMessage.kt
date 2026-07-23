package com.mediacollector.app.ui.chat

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * MQTT 聊天消息密封类
 */
sealed class ChatMessage {

    /** 消息类型 */
    abstract val messageId: String
    abstract val roomId: String
    abstract val senderUser: String
    abstract val senderName: String
    abstract val timestamp: Long

    /** 文本消息（内容已加密） */
    data class TextMessage(
        override val messageId: String = UUID.randomUUID().toString(),
        override val roomId: String = "lobby",
        override val senderUser: String = "",
        override val senderName: String = "",
        val encryptedContent: String = "",  // AES 加密后的 Base64
        override val timestamp: Long = System.currentTimeMillis() / 1000
    ) : ChatMessage() {
        /** 解密后的文本 */
        val decryptedContent: String
            get() = ChatCrypto.decrypt(encryptedContent)
    }

    /** 图片消息（URL 不加密） */
    data class ImageMessage(
        override val messageId: String = UUID.randomUUID().toString(),
        override val roomId: String = "lobby",
        override val senderUser: String = "",
        override val senderName: String = "",
        val imageUrl: String = "",
        override val timestamp: Long = System.currentTimeMillis() / 1000
    ) : ChatMessage()

    /** 系统消息 */
    data class SystemMessage(
        override val messageId: String = UUID.randomUUID().toString(),
        override val roomId: String = "lobby",
        override val senderUser: String = "system",
        override val senderName: String = "系统",
        val content: String = "",
        override val timestamp: Long = System.currentTimeMillis() / 1000
    ) : ChatMessage()
}

/**
 * MQTT JSON 消息体
 *
 * 统一格式:
 * {
 *   "type": "text" | "image" | "system",
 *   "data": { ... } // 不同类型不同结构
 * }
 */
@Serializable
data class MqttPayload(
    val type: String, // text / image / system
    val data: MqttData
)

@Serializable
data class MqttData(
    val messageId: String = "",
    val roomId: String = "lobby",
    val senderUser: String = "",
    val senderName: String = "",
    val content: String = "",       // text: 加密内容; system: 消息内容
    val imageUrl: String = "",      // image: 图片 URL
    val timestamp: Long = System.currentTimeMillis() / 1000
)

/**
 * 将 MqttPayload 转为密封类 ChatMessage
 */
fun MqttPayload.toChatMessage(): ChatMessage {
    val d = data
    return when (type) {
        "text" -> ChatMessage.TextMessage(
            messageId = d.messageId.ifEmpty { java.util.UUID.randomUUID().toString() },
            roomId = d.roomId,
            senderUser = d.senderUser,
            senderName = d.senderName,
            encryptedContent = d.content,
            timestamp = d.timestamp
        )
        "image" -> ChatMessage.ImageMessage(
            messageId = d.messageId.ifEmpty { java.util.UUID.randomUUID().toString() },
            roomId = d.roomId,
            senderUser = d.senderUser,
            senderName = d.senderName,
            imageUrl = d.imageUrl,
            timestamp = d.timestamp
        )
        "system" -> ChatMessage.SystemMessage(
            messageId = d.messageId.ifEmpty { java.util.UUID.randomUUID().toString() },
            roomId = d.roomId,
            senderUser = d.senderUser,
            senderName = d.senderName,
            content = d.content,
            timestamp = d.timestamp
        )
        else -> ChatMessage.SystemMessage(
            content = "未知消息类型",
            timestamp = d.timestamp
        )
    }
}

/**
 * 将 ChatMessage 转为 MqttPayload（用于发送）
 */
fun ChatMessage.toMqttPayload(): MqttPayload {
    return when (this) {
        is ChatMessage.TextMessage -> MqttPayload(
            type = "text",
            data = MqttData(
                messageId = messageId,
                roomId = roomId,
                senderUser = senderUser,
                senderName = senderName,
                content = encryptedContent,
                timestamp = timestamp
            )
        )
        is ChatMessage.ImageMessage -> MqttPayload(
            type = "image",
            data = MqttData(
                messageId = messageId,
                roomId = roomId,
                senderUser = senderUser,
                senderName = senderName,
                imageUrl = imageUrl,
                timestamp = timestamp
            )
        )
        is ChatMessage.SystemMessage -> MqttPayload(
            type = "system",
            data = MqttData(
                messageId = messageId,
                roomId = roomId,
                senderUser = senderUser,
                senderName = senderName,
                content = content,
                timestamp = timestamp
            )
        )
    }
}
