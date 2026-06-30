package com.bsit.cyclesync.model


data class Session(
    val sessionId: String = "",
    val sessionOwner: String = "",
    val members: List<Ride> = emptyList(),
    val startPosition: Place? = null,
    val destinationPosition: Place? = null,
    val active: Boolean = false,
    val sessionDate: String = "",
    val sessionOwnerSpeed: Double? = null,
    val sessionOwnerTime: Long? = null
)
