package com.example.payroll.UIData

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.payroll.R
import com.example.payroll.components.DashBoardCard

@Composable

fun DashboardScreen(
    modifier: Modifier = Modifier
) {

    LazyVerticalGrid(
        modifier = modifier.padding(),
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashBoardCard(
                modifier = Modifier,
                onClick = { /*TODO*/ },
                name = "Attendance",
                id = R.drawable.attendance
            )
        }
        item {
            DashBoardCard(
                modifier = Modifier,
                onClick = { /*TODO*/ },
                name = "Profile",
                id = R.drawable.user
            )
        }
        item {
            DashBoardCard(
                modifier = Modifier,
                onClick = { /*TODO*/ },
                name = "Leave Request",
                id = R.drawable.leaverequest
            )
        }
        item {
            DashBoardCard(
                modifier = Modifier,
                onClick = { /*TODO*/ },
                name = "Leave History",
                id = R.drawable.leavehistory
            )
        }
        item {
            DashBoardCard(
                modifier = Modifier,
                onClick = { /*TODO*/ },
                name = "Holidays",
                id = R.drawable.holidays
            )
        }
        item {
            DashBoardCard(
                modifier = Modifier,
                onClick = { /*TODO*/ },
                name = "Calendar",
                id = R.drawable.calnder
            )
        }
    }


}

@Composable
@Preview
fun Preview_DashboardScreen() {
    DashboardScreen(Modifier)
}