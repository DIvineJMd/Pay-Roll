package com.example.payroll.database


import androidx.room.*
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.Flow


@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: User)

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): User?

    @Query("DELETE FROM users")
    suspend fun clearUser()
}
@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationRequest)

    @Query("SELECT COUNT(*) FROM location_requests")
    suspend fun getLocationCount(): Int

    @Query("SELECT * FROM location_requests")
    fun getAllLocationsFlow(): Flow<List<LocationRequest>>

    @Query("DELETE FROM location_requests")
    suspend fun deleteAllLocations()

    @Query("DELETE FROM location_requests WHERE id = :id")
    suspend fun deleteLocationById(id: Int)

    @Query("SELECT * FROM location_requests WHERE uploaded = 0")
    suspend fun getPendingLocations(): List<LocationRequest>
    // New query to update the upload status
    @Query("UPDATE location_requests SET uploaded = :status WHERE id = :id")
    suspend fun updateUploadedStatus(id: Int, status: Boolean)
}

