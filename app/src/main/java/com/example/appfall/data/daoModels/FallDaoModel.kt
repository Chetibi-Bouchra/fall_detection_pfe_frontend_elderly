package com.example.appfall.data.daoModels

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "falls")
data class FallDaoModel(
    @PrimaryKey
    @ColumnInfo(name = "_id")
    val id: String,
    val longitude: Double,
    val latitude: Double,
    val status: String,
    val datetime: Long
)