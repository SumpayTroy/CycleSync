package com.bsit.cyclesync.ui.auth

import android.Manifest
import com.bsit.cyclesync.R
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bsit.cyclesync.ui.theme.GreenStart
import com.bsit.cyclesync.ui.theme.LightBlueBackground
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AuthScreen(
    onSignUpClicked: () -> Unit,
    onSignInClicked: () -> Unit,
    onRedirect: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val isBlocked = viewModel.isBlocked
    val remainingTime = viewModel.remainingTime



    // Auto-redirect only if user is signed in AND email is verified
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful && currentUser.isEmailVerified) {
                    viewModel.markLoggedIn(true)
                    onRedirect()
                } else {
                    // Sign out if not verified, and stay on login screen
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(context, "Please verify your email before logging in.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    LaunchedEffect(viewModel.isLoggedIn, locationPermissionState.status, cameraPermissionState.status) {
        if (viewModel.isLoggedIn) onRedirect()
        if (!locationPermissionState.status.isGranted) locationPermissionState.launchPermissionRequest()
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    fun handleLogin() {
        if (isBlocked) return

        // Hook up inputs to ViewModel
        viewModel.email = email
        viewModel.password = password

        viewModel.signIn(
            onSuccess = {
                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                onSignInClicked() // Navigate to main screen
            },
            onError = { errorMsg ->
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GreenStart, Color(0xFF1B5E20))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(80.dp))
            AppLogo()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to CycleSync",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Track your rides, share locations,\nand sync with friends in real time!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Inputs
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LightBlueBackground,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.9f),
                    focusedLabelColor = LightBlueBackground,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.9f),
                    cursorColor = LightBlueBackground,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle Password", tint = LightBlueBackground)
                    }
                },colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LightBlueBackground,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.9f),
                    focusedLabelColor = LightBlueBackground,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.9f),
                    cursorColor = LightBlueBackground,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Forgot Password?",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable {
                        if (email.isNotBlank()) {
                            viewModel.resetPassword(
                                email,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Reset link sent to $email",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onError = { e ->
                                    Toast.makeText(
                                        context,
                                        "Failed: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Enter your email first", Toast.LENGTH_SHORT).show()
                        }
                    },
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (viewModel.showProgressBar) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Button(
                onClick = { handleLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !viewModel.isBlocked,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(Color.White, Color.White))
                )
            ) {
                Text(
                    if (viewModel.isBlocked) "Blocked: ${viewModel.remainingTime} s" else "Sign In",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSignUpClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(Color.White, Color.White))
                )
            ) {
                Text("Create Account", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

}

