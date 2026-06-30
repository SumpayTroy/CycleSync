package com.bsit.cyclesync.ui.auth

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import com.bsit.cyclesync.ui.utils.AppProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel() {

    // Form states
    var email by mutableStateOf("")
    var username by mutableStateOf("")
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var acceptedTerms by mutableStateOf(false)

    // Error states
    var emailError by mutableStateOf<String?>(null)
    var usernameError by mutableStateOf<String?>(null)
    var firstNameError by mutableStateOf<String?>(null)
    var lastNameError by mutableStateOf<String?>(null)
    var passwordError by mutableStateOf<String?>(null)
    var confirmPasswordError by mutableStateOf<String?>(null)

    var showProgressBar by mutableStateOf(false)
    private var currentLocation: LatLng? by mutableStateOf(null)

    init {
        getCurrentLocation()
    }

    fun validateFields(): Boolean {
        var isValid = true
        val foulWords = listOf("gago", "mamataykana", "putangIna", "fuckYou", "fuck", "putang_ina", "kantot",
            "nigger", "cunt", "slut", "whore", "bastard", "shit", "bitch", "asshole", "dick", "pussy", "faggot")
        val foulWordsLower = foulWords.map { it.lowercase() }
        val nameRegex = Regex("^[A-Za-z0-9]+$")

        if (email.isBlank()) {
            emailError = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Invalid email format"
            isValid = false
        } else emailError = null

        if (username.isBlank()) {
            usernameError = "Username is required"
            isValid = false
        } else if (foulWordsLower.any { word -> username.lowercase().contains(word) }) {
            usernameError = "Username contains inappropriate words"
            isValid = false
        } else usernameError = null

        if (firstName.isBlank()) {
            firstNameError = "First name is required"
            isValid = false
        } else if (!firstName.matches(nameRegex)) {
            firstNameError = "First name cannot contain special characters or symbols"
            isValid = false
        } else firstNameError = null

        if (lastName.isBlank()) {
            lastNameError = "Last name is required"
            isValid = false
        } else if (!lastName.matches(nameRegex)) {
            lastNameError = "Last name cannot contain numbers or symbols"
            isValid = false
        } else lastNameError = null

        if (password.isBlank()) {
            passwordError = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else passwordError = null

        if (confirmPassword.isBlank()) {
            confirmPasswordError = "Confirm password is required"
            isValid = false
        } else if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        } else confirmPasswordError = null

        return isValid
    }

    fun signUpWithFirebase(
        email: String,
        username: String,
        firstName: String,
        lastName: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (password != confirmPassword) {
            onError("Passwords do not match")
            return
        }

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseDatabase.getInstance().reference

        showProgressBar = true

        // Pre-check if email is already in use
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { fetchTask ->
                if (fetchTask.isSuccessful) {
                    val signInMethods = fetchTask.result?.signInMethods ?: emptyList()
                    if (signInMethods.isNotEmpty()) {
                        showProgressBar = false
                        onError("This email is already registered. Try logging in instead.")
                        return@addOnCompleteListener
                    }

                    // Proceed with account creation if email is available
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                                auth.currentUser?.sendEmailVerification()
                                    ?.addOnSuccessListener {
                                        val user = com.bsit.cyclesync.model.User(
                                            uid = uid,
                                            email = email,
                                            username = username,
                                            firstName = firstName,
                                            lastName = lastName,
                                            password = password,
                                            currentLocation = currentLocation?.let {
                                                com.bsit.cyclesync.model.Place(
                                                    latitude = it.latitude,
                                                    longitude = it.longitude,
                                                    name = "Current Location"
                                                )
                                            },
                                            createdAt = System.currentTimeMillis()
                                        )

                                        db.child("users").child(uid).setValue(user)
                                            .addOnSuccessListener {
                                                auth.signOut() // Sign out immediately
                                                showProgressBar = false
                                                onSuccess()
                                            }
                                            .addOnFailureListener { e ->
                                                showProgressBar = false
                                                Log.e("SignUpError", "Failed to save user: ${e.message}")
                                                onError("Failed to save user: ${e.message}")
                                            }
                                    }
                                    ?.addOnFailureListener {
                                        showProgressBar = false
                                        Log.e("SignUpError", "Failed to send verification email: ${it.message}")
                                        onError("Failed to send verification email: ${it.message}")
                                    }
                            } else {
                                showProgressBar = false
                                Log.e("SignUpError", "Sign-up failed: ${task.exception?.message}")
                                onError(task.exception?.message ?: "Sign-up failed")
                            }
                        }
                } else {
                    showProgressBar = false
                    Log.e("SignUpError", "Error checking email: ${fetchTask.exception?.message}")
                    onError("Error checking email: ${fetchTask.exception?.message}")
                }
            }
    }

    private fun getCurrentLocation() {
        val context = AppProvider.getContext() as Context
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)
        val locationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION

        if (ActivityCompat.checkSelfPermission(context, locationPermission) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                }
            }
        }
    }
}