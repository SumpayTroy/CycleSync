package com.bsit.cyclesync.data

import com.bsit.cyclesync.ui.utils.FirebaseUtils.database
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object CalendarRepository {

    private val db = FirebaseDatabase.getInstance().getReference("calendar_events")

    // Real-time flow of events for a specific user
    fun getRealtimeEventsForUser(userId: String): Flow<List<CalendarEvent>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val events = snapshot.children.mapNotNull { child ->
                    try {
                        val event = child.getValue(CalendarEvent::class.java)
                        if (event != null && (event.userId == userId || event.participants?.containsKey(userId) == true)) {
                            event
                        } else null
                    } catch (e: Exception) {
                        // Remove malformed event
                        child.ref.removeValue()
                        null
                    }
                }
                trySend(events)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        db.addValueEventListener(listener)
        awaitClose { db.removeEventListener(listener) }
    }

    suspend fun addEvent(event: CalendarEvent, friendIds: List<String> = emptyList()) {
        val newRef = db.push()
        val eventId = newRef.key ?: return
        val participants = mutableMapOf<String, Boolean>()
        participants[event.userId] = true
        friendIds.forEach { participants[it] = true }
        val eventWithParticipants = event.copy(id = eventId, participants = participants)
        newRef.setValue(eventWithParticipants).await()

        // Optional: send notifications
        sendRideNotificationToFriends(eventWithParticipants, friendIds)
    }

    suspend fun deleteEvent(eventId: String) {
        db.child(eventId).removeValue().await()
    }
    suspend fun removeUserFromEvent(eventId: String, userId: String) {
        val ref = db.child(eventId).child("participants")
        ref.child(userId).removeValue().await()
    }

    suspend fun updateEvent(eventId: String, updatedEvent: CalendarEvent) {
        db.child(eventId).setValue(updatedEvent).await()
    }

    fun sendRideNotificationToFriends(event: CalendarEvent, friendIds: List<String>) {
        val ref = FirebaseDatabase.getInstance().getReference("notifications")
        val notificationData = mapOf(
            "title" to "New Ride Scheduled!",
            "message" to "${event.title} on ${event.date} at ${event.time}",
            "timestamp" to System.currentTimeMillis()
        )
        friendIds.forEach { friendId -> ref.child(friendId).push().setValue(notificationData) }
    }
}
