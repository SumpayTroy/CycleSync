package com.bsit.cyclesync

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bsit.cyclesync.data.ThemePreferences
import com.bsit.cyclesync.ui.auth.AuthScreen
import com.bsit.cyclesync.ui.auth.AuthViewModel
import com.bsit.cyclesync.ui.auth.SignUpScreen
import com.bsit.cyclesync.ui.auth.TermsScreen
import com.bsit.cyclesync.ui.calendar.NotificationUtils
import com.bsit.cyclesync.ui.gallery.AlbumScreen
import com.bsit.cyclesync.ui.home.BottomNavigationBar
import com.bsit.cyclesync.ui.home.HomeViewModel
import com.bsit.cyclesync.ui.navigation.MainDestinations
import com.bsit.cyclesync.ui.navigation.mainGraph
import com.bsit.cyclesync.ui.navigation.rememberCycleSyncNavController
import com.bsit.cyclesync.ui.splash.SplashScreen
import com.bsit.cyclesync.ui.theme.CycleSyncTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var themePreferences: ThemePreferences
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔔 Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }

        enableEdgeToEdge()
        NotificationUtils.createNotificationChannel(this)
        themePreferences = ThemePreferences(this)

        // Observe dark mode from DataStore
        lifecycleScope.launch {
            themePreferences.darkModeFlow.collect { darkMode ->
                setContent {
                    val viewModel: AuthViewModel = hiltViewModel()
                    val isLoggedIn = viewModel.isLoggedIn
                    val cycleSyncNavController = rememberCycleSyncNavController()

                    var isDarkTheme by remember { mutableStateOf(darkMode) }

                    CycleSyncTheme(darkTheme = isDarkTheme) {

                        LaunchedEffect(isLoggedIn) {
                            if (!isLoggedIn) {
                                kotlinx.coroutines.delay(300)
                                cycleSyncNavController.navigateToMainDestinations(
                                    MainDestinations.AUTH_ROUTE
                                )
                            }
                        }

                        val navBackStackEntry by cycleSyncNavController.navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        val showBottomBar = currentRoute in listOf(
                            MainDestinations.HOME_ROUTE.route,
                            MainDestinations.WEATHER_ROUTE.route,
                            MainDestinations.PROFILE_ROUTE.route,
                            MainDestinations.HISTORY_ROUTE.route,
                            MainDestinations.FRIENDS_ROUTE.route,
                            MainDestinations.SETTINGS_ROUTE.route,
                            MainDestinations.GALLERY_ROUTE.route,
                            MainDestinations.ADD_FRIENDS_ROUTE.route,
                            MainDestinations.FRIENDS_REQUEST_ROUTE.route,
                        )

                        Scaffold(
                            bottomBar = {
                                AnimatedVisibility(showBottomBar) {
                                    BottomNavigationBar(
                                        navController = cycleSyncNavController
                                    )
                                }
                            }
                        ) { paddingValues ->
                            NavHost(
                                navController = cycleSyncNavController.navController,
                                startDestination = MainDestinations.SPLASH_ROUTE.route,
                                modifier = Modifier.padding(paddingValues)
                            ) {
                                // AUTH SCREEN
                                composable(MainDestinations.AUTH_ROUTE.route) {
                                    AuthScreen(
                                        onSignUpClicked = {
                                            cycleSyncNavController.navController.navigate(
                                                MainDestinations.SIGN_UP_ROUTE.route
                                            ) {
                                                popUpTo(MainDestinations.AUTH_ROUTE.route) { inclusive = true }
                                            }
                                        },
                                        onSignInClicked = {
                                            viewModel.markLoggedIn(true)
                                            cycleSyncNavController.navigateToMainDestinations(MainDestinations.HOME_ROUTE)
                                        },
                                        onRedirect = {
                                            cycleSyncNavController.navigateToMainDestinations(MainDestinations.HOME_ROUTE)
                                        }
                                    )
                                    BackHandler { finish() }
                                }

                                // SIGN UP SCREEN
                                composable(MainDestinations.SIGN_UP_ROUTE.route) {
                                    val context = LocalContext.current
                                    SignUpScreen(
                                        navController = cycleSyncNavController.navController,
                                        context = context,
                                        onSignUpClick = {
                                            cycleSyncNavController.navController.navigate(
                                                MainDestinations.AUTH_ROUTE.route
                                            )
                                        },
                                        onTermsClick = {
                                            cycleSyncNavController.navController.navigate(
                                                MainDestinations.TERMS_ROUTE.route
                                            )
                                        },
                                        onBackClick = {
                                            cycleSyncNavController.navController.navigate(
                                                MainDestinations.AUTH_ROUTE.route
                                            ) {
                                                popUpTo(MainDestinations.SIGN_UP_ROUTE.route) { inclusive = true }
                                            }

                                        }
                                    )
                                }

                                // TERMS SCREEN
                                composable(MainDestinations.TERMS_ROUTE.route) {
                                    TermsScreen(
                                        onBackClick = {
                                            cycleSyncNavController.navController.popBackStack()
                                        }
                                    )
                                }

                                // SPLASH SCREEN
                                composable(MainDestinations.SPLASH_ROUTE.route) {
                                    SplashScreen(navController = cycleSyncNavController.navController)
                                }

                                composable(route = MainDestinations.GALLERY_ROUTE.route) {
                                    AlbumScreen(
                                        navController = cycleSyncNavController.navController,
                                        albumName = "Default",
                                        onRename = { _, _ -> },
                                        onDelete = {}
                                    )
                                }


                                // MAIN NAVIGATION GRAPH
                                navigation(
                                    route = MainDestinations.MAIN_ROUTE.route,
                                    startDestination = MainDestinations.HOME_ROUTE.route
                                ) {
                                    mainGraph(
                                        navController = cycleSyncNavController,
                                        modifier = Modifier.padding(paddingValues),
                                        onLogout = { viewModel.markLoggedIn(false) },
                                        onThemeChange = { isDark ->
                                            isDarkTheme = isDark
                                            lifecycleScope.launch { themePreferences.saveDarkMode(isDark) }
                                        },
                                        isDarkTheme = isDarkTheme,
                                        finish = {
                                            finish()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("notifications").child(userId)

        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val title = snapshot.child("title").getValue(String::class.java) ?: "CycleSync"
                val message = snapshot.child("message").getValue(String::class.java) ?: "You have a new ride"
                NotificationUtils.showNotification(this@MainActivity, title, message)
            }

            override fun onCancelled(error: DatabaseError) {}
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        })
    }
}
