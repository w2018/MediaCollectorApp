package com.mediacollector.app.di

import android.content.Context
import coil3.ImageLoader
import com.mediacollector.app.MediaCollectorApp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** 提供 Coil 全局 ImageLoader（在 Application 中初始化） */
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return (context.applicationContext as MediaCollectorApp).imageLoader
    }
}
