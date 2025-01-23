package com.example.payroll.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [User::class, LocationRequest::class,AttendanceRequest::class], version = 4, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun locationDao(): LocationDao
    abstract fun attendanceDao():AttendanceDao
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
