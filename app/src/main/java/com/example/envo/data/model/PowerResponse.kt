package com.example.envo.data.model

import com.google.gson.annotations.SerializedName

data class PowerResponse(
    @SerializedName("messages")
    val messages: List<String>?,
    @SerializedName("properties")
    val properties: Properties,
    @SerializedName("type")
    val type: String,
    @SerializedName("features")
    val features: List<Feature>,
    @SerializedName("header")
    val header: Header
)

data class Header(
    @SerializedName("title")
    val title: String,
    @SerializedName("api")
    val api: ApiInfo
)

data class ApiInfo(
    @SerializedName("version")
    val version: String,
    @SerializedName("name")
    val name: String
)

data class Feature(
    @SerializedName("geometry")
    val geometry: Geometry,
    @SerializedName("type")
    val type: String,
    @SerializedName("properties")
    val properties: FeatureProperties
)

data class Geometry(
    @SerializedName("coordinates")
    val coordinates: List<Double>,
    @SerializedName("type")
    val type: String
)

data class FeatureProperties(
    @SerializedName("parameter")
    val parameter: Parameters
)

data class Properties(
    @SerializedName("parameter")
    val parameter: Parameters
)

data class Parameters(
    @SerializedName("T2M")
    val temperature: Map<String, Double>,
    @SerializedName("ALLSKY_SFC_SW_DWN")
    val solarRadiation: Map<String, Double>,
    @SerializedName("CLOUD_AMT")
    val cloudCover: Map<String, Double>
)

// Data class for historical temperature data
data class TemperatureHistory(
    val date: String,
    val value: Double
) 