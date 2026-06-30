package com.bsit.cyclesync.ui.friendlist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendListScreen(
    onBack: () -> Unit = {},
    onAddFriends: () -> Unit = {},
    viewModel: FriendListViewModel = hiltViewModel()
) {
    val friends by remember { derivedStateOf { viewModel.users } }
    val search by remember { derivedStateOf { viewModel.search } }
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var selectedFriend by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenStart,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFriends) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Friends")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = search,
                onValueChange = viewModel::onSearch,
                label = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (friends.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No friends yet.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(friends) { friend ->
                        FriendCard(friend = friend) {
                            selectedFriend = friend
                            showDialog = true
                        }
                    }
                }
            }
        }
    }

    // Unfriend confirmation dialog
    if (showDialog && selectedFriend != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Unfriend ${selectedFriend!!.firstName}?") },
            text = { Text("Are you sure you want to remove this friend?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unfriend(selectedFriend!!)
                    Toast.makeText(
                        context,
                        "Unfriended ${selectedFriend!!.firstName}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showDialog = false
                }) { Text("Yes", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun FriendCard(
    friend: User,
    onUnfriend: () -> Unit
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

            // Friend info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (friend.username.isNotEmpty()) friend.username else "${friend.firstName} ${friend.lastName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    maxLines = 1
                )
                Text(
                    text = friend.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Unfriend button
            IconButton(onClick = onUnfriend) {
                Icon(Icons.Default.PersonRemove, contentDescription = "Unfriend", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
