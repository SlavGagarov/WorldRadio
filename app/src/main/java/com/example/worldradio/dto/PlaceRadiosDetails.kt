package com.example.worldradio.dto

import com.google.gson.annotations.SerializedName

data class PlaceRadiosDetails(
    @SerializedName("apiVersion") val apiVersion: Int,
    @SerializedName("version") val version: String,
    @SerializedName("data") val data: ResponseDataDetails
)

data class ResponseDataDetails(
    @SerializedName("type") val type: String,
    @SerializedName("count") val count: Int,
    @SerializedName("map") val map: String,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("url") val url: String,
    @SerializedName("utcOffset") val utcOffset: Int,
    @SerializedName("content") val content: List<ContentItem>
)

data class ContentItem(
    @SerializedName("items") val items: List<Item>,
    @SerializedName("itemsType") val itemsType: String,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String
)

data class Item(
    @SerializedName("title") val title: String,
    @SerializedName("href") val href: String,
    @SerializedName("page") val page: Page
)

data class Page(
    @SerializedName("type") val type: String,
    @SerializedName("url") val url: String,
    @SerializedName("stream") val stream: String,
    @SerializedName("title") val title: String
)