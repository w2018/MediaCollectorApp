package com.mediacollector.app.ui.chat

/**
 * MQTT 配置
 *
 * Broker: broker.emqx.io (免费公共 MQTT Broker)
 * - TCP: 1883（部分运营商网络可能封锁）
 * - WebSocket: 8083（走 HTTP 升级，兼容性更好）
 * - WebSocket Secure: 8084
 * 主题格式: mc/a7x9k3m2/{roomId}/broadcast
 * QoS: 1
 */
object MqttConfig {
    /** 主要 Broker 地址（WebSocket，兼容性更好） */
    const val BROKER_URL = "ws://broker.emqx.io:8083"

    /** 备用 Broker 地址（TCP，部分网络可直连） */
    const val BROKER_URL_FALLBACK = "tcp://broker.emqx.io:1883"

    /** 客户端 ID 前缀 */
    const val CLIENT_ID_PREFIX = "mc_android_"

    /** 主题前缀（固定复杂字符串防干扰） */
    const val TOPIC_PREFIX = "mc/a7x9k3m2"

    /** 获取订阅主题 */
    fun getTopic(roomId: String): String = "$TOPIC_PREFIX/$roomId/broadcast"

    /** QoS 等级 */
    const val QOS = 1

    /** 心跳间隔（秒） */
    const val HEARTBEAT_INTERVAL = 30L

    /** 连接超时（秒） */
    const val CONNECTION_TIMEOUT = 10

    /** 自动重连间隔基础值（秒） */
    const val RECONNECT_BASE_DELAY = 2L

    /** 自动重连最大间隔（秒） */
    const val RECONNECT_MAX_DELAY = 60L
}
