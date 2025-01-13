package com.example.payroll

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.payroll.UIData.LoginPage
import com.example.payroll.UIData.MainPage
import com.example.payroll.ui.theme.PayRollTheme
import com.example.payroll.data.ViewModel
import com.example.payroll.data.ViewModelFactory
import com.example.payroll.database.UserDatabase
import com.example.payroll.database.UserRepository
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: ViewModel by viewModels()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize database and repository
        val database = UserDatabase.getDatabase(applicationContext)

        val repository = UserRepository(database.userDao(),database.locationDao())

        // Create the ViewModel using the factory
        val viewModel: ViewModel by viewModels {
            ViewModelFactory(repository)
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
                    Scaffold(
                        topBar = {
                            TopAppBar(title = { Text("Pay Roll", fontWeight = FontWeight.Bold) })
                        }
                    ) { innerPadding ->
                        // Navigation host to manage destinations
                        NavHost(
                            navController = navController,
                            startDestination = if (token.isNullOrEmpty()) "loginPage" else "Main",
                            modifier = Modifier.padding(innerPadding)
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
                                MainPage()
                            }
                        }
                    }
                }
            }
        }
    }
}
