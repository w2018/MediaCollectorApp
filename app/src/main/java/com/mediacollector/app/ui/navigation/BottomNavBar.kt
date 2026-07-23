package com.mediacollector.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector

/** 底部导航项 */
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("媒体", Icons.Default.GridView, Routes.MediaGrid.route),
    BottomNavItem("下载", Icons.Default.Download, Routes.Downloads.route),
    BottomNavItem("收藏", Icons.Default.Favorite, Routes.Favorites.route),
    BottomNavItem("历史", Icons.Default.History, Routes.History.route),
    BottomNavItem("聊天", Icons.Default.Chat, Routes.Chat.route),
    BottomNavItem("我的", Icons.Default.Person, Routes.Profile.route)
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}
