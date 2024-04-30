package com.example.appfall.data.daoModels

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val name: String,
    val phone: String,
    val token: String
)