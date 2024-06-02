package com.example.appfall.data.daoModels

import androidx.room.TypeConverter
import java.util.Date

class DateConverter{
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(millisSinceEpoch: Long?): Date? {
        return millisSinceEpoch?.let { Date(it) }
    }
}