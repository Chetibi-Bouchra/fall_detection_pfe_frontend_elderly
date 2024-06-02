package com.example.appfall.data.repositories.dataStorage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.appfall.data.daoModels.FallDaoModel

@Dao
interface FallDao {
    @Insert
    suspend fun insert(fall: FallDaoModel)

    @Query("SELECT * FROM falls")
    suspend fun getAllFalls(): List<FallDaoModel>

    @Query("DELETE FROM falls")
    fun deleteFalls()
}