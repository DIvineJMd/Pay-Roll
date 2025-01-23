package com.example.payroll.UIData

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects

class CameraCapture {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "SimpleDateFormat")
    @OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
    @Composable
    fun Update_attendance(viewModel: ViewModel,navController: NavController) {
        val context = LocalContext.current
        val file = context.createImageFile()
        var uri = FileProvider.getUriForFile(
            Objects.requireNonNull(context), BuildConfig.APPLICATION_ID + ".provider", file
        )
        var photoFile by remember { mutableStateOf<File?>(null) }

        val attendanceState by viewModel.attendanceState.collectAsState()
        var selected by remember { mutableStateOf(0) }
        var capturedImageUri by remember { mutableStateOf<Uri>(Uri.EMPTY) }
        var remark by remember { mutableStateOf("") }
        var showLoadingDialog by remember { mutableStateOf(false) }


        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    capturedImageUri = uri ?: Uri.EMPTY
                }
            }



        val createFileAndUri = {
            photoFile = context.createImageFile()
            uri = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider",
                photoFile!!
            )
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
                        showLoadingDialog = true
                        println("Debug: PhotoFile before submission = ${photoFile?.absolutePath}")

                        val locationManager =
                            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val location =
                                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            location?.let {
                                val dateFormat = SimpleDateFormat("dd-MM-yyyy")
                                val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                val currentDate = Date()

                                val attendanceStatus = when (selected) {
                                    0 -> "present"
                                    1 -> "absent"
                                    2 -> "leave"
                                    else -> ""
                                }

                                val requestData = AttendanceRequest_api(
                                    status = attendanceStatus,
                                    transDate = dateFormat.format(currentDate),
                                    inTime = timeFormat.format(currentDate),
                                    lat = it.latitude.toString(),
                                    lang = it.longitude.toString(),
                                )

                                if (photoFile == null) {
                                    Toast.makeText(context, "Please take a photo first", Toast.LENGTH_SHORT).show()
                                    showLoadingDialog = false
                                    return@Button
                                }

                                viewModel.saveAttendance(
                                    requestData,
                                    photoFile,
                                    context
                                )
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
                        "PUNCH IN / OUT",
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


