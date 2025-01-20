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
import androidx.compose.runtime.MutableState
import com.example.payroll.database.User
import com.example.payroll.database.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}

class ViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _locations =
        MutableStateFlow<List<com.example.payroll.database.LocationRequest>>(emptyList())

    private val _loginState = MutableStateFlow<Resource<String>>(Resource.Loading)
    val loginState: StateFlow<Resource<String>> = _loginState.asStateFlow()

    private val _Post = MutableStateFlow<Resource<String>>(Resource.Loading)
    val post: StateFlow<Resource<String>> = _Post.asStateFlow()


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


                        saveAuthToken(context, authToken!!, loginResponse.empId.toString())

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

    private fun saveAuthToken(context: Context, token: String, empID: String) {
        val sharedPref = context.getSharedPreferences("AppData", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putString("auth_token", token)
            apply()
        }
        sharedPref.edit().apply {
            putString("empID", empID)
            apply()
        }
    }

    fun saveLocation(request: LocationRequest, context: Context) {
        _Post.value = Resource.Loading
        viewModelScope.launch {
            try {
                val token = getAuthToken(context = context)
                if (token.isNullOrEmpty()) {
                    _Post.value = Resource.Error("Token is missing. Please login again.")
                    return@launch
                }

                val apiService = ApiClient.getInstance("Bearer $token")
                val response = apiService.saveLocation(request).awaitResponse()
                if (response.isSuccessful) {
                    _Post.value = Resource.Success("Location saved successfully!")
                } else {
                    _Post.value =
                        Resource.Error("Failed to save location: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _Post.value = Resource.Error("Exception: ${e.message}")
            }
        }
    }

    suspend fun getAuthToken(context: Context): String? {
        val sharedPref = context.getSharedPreferences("AppData", Context.MODE_PRIVATE)
        val data = userRepository.getUser()

        // Get the expiry time
        val expiryTime = data?.expiryTime

        // Get the current time in milliseconds
        val currentTime = System.currentTimeMillis()

        // Check if the token exists
        val authToken = sharedPref.getString("auth_token", null)

        // If the token exists and has expired, remove it
        if (expiryTime != null) {
            if (authToken != null && expiryTime < currentTime) {
                sharedPref.edit().remove("auth_token").apply()
                sharedPref.edit().remove("empID").apply()
                userRepository.clearUser()
                return null
            }
        }
        if(sharedPref.getString("empID", null) == null){
            return null
        }
        return authToken
    }


}
