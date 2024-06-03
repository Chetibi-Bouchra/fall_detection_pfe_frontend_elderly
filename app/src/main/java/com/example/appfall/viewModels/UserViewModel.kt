package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appfall.data.daoModels.UserDaoModel
import com.example.appfall.data.models.LoginResponse
import com.example.appfall.data.models.UpdateResponse
import com.example.appfall.data.models.User
import com.example.appfall.data.models.UserCredential
import com.example.appfall.data.models.UserName
import com.example.appfall.data.models.UserPassword
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.ContactDao
import com.example.appfall.data.repositories.dataStorage.FallDao
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.retrofit.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: UserDao = AppDatabase.getInstance(application).userDao()
    private val contactsDao: ContactDao = AppDatabase.getInstance(application).contactDao()
    private val fallsDao: FallDao = AppDatabase.getInstance(application).fallDao()

    private val _loginResponse: MutableLiveData<LoginResponse> = MutableLiveData()
    val loginResponse: LiveData<LoginResponse> = _loginResponse

    private val _userAddStatus: MutableLiveData<Boolean> = MutableLiveData()
    val userAddStatus: LiveData<Boolean> = _userAddStatus

    private val _addErrorStatus: MutableLiveData<String> = MutableLiveData()
    val addErrorStatus: LiveData<String> = _addErrorStatus

    private val _localUser: MutableLiveData<UserDaoModel?> = MutableLiveData()
    val localUser: MutableLiveData<UserDaoModel?> = _localUser

    private val _updateNameResponse: MutableLiveData<UpdateResponse> = MutableLiveData()
    val updateNameResponse: LiveData<UpdateResponse> = _updateNameResponse

    private val _updatePasswordResponse: MutableLiveData<UpdateResponse> = MutableLiveData()
    val updatePasswordResponse: LiveData<UpdateResponse> = _updatePasswordResponse

    private lateinit var token: String

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUser()
            user?.let {
                token = it.token
            }
        }
    }

    fun addUser(user: User) {
        RetrofitInstance.fallApi.addUser(user).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    _loginResponse.value = response.body()
                    viewModelScope.launch(Dispatchers.IO) {
                        val userDaoModel = UserDaoModel(
                            phone = user.phone,
                            token = _loginResponse.value?.data?.accessToken ?: ""
                        )
                        try {
                            userDao.addUser(userDaoModel)
                            _userAddStatus.postValue(true)
                        } catch (e: Exception) {
                            _userAddStatus.postValue(false)
                            handleErrorResponse(response.errorBody())
                            Log.e("UserViewModel", "Failed to add user to local database", e)
                        }
                    }
                } else {
                    handleErrorResponse(response.errorBody())
                    Log.e("UserViewModel", "Failed to add user to remote server: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                val errorMessage = t.message ?: "Une erreur s'est produite lors de l'authentification"
                _addErrorStatus.postValue(errorMessage)
                Log.e("UserViewModel", "Failed to add user to remote server: $errorMessage", t)
            }
        })
    }

    fun loginUser(user: UserCredential) {
        RetrofitInstance.fallApi.loginUser(user).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    _loginResponse.value = response.body()
                    viewModelScope.launch(Dispatchers.IO) {
                        val userDaoModel = UserDaoModel(
                            phone = user.credential,
                            token = _loginResponse.value?.data?.accessToken ?: ""
                        )
                        try {
                            userDao.addUser(userDaoModel)
                            _userAddStatus.postValue(true)
                        } catch (e: Exception) {
                            _userAddStatus.postValue(false)
                            handleErrorResponse(response.errorBody())
                            Log.e("UserViewModel", "Failed to add user to local database", e)
                        }
                    }
                } else {
                    handleErrorResponse(response.errorBody())
                    Log.e("UserViewModel", "Failed to login: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                val errorMessage = t.message ?: "Une erreur s'est produite lors de l'authentification"
                _addErrorStatus.postValue(errorMessage)
                Log.e("UserViewModel", "Failed to login", t)
            }
        })
    }

    fun updateName(newName: String) {
        val userName = UserName(newName)
        RetrofitInstance.fallApi.updateUserName("Bearer $token", userName).enqueue(object : Callback<UpdateResponse> {
            override fun onResponse(call: Call<UpdateResponse>, response: Response<UpdateResponse>) {
                if (response.isSuccessful) {
                    _updateNameResponse.value = response.body()
                } else {
                    handleErrorResponse(response.errorBody())
                    Log.e("UserViewModel", "Failed to update name: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UpdateResponse>, t: Throwable) {
                val errorMessage = t.message ?: "An error occurred while updating name"
                _addErrorStatus.postValue(errorMessage)
                Log.e("UserViewModel", errorMessage, t)
            }
        })
    }

    fun updatePassword(newPassword: String) {
        val userPassword = UserPassword(newPassword)
        RetrofitInstance.fallApi.updateUserPassword("Bearer $token", userPassword).enqueue(object : Callback<UpdateResponse> {
            override fun onResponse(call: Call<UpdateResponse>, response: Response<UpdateResponse>) {
                if (response.isSuccessful) {
                    _updatePasswordResponse.value = response.body()
                } else {
                    handleErrorResponse(response.errorBody())
                    Log.e("UserViewModel", "Failed to update password: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UpdateResponse>, t: Throwable) {
                val errorMessage = t.message ?: "An error occurred while updating password"
                _addErrorStatus.postValue(errorMessage)
                Log.e("UserViewModel", errorMessage, t)
            }
        })
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            userDao.deleteUser()
            contactsDao.deleteContacts()
            fallsDao.deleteFalls()
        }
    }

    fun getLocalUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val localUser = userDao.getUser()
            _localUser.postValue(localUser)
        }
    }

    private fun handleErrorResponse(errorBody: ResponseBody?) {
        val errorMessage = errorBody?.string()?.let { errorContent ->
            try {
                val jsonObject = JSONObject(errorContent)
                val nestedMessage = jsonObject.getJSONObject("message").getString("message")
                nestedMessage
            } catch (e: Exception) {
                "Une erreur s'est produite lors de l'authentification"
            }
        } ?: "Une erreur s'est produite lors de l'authentification"
        _addErrorStatus.postValue(errorMessage)
    }
}
