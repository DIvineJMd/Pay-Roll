package com.example.payroll.Worker

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.payroll.data.ApiClient
import com.example.payroll.database.LocationDao
import com.example.payroll.database.UserDatabase
import com.example.payroll.data.LocationRequest
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LocationForegroundService : Service() {
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationDao: LocationDao
    private lateinit var wakeLock: PowerManager.WakeLock
    private var isTracking = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var pendingLocationsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LocationForegroundService::WakeLock"
        )
        wakeLock.acquire(TimeUnit.HOURS.toMillis(12))

        locationDao = UserDatabase.getDatabase(applicationContext).locationDao()

        // Start periodic pending locations check
        startPendingLocationsCheck()
    }

    private fun startPendingLocationsCheck() {
        pendingLocationsJob = serviceScope.launch {
            while (isActive) {
                val token = getSharedPreferences("AppData", MODE_PRIVATE)
                    .getString("auth_token", null)

                if (!token.isNullOrEmpty()) {
                    sendPendingLocations(token)
                }
                delay(TimeUnit.MINUTES.toMillis(15)) // Check every 15 minutes
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_TRACKING" -> startTracking()
            "STOP_TRACKING" -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (!isTracking) {
            startForeground(1, createNotification("Starting location tracking..."))
            initializeLocationTracking()
            isTracking = true

            getSharedPreferences("AppData", MODE_PRIVATE).edit()
                .putBoolean("is_tracking", true)
                .apply()
        }
    }

    private fun stopTracking() {
        if (isTracking) {
            isTracking = false
            if (::locationClient.isInitialized) {
                locationClient.removeLocationUpdates(locationCallback)
            }

            getSharedPreferences("AppData", MODE_PRIVATE).edit()
                .putBoolean("is_tracking", false)
                .apply()

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun initializeLocationTracking() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            TimeUnit.MINUTES.toMillis(2)
        )
            .setWaitForAccurateLocation(false)
            .setMinUpdateDistanceMeters(0f)
            .setMaxUpdateDelayMillis(TimeUnit.MINUTES.toMillis(15))
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                Log.d(TAG, "Location update received: ${location.latitude}, ${location.longitude}")
                handleLocation(location)
            }
        }
    }

    private fun handleLocation(location: android.location.Location) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTime = dateFormat.format(Date())

        val sharedPref = getSharedPreferences("AppData", MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", null)
        val empId = sharedPref.getString("empID", null) ?: "unknown"

        val locationRequest = LocationRequest(
            lat = location.latitude.toString(),
            lang = location.longitude.toString(),
            timing = formattedTime,
            accId = empId
        )

        serviceScope.launch {
            try {


                // Try to send to API if token exists
                if (!token.isNullOrEmpty()) {
                    try {
                        val apiService = ApiClient.getInstance(token)
                        val response = apiService.saveLocation(locationRequest).awaitResponse()

                        if (response.isSuccessful) {
                            // Save to local database
                            val localLocation = com.example.payroll.database.LocationRequest(
                                lat = locationRequest.lat,
                                lang = locationRequest.lang,
                                timing = locationRequest.timing,
                                accId = locationRequest.accId,
                                uploaded = true
                            )
                            locationDao.insertLocation(localLocation)
                            updateNotification("Location tracked: ${locationRequest.lat}, ${locationRequest.lang} ")
                        }
                    } catch (e: Exception) {
                        // Save to local database
                        val localLocation = com.example.payroll.database.LocationRequest(
                            lat = locationRequest.lat,
                            lang = locationRequest.lang,
                            timing = locationRequest.timing,
                            accId = locationRequest.accId,
                            uploaded = false
                        )
                        locationDao.insertLocation(localLocation)
                        Log.e(TAG, "Error sending location to API", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling location", e)
            }
        }
    }

    private fun createNotification(data: String): Notification {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for location tracking"
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        val notificationIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking")
            .setContentText(data)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(data: String) {
        val notification = createNotification(data)
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(1, notification)
    }

    private suspend fun sendPendingLocations(token: String) {
        try {
            val pendingLocations = locationDao.getPendingLocations()
            val apiService = ApiClient.getInstance(token)

            for (location in pendingLocations) {
                try {
                    val apiRequest = LocationRequest(
                        lat = location.lat,
                        lang = location.lang,
                        timing = location.timing,
                        accId = location.accId
                    )
                    val response = apiService.saveLocation(apiRequest).awaitResponse()

                    if (response.isSuccessful) {
                        locationDao.updateUploadedStatus(location.id, true)
                        Log.d(TAG, "Pending location sent successfully: $location")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending pending location: ${location.id}", e)
                    continue
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendPendingLocations", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        if (::locationClient.isInitialized) {
            locationClient.removeLocationUpdates(locationCallback)
        }
        pendingLocationsJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationForegroundService"
        private const val CHANNEL_ID = "location_tracking_channel"
    }
}