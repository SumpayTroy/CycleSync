package com.bsit.cyclesync.ui.utils

import com.bsit.cyclesync.model.User
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

/**
 * Keeps user info (like username) synced in real time across friend lists, requests, etc.
 */
class UsernameRealtimeUpdater(private val usersRef: DatabaseReference) {

    private val listeners = mutableMapOf<String, ValueEventListener>()

    /**
     * Attaches real-time listeners for a list of users.
     * Whenever any of those users update their info (username, etc.), the callback fires.
     */
    fun attach(users: List<User>, onUpdate: (List<User>) -> Unit) {
        clear() // clear old listeners
        val updatedList = users.toMutableList()

        for ((index, user) in users.withIndex()) {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updatedUser = snapshot.getValue(User::class.java)
                    if (updatedUser != null) {
                        updatedList[index] = updatedUser
                        onUpdate(updatedList.toList()) // trigger update callback
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            user.uid?.let { uid ->
                usersRef.child(uid).addValueEventListener(listener)
                listeners[uid] = listener
            }
        }
    }

    /** Remove all active listeners. */
    fun clear() {
        for ((uid, listener) in listeners) {
            usersRef.child(uid).removeEventListener(listener)
        }
        listeners.clear()
    }

    companion object {
        private var instance: UsernameRealtimeUpdater? = null

        /** Singleton access (shared across ViewModels) */
        fun getInstance(usersRef: DatabaseReference): UsernameRealtimeUpdater {
            if (instance == null) {
                instance = UsernameRealtimeUpdater(usersRef)
            }
            return instance!!
        }

        /** Clear all global listeners */
        fun clear() {
            instance?.clear()
        }
    }
}
