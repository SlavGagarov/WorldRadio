package com.example.worldradio

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface RadioApiService {
    companion object {
        const val BASE_URL: String = "http://radio.garden/api/"
    }

    @GET("ara/content/channel/{id}")
    fun getRadio(@Path("id") id: String): Call<RadioDetailsResponse>

    @GET("ara/content/places")
    fun getAllPlaces(): Call<PlaceDetailsResponse>

    @GET("ara/content/page/{id}")
    fun getPlaceRadios(@Path("id") id: String): Call<PlaceRadiosDetails>

}