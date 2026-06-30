package com.bsit.cyclesync.ui.calendar

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.bsit.cyclesync.model.User
import com.bsit.cyclesync.ui.utils.FirebaseUtils
import com.google.firebase.database.*

class SharedUserViewModel : ViewModel() {
    private val _friends = mutableStateListOf<User>()
    val friends: SnapshotStateList<User> get() = _friends

    init { loadFriends() }

    private fun loadFriends() {
        val currentUid = FirebaseUtils.auth.currentUser?.uid ?: return
        FirebaseUtils.userRef.child(currentUid).child("friends")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Read friends as a map
                    val friendsMap = snapshot.getValue(object : GenericTypeIndicator<Map<String, Boolean>>() {})
                        ?: emptyMap()
                    val friendIds = friendsMap.keys.toList() // get list of UIDs

                    if (friendIds.isEmpty()) {
                        _friends.clear()
                        return
                    }

                    // Fetch all users
                    FirebaseUtils.userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val allUsers = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                            _friends.clear()
                            _friends.addAll(allUsers.filter { friendIds.contains(it.uid) })
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

}
