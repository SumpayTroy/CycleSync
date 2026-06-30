package com.bsit.cyclesync.ui.friendrequests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bsit.cyclesync.model.User
import com.bsit.cyclesync.ui.theme.GreenStart
import kotlinx.coroutines.launch
import com.bsit.cyclesync.ui.addfriend.AddFriendViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(
    onBack: () -> Unit = {},
    onFriendAccepted: () -> Unit = {},
    viewModel: AddFriendViewModel = hiltViewModel()
) {
    val requests by remember { derivedStateOf { viewModel.friendRequests } }
    val message by remember { derivedStateOf { viewModel.message } }
    val navigateToFriendList by remember { derivedStateOf { viewModel.navigateToFriendList } }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar messages
    LaunchedEffect(message) {
        message?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearMessage()
        }
    }

    // Navigation trigger when accepted
    LaunchedEffect(navigateToFriendList) {
        if (navigateToFriendList) {
            onFriendAccepted()
            viewModel.resetNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friend Requests") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (requests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No pending friend requests")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(requests) { user ->
                        FriendRequestCard(
                            user = user,
                            onAccept = { viewModel.acceptFriendRequest(user) },
                            onDecline = { viewModel.declineFriendRequest(user) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendRequestCard(
    user: User,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = user.username.ifEmpty { "${user.firstName} ${user.lastName}" },
                    style = MaterialTheme.typography.titleMedium
                )
                if (user.email.isNotEmpty()) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenStart)
                ) { Text("Accept") }

                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Decline") }
            }
        }
    }
}
