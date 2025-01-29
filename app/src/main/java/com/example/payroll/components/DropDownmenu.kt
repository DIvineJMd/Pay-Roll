package com.example.payroll.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.payroll.data.DashBoardViewModel
import com.example.payroll.data.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleDropdownMenu(viewModel: DashBoardViewModel,context: Context) {
    val payrollCycles by viewModel.payrollCycles.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var selectedCycle by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        when (payrollCycles) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is Resource.Success -> {
                val cycleList = (payrollCycles as Resource.Success<List<String>>).data
                if (cycleList.isNotEmpty()) {
                    Box {
                        OutlinedTextField(
                            value = selectedCycle.ifEmpty { "Select Cycle" },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth(),
                            label = { Text("Payroll Cycle") },
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(
                                        imageVector = if (expanded)
                                            Icons.Filled.KeyboardArrowUp
                                        else
                                            Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Toggle dropdown"
                                    )
                                }
                            },
                            colors = TextFieldDefaults.textFieldColors(

                                cursorColor = Color(0xFFDC2626),
                                focusedLabelColor = Color(0xFFDC2626)
                            )
                        )

                        // Transparent clickable overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expanded = true }
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f).background(color = Color.White) ,

                        ) {
                            cycleList.forEach { cycle ->
                                DropdownMenuItem(
                                    text = { Text(cycle) },
                                    onClick = {
                                        selectedCycle = cycle
                                        expanded = false
                                        viewModel.fetchSalarySlip(context, selectedCycle)
                                        }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "No cycles available",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is Resource.Error -> {
                Text(
                    "Error: ${(payrollCycles as Resource.Error).message}",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}