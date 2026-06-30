package com.bsit.cyclesync.ui.addfriend

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bsit.cyclesync.model.User
import com.bsit.cyclesync.ui.theme.GreenStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    onBack: () -> Unit = {},
    onOpenFriendRequests: () -> Unit = {},
    viewModel: AddFriendViewModel = hiltViewModel()
) {
    val users by remember { derivedStateOf { viewModel.users } }
    val friendRequests by remember { derivedStateOf { viewModel.friendRequests } }
    val search by remember { derivedStateOf { viewModel.search } }
    val context = LocalContext.current
    val addedUsername by remember { derivedStateOf { viewModel.userAdded } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add & Manage Friends") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenStart,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // --- Friend Requests Button ---
            Button(
                onClick = onOpenFriendRequests,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenStart,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("View Friend Requests (${friendRequests.size})")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Add Friend Section ---
            Text(
                text = "Search & Add Friends",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = search,
                onValueChange = viewModel::onSearch,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                placeholder = { Text("Search by username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No users found. Try another search!", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(users) { user ->
                        AddFriendCard(user = user) {
                            viewModel.sendFriendRequest(user)
                        }
                    }
                }
            }
        }
    }

    // --- Toast for request sent ---
    LaunchedEffect(addedUsername) {
        if (addedUsername.isNotEmpty()) {
            Toast.makeText(context, "Friend request sent to $addedUsername", Toast.LENGTH_SHORT).show()
        }
    }
}
@Composable
fun AddFriendCard(
    user: User,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Avatar", tint = GreenStart)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (user.username.isNotEmpty()) user.username else "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    maxLines = 1
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Add Friend button
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend", tint = GreenStart)
            }
        }
    }
}
