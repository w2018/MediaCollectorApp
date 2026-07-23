package com.mediacollector.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_favorites",
    indices = [Index(value = ["media_id"], unique = true)]
)
data class LocalFavorite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val media_id: Int,
    val synced: Boolean = false,
    val created_at: Long = System.currentTimeMillis()
)
