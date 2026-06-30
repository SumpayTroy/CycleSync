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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CircularSpeedDisplay(
    modifier: Modifier = Modifier,
    speed: Float, // The current speed value
    speedUnit: String = "km/h",
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface, // Or surfaceVariant
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    speedTextColor: Color = MaterialTheme.colorScheme.primary,
    speedTextSize: TextUnit = 32.sp,
    unitTextSize: TextUnit = 14.sp,
    maxSpeedForGauge: Float? = null, // Optional: if you want a progress-like ring
    currentSpeedGaugeColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .padding(strokeWidth / 2) // Ensure stroke is not clipped
    ) {
        // Background Circle
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = backgroundColor,
                radius = (this.size.minDimension / 2f)
            )

            // Optional: Outer border stroke (if you want a distinct border)
            drawCircle(
                color = contentColor.copy(alpha = 0.3f), // A subtle border
                radius = (this.size.minDimension / 2f) - (strokeWidth.toPx() / 2f),
                style = Stroke(width = strokeWidth.toPx() / 2) // Thin border
            )

            // Optional: Speed Gauge Arc (Progress-like ring)
            maxSpeedForGauge?.let { maxSpeed ->
                if (maxSpeed > 0) {
                    val sweepAngle = (speed.coerceIn(0f, maxSpeed) / maxSpeed) * 360f
                    drawArc(
                        color = currentSpeedGaugeColor,
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

        // Speed Text Content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.0f".format(speed), // Display speed as an integer
                color = speedTextColor,
                fontSize = speedTextSize,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = speedUnit,
                color = contentColor,
                fontSize = unitTextSize
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun CircularSpeedDisplayPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CircularSpeedDisplay(
                speed = 60.7f,
                speedUnit = "mph",
                maxSpeedForGauge = 120f // Example with gauge
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF808080) // Darker background
@Composable
fun CircularSpeedDisplayNoGaugePreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CircularSpeedDisplay(
                speed = 125f,
                size = 100.dp,
                backgroundColor = Color.DarkGray,
                contentColor = Color.White,
                speedTextColor = Color.Cyan
            )
        }
    }
}
