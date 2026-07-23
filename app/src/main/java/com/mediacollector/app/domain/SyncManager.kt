package com.mediacollector.app.domain

import com.mediacollector.app.data.repository.FavoriteRepository
import com.mediacollector.app.data.repository.HistoryRepository
import com.mediacollector.app.data.settings.AuthStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步管理器
 *
 * 负责启动时全量拉取、联网时增量推送、登录/退出时数据迁移。
 */
@Singleton
class SyncManager @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository,
    private val authStore: AuthStore
) {
    /**
     * 启动时同步：拉取服务器数据 → 合并到本地
     */
    suspend fun syncOnStartup() {
        if (!authStore.isLoggedIn()) return

        // 先推送本地未同步数据
        favoriteRepository.syncPending()
        historyRepository.syncPending()

        // 再从服务器拉取全量覆盖本地
        favoriteRepository.syncFromServer()
        historyRepository.syncFromServer()
    }

    /**
     * 联网时推送本地未同步数据
     */
    suspend fun syncPendingChanges() {
        if (!authStore.isLoggedIn()) return
        favoriteRepository.syncPending()
        historyRepository.syncPending()
    }
}
