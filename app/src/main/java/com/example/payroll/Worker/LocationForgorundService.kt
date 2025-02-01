package com.example.payroll.Worker

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import android.provider.Settings
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
        startGPSStatusMonitoring()
        initializeLocationTracking()
    }

    private fun startPendingLocationsCheck() {
        pendingLocationsJob = serviceScope.launch {
            while (isActive) {
                    sendPendingLocations()
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
            startForeground(1, createNotification("We are happy to have you onboard! 🚀 Let's get started with PayRoll."))
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

    // Modify initializeLocationTracking to include GPS check
    private fun initializeLocationTracking() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled) {
            updateNotification(" Urgent: Open PayRoll for Important Updates!  We need your attention.")
            return
        }

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
                serviceScope.launch{ handleLocation(location) }
            }
        }
    }
    private fun startGPSStatusMonitoring() {
        serviceScope.launch {
            while (isActive) {
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                if (!isGpsEnabled) {
                    showGPSNotification()
                } else {
                    // GPS is now ON
                    dismissGPSNotification()

                    // Restart location tracking if it was previously started
                    if (isTracking) {
                        withContext(Dispatchers.Main) {
                            initializeLocationTracking()
                        }
                    }
                }
                delay(TimeUnit.MINUTES.toMillis(5))
            }
        }
    }

    private fun showGPSNotification() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(" We need your attention.")
            .setContentText("Tap to Open App Settings")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(GPS_NOTIFICATION_ID, notification)
    }

    private fun dismissGPSNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(GPS_NOTIFICATION_ID)
    }

    private suspend fun handleLocation(location: android.location.Location) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTime = dateFormat.format(Date())
        val database =UserDatabase.getDatabase(this).userDao().getCurrentUser()
        val token = database?.token
        val empId = database?.empId
        println("handling location data get ->.>>>>>>>> $token , $empId")
        val locationRequest = LocationRequest(
            lat = location.latitude.toString(),
            lang = location.longitude.toString(),
            timing = formattedTime,
            accId = empId.toString()
        )

        serviceScope.launch {
            try {


                if(isInternetAvailable(context = applicationContext)){
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
                                Log.d(
                                    "Debug Location ",
                                    "api success : $localLocation inserted in database"
                                )
//                                updateNotification("Location tracked: ${locationRequest.lat}, ${locationRequest.lang} ")
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
                }else{
                    val localLocation = com.example.payroll.database.LocationRequest(
                        lat = locationRequest.lat,
                        lang = locationRequest.lang,
                        timing = locationRequest.timing,
                        accId = locationRequest.accId,
                        uploaded = false
                    )
                    locationDao.insertLocation(localLocation)
                    Log.d(TAG, "Internet Off saving ing locally")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling location", e)
            }
        }
    }
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    private fun createNotification(data: String): Notification {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "PayRoll Service",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for AppService"
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
            .setContentTitle("PayRoll Service")
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

    private suspend fun sendPendingLocations() {
        if(isInternetAvailable(applicationContext)){
            try {
                val token = UserDatabase.getDatabase(this).userDao().getCurrentUser()?.token
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
        }else{
            updateNotification("Please Turn on Internet While Working.")
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
        private const val GPS_NOTIFICATION_ID = 2 // Unique ID for GPS notification

    }
}