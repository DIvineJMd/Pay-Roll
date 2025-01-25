package com.example.payroll.UIData

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.example.payroll.BuildConfig
import com.example.payroll.data.AttendanceRequest_api
import com.example.payroll.data.Resource
import com.example.payroll.data.ViewModel
import com.example.payroll.database.AttendanceRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects

class CameraCapture {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "SimpleDateFormat")
    @OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
    @Composable
    fun Update_attendance(viewModel: ViewModel, navController: NavController) {
        val context = LocalContext.current
        val file = context.createImageFile()
        var uri = FileProvider.getUriForFile(
            Objects.requireNonNull(context), BuildConfig.APPLICATION_ID + ".provider", file
        )
        var photoFile by remember { mutableStateOf<File?>(null) }
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
        val attendanceState by viewModel.attendanceState.collectAsState()
        var selected by remember { mutableStateOf(0) }
        var capturedImageUri by remember { mutableStateOf<Uri>(Uri.EMPTY) }
        var remark by remember { mutableStateOf("") }
        var showLoadingDialog by remember { mutableStateOf(false) }
        val createFileAndUri = {
            photoFile = context.createImageFile()
            uri = photoFile?.let {
                FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    it
                )
            }
        }
        val cameraLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    // Compress the image file
                    photoFile = context.compressImageFile(photoFile)
                    capturedImageUri = uri ?: Uri.EMPTY
                }
            }


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
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
                createFileAndUri()
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
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
            containerColor = Color.Transparent, // Light gray background
            topBar = {
                TopAppBar(
                    navigationIcon = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.Black
                    ),
                    title = {
                        Text(
                            "UPDATE ATTENDANCE",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                )
            }
        ) { paddingValues ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Camera Preview Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE0E0E0))
                    ) {
                        if (capturedImageUri != Uri.EMPTY) {
                            Image(
                                modifier = Modifier.fillMaxSize(),
                                painter = rememberImagePainter(
                                    data = capturedImageUri,
                                    builder = {
                                        crossfade(true)
                                        placeholder(drawableResId = android.R.drawable.ic_menu_camera)
                                    }
                                ),
                                contentDescription = "Captured Image"
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Button(
                                onClick = {
                                    val permissionCheckResult = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    )
                                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                        createFileAndUri()  // Create new file and URI before launching camera

                                        cameraLauncher.launch(uri)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFDC2626
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (capturedImageUri != Uri.EMPTY) "Retake Photo" else "Take Photo")
                            }
                        }
                    }
                }

                // Attendance Status Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val buttonModifier = Modifier
                        .weight(1f)
                        .height(48.dp)

                    listOf("Present", "Absent", "Leave").forEachIndexed { index, text ->
                        Button(
                            modifier = buttonModifier,
                            onClick = { selected = index },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected == index) Color(0xFFDC2626) else Color.White,
                                contentColor = if (selected == index) Color.White else Color.Black
                            ),
                            border = BorderStroke(1.dp, Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.elevatedButtonElevation(
                                pressedElevation = 10.dp,
                                defaultElevation = 5.dp
                            )
                        ) {
                            Text(text = text)
                        }
                    }
                }

                // Remarks TextField
                TextField(
                    value = remark,
                    onValueChange = { remark = it },
                    modifier = Modifier
                        .fillMaxWidth()
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

                // Submit Button
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    onClick = {
                        if (photoFile == null || capturedImageUri == Uri.EMPTY) {
                            Toast.makeText(context, "Please take a photo first", Toast.LENGTH_SHORT)
                                .show()
                            showLoadingDialog = false
                            return@Button
                        }
                        println("photoFile = $photoFile")
                        checkGPSStatus(context) { status ->
                            gpsStatus = status == "GPS is ON"
                            println("GPS Status: $gpsStatus")

                            if (gpsStatus) {
                                showDialog = false
                                showLoadingDialog = true
                                println("Debug: Loading dialog set to true")

                                println("Debug: PhotoFile before submission = ${photoFile?.absolutePath}")

                                val fusedLocationClient =
                                    LocationServices.getFusedLocationProviderClient(context)
                                println("Debug: FusedLocationProviderClient initialized")

                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    println("Debug: Location permission granted")




                                    println("Debug:  requesting current location")
                                    Toast.makeText(
                                        context,
                                        "requesting current location",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    fusedLocationClient.getCurrentLocation(
                                        LocationRequest.PRIORITY_HIGH_ACCURACY, null
                                    ).addOnSuccessListener { currentLocation ->
                                        if (currentLocation != null) {
                                            Toast.makeText(
                                                context,
                                                "Got the location",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            println("Debug: Current location fetched = $currentLocation")

                                            processLocation(
                                                viewModel,
                                                currentLocation.latitude.toString(),
                                                currentLocation.longitude.toString(),
                                                photoFile,
                                                context,
                                                selected
                                            )
                                        } else {
                                            println("Debug: Failed to fetch current location")
                                            Toast.makeText(
                                                context,
                                                "Unable to get location. Try again.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showLoadingDialog = false
                                        }
                                    }.addOnFailureListener { e ->
                                        println("Debug: Error fetching current location: ${e.message}")
                                        Toast.makeText(
                                            context,
                                            "Error getting location: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showLoadingDialog = false
                                    }


                                } else {
                                    println("Debug: Location permission not granted")
                                    Toast.makeText(
                                        context,
                                        "Location permission not granted",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showLoadingDialog = false
                                }
                            } else {
                                showDialog = true
                                Toast.makeText(
                                    context,
                                    "Please enable GPS and try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        pressedElevation = 15.dp,
                        defaultElevation = 5.dp
                    )
                ) {
                    Text(
                        "PUNCH IN ",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                }
                LaunchedEffect(attendanceState) {
                    when (attendanceState) {
                        is Resource.Success -> {
                            Toast.makeText(
                                context,
                                (attendanceState as Resource.Success<String>).data,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetloader()
                            navController.popBackStack("Capture", inclusive = true)
                        }

                        is Resource.Error -> {
                            Toast.makeText(
                                context,
                                (attendanceState as Resource.Error).message,
                                Toast.LENGTH_SHORT
                            ).show()
                            println((attendanceState as Resource.Error).message)
                            showLoadingDialog = false
                        }

                        else -> {}
                    }
                }
                if (showLoadingDialog) {
                    BasicAlertDialog(onDismissRequest = {
                        showLoadingDialog = false
                    },
                        content = {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFFDC2626))
                            }
                        }
                    )
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun processLocation(
        viewModel: ViewModel,
        lat: String,
        lang: String,
        photoFile: File?,
        context: Context,
        selected: Int
    ) {
        println("Debug: Location is not null, processing location data")

        val dateFormat = SimpleDateFormat("dd-MM-yyyy")
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentDate = Date()
        println("Debug: Current date = $currentDate")

        val attendanceStatus = when (selected) {
            0 -> "present"
            1 -> "absent"
            2 -> "leave"
            else -> ""
        }
        println("Debug: Attendance status selected = $attendanceStatus")

        val requestData = AttendanceRequest_api(
            status = attendanceStatus,
            transDate = dateFormat.format(currentDate),
            inTime = timeFormat.format(currentDate),
            lat = lat,
            lang = lang,
        )
        println("Debug: Request data created = $requestData")



        println("Debug: Photo file exists, submitting attendance")
        viewModel.saveAttendance(
            requestData,
            photoFile,
            context
        )
        println("Debug: Attendance saved")

        Toast.makeText(
            context,
            "${photoFile!!.length() / 1024} kb",
            Toast.LENGTH_SHORT
        ).show()
        println("Debug: Photo file size displayed to user")
    }

    fun Context.compressImageFile(file: File?): File? {
        return file?.let {
            val compressedFile = File(externalCacheDir, file.name)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 400, 500, true)

            var quality = 95
            while (quality > 50) {
                val outputStream = FileOutputStream(compressedFile)
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.close()

                if (compressedFile.length() <= 300 * 1024) break
                quality -= 5
            }

            compressedFile
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun Context.createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        return File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            externalCacheDir      /* directory */
        )
    }

}


