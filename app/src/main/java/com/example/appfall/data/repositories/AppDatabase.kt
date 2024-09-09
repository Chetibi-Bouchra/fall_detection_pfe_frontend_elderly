package com.example.appfall.data.repositories

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.appfall.data.daoModels.UserDaoModel
import com.example.appfall.data.repositories.dataStorage.ContactDao
import com.example.appfall.data.daoModels.ContactDaoModel
import com.example.appfall.data.daoModels.DateConverter
import com.example.appfall.data.daoModels.FallDaoModel
import com.example.appfall.data.repositories.dataStorage.FallDao
import com.example.appfall.data.repositories.dataStorage.UserDao

@Database(entities = [UserDaoModel::class, ContactDaoModel::class, FallDaoModel::class], version = 1)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun fallDao(): FallDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "Fall_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}