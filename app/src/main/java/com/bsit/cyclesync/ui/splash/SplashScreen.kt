package com.bsit.cyclesync.ui.splash

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import com.bsit.cyclesync.OnboardingActivity
import com.bsit.cyclesync.R
import com.bsit.cyclesync.ui.navigation.MainDestinations
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: SplashViewModel = hiltViewModel()
    val splashState by viewModel.splashState.collectAsState()

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.splash_loading_hourglass)
    )

    LaunchedEffect(splashState) {
        if (splashState != SplashState.Loading) {
            delay(4000) // shorter splash delay (optional)
            when (splashState) {
                SplashState.FirstLaunch -> {
                    context.startActivity(Intent(context, OnboardingActivity::class.java))
                    (context as? Activity)?.finish()
                }
                SplashState.LoggedIn -> {
                    navController.navigate(MainDestinations.MAIN_ROUTE.route) {
                        popUpTo(MainDestinations.SPLASH_ROUTE.route) { inclusive = true }
                    }
                }
                SplashState.NotLoggedIn -> {
                    navController.navigate(MainDestinations.AUTH_ROUTE.route) {
                        popUpTo(MainDestinations.SPLASH_ROUTE.route) { inclusive = true }
                    }
                }
                else -> Unit
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )
    }
}
