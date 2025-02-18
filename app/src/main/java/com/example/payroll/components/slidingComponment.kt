package com.example.payroll.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.FractionalThreshold
import androidx.wear.compose.material.ResistanceConfig
import androidx.wear.compose.material.SwipeProgress
import androidx.wear.compose.material.SwipeableDefaults
import androidx.wear.compose.material.SwipeableState
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.example.payroll.R
import com.example.payroll.ui.theme.PayRollTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SlideToUnlock(
    isLoading: Boolean,
    onUnlockRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val swipeState = rememberSwipeableState(
        initialValue = if (isLoading) Anchor.End else Anchor.Start,
        confirmStateChange = { anchor ->
            if (anchor == Anchor.End) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onUnlockRequested()
            }
            true
        }
    )

    val swipeFraction by remember {
        derivedStateOf { calculateSwipeFraction(swipeState.progress) }
    }

    LaunchedEffect(isLoading) {
        swipeState.animateTo(if (isLoading) Anchor.End else Anchor.Start)
    }

    Track(
        swipeState = swipeState,
        swipeFraction = swipeFraction,
        enabled = !isLoading,
        modifier = modifier,
    ) {
        Hint(
            text = "Swipe to Punch Out",
            swipeFraction = swipeFraction,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(PaddingValues(horizontal = Thumb.Size + 8.dp)),
        )

        Thumb(
            isLoading = isLoading,
            modifier = Modifier.offset {
                IntOffset(swipeState.offset.value.roundToInt(), 0)
            },
        )
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
fun calculateSwipeFraction(progress: SwipeProgress<Anchor>): Float {
    return try {
        when {
            progress.from == progress.to ->
                if (progress.from == Anchor.Start) 0f else 1f
            progress.from == Anchor.Start ->
                progress.fraction.coerceIn(0f, 1f)
            else ->
                (1f - progress.fraction).coerceIn(0f, 1f)
        }
    } catch (e: Exception) {
        0f  // Safe default
    }
}
enum class Anchor { Start, End }

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun Track(
    swipeState: SwipeableState<Anchor>,
    swipeFraction: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (BoxScope.() -> Unit),
) {
    val density = LocalDensity.current
    var fullWidth by remember { mutableIntStateOf(0) }
    val horizontalPadding = 10.dp

    // Calculate track bounds
    val startOfTrackPx = with(density) { horizontalPadding.toPx() }
    val endOfTrackPx = with(density) {
        if (fullWidth > 0) {
            fullWidth - (horizontalPadding + Thumb.Size).toPx()
        } else {
            startOfTrackPx
        }
    }

    // Create anchors map with proper bounds checking
    val anchors = remember(fullWidth, startOfTrackPx, endOfTrackPx) {
        if (fullWidth > 0 && endOfTrackPx > startOfTrackPx) {
            val anchorPoints = mutableMapOf<Float, Anchor>()
            // Add start anchor
            anchorPoints[startOfTrackPx] = Anchor.Start
            // Add end anchor only if we have valid width
            if (endOfTrackPx > startOfTrackPx) {
                anchorPoints[endOfTrackPx] = Anchor.End
            }
            anchorPoints
        } else {
            mapOf(0f to Anchor.Start)  // Fallback anchors
        }
    }

    val backgroundColor by remember(swipeFraction) {
        derivedStateOf { calculateTrackColor(swipeFraction) }
    }

    // Convert resistance basis from Dp to pixels
    val resistanceBasis = with(density) { 100.dp.toPx() }

    Box(
        modifier = modifier
            .onSizeChanged { size ->
                fullWidth = size.width
            }
            .height(56.dp)
            .fillMaxWidth()
            .swipeable(
                enabled = enabled,
                state = swipeState,
                orientation = Orientation.Horizontal,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                velocityThreshold = Track.VelocityThreshold,
                resistance = ResistanceConfig(
                    basis = resistanceBasis,
                    factorAtMin = 0f,
                    factorAtMax = 1f
                )
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(percent = 50),
            )
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        content = content,
    )
}
val AlmostBlack = Color(0xFFDC2626)
val Yellow = Color(0xFFDC2626)
fun calculateTrackColor(swipeFraction: Float): Color {
    val endOfColorChangeFraction = 0.4f
    val fraction = (swipeFraction / endOfColorChangeFraction).coerceIn(0f..1f)
    return lerp(AlmostBlack, Yellow, fraction)
}

@Composable
fun Thumb(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Thumb.Size)
            .background(color = Color.White, shape = CircleShape)
            .padding(8.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(2.dp),
                color = Color.Black,
                strokeWidth = 2.dp
            )
        } else {
            Image(
                painter = painterResource(R.drawable.right),
                contentDescription = null,
            )
        }
    }
}

@Composable
fun Hint(
    text: String,
    swipeFraction: Float,
    modifier: Modifier = Modifier,
) {
    val hintTextColor by remember(swipeFraction) {
        derivedStateOf { calculateHintTextColor(swipeFraction) }
    }

    Text(
        text = text,
        color = hintTextColor,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier
    )
}

fun calculateHintTextColor(swipeFraction: Float): Color {
    val endOfFadeFraction = 0.35f
    val fraction = (swipeFraction / endOfFadeFraction).coerceIn(0f..1f)
    return lerp(Color.White, Color.White.copy(alpha = 0f), fraction)
}


private object Thumb {
    val Size = 40.dp
}

private object Track {
    @OptIn(ExperimentalWearMaterialApi::class)
    val VelocityThreshold = SwipeableDefaults.VelocityThreshold * 10
}

@OptIn(ExperimentalWearMaterialApi::class)
@Preview
@Composable
private fun Preview() {
    val previewBackgroundColor = Color(0xFFEDEDED)
    var isLoading by remember { mutableStateOf(false) }
    PayRollTheme {
        val spacing = 88.dp
        Column(
            verticalArrangement = spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(previewBackgroundColor)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(spacing))

            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Normal")
                    Spacer(modifier = Modifier.weight(1f))
                    Thumb(isLoading = false)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Loading")
                    Spacer(modifier = Modifier.widthIn(min = 16.dp))
                    Thumb(isLoading = true)
                }


            }

            Spacer(modifier = Modifier.height(spacing))

            Text(text = "Inactive")
            Track(
                swipeState = SwipeableState(Anchor.Start),
                swipeFraction = 0f,
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                content = {},
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Active")
            Track(
                swipeState = SwipeableState(Anchor.Start),
                swipeFraction = 1f,
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                content = {},
            )


            Spacer(modifier = Modifier.height(spacing))


            SlideToUnlock(
                isLoading = isLoading,
                onUnlockRequested = { isLoading = true },
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                colors = ButtonDefaults.outlinedButtonColors(),
                shape = RoundedCornerShape(percent = 50),
                onClick = { isLoading = false }) {
                Text(text = "Cancel loading", style = MaterialTheme.typography.labelMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

