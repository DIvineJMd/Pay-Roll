package com.example.payroll.UIData


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.sharp.Lock
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.payroll.R
import com.example.payroll.data.ViewModel
import com.example.payroll.data.Resource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


@SuppressLint("InlinedApi")
@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LoginPage(
    viewModel: ViewModel, context: Context, navController: NavController
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var clicked by remember { mutableStateOf(false) }
    val loginState by viewModel.loginState.collectAsState()

    val notificationPermission = rememberPermissionState(
        permission = Manifest.permission.POST_NOTIFICATIONS
    )


    LaunchedEffect(Unit) {
        if (!notificationPermission.status.isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission.launchPermissionRequest()
            } else {
                openNotificationSettings(context)
            }
        }

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
                    Text(
                        "Pay Roll",
                        style = MaterialTheme.typography.titleLarge,
                    )
                })
        }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rest of the UI code remains the same...
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.companyicon),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 18.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    // Username Card
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("User Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Email Icon",
                                    tint = Color.Black
                                )
                            },
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color(0xFFDC2626),
                                focusedLabelColor = Color(0xFFDC2626)
                            )
                        )
                    }

                    // Password Card
                    var passwordVisible by remember { mutableStateOf(false) }
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Password Icon",
                                    tint = Color.Black
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(id = if (passwordVisible) R.drawable.visibility else R.drawable.visible),
                                    contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { passwordVisible = !passwordVisible },
                                    tint = Color.Black
                                )
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color(0xFFDC2626),
                                focusedLabelColor = Color(0xFFDC2626)
                            )
                        )
                    }

                    // Login Button
                    Button(
                        onClick = {
                            clicked = true
                            viewModel.login(username, password, context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626)
                        )
                    ) {
                        when {
                            clicked && loginState is Resource.Loading -> {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            }

                            clicked && loginState is Resource.Error -> {
                                clicked = false
                                val errorMessage =
                                    (loginState as? Resource.Error)?.message?.let { msg ->
                                        val errorRegex = """"message":"(.*?)"""".toRegex()
                                        val matchResult = errorRegex.find(msg)
                                        matchResult?.groups?.get(1)?.value ?: "Invalid credential"
                                    } ?: "Invalid credential"
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            }

                            clicked && loginState is Resource.Success<*> -> {
                                val successMessage = (loginState as Resource.Success<String>).data
                                Text(text = successMessage)
                                navController.navigate("Main") {
                                    popUpTo("loginPage") { inclusive = true }
                                    launchSingleTop = true
                                }
                                Log.d("Success", "Login Successful")
                                clicked = false
                            }

                            else -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Log in", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.ExitToApp,
                                        contentDescription = "Login",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
