package com.example.worldradio

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface RadioApiService {
    @GET("ara/content/channel/{id}")
    fun getRadio(@Path("id") id: String): Call<RadioResponse>
}