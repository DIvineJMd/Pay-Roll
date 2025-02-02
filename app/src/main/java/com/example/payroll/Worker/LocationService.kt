package com.example.payroll.Worker

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
fun startLocationService(context: Context) {
    val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
        action = "START_TRACKING"
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
    } else {
        context.startService(serviceIntent)
    }
}

fun stopLocationService(context: Context) {
    val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
        action = "STOP_TRACKING"
    }
    context.stopService(serviceIntent)
}
@RequiresApi(Build.VERSION_CODES.O)
fun handleLocationTracking(
    context: Context,
    trackingPreference: String?,
    isPunchedIn: Boolean,
    isPunchedOut: Boolean,
    hasPermissions: Boolean
) {
    val normalizedPreference = trackingPreference?.lowercase() ?: "always" // Default to "always" if null
    println("yaaaaaaaaaaaaaa$hasPermissions, $normalizedPreference")
    when (normalizedPreference) {
        "out" -> {
            if (isPunchedOut && hasPermissions) {
                startLocationService(context)
            } else {
                stopLocationService(context)
            }
        }
        "in" -> {
            if (isPunchedIn && hasPermissions) {
                startLocationService(context)
            } else {
                stopLocationService(context)
            }
        }
        "in-out", "work-hrs" -> {
            if (isPunchedIn && !isPunchedOut && hasPermissions) {
                startLocationService(context)
            } else {
                stopLocationService(context)
            }
        }
        "always" -> {
            if (hasPermissions) {
                startLocationService(context)
            } else {
                stopLocationService(context)
            }
        }
        "never" -> {
            stopLocationService(context)
        }
        else -> {
            // Default case: Start tracking always if no condition matches
            if (hasPermissions) {
                startLocationService(context)
            } else {
                stopLocationService(context)
            }
        }
    }
}