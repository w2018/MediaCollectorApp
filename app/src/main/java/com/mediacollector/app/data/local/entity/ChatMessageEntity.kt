package com.mediacollector.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["room_id", "timestamp"])]
)
data class ChatMessageEntity(
    @PrimaryKey val message_id: String,
    val room_id: String,
    val sender_user: String,
    val sender_name: String,
    val type: String = "text",
    val content: String,
    val timestamp: Long
)
