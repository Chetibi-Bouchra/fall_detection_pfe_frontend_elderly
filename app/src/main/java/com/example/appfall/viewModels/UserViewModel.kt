package com.example.appfall.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appfall.data.models.LoginResponse
import com.example.appfall.data.models.User
import com.example.appfall.data.models.UserCredential
import com.example.appfall.retrofit.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserViewModel : ViewModel() {
    private val _loginResponse: MutableLiveData<LoginResponse> = MutableLiveData()
    val loginResponse: LiveData<LoginResponse> = _loginResponse


    fun addUser(user: User) {
        RetrofitInstance.fallApi.addUser(user).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    _loginResponse.value = response.body()
                } else {
                    // Handle error
                    Log.e("UserViewModel", "Failed to add user: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // Handle failure
                Log.e("UserViewModel", "Failed to add user", t)
            }
        })
    }

    fun loginUser(user: UserCredential) {
        RetrofitInstance.fallApi.loginUser(user).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    _loginResponse.value = response.body()
                } else {
                    // Handle error
                    Log.e("UserViewModel", "Failed to login: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // Handle failure
                Log.e("UserViewModel", "Failed to login", t)
            }
        })
    }
}
