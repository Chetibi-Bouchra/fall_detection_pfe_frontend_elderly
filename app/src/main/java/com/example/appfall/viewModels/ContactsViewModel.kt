package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appfall.data.daoModels.UserDaoModel
import com.example.appfall.data.models.ConnectedSupervisor
import com.example.appfall.data.models.ConnectedSupervisorsResponse
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.retrofit.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: UserDao = AppDatabase.getInstance(application).userDao()
    private val mutableContactsList: MutableLiveData<List<ConnectedSupervisor>>  = MutableLiveData<List<ConnectedSupervisor>>()
    //private lateinit var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2MjA0M2NhNTBhMmRiMGNkZDZlY2JhNSIsImlhdCI6MTcxMzM5MjgxNn0.UhL9U8R78Y4iZgoU8vPUPZva_n-4w45x9Z4Rar3PWYM"
    private lateinit var token: String
    init {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUser()
            user?.let {
                token = it.token
                getContacts()
            }
        }
    }


    private fun getContacts() {
        RetrofitInstance.fallApi.getContacts("Bearer $token").enqueue(object : Callback<ConnectedSupervisorsResponse> {
            override fun onResponse(call: Call<ConnectedSupervisorsResponse>, response: Response<ConnectedSupervisorsResponse>) {
                mutableContactsList.value = response.body()!!.connectedSupervisors
            }

            override fun onFailure(call: Call<ConnectedSupervisorsResponse>, t: Throwable) {
                Log.d("ContactsMvvM", t.message.toString())
            }
        })
    }


    fun observeContactsList(): LiveData<List<ConnectedSupervisor>> {
        return mutableContactsList
    }
}
