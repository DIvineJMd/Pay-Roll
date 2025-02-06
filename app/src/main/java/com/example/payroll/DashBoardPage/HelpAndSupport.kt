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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HelpAndSupport {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun PermissionsScreen(
        context: Context,
        navController: NavController,
        modifier: Modifier = Modifier
    ) {
        var refreshing by remember { mutableStateOf(false) }
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
            mutableStateOf(areNotificationsEnabled(context))
        }
        val scope = rememberCoroutineScope()

        var gpsStatus by remember(refreshKey) { mutableStateOf(false) }
        val pullRefreshState = rememberPullRefreshState(
            refreshing = refreshing,
            onRefresh = {
                refreshing = true
                scope.launch {
                    refreshKey += 1
                    refreshing = false
                }
            }
        )
        val locationSettingsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                gpsStatus = true
            }
        }
        LaunchedEffect(Unit,refreshKey) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pullRefresh(pullRefreshState)
            ) {
                Box(modifier.fillMaxWidth().align(Alignment.TopStart).background(Color.Gray).padding(5.dp)){
                    Text(text = "Swipe down to refresh", color = Color.White, modifier = modifier.align(
                        Alignment.Center))
                }
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()) // Make the Column scrollable
                       .padding(16.dp)
                ) {
                    PermissionItem(
                        title = "Location Permissions",
                        isGranted = hasLocationPermissions,
                        description = "Required for attendance tracking",
                        onRequestPermission = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )

                    // Background Location
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        PermissionItem(
                            title = "Background Location",
                            isGranted = hasBackgroundLocation,
                            description = "Allow location access when app is in background",
                            onRequestPermission = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }

                    // Notifications
                    PermissionItem(
                        title = "Notifications",
                        isGranted = hasNotificationPermission,
                        description = "Receive app notifications",
                        onRequestPermission = {
                            scope.launch(Dispatchers.Main) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        }
                    )

                    // Battery Optimization
                    PermissionItem(
                        title = "Battery Optimization",
                        isGranted = hasBatteryOptimization,
                        description = "Disable battery optimization for accurate tracking",
                        onRequestPermission = {
//                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
//                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                            }
//                            context.startActivity(intent)
                            context.requestDisableBatteryOptimization()
                        }
                    )

                    // GPS Status
                    PermissionItem(
                        title = "GPS Status",
                        isGranted = gpsStatus,
                        description = "Location services must be enabled",
                        onRequestPermission = {
//                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
//                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                            }
//                            context.startActivity(intent)
                            requestGPSEnable(context, locationSettingsLauncher)

                        }
                    )
                }
                PullRefreshIndicator(
                    refreshing = refreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
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