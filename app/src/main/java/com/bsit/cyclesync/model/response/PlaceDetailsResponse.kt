package com.bsit.cyclesync.model.response


data class PlaceDetailsResponse(
    val result: PlaceResult
)

data class PlaceResult(
    val geometry: Geometry
)

data class Geometry(
    val location: LatLngBody
)

data class LatLngBody(
    val lat: Double,
    val lng: Double
)
