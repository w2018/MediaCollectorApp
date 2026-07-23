package com.mediacollector.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_history",
    indices = [Index(value = ["media_id"], unique = true)]
)
data class LocalHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val media_id: Int,
    val media_type: String = "photo",
    val watch_position: Int = 0,
    val synced: Boolean = false,
    val watched_at: Long = System.currentTimeMillis()
)
