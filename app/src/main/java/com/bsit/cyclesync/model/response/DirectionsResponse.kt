package com.bsit.cyclesync.model.response

import com.google.gson.annotations.SerializedName


data class DirectionsResponse(
    val routes: List<Route>
)

data class Route(
    @SerializedName("overview_polyline")
    val overviewPolyline: OverviewPolyline
)

data class OverviewPolyline(
    val points: String
)
