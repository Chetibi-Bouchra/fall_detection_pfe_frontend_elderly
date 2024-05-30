package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appfall.data.models.Fall
import com.example.appfall.retrofit.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FallsViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableFallsList: MutableLiveData<List<Fall>> = MutableLiveData()

    private var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2NTczMTY0ODViOTFiZjY3ZGZjNjM5ZiIsImlhdCI6MTcxNjk5MDMwOH0.4HxGAUghy9zX-LzXG7ukzY3ugx9Pld_kDGz342E0_Uc"

    fun getFalls(userId: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.fallApi.getFallsByUser("Bearer $token", userId).execute()
                }
                if (response.isSuccessful) {
                    val fallResponse = response.body()
                    fallResponse?.let {
                        mutableFallsList.postValue(it.data)
                        Log.d("FallsViewModel", "Données de chute reçues: $it")
                    } ?: run {
                        Log.d("FallViewModel", "Response body is null")
                    }
                } else {
                    Log.d("FallViewModel", "Response not successful: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.d("FallViewModel", "API call failed: ${e.message}")
            }
        }
    }

    fun observeFallsList(): LiveData<List<Fall>> {
        return mutableFallsList
    }
}
