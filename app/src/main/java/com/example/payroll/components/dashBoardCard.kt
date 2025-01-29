package com.example.payroll.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import com.example.payroll.R

@Composable
fun DashBoardCard(modifier: Modifier, onClick: () -> Unit, name: String,@DrawableRes id: Int) {
    Card(
        modifier = modifier,
        onClick = { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 10.dp,
            pressedElevation = 15.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = id),
                "",
                modifier = modifier.padding(horizontal = 32.dp, vertical = 15.dp)
            )
            Text(
                text = name,
                modifier = Modifier.padding(top = 10.dp),
                color = Color(0xFFDC2626),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .fillMaxWidth(0.30f)
                    .height(5.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFDC2626),
                                Color(0xFFD9BDBD)
                            )
                        ),
                        shape = RoundedCornerShape(5.dp)
                    )
            )
        }
    }
}

@Composable
@Preview
fun Preview_Card() {
    DashBoardCard(Modifier, {}, "Attendance",R.drawable.user)
}