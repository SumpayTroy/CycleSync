package com.bsit.cyclesync.ui.navigation

import androidx.activity.compose.BackHandler
import com.bsit.cyclesync.ui.calendar.CalendarScreen
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bsit.cyclesync.ui.addfriend.AddFriendScreen
import com.bsit.cyclesync.ui.home.DEFAULT_BOTTOM_BAR_HEIGHT
import com.bsit.cyclesync.ui.home.MapScreen
import com.bsit.cyclesync.ui.navigation.MainDestinations.HISTORY_ID
import com.bsit.cyclesync.ui.profile.ProfileScreen
import com.bsit.cyclesync.ui.friendlist.FriendListScreen
import com.bsit.cyclesync.ui.friendrequests.FriendRequestsScreen
import com.bsit.cyclesync.ui.gallery.AlbumScreen
import com.bsit.cyclesync.ui.historyride.HistoryScreen
import com.bsit.cyclesync.ui.profile_settings.SettingsScreen
import com.bsit.cyclesync.ui.splash.SplashScreen


// Refactored from https://github.com/android/compose-samples/blob/main/Jetsnack/app/src/main/java/com/example/jetsnack/ui/navigation/JetsnackNavController.kt

enum class MainDestinations(val route: String) {
    AUTH_ROUTE("auth"),
    SPLASH_ROUTE("splash"),
    SIGN_UP_ROUTE("sign_up"),
    MAIN_ROUTE("main"),
    HOME_ROUTE("home"),
    WEATHER_ROUTE("weather"),
    PROFILE_ROUTE("profile"),
    HISTORY_ROUTE("history"),
    HISTORY_ID("history_id"),
    FRIENDS_ROUTE("friends_list"),
    ADD_FRIENDS_ROUTE("add_friends"),
    FRIENDS_REQUEST_ROUTE("friendRequests"),
    SETTINGS_ROUTE("settings"),
    TERMS_ROUTE("terms"),
    CALENDAR_ROUTE("calendar"),
    GALLERY_ROUTE("gallery"),
    VERIFY_EMAIL_ROUTE("verify_email");


}

fun NavGraphBuilder.mainGraph(
    navController: CycleSyncNavController,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onThemeChange: (Boolean) -> Unit,
    isDarkTheme: Boolean,
    finish: () -> Unit
) {
    composable(
        route = MainDestinations.SPLASH_ROUTE.route,
        enterTransition = { fadeIn(animationSpec = tween(700)) },
        exitTransition = { fadeOut(animationSpec = tween(700)) }
    ) {
        SplashScreen(navController = navController.navController)
    }

    composable(
        route = MainDestinations.HOME_ROUTE.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) +
                    fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(300)
            ) +
                    fadeOut(animationSpec = tween(300))
        }
    ) { backStackEntry ->
        MapScreen(modifier = Modifier.padding(bottom = DEFAULT_BOTTOM_BAR_HEIGHT.dp + 24.dp),)

        BackHandler {
            finish()
        }
    }

    // Composable for Gallery or Album
    composable(route = MainDestinations.GALLERY_ROUTE.route) {
        AlbumScreen(
            navController = navController.navController,
            albumName = "Default",
            onRename = { oldName, newName -> },
            onDelete = { deletedAlbum -> }
        )
    }
     //Composable for Ride History
    composable(route = MainDestinations.HISTORY_ROUTE.route) {
        HistoryScreen()
    }
    //Composable for FriendRequest
    composable(route = MainDestinations.FRIENDS_REQUEST_ROUTE.route) {
        FriendRequestsScreen(
            onBack = { navController.upPress() },
        )
    }

    // Composable for Profile
    composable(route = MainDestinations.PROFILE_ROUTE.route) {
        // TODO implement profile screen
        ProfileScreen(
            onLoggedOut = { navController.navigateToAuth() },
            onFriendListClick = {
                navController.navigateTo(MainDestinations.FRIENDS_ROUTE)
            },
            onAddFriendListClick = {
                navController.navigateTo(MainDestinations.ADD_FRIENDS_ROUTE)
            },onCalendarClick = {
                navController.navigateTo(MainDestinations.CALENDAR_ROUTE)
            },  onSettingsClick = {
                navController.navigateTo(MainDestinations.SETTINGS_ROUTE)
            }
        )
    }

    // Composable for Friends List
    composable(route = MainDestinations.FRIENDS_ROUTE.route) {
        FriendListScreen(
            onBack = { navController.upPress() },
            onAddFriends = { navController.navigateTo(MainDestinations.ADD_FRIENDS_ROUTE) }
        )
    }
    // Composable for Add Friend
    composable(route = MainDestinations.ADD_FRIENDS_ROUTE.route) {
        AddFriendScreen(
            onBack = { navController.upPress() },
            onOpenFriendRequests = {
                navController.navigateTo(MainDestinations.FRIENDS_REQUEST_ROUTE)
            }
        )
    }
    // Composable for Calendar
    composable(route = MainDestinations.CALENDAR_ROUTE.route) {
        CalendarScreen(
            onBack = { navController.upPress() }
        )
    }
    // Composable for Settings
    composable(route = MainDestinations.SETTINGS_ROUTE.route) {
        SettingsScreen(
            onBack = { navController.upPress() },
            isDarkTheme = isDarkTheme,
            onThemeChange = onThemeChange,
        )
    }


}

@Composable
fun rememberCycleSyncNavController(navController: NavHostController = rememberNavController()): CycleSyncNavController =
    remember(navController) {
        CycleSyncNavController(navController)
    }


@Stable
class CycleSyncNavController(val navController: NavHostController) {
    // ----------------------------------------------------------
    // Navigation state source of truth
    // ----------------------------------------------------------
    fun upPress() {
        navController.navigateUp()
    }

    fun navigateToBottomBarRoute(route: String) {
        if (route != navController.currentDestination?.route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }
    // Navigate to Auth screen and clear backstack (used on logout)
    fun navigateToAuth() {
        navController.navigate(MainDestinations.AUTH_ROUTE.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    // On navigating to app scenes i.e. splash/bootstrap or main
    // - Pop up to the start destination of the graph and don't save state since
    //   no route hierarchy should be maintained
    // - launchSingleTop to avoid multiple copies of the same destination
    // - Don't restore any state
    fun navigateToMainDestinations(destination: MainDestinations) {
        val graph = navController.graph
        // Only navigate if graph is already set
        if (graph.startDestinationId != 0) {
            if (destination.route != navController.currentDestination?.route) {
                navController.navigate(destination.route) {
                    popUpTo(graph.findStartDestination().id) {
                        inclusive = true
                        saveState = false
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        } else {
            // Delay navigation until NavHost is initialized
            navController.navigate(destination.route)
        }
    }


    fun navigateTo(destination: MainDestinations) {
        if (destination.route != navController.currentDestination?.route) {
            navController.navigate(destination.route) {
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    fun navigateToHistory(historyId: Long, origin: String, from: NavBackStackEntry) {
        // In order to discard duplicated navigation events, we check the Lifecycle
        if (from.lifecycleIsResumed()) {
            navController.navigate("${MainDestinations.HISTORY_ROUTE}/$HISTORY_ID?origin=$origin")
        }
    }

    @Composable
    fun getCurrentBackStackEntry(): State<NavBackStackEntry?> {
        return navController.currentBackStackEntryAsState()
    }


    /**
     * Default bottom navigation function
     */
//    navController.navigate(item.route) {
//        // Pop up to the start destination of the graph to
//        // avoid building up a large stack of destinations
//        // on the back stack as users select items
//        popUpTo(navController.graph.startDestinationId) {
//            saveState = true
//        }
//        // Avoid multiple copies of the same destination when
//        // reselecting the same item
//        launchSingleTop = true
//        // Restore state when reselecting a previously selected item
//        restoreState = true
//    }
}


private fun NavBackStackEntry.lifecycleIsResumed() =
    this.lifecycle.currentState == Lifecycle.State.RESUMED

private val NavGraph.startDestination: NavDestination?
    get() = findNode(startDestinationId)


private tailrec fun findStartDestination(graph: NavDestination): NavDestination {
    return if (graph is NavGraph) findStartDestination(graph.startDestination!!) else graph
}
