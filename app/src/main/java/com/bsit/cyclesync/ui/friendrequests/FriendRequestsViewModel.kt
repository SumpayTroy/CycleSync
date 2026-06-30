package com.bsit.cyclesync.ui.friendrequests

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.bsit.cyclesync.model.User
import com.bsit.cyclesync.ui.utils.UsernameRealtimeUpdater
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FriendRequestsViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance().reference
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private val _receivedRequests = mutableStateListOf<User>()
    val receivedRequests: List<User> get() = _receivedRequests

    var message: String? = null
        private set

    var navigateToFriendList = false
        private set

    private var usernameUpdater: UsernameRealtimeUpdater? = null

    init {
        loadReceivedRequests()
    }

    // 🔹 Load received friend requests
    private fun loadReceivedRequests() {
        if (currentUid == null) return

        val receivedRef = db.child("friend_requests").child(currentUid).child("received")

        receivedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _receivedRequests.clear()

                val tempList = mutableListOf<User>()
                val usersRef = db.child("users")

                val totalChildren = snapshot.childrenCount.toInt()
                if (totalChildren == 0) {
                    usernameUpdater?.clear()
                    return
                }

                var processed = 0
                for (req in snapshot.children) {
                    val senderUid = req.key ?: continue
                    usersRef.child(senderUid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnap: DataSnapshot) {
                                userSnap.getValue(User::class.java)?.let { sender ->
                                    tempList.add(sender)
                                }
                                processed++
                                if (processed == totalChildren) {
                                    _receivedRequests.clear()
                                    _receivedRequests.addAll(tempList)

                                    // Real-time username listener
                                    usernameUpdater?.clear()
                                    usernameUpdater = UsernameRealtimeUpdater(usersRef)
                                    usernameUpdater?.attach(_receivedRequests) { updatedList ->
                                        _receivedRequests.clear()
                                        _receivedRequests.addAll(updatedList)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔹 Accept a friend request
    fun acceptRequest(sender: User) {
        if (currentUid == null) return
        val senderUid = sender.uid

        val updates = mapOf(
            "users/$currentUid/friends/$senderUid" to true,
            "users/$senderUid/friends/$currentUid" to true
        )

        db.updateChildren(updates).addOnSuccessListener {
            // ✅ Corrected Firebase paths
            db.child("friend_requests/$currentUid/received/$senderUid").removeValue()
            db.child("friend_requests/$senderUid/sent/$currentUid").removeValue()

            _receivedRequests.removeIf { it.uid == senderUid }
            message = "Friend request accepted!"
            navigateToFriendList = true
        }.addOnFailureListener {
            message = "Failed to accept request: ${it.message}"
        }
    }

    // 🔹 Decline a friend request
    fun declineRequest(sender: User) {
        if (currentUid == null) return
        val senderUid = sender.uid

        // ✅ Corrected Firebase paths
        db.child("friend_requests/$currentUid/received/$senderUid").removeValue()
        db.child("friend_requests/$senderUid/sent/$currentUid").removeValue()

        _receivedRequests.removeIf { it.uid == senderUid }
        message = "Friend request declined."
    }

    fun clearMessage() {
        message = null
    }

    fun resetNavigation() {
        navigateToFriendList = false
    }

    override fun onCleared() {
        super.onCleared()
        usernameUpdater?.clear()
    }
}
