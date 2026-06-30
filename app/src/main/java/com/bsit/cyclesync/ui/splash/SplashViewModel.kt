package com.bsit.cyclesync.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsit.cyclesync.data.FirstLaunchPreferences
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SplashState { Loading, FirstLaunch, LoggedIn, NotLoggedIn }

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val firstLaunchPrefs: FirstLaunchPreferences,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _splashState = MutableStateFlow(SplashState.Loading)
    val splashState: StateFlow<SplashState> = _splashState

    init {
        checkState()
    }

    private fun checkState() {
        viewModelScope.launch {
            val isFirstLaunch = firstLaunchPrefs.isFirstLaunch.first()
            val currentUser = firebaseAuth.currentUser

            if (isFirstLaunch) {
                firstLaunchPrefs.setFirstLaunchDone()
                _splashState.value = SplashState.FirstLaunch
                return@launch
            }

            if (currentUser != null) {
                // Reload to get the latest email verification state
                currentUser.reload().addOnSuccessListener {
                    if (currentUser.isEmailVerified) {
                        _splashState.value = SplashState.LoggedIn
                    } else {
                        // Not verified → sign out and go to login
                        firebaseAuth.signOut()
                        _splashState.value = SplashState.NotLoggedIn
                    }
                }.addOnFailureListener {
                    firebaseAuth.signOut()
                    _splashState.value = SplashState.NotLoggedIn
                }
            } else {
                _splashState.value = SplashState.NotLoggedIn
            }
        }
    }
}
