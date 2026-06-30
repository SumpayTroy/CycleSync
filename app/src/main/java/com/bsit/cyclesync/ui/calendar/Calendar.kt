package com.bsit.cyclesync.ui.calendar

import android.app.TimePickerDialog
import android.widget.CalendarView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsit.cyclesync.data.CalendarEvent
import com.bsit.cyclesync.ui.theme.GreenStart
import com.google.firebase.auth.FirebaseAuth
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit = {},
    viewModel: CalendarViewModel = viewModel(),
    sharedUserViewModel: SharedUserViewModel = viewModel()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return

    val events by viewModel.events.collectAsState()
    val friends = sharedUserViewModel.friends

    var selectedDate by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    val selectedFriends = remember { mutableStateListOf<String>() }

    var eventTitle by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("8:00 AM") }
    var destination by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf<CalendarEvent?>(null) }

    val primaryColor = GreenStart
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    LaunchedEffect(userId) {
        viewModel.startRealtimeUpdates(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CycleSync Calendar", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = primaryColor,
                onClick = {
                    if (selectedDate.isEmpty()) {
                        Toast.makeText(context, "Please select a date first", Toast.LENGTH_SHORT).show()
                    } else if (!viewModel.isDateInFutureOrToday(selectedDate)) {
                        Toast.makeText(context, "You cannot schedule a past date", Toast.LENGTH_SHORT).show()
                    } else {
                        showDialog = true
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 80.dp)
                .background(backgroundColor)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            // Loading animation
            if (friends.isEmpty() && events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenStart)
                }
            } else {
                AnimatedVisibility(visible = friends.isNotEmpty() || events.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            // Calendar Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        CalendarView(ctx).apply {
                            setOnDateChangeListener { _, year, month, day ->
                                selectedDate = String.format(
                                    Locale.getDefault(),
                                    "%02d/%02d/%04d",
                                    month + 1, day, year
                                )
                                Toast.makeText(ctx, "Selected: $selectedDate", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            // Rides display by category
            if (selectedDate.isNotEmpty()) {
                val filtered = events.filter { it.date == selectedDate }

                if (filtered.isEmpty()) {
                    Text("No rides for $selectedDate", color = textColor.copy(alpha = 0.7f))
                } else {
                    val yourRides = filtered.filter { it.userId == userId }
                    val invitedRides = filtered.filter { it.userId != userId }

                    if (yourRides.isNotEmpty()) {
                        Text(
                            "🟢 Your Rides on $selectedDate",
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        yourRides.forEach { event ->
                            val invitedFriends = event.friends.mapNotNull { id ->
                                friends.find { it.uid == id }?.username
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(event.title, style = MaterialTheme.typography.titleMedium, color = textColor)
                                    Text("🕒 ${event.time}", color = textColor.copy(alpha = 0.8f))
                                    Text("📍 ${event.location}", color = textColor.copy(alpha = 0.8f))
                                    if (event.description.isNotBlank())
                                        Text(event.description, color = textColor.copy(alpha = 0.7f))

                                    if (invitedFriends.isNotEmpty()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "👥 Invited: ${invitedFriends.joinToString(", ")}",
                                            color = primaryColor,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(onClick = {
                                            editingEvent = event
                                            eventTitle = event.title
                                            eventDescription = event.description
                                            startTime = event.time
                                            destination = event.location
                                            selectedFriends.clear()
                                            selectedFriends.addAll(event.friends)
                                            showDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = primaryColor)
                                        }
                                        IconButton(onClick = {
                                                showDeleteDialog = event
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (invitedRides.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "🔵 Invited Rides on $selectedDate",
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        invitedRides.forEach { event ->
                            val creatorLabel = if (event.userId == userId) {
                                "👤 Created by You"
                            } else {
                                val creatorName = friends.find { it.uid == event.userId }?.username ?: "Unknown"
                                "👤 Created by: $creatorName"
                            }
                            val invitedFriends = event.friends.mapNotNull { id ->
                                friends.find { it.uid == id }?.username
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(event.title, style = MaterialTheme.typography.titleMedium, color = textColor)
                                    Text("🕒 ${event.time}", color = textColor.copy(alpha = 0.8f))
                                    Text("📍 ${event.location}", color = textColor.copy(alpha = 0.8f))
                                    if (event.description.isNotBlank())
                                        Text(event.description, color = textColor.copy(alpha = 0.7f))

                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = creatorLabel,
                                        color = primaryColor
                                    )
                                    if (invitedFriends.isNotEmpty()) {
                                        Text(
                                            "👥 Other Invited: ${invitedFriends.joinToString(", ")}",
                                            color = primaryColor,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(onClick = {
                                            showDeleteDialog = event
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove from Calendar",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 🔹 All Upcoming Rides
            Spacer(Modifier.height(20.dp))

            Text(
                "All Upcoming Rides",
                style = MaterialTheme.typography.titleMedium,
                color = primaryColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            val upcoming = events.sortedBy { it.date }

            upcoming.forEach { event ->
                val isCreator = event.userId == userId
                val isInvited = event.friends.contains(userId)

                val creatorName = if (isCreator) {
                    "You"
                } else {
                    friends.find { it.uid == event.userId }?.username ?: "Unknown"
                }

                val invitedFriends = event.friends.mapNotNull { id ->
                    friends.find { it.uid == id }?.username
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {

                        Text(
                            event.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor
                        )
                        Text("Date: ${event.date}", color = textColor.copy(alpha = 0.8f))
                        Text("🕒 ${event.time}", color = textColor.copy(alpha = 0.8f))
                        Text("📍 ${event.location}", color = textColor.copy(alpha = 0.8f))

                        if (event.description.isNotBlank())
                            Text(event.description, color = textColor.copy(alpha = 0.7f))

                        Spacer(Modifier.height(6.dp))

                        // Creator
                        Text("👤 Created by: $creatorName", color = primaryColor)

                        // Invited Friends
                        if (invitedFriends.isNotEmpty()) {
                            Text(
                                "👥 Invited: ${invitedFriends.joinToString(", ")}",
                                color = primaryColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                }
            }

            // Confirmation Dialog
            if (showDeleteDialog != null) {
                val event = showDeleteDialog!!
                val isCreator = event.userId == userId

                AlertDialog(
                    onDismissRequest = { showDeleteDialog = null },
                    title = { Text("Remove Ride", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            if (isCreator)
                                "Do you want to delete this ride for everyone?"
                            else
                                "Do you want to remove this ride from your calendar?"
                        )
                    },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            onClick = {
                                viewModel.deleteEvent(event, userId)
                                Toast.makeText(
                                    context,
                                    if (isCreator) "Ride deleted for everyone" else "Removed from your calendar",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showDeleteDialog = null
                            }
                        ) {
                            Text("Confirm", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // 🚲 Add/Edit Ride Dialog
            if (showDialog) {
                var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(if (editingEvent != null) "Edit Ride" else "Schedule Ride", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextField(value = eventTitle, onValueChange = { eventTitle = it }, label = { Text("Ride Title") })
                            TextField(value = destination, onValueChange = { destination = it }, label = { Text("Destination / Route") })
                            TextField(value = eventDescription, onValueChange = { eventDescription = it }, label = { Text("Description") })

                            TextField(
                                value = startTime,
                                onValueChange = {},
                                label = { Text("Start Time") },
                                readOnly = true,
                                trailingIcon = {
                                    TextButton(onClick = {
                                        val cal = Calendar.getInstance()
                                        TimePickerDialog(
                                            context,
                                            { _, h, m ->
                                                startTime = String.format(
                                                    Locale.getDefault(),
                                                    "%02d:%02d %s",
                                                    if (h % 12 == 0) 12 else h % 12,
                                                    m,
                                                    if (h >= 12) "PM" else "AM"
                                                )
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            false
                                        ).show()
                                    }) { Text("Pick") }
                                }
                            )

                            Text("Invite Friends", color = primaryColor)
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Search friends...") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            val filteredFriends = if (searchQuery.text.isNotEmpty()) {
                                friends.filter { it.username?.contains(searchQuery.text, ignoreCase = true) == true }
                            } else friends

                            if (filteredFriends.isEmpty()) {
                                Text("No friends found.", color = textColor.copy(alpha = 0.6f))
                            } else {
                                Column {
                                    filteredFriends.forEach { friend ->
                                        val friendId = friend.uid ?:return@forEach
                                        val friendName = friend.username ?:"Unknown"
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (selectedFriends.contains(friendId)) selectedFriends.remove(friendId)
                                                    else selectedFriends.add(friendId)
                                                }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Checkbox(
                                                checked = selectedFriends.contains(friendId),
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedFriends.add(friendId)
                                                    else selectedFriends.remove(friendId)
                                                }
                                            )
                                            Text(friendName, color = textColor)
                                        }
                                    }
                                }
                            }

                        }
                    },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            onClick = {
                                if (!viewModel.isDateInFutureOrToday(selectedDate)) {
                                    Toast.makeText(context, "Cannot schedule in the past", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (eventTitle.isNotBlank()) {
                                    if (editingEvent != null) {
                                        // Only allow creator to edit
                                        if (editingEvent!!.userId == userId) {
                                            val updatedEvent = editingEvent!!.copy(
                                                title = eventTitle,
                                                description = eventDescription,
                                                time = startTime,
                                                location = destination,
                                                friends = selectedFriends.toList(),
                                                participants = (editingEvent!!.participants ?: mutableMapOf()).toMutableMap().apply {
                                                    this[userId] = true
                                                    selectedFriends.forEach { this[it] = true }
                                                }
                                            )
                                            viewModel.updateEvent(editingEvent!!.id, updatedEvent, userId)
                                            Toast.makeText(context, "Ride updated!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Only the creator can edit this ride", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        val newEvent = CalendarEvent(
                                            title = eventTitle,
                                            description = eventDescription,
                                            date = selectedDate,
                                            time = startTime,
                                            location = destination,
                                            userId = userId,
                                            creatorName = sharedUserViewModel.friends.find { it.uid == userId }?.username ?: "You",
                                            friends = selectedFriends.toList(),
                                            participants = mutableMapOf<String, Boolean>().apply {
                                                this[userId] = true
                                                selectedFriends.forEach { this[it] = true }
                                            }
                                        )
                                        viewModel.addEvent(newEvent, userId, selectedFriends.toList())
                                        val invitedNames = friends.filter { selectedFriends.contains(it.uid) }
                                            .joinToString(", ") { it.username ?:"Unknown" }
                                        Toast.makeText(context, "Ride scheduled! Invited: $invitedNames", Toast.LENGTH_LONG).show()
                                    }

                                    selectedFriends.clear()
                                    showDialog = false
                                } else {
                                    Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(if (editingEvent != null) "Update" else "Save", color = Color.White)
                        }
                    },
                    dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
                )
            }
        }
    }
}
