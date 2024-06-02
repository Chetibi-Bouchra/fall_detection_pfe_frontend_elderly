package com.example.appfall.data.repositories.dataStorage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appfall.data.daoModels.ContactDaoModel

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts")
    fun getAllContacts(): List<ContactDaoModel>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addContact(contact: ContactDaoModel)

    @Delete
    fun deleteContact(contact: ContactDaoModel)

    @Query("DELETE FROM contacts")
    fun deleteContacts()
}