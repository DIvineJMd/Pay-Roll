package com.example.payroll.UIData

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.payroll.R
import com.example.payroll.Worker.startLocationTracking
import com.example.payroll.data.ViewModel
import com.example.payroll.Worker.LocationForegroundService
import com.example.payroll.database.User
import kotlinx.coroutines.coroutineScope
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.shadow
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.payroll.data.OutData
import com.example.payroll.data.Resource
import com.example.payroll.database.AttendanceRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "CoroutineCreationDuringComposition")
@Composable
fun MainPage(
    modifier: Modifier = Modifier,
    viewModel: ViewModel,
    navHostController: NavController
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 3 })
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
    val coroutineScope = rememberCoroutineScope()
    var attendanceState by remember { mutableStateOf<AttendanceRequest?>(null) }
    var isTracking by remember {
        mutableStateOf(
            context.getSharedPreferences("AppData", Context.MODE_PRIVATE)
                .getBoolean("is_tracking", false)
        )
    }
    var selectedItem by remember { mutableStateOf(0) }

    val user by viewModel.userData.collectAsState()
    // Check initial permission states
    LaunchedEffect(Unit) {
        checkInitialPermissions(context) { location, background, battery ->
            hasLocationPermissions = location
            hasBackgroundPermission = background
            hasBatteryOptimization = battery
        }
    }
    LaunchedEffect(Unit) {
        viewModel.fetchUserData()
    }
    LaunchedEffect(Unit ) {
        attendanceState = viewModel.getAttedance()
    }
    LaunchedEffect(showBottomSheet ) {
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
    LaunchedEffect(hasPermissions, isTracking) {
        println("-----> $hasPermissions $isTracking")
        if (hasPermissions || !isTracking) {
            // Start the location service only if not already tracking
            println("Starting the service as permissions are granted and tracking is not active")

            val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
                action = "START_TRACKING"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            println("Started ======>")
            // Update state and shared preferences
            isTracking = true
            context.getSharedPreferences("AppData", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_tracking", true)
                .apply()

        } else if (!hasPermissions && isTracking) {
            println("permission revoked---------->")
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
                        hasPermissions = hasAllPermissions(context)
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


//        Button(
//            onClick = {
//                if (hasAllPermissions(context)) {
//                    val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
//                        action = if (!isTracking) "START_TRACKING" else "STOP_TRACKING"
//                    }
//
//                    if (!isTracking) {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                            context.startForegroundService(serviceIntent)
//                        } else {
//                            context.startService(serviceIntent)
//                        }
//                    } else {
//                        context.stopService(serviceIntent)
//                    }
//
//                    isTracking = !isTracking
//                    context.getSharedPreferences("AppData", Context.MODE_PRIVATE)
//                        .edit()
//                        .putBoolean("is_tracking", isTracking)
//                        .apply()
//                } else {
//                    requestPermissionsInSequence(
//                        context,
//                        locationPermissionLauncher,
//                        backgroundPermissionLauncher,
//                        hasLocationPermissions,
//                        hasBackgroundPermission,
//                        hasBatteryOptimization
//                    ) {
//                        showBatteryDialog = true
//                    }
//                }
//            }
//        ) {
//            Text(if (isTracking) "Stop Tracking" else "Start Tracking")
//        }

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
                    IconButton(onClick = { /* Handle settings click */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.bell),
                            contentDescription = "Notification",
                            tint = Color.Red
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
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
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
                            PunchInCircleButton(
                                {
                                    if ((attendanceState?.inTime.isNullOrEmpty() && attendanceState?.outTime.isNullOrEmpty()) || (attendanceState?.inTime?.isNotEmpty() == true && attendanceState?.outTime?.isNotEmpty() == true)) {
                                        navHostController.navigate("Capture")
                                    } else {
                                        showBottomSheet = true
                                    }
                                },
                                InPunch = (attendanceState?.inTime.isNullOrEmpty() && attendanceState?.outTime.isNullOrEmpty()) || (attendanceState?.inTime?.isNotEmpty() == true && attendanceState?.outTime?.isNotEmpty() == true)
                            )
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
                                    attendanceState?.let {
                                        Text(
                                            text = it.inTime,
//                                            fontSize = 20.sp,
                                            style = MaterialTheme.typography.titleSmall
                                        )
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
                                    attendanceState?.let {
                                        it.outTime?.let { it1 ->
                                            Text(
                                                text = it1,
                                                //                                            fontSize = 20.sp,
                                                style = MaterialTheme.typography.titleSmall
                                            )
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
                            Text("Reports Page")
                        }
                    }

                    2 -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Calendar Page")
                        }
                    }
                }
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
                            .fillMaxWidth().padding(5.dp)
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
                    SlideToUnlock(
                        modifier = Modifier.padding(5.dp),
                        isLoading = isLoading,
                        onUnlockRequested = {
                            viewModel.userData.value?.empId?.let {
                                OutData(
                                    accId = it,
                                    transDate = transDate,
                                    outTime = outTime,
                                    remark = remark
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

                            Toast.makeText(context, "Punch Out Successfully", Toast.LENGTH_SHORT)
                                .show()

                            val currentDate = Date()

                            attendanceState?.let {
                                viewModel.putOuttime(
                                    id = it.id,
                                    outtime = dateFormat.format(currentDate)
                                )
                            }
                            scope.launch { sheetState.hide()

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
                text = { Text("For reliable location tracking, please disable battery optimization for this app.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBatteryDialog = false
                            context.requestDisableBatteryOptimization()
                            hasBatteryOptimization = true
                            hasPermissions = hasAllPermissions(context)
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
    }
}

//        LocationListScreen(viewModel = viewModel,modifier.padding(it))

@Composable
fun CustomBottomBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFFDC2626)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home button
            AnimatedButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = "HOME",
                selected = selectedItem == 0,
                onClick = { onItemSelected(0) }
            )

            // Grid button
            AnimatedButton(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.grid),
                        contentDescription = "Grid",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = "GRID",
                selected = selectedItem == 1,
                onClick = { onItemSelected(1) }
            )

            // Calendar button //
            AnimatedButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = "CALENDAR",
                selected = selectedItem == 2,
                onClick = { onItemSelected(2) }
            )
        }
    }
}

@Composable
private fun AnimatedButton(
    icon: @Composable () -> Unit,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val transition = updateTransition(selected, label = "selected")

    val buttonColor by transition.animateColor(label = "backgroundColor") { isSelected ->
        if (isSelected) Color(0xFFB91C1C) else Color.Transparent
    }

    val textWidth by transition.animateFloat(label = "textWidth") { isSelected ->
        if (isSelected) 1f else 0f
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(
            horizontal = if (selected) 16.dp else 12.dp,
            vertical = 8.dp
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = null,
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.wrapContentWidth()
        ) {
            icon()
            if (textWidth > 0f) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.graphicsLayer {
                        alpha = textWidth
                    }
                )
            }
        }
    }
}

@Composable
fun PunchInCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    InPunch: Boolean
) {
    Box(
        modifier = modifier
            .size(240.dp)  // Decreased from 270.dp
            .clip(CircleShape)
            .background(Color.White)
            .border(
                width = 30.dp,  // Decreased from 34.dp
                color = if (isEnabled) Color(0xFFE1E5E9) else Color.Gray,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer gradient ring
        Box(
            modifier = Modifier
                .size(180.dp)  // Decreased from 203.dp
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE1E5E9),
                            Color(0xFFF5F7F9)
                        ),
                        center = Offset.Zero,
                        radius = 216f  // Decreased from 243f
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Inner white circle with content
            Box(
                modifier = Modifier
                    .size(156.dp)  // Decreased from 176.dp
                    .background(Color.White, CircleShape)
                    .clip(CircleShape)
                    .clickable(enabled = isEnabled, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(5.dp)  // Decreased from 6.dp
                ) {
                    Icon(
                        painter = painterResource(R.drawable.punchin),
                        contentDescription = "Punch In",
                        modifier = Modifier.size(48.dp),  // Decreased from 54.dp
                        tint = if (InPunch) Color(0xFF4CAF50) else Color(0xFFB91C1C)
                    )
                    Spacer(modifier = Modifier.height(8.dp))  // Decreased from 10.dp
                    Text(
                        text = if (InPunch) "PUNCH IN" else "PUNCH OUT",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,  // Decreased from 15.sp
                            letterSpacing = 0.6.sp  // Decreased from 0.7.sp
                        ),
                        color = Color.Black
                    )
                }
            }
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
