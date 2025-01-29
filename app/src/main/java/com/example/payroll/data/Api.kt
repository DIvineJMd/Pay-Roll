package com.example.payroll.data


import com.example.payroll.database.AttendanceRequest
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    @POST("auth/signin")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("location/save")
    fun saveLocation(@Body locationRequest: LocationRequest): Call<Void>

    @GET("location/get-all")
    fun getAllLocations(): Call<List<LocationResponse>>

    @GET("payroll/get-sal-slip")
    suspend fun getSalarySlip(
        @Query("accId") accId: Int,
        @Query("cycle") cycle: String
    ): Response<SalSlipRequest>

    @GET("payroll/get-cycles")
    suspend fun getPayrollCycles(
        @Query("accId") accId: Int
    ): Response<PayrollCyclesResponse>

    @Multipart
    @POST("attendance/save")
    fun saveAttendance(
        @Part("data") attendanceRequest: AttendanceRequest_api,
        @Part image: MultipartBody.Part
    ): Call<Void>

    @PUT("attendance/update-outtime")
    fun outTime(@Body outData: OutData): Call<Void>

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

