package com.example.worldradio.dto

import com.google.gson.annotations.SerializedName

data class RadioDetailsResponse(
    @SerializedName("apiVersion") val apiVersion: Int,
    @SerializedName("version") val version: String,
    @SerializedName("data") val data: RadioData
)

data class RadioData(
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("stream") val stream: String,
    @SerializedName("website") val website: String,
    @SerializedName("secure") val secure: Boolean,
    @SerializedName("place") val place: Place,
    @SerializedName("country") val country: Country
)

data class Place(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String
)

data class Country(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String
)