package com.bsit.cyclesync.ui.historyride

import androidx.lifecycle.ViewModel
import com.bsit.cyclesync.model.Ride
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

data class HistoryUiState(
    val isLoading: Boolean = false,
    val historyList: List<Ride> = emptyList(),
    val error: String? = null
)

class HistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState

    private var ridesListener: ValueEventListener? = null
    private val db = FirebaseDatabase.getInstance().getReference("rides")

    /** Listen for real-time updates of user's ride history */
    fun loadHistory(uid: String) {
        _uiState.value = HistoryUiState(isLoading = true)

        // Remove old listener to avoid duplicates
        ridesListener?.let { db.child(uid).removeEventListener(it) }

        ridesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rides = snapshot.children.mapNotNull { it.getValue(Ride::class.java) }
                    .map { ride ->
                        val distanceRaw = ride.distance ?: 0.0
                        val speedRaw = ride.speed ?: 0.0

                        val distance = String.format("%.2f", distanceRaw).toDouble()
                        val speed = String.format("%.2f", speedRaw).toDouble()

                        ride.eta = if (speed > 0) {
                            val etaMinutes = ((distance / speed) * 60).roundToInt()
                            val hours = etaMinutes / 60
                            val minutes = etaMinutes % 60
                            when {
                                hours > 0 -> "$hours hr ${minutes} min"
                                else -> "$minutes min"
                            }
                        } else "N/A"

                        ride.copy(distance = distance, speed = speed, eta = ride.eta)
                    }
                    .sortedByDescending { it.sessionDate }

                _uiState.value = HistoryUiState(historyList = rides)
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = HistoryUiState(error = error.message)
            }
        }

        db.child(uid).addValueEventListener(ridesListener as ValueEventListener)
    }

    /** Delete ride by session date */
    fun deleteHistory(uid: String, sessionDate: String) {
        db.child(uid).get().addOnSuccessListener { snapshot ->
            for (rideSnap in snapshot.children) {
                val date = rideSnap.child("sessionDate").getValue(String::class.java)
                if (date == sessionDate) {
                    rideSnap.ref.removeValue()
                    break
                }
            }
        }.addOnFailureListener {
            _uiState.value = _uiState.value.copy(error = it.message)
        }
    }

    /** Clean up Firebase listener */
    override fun onCleared() {
        super.onCleared()
        val uid = FirebaseDatabase.getInstance().reference.key ?: return
        ridesListener?.let { db.child(uid).removeEventListener(it) }
    }
}
