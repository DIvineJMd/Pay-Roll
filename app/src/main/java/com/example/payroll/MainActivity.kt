package com.example.payroll

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.payroll.DashBoardPage.paySlip
import com.example.payroll.UIData.CalendarPage
import com.example.payroll.UIData.CameraCapture
import com.example.payroll.UIData.LoginPage
import com.example.payroll.UIData.MainPage
import com.example.payroll.UIData.hasAllPermissions
import com.example.payroll.UIData.hasBackgroundPermission
import com.example.payroll.UIData.hasLocationPermissions
import com.example.payroll.UIData.isIgnoringBatteryOptimizations
import com.example.payroll.Worker.LocationForegroundService
import com.example.payroll.Worker.areNotificationsEnabled
import com.example.payroll.Worker.openNotificationSettings
import com.example.payroll.data.DashBoardViewModel
import com.example.payroll.data.DashBoardViewModelFactory
import com.example.payroll.ui.theme.PayRollTheme
import com.example.payroll.data.ViewModel
import com.example.payroll.data.ViewModelFactory
import com.example.payroll.database.LocationDao
import com.example.payroll.database.UserDao
import com.example.payroll.database.UserDatabase
import com.example.payroll.database.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Initialize database and repository
        val database = UserDatabase.getDatabase(applicationContext)

        val repository = UserRepository(database.userDao(), database.locationDao(),database.attendanceDao())

        // Create the ViewModel using the factory
        val viewModel: ViewModel by viewModels {
            ViewModelFactory(repository)
        }
        val dashBoardViewModel : DashBoardViewModel by viewModels {
            DashBoardViewModelFactory(repository)
        }
        setContent {
            val navController = rememberNavController()
            var token by remember { mutableStateOf<String?>(null) }
            var isTokenReady by remember { mutableStateOf(false) }

            // Asynchronously fetch the token
            LaunchedEffect(Unit) {
                token = viewModel.getAuthToken(applicationContext)
                delay(1000)
                isTokenReady = true
                println("Token------------>: $token")
            }

            // Wait until token is ready
            if (!isTokenReady) {
                // Show a loading screen or placeholder until token is ready
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                PayRollTheme {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val painter = painterResource(id = R.drawable.background)
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            painter = painter,
                            contentDescription = "background_image",
                            contentScale = ContentScale.Crop
                        )

                            // Navigation host to manage destinations
                            NavHost(
                                navController = navController,
                                startDestination = if (token.isNullOrEmpty()) "loginPage" else "Main",
                                modifier = Modifier.padding()
                            ) {
                                // Login Page
                                composable("loginPage") {
                                    LoginPage(
                                        viewModel = viewModel,
                                        context = this@MainActivity,
                                        navController = navController
                                    )
                                }
                                // Main Page
                                composable("Main") {
                                    MainPage(viewModel = viewModel,
                                        navHostController = navController,
                                        viewModelDashBoard =dashBoardViewModel
                                    )
                                }
                                composable("Capture") {
                                    CameraCapture().Update_attendance(
                                        viewModel,navController
                                    )
                                }
                                composable("DashBoard"){
                                 paySlip().PaySlipScreen(  dashBoardViewModel,applicationContext,navController)
                                }
                                composable("Calendar") {
                                    dashBoardViewModel.fetchUserData()
                                   CalendarPage(dashBoardViewModel,navController).CalendarScreen(applicationContext)
                                }
                            }

                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRestart() {
        super.onRestart()

        val userDao: UserDao by lazy {
            UserDatabase.getDatabase(applicationContext).userDao()
        }

        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = userDao.getCurrentUser()

            if (currentUser != null) {
                withContext(Dispatchers.Main) {
                    try {
                        // Check if notifications are enabled
                        if (!areNotificationsEnabled(applicationContext)) {
                            openNotificationSettings(applicationContext)
                        }

                        // Check remaining permissions
                        if (!hasLocationPermissions(applicationContext)) {
                            openLocationSettings(applicationContext)
                        }

                        if (!hasBackgroundPermission(applicationContext)) {
                            openBackgroundLocationSettings(applicationContext)
                        }

                        if (!isIgnoringBatteryOptimizations()) {
                            openBatteryOptimizationSettings()
                        }

                        // Check location tracking
                        val isTracking = getSharedPreferences("AppData", MODE_PRIVATE)
                            .getBoolean("is_tracking", false)

                        if (!isTracking) {
                            val serviceIntent = Intent(applicationContext, LocationForegroundService::class.java).apply {
                                action = "START_TRACKING"
                            }
                            startService(serviceIntent)
                        } else {

                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error opening settings: ${e.message}")
                    }
                }
            }
        }
    }

    // Function to open notification settings
    @RequiresApi(Build.VERSION_CODES.O)
    private fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // Function to open location settings
    private fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // Function to open background location settings (Android 10 and above)
    private fun openBackgroundLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            val uri = Uri.fromParts("package", context.packageName, null)
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // Function to open battery optimization settings
    private fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}