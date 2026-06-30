package com.bsit.cyclesync.model

data class Ride(
    val uid: String = "",
    val username: String = "",
    val speed: Double? = null,
    val time: String = "",
    val distance: Double? = null,
    val sessionDate: String = "",
    val friends: List<String> = emptyList(),
    var eta: String = "",
    val startPosition: Place? = null,
    val destinationPosition: Place? = null,
    val timestamp: Long = System.currentTimeMillis()
)