package com.bsit.cyclesync.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object RideRepository {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val rootRef = database.reference

    /**
     * Observes samples for users/{uid}/rides/{rideId}/samples
     * limitToLast prevents huge payloads; tune limit for your app.
     */
    fun observeSamples(uid: String, rideId: String, limit: Int = 1000): Flow<List<RideSample>> =
        callbackFlow {
            val ref = rootRef
                .child("users")
                .child(uid)
                .child("rides")
                .child(rideId)
                .child("samples")
                .limitToLast(limit)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children
                        .mapNotNull { it.getValue(RideSample::class.java) }
                        .sortedBy { it.timestampMs } // keep data in order
                    trySend(list).isSuccess
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }
}