package com.example.payroll.UIData

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.payroll.R
import com.example.payroll.data.ViewModel
import com.example.payroll.Worker.LocationForegroundService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.shadow
import androidx.navigation.NavController
import com.example.payroll.Worker.areNotificationsEnabled
import com.example.payroll.Worker.checkGPSStatus
import com.example.payroll.Worker.handleLocationTracking
import com.example.payroll.Worker.requestGPSEnable
import com.example.payroll.Worker.stopLocationService
import com.example.payroll.components.CustomBottomBar
import com.example.payroll.components.PunchInCircleButton
import com.example.payroll.components.SlideToUnlock
import com.example.payroll.data.DashBoardViewModel
import com.example.payroll.data.OutData
import com.example.payroll.data.Resource
import com.example.payroll.database.AttendanceRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@SuppressLint(
    "UnusedMaterial3ScaffoldPaddingParameter", "CoroutineCreationDuringComposition",
    "InlinedApi"
)
@Composable
fun MainPage(
    modifier: Modifier = Modifier,
    viewModel: ViewModel,
    navHostController: NavController,
    viewModelDashBoard: DashBoardViewModel
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    var showBatteryDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // State to track permissions
    var hasLocationPermissions by remember { mutableStateOf(false) }
    var hasBackgroundPermission by remember { mutableStateOf(false) }
    var hasBatteryOptimization by remember { mutableStateOf(false) }
    var hasNotification by remember { mutableStateOf(false) }
    var hasPermissions by remember {
        mutableStateOf(
            hasAllPermissions(context)
        )
    }
    val attendanceEntryState by viewModel.attendanceEntry.collectAsState()

    val notificationPermission = rememberPermissionState(
        permission = Manifest.permission.POST_NOTIFICATIONS
    )
    var showDialog by remember { mutableStateOf(false) }
    var gpsStatus by remember { mutableStateOf(false) }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            gpsStatus = true
            showDialog = false

        }
    }


    val coroutineScope = rememberCoroutineScope()
    var attendanceState by remember { mutableStateOf<AttendanceRequest?>(null) }
    var isTracking by remember {
        mutableStateOf(
            context.getSharedPreferences("AppData", Context.MODE_PRIVATE)
                .getBoolean("is_tracking", false)
        )
    }
    var selectedItem by remember { mutableStateOf(0) }
    var showNotificationbox by remember { mutableStateOf(false) }

    val user by viewModel.userData.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.fetchUserData()
    }
    when (user) {
        null -> {
        }

        else -> {
            // Handle when user data is available
            viewModel.fetchLastAttendanceEntry(user!!.empId, context)
        }
    }
    // Check initial permission states
    LaunchedEffect(Unit) {
        checkGPSStatus(context) { status ->
            gpsStatus = status == "GPS is ON"
            println("GPS Status: $gpsStatus")
            if (gpsStatus) {
                showDialog = false
            } else {
                showDialog = true
            }
        }
        checkInitialPermissions(context) { location, background, battery ->
            hasLocationPermissions = location
            hasBackgroundPermission = background
            hasBatteryOptimization = battery
        }
        hasNotification = areNotificationsEnabled(context)
        if (hasNotification) {
            showNotificationbox = false
        } else {
            showNotificationbox = true
        }
        println("==========> notification $hasNotification")
    }

    LaunchedEffect(Unit) {
        attendanceState = viewModel.getAttedance()
    }
    LaunchedEffect(showBottomSheet) {
        attendanceState = viewModel.getAttedance()
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBackgroundPermission = isGranted
        if (isGranted && !hasBatteryOptimization) {
            showBatteryDialog = true
        }
    }
    var remark by remember { mutableStateOf("") }
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
            context = context,
            locationLauncher = locationPermissionLauncher,
            backgroundLauncher = backgroundPermissionLauncher,
            hasLocation = hasLocationPermissions,
            hasBackground = hasBackgroundPermission,
            hasBattery = hasBatteryOptimization,
            hasNotificationPermission = hasNotification,
            requestNotificationPermission = {
                notificationPermission.launchPermissionRequest()
            }
        ) {
            showBatteryDialog = true
        }
    }
    LaunchedEffect(Unit) {
        // Recalculate hasPermissions when the app starts
        hasPermissions = hasAllPermissions(context)
    }
    LaunchedEffect(
        hasPermissions,
        gpsStatus,
        user?.locTracking,
        isTracking,
        showBottomSheet,
        showNotificationbox,
        showDialog,
        showBatteryDialog,
        hasLocationPermissions,
        hasBackgroundPermission
    ) {
        println("Entereddddddddddddddddddddddddd $hasPermissions")
        when (val state = attendanceEntryState) {
            is Resource.Error -> {}
            is Resource.Loading -> {
                println("loading mai hu")
            }
            is Resource.Success -> {
                val attendance = state.data
                println("success mai abhi aaaya: $attendance")
                // Calculate punch status
                val isPunchedIn =
                    attendance.dto.inTime.isNotEmpty() && attendance.dto.outTime.isNullOrEmpty()
                val isPunchedOut =
                    attendance.dto.inTime.isNotEmpty() && attendance.dto.outTime?.isNotEmpty() == true

                // Handle tracking with consolidated logic
                if (hasPermissions && gpsStatus) {
                    println("$hasPermissions $gpsStatus")
                    handleLocationTracking(
                        context = context,
                        trackingPreference = user?.locTracking,
                        isPunchedIn = isPunchedIn,
                        isPunchedOut = isPunchedOut,
                        hasPermissions = hasPermissions
                    )
                } else {
                    println("$hasPermissions $gpsStatus")

                    stopLocationService(context)
                }
            }
        }
    }

//    LaunchedEffect(hasPermissions, isTracking) {
//
//        println("-----> $hasPermissions $isTracking")
//        if ((hasPermissions || !isTracking) && user?.locTracking == "ALWAYS") {
//            // Start the location service only if not already tracking
//            println("Starting the service as permissions are granted and tracking is not active")
//
//            val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
//                action = "START_TRACKING"
//            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(serviceIntent)
//            } else {
//                context.startService(serviceIntent)
//            }
//            println("Started ======>")
//            // Update state and shared preferences
//            isTracking = true
//            context.getSharedPreferences("AppData", Context.MODE_PRIVATE)
//                .edit()
//                .putBoolean("is_tracking", true)
//                .apply()
//
//        } else if (!hasPermissions && isTracking) {
//            println("permission revoked---------->")
//            requestPermissionsInSequence(
//                context = context,
//                locationLauncher = locationPermissionLauncher,
//                backgroundLauncher = backgroundPermissionLauncher,
//                hasLocation = hasLocationPermissions,
//                hasBackground = hasBackgroundPermission,
//                hasBattery = hasBatteryOptimization,
//                hasNotificationPermission = hasNotification,
//                requestNotificationPermission = {
//                    notificationPermission.launchPermissionRequest()
//                }
//            ) {
//                showBatteryDialog = true
//            }
//
//        }
//    }

    // Battery optimization dialog
    // Battery Optimization Dialog
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
                    }
                ) {
                    Text("ALLOW EXCEPTION")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog = false }) {
                    Text("REMIND ME LATER")
                }
            }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                // Prevent dismissal unless GPS is on
                checkGPSStatus(context) { status ->
                    gpsStatus = status == "GPS is ON"
                    if (gpsStatus) {
                        showDialog = false
                    }
                }
            },
            title = { Text("GPS Required") },
            text = { Text("Please enable GPS to continue using the app.") },
            confirmButton = {
                Button(
                    onClick = {
                        requestGPSEnable(context, locationSettingsLauncher)
                    }
                ) {
                    Text("Enable GPS")
                }
            }
        )
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                navigationIcon = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.profile),
                            contentDescription = "Profile Icon",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Hey, ${user?.username ?: "Guest"}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "${user?.empId ?: ""}",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        navHostController.popBackStack("Main", inclusive = true)
                        navHostController.navigate("loginPage")
                    }) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Notification",
                            tint = Color.Red,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
            )
        },
        bottomBar = {
            CustomBottomBar(
                selectedItem = selectedItem,
                onItemSelected = { index ->
                    selectedItem = index
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            index
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val currentDateTime = LocalDateTime.now()
                            val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
                            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - EEEE")
                            val formattedTime = currentDateTime.format(timeFormatter)
                            val formattedDate = currentDateTime.format(dateFormatter)

                            Spacer(Modifier.height(35.dp))
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 45.sp
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(40.dp))
                            when (val state = attendanceEntryState) {
                                is Resource.Loading -> {
                                    CircularProgressIndicator()
                                }

                                is Resource.Success -> {
                                    val attendance = state.data

                                    PunchInCircleButton(
                                        onClick = {
                                            if ((attendance.dto.inTime.isEmpty() && (attendance.dto.outTime?.isEmpty() != false)) ||
                                                (attendance.dto.inTime.isNotEmpty() && (attendance.dto.outTime?.isNotEmpty() == true))
                                            ) {
                                                navHostController.navigate("Capture")
                                            } else {
                                                showBottomSheet = true
                                            }
                                        },
                                        InPunch = (attendance.dto.inTime.isEmpty() && attendance.dto.outTime.isNullOrEmpty()) ||
                                                (attendance.dto.inTime.isNotEmpty() && attendance.dto.outTime?.isNotEmpty() == true)
                                    )
                                }

                                is Resource.Error -> {
                                    // Handle error state if needed
                                    Log.e("MainPage", "Attendance fetch error: ${state.message}")
                                }
                            }


                            Spacer(Modifier.height(80.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(
                                        painter = painterResource(R.drawable.punchintime),
                                        contentDescription = "Punch In Time",
                                    )
                                    when (val state = attendanceEntryState) {
                                        is Resource.Loading -> {
                                            Text(text = "Loading...")
                                        }

                                        is Resource.Error -> {
                                            Text(text = "Error")

                                        }

                                        is Resource.Success -> {
                                            state.data.dto.inTime?.let {
                                                Text(
                                                    text = it,
//                                            fontSize = 20.sp,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "punch in time",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )

                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(
                                        painter = painterResource(R.drawable.punchouttime),
                                        contentDescription = "Punch Out Time",
                                    )
                                    when (val state = attendanceEntryState) {
                                        is Resource.Loading -> {
                                            Text(text = "Loading...")
                                        }

                                        is Resource.Error -> {
                                            Text(text = "Error")

                                        }

                                        is Resource.Success -> {
                                            state.data.dto.outTime?.let {
                                                Text(
                                                    text = it,
                                                    //                                            fontSize = 20.sp,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "punch out time",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            DashboardScreen(Modifier, navHostController, onCalendarClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage + 1
                                    )
                                }
                            }, onAttendanceClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage - 1
                                    )
                                }
                            })
                        }
                    }

                    2 -> {
                        CalendarPage(viewModelDashBoard, navHostController).CalendarScreen(context)
                    }
                }
            }
            LaunchedEffect(pagerState.currentPage) {
                selectedItem = pagerState.currentPage
            }
        }
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(false) }
        val outloader by viewModel.outloader.collectAsState()
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    TextField(
                        value = remark,
                        onValueChange = { remark = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        placeholder = { Text("Remark (If Any)", color = Color.Gray) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                    val transDate = dateFormat.format(currentDate)
                    val outTime = dateTimeFormat.format(currentDate)
                    println("Out Time: $outTime")
                    SlideToUnlock(
                        modifier = Modifier.padding(5.dp),
                        isLoading = isLoading,
                        onUnlockRequested = {
                            isLoading = true
                            viewModel.userData.value?.empId?.let {
                                OutData(
                                    accId = it,
                                    transDate = transDate,
                                    outTime = outTime,
                                    remark = "android_v_3 $remark"
                                )
                            }?.let {
                                viewModel.punchOut(
                                    outData = it,
                                    context
                                )
                            }
                        },
                    )
                    when (outloader) {
                        is Resource.Error -> {
                            isLoading = false
                            Toast.makeText(
                                context,
                                (outloader as Resource.Error).message,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }

                        is Resource.Loading -> {

                        }

                        is Resource.Success -> {
                            isLoading = false
                            Toast.makeText(context, "Punch Out Successfully", Toast.LENGTH_SHORT)
                                .show()

                            scope.launch {
                                sheetState.hide()

                            }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                                viewModel.resetloader()
                            }

                        }
                    }
                }
            }
        }
        if (showBatteryDialog) {
            AlertDialog(
                onDismissRequest = { showBatteryDialog = false },
                title = { Text("Battery Optimization") },
                text = { Text("For reliable performance, please allow an exception for this app in battery optimization settings.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBatteryDialog = false
                            context.requestDisableBatteryOptimization()
                            hasBatteryOptimization = true
                            hasPermissions = hasAllPermissions(context)
                        }
                    ) {
                        Text("ALLOW ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatteryDialog = false }) {
                        Text("REMIND ME LATER")
                    }
                }
            )
        }

    }
}

@Composable
fun LocationListScreen(viewModel: ViewModel, modifier: Modifier) {
    val locations by viewModel.locations.collectAsState()

    LazyColumn(modifier = modifier) {
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
@RequiresApi(Build.VERSION_CODES.O)
fun requestPermissionsInSequence(
    context: Context,
    locationLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    backgroundLauncher: ManagedActivityResultLauncher<String, Boolean>,
    hasLocation: Boolean,
    hasBackground: Boolean,
    hasBattery: Boolean,
    hasNotificationPermission: Boolean,
    requestNotificationPermission: () -> Unit,
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

        !hasNotificationPermission -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                println("Requestiing notification")
                requestNotificationPermission()
            } else {
                openNotificationSettings(context)
            }
        }

        !hasBattery -> {
            showBatteryDialog()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
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

fun hasLocationPermissions(context: Context): Boolean {
    return arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

fun hasBackgroundPermission(context: Context): Boolean {
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
            context.isIgnoringBatteryOptimizations() &&
            areNotificationsEnabled(context)
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
