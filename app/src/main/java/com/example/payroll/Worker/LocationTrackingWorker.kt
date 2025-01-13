package com.example.payroll.Worker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.example.payroll.data.ApiClient
import com.example.payroll.data.Resource
import com.example.payroll.data.ViewModel
import com.example.payroll.database.LocationDao
import com.example.payroll.database.UserDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume


// Worker class for tracking location
class LocationTrackingWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private lateinit var locationClient: FusedLocationProviderClient
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var wakeLock: PowerManager.WakeLock? = null
    private val locationDao: LocationDao by lazy {
        UserDatabase.getDatabase(applicationContext).locationDao()
    }

    override suspend fun doWork(): Result {
        try {
            locationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            createNotificationChannel()
//            setForegroundAsync(createForegroundInfo("Starting location tracking..."))
            acquireWakeLock()

            // Try to get location with timeout
            val location = fetchLocation()
            if (location != null) {
                handleLocation(location)
                return Result.success()
            }

            return Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Error in location worker", e)
            updateNotification("Location Error", "An unexpected error occurred")
            return Result.failure()
        } finally {
            releaseWakeLock()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Create a high-priority location request
                val locationRequest = LocationRequest.Builder(2000) // 2 second interval
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setWaitForAccurateLocation(true)
                    .setMinUpdateDistanceMeters(0f)
                    .setMaxUpdates(1)
                    .build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        locationClient.removeLocationUpdates(this)
                        continuation.resume(result.lastLocation)
                    }
                }

                // Request location updates
                locationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )

                // Set a timeout of 15 seconds
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    if (continuation.isActive) {
                        locationClient.removeLocationUpdates(locationCallback)
                        continuation.resume(null)
                    }
                }, 15000)

                continuation.invokeOnCancellation {
                    locationClient.removeLocationUpdates(locationCallback)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching location", e)
                continuation.resume(null)
            }
        }
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Location Tracking")
            .setTicker("Location Tracking")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setAutoCancel(false)  // Prevent auto-cancellation
            .setOngoing(true)      // Make notification persistent
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private suspend fun handleLocation(location: Location) {
        val locationText = "Location: ${location.latitude}, ${location.longitude}"
        Log.d(TAG, locationText)
        try {
            val sharedPref = applicationContext.getSharedPreferences("AppData", Context.MODE_PRIVATE)
            val token = sharedPref.getString("auth_token", null)
            val empId = sharedPref.getString("empID", null)

            if (token.isNullOrEmpty() || empId.isNullOrEmpty()) {
                Log.e(TAG, "Token or EmpID is missing. Saving locally.")
                saveLocationLocally(createLocationRequest(location, empId ?: "unknown"))
                updateNotification("Location Saved", "Stored locally (waiting for authentication)")
                return
            }

            val request = createLocationRequest(location, empId)
            try {
                val apiService = ApiClient.getInstance(token)
                val response = apiService.saveLocation(request).awaitResponse()

                if (response.isSuccessful) {
                    Log.d(TAG, "Location successfully saved on the server.")
                    updateNotification("Location Saved", "Location uploaded successfully!")
                    // Try to send any pending locations while we have connectivity
                    withContext(Dispatchers.IO) {
                        if (locationDao.getLocationCount() > 0) {
                            updateNotification("Syncing", "Sending pending locations...")
                            sendPendingLocations()
                        }
                    }
                } else {
                    handleFailedUpload(request, response.errorBody()?.string())
                }
            } catch (e: Exception) {
                handleFailedUpload(request, e.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in handleLocation", e)
            updateNotification("Error", "Failed to process location")
        }
    }

    private fun createLocationRequest(location: Location, empId: String): com.example.payroll.data.LocationRequest {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTime = dateFormat.format(Date(System.currentTimeMillis()))

        return com.example.payroll.data.LocationRequest(
            lat = location.latitude.toString(),
            lang = location.longitude.toString(),
            timing = formattedTime,
            accId = empId
        )
    }

    private suspend fun handleFailedUpload(request: com.example.payroll.data.LocationRequest, error: String?) {
        Log.e(TAG, "Failed to save location: $error")
        saveLocationLocally(request)
        val pendingCount = withContext(Dispatchers.IO) { locationDao.getLocationCount() }
        updateNotification(
            "Offline Mode",
            "Location stored locally ($pendingCount pending)"
        )
    }

    private suspend fun sendPendingLocations() {
        val sharedPref = applicationContext.getSharedPreferences("AppData", Context.MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Token is missing. Cannot send pending locations.")
            return
        }

        val apiService = ApiClient.getInstance(token)
        val pendingLocations = withContext(Dispatchers.IO) {
            locationDao.getAllLocations()
        }

        var successCount = 0
        var failureCount = 0

        for (locationRequest in pendingLocations) {
            try {
                val apiRequest = com.example.payroll.data.LocationRequest(
                  lat =   locationRequest.lat,
                   lang =  locationRequest.lang,
                   timing =  locationRequest.timing,
                   accId =  locationRequest.accId
                )
                println("Try to send pending location: $apiRequest")
                val response = apiService.saveLocation(apiRequest).awaitResponse()
                if (response.isSuccessful) {
                    withContext(Dispatchers.IO) {
                        locationDao.deleteLocationById(locationRequest.id)
                    }
                    successCount++
                    Log.d(TAG, "Successfully sent pending location: $locationRequest")
                } else {
                    failureCount++
                    Log.e(TAG, "Failed to send pending location: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                failureCount++
                Log.e(TAG, "Error sending pending location", e)
            }
        }

        if (successCount > 0 || failureCount > 0) {
            updateNotification(
                "Sync Complete",
                "Sent: $successCount, Failed: $failureCount, Remaining: ${pendingLocations.size - successCount}"
            )
        }
    }

      private suspend fun saveLocationLocally(locationRequest: com.example.payroll.data.LocationRequest) {
        withContext(Dispatchers.IO) {
            locationDao.insertLocation(
                com.example.payroll.database.LocationRequest(
                    lat = locationRequest.lat,
                    lang = locationRequest.lang,
                    timing = locationRequest.timing,
                    accId = locationRequest.accId
                )
            )
        }
    }
    private fun acquireWakeLock() {
        val powerManager =
            applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LocationTracker::WakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutes timeout
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_HIGH  // Changed to HIGH importance
            ).apply {
                description = "Location tracking notifications"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)  // Prevent auto-cancellation
//            .setOngoing(true)      // Make notification persistent
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }


    companion object {
        private const val TAG = "LocationTrackingWorker"
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1001
    }
}

fun startLocationTracking(context: Context) {


    // Set up work constraints
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .build()

    // Create exponential backoff request for work manager
    val workRequest = PeriodicWorkRequestBuilder<LocationTrackingWorker>(
        15, TimeUnit.MINUTES,
        5, TimeUnit.MINUTES
    )
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.LINEAR,
            WorkRequest.MIN_BACKOFF_MILLIS,
            TimeUnit.MILLISECONDS
        )
        .setInitialDelay(0, TimeUnit.MILLISECONDS) // Start immediately
        .addTag("location_tracking")
        .build()

    // Enqueue the work with KEEP policy to prevent accidental cancellation
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "LocationTrackingWork",
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )

    // Schedule a one-time work to ensure immediate start
    val immediateWork = OneTimeWorkRequestBuilder<LocationTrackingWorker>()
        .setConstraints(constraints)
        .build()
    WorkManager.getInstance(context).enqueue(immediateWork)
}


