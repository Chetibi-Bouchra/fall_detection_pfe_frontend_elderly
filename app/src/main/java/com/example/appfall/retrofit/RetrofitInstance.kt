package com.example.appfall.retrofit

import com.example.appfall.utils.url
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    val fallApi:FallAPI by lazy {
        Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FallAPI::class.java)
    }
}