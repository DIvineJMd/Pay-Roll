package com.example.payroll.DashBoardPage

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.wear.compose.material.MaterialTheme
import com.example.payroll.data.HolidayItem
import com.example.payroll.data.Resource
import com.example.payroll.data.ViewModel
import com.example.payroll.ui.theme.red
import java.text.SimpleDateFormat
import java.util.Locale

class Holiday(
    private val navController: NavController,
    private val viewModel: ViewModel,
    private val context: Context
) {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HolidayScreen() {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Holidays Calendar") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black
                    )
                )
            }
        ) { paddingValues ->
            LaunchedEffect(Unit) {
                viewModel.fetchHolidays(context)
            }

            val holidayState by viewModel.Holiday.observeAsState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (holidayState) {
                    is Resource.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = red
                        )
                    }

                    is Resource.Success -> {
                        val holidays = (holidayState as Resource.Success<List<HolidayItem>>).data
                        LazyColumn {
                            items(holidays) { holiday ->
                                HolidayItemView(holiday = holiday)
                            }
                        }
                    }

                    is Resource.Error -> {
                        val error = (holidayState as Resource.Error).message
                        Text(
                            text = error,
                            color = Color.Red,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }

                    null -> {
                        // Initial state, do nothing or show loading
                    }
                }
            }
        }
    }

    @Composable
    fun HolidayItemView(holiday: HolidayItem) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                // Date section
                Column(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .background(color = Color(0xFFF8E8E8), shape = RoundedCornerShape(8.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,

                    ) {
                    Text(
                        text = extractDay(holiday.date),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = red
                        )
                    )
                    Text(
                        text = extractMonth(holiday.date),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),

                        style = TextStyle(
                            fontSize = 16.sp,
                            color = red
                        )
                    )

                }

                // Holiday details section
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = holiday.title,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    if (holiday.remark.isNotEmpty()) {
                        Text(
                            text = holiday.remark,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color.Gray
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                        text = extractDayOfWeek(holiday.date),
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    )
                }
            }
        }
    }

    // Helper functions to extract date components
    private fun extractDay(date: String): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = formatter.parse(date)
            SimpleDateFormat("dd", Locale.getDefault()).format(parsedDate!!)
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractMonth(date: String): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = formatter.parse(date)
            SimpleDateFormat("MMM", Locale.getDefault()).format(parsedDate!!)
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractDayOfWeek(date: String): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = formatter.parse(date)
            SimpleDateFormat("EEEE", Locale.getDefault()).format(parsedDate!!)
        } catch (e: Exception) {
            ""
        }
    }
}