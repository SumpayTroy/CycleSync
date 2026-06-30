package com.bsit.cyclesync.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LineChartXY(
    xValues: List<Float>,
    yValues: List<Float>,
    chartColor: Color,
    modifier: Modifier = Modifier
) {
    if (xValues.size < 2 || yValues.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFFFAFAFA)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("Not enough data", textAlign = TextAlign.Center)
        }
        return
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color(0xFFFAFAFA))
    ) {
        val widthPx = size.width
        val heightPx = size.height

        val maxPoints = 600
        val stride = (xValues.size / maxPoints).coerceAtLeast(1)
        val xs = xValues.filterIndexed { i, _ -> i % stride == 0 }
        val ys = yValues.filterIndexed { i, _ -> i % stride == 0 }

        val minX = xs.minOrNull() ?: 0f
        val maxX = xs.maxOrNull() ?: 1f
        val minY = ys.minOrNull() ?: 0f
        val maxY = ys.maxOrNull() ?: 1f
        val rangeX = (maxX - minX).let { if (it <= 0f) 1f else it }
        val rangeY = (maxY - minY).let { if (it <= 0f) 1f else it }

        val path = Path()
        xs.forEachIndexed { i, xv ->
            val yv = ys[i]
            val px = ((xv - minX) / rangeX) * widthPx
            val py = heightPx - ((yv - minY) / rangeY) * heightPx
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }

        drawPath(path = path, color = chartColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
    }
}