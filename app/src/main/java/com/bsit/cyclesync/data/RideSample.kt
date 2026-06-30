package com.bsit.cyclesync.data

data class RideSample(
    val timestampMs: Long = 0L,
    val speedMps: Double = 0.0,        // meters per second
    val distanceMeters: Double = 0.0,  // cumulative distance in meters
    val elevationMeters: Double = 0.0  // meters
)
