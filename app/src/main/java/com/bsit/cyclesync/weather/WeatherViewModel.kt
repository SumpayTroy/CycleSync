package com.bsit.cyclesync.weather

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsit.cyclesync.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor() : ViewModel() {

    private val api: WeatherApi = RetrofitInstance.api
    private val _weather = MutableStateFlow<WeatherResponse?>(null)
    val weather: StateFlow<WeatherResponse?> = _weather.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchWeatherByCords(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.OPENWEATHER_API_KEY

                // Debug log (remove later for production)
                Log.d("WeatherViewModel", "Using API Key: $apiKey")

                if (apiKey.isBlank()) {
                    _error.value = "Missing API key"
                    _weather.value = null
                    return@launch
                }

                val resp = api.getWeatherByCords(lat, lon, apiKey)

                // If successful, update weather and clear error
                _weather.value = resp
                _error.value = null
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Weather fetch failed", e)
                _error.value = e.message ?: "Failed to fetch weather"
                _weather.value = null
            }
        }
    }
}
