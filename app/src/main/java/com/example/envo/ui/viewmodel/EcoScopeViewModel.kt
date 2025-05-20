package com.example.envo.ui.viewmodel

import android.location.Address
import android.location.Geocoder
import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.envo.data.api.NdviService
import com.example.envo.data.api.RetrofitClient
import com.example.envo.data.api.WmsService
import com.example.envo.data.model.TemperatureHistory
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject
import java.net.URLEncoder

data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String
)

data class EnvironmentalData(
    val temperature: Double = 0.0,
    val cloudCover: Double = 0.0,
    val solarRadiation: Double = 0.0,
    val temperatureHistory: List<TemperatureHistory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEstimated: Boolean = false
)

data class FunFactState(
    val summary: String? = null,
    val imageUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class EcoScopeViewModel : ViewModel() {
    private val _environmentalData = mutableStateOf(EnvironmentalData(isLoading = true))
    val environmentalData: State<EnvironmentalData> = _environmentalData
    
    private val _currentLocation = mutableStateOf(Location(40.7128, -74.0060, "New York"))
    val currentLocation: State<Location> = _currentLocation

    private val _funFact = mutableStateOf(FunFactState(isLoading = false))
    val funFact: State<FunFactState> = _funFact

    val locationName: String
        get() = _currentLocation.value.name

    init {
        viewModelScope.launch {
            try {
                fetchEnvironmentalData(_currentLocation.value.latitude, _currentLocation.value.longitude)
            } catch (e: Exception) {
                Log.e("EcoScopeViewModel", "Error during initialization", e)
                _environmentalData.value = _environmentalData.value.copy(
                    isLoading = false,
                    error = "Failed to initialize: ${e.localizedMessage ?: "Unknown error"}",
                    isEstimated = true
                )
            }
        }
    }

    fun searchLocation(query: String, context: Context) {
        if (query.isBlank()) {
            return
        }

        viewModelScope.launch {
            try {
                _environmentalData.value = _environmentalData.value.copy(isLoading = true, error = null)
                _funFact.value = FunFactState(isLoading = true)
                
                val geocodeResult = withContext(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context)
                        geocoder.getFromLocationName(query, 1)?.firstOrNull()
                    } catch (e: IOException) {
                        Log.e("EcoScopeViewModel", "Geocoding error", e)
                        null
                    }
                }
                
                if (geocodeResult == null) {
                    _environmentalData.value = _environmentalData.value.copy(
                        isLoading = false,
                        error = "Location not found. Please try another location."
                    )
                    _funFact.value = FunFactState(error = "No fun fact available for this location.", isLoading = false)
                    return@launch
                }
                
                val newLocation = Location(
                    latitude = geocodeResult.latitude,
                    longitude = geocodeResult.longitude,
                    name = geocodeResult.locality 
                        ?: geocodeResult.adminArea 
                        ?: geocodeResult.countryName
                        ?: query
                )
                
                // Validate coordinates before updating
                if (!isValidCoordinates(newLocation.latitude, newLocation.longitude)) {
                    _environmentalData.value = _environmentalData.value.copy(
                        isLoading = false,
                        error = "Invalid coordinates received for the location."
                    )
                    _funFact.value = FunFactState(error = "No fun fact available for this location.", isLoading = false)
                    return@launch
                }
                
                _currentLocation.value = newLocation
                fetchEnvironmentalData(newLocation.latitude, newLocation.longitude)
                fetchWikipediaSummary(newLocation.name)
            } catch (e: Exception) {
                Log.e("EcoScopeViewModel", "Error searching location", e)
                _environmentalData.value = _environmentalData.value.copy(
                    isLoading = false,
                    error = "Error searching location: ${e.localizedMessage ?: "Unknown error"}"
                )
                _funFact.value = FunFactState(error = "No fun fact available for this location.", isLoading = false)
            }
        }
    }

    private fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    fun fetchEnvironmentalData(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                _environmentalData.value = _environmentalData.value.copy(
                    isLoading = true,
                    error = null,
                    isEstimated = false
                )
                
                // Launch parallel requests for API data and NDVI image
                val apiDeferred = async(Dispatchers.IO) {
                    val today = LocalDate.now()
                    val sevenDaysAgo = today.minusDays(7)
                    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                    val startDate = sevenDaysAgo.format(formatter)
                    val endDate = today.format(formatter)
                    
                    Log.d("EcoScopeViewModel", "Fetching data for lat: $latitude, lon: $longitude")
                    
                    RetrofitClient.instance.getEnvironmentalData(
                        latitude = latitude,
                        longitude = longitude,
                        startDate = startDate,
                        endDate = endDate
                    )
                }
                
                val ndviImageDeferred = async(Dispatchers.IO) {
                    try {
                        NdviService.getNdviImage(
                            latitude = latitude,
                            longitude = longitude,
                            date = LocalDate.now()
                        ).also { bitmap ->
                            if (bitmap == null) {
                                Log.w("EcoScopeViewModel", "Failed to load NDVI image for location: $latitude, $longitude")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("EcoScopeViewModel", "Error loading NDVI image", e)
                        null
                    }
                }

                // Wait for both requests to complete
                val response = apiDeferred.await()
                val ndviImage = ndviImageDeferred.await()
                
                if (response.messages?.isNotEmpty() == true) {
                    Log.w("EcoScopeViewModel", "API Messages: ${response.messages}")
                }

                val feature = response.features.firstOrNull()
                if (feature == null) {
                    throw IOException("No data available for this location")
                }
                
                val parameters = feature.properties.parameter
                val temperatureHistory = parameters.temperature.map { (date, value) ->
                    TemperatureHistory(date, value)
                }.sortedBy { it.date }

                if (temperatureHistory.isEmpty()) {
                    throw IOException("No temperature data available")
                }

                _environmentalData.value = EnvironmentalData(
                    temperature = temperatureHistory.last().value,
                    cloudCover = parameters.cloudCover.values.last(),
                    solarRadiation = parameters.solarRadiation.values.last(),
                    temperatureHistory = temperatureHistory,
                    isLoading = false,
                    isEstimated = false
                )
                
                Log.d("EcoScopeViewModel", "Successfully fetched data for $locationName")
            } catch (e: Exception) {
                Log.e("EcoScopeViewModel", "Error fetching environmental data", e)
                
                val errorMessage = when (e) {
                    is IOException -> "Network error: Please check your internet connection"
                    is retrofit2.HttpException -> when (e.code()) {
                        404 -> "Location data not available"
                        422 -> "Invalid location coordinates"
                        429 -> "Too many requests. Please try again later"
                        else -> "Server error: ${e.code()}"
                    }
                    else -> "Error: ${e.localizedMessage}"
                }
                
                // Only use estimated data if we really can't get the real data
                val estimatedTemp = estimateTemperature(latitude, LocalDate.now().monthValue)
                val estimatedCloud = estimateCloudCover(latitude, LocalDate.now().monthValue)
                val estimatedSolar = estimateSolarRadiation(latitude, LocalDate.now().monthValue)
                val fallbackHistory = generateFallbackTemperatureHistory(estimatedTemp)
                
                _environmentalData.value = EnvironmentalData(
                    temperature = estimatedTemp,
                    cloudCover = estimatedCloud,
                    solarRadiation = estimatedSolar,
                    temperatureHistory = fallbackHistory,
                    isLoading = false,
                    error = errorMessage,
                    isEstimated = true
                )
            }
        }
    }

    private fun estimateTemperature(latitude: Double, month: Int): Double {
        val baseTemp = 20.0
        val latitudeFactor = (90.0 - Math.abs(latitude)) / 90.0
        val seasonalFactor = when (month) {
            12, 1, 2 -> if (latitude > 0) 0.5 else 1.5
            6, 7, 8 -> if (latitude > 0) 1.5 else 0.5
            else -> 1.0
        }
        return (baseTemp * latitudeFactor * seasonalFactor).roundToInt().toDouble()
    }

    private fun estimateCloudCover(latitude: Double, month: Int): Double {
        val baseCloud = 50.0
        val latitudeFactor = (90.0 - Math.abs(latitude)) / 90.0
        val seasonalFactor = when (month) {
            12, 1, 2 -> if (latitude > 0) 1.2 else 0.8
            6, 7, 8 -> if (latitude > 0) 0.8 else 1.2
            else -> 1.0
        }
        return (baseCloud * latitudeFactor * seasonalFactor).coerceIn(0.0, 100.0)
    }

    private fun estimateSolarRadiation(latitude: Double, month: Int): Double {
        val baseSolar = 250.0
        val latitudeFactor = (90.0 - Math.abs(latitude)) / 90.0
        val seasonalFactor = when (month) {
            12, 1, 2 -> if (latitude > 0) 0.6 else 1.4
            6, 7, 8 -> if (latitude > 0) 1.4 else 0.6
            else -> 1.0
        }
        return (baseSolar * latitudeFactor * seasonalFactor).roundToInt().toDouble()
    }

    private fun generateFallbackTemperatureHistory(baseTemp: Double): List<TemperatureHistory> {
        val today = LocalDate.now()
        return (0..6).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val variation = (-2..2).random()
            TemperatureHistory(
                date = date.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                value = baseTemp + variation
            )
        }.sortedBy { it.date }
    }

    fun getNdviDescription(ndvi: Double): String {
        return when {
            ndvi < 0.1 -> "Barren/Very Sparse Vegetation"
            ndvi < 0.2 -> "Sparse Vegetation"
            ndvi < 0.4 -> "Moderate Vegetation"
            ndvi < 0.6 -> "Dense Vegetation"
            else -> "Very Dense Vegetation"
        }
    }

    fun getNdviColor(ndvi: Double): Int {
        return when {
            ndvi < 0.1 -> 0xFFE57373.toInt() // Light Red
            ndvi < 0.2 -> 0xFFFFB74D.toInt() // Orange
            ndvi < 0.4 -> 0xFFFFF176.toInt() // Yellow
            ndvi < 0.6 -> 0xFF81C784.toInt() // Light Green
            else -> 0xFF43A047.toInt() // Dark Green
        }
    }

    fun fetchWikipediaSummary(locationName: String) {
        viewModelScope.launch {
            _funFact.value = FunFactState(isLoading = true)
            try {
                val encoded = URLEncoder.encode(locationName, "UTF-8")
                val url = "https://en.wikipedia.org/api/rest_v1/page/summary/$encoded"
                val result = withContext(Dispatchers.IO) {
                    java.net.URL(url).openStream().bufferedReader().use { it.readText() }
                }
                val json = JSONObject(result)
                val extract = json.optString("extract", null)
                val thumbnail = json.optJSONObject("thumbnail")?.optString("source", null)
                if (extract != null) {
                    _funFact.value = FunFactState(summary = extract, imageUrl = thumbnail, isLoading = false)
                } else {
                    _funFact.value = FunFactState(error = "No fun fact available for this location.", isLoading = false)
                }
            } catch (e: Exception) {
                _funFact.value = FunFactState(error = "No fun fact available for this location.", isLoading = false)
            }
        }
    }
} 