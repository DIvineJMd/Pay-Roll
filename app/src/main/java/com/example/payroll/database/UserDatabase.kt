package com.example.payroll.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [User::class, LocationRequest::class], version = 2, exportSchema = false) // Increment version to 2
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun locationDao(): LocationDao // Add DAO for LocationRequest

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "payroll_database"
                )
                    .fallbackToDestructiveMigration() // Handles schema changes destructively
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
