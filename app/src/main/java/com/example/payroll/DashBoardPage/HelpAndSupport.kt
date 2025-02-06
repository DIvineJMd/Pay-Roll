package com.example.payroll.DashBoardPage

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.payroll.UIData.checkInitialPermissions
import com.example.payroll.UIData.hasBackgroundPermission
import com.example.payroll.UIData.hasLocationPermissions
import com.example.payroll.UIData.isIgnoringBatteryOptimizations
import com.example.payroll.UIData.openNotificationSettings
import com.example.payroll.UIData.requestDisableBatteryOptimization
import com.example.payroll.Worker.areNotificationsEnabled
import com.example.payroll.Worker.checkGPSStatus
import com.example.payroll.Worker.requestGPSEnable

class HelpAndSupport {
    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun PermissionsScreen(
        context: Context,
        navController: NavController,
        modifier: Modifier = Modifier
    ) {
        var refreshKey by remember { mutableStateOf(0) }

        var hasLocationPermissions by remember(refreshKey) {
            mutableStateOf(hasLocationPermissions(context))
        }
        var hasBackgroundLocation by remember(refreshKey) {
            mutableStateOf(hasBackgroundPermission(context))
        }
        var hasBatteryOptimization by remember(refreshKey) {
            mutableStateOf(context.isIgnoringBatteryOptimizations())
        }
        var hasNotificationPermission by remember(refreshKey) {
            mutableStateOf(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    areNotificationsEnabled(context)
                else true
            )
        }
        var gpsStatus by remember(refreshKey) { mutableStateOf(false) }

        val refreshPermissions = {
            refreshKey++ // Increment to force recomposition
        }

        val backgroundPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasBackgroundLocation = isGranted
            refreshPermissions()
        }

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            hasLocationPermissions = permissions.values.all { it }
            if (hasLocationPermissions && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            refreshPermissions()
        }

        val locationSettingsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                gpsStatus = true
            }
            refreshPermissions()
        }

        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasNotificationPermission = isGranted
            refreshPermissions()
        }

        LaunchedEffect(Unit) {
            checkGPSStatus(context) { status ->
                gpsStatus = status == "GPS is ON"
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = { Text("App Permissions") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Location Permissions
                PermissionItem(
                    title = "Location Permissions",
                    isGranted = hasLocationPermissions,
                    description = "Required for attendance tracking",
                    onRequestPermission = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )

                // Background Location
                // Background Location
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    PermissionItem(
                        title = "Background Location",
                        isGranted = hasBackgroundLocation,
                        description = "Allow location access when app is in background",
                        onRequestPermission = {
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }

                // Notification Permissions

                    PermissionItem(
                        title = "Notifications",
                        isGranted = hasNotificationPermission,
                        description = "Receive app notifications",
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }else {
                                openNotificationSettings(context)
                            }
                        }
                    )


                // Battery Optimization
                PermissionItem(
                    title = "Battery Optimization",
                    isGranted = hasBatteryOptimization,
                    description = "Disable battery optimization for accurate tracking",
                    onRequestPermission = {
                        context.requestDisableBatteryOptimization()
                        refreshPermissions()
                    }
                )

                // GPS Status
                PermissionItem(
                    title = "GPS Status",
                    isGranted = gpsStatus,
                    description = "Location services must be enabled",
                    onRequestPermission = {
                        requestGPSEnable(context, locationSettingsLauncher)
                    }
                )
            }
        }
    }

    @Composable
    fun PermissionItem(
        title: String,
        isGranted: Boolean,
        description: String,
        onRequestPermission: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Status Indicator
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (isGranted) Color.Green else Color.Red,
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Request Button
                Button(
                    onClick = onRequestPermission,
                    enabled = !isGranted
                ) {
                    Text("Request")
                }
            }
        }
    }

}