package com.bsit.cyclesync.data

import com.bsit.cyclesync.model.Session
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getUserSessions(uid: String): Flow<List<Session>> = callbackFlow {
        val listener = firestore.collection("sessions")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { it.toObject(Session::class.java) } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }
}

