package com.example.payroll.UIData

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.payroll.Worker.startLocationTracking
import com.example.payroll.data.ViewModel
import com.example.payroll.Worker.LocationForegroundService


@Composable
fun MainPage(modifier: Modifier = Modifier, viewModel: ViewModel) {
    var showBatteryDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // State to track permissions
    var hasLocationPermissions by remember { mutableStateOf(false) }
    var hasBackgroundPermission by remember { mutableStateOf(false) }
    var hasBatteryOptimization by remember { mutableStateOf(false) }
    var hasPermissions by remember {
        mutableStateOf(
            hasAllPermissions(context)
        )
    }
    var isTracking by remember {
        mutableStateOf(
            context.getSharedPreferences("AppData", Context.MODE_PRIVATE)
                .getBoolean("is_tracking", false)
        )
    }
    // Check initial permission states
    LaunchedEffect(Unit) {
        checkInitialPermissions(context) { location, background, battery ->
            hasLocationPermissions = location
            hasBackgroundPermission = background
            hasBatteryOptimization = battery
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBackgroundPermission = isGranted
        if (isGranted && !hasBatteryOptimization) {
            showBatteryDialog = true
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val areGranted = permissions.values.all { it }
        hasLocationPermissions = areGranted
        if (areGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        requestPermissionsInSequence(
            context,
            locationPermissionLauncher,
            backgroundPermissionLauncher,
            hasLocationPermissions,
            hasBackgroundPermission,
            hasBatteryOptimization
        ) {
            showBatteryDialog = true
        }
    }

    // Battery optimization dialog
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text("Battery Optimization") },
            text = { Text("For reliable location tracking, please disable battery optimization for this app.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryDialog = false
                        context.requestDisableBatteryOptimization()
                        hasBatteryOptimization = true
                    hasPermissions=  hasAllPermissions(context)
                    }
                ) {
                    Text("DISABLE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog = false }) {
                    Text("LATER")
                }
            }
        )
    }


    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                if (hasAllPermissions(context)) {
                    val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
                        action = if (!isTracking) "START_TRACKING" else "STOP_TRACKING"
                    }

                    if (!isTracking) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } else {
                        context.stopService(serviceIntent)
                    }

                    isTracking = !isTracking
                } else {
                    requestPermissionsInSequence(
                        context,
                        locationPermissionLauncher,
                        backgroundPermissionLauncher,
                        hasLocationPermissions,
                        hasBackgroundPermission,
                        hasBatteryOptimization
                    ) {
                        showBatteryDialog = true
                    }
                }
            }
        ) {
            Text(if (isTracking) "Stop Tracking" else "Start Tracking")
        }

        // Status texts
        if (isTracking) {
            Text(
                "Location tracking is active",
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (!hasPermissions) {
            Text(
                "⚠️ Some permissions are missing",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        LocationListScreen(viewModel = viewModel)
    }
}

@Composable
fun LocationListScreen(viewModel: ViewModel) {
    val locations by viewModel.locations.collectAsState()

    LazyColumn {
        items(locations) { location ->
            Card(
                modifier = Modifier.padding(5.dp),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 5.dp
                )
            ) {
                Text(
                    modifier = Modifier.padding(3.dp),
                    text = "Lat: ${location.lat}, Lng: ${location.lang}, Time: ${location.timing}, Status: ${location.uploaded}"
                )
            }
        }
    }
}


// Permission handling functions
fun requestPermissionsInSequence(
    context: Context,
    locationLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    backgroundLauncher: ManagedActivityResultLauncher<String, Boolean>,
    hasLocation: Boolean,
    hasBackground: Boolean,
    hasBattery: Boolean,
    showBatteryDialog: () -> Unit
) {
    when {
        !hasLocation -> {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        !hasBackground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        !hasBattery -> {
            showBatteryDialog()
        }
    }
}

fun checkInitialPermissions(
    context: Context,
    callback: (location: Boolean, background: Boolean, battery: Boolean) -> Unit
) {
    val hasLocation = hasLocationPermissions(context)
    val hasBackground = hasBackgroundPermission(context)
    val hasBattery = context.isIgnoringBatteryOptimizations()
    callback(hasLocation, hasBackground, hasBattery)
}

private fun hasLocationPermissions(context: Context): Boolean {
    return arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private fun hasBackgroundPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun hasAllPermissions(context: Context): Boolean {
    return hasLocationPermissions(context) &&
            hasBackgroundPermission(context) &&
            context.isIgnoringBatteryOptimizations()
}

@SuppressLint("BatteryLife")
fun Context.requestDisableBatteryOptimization() {
    val intent = Intent().apply {
        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        data = Uri.parse("package:$packageName")
    }
    startActivity(intent)
}

fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(packageName)
}
