package com.bsit.cyclesync.ui.calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsit.cyclesync.data.CalendarEvent
import com.bsit.cyclesync.data.CalendarRepository
import com.bsit.cyclesync.ui.utils.FirebaseUtils.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events

    fun startRealtimeUpdates(userId: String) {
        viewModelScope.launch {
            CalendarRepository.getRealtimeEventsForUser(userId).collect { eventList ->
                _events.value = eventList
            }
        }
    }

    fun addEvent(event: CalendarEvent, userId: String, friendIds: List<String> = emptyList()) {
        viewModelScope.launch {
            CalendarRepository.addEvent(event.copy(userId = userId), friendIds)
        }
    }

    fun deleteEvent(event: CalendarEvent, userId: String) {
        viewModelScope.launch {
            if (event.userId == userId) {
                // Full delete (creator)
                CalendarRepository.deleteEvent(event.id)
            } else {
                // Remove only for that member
                CalendarRepository.removeUserFromEvent(event.id, userId)
            }
        }
    }

    fun updateEvent(eventId: String, updated: CalendarEvent, userId: String) {
        if (updated.userId == userId) {
            viewModelScope.launch {
                CalendarRepository.updateEvent(eventId, updated)
            }
        } else {
            println("Unauthorized update attempt by user $userId on event $eventId")
        }
    }


    fun isDateInFutureOrToday(dateString: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            sdf.isLenient = false

            val selectedDate = sdf.parse(dateString)
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            selectedDate != null && !selectedDate.before(today)
        } catch (e: Exception) {
            false
        }
    }

}

