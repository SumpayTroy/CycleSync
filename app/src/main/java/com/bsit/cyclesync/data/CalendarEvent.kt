package com.bsit.cyclesync.data
data class CalendarEvent(
    val id: String = "",
    val userId: String = "", // creator’s UID
    val creatorName: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val participants: Map<String, Boolean>? = null,
    val friends: List<String> = emptyList()
)

