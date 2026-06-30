package com.bsit.cyclesync.ui.historyride

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bsit.cyclesync.ui.theme.GreenStart
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedSessionDate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { viewModel.loadHistory(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ride History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenStart)
            )
        }
    ) { innerPadding ->

        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = GreenStart) }

            state.error != null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("Error: ${state.error}", color = Color.Red, textAlign = TextAlign.Center) }

            state.historyList.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("No ride history found", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium) }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF9F9F9))
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                //  Sort by timestamp (latest first)
                val sortedRides = state.historyList.sortedByDescending { it.timestamp }

                items(sortedRides) { item ->

                    // Format date + time
                    val formattedDateTime = try {
                        val sdf = SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.getDefault())
                        sdf.format(Date(item.timestamp))
                    } catch (e: Exception) {
                        item.sessionDate
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                            // Header
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // Username (top)
                                    Text(
                                        text = item.username,
                                        color = GreenStart,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )

                                    // Formatted Date (below username)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = GreenStart.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = formattedDateTime,
                                            color = GreenStart,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Delete icon on the right side
                                IconButton(
                                    onClick = {
                                        selectedSessionDate = item.sessionDate
                                        showDeleteDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red
                                    )
                                }
                            }


                            Divider(color = Color(0xFFE0E0E0), thickness = 2.dp)

                            // Stats
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                StatItem(label = "Speed", value = "${item.speed} km/h")
                                StatItem(label = "Distance", value = "${item.distance} km")
                                StatItem(label = "Time", value = item.time)
                            }

                            // From / To
                            item.startPosition?.name?.let { Text("From: $it", color = Color.DarkGray, fontSize = 14.sp) }
                            item.destinationPosition?.name?.let { Text("To: $it", color = Color.DarkGray, fontSize = 14.sp) }

                            // ETA
                            if (item.eta != "N/A") Text("ETA: ${item.eta}", color = Color.Black, fontSize = 14.sp)

                            // Friends
                            if (item.friends.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                    Text("Friends: ", color = Color(0xFF4A4A4A), fontSize = 13.sp)
                                    item.friends.forEach { friend ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = GreenStart.copy(alpha = 0.1f),
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            Text(
                                                friend,
                                                fontSize = 12.sp,
                                                color = GreenStart,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete dialog
        if (showDeleteDialog && selectedSessionDate != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Ride", color = GreenStart, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this ride? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        FirebaseAuth.getInstance().currentUser?.uid?.let {
                            viewModel.deleteHistory(it, selectedSessionDate!!)
                        }
                        showDeleteDialog = false
                    }) { Text("Yes, Delete", color = Color.Red, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
