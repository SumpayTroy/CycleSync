package com.bsit.cyclesync.weather

import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherResponse(
    val weather: List<WeatherDescription>,
    val main: MainWeather,
    val name: String
)

data class WeatherDescription(val main: String, val description: String, val icon: String)
data class MainWeather(val temp: Float, val feels_like: Float, val temp_min: Float, val temp_max: Float)

interface WeatherApi {
    // By lat / lon
    @GET("data/2.5/weather")
    suspend fun getWeatherByCords(

        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"

    ): WeatherResponse
}