package io.github.tomasloksa.cgeowear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import io.github.tomasloksa.cgeowear.common.NavState
import io.github.tomasloksa.cgeowear.sensor.Heading
import java.util.Locale
import kotlin.math.min

private val RingColor = Color(0xFF444444)
private val NorthColor = Color(0xFFCC4444)
private val ArrowColor = Color(0xFF6DD58C)
private val CalibrateColor = Color(0xFFE0A030)

/**
 * The whole v1 screen: compass ring rotating with the wearer's heading,
 * an arrow pointing at the cache, distance in the middle.
 */
@Composable
fun compassScreen(state: NavState?, heading: Heading?) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (state == null) {
                Text("Waiting for target…", color = Color.Gray)
            } else {
                val azimuth = heading?.azimuthDeg ?: 0f
                val needsCalibration = heading?.calibrated == false
                CompassRing(
                    azimuthDeg = azimuth,
                    targetBearingDeg = state.tick.bearingDeg,
                    showArrow = !needsCalibration,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatDistance(state.tick.distanceMeters),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = state.target.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Text(
                        text = state.target.geocode,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                    if (needsCalibration) {
                        Text(
                            text = "Calibrate compass",
                            style = MaterialTheme.typography.bodySmall,
                            color = CalibrateColor,
                        )
                        Text(
                            text = "move wrist in a figure-8",
                            fontSize = 10.sp,
                            color = CalibrateColor,
                        )
                    }
                }
            }
            TimeText { time -> timeTextCurvedText(time) }
        }
    }
}

/** Ring + cardinal marks rotate with the device so marks point at real directions. */
@Composable
private fun CompassRing(azimuthDeg: Float, targetBearingDeg: Float, showArrow: Boolean = true) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = min(size.width, size.height) / 2f - 10.dp.toPx()

        rotate(degrees = -azimuthDeg) {
            drawCircle(
                color = RingColor,
                radius = radius,
                style = Stroke(width = 2.dp.toPx()),
            )
            // Tick marks every 30 deg; north tick highlighted.
            for (angle in 0 until 360 step 30) {
                val isNorth = angle == 0
                rotate(degrees = angle.toFloat()) {
                    drawLine(
                        color = if (isNorth) NorthColor else RingColor,
                        start = Offset(center.x, center.y - radius),
                        end = Offset(center.x, center.y - radius + (if (isNorth) 14 else 8).dp.toPx()),
                        strokeWidth = (if (isNorth) 4 else 2).dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        // Arrow to the target, relative to where the wearer is facing.
        // Hidden while the compass is uncalibrated — a wrong arrow is worse than none.
        if (showArrow) {
            rotate(degrees = targetBearingDeg - azimuthDeg) {
                drawTargetArrow(radius)
            }
        }
    }
}

private fun DrawScope.drawTargetArrow(radius: Float) {
    val tipY = center.y - radius + 4.dp.toPx()
    val arrow = Path().apply {
        moveTo(center.x, tipY)
        lineTo(center.x - 9.dp.toPx(), tipY + 20.dp.toPx())
        lineTo(center.x, tipY + 14.dp.toPx())
        lineTo(center.x + 9.dp.toPx(), tipY + 20.dp.toPx())
        close()
    }
    drawPath(arrow, ArrowColor)
}

private fun formatDistance(meters: Double): String = when {
    meters >= 10_000 -> String.format(Locale.getDefault(), "%.1f km", meters / 1000)
    meters >= 1_000 -> String.format(Locale.getDefault(), "%.2f km", meters / 1000)
    else -> String.format(Locale.getDefault(), "%.0f m", meters)
}
