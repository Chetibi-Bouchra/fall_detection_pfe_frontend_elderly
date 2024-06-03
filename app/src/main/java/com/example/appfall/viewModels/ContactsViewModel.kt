package com.example.appfall.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: UserDao = AppDatabase.getInstance(application).userDao()
    private val contactsDao: ContactDao = AppDatabase.getInstance(application).contactDao()
    private var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY2M2Q2NjNmYzExN2RlYTdiYmYyOThlOCIsImlhdCI6MTcxNzM4MDE1OH0._Kiim5YC1OUiBrOL7fhkpsr1_dbXBQy1EJzo1xN3ZsU"
    private val mutableContactsList: MutableLiveData<List<ConnectedSupervisor>> = MutableLiveData()
    //private lateinit var token: String
    private val networkHelper = NetworkHelper(application)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUser()
            user?.let {
                //token = it.token
                println("aaaaa ${contactsDao.getAllContacts()}")
                if (networkHelper.isInternetAvailable()) {
                    getContactsNetwork()
                } else {
                    getContactsOffline()
                }
            }
        }
    }

    fun getContacts() {
        if (networkHelper.isInternetAvailable()) {
            getContactsNetwork()
        } else {
            getContactsOffline()
        }
    }

    private fun getContactsOffline() {
        viewModelScope.launch(Dispatchers.IO) {
            val contacts = contactsDao.getAllContacts()
            val connectedSupervisors = contacts.map { contact ->
                ConnectedSupervisor(
                    _id = contact._id,
                    name = contact.name,
                    phone = contact.phone
                )
            }
            mutableContactsList.postValue(connectedSupervisors)
        }
    }

    private fun getContactsNetwork() {
        RetrofitInstance.fallApi.getContacts("Bearer $token").enqueue(object : Callback<ConnectedSupervisorsResponse> {
            override fun onResponse(call: Call<ConnectedSupervisorsResponse>, response: Response<ConnectedSupervisorsResponse>) {
                val networkContacts = response.body()?.connectedSupervisors ?: emptyList()
                viewModelScope.launch(Dispatchers.IO) {
                    val localContacts = contactsDao.getAllContacts()

                    // Identify new contacts to be added
                    val newContacts = networkContacts.filterNot { networkContact ->
                        localContacts.any { localContact ->
                            localContact._id == networkContact._id
                        }
                    }

                    // Identify contacts to be deleted
                    val deletedContacts = localContacts.filterNot { localContact ->
                        networkContacts.any { networkContact ->
                            localContact._id == networkContact._id
                        }
                    }

                    // Convert newContacts to ContactDaoModel objects
                    val newContactsDaoModels = newContacts.map { newContact ->
                        ContactDaoModel(newContact._id, newContact.name, newContact.phone)
                    }

                    // Add new contacts
                    newContactsDaoModels.forEach { newContactDaoModel ->
                        contactsDao.addContact(newContactDaoModel)
                    }

                    // Delete contacts not present in the network response
                    deletedContacts.forEach { deletedContact ->
                        contactsDao.deleteContact(deletedContact)
                    }

                    // Update the mutable contacts list
                    mutableContactsList.postValue(networkContacts)
                }
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
