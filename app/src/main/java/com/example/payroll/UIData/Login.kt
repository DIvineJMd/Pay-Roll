package com.example.payroll.UIData


import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.navigation.NavController
import com.example.payroll.R
import com.example.payroll.data.ViewModel
import com.example.payroll.data.Resource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(
    viewModel: ViewModel, context: Context, navController: NavController
) {
    Scaffold(topBar = {
    }) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var clicked by remember { mutableStateOf(false) }
        val loginState by viewModel.loginState.collectAsState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .padding(bottom = 18.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
//                        placeholder = ,
                        label = { Text("User Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Email Icon",
                                tint = MaterialTheme.colorScheme.tertiary // Set the icon color
                            )
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent, // Removes the background color
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary, // Hides the underline when focused
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.tertiary

                        )
                    )

                    // Password input
//                    Text(
//                        text = buildAnnotatedString {
//                            withStyle(style = SpanStyle(fontSize = 20.sp)) { // Increase text size
//                                append("Password") // Add the main text
//                            }
//                            withStyle(
//                                style = SpanStyle(
//                                    color = Color.Red,
//                                    fontSize = 20.sp
//                                )
//                            ) { // Red color for the asterisk
//                                append("*")
//                            }
//                        },
//                        modifier = Modifier
//                            .padding(bottom = 8.dp)
//                            .align(Alignment.Start)
//                    )
                    var passwordVisible by remember { mutableStateOf(false) } // State to track password visibility
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Password Icon",
                                tint = MaterialTheme.colorScheme.tertiary // Set the icon color
                            )
                        },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(id = if (passwordVisible) R.drawable.visibility else R.drawable.visible),
                                contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                modifier = Modifier
                                    .size(24.dp) // Set the icon size
                                    .clickable {
                                        passwordVisible = !passwordVisible
                                    }, // Toggle visibility on click
                                tint = MaterialTheme.colorScheme.tertiary // Set the icon color
                            )
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent, // Removes the background color
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary, // Hides the underline when focused
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.tertiary
                        )
                    )


                    Spacer(modifier = Modifier.height(16.dp))
                    // Login button
                    Button(
                        onClick = {
                            clicked = true
                            viewModel.login(username, password, context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        when {
                            clicked && loginState is Resource.Loading -> {
                                // Show the loading indicator
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            }

                            clicked && loginState is Resource.Error -> {
                                clicked = false // Reset clicked to allow retry
                                val errorMessage =
                                    (loginState as? Resource.Error)?.message?.let { msg ->
                                        // Extract the main error message
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
                                    popUpTo("loginPage") {
                                        inclusive = true
                                    } // Clear the login page from the back stack
                                    launchSingleTop =
                                        true // Avoid multiple instances of the same destination
                                }
                                Log.d("Success", "Login Successful")
                                clicked = false
                            }

                            else -> {
                                // Default state, show the login text and icon
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Log in", style = MaterialTheme.typography.bodyMedium)
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


@Composable
fun LoginAlertDialog(
    viewModel: ViewModel,
    onDismiss: () -> Unit,
    navController: NavController
) {


    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            }
        }
    }
}