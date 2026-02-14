package io.github.martinschneider.baiyue.ui.screen.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.martinschneider.baiyue.ui.screen.detail.smoothPathSegments
import io.github.martinschneider.baiyue.ui.theme.BaiyuePrimary
import io.github.martinschneider.baiyue.ui.theme.XiaobaiyuePrimary

private val OldXiaobaiyueChartColor = Color(0xFF9E9E9E)
private val GridColor = Color.LightGray.copy(alpha = 0.5f)
private val LabelStyle = TextStyle(fontSize = 10.sp, color = Color.Gray)

@Composable
fun ProgressTimelineChart(
    timeline: ProgressTimeline,
    showBaiyue: Boolean,
    showXiaobaiyue: Boolean,
    showOldXiaobaiyue: Boolean,
) {
    if (timeline.points.isEmpty()) return
    if (!showBaiyue && !showXiaobaiyue && !showOldXiaobaiyue) return

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val paddingStart = 36.dp.toPx()
        val paddingTop = 8.dp.toPx()
        val paddingEnd = 8.dp.toPx()
        val paddingBottom = 24.dp.toPx()

        val chartWidth = size.width - paddingStart - paddingEnd
        val chartHeight = size.height - paddingTop - paddingBottom
        val maxY = 100f

        // Convert date strings (YYYY-MM-DD) to day offsets for proportional x-axis
        fun dateToDays(date: String): Int {
            val y = date.substring(0, 4).toIntOrNull() ?: 0
            val m = date.substring(5, 7).toIntOrNull() ?: 1
            val d = date.substring(8, 10).toIntOrNull() ?: 1
            return y * 365 + (y / 4) - (y / 100) + (y / 400) + (m * 30) + d
        }

        val dayValues = timeline.points.map { dateToDays(it.date) }
        val minDay = dayValues.min()
        val maxDay = dayValues.max()
        val daySpan = (maxDay - minDay).coerceAtLeast(1)

        fun dataX(dayValue: Int): Float {
            val ratio = (dayValue - minDay).toFloat() / daySpan
            return paddingStart + ratio * chartWidth
        }

        fun dataY(value: Int): Float {
            val ratio = (value.toFloat() / maxY).coerceIn(0f, 1f)
            return paddingTop + (1f - ratio) * chartHeight
        }

        // Y-axis grid lines and labels
        for (value in listOf(0, 25, 50, 75, 100)) {
            val y = dataY(value)
            drawLine(
                color = GridColor,
                start = Offset(paddingStart, y),
                end = Offset(size.width - paddingEnd, y),
                strokeWidth = 1f
            )
            val label = textMeasurer.measure(value.toString(), LabelStyle)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(
                    paddingStart - label.size.width - 4.dp.toPx(),
                    y - label.size.height / 2f
                )
            )
        }

        // X-axis year labels (only within data range)
        val firstYear = timeline.points.first().date.take(4).toInt()
        val lastYear = timeline.points.last().date.take(4).toInt()
        for (year in firstYear..lastYear) {
            val yearDay = dateToDays("$year-01-01")
            if (yearDay < minDay || yearDay > maxDay) continue
            val x = dataX(yearDay)
            val label = textMeasurer.measure(year.toString(), LabelStyle)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(
                    x - label.size.width / 2f,
                    size.height - paddingBottom + 4.dp.toPx()
                )
            )
        }

        // Line series
        val series = buildList {
            if (showBaiyue) add(timeline.points.map { it.baiyue } to BaiyuePrimary)
            if (showXiaobaiyue) add(timeline.points.map { it.xiaobaiyue } to XiaobaiyuePrimary)
            if (showOldXiaobaiyue) add(timeline.points.map { it.oldXiaobaiyue } to OldXiaobaiyueChartColor)
        }

        val lineStroke = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        for ((values, color) in series) {
            val path = Path()
            val xs = FloatArray(values.size) { i -> dataX(dayValues[i]) }
            val ys = FloatArray(values.size) { i -> dataY(values[i]) }
            if (xs.isNotEmpty()) {
                path.moveTo(xs[0], ys[0])
                smoothPathSegments(path, Path(), xs, ys)
            }
            drawPath(path, color, style = lineStroke)
        }
    }
}
