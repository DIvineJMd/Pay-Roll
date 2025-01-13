package com.example.payroll.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(
    private val userDao: UserDao,
    private val locationDao: LocationDao
) {
    // User operations
    suspend fun saveUser(user: User) = withContext(Dispatchers.IO) {
        userDao.insertOrUpdateUser(user)
    }

    suspend fun getUser(): User? = withContext(Dispatchers.IO) {
        userDao.getCurrentUser()
    }

    suspend fun clearUser() = withContext(Dispatchers.IO) {
        userDao.clearUser()
    }

    // LocationRequest operations
    suspend fun saveLocation(location: LocationRequest) = withContext(Dispatchers.IO) {
        locationDao.insertLocation(location)
    }

    suspend fun getAllLocations(): List<LocationRequest> = withContext(Dispatchers.IO) {
        locationDao.getAllLocations()
    }

    suspend fun clearLocations() = withContext(Dispatchers.IO) {
        locationDao.deleteAllLocations()
    }

    // Optional: Clear all data
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        userDao.clearUser()
        locationDao.deleteAllLocations()
    }
}
