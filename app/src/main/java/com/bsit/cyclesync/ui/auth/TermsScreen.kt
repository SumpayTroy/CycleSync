package com.bsit.cyclesync.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    onBackClick: () -> Unit //callback for back navigation
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Privacy") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Terms of Service & Privacy Policy",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(16.dp))
            Text(
                """
Welcome to CycleSync. By using our application, you agree to the following terms regarding the collection and use of your personal data.

1. Data Collection
CycleSync may collect basic personal information such as your name, email address, and account credentials. Additionally, the app may collect location data during your rides in order to provide real-time tracking, meetup coordination, and ride history features.

2. Purpose of Data Use
The information we collect is used solely to:
- Enable authentication and secure login
- Provide real-time location sharing with friends or group members
- Display ride history and routes
- Improve the app’s performance and user experience

3. Data Sharing
Your personal and location data will not be shared with third parties, except as required by law, or when you choose to share your ride details with others through the app’s features.

4. Data Security
We take reasonable measures to protect your personal information and location data from unauthorized access, alteration, or disclosure. All sensitive data is transmitted securely and stored using Firebase services.

5. User Responsibility
You agree not to misuse CycleSync, including but not limited to:
- Providing false information
- Attempting to access another user’s account
- Using the app in unsafe conditions (e.g., while riding without caution)
- **Creating usernames that contain foul, offensive, or inappropriate language**  
CycleSync maintains a zero-tolerance policy for the use of vulgar, discriminatory, or harmful words in usernames.
- **Using special characters or symbols in your first name or last name**  
 first and last names must only contain standard alphabetic characters.

6. Data Retention
Your data will be retained as long as your account is active.

7. Updates to Policy
CycleSync may update these Terms and Privacy Policy from time to time. You will be notified of significant changes through the app.

By using CycleSync, you consent to the collection and use of your personal data as described in this policy.
                """.trimIndent()
            )
        }
    }
}
