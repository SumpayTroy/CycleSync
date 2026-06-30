package com.bsit.cyclesync.ui.home

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bsit.cyclesync.ui.navigation.CycleSyncNavController
import com.bsit.cyclesync.ui.navigation.MainDestinations
import com.bsit.cyclesync.ui.theme.GreenStart

// Define a data class for navigation items
data class BottomNavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

const val DEFAULT_BOTTOM_BAR_HEIGHT = 64

@Composable
fun BottomNavigationBar(navController: CycleSyncNavController) {
    val items = listOf(
        BottomNavigationItem("Home", Icons.Filled.Home, MainDestinations.HOME_ROUTE.route),
        BottomNavigationItem("History", Icons.Filled.History, MainDestinations.HISTORY_ROUTE.route),
        BottomNavigationItem("Gallery", Icons.Filled.PhotoAlbum, MainDestinations.GALLERY_ROUTE.route),
        BottomNavigationItem("Profile", Icons.Filled.AccountCircle, MainDestinations.PROFILE_ROUTE.route)
    )

    var selectedItemIndex by remember { mutableIntStateOf(0) }
    val currentRoute = navController.navController.currentBackStackEntryAsState().value?.destination?.route

    // Update selected item when route changes
    LaunchedEffect(currentRoute) {
        items.indexOfFirst { it.route == currentRoute }.takeIf { it != -1 }?.let {
            selectedItemIndex = it
        }
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 12.dp,
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedItemIndex == index

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        selectedItemIndex = index
                        navController.navigateToBottomBarRoute(item.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) GreenStart else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (isSelected) GreenStart else Color.Gray,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                modifier = Modifier.height(DEFAULT_BOTTOM_BAR_HEIGHT.dp),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GreenStart,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = GreenStart,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = GreenStart.copy(alpha = 0.1f)
                )
            )
        }
    }

}
