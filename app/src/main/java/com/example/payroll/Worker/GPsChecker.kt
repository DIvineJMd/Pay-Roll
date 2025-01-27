package com.example.payroll.Worker

import android.content.Context
import android.content.IntentSender
import android.location.LocationManager
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest

fun checkGPSStatus(context: Context, onStatusChecked: (String) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    onStatusChecked(if (isGpsEnabled) "GPS is ON" else "GPS is OFF")
}

fun requestGPSEnable(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>
) {
    val locationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    val builder = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)
        .setAlwaysShow(true)

    val client = LocationServices.getSettingsClient(context)
    val task = client.checkLocationSettings(builder.build())

    task.addOnFailureListener { exception ->
        if (exception is com.google.android.gms.common.api.ResolvableApiException) {
            try {
                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                launcher.launch(intentSenderRequest)
            } catch (sendEx: IntentSender.SendIntentException) {
                sendEx.printStackTrace()
            }
        }
    }
}