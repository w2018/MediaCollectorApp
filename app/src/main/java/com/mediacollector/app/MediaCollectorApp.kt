package com.mediacollector.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MediaCollectorApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /** 全局 Coil ImageLoader，供 Hilt 注入 */
    lateinit var imageLoader: ImageLoader
        private set

    override fun onCreate() {
        super.onCreate()
        initImageLoader()
    }

    private fun initImageLoader() {
        imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this@MediaCollectorApp, 0.25)
                    .build()
            }
            .build()
    }
}
