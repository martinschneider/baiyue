package io.github.martinschneider.baiyue.ui.screen.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Elevation profile chart drawn with Compose Canvas.
 * [track] is a list of [lat, lng, ele] triples.
 */
@Composable
fun ElevationChart(
    track: List<List<Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFFFF7043),
    fillColor: Color = Color(0x40FF7043),
) {
    if (track.size < 2 || track.any { it.size < 3 }) return

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    // Pre-compute cumulative distances and elevation range
    val profile = remember(track) { buildProfile(track) }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (profile.distances.isEmpty()) return@Canvas

        val totalDist = profile.distances.last()
        if (totalDist <= 0.0) return@Canvas

        val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

        // Layout: leave space for axis labels
        val leftPad = 44f
        val bottomPad = 20f
        val topPad = 8f
        val rightPad = 8f
        val plotW = size.width - leftPad - rightPad
        val plotH = size.height - topPad - bottomPad

        if (plotW <= 0 || plotH <= 0) return@Canvas

        // Elevation range with some padding, clamped to >= 0
        val eleMin = profile.eleMin
        val eleMax = profile.eleMax
        val eleRange = (eleMax - eleMin).coerceAtLeast(50.0)
        val paddedMin = (eleMin - eleRange * 0.05).coerceAtLeast(0.0)
        val paddedRange = eleMax + eleRange * 0.05 - paddedMin

        // Build path with monotone cubic smoothing
        val linePath = Path()
        val fillPath = Path()
        val baseY = topPad + plotH

        val xs = FloatArray(profile.distances.size) { i ->
            leftPad + (profile.distances[i] / totalDist).toFloat() * plotW
        }
        val ys = FloatArray(profile.distances.size) { i ->
            topPad + plotH - ((profile.elevations[i] - paddedMin) / paddedRange).toFloat() * plotH
        }

        if (xs.size >= 2) {
            linePath.moveTo(xs[0], ys[0])
            fillPath.moveTo(xs[0], baseY)
            fillPath.lineTo(xs[0], ys[0])
            smoothPathSegments(linePath, fillPath, xs, ys)
        }
        // Close fill path
        val lastX = leftPad + plotW
        fillPath.lineTo(lastX, baseY)
        fillPath.close()

        drawPath(fillPath, fillColor)
        drawPath(linePath, lineColor, style = Stroke(width = 2f))

        // Y-axis labels (elevation)
        drawElevationLabels(
            textMeasurer, labelStyle,
            eleMin = paddedMin, eleRange = paddedRange,
            leftPad = leftPad, topPad = topPad, plotH = plotH
        )

        // X-axis labels (distance)
        drawDistanceLabels(
            textMeasurer, labelStyle,
            totalDist = totalDist,
            leftPad = leftPad, topPad = topPad, plotW = plotW, plotH = plotH
        )
    }
}

private fun DrawScope.drawElevationLabels(
    textMeasurer: TextMeasurer,
    style: TextStyle,
    eleMin: Double,
    eleRange: Double,
    leftPad: Float,
    topPad: Float,
    plotH: Float,
) {
    val nTicks = 3
    for (i in 0..nTicks) {
        val frac = i.toDouble() / nTicks
        val ele = eleMin + frac * eleRange
        val y = topPad + plotH - frac.toFloat() * plotH
        val label = "${ele.toInt()} m"
        val result = textMeasurer.measure(label, style)
        drawText(
            result,
            topLeft = Offset(leftPad - result.size.width - 4f, y - result.size.height / 2f)
        )
    }
}

private fun DrawScope.drawDistanceLabels(
    textMeasurer: TextMeasurer,
    style: TextStyle,
    totalDist: Double,
    leftPad: Float,
    topPad: Float,
    plotW: Float,
    plotH: Float,
) {
    val nTicks = 3
    for (i in 0..nTicks) {
        val frac = i.toDouble() / nTicks
        val dist = frac * totalDist
        val x = leftPad + frac.toFloat() * plotW
        val label = if (dist < 1000) "${dist.toInt()} m" else "%.1f km".format(dist / 1000.0)
        val result = textMeasurer.measure(label, style)
        drawText(
            result,
            topLeft = Offset(x - result.size.width / 2f, topPad + plotH + 2f)
        )
    }
}

private data class ElevationProfile(
    val distances: List<Double>,
    val elevations: List<Double>,
    val eleMin: Double,
    val eleMax: Double,
)

private fun buildProfile(track: List<List<Double>>): ElevationProfile {
    val distances = mutableListOf(0.0)
    val elevations = mutableListOf(track[0][2])
    var cumDist = 0.0

    for (i in 1 until track.size) {
        val prev = track[i - 1]
        val curr = track[i]
        cumDist += haversineMeters(prev[0], prev[1], curr[0], curr[1])
        distances.add(cumDist)
        elevations.add(curr[2])
    }

    return ElevationProfile(
        distances = distances,
        elevations = elevations,
        eleMin = elevations.min(),
        eleMax = elevations.max(),
    )
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/**
 * Monotone cubic interpolation (Fritsch-Carlson) for smooth chart lines.
 * Adds cubic bezier segments to [linePath] and [fillPath] for points after the first.
 */
internal fun smoothPathSegments(linePath: Path, fillPath: Path, xs: FloatArray, ys: FloatArray) {
    val n = xs.size
    if (n < 2) return
    if (n == 2) {
        linePath.lineTo(xs[1], ys[1])
        fillPath.lineTo(xs[1], ys[1])
        return
    }

    // Compute tangents using Fritsch-Carlson monotone method
    val dx = FloatArray(n - 1) { i -> xs[i + 1] - xs[i] }
    val dy = FloatArray(n - 1) { i -> ys[i + 1] - ys[i] }
    val slopes = FloatArray(n - 1) { i -> if (dx[i] != 0f) dy[i] / dx[i] else 0f }

    val tangents = FloatArray(n)
    tangents[0] = slopes[0]
    tangents[n - 1] = slopes[n - 2]
    for (i in 1 until n - 1) {
        if (slopes[i - 1] * slopes[i] <= 0f) {
            tangents[i] = 0f
        } else {
            tangents[i] = (slopes[i - 1] + slopes[i]) / 2f
        }
    }

    // Ensure monotonicity
    for (i in 0 until n - 1) {
        if (slopes[i] == 0f) {
            tangents[i] = 0f
            tangents[i + 1] = 0f
        } else {
            val alpha = tangents[i] / slopes[i]
            val beta = tangents[i + 1] / slopes[i]
            val s = alpha * alpha + beta * beta
            if (s > 9f) {
                val t = 3f / sqrt(s)
                tangents[i] = t * alpha * slopes[i]
                tangents[i + 1] = t * beta * slopes[i]
            }
        }
    }

    // Build cubic bezier segments
    for (i in 0 until n - 1) {
        val segLen = dx[i] / 3f
        val cx1 = xs[i] + segLen
        val cy1 = ys[i] + tangents[i] * segLen
        val cx2 = xs[i + 1] - segLen
        val cy2 = ys[i + 1] - tangents[i + 1] * segLen
        linePath.cubicTo(cx1, cy1, cx2, cy2, xs[i + 1], ys[i + 1])
        fillPath.cubicTo(cx1, cy1, cx2, cy2, xs[i + 1], ys[i + 1])
    }
}
