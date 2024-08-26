package com.example.appfall.retrofit

import com.example.appfall.data.models.AddFallResponse
import com.example.appfall.data.models.ConnectedSupervisorsResponse
import com.example.appfall.data.models.Fall
import com.example.appfall.data.models.FallFilter
import com.example.appfall.data.models.FallStatus
import com.example.appfall.data.models.FallWithoutID
import com.example.appfall.data.models.FallsResponse
import com.example.appfall.data.models.LoginResponse
import com.example.appfall.data.models.UpdateResponse
import com.example.appfall.data.models.User
import com.example.appfall.data.models.UserCredential
import com.example.appfall.data.models.UserName
import com.example.appfall.data.models.UserPassword
import com.example.appfall.data.models.UserPhone
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface FallAPI {
    @GET("users/getContacts")
    fun getContacts(@Header("Authorization") token: String): Call<ConnectedSupervisorsResponse>

    @POST("users/addUser")
    fun addUser(
        @Body request: User,
    ): Call<LoginResponse>

    @POST("login/loginUser")
    fun loginUser(
        @Body request: UserCredential,
    ): Call<LoginResponse>

    @POST("falls/getFalls")
    fun getFalls(@Header("Authorization") token: String, @Body request: FallFilter): Call<FallsResponse>

    @PUT("users/updateUser")
    fun updateUserPassword(
        @Header("Authorization") token: String,
        @Body request: UserPassword
    ): Call<UpdateResponse>

    @PUT("users/updateUser")
    fun updateUserName(
        @Header("Authorization") token: String,
        @Body request: UserName
    ): Call<UpdateResponse>

    @PUT("users/updateUser")
    fun updateUserPhone(
        @Header("Authorization") token: String,
        @Body request: UserPhone
    ): Call<UpdateResponse>

    @PUT("falls/updateFall/{fallId}")
    fun updateFall(
        @Header("Authorization") token: String,
        @Path("fallId") fallId: String,
        @Body request: FallStatus
    ): Call<UpdateResponse>

    @POST("falls/addFall")
    fun addFall(
        @Header("Authorization") token: String,
        @Body request: FallWithoutID,
    ): Call<AddFallResponse>
}
