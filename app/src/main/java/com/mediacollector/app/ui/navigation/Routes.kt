package com.mediacollector.app.ui.navigation

/**
 * 导航路由定义
 */
sealed class Routes(val route: String) {
    // ── 主 Tab 页面 ──
    data object MediaGrid : Routes("media")
    data object Favorites : Routes("favorites")
    data object History : Routes("history")
    data object Profile : Routes("profile")
    data object Chat : Routes("chat")

    // ── 子页面 ──
    data object PhotoViewer : Routes("photo/{mediaId}") {
        fun createRoute(mediaId: Int) = "photo/$mediaId"
    }
    data object VideoPlayer : Routes("video/{mediaId}") {
        fun createRoute(mediaId: Int) = "video/$mediaId"
    }
    data object MediaDetail : Routes("media/{mediaId}") {
        fun createRoute(mediaId: Int) = "media/$mediaId"
    }
    data object Collections : Routes("collections")
    data object CollectionDetail : Routes("collection/{id}") {
        fun createRoute(id: Int) = "collection/$id"
    }
    data object Tags : Routes("tags")
    data object Search : Routes("search")

    // ── 认证 ──
    data object Login : Routes("auth/login")
    data object Register : Routes("auth/register")

    // ── 设置 ──
    data object Settings : Routes("settings")
    data object ServerConfig : Routes("settings/server")
    data object ApiCheck : Routes("settings/server/check")
    data object CacheManage : Routes("settings/cache")
    data object Downloads : Routes("downloads")

    // ── 其他 ──
    data object About : Routes("about")
}
