package com.example.payroll.data
import com.google.gson.annotations.SerializedName

data class LoginRequest(val userName: String, val password: String)

data class LoginResponse(
    @SerializedName("empId") val empId: Int,
    @SerializedName("expiryTime") val expiryTime: Long,
    @SerializedName("locTracking") val locTracking: String,
    @SerializedName("username") val username: String,
    @SerializedName("token") val token: String
)
data class LocationRequest(
    @SerializedName("accId") val accId: String,
    @SerializedName("timing") val timing: String,
    @SerializedName("lat") val lat: String,
    @SerializedName("lang") val lang: String
)

data class LocationResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("accId") val accId: String,
    @SerializedName("timing") val timing: String,
    @SerializedName("lat") val lat: String,
    @SerializedName("lang") val lang: String
)
