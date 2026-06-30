package com.bsit.cyclesync.services.network

import com.bsit.cyclesync.model.response.AutocompleteResponse
import com.bsit.cyclesync.model.response.PlaceDetailsResponse
import com.bsit.cyclesync.ui.utils.TableConstants
import retrofit2.http.GET
import retrofit2.http.Query


interface PlacesApi {
    @GET("place/autocomplete/json")
    suspend fun autocomplete(
        @Query("input") input: String,
        @Query("includeQueryPredictions") includeQuery: Boolean = true,
        @Query("rankBy") rankBy: String = "distance",
        @Query("key") apiKey: String = TableConstants.DIRECTIONS_KEY
    ): AutocompleteResponse

    @GET("place/details/json")
    suspend fun placeDetails(
        @Query("place_id") placeId: String,
        @Query("key") key: String = TableConstants.DIRECTIONS_KEY,
    ): PlaceDetailsResponse
}