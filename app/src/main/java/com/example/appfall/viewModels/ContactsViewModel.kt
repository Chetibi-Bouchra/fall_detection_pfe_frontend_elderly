package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appfall.data.daoModels.ContactDaoModel
import com.example.appfall.data.models.ConnectedSupervisor
import com.example.appfall.data.models.ConnectedSupervisorsResponse
import com.example.appfall.data.repositories.AppDatabase
import com.example.appfall.data.repositories.dataStorage.ContactDao
import com.example.appfall.data.repositories.dataStorage.UserDao
import com.example.appfall.retrofit.RetrofitInstance
import com.example.appfall.services.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: UserDao = AppDatabase.getInstance(application).userDao()
    private val contactsDao: ContactDao = AppDatabase.getInstance(application).contactDao()
    //private var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2M2Q2NjNmYzExN2RlYTdiYmYyOThlOCIsImlhdCI6MTcxNzM4MDE1OH0._Kiim5YC1OUiBrOL7fhkpsr1_dbXBQy1EJzo1xN3ZsU"
    private val mutableContactsList: MutableLiveData<List<ConnectedSupervisor>?> = MutableLiveData()
    private lateinit var token: String
    private val networkHelper = NetworkHelper(application)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUser()
            user?.let {
                token = it.token
                println("aaaaa ${contactsDao.getAllContacts()}")
                //getContacts()
            }
        }
    }

    private fun getContacts() {
        if (networkHelper.isInternetAvailable()) {
            getContactsNetwork()
        } else {
            getContactsOffline()
        }
    }

    fun getContactsOffline() {
        viewModelScope.launch(Dispatchers.IO) {
            // Retrieve all contacts from the local database
            val contacts = contactsDao.getAllContacts()

            Log.d("ContactsMvvMOfflineContacts", contacts.toString())

            // Map local contacts to ConnectedSupervisor objects
            val connectedSupervisors = contacts.map { contact ->
                ConnectedSupervisor(
                    _id = contact._id,
                    name = contact.name,
                    phone = contact.phone
                )
            }

            Log.d("ContactsMvvMOffline", connectedSupervisors.toString())

            // Post the list to mutableContactsList on the main thread
            withContext(Dispatchers.Main) {
                // Use emptyList() if there are no contacts, otherwise use the mapped list
                mutableContactsList.value = connectedSupervisors
                Log.d("mutableContactsList", mutableContactsList.value.toString())
            }
        }
    }


    fun getContactsNetwork() {
        RetrofitInstance.fallApi.getContacts("Bearer $token").enqueue(object : Callback<ConnectedSupervisorsResponse> {
            override fun onResponse(
                call: Call<ConnectedSupervisorsResponse>,
                response: Response<ConnectedSupervisorsResponse>
            ) {
                viewModelScope.launch(Dispatchers.IO) {
                    if (response.isSuccessful) {
                        val networkContacts = response.body()?.connectedSupervisors ?: emptyList()

                        // Clear the local contacts table
                        contactsDao.deleteContacts()

                        // Convert network contacts to ContactDaoModel objects
                        val newContactsDaoModels = networkContacts.map { networkContact ->
                            ContactDaoModel(networkContact._id, networkContact.name, networkContact.phone)
                        }

                        // Add all contacts from network response
                        newContactsDaoModels.forEach { newContactDaoModel ->
                            contactsDao.addContact(newContactDaoModel)
                        }
                        Log.d("ContactsMvvM", newContactsDaoModels.toString())

                        // Update the mutable contacts list on the main thread
                        withContext(Dispatchers.Main) {
                            mutableContactsList.value = networkContacts
                            Log.d("ContactsMvvMNetwork", networkContacts.toString())
                        }
                    } else {
                        // Handle the case when the response is not successful
                        withContext(Dispatchers.Main) {
                            mutableContactsList.value = emptyList()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ConnectedSupervisorsResponse>, t: Throwable) {
                Log.d("ContactsMvvM", t.message.toString())
                viewModelScope.launch(Dispatchers.Main) {
                    mutableContactsList.value = null
                }
            }
        })
    }

    fun observeContactsList(): MutableLiveData<List<ConnectedSupervisor>?> {
        return mutableContactsList
    }
}
