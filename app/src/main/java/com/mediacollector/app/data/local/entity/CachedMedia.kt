package com.mediacollector.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_media")
data class CachedMedia(
    @PrimaryKey val id: Int,
    val title: String,
    val url: String,
    val type: String,
    val thumbnail_url: String,
    val local_thumbnail_path: String = "",
    val author: String,
    val cached_at: Long = System.currentTimeMillis()
)
