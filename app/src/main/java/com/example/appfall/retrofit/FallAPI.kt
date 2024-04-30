package com.example.appfall.retrofit

import com.example.appfall.data.models.ConnectedSupervisorsResponse
import com.example.appfall.data.models.LoginResponse
import com.example.appfall.data.models.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface FallAPI {
    @GET("users/getContacts")
    fun getContacts(@Header("Authorization") token: String): Call<ConnectedSupervisorsResponse>

    @POST("users/addUser")
    fun addUser(
        @Body request: User,
    ): Call<LoginResponse>
}
