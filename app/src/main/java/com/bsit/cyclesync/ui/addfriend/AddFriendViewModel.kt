package com.bsit.cyclesync.ui.addfriend

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.bsit.cyclesync.model.User
import com.bsit.cyclesync.ui.utils.FirebaseUtils
import com.bsit.cyclesync.ui.utils.UsernameRealtimeUpdater
import com.google.firebase.database.*

class AddFriendViewModel : ViewModel() {

    private val auth = FirebaseUtils.auth
    private val usersRef = FirebaseUtils.userRef
    private val requestsRef = FirebaseUtils.database.getReference("friend_requests")

    private var allUsers: List<User> = emptyList()
    private var currentFriends = mutableSetOf<String>()
    private var sentRequests = mutableSetOf<String>()
    private var receivedRequests = mutableSetOf<String>()

    var search by mutableStateOf("")
        private set

    var userAdded by mutableStateOf("")
    private val _users = mutableStateOf(listOf<User>())
    val users: List<User> get() = _users.value

    var friendRequests by mutableStateOf<List<User>>(emptyList())
        private set

    var friendRequestsCount by mutableIntStateOf(0)
        private set
    // New: For messages and navigation (shared with FriendRequestsScreen)
    var message by mutableStateOf<String?>(null)
        private set
    var navigateToFriendList by mutableStateOf(false)
        private set

    init {
        loadCurrentFriends()
        loadSentRequests()
        loadReceivedRequests()
        loadAllUsers()
        loadFriendRequests()
    }

    // 🔹 Load current friends
    private fun loadCurrentFriends() {
        val uid = auth.currentUser?.uid ?: return
        usersRef.child(uid).child("friends")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentFriends = snapshot.children.mapNotNull { it.key }.toMutableSet()

                    refreshUsers()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // 🔹 Load received friend requests
    fun loadFriendRequests() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseUtils.database.reference.child("friend_requests").child(uid).child("received")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requestIds = snapshot.children.mapNotNull { it.key }
                    FirebaseUtils.userRef.get().addOnSuccessListener { data ->
                        val allUsers = data.children.mapNotNull { it.getValue(User::class.java) }
                        friendRequests = allUsers.filter { requestIds.contains(it.uid) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // Updated: Accept a friend request (now takes User for consistency)
    fun acceptFriendRequest(user: User) {
        val currentUid = auth.currentUser?.uid ?: return
        val friendUid = user.uid
        val updates = mapOf(
            "users/$currentUid/friends/$friendUid" to true,
            "users/$friendUid/friends/$currentUid" to true
        )
        FirebaseUtils.database.reference.updateChildren(updates).addOnSuccessListener {
            // Remove from requests
            FirebaseUtils.database.reference.child("friend_requests/$currentUid/received/$friendUid").removeValue()
            FirebaseUtils.database.reference.child("friend_requests/$friendUid/sent/$currentUid").removeValue()
            // Update UI
            friendRequests = friendRequests.filterNot { it.uid == friendUid }
            message = "Friend request accepted!"
            navigateToFriendList = true
        }.addOnFailureListener {
            message = "Failed to accept request: ${it.message}"
        }
    }


    // Updated: Decline a friend request (now takes User)
    fun declineFriendRequest(user: User) {
        val currentUid = auth.currentUser?.uid ?: return
        val friendUid = user.uid
        FirebaseUtils.database.reference.child("friend_requests/$currentUid/received/$friendUid").removeValue()
        FirebaseUtils.database.reference.child("friend_requests/$friendUid/sent/$currentUid").removeValue()
        friendRequests = friendRequests.filterNot { it.uid == friendUid }
        message = "Friend request declined."
    }

    // 🔹 Load sent requests
    private fun loadSentRequests() {
        val uid = auth.currentUser?.uid ?: return
        requestsRef.child(uid).child("sent")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    sentRequests = snapshot.children.mapNotNull { it.key }.toMutableSet()
                    refreshUsers()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // 🔹 Load received requests (for badge count)
    private fun loadReceivedRequests() {
        val uid = auth.currentUser?.uid ?: return
        requestsRef.child(uid).child("received")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    receivedRequests = snapshot.children.mapNotNull { it.key }.toMutableSet()
                    friendRequestsCount = receivedRequests.size
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // 🔹 Load all users
    private fun loadAllUsers() {
        val uid = auth.currentUser?.uid ?: return
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                    .filter { it.uid != uid }
                allUsers = list

                // Attach real-time username updates
                val updater = UsernameRealtimeUpdater.getInstance(usersRef)
                updater.attach(list) { updatedList ->
                    allUsers = updatedList
                    refreshUsers()
                }

                refreshUsers()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔹 Search filter refresh
    private fun refreshUsers() {
        val uid = auth.currentUser?.uid ?: return
        _users.value = allUsers.filter { user ->
            user.uid != uid && // not yourself
             !currentFriends.contains(user.uid) && // not already friends
             !sentRequests.contains(user.uid) && // not someone you already sent request to
             !receivedRequests.contains(user.uid) && // not someone who already sent you a request
                    (search.isEmpty() ||
                            user.username.contains(search, ignoreCase = true) ||
                            user.firstName.contains(search, ignoreCase = true) ||
                            user.lastName.contains(search, ignoreCase = true) ||
                            user.email.contains(search, ignoreCase = true))
        }
    }

    fun onSearch(query: String) {
        search = query
        refreshUsers()
    }

    // 🔹 Send friend request
    fun sendFriendRequest(targetUser: User) {
        val uid = auth.currentUser?.uid ?: return
        val targetUid = targetUser.uid

        if (uid == targetUid) return
        if (sentRequests.contains(targetUid) || currentFriends.contains(targetUid)) return

        val updates = hashMapOf<String, Any>(
            "/friend_requests/$uid/sent/$targetUid" to true,
            "/friend_requests/$targetUid/received/$uid" to true
        )

        FirebaseUtils.database.reference.updateChildren(updates)
            .addOnSuccessListener {
                sentRequests.add(targetUid)
                refreshUsers()
                userAdded = targetUser.username
            }
    }
    // New: Clear message (for UI feedback)
    fun clearMessage() {
        message = null
    }
    // New: Reset navigation flag
    fun resetNavigation() {
        navigateToFriendList = false
    }

    override fun onCleared() {
        super.onCleared()
        UsernameRealtimeUpdater.clear()
    }
}
