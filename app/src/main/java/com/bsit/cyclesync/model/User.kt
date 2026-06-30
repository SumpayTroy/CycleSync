package com.bsit.cyclesync.model

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val password: String = "",
    val currentLocation: Place? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val friends: Map<String, Boolean> = emptyMap(),
    val isSubscribed: Boolean = false,
    val trialSessionsLeft: Int = 3
) {
    // Updated: Extract friend UIDs from the map keys
    fun getFriendList(): List<String> {
        return friends.keys.toList()  // Simple extraction from map keys
    }
}
