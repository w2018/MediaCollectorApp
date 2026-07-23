package com.mediacollector.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mediacollector.app.ui.auth.*
import com.mediacollector.app.ui.chat.ChatScreen
import com.mediacollector.app.ui.collection.*
import com.mediacollector.app.ui.detail.MediaDetailScreen
import com.mediacollector.app.ui.download.DownloadManageScreen
import com.mediacollector.app.ui.favorite.FavoriteListScreen
import com.mediacollector.app.ui.history.HistoryListScreen
import com.mediacollector.app.ui.media.MediaGridScreen
import com.mediacollector.app.ui.photo.PhotoViewerScreen
import com.mediacollector.app.ui.search.SearchScreen
import com.mediacollector.app.ui.settings.*
import com.mediacollector.app.ui.video.VideoPlayerScreen

/** 需要显示底部导航栏的路由 */
private val bottomNavRoutes = setOf(
    Routes.MediaGrid.route,
    Routes.Downloads.route,
    Routes.Favorites.route,
    Routes.History.route,
    Routes.Chat.route,
    Routes.Profile.route
)

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MediaGrid.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── 主 Tab 页面 ──
            composable(Routes.MediaGrid.route) {
                MediaGridScreen(
                    onMediaClick = { mediaId, type ->
                        if (type == "video") {
                            navController.navigate(Routes.VideoPlayer.createRoute(mediaId))
                        } else {
                            navController.navigate(Routes.PhotoViewer.createRoute(mediaId))
                        }
                    },
                    onSearchClick = { navController.navigate(Routes.Search.route) },
                    onCollectionsClick = { navController.navigate(Routes.Collections.route) }
                )
            }

            composable(Routes.Downloads.route) {
                DownloadManageScreen()
            }

            composable(Routes.Favorites.route) {
                FavoriteListScreen(
                    onMediaClick = { mediaId, type ->
                        if (type == "video") {
                            navController.navigate(Routes.VideoPlayer.createRoute(mediaId))
                        } else {
                            navController.navigate(Routes.PhotoViewer.createRoute(mediaId))
                        }
                    }
                )
            }

            composable(Routes.History.route) {
                HistoryListScreen(
                    onMediaClick = { mediaId, type ->
                        if (type == "video") {
                            navController.navigate(Routes.VideoPlayer.createRoute(mediaId))
                        } else {
                            navController.navigate(Routes.PhotoViewer.createRoute(mediaId))
                        }
                    }
                )
            }

            composable(Routes.Chat.route) {
                ChatScreen()
            }

            composable(Routes.Profile.route) {
                ProfileScreen(
                    onNavigateToLogin = { navController.navigate(Routes.Login.route) },
                    onNavigateToRegister = { navController.navigate(Routes.Register.route) },
                    onNavigateToSettings = { navController.navigate(Routes.Settings.route) },
                    onNavigateToCacheManage = { navController.navigate(Routes.CacheManage.route) },
                    onNavigateToAbout = { navController.navigate(Routes.About.route) }
                )
            }

            // ── 子页面 ──
            composable(
                route = Routes.PhotoViewer.route,
                arguments = listOf(navArgument("mediaId") { type = NavType.IntType })
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getInt("mediaId") ?: return@composable
                PhotoViewerScreen(
                    mediaId = mediaId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.VideoPlayer.route,
                arguments = listOf(navArgument("mediaId") { type = NavType.IntType })
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getInt("mediaId") ?: return@composable
                VideoPlayerScreen(
                    mediaId = mediaId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.MediaDetail.route,
                arguments = listOf(navArgument("mediaId") { type = NavType.IntType })
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getInt("mediaId") ?: return@composable
                MediaDetailScreen(
                    mediaId = mediaId,
                    onBack = { navController.popBackStack() },
                    onPlayVideo = { navController.navigate(Routes.VideoPlayer.createRoute(mediaId)) }
                )
            }

            composable(Routes.Collections.route) {
                CollectionListScreen(
                    onCollectionClick = { id -> navController.navigate(Routes.CollectionDetail.createRoute(id)) }
                )
            }

            composable(
                route = Routes.CollectionDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: return@composable
                CollectionDetailScreen(
                    collectionId = id,
                    onMediaClick = { mediaId, type ->
                        if (type == "video") {
                            navController.navigate(Routes.VideoPlayer.createRoute(mediaId))
                        } else {
                            navController.navigate(Routes.PhotoViewer.createRoute(mediaId))
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.Search.route) {
                SearchScreen(
                    onMediaClick = { mediaId, type ->
                        if (type == "video") {
                            navController.navigate(Routes.VideoPlayer.createRoute(mediaId))
                        } else {
                            navController.navigate(Routes.PhotoViewer.createRoute(mediaId))
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ── 认证 ──
            composable(Routes.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.Profile.route) {
                            popUpTo(Routes.MediaGrid.route) { saveState = false }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.Register.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Routes.Profile.route) {
                            popUpTo(Routes.MediaGrid.route) { saveState = false }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            // ── 设置 ──
            composable(Routes.Settings.route) {
                SettingsScreen(
                    onServerConfig = { navController.navigate(Routes.ServerConfig.route) },
                    onCacheManage = { navController.navigate(Routes.CacheManage.route) },
                    onAbout = { navController.navigate(Routes.About.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ServerConfig.route) {
                ServerConfigScreen(
                    onCheckClick = { navController.navigate(Routes.ApiCheck.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ApiCheck.route) {
                ApiCheckScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.CacheManage.route) {
                CacheManageScreen(onBack = { navController.popBackStack() })
            }

            // ── 关于 ──
            composable(Routes.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
