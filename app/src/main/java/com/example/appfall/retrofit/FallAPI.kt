package com.example.appfall.retrofit

import com.example.appfall.data.models.ConnectedSupervisorsResponse
import com.example.appfall.data.models.LoginResponse
import com.example.appfall.data.models.User
import com.example.appfall.data.models.UserCredential
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface FallAPI {
    @GET("supervisors/getContacts")
    fun getContacts(@Header("Authorization") token: String): Call<ConnectedSupervisorsResponse>


    @POST("supervisors/addSupervisor")
    fun addUser(
        @Body request: User,
    ): Call<LoginResponse>

    @POST("login/loginSupervisor")
    fun loginUser(
        @Body request: UserCredential,
    ): Call<LoginResponse>


}
