package com.mediacollector.app.ui.photo

/**
 * 浏览上下文 — 持有当前媒体列表，供全屏查看器做左右滑动导航。
 *
 * MediaGridScreen 会在加载媒体列表时更新此处的 mediaIds，
 * PhotoViewerViewModel 在 loadMedia() 时读取。
 */
object BrowsingContext {
    @Volatile
    var mediaIds: List<Int> = emptyList()
}
