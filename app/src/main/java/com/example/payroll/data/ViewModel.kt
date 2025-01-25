package com.example.payroll.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import com.example.payroll.database.AttendanceRequest
import com.example.payroll.database.User
import com.example.payroll.database.UserRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}

class ViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _attendanceState = MutableStateFlow<Resource<String>>(Resource.Loading)
    val attendanceState: StateFlow<Resource<String>> = _attendanceState.asStateFlow()
    private val _outloader = MutableStateFlow<Resource<String>>(Resource.Loading)
    val outloader: StateFlow<Resource<String>> = _outloader.asStateFlow()
    private val _locations =
        MutableStateFlow<List<com.example.payroll.database.LocationRequest>>(emptyList())

    private val _loginState = MutableStateFlow<Resource<String>>(Resource.Loading)
    val loginState: StateFlow<Resource<String>> = _loginState.asStateFlow()

    private val _Post = MutableStateFlow<Resource<String>>(Resource.Loading)
    val post: StateFlow<Resource<String>> = _Post.asStateFlow()
    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData
    private var authToken: String? = null
    val locations: StateFlow<List<com.example.payroll.database.LocationRequest>> =
        userRepository.getAllLocationsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun login(username: String, password: String, context: Context) {
        _loginState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val apiService = ApiClient.getInstance()
                val response = apiService.login(LoginRequest(username, password)).awaitResponse()
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.token != null) {
                        authToken = loginResponse.token

                        // Save the token to the repository
                        userRepository.saveUser(
                            User(
                                empId = loginResponse.empId,
                                expiryTime = loginResponse.expiryTime,
                                locTracking = loginResponse.locTracking,
                                username = loginResponse.username,
                                token = authToken
                            )
                        )


//                        saveAuthToken(context, authToken!!, loginResponse.empId.toString())

                        _loginState.value = Resource.Success("Login Successful")
                        Log.d("LoginResult", "Login Successful")

                    } else {
                        _loginState.value = Resource.Error("Login failed: Missing token")
                        Log.d("LoginResult", "Login failed: Missing token")
                    }
                } else {
                    _loginState.value =
                        Resource.Error("Login failed ${response.errorBody()?.string()}")
                    Log.d("LoginResult", "Login failed why: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _loginState.value = Resource.Error("Exception: ${e.message}")
                Log.d("LoginResult", "Exception: ${e.message}")
            }
        }
    }
    fun resetloader(){
        _attendanceState.value=Resource.Loading
        _outloader.value=Resource.Loading
    }
    fun logout(){
        viewModelScope.launch {
            userRepository.clearAllData()
            resetloader()
        }
    }
    fun punchOut(outData: OutData, context: Context) {
        viewModelScope.launch {
            try {
                _attendanceState.value = Resource.Loading
                val token = getAuthToken(context)
                if (token.isNullOrEmpty()) {
                    _outloader.value = Resource.Error("Token is missing. Please login again.")
                    return@launch
                }

                val apiService = ApiClient.getInstance(token)
                println("Token: $token")

                // Perform the network call off the main thread
                withContext(Dispatchers.IO) {
                    println(outData)
                    val response = apiService.outTime(outData).execute()

                    // Handle API response on the main thread after completion
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            putOuttime(1,outData.outTime)
                            _outloader.value = Resource.Success("Out time updated successfully.")
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown error"
                            val responseCode = response.code()
                            val headers = response.headers()

                            println("Error Response Code: $responseCode")
                            println("Error Response Body: $errorBody")
                            println("Response Headers: $headers")

                            _outloader.value = Resource.Error("Failed to update out time. Error: $errorBody")
                            delay(2000)

                        }
                    }
                }
            } catch (e: HttpException) {
                _outloader.value = Resource.Error("HTTP error occurred: ${e.message()}")
                e.printStackTrace()  // Prints full stack trace for better debugging
                println("HTTP Error: ${e.message()}")
            } catch (e: IOException) {
                _outloader.value = Resource.Error("Network error occurred: ${e.message}")
                e.printStackTrace()  // Prints full stack trace for better debugging
                println("IO Error: ${e.message}")
            } catch (e: Exception) {
                _outloader.value = Resource.Error("An unexpected error occurred: ${e.message}")
                e.printStackTrace()  // Prints full stack trace for better debugging
                println("Unexpected Error: ${e.message}")
            }
        }
    }


    fun saveAttendance(request: AttendanceRequest_api, imageFile: File?, context: Context) {
    _attendanceState.value = Resource.Loading
    viewModelScope.launch {
        try {
            println("Hereeeeeeeeeeeeeeeeee")
            val token = getAuthToken(context)
            if (token.isNullOrEmpty()) {
                _attendanceState.value = Resource.Error("Token is missing. Please login again.")
                return@launch
            }
            println("Mill gyaaaa tokennnnnnnnnnnnnnnn")
            val imagePart = imageFile?.let{
                MultipartBody.Part.createFormData("image",it.name,it.asRequestBody("image/*".toMediaType()))
            }
            // Call the API
            println("PhotooBhi haiiiiiiiiiii ")
            if(imagePart.toString().isNotEmpty()){
                Toast.makeText(
                    context,
                    "image is ready , calling api",
                    Toast.LENGTH_SHORT
                ).show()
            }
            val apiService = ApiClient.getInstance(token)
            val response = imagePart?.let { apiService.saveAttendance(request, it).awaitResponse() }
            println("Responseeeeeeeeeeeeeeeeeeeee")
            if (response != null) {
                if (response.isSuccessful) {
                        println("Successssssssssssssssssssss")
                    _attendanceState.value = Resource.Success("Attendance marked successfully!")
                    userRepository.saveAttendance(attendanceRequest = AttendanceRequest(
                        id = 1,
                        status = request.status,
                        transDate = request.transDate,
                        inTime = request.inTime,
                        lat=request.lat,
                        lang = request.lang,
                        outTime = ""
                    ))

                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _attendanceState.value = Resource.Error("Failed to mark attendance: $errorBody")
                }
            }
        } catch (e: Exception) {
            _attendanceState.value = Resource.Error("Exception: ${e.message}")
        }
    }
}
    suspend fun getAttedance(): AttendanceRequest? {
        return userRepository.getAttendance()
    }
     fun putOuttime(id:Int,outtime:String){
         viewModelScope.launch{ userRepository.updateOuttime(id, outtime) }
    }
//    fun saveLocation(request: LocationRequest, context: Context) {
//        _Post.value = Resource.Loading
//        viewModelScope.launch {
//            try {
//                val token = getAuthToken(context = context)
//                if (token.isNullOrEmpty()) {
//                    _Post.value = Resource.Error("Token is missing. Please login again.")
//                    return@launch
//                }
//
//                val apiService = ApiClient.getInstance("Bearer $token")
//                val response = apiService.saveLocation(request).awaitResponse()
//                if (response.isSuccessful) {
//                    _Post.value = Resource.Success("Location saved successfully!")
//                } else {
//                    _Post.value =
//                        Resource.Error("Failed to save location: ${response.errorBody()?.string()}")
//                }
//            } catch (e: Exception) {
//                _Post.value = Resource.Error("Exception: ${e.message}")
//            }
//        }
//    }

    suspend fun getAuthToken(context: Context): String? {
        val data = userRepository.getUser()

        // Get the expiry time
        val expiryTime = data?.expiryTime

        // Get the current time in milliseconds
        val currentTime = System.currentTimeMillis()

        // Check if the token exists
//        val authToken = sharedPref.getString("auth_token", null)
        val authToken= data?.token

        // If the token exists and has expired, remove it
        if (expiryTime != null) {
            println("----> checking expriry time")
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

