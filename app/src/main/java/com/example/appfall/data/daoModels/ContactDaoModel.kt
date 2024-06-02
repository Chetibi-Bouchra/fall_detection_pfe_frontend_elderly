package com.example.appfall.data.daoModels

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactDaoModel(
    @PrimaryKey
    val _id: String,
    val name: String,
    val phone: String
)
