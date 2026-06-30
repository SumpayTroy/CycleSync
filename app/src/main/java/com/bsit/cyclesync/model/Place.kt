package com.bsit.cyclesync.model

import com.google.android.gms.maps.model.LatLng


data class Place(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {

    fun getGoogleMapLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }
}
