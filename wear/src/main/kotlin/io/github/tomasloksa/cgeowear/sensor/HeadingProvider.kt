package io.github.tomasloksa.cgeowear.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Device azimuth in degrees and whether the compass is calibrated. */
data class Heading(
    val azimuthDeg: Float,
    val calibrated: Boolean = true,
)

/** Emits device heading from the rotation-vector sensor as a cold flow. */
class HeadingProvider(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val heading: Flow<Heading> = callbackFlow {
        var smoothSin = 0.0
        var smoothCos = 0.0
        var magAccuracy = if (magnetometer != null) {
            SensorManager.SENSOR_STATUS_UNRELIABLE
        } else {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        }

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_MAGNETIC_FIELD -> return
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val azimuthRad = orientation[0].toDouble()
                        smoothSin += RESPONSIVENESS * (sin(azimuthRad) - smoothSin)
                        smoothCos += RESPONSIVENESS * (cos(azimuthRad) - smoothCos)
                        val smoothedDeg = Math.toDegrees(atan2(smoothSin, smoothCos))
                        trySend(
                            Heading(
                                azimuthDeg = normalize(smoothedDeg.toFloat()),
                                calibrated = magAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM,
                            ),
                        )
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    magAccuracy = accuracy
                    Log.d(TAG, "magnetometer accuracy -> ${accuracyName(accuracy)} ($accuracy)")
                }
            }
        }

        Log.d(
            TAG,
            "starting: rotationSensor=${rotationSensor != null}, magnetometer=${magnetometer != null}",
        )
        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }
        if (magnetometer != null) {
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun normalize(deg: Float): Float = ((deg % 360f) + 360f) % 360f

    private fun accuracyName(accuracy: Int): String = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "HIGH"
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM"
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW"
        SensorManager.SENSOR_STATUS_UNRELIABLE -> "UNRELIABLE"
        SensorManager.SENSOR_STATUS_NO_CONTACT -> "NO_CONTACT"
        else -> "UNKNOWN($accuracy)"
    }

    private companion object {
        const val TAG = "HeadingProvider"
        const val RESPONSIVENESS = 0.4
    }
}
