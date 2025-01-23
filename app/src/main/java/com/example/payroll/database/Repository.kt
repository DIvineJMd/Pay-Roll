package com.example.payroll.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserRepository(
    private val userDao: UserDao,
    private val locationDao: LocationDao,
    private val attendanceDao:AttendanceDao
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

    fun getAllLocationsFlow(): Flow<List<LocationRequest>> = locationDao.getAllLocationsFlow()


    suspend fun clearLocations() = withContext(Dispatchers.IO) {
        locationDao.deleteAllLocations()
    }

    // Optional: Clear all data
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        userDao.clearUser()
        locationDao.deleteAllLocations()
    }


    suspend fun saveAttendance(attendanceRequest: AttendanceRequest) = withContext(Dispatchers.IO) {
        attendanceDao.insertOrUpdateAttendance(attendanceRequest)
    }

    suspend fun getAttendance(): AttendanceRequest? = withContext(Dispatchers.IO) {
        attendanceDao.getAttendanceData()
    }

    suspend fun clearAttendance() = withContext(Dispatchers.IO) {
        attendanceDao.ClearAttendanceRequest()
    }
    suspend fun updateOuttime(id:Int,Outtime:String)= withContext(Dispatchers.IO) {
        attendanceDao.updateOutTime(id, outTime = Outtime)
    }

}
