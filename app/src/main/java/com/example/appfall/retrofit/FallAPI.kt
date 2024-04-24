package com.example.appfall.retrofit

import com.example.appfall.models.ConnectedSupervisorsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface FallAPI {
    @GET("users/getContacts")
    fun getContacts(@Header("Authorization") token: String): Call<ConnectedSupervisorsResponse>
}
