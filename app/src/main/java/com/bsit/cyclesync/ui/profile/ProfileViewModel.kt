package com.bsit.cyclesync.ui.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsit.cyclesync.ui.utils.FirebaseUtils
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf


@HiltViewModel
class ProfileViewModel @Inject constructor(): ViewModel() {

    /**
     * Logs out the current user.
     * @param onComplete A callback function to be invoked after the logout process is finished.
     */

    val currentUserId: String
        get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _username = MutableStateFlow("Loading...")
    val username: StateFlow<String> = _username

    private val _friendRequestsCount = mutableIntStateOf(0)
    val friendRequestsCount: State<Int> = _friendRequestsCount


    init {
        loadUsername()
        loadFriendRequestsCount()
    }

    private fun loadUsername() {
        val uid = FirebaseUtils.auth.currentUser?.uid ?: return
        FirebaseUtils.database.reference.child("users").child(uid)
            .child("username")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener
            {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    _username.value = snapshot.getValue(String::class.java) ?: "Unknown"
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })

    }
    fun loadFriendRequestsCount() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseUtils.database.reference.child("friend_requests").child(uid).child("received")

        ref.get().addOnSuccessListener { snapshot ->
            _friendRequestsCount.value = snapshot.childrenCount.toInt()
        }

        // Optional: listen in real-time
        ref.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                _friendRequestsCount.value = snapshot.childrenCount.toInt()
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private val _calendarNotificationCount = mutableStateOf(0)
    val calendarNotificationCount: State<Int> = _calendarNotificationCount

    fun loadCalendarNotifications(userId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("notifications").child(userId)

        ref.get().addOnSuccessListener { snapshot ->
            val count = snapshot.childrenCount.toInt()
            _calendarNotificationCount.value = count
        }
    }
    fun onLogout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                FirebaseUtils.auth.signOut()
                println("User logged out successfully.")
            } catch (e: Exception) {
                println("Error during logout: ${e.message}")
            } finally {
                onComplete()
            }
        }
    }
    fun clearCalendarNotifications(userId: String) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("notifications")
            .child(userId)

        ref.removeValue().addOnSuccessListener {
            _calendarNotificationCount.value = 0
        }
    }

}