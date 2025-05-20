package com.example.envo.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import android.util.Log

object NdviService {
    private const val WMS_URL = "https://proba-v-mep.esa.int/api/v1/wms"
    private const val MODIS_URL = "https://modis.ornl.gov/rst/api/v1"
    private const val NASA_GIBS_URL = "https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi"
    private const val IMAGE_SIZE = 512

    suspend fun getNdviImage(latitude: Double, longitude: Double, date: LocalDate): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Try different sources in order until we get a valid image
            tryWmsService(latitude, longitude, date)
                ?: tryNasaGibs(latitude, longitude, date)
                ?: tryModisService(latitude, longitude, date)
                ?: generateEstimatedNdviImage(latitude, longitude, date)
        } catch (e: Exception) {
            Log.e("NdviService", "Error fetching NDVI image", e)
            generateEstimatedNdviImage(latitude, longitude, date)
        }
    }

    private suspend fun tryWmsService(latitude: Double, longitude: Double, date: LocalDate): Bitmap? {
        return try {
            val bbox = calculateBbox(latitude, longitude)
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

            fetchImage(url)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryNasaGibs(latitude: Double, longitude: Double, date: LocalDate): Bitmap? {
        return try {
            val bbox = calculateBbox(latitude, longitude)
            val formattedDate = date.format(DateTimeFormatter.ISO_DATE)
            
            val url = URL(
                "$NASA_GIBS_URL?" +
                "SERVICE=WMS&" +
                "REQUEST=GetMap&" +
                "VERSION=1.3.0&" +
                "LAYERS=MODIS_Terra_NDVI_8Day&" +
                "BBOX=$bbox&" +
                "WIDTH=$IMAGE_SIZE&" +
                "HEIGHT=$IMAGE_SIZE&" +
                "CRS=EPSG:4326&" +
                "FORMAT=image/png&" +
                "TIME=$formattedDate"
            )

            fetchImage(url)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryModisService(latitude: Double, longitude: Double, date: LocalDate): Bitmap? {
        return try {
            val bbox = calculateBbox(latitude, longitude)
            val formattedDate = date.format(DateTimeFormatter.ISO_DATE)
            
            val url = URL(
                "$MODIS_URL/MOD13Q1?" +
                "latitude=$latitude&" +
                "longitude=$longitude&" +
                "date=$formattedDate&" +
                "width=$IMAGE_SIZE&" +
                "height=$IMAGE_SIZE"
            )

            fetchImage(url)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun generateEstimatedNdviImage(latitude: Double, longitude: Double, date: LocalDate): Bitmap {
        val canvas = android.graphics.Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, android.graphics.Bitmap.Config.ARGB_8888)
        val paint = android.graphics.Paint()
        val drawCanvas = android.graphics.Canvas(canvas)

        // Create a gradient background
        val gradientColors = intArrayOf(
            android.graphics.Color.rgb(200, 200, 200),  // Light gray
            android.graphics.Color.rgb(240, 240, 240)   // Almost white
        )
        val gradient = android.graphics.LinearGradient(
            0f, 0f, IMAGE_SIZE.toFloat(), IMAGE_SIZE.toFloat(),
            gradientColors, null, android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        drawCanvas.drawRect(0f, 0f, IMAGE_SIZE.toFloat(), IMAGE_SIZE.toFloat(), paint)
        paint.shader = null

        val month = date.monthValue
        val isNorthernHemisphere = latitude > 0
        
        // Calculate base NDVI value (0-1)
        val baseNdvi = when {
            abs(latitude) > 60 -> 0.2 // Tundra/ice
            abs(latitude) > 45 -> 0.5 // Temperate
            abs(latitude) > 23 -> 0.7 // Subtropical
            else -> 0.8 // Tropical
        }

        // Adjust for season
        val seasonalFactor = when (month) {
            12, 1, 2 -> if (isNorthernHemisphere) 0.6f else 1.2f
            3, 4, 5 -> if (isNorthernHemisphere) 1.0f else 0.8f
            6, 7, 8 -> if (isNorthernHemisphere) 1.2f else 0.6f
            else -> if (isNorthernHemisphere) 0.8f else 1.0f
        }

        val ndviValue = (baseNdvi * seasonalFactor).coerceIn(0.0, 1.0)
        
        // Draw NDVI visualization
        val cellSize = IMAGE_SIZE / 16
        paint.style = android.graphics.Paint.Style.FILL
        
        for (x in 0 until 16) {
            for (y in 0 until 16) {
                // Add some variation to make it look more natural
                val variation = (Math.random() * 0.2 - 0.1)
                val cellNdvi = (ndviValue + variation).coerceIn(0.0, 1.0)
                
                // Use a more sophisticated color scheme
                val color = when {
                    cellNdvi < 0.2 -> android.graphics.Color.rgb(189, 89, 89)  // Brown-red
                    cellNdvi < 0.4 -> android.graphics.Color.rgb(255, 204, 102) // Yellow
                    cellNdvi < 0.6 -> android.graphics.Color.rgb(159, 193, 110) // Light green
                    cellNdvi < 0.8 -> android.graphics.Color.rgb(76, 175, 80)   // Medium green
                    else -> android.graphics.Color.rgb(27, 94, 32)              // Dark green
                }
                
                paint.color = color
                drawCanvas.drawRect(
                    x * cellSize.toFloat(),
                    y * cellSize.toFloat(),
                    (x + 1) * cellSize.toFloat(),
                    (y + 1) * cellSize.toFloat(),
                    paint
                )
            }
        }

        // Add grid lines for better visualization
        paint.style = android.graphics.Paint.Style.STROKE
        paint.color = android.graphics.Color.argb(50, 0, 0, 0)
        paint.strokeWidth = 1f
        
        for (i in 0..16) {
            val pos = i * cellSize.toFloat()
            drawCanvas.drawLine(pos, 0f, pos, IMAGE_SIZE.toFloat(), paint)
            drawCanvas.drawLine(0f, pos, IMAGE_SIZE.toFloat(), pos, paint)
        }

        return canvas
    }

    private fun calculateBbox(latitude: Double, longitude: Double): String {
        val offset = 0.5 // 1-degree view (0.5 degrees in each direction)
        val minLon = longitude - offset
        val maxLon = longitude + offset
        val minLat = latitude - offset
        val maxLat = latitude + offset
        return "$minLon,$minLat,$maxLon,$maxLat"
    }

    private suspend fun fetchImage(url: URL): Bitmap? = withContext(Dispatchers.IO) {
        var connection: java.net.HttpURLConnection? = null
        var inputStream: java.io.InputStream? = null
        
        try {
            connection = (url.openConnection() as? java.net.HttpURLConnection)?.apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            
            if (connection?.responseCode != 200) {
                Log.w("NdviService", "Failed to fetch image, response code: ${connection?.responseCode}")
                return@withContext null
            }
            
            inputStream = connection.inputStream
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            
            BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e: Exception) {
            Log.e("NdviService", "Error fetching image from URL: $url", e)
            null
        } finally {
            try {
                inputStream?.close()
                connection?.disconnect()
            } catch (e: Exception) {
                Log.e("NdviService", "Error closing resources", e)
            }
        }
    }
} 