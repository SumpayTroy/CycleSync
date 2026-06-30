package com.bsit.cyclesync.ui.friendlist

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.bsit.cyclesync.model.User
import com.bsit.cyclesync.ui.utils.FirebaseUtils
import com.bsit.cyclesync.ui.utils.UsernameRealtimeUpdater
import com.google.firebase.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FriendListViewModel @Inject constructor() : ViewModel() {

    private val usersRef = FirebaseUtils.userRef
    private val auth = FirebaseUtils.auth

    private var currentUser: User? by mutableStateOf(null)
    private var allUsers: List<User> = emptyList()

    var users by mutableStateOf(listOf<User>())
        private set

    var search by mutableStateOf("")
        private set


    private var currentUserListener: ValueEventListener? = null

    init {
        getCurrentUserData()
    }

    private fun getCurrentUserData() {
        val uid = auth.currentUser?.uid ?: return

        currentUserListener?.let { usersRef.child(uid).removeEventListener(it) }

        currentUserListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
                loadFriendsList()
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        usersRef.child(uid).addValueEventListener(currentUserListener as ValueEventListener)
    }

    private fun loadFriendsList() {
        val friendIds = currentUser?.getFriendList() ?: emptyList()
        if (friendIds.isEmpty()) {
            users = emptyList()
            return
        }

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                val friendList = allUsers.filter { friendIds.contains(it.uid) }

                users = friendList

                // Attach realtime username listener
                val updater = UsernameRealtimeUpdater.getInstance(usersRef)
                updater.attach(friendList) { updatedList ->
                    users = updatedList
                }

            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun onSearch(query: String) {
        search = query
        val friendIds = currentUser?.getFriendList() ?: return
        users = if (query.isEmpty()) {
            allUsers.filter { friendIds.contains(it.uid) }
        } else {
            allUsers.filter {
                friendIds.contains(it.uid) &&
                        (it.username.contains(query, true) ||
                                "${it.firstName} ${it.lastName}".contains(query, true) ||
                                it.email.contains(query, true))
            }
        }
    }

    fun unfriend(friend: User) {
        val uid = currentUser?.uid ?: return

        // Updated: Work with the map format
        val updatedFriendsMap = currentUser?.friends?.toMutableMap() ?: mutableMapOf()
        updatedFriendsMap.remove(friend.uid)  // Remove the friend UID from the map
        usersRef.child(uid).child("friends").setValue(updatedFriendsMap)  // Set the updated map

        // Updated: Also update the other user's friends map
        usersRef.child(friend.uid).child("friends")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val friendFriendsMap = snapshot.getValue(object : GenericTypeIndicator<Map<String, Boolean>>() {}) ?: emptyMap()
                    val updatedFriendMap = friendFriendsMap.toMutableMap()
                    updatedFriendMap.remove(uid)  // Remove current user from friend's map
                    usersRef.child(friend.uid).child("friends").setValue(updatedFriendMap)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onCleared() {
        super.onCleared()
        currentUserListener?.let {
            usersRef.child(auth.currentUser?.uid ?: return).removeEventListener(it)
        }

        // Clear shared username listeners
        UsernameRealtimeUpdater.clear()
    }
}
