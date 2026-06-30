package com.bsit.cyclesync.ui.profile_settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bsit.cyclesync.ui.theme.GreenStart
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onThemeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseDatabase.getInstance().reference
    val user = auth.currentUser
    val bannedWords = listOf(
        "fuck", "shit", "bitch", "asshole", "dick", "pussy", "faggot",
        "nigger", "cunt", "slut", "whore", "bastard","gago","gagi","tanginamo", "tang ina mo",
        "pakyu","mamatay kana"
    )

    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
    var darkModeEnabled by rememberSaveable { mutableStateOf(isDarkTheme) }

    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            db.child("users").child(uid).child("notificationsEnabled")
                .get()
                .addOnSuccessListener { snapshot ->
                    notificationsEnabled = snapshot.getValue(Boolean::class.java) ?: true
                }
        }
    }


    var newUsername by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    fun containsBadWord(input: String): Boolean {
        val lowerInput = input.lowercase()
        return bannedWords.any { badWord ->
            lowerInput.contains(badWord)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenStart)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            //  Preferences Section
            SettingsSection(title = "Preferences") {
                SettingsItem(
                    title = "Enable Notifications",
                    subtitle = "Receive updates and ride reminders",
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        notificationsEnabled = enabled

                        // Save preference in Firebase
                        user?.uid?.let { uid ->
                            db.child("users").child(uid).child("notificationsEnabled")
                                .setValue(enabled)
                                .addOnSuccessListener {
                                    // ✅ Save locally too
                                    val prefs = context.getSharedPreferences("CycleSyncPrefs", Context.MODE_PRIVATE)
                                    prefs.edit().putBoolean("notificationsEnabled", enabled).apply()

                                    Toast.makeText(context,
                                        if (enabled) "Notifications Enabled" else "Notifications Disabled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to update: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                )

                SettingsItem(
                    title = "Dark Mode",
                    subtitle = "Switch between light and dark themes",
                    checked = darkModeEnabled,
                    onCheckedChange = {
                        darkModeEnabled = it
                        onThemeChange(it)
                    }
                )
            }

            //  Account Section
            SettingsSection(title = "Account") {
                // 🧍‍♂️ Change Username
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("New Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        when {
                            newUsername.isBlank() -> {
                                Toast.makeText(context, "Please enter a valid username", Toast.LENGTH_SHORT).show()
                            }
                            containsBadWord(newUsername) -> {
                                Toast.makeText(
                                    context,
                                    "Username contains inappropriate words. Please change it.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> {
                                showConfirmDialog = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenStart)
                ) {
                    Text("Update Username")
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = GreenStart
                    )
                }
            }

            //  About Section
            SettingsSection(title = "About") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CycleSync v1.0.0",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Developed for your cycling journeys 🚴",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    //  Confirmation Dialog
    if (showConfirmDialog && user != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Username Change") },
            text = { Text("Are you sure you want to change your username to \"$newUsername\"?") },
            confirmButton = {
                Button(onClick = {
                    // Double-check before saving
                    if (containsBadWord(newUsername)) {
                        Toast.makeText(
                            context,
                            "Username contains inappropriate words. Please change it.",
                            Toast.LENGTH_LONG
                        ).show()
                        showConfirmDialog = false
                        return@Button
                    }

                    showConfirmDialog = false
                    isLoading = true

                    db.child("users").child(user.uid).child("username")
                        .setValue(newUsername)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Username updated!", Toast.LENGTH_SHORT).show()
                            newUsername = ""
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener { isLoading = false }
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
