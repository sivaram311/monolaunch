package buzz.delena.monolaunch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Minimalist white-outline analog clock, hand-drawn on [Canvas]. No dial
 * face fill, no shadows/blur — flat strokes only, cheap to redraw every
 * second at 120Hz.
 */
@Composable
fun AnalogClock(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 120.dp) {
    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000L - (value % 1000L))
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val radius = min(this.size.width, this.size.height) / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Dial rim.
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )

        // Hour ticks (12 marks, thicker every 3 hours).
        for (tick in 0 until 12) {
            val angle = Math.toRadians((tick * 30 - 90).toDouble())
            val outer = Offset(
                center.x + radius * cos(angle).toFloat(),
                center.y + radius * sin(angle).toFloat(),
            )
            val inner = Offset(
                center.x + (radius - if (tick % 3 == 0) 10.dp.toPx() else 5.dp.toPx()) * cos(angle).toFloat(),
                center.y + (radius - if (tick % 3 == 0) 10.dp.toPx() else 5.dp.toPx()) * sin(angle).toFloat(),
            )
            drawLine(
                color = Color.White,
                start = inner,
                end = outer,
                strokeWidth = if (tick % 3 == 0) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        fun handEnd(unitsElapsed: Float, unitsTotal: Float, length: Float): Offset {
            val angle = Math.toRadians((unitsElapsed / unitsTotal * 360 - 90).toDouble())
            return Offset(
                center.x + length * cos(angle).toFloat(),
                center.y + length * sin(angle).toFloat(),
            )
        }

        val hourUnits = (hour % 12) + minute / 60f
        drawLine(
            color = Color.White,
            start = center,
            end = handEnd(hourUnits, 12f, radius * 0.5f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White,
            start = center,
            end = handEnd(minute.toFloat(), 60f, radius * 0.75f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.7f),
            start = center,
            end = handEnd(second.toFloat(), 60f, radius * 0.85f),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
    }
}
