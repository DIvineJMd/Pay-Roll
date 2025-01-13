package com.example.payroll.data


import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("auth/signin")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("location/save")
    fun saveLocation(@Body locationRequest: LocationRequest): Call<Void>

    @GET("location/get-all")
    fun getAllLocations(): Call<List<LocationResponse>>
}


object ApiClient {
    private const val BASE_URL = "https://petroprime.info:8442/emp/api/"

    fun getInstance(authToken: String? = null): ApiService {
        val client = OkHttpClient.Builder()
            .apply {
                if (!authToken.isNullOrEmpty()) {
                    addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $authToken")
                            .build()
                        chain.proceed(request)
                    }
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

