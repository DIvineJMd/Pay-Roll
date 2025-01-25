package com.example.payroll.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.payroll.R


@Composable
fun CustomBottomBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    val insets = WindowInsets.systemBars.asPaddingValues()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = insets.calculateBottomPadding() + 8.dp ),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFFDC2626)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home button
            AnimatedButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = "HOME",
                selected = selectedItem == 0,
                onClick = { onItemSelected(0) }
            )

            // Grid button
            AnimatedButton(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.grid),
                        contentDescription = "Grid",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = "Dashboard",
                selected = selectedItem == 1,
                onClick = { onItemSelected(1) }
            )

            // Calendar button //
            AnimatedButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = "CALENDAR",
                selected = selectedItem == 2,
                onClick = { onItemSelected(2) }
            )
        }
    }
}

@Composable
private fun AnimatedButton(
    icon: @Composable () -> Unit,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val transition = updateTransition(selected, label = "selected")

    val buttonColor by transition.animateColor(label = "backgroundColor") { isSelected ->
        if (isSelected) Color(0xFFB91C1C) else Color.Transparent
    }

    val textWidth by transition.animateFloat(label = "textWidth") { isSelected ->
        if (isSelected) 1f else 0f
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(
            horizontal = if (selected) 16.dp else 12.dp,
            vertical = 8.dp
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = null,
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.wrapContentWidth()
        ) {
            icon()
            if (textWidth > 0f) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.graphicsLayer {
                        alpha = textWidth
                    }
                )
            }
        }
    }
}

@Composable
fun PunchInCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    InPunch: Boolean
) {
    Box(
        modifier = modifier
            .size(240.dp)  // Decreased from 270.dp
            .clip(CircleShape)
            .background(Color.White)
            .border(
                width = 30.dp,  // Decreased from 34.dp
                color = if (isEnabled) Color(0xFFE1E5E9) else Color.Gray,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer gradient ring
        Box(
            modifier = Modifier
                .size(180.dp)  // Decreased from 203.dp
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE1E5E9),
                            Color(0xFFF5F7F9)
                        ),
                        center = Offset.Zero,
                        radius = 216f  // Decreased from 243f
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Inner white circle with content
            Box(
                modifier = Modifier
                    .size(156.dp)  // Decreased from 176.dp
                    .background(Color.White, CircleShape)
                    .clip(CircleShape)
                    .clickable(enabled = isEnabled, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(5.dp)  // Decreased from 6.dp
                ) {
                    Icon(
                        painter = painterResource(R.drawable.punchin),
                        contentDescription = "Punch In",
                        modifier = Modifier.size(48.dp),  // Decreased from 54.dp
                        tint = if (InPunch) Color(0xFF4CAF50) else Color(0xFFB91C1C)
                    )
                    Spacer(modifier = Modifier.height(8.dp))  // Decreased from 10.dp
                    Text(
                        text = if (InPunch) "PUNCH IN" else "PUNCH OUT",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,  // Decreased from 15.sp
                            letterSpacing = 0.6.sp  // Decreased from 0.7.sp
                        ),
                        color = Color.Black
                    )
                }
            }
        }
    }
}
