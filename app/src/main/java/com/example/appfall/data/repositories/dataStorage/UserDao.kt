package com.example.appfall.data.repositories.dataStorage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.appfall.data.daoModels.UserDaoModel

@Dao
interface UserDao {
    @Query("select * from users LIMIT 1")
    fun getUser(): UserDaoModel?

    // Insert a new user
    @Insert
    fun addUser(user: UserDaoModel)

    // Delete a user
    @Delete
    fun deleteUser(user: UserDaoModel)

    //update a user
    @Update
    fun updateUser(user: UserDaoModel)
}