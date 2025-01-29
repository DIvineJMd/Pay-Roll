package com.example.payroll.DashBoardPage

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.payroll.data.DashBoardViewModel
import com.example.payroll.data.Resource
import com.example.payroll.components.CycleDropdownMenu
import com.example.payroll.ui.theme.red

class paySlip{
    @SuppressLint("StateFlowValueCalledInComposition", "DefaultLocale")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PaySlipScreen(viewModel: DashBoardViewModel, context: Context,navController: NavController) {
        val salarySlip by viewModel.salarySlip.collectAsState()
        var selectedCycle by remember { mutableStateOf("") }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = {
                        Text(
                            "Pay Slip",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                )
            }
        ) { paddingValues ->
            viewModel.fetchPayrollCycles(context)
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                CycleDropdownMenu(viewModel, context)

                when (salarySlip) {
                    is Resource.Loading -> {
                        if(viewModel.payrollCycles.value is Resource.Success){
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is Resource.Success -> {
                        val data = (salarySlip as Resource.Success).data.bean

                        // Employee Info Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = red
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Name: ${data.empName} (Marketing)",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Salary (CTC): ${data.ctc} / ${data.ctcType}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        // Attendance Summary Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Attendance Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    color =red,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                AttendanceRow("Present", data.presentDays.toInt())
                                AttendanceRow("Absent", data.absentDays.toInt())
                                AttendanceRow("Leaves", data.leaves.toInt())
                                AttendanceRow("WFH", data.wfhDays.toInt())
                                AttendanceRow("Holidays", data.holidays.toInt())
                                AttendanceRow("Week Off", data.weekOffs.toInt())
                                AttendanceRow("Half Day", data.halfDays.toInt())
                                AttendanceRow("Paid Leaves", data.paidLeaves.toInt())
                                AttendanceRow("Total Days", data.totalMarkDays.toInt())
                            }
                        }

                        // Additions/Deductions Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Additions / Deductions\nAmount",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = red
                                    )
                                    Text(
                                        "Remarks",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = red
                                    )
                                }

                                data.adjustDTOs.forEach { adjustment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${adjustment.transType} ₹ ${adjustment.amount}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            adjustment.remark,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        // Salary Details Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(red, shape = RoundedCornerShape(5.dp))
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Category",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        "Value",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }

                                SalaryRow("Payable Days", data.payableDays.toInt().toString())
                                SalaryRow("Per Day Salary", "₹ ${String.format("%.2f", data.perDaySalary)}")
                                SalaryRow("Total Salary", "₹ ${String.format("%.2f", data.totalSalary)}")
                                SalaryRow("Additions", "₹ ${String.format("%.2f", data.additions)}")
                                SalaryRow("Deductions", "₹ ${String.format("%.2f", data.deductions)}")
                                SalaryRow("Net Payable", "₹ ${String.format("%.2f", data.netPayable)}")
                                SalaryRow("Paid", "₹ ${String.format("%.2f", data.paid)}")
                                SalaryRow("Balance", "₹ ${String.format("%.2f", data.balance)}")
                            }
                        }
                    }

                    is Resource.Error -> {
                        Text(
                            (salarySlip as Resource.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AttendanceRow(label: String, value: Int) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun SalaryRow(category: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}