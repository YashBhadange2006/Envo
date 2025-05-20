package com.example.envo.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object WmsService {
    private const val WMS_URL = "https://proba-v-mep.esa.int/api/v1/wms"
    private const val IMAGE_SIZE = 512

    suspend fun getNdviImage(latitude: Double, longitude: Double, date: LocalDate): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Calculate bounding box (1 degree around the point)
            val minLon = longitude - 0.5
            val maxLon = longitude + 0.5
            val minLat = latitude - 0.5
            val maxLat = latitude + 0.5
            
            val bbox = "$minLon,$minLat,$maxLon,$maxLat"
            val formattedDate = date.format(DateTimeFormatter.ISO_DATE)
            
            val url = URL(
                "$WMS_URL?" +
                "service=WMS&" +
                "request=GetMap&" +
                "layers=NDVI&" +
                "version=1.3.0&" +
                "bbox=$bbox&" +
                "width=$IMAGE_SIZE&" +
                "height=$IMAGE_SIZE&" +
                "crs=EPSG:4326&" +
                "format=image/png&" +
                "time=$formattedDate"
            )

            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val inputStream = connection.getInputStream()
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
} 