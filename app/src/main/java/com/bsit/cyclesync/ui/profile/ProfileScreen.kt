package com.bsit.cyclesync.ui.profile

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bsit.cyclesync.ui.theme.GreenEnd
import com.bsit.cyclesync.ui.theme.GreenStart
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.bsit.cyclesync.R


@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = GreenStart,
    notificationCount: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )
        if (notificationCount > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 10.dp, end = 4.dp)
                    .size(22.dp)
                    .background(Color.Red, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = notificationCount.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
    onLoggedOut: () -> Unit = {},
    onFriendListClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onAddFriendListClick: () -> Unit = {}
) {
    val mediaPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val calendarCount by viewModel.calendarNotificationCount
    val friendRequestsCount by viewModel.friendRequestsCount

    LaunchedEffect(Unit) {
        viewModel.loadCalendarNotifications(viewModel.currentUserId) // replace with your actual userId
    }


    LaunchedEffect(mediaPermissionState.status.isGranted) {
        if (!mediaPermissionState.status.isGranted) {
            mediaPermissionState.launchPermissionRequest()
        }
    }

    val username by viewModel.username.collectAsState(initial = "Cyclist")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(GreenStart, GreenEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Picture",
                                tint = Color.White,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Hello, $username!",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Manage your account and activities",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Card Section for Menu Items
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Default.Group,
                            label = "Friend List",
                            onClick = onFriendListClick
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        ProfileMenuItem(
                            icon = Icons.Default.PersonAdd,
                            label = "Add Person",
                            onClick = onAddFriendListClick,
                            notificationCount = friendRequestsCount
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        ProfileMenuItem(
                            icon = Icons.Default.CalendarMonth,
                            label = "Calendar",
                            onClick = {
                                viewModel.clearCalendarNotifications(viewModel.currentUserId)
                                onCalendarClick()
                            },
                            notificationCount = calendarCount
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        ProfileMenuItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            onClick = onSettingsClick
                        )
                    }
                }
            }

            // Logout Button
            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .padding(horizontal = 40.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenStart,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(text = "Log Out", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(30.dp))
                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        title = { Text("Confirm Logout") },
                        text = { Text("Are you sure you want to log out of your account?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showLogoutDialog = false
                                    viewModel.onLogout { onLoggedOut() }
                                }
                            ) {
                                Text("Logout")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogoutDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(
        onLoggedOut = {},
        onFriendListClick = {},
        onSettingsClick = {},
        onCalendarClick = {},
        onAddFriendListClick = {}
    )
}
