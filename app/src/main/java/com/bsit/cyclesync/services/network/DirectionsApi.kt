package com.bsit.cyclesync.services.network

import com.bsit.cyclesync.model.response.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Query


interface DirectionsApi {
    @GET("directions/json")
    suspend fun getRoute(
//        @Query("mode") mode: String = "bicycling",
        @Query("origin") origin: String,           // e.g., "14.5995,120.9842"
        @Query("destination") destination: String, // e.g., "14.5547,121.0244"
        @Query("avoid") avoid: String = "indoor|tolls",
        @Query("key") key: String
    ): DirectionsResponse
}