package com.example.payroll.UIData

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.payroll.data.AttendanceResponse
import com.example.payroll.data.DashBoardViewModel
import com.example.payroll.data.Resource
import com.example.payroll.ui.theme.red
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*


@RequiresApi(Build.VERSION_CODES.O)
class CalendarPage(
    private val viewModel: DashBoardViewModel,
    private val navController: NavController
) {
    @Composable
    fun AttendanceCard(
        title: String,
        count: Int,
        backgroundColor: Color,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .height(60.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 12.sp,
                    color = Color.White
                )
                Text(
                    text = count.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 0.dp, bottom = 0.dp)
                )
            }
        }
    }

    @Composable
    fun CustomCalendar(
        modifier: Modifier = Modifier,
        attendanceData: AttendanceResponse,
        currentMonth: YearMonth,
        onMonthChanged: (YearMonth) -> Unit,
        onDateSelected: (LocalDate) -> Unit = {}
    ) {
        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

        Box(modifier = modifier.fillMaxSize()) {  // Wrap the content inside a Box
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Attendance Status Cards
                val summaryMap = attendanceData.summary.summary.associate { it.status to it.count }

                Column(modifier = Modifier.weight(0.25f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        AttendanceCard(
                            title = "LEAVE",
                            count = summaryMap["leave"] ?: 0,
                            backgroundColor = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceCard(
                            title = "ABSENT",
                            count = summaryMap["absent"] ?: 0,
                            backgroundColor = Color(0xFFE57373),
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceCard(
                            title = "PRESENT",
                            count = summaryMap["present"] ?: 0,
                            backgroundColor = Color(0xFF81C784),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttendanceCard(
                            title = "WFH",
                            count = summaryMap["wfh"] ?: 0,
                            backgroundColor = Color(0xFF64B5F6),
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceCard(
                            title = "HOLIDAY",
                            count = summaryMap["holiday"] ?: 0,
                            backgroundColor = Color(0xFFFFD54F),
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceCard(
                            title = "WEEKOFF",
                            count = summaryMap["weekoff"] ?: 0,
                            backgroundColor = Color(0xFFFFB74D),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Month Selector
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onMonthChanged(currentMonth.minusMonths(1)) }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Previous Month",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "${
                                currentMonth.month.getDisplayName(
                                    TextStyle.FULL,
                                    Locale.getDefault()
                                )
                            } ${currentMonth.year}",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = { onMonthChanged(currentMonth.plusMonths(1)) }) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Next Month",
                                tint = Color.White
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.75f)
                    ,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.75f)
                            .fillMaxWidth()
                    ) {
                        CalendarGrid(
                            currentMonth = currentMonth,
                            selectedDate = selectedDate,
                            attendanceData = attendanceData,
                            onDateSelected = { date ->
                                selectedDate = date
                                onDateSelected(date)
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(0.25f)  // Give 40% of the remaining space to details
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedDate == null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = red

                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Tap on Date to View Attendance",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Selected Date Card
                        selectedDate?.let { date ->
                            val attendanceInfo = attendanceData.summary.attendance.find {
                                LocalDate.parse(it.transDate) == date
                            }

                            if (attendanceInfo != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (attendanceInfo.status) {
                                            "present" -> Color(0xFF81C784)
                                            "wfh" -> Color(0xFF64B5F6)
                                            "holiday" -> Color(0xFFFFD54F)
                                            "leave" -> Color.Gray
                                            "absent" -> Color(0xFFE57373)
                                            "weekoff" -> Color(0xFFFFB74D)
                                            else -> Color.LightGray
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ){
                                            Text(
                                                text = "${date.dayOfMonth}",
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = date.dayOfWeek.toString(),
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )

                                        }
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = attendanceInfo.status.uppercase(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                            if (attendanceInfo.footer != null) {
                                                Text(
                                                    text = attendanceInfo.footer.toString(),
                                                    fontSize = 12.sp,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CalendarGrid(
        currentMonth: YearMonth,
        selectedDate: LocalDate?,
        attendanceData: AttendanceResponse,
        onDateSelected: (LocalDate) -> Unit
    ) {
        val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
        val firstDayOfMonth = currentMonth.atDay(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
        val days = (1..currentMonth.lengthOfMonth()).map { currentMonth.atDay(it) }

        // Create a map of date to attendance status
        val dateStatusMap = attendanceData.summary.attendance.associate {
            LocalDate.parse(it.transDate) to it.status
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Days of week header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar days
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(42) { index ->
                    val dayIndex = index - firstDayOfWeek
                    if (dayIndex in 0 until days.size) {
                        val date = days[dayIndex]
                        DayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            status = dateStatusMap[date] ?: "",
                            onDateSelected = onDateSelected,
                            footer = attendanceData.summary.attendance.find {
                                LocalDate.parse(it.transDate) == date
                            }?.footer ?: ""
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun DayCell(
        date: LocalDate,
        isSelected: Boolean,
        status: String,
        footer: String,
        onDateSelected: (LocalDate) -> Unit
    ) {
        val backgroundColor = when (status.lowercase()) {
            "leave" -> Color.Gray
            "absent" -> Color(0xFFE57373)
            "present" -> Color(0xFF81C784)
            "wfh" -> Color(0xFF64B5F6)
            "holiday" -> Color(0xFFFFD54F)
            "weekoff" -> Color(0xFFFFB74D)
            else -> Color.Transparent
        }

        Column(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.LightGray,
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable { onDateSelected(date) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = if (backgroundColor != Color.Transparent) Color.White else Color.Black,
                fontSize = 14.sp
            )
            if (footer.isNotEmpty()) {
                Text(
                    text = footer,
                    color = if (backgroundColor != Color.Transparent) Color.White else Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable

    fun CalendarScreen(context: Context) {
        val attendanceState = viewModel.attendance.collectAsState()
        var currentMonth by remember { mutableStateOf(YearMonth.now()) }

        LaunchedEffect(currentMonth) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val firstDay = currentMonth.atDay(1).format(formatter)
            val lastDay = currentMonth.atEndOfMonth().format(formatter)
            viewModel.fetchAttendance(context, firstDay, lastDay)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(),
        ) {
            when (val state = attendanceState.value) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                is Resource.Success -> {
                    CustomCalendar(
                        modifier = Modifier.fillMaxSize(),
                        attendanceData = state.data,
                        onDateSelected = { date ->
//                                println("Selected Date: $date")
                        },
                        currentMonth = currentMonth,
                        onMonthChanged = { newMonth ->
                            currentMonth = newMonth
                        }
                    )
                }

                is Resource.Error -> {
                    Text(text = state.message)
                }
            }
        }
    }

}