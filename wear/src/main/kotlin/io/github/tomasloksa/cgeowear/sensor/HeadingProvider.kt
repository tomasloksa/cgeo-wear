package io.github.tomasloksa.cgeowear.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/** Device azimuth (deg clockwise from true-ish north) and whether it is simulated. */
data class Heading(val azimuthDeg: Float, val simulated: Boolean)

/**
 * Wraps TYPE_ROTATION_VECTOR as a cold flow. The listener is registered only
 * while the flow is collected — collect it lifecycle-aware and the sensor is
 * released the moment the screen is not interactive (battery rule: never leave
 * sensor listeners running).
 *
 * Emulator fallback: if no sensor exists or no event arrives within
 * [SENSOR_TIMEOUT_MS], emits a slow synthetic sweep flagged [Heading.simulated]
 * so the demo visibly rotates on emulators without IMU support.
 */
class HeadingProvider(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val heading: Flow<Heading> = callbackFlow {
        var gotRealEvent = false
        // Low-pass smoothing on the unit circle to avoid the 359->0 jump.
        var smoothSin = 0.0
        var smoothCos = 0.0

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                gotRealEvent = true
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val azimuthRad = orientation[0].toDouble()
                smoothSin += SMOOTHING * (sin(azimuthRad) - smoothSin)
                smoothCos += SMOOTHING * (cos(azimuthRad) - smoothCos)
                val smoothedDeg = Math.toDegrees(atan2(smoothSin, smoothCos))
                trySend(Heading(normalize(smoothedDeg.toFloat()), simulated = false))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        val fallback = launch {
            delay(SENSOR_TIMEOUT_MS)
            var azimuth = 0f
            while (!gotRealEvent) {
                trySend(Heading(normalize(azimuth), simulated = true))
                azimuth += SIM_SWEEP_DEG_PER_TICK
                delay(SIM_TICK_MS)
            }
        }

        awaitClose {
            fallback.cancel()
            sensorManager.unregisterListener(listener)
        }
    }

    private fun normalize(deg: Float): Float = ((deg % 360f) + 360f) % 360f

    private companion object {
        const val SMOOTHING = 0.25
        const val SENSOR_TIMEOUT_MS = 3_000L
        const val SIM_TICK_MS = 100L
        const val SIM_SWEEP_DEG_PER_TICK = 1.5f // one full turn per 24 s
    }
}
