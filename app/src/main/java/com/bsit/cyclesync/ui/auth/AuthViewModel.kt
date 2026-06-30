package com.bsit.cyclesync.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsit.cyclesync.services.LocationService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val locationService: LocationService,


) : ViewModel() {

    // ----------------- User Input & State -----------------
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var showProgressBar by mutableStateOf(false)
    var loginAttempts by mutableStateOf(0)
    var isBlocked by mutableStateOf(false)
    var remainingTime by mutableStateOf(0)



    // ----------------- Logged In State -----------------
    var isLoggedIn by mutableStateOf(false)
        private set

    // Use this instead of the old setter to avoid JVM signature clash
    fun markLoggedIn(loggedIn: Boolean) {
        isLoggedIn = loggedIn
    }
    init {
        // Check if a user is already signed in
        if (firebaseAuth.currentUser != null) {
            isLoggedIn = true
        }
    }


    // ----------------- Sign In -----------------
    fun signIn(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isBlocked) return

        if (email.isBlank() || password.isBlank()) {
            onError("Email and password cannot be empty")
            return
        }

        showProgressBar = true
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showProgressBar = false
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null && !user.isEmailVerified) {
                        //  Stop login if email not verified
                        onError("Please verify your email before logging in.")
                        firebaseAuth.signOut()
                    } else {
                        loginAttempts = 0
                        onSuccess()
                    }
                } else {
                    loginAttempts++
                    if (loginAttempts >= 3) startBlockTimer()
                    onError(task.exception?.message ?: "Login failed")
                }
            }
    }



    private fun startBlockTimer() {
        isBlocked = true
        remainingTime = 30
        viewModelScope.launch {
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
            }
            loginAttempts = 0
            isBlocked = false
        }
    }

    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(task.exception ?: Exception("Unknown error"))
                }
            }
    }

}
