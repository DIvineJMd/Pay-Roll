package com.example.payroll.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    @ColumnInfo(name = "emp_id") val empId: Int, // Matches "empId" from the JSON
    @ColumnInfo(name = "expiry_time") val expiryTime: Long?, // Use Long for timestamp
    @ColumnInfo(name = "loc_tracking") val locTracking: String?, // Matches "locTracking"
    @ColumnInfo(name = "username") val username: String?, // Add "username"
    @ColumnInfo(name = "token") val token: String? // Add "token"
)

@Entity(tableName = "location_requests")
data class LocationRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,  // Auto-generated primary key
    @SerializedName("accId") val accId: String,
    @SerializedName("timing") val timing: String,
    @SerializedName("lat") val lat: String,
    @SerializedName("lang") val lang: String,
    val uploaded :  Boolean
)