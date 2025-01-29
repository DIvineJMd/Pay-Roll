package com.example.payroll.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
// Login
data class LoginRequest(val userName: String, val password: String)

data class LoginResponse(
    @SerializedName("empId") val empId: Int,
    @SerializedName("expiryTime") val expiryTime: Long,
    @SerializedName("locTracking") val locTracking: String,
    @SerializedName("username") val username: String,
    @SerializedName("token") val token: String
)
// location
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
// punch_in
data class AttendanceRequest_api(
    val status: String,
    val transDate: String,
    val inTime: String,
    val lat: String,
    val lang: String,
    )
//punch out
data class OutData(
    val accId: Int,
    val transDate: String,
    val outTime: String,
    val remark: String
)

// get SAL SLIP

data class SalSlipRequest(
    @SerializedName("bean") val bean: EmployeePayroll
)

data class EmployeePayroll(
    val empName: String,
    val cycle: String,
    val ctc: Double,
    val ctcType: String,
    val presentDays: Double,
    val absentDays: Double,
    val leaves: Double,
    val wfhDays: Double,
    val halfDays: Double,
    val weekOffs: Double,
    val paidLeaves: Double,
    val holidays: Double,
    val totalMarkDays: Double,
    val payableDays: Double,
    val perDaySalary: Double,
    val totalSalary: Double,
    val additions: Double,
    val deductions: Double,
    val netPayable: Double,
    val paid: Double,
    val balance: Double,
    val adjustDTOs: List<Adjustment>,
    val paymentDTOs: List<Payment>
)

data class Adjustment(
    val cycle: String,
    val transDate: String,
    val amount: Double,
    val remark: String,
    val empAccName: String,
    val transType: String
)

data class Payment(
    val cycle: String,
    val transDate: String,
    val crAccName: String,
    val amount: Double,
    val remark: String,
    val drAccName: String
)
// get payroll cycles
data class PayrollCyclesResponse(
    val list: List<String>
)
//
// Data class for the attendance response
data class AttendanceResponse(
    val summary: Summary
)

data class Summary(
    val attendance: List<AttendanceRecord>,
    val summary: List<StatusSummary>
)

data class AttendanceRecord(
    val transDate: String,
    val footer: String,
    val status: String
)

data class StatusSummary(
    val status: String,
    val count: Int
)
