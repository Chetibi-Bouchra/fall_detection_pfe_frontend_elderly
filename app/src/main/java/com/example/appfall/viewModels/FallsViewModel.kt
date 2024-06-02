package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.appfall.data.models.Fall
import com.example.appfall.data.models.FallsResponse
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.data.daoModels.FallDaoModel
import com.example.appfall.data.models.FallStatus
import com.example.appfall.data.models.Place
import com.example.appfall.data.models.UpdateResponse
import com.example.appfall.retrofit.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FallsViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: UserDao = AppDatabase.getInstance(application).userDao()
    private val fallDao = AppDatabase.getInstance(application).fallDao()

    private val mutableFallsList: MutableLiveData<List<Fall>> = MutableLiveData()
    private val activeFallsList: MutableLiveData<List<Fall>> = MutableLiveData()
    private val falseFallsList: MutableLiveData<List<Fall>> = MutableLiveData()
    private val rescuedFallsList: MutableLiveData<List<Fall>> = MutableLiveData()
    private val offlineFallsList: MutableLiveData<List<FallDaoModel>> = MutableLiveData()
    private val combinedFallsList: MutableLiveData<List<Fall>> = MutableLiveData()

    private var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2NTg4MWZlMDcwOGZhMGRjMzQ2NjY4NCIsImlhdCI6MTcxNzE5NDAzNn0.vG-IyMiwCs7Jsj3RSdjVD0pO4gte9M9rBGuvRc1DZxo"

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUser()
            user?.let {
                getFalls()
                getOfflineFalls()
            }
        }
    }

    private fun getFalls() {
        RetrofitInstance.fallApi.getFalls("Bearer $token").enqueue(object : Callback<FallsResponse> {
            override fun onResponse(call: Call<FallsResponse>, response: Response<FallsResponse>) {
                if (response.isSuccessful) {
                    val falls = response.body()?.data ?: emptyList()
                    mutableFallsList.value = falls
                    updateCombinedFalls()
                } else {
                    Log.d("FallsViewModel", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<FallsResponse>, t: Throwable) {
                Log.d("FallsViewModel", t.message.toString())
            }
        })
    }

    private fun getOfflineFalls() {
        viewModelScope.launch {
            val falls = withContext(Dispatchers.IO) { fallDao.getAllFalls() }
            offlineFallsList.value = falls
            updateCombinedFalls()
        }
    }

    private fun updateCombinedFalls() {
        val combinedList = mutableListOf<Fall>()
        val networkFalls = mutableFallsList.value ?: emptyList()
        val offlineFalls = offlineFallsList.value?.map { it.toFall() } ?: emptyList()

        combinedList.addAll(networkFalls)
        combinedList.addAll(offlineFalls)

        combinedFallsList.value = combinedList
        updateFilteredLists(combinedList)
    }

    private fun updateFilteredLists(falls: List<Fall>) {
        activeFallsList.value = falls.filter { it.status == "active" }
        falseFallsList.value = falls.filter { it.status == "false" }
        rescuedFallsList.value = falls.filter { it.status == "rescued" }
    }

    fun getActiveFalls(): LiveData<List<Fall>> {
        return activeFallsList
    }

    fun getFalseFalls(): LiveData<List<Fall>> {
        return falseFallsList
    }

    fun getRescuedFalls(): LiveData<List<Fall>> {
        return rescuedFallsList
    }

    fun observeFallsList(): LiveData<List<Fall>> {
        return combinedFallsList
    }

    fun observeOfflineFalls(): LiveData<List<Fall>> {
        return offlineFallsList.map { offlineFalls ->
            offlineFalls.map { it.toFall() }
        }
    }

    fun updateFallStatus(fallId: String, newStatus: String) {
        val request = FallStatus(status = newStatus)

        RetrofitInstance.fallApi.updateFall("Bearer $token", fallId, request).enqueue(object : Callback<UpdateResponse> {
            override fun onResponse(call: Call<UpdateResponse>, response: Response<UpdateResponse>) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val updatedFall = response.body()
                    Log.d("FallsViewModel", "Fall updated successfully: $updatedFall")

                    // Clear relevant variables
                    mutableFallsList.value = emptyList()
                    activeFallsList.value = emptyList()
                    falseFallsList.value = emptyList()
                    rescuedFallsList.value = emptyList()
                    offlineFallsList.value = emptyList()
                    combinedFallsList.value = emptyList()

                    // Refresh the falls list
                    getFalls()
                    getOfflineFalls()
                } else {
                    // Handle unsuccessful response
                    Log.d("FallsViewModel", "Update failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<UpdateResponse>, t: Throwable) {
                // Handle request failure
                Log.d("FallsViewModel", "Update error: ${t.message}")
            }
        })
    }
}

// Extension function to convert FallDaoModel to Fall
private fun FallDaoModel.toFall(): Fall {
    return Fall(
        _id = this.id.toString(),
        createdAt = this.datetime.toString(),
        place = Place(
            latitude = this.latitude,
            longitude = this.longitude
        ),
        status = this.status,
        updatedAt = this.datetime.toString()
    )
}

