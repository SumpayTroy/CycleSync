package com.bsit.cyclesync.ui.auth

import android.content.Context
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bsit.cyclesync.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import android.os.Handler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

@Composable
fun SignUpScreen(
    navController: NavController,
    context: Context,
    onSignUpClick: () -> Unit,
    onTermsClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val showProgressBar = viewModel.showProgressBar

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GreenStart, Color(0xFF1B5E20))
                )
            )
    ) {

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 40.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Back Arrow
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 40.dp, start = 16.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.height(5.dp))

            Text(
                text = "CycleSync",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Connect your rides, track your routes, and meet up with friends in real time.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(32.dp))

            // 🔹 Email
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email") },
                isError = viewModel.emailError != null,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedFieldColors()
            )
            viewModel.emailError?.let {
                Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))

            // 🔹 Username
            OutlinedTextField(
                value = viewModel.username,
                onValueChange = { viewModel.username = it },
                label = { Text("Username") },
                isError = viewModel.usernameError != null,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedFieldColors()
            )
            viewModel.usernameError?.let {
                Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))

            // 🔹 First & Last Name
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = viewModel.firstName,
                        onValueChange = { viewModel.firstName = it },
                        label = { Text("First Name") },
                        isError = viewModel.firstNameError != null,
                        colors = outlinedFieldColors()
                    )
                    viewModel.firstNameError?.let {
                        Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = viewModel.lastName,
                        onValueChange = { viewModel.lastName = it },
                        label = { Text("Last Name") },
                        isError = viewModel.lastNameError != null,
                        colors = outlinedFieldColors()
                    )
                    viewModel.lastNameError?.let {
                        Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 🔹 Password
            var passwordVisible by rememberSaveable { mutableStateOf(false) }
            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text("Password") },
                isError = viewModel.passwordError != null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle Password",
                            tint = LightBlueBackground
                        )
                    }
                },
                colors = outlinedFieldColors()
            )
            viewModel.passwordError?.let {
                Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))

            // 🔹 Confirm Password
            var confirmVisible by rememberSaveable { mutableStateOf(false) }
            OutlinedTextField(
                value = viewModel.confirmPassword,
                onValueChange = { viewModel.confirmPassword = it },
                label = { Text("Confirm Password") },
                isError = viewModel.confirmPasswordError != null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            imageVector = if (confirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle Confirm Password",
                            tint = LightBlueBackground
                        )
                    }
                },
                colors = outlinedFieldColors()
            )
            viewModel.confirmPasswordError?.let {
                Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))

            // 🔹 Terms Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Checkbox(
                    checked = viewModel.acceptedTerms,
                    onCheckedChange = { viewModel.acceptedTerms = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.White,
                        uncheckedColor = Color.LightGray,
                        checkmarkColor = Color.Black
                    )
                )

                val annotatedText = buildAnnotatedString {
                    append("I agree to the ")

                    pushStringAnnotation(tag = "TERMS", annotation = "terms")
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF90CAF9), // Light blue
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("Terms of Service")
                    }
                    pop()

                    append(" and ")

                    pushStringAnnotation(tag = "TERMS", annotation = "privacy")
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF90CAF9),
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("Privacy Policy")
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedText,
                    onClick = { offset ->
                        annotatedText.getStringAnnotations("TERMS", offset, offset)
                            .firstOrNull()?.let {
                                onTermsClick() // Same handler for both links
                            }
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }


            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (viewModel.validateFields() && viewModel.acceptedTerms) {
                        viewModel.showProgressBar = true
                        viewModel.signUpWithFirebase(
                            email = viewModel.email,
                            username = viewModel.username,
                            firstName = viewModel.firstName,
                            lastName = viewModel.lastName,
                            password = viewModel.password,
                            confirmPassword = viewModel.confirmPassword,
                            onSuccess = {
                                viewModel.showProgressBar = false
                                Toast.makeText(context, "Account created successfully! Please log in.", Toast.LENGTH_SHORT).show()
                                // Use onSignUpClick to navigate to AuthScreen
                                onSignUpClick()
                            }, onError = { error ->
                                viewModel.showProgressBar = false
                                Toast.makeText(context, "Sign-up failed: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                enabled = viewModel.acceptedTerms && !viewModel.showProgressBar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = GreenStart,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    if (viewModel.showProgressBar) {
                        CircularProgressIndicator(
                            color = GreenStart,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp).padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = "Create Account",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
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


@Preview(showBackground = true, showSystemUi = true, name = "SignUp - Empty Fields (Errors)")
@Composable
fun SignUpScreenPreviewEmpty() {
    MaterialTheme {
        SignUpScreenPreviewContent(
            email = "",
            username = "",
            firstName = "",
            lastName = "",
            password = "",
            confirmPassword = "",
            acceptedTerms = false
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "SignUp - Filled Fields (No Errors)")
@Composable
fun SignUpScreenPreviewFilled() {
    MaterialTheme {
        SignUpScreenPreviewContent(
            email = "test@email.com",
            username = "cycleUser",
            firstName = "Troy",
            lastName = "Sumpay",
            password = "123456",
            confirmPassword = "123456",
            acceptedTerms = true
        )
    }
}

@Composable
private fun SignUpScreenPreviewContent(
    email: String,
    username: String,
    firstName: String,
    lastName: String,
    password: String,
    confirmPassword: String,
    acceptedTerms: Boolean
) {
    var emailState by remember { mutableStateOf(email) }
    var usernameState by remember { mutableStateOf(username) }
    var firstNameState by remember { mutableStateOf(firstName) }
    var lastNameState by remember { mutableStateOf(lastName) }
    var passwordState by remember { mutableStateOf(password) }
    var confirmPasswordState by remember { mutableStateOf(confirmPassword) }
    var termsState by remember { mutableStateOf(acceptedTerms) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CycleSync 🚴", style = MaterialTheme.typography.headlineMedium)
        Text("Create your account to start syncing rides")

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = emailState,
            onValueChange = { emailState = it },
            label = { Text("Email") },
            isError = emailState.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )
        if (emailState.isBlank()) Text("Email is required", color = Color.White, style = MaterialTheme.typography.bodySmall,modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = usernameState,
            onValueChange = { usernameState = it },
            label = { Text("Username") },
            isError = usernameState.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )
        if (usernameState.isBlank()) Text("Username is required", color = Color.White, style = MaterialTheme.typography.bodySmall,modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                OutlinedTextField(
                    value = firstNameState,
                    onValueChange = { firstNameState = it },
                    label = { Text("First Name") },
                    isError = firstNameState.isBlank()
                )
                if (firstNameState.isBlank()) Text("First name is required", color = Color.White, style = MaterialTheme.typography.bodySmall,modifier = Modifier.padding(top = 4.dp))
            }
            Column(Modifier.weight(1f)) {
                OutlinedTextField(
                    value = lastNameState,
                    onValueChange = { lastNameState = it },
                    label = { Text("Last Name") },
                    isError = lastNameState.isBlank()
                )
                if (lastNameState.isBlank()) Text("Last name is required", color = Color.White, style = MaterialTheme.typography.bodySmall,modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = passwordState,
            onValueChange = { passwordState = it },
            label = { Text("Password") },
            isError = passwordState.isBlank(),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (passwordState.isBlank()) Text("Password is required", color = Color.White, style = MaterialTheme.typography.bodySmall,modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPasswordState,
            onValueChange = { confirmPasswordState = it },
            label = { Text("Confirm Password") },
            isError = confirmPasswordState.isBlank() || confirmPasswordState != passwordState,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        when {
            confirmPasswordState.isBlank() -> Text("Confirm password is required", color = Color.White, style = MaterialTheme.typography.bodySmall,modifier = Modifier.padding(top = 4.dp))
            confirmPasswordState != passwordState -> Text("Passwords do not match", color = Color.White, style = MaterialTheme.typography.bodySmall,modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = termsState, onCheckedChange = { termsState = it })
            Text("I agree to the Terms & Conditions")
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { /* Preview Only */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = termsState
        ) {
            Text("Sign Up")
        }
    }
}

