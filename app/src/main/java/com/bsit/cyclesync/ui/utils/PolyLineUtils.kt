package com.bsit.cyclesync.ui.utils

import com.google.android.gms.maps.model.LatLng


object PolyLineUtils {

    private var directionPolyLineContainer: List<LatLng> = emptyList()

    fun getDirectionPolyLines(): List<LatLng> {
        return directionPolyLineContainer
    }

    fun setDirectionPolyLines(polylines: List<LatLng>) {
        this.directionPolyLineContainer = polylines
    }


}