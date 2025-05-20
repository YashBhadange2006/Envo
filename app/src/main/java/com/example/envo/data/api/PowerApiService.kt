package com.example.envo.data.api

import com.example.envo.data.model.PowerResponse
import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit interface for NASA POWER API to fetch environmental data
// Includes Max Air Temperature (2m above ground), Cloud Cover, and Solar Radiation
interface PowerApiService {
    /**
     * Fetches environmental data from NASA POWER API for a given location and date range.
     * @param parameters Comma-separated parameters (default: T2M_MAX,ALLSKY_SFC_SW_DWN,CLOUD_AMT)
     * @param community Community type (default: SB)
     * @param longitude Longitude of the location
     * @param latitude Latitude of the location
     * @param startDate Start date in yyyyMMdd format
     * @param endDate End date in yyyyMMdd format
     * @param timeStandard Time standard (default: UTC)
     * @return PowerResponse containing Max Air Temperature (2m), Cloud Cover, and Solar Radiation
     */
    @GET("temporal/daily/point")
    suspend fun getEnvironmentalData(
        @Query("parameters") parameters: String = "T2M_MAX,ALLSKY_SFC_SW_DWN,CLOUD_AMT", // T2M_MAX = Max Air Temperature (2m above ground)
        @Query("community") community: String = "SB",
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("start") startDate: String,
        @Query("end") endDate: String,
        @Query("time-standard") timeStandard: String = "UTC"
    ): PowerResponse
} 