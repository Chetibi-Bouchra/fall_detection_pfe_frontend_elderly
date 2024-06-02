package com.example.appfall.data.repositories

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.appfall.data.daoModels.UserDaoModel
import com.example.appfall.data.repositories.dataStorage.ContactDao
import com.example.appfall.data.daoModels.ContactDaoModel
import com.example.appfall.data.daoModels.DateConverter
import com.example.appfall.data.daoModels.FallDaoModel
import com.example.appfall.data.repositories.dataStorage.FallDao
import com.example.appfall.data.repositories.dataStorage.UserDao
import java.util.Date

@Database(entities = [UserDaoModel::class, ContactDaoModel::class, FallDaoModel::class], version = 3)
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
                ).addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS falls (" +
                            "_id TEXT PRIMARY KEY NOT NULL, " +
                            "longitude REAL NOT NULL, " +
                            "latitude REAL NOT NULL, " +
                            "status TEXT NOT NULL, " +
                            "datetime INTEGER NOT NULL" +
                            ")"
                )
            }
        }
    }
}