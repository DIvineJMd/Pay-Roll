package com.example.payroll.DashBoardPage

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.payroll.data.LeaveHistoryItem
import com.example.payroll.data.Resource
import com.example.payroll.data.ViewModel
import com.example.payroll.ui.theme.red
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LeaveManagement
    (
    private val navController: NavController,
    private val viewModel: ViewModel,
    private val context: Context
) {
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    fun LeaveManagementScreen(page: Int) {
        var selectedLeaveType by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }
        var showDatePicker by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf<Long?>(null) }
        val pagerState = rememberPagerState(pageCount = { 2 })
        val scope = rememberCoroutineScope()
        val leaveTypes = listOf(
            "Sick Leave",
            "Casual Leave",
            "Paid Leave",
            "Half Day",
            "WFH",
            "Week Off"
        )
        LaunchedEffect (Unit){
            scope.launch {
                pagerState.animateScrollToPage(page)
            }
        }
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Leave Management") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack()}) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                // Tabs with red indicator
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    contentColor = Color.Black,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = Color.Red
                        )
                    }
                ) {
                    listOf("REQUEST", "HISTORY").forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> RequestPage(
                            selectedLeaveType = selectedLeaveType,
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            leaveTypes = leaveTypes,
                            onLeaveTypeSelected = {
                                selectedLeaveType = it
                                expanded = false
                            },
                            selectedDate = selectedDate,
                            onDateClick = {
                                showDatePicker = true
                                println(": $showDatePicker")
                            },
                            onSubmit = { leaveType, date, reason ->
                                val formatedDate = formatDate(date)
                                viewModel.submitLeave(
                                    date = formatedDate,
                                    remark = reason,
                                    leavetype = leaveType,
                                    context = context
                                )
                            }
                        )

                        1 -> HistoryPage()
                    }
                }
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedDate = datePickerState.selectedDateMillis
                            showDatePicker = false
                        }) {
                            Text("OK", color = red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel", color = red)
                        }
                    }
                ) {
                    DatePicker(
                        state = datePickerState,
                        title = {
                        },
                        showModeToggle = true,
                        colors = DatePickerDefaults.colors(

                            todayContentColor = red,
                            todayDateBorderColor = red,
                            selectedDayContainerColor = red,
                            selectedYearContainerColor = red

                        )
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RequestPage(
        selectedLeaveType: String,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        leaveTypes: List<String>,
        onLeaveTypeSelected: (String) -> Unit,
        selectedDate: Long?,
        onDateClick: () -> Unit,
        onSubmit: (String, Long, String) -> Unit // Callback for API call,
    ) {
        var reasonText by remember { mutableStateOf("") }

        // Validation states
        var leaveTypeError by remember { mutableStateOf(false) }
        var dateError by remember { mutableStateOf(false) }
        var reasonError by remember { mutableStateOf(false) }
        val postLeavesate by viewModel.post.collectAsState()
        var onClick by remember { mutableStateOf(false) }

        // Reset form on success or error
        LaunchedEffect(postLeavesate) {
            when (postLeavesate) {
                is Resource.Success, is Resource.Error -> {
                    delay(2000)
                    onLeaveTypeSelected("")
                    reasonText = ""
                    leaveTypeError = false
                    dateError = false
                    reasonError = false
                    onClick = false

                }

                else -> {}
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Leave Type Dropdown
            Text("Leave Type", color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
                OutlinedTextField(
                    value = selectedLeaveType.ifEmpty { "Select leave type" },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = red,
                        unfocusedIndicatorColor = red
                    ),
                    isError = leaveTypeError
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }) {
                    leaveTypes.forEach { leaveType ->
                        DropdownMenuItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            text = { Text(leaveType) },
                            onClick = { onLeaveTypeSelected(leaveType) }
                        )
                    }
                }
            }
            if (leaveTypeError) Text(
                "Please select a leave type",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )

            // Date Selection
            Text("Select Date", color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, red, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(8.dp),
                onClick = onDateClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDate?.let { formatDate(it) } ?: "Select Date",
                        color = Color.Black,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Icon(Icons.Filled.DateRange, contentDescription = "Select date", tint = red)
                }
            }
            if (dateError) Text(
                "Please select a date",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )

            // Reason Input
            Text("Reason for Leave", color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            OutlinedTextField(
                value = reasonText,
                onValueChange = { reasonText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Reason for Leave") },
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = red,
                    unfocusedIndicatorColor = red,
                ),
                isError = reasonError
            )
            if (reasonError) Text(
                "Please provide a reason",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )

            // Submit Button
            Button(
                onClick = {
                    leaveTypeError = selectedLeaveType.isEmpty()
                    dateError = selectedDate == null
                    reasonError = reasonText.isEmpty()

                    if (!leaveTypeError && !dateError && !reasonError) {
                        onSubmit(selectedLeaveType, selectedDate!!, reasonText)
                        onClick = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = red)
            ) {
                when (postLeavesate) {
                    is Resource.Error -> {
                        Text(text = "Error")
                    }

                    Resource.Loading -> {
                        if (onClick) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Text("Submit Request")
                        }
                    }

                    is Resource.Success -> {
                        if (onClick) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "tick",
                                tint = Color.White
                            )
                            Text(
                                "Request Submitted Successfully",
                                modifier = Modifier.padding(horizontal = 3.dp)
                            )

                        } else {
                            Text("Submit Request")

                        }
                    }

                    else -> {
                        Text("Submit Request")
                    }
                }
            }
        }
    }


    @Composable
    fun HistoryPage() {
        val leaveHistory by viewModel.leaveHistory.observeAsState()

        LaunchedEffect(Unit) {
            viewModel.fetchLeaveHistory(context)
        }

        when (leaveHistory) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Success -> {
                val historyList = (leaveHistory as Resource.Success).data
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(historyList) { item ->
                        LeaveHistoryItemView(item)
                    }
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = (leaveHistory as Resource.Error).message ?: "Error fetching leave history")
                }
            }
            else -> {}
        }
    }
    @Composable
    fun LeaveHistoryItemView(item: LeaveHistoryItem) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Header with Leave Type and Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.leaveType.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    // Status Badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (item.status.equals("approved", ignoreCase = true)) {
                                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                                } else {
                                    Color.Gray.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.status.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.status.equals("approved", ignoreCase = true)) {
                                Color(0xFF4CAF50)
                            } else {
                                Color.Gray
                            }
                        )
                    }
                }

                    Spacer(modifier = Modifier.height(4.dp))

                // Date and Reason Section
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    // Date of Leave
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Date of Leave:",
                            style = MaterialTheme.typography.titleMedium,

                        )
                        Text(
                            text = item.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    // Reason (if available)
                    if (!item.remark.isNullOrEmpty()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Reason:",
                                style = MaterialTheme.typography.titleMedium,

                            )
                            Text(
                                text = item.remark,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }


                    // Request and Approval Information
                    Text(
                        text = "Requested on: ${item.reqDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    if (item.status.equals("approved", ignoreCase = true)) {
                        item.lastStatusDate?.let {
                            Text(
                                text = "Approved on: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        item.lastUserName?.let {
                            Text(
                                text = "Approved by: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
    private fun formatDate(millis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date(millis))
    }

}