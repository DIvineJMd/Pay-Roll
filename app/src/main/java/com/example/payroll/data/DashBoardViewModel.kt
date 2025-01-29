package com.example.payroll.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.payroll.database.User
import com.example.payroll.database.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import retrofit2.HttpException

class DashBoardViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData

    private val _payrollCycles = MutableStateFlow<Resource<List<String>>>(Resource.Loading)
    val payrollCycles: StateFlow<Resource<List<String>>> get() = _payrollCycles

    private val _salarySlip = MutableStateFlow<Resource<SalSlipRequest>>(Resource.Loading)
    val salarySlip: StateFlow<Resource<SalSlipRequest>> = _salarySlip

    private val _attendance = MutableStateFlow<Resource<AttendanceResponse>>(Resource.Loading)
    val attendance: StateFlow<Resource<AttendanceResponse>> = _attendance

    fun fetchSalarySlip(context: Context, cycle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _salarySlip.value = Resource.Loading

            if (userData.value == null) {
                fetchUserData()
            }

            val token = getAuthToken(context)
            if (token.isNullOrEmpty()) {
                _salarySlip.value = Resource.Error("Token is missing. Please login again.")
                return@launch
            }

            try {
                val empId = userData.value?.empId
                if (empId == null) {
                    println("abhibhi nulll")
                    _salarySlip.value = Resource.Error("Employee ID not found")
                    return@launch
                }

                val response = ApiClient.getInstance(token).getSalarySlip(empId, cycle)

                if (response.isSuccessful) {
                    response.body()?.let { salSlip ->
                        _salarySlip.value = Resource.Success(salSlip)
                    } ?: run {
                        _salarySlip.value = Resource.Error("No salary slip data available")
                    }
                } else {
                    _salarySlip.value = Resource.Error("Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: HttpException) {
                Log.e("API_ERROR", "HttpException: ${e.message}")
                _salarySlip.value = Resource.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e("API_ERROR", "Exception: ${e.message}")
                _salarySlip.value = Resource.Error("Unexpected error: ${e.message}")
            }
        }
    }
    fun fetchPayrollCycles(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            fetchUserData()
            _payrollCycles.value = Resource.Loading
            val token = getAuthToken(context)
            if (token.isNullOrEmpty()) {
                _payrollCycles.value = Resource.Error("Token is missing. Please login again.")
                return@launch
            }
            try {
                val response = userData.value?.let {
                    ApiClient.getInstance(token).getPayrollCycles(
                        it.empId)
                }
                if (response != null) {
                    if (response.isSuccessful) {
                        response.body()?.list?.let { cycles ->
                            _payrollCycles.value = Resource.Success(cycles)
                        } ?: run {
                            _payrollCycles.value = Resource.Error("No data available")
                        }
                    } else {
                        _payrollCycles.value = Resource.Error("Error: ${response.code()} - ${response.message()}")
                    }
                }
            } catch (e: HttpException) {
                Log.e("API_ERROR", "HttpException: ${e.message}")
                _payrollCycles.value = Resource.Error("HttpException: ${e.message}")
            } catch (e: Exception) {
                Log.e("API_ERROR", "Exception: ${e.message}")
                _payrollCycles.value = Resource.Error("Exception: ${e.message}")
            }
        }
    }

    fun fetchAttendance(context: Context, dateFrom: String, dateTo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _attendance.value = Resource.Loading
            if(userData.value==null){
                println("null milla ===========================================")
                fetchUserData()
                delay(1000)
            }

            val token = getAuthToken(context)
            if (token.isNullOrEmpty()) {
                _attendance.value = Resource.Error("Token is missing. Please login again.")
                return@launch
            }

            try {
                val empId = userData.value?.empId
                if (empId == null) {
                    println("abhibhi nulll")
                    fetchUserData()
                }

                val response = empId?.let {
                    ApiClient.getInstance(token).getAttendance(
                        reportBy = "accId",
                        reportByValue = it,
                        dateFrom = dateFrom,
                        dateTo = dateTo
                    )
                }

                if (response != null) {
                    if (response.isSuccessful) {
                        response.body()?.let { attendanceData ->
                            _attendance.value = Resource.Success(attendanceData)
                        } ?: run {
                            _attendance.value = Resource.Error("No attendance data available")
                        }
                    } else {
                        _attendance.value = Resource.Error("Error: ${response.code()} - ${response.message()}")
                    }
                }
            } catch (e: HttpException) {
                Log.e("API_ERROR", "HttpException: ${e.message}")
                _attendance.value = Resource.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e("API_ERROR", "Exception: ${e.message}")
                _attendance.value = Resource.Error("Unexpected error: ${e.message}")
            }
        }
    }
    suspend fun getAuthToken(context: Context): String? {
        val data = userRepository.getUser()

        val expiryTime = data?.expiryTime

        val currentTime = System.currentTimeMillis()

        val authToken= data?.token

        if (expiryTime != null) {
            if (authToken != null && expiryTime < currentTime) {
                userRepository.clearUser()
                return null
            }
        }

        return authToken
    }
    fun fetchUserData() {
        viewModelScope.launch {
            val user = userRepository.getUser()
            _userData.value = user
        }
    }
}
