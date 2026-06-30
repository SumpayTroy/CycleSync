package com.bsit.cyclesync.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset // Keep for potential gauge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap // Keep for potential gauge
import androidx.compose.ui.graphics.drawscope.Stroke // Keep for potential gauge
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CircularDistanceDisplay(
    modifier: Modifier = Modifier,
    distance: Float, // The current distance value
    distanceUnit: String = "km",
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp, // For potential gauge/border
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    distanceTextColor: Color = MaterialTheme.colorScheme.primary,
    distanceTextSize: TextUnit = 26.sp,
    unitTextSize: TextUnit = 14.sp,
    // Optional: if you want a progress-like ring for a target distance
    targetDistanceForGauge: Float? = null,
    currentDistanceGaugeColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .padding(strokeWidth / 2) // Ensure stroke is not clipped if gauge is used
    ) {
        // Background Circle
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = backgroundColor,
                radius = (this.size.minDimension / 2f)
            )

            // Optional: Outer border stroke
            drawCircle(
                color = contentColor.copy(alpha = 0.3f),
                radius = (this.size.minDimension / 2f) - (strokeWidth.toPx() / 2f), // Thinner border
                style = Stroke(width = strokeWidth.toPx() / 2f)
            )

            // Optional: Distance Gauge Arc (Progress towards a target)
            targetDistanceForGauge?.let { targetDistance ->
                if (targetDistance > 0) {
                    val sweepAngle = (distance.coerceIn(0f, targetDistance) / targetDistance) * 360f
                    drawArc(
                        color = currentDistanceGaugeColor,
                        startAngle = -90f, // Start from the top
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(strokeWidth.toPx() / 2f, strokeWidth.toPx() / 2f),
                        size = this.size.copy(
                            width = this.size.width - strokeWidth.toPx(),
                            height = this.size.height - strokeWidth.toPx()
                        )
                    )
                }
            }
        }

        // Distance Text Content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.2f".format(distance), // Display distance, e.g., to 2 decimal places
                color = distanceTextColor,
                fontSize = distanceTextSize,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = distanceUnit,
                color = contentColor,
                fontSize = unitTextSize
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun CircularDistanceDisplayPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CircularDistanceDisplay(
                distance = 12.345f,
                distanceUnit = "km",
                targetDistanceForGauge = 20f // Example with gauge
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF808080)
@Composable
fun CircularDistanceDisplayNoGaugePreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CircularDistanceDisplay(
                distance = 5.7f,
                distanceUnit = "meters",
                size = 100.dp,
                backgroundColor = Color.DarkGray,
                contentColor = Color.White,
                distanceTextColor = Color.Cyan
            )
        }
    }
}
