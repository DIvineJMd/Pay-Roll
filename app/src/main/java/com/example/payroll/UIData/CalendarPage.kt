package com.example.payroll.UIData
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

class CalendarPage {

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun CustomCalendar(
        modifier: Modifier = Modifier,
        onDateSelected: (LocalDate) -> Unit = {}
    ) {
        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
        var currentMonth by remember { mutableStateOf(YearMonth.now()) }

        val firstDayOfMonth = currentMonth.atDay(1)
        val lastDayOfMonth = currentMonth.atEndOfMonth()
        val daysInMonth = lastDayOfMonth.dayOfMonth

        val days = (1..daysInMonth).map { currentMonth.atDay(it) }
        val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        Column(
            modifier = modifier.padding(16.dp)
        ) {
            // Month and Year Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous Month")
                }
                Text(
                    text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next Month", tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Days of the Week
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val firstDayIndex = firstDayOfMonth.dayOfWeek.value % 7
            val totalCells = days.size + firstDayIndex

            LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(totalCells) { index ->
                    if (index < firstDayIndex) {
                        Spacer(modifier = Modifier.height(40.dp)) // Empty cell
                    } else {
                        val day = days[index - firstDayIndex]
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .background(
                                    if (selectedDate == day) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    CircleShape
                                )
                                .clickable {
                                    selectedDate = day
                                    onDateSelected(day)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                color = if (selectedDate == day) Color.White else Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    @Preview

    fun CalendarScreen() {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Custom Calendar") }) }
        ) {
            CustomCalendar(modifier = Modifier.padding(it), onDateSelected = { date ->
                println("Selected Date: $date")
            })
        }
    }
}

