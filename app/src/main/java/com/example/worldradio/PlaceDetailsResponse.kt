package com.example.worldradio

import com.google.gson.annotations.SerializedName

data class PlaceDetailsResponse(
    @SerializedName("apiVersion") val apiVersion: Int,
    @SerializedName("version") val version: String,
    @SerializedName("data") val data: PlaceData
)

data class PlaceData(
    @SerializedName("list") val list: List<PlaceDetails>
)

data class PlaceDetails(
    @SerializedName("size") val size: Int,
    @SerializedName("id") val id: String,
    @SerializedName("geo") val geo: List<Double>,
    @SerializedName("url") val url: String,
    @SerializedName("boost") val boost: Boolean,
    @SerializedName("title") val title: String,
    @SerializedName("country") val country: String
)