package com.bsit.cyclesync.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsit.cyclesync.data.RideRepository
import com.bsit.cyclesync.data.RideSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RideViewModel : ViewModel() {
    private val _samples = MutableStateFlow<List<RideSample>>(emptyList())
    val samples: StateFlow<List<RideSample>> = _samples

    private var currentUid: String? = null
    private var currentRideId: String? = null

    fun startObserving(uid: String, rideId: String) {
        // Avoid restarting same subscription
        if (currentUid == uid && currentRideId == rideId) return
        currentUid = uid
        currentRideId = rideId

        viewModelScope.launch {
            RideRepository.observeSamples(uid, rideId).collectLatest { list ->
                _samples.value = list
            }
        }
    }
}
