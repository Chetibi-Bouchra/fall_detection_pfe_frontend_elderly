package com.example.appfall.data.daoModels

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "falls")
data class Fall(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val place: String,
    val status: String,
    val user: String,
    val datetime: Date
)