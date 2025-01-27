package com.example.payroll.Worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

class BootReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val trackingPrefs = context.getSharedPreferences("AppData", Context.MODE_PRIVATE)
            val wasTracking = trackingPrefs.getBoolean("is_tracking", false)

            if (wasTracking) {
                val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
                    action = "START_TRACKING"
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}