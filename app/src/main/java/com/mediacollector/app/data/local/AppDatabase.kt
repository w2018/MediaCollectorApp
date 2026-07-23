package com.mediacollector.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mediacollector.app.data.local.dao.MediaDao
import com.mediacollector.app.data.local.entity.*

@Database(
    entities = [
        CachedMedia::class,
        LocalFavorite::class,
        LocalHistory::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}
