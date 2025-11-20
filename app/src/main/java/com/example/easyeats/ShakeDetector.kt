package com.example.easyeats

// Import the necessary math function
import kotlin.math.sqrt
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// ---------------------------------------------------------------------
// 1. EXTRACTED SHAKE DETECTOR CLASS
// ---------------------------------------------------------------------
class ShakeDetector(private val context: Context, private val onShake: () -> Unit) {
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val SHAKE_THRESHOLD = 3f
    private val SHAKE_TIME_INTERVAL = 500L

    private var lastShakeTime: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f

    private val sensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* Ignored */ }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val currentTime = System.currentTimeMillis()

                if ((currentTime - lastShakeTime) > SHAKE_TIME_INTERVAL) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val deltaX = x - lastX
                    val deltaY = y - lastY
                    val deltaZ = z - lastZ

                    val accelerationMagnitude = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

                    if (accelerationMagnitude > SHAKE_THRESHOLD) {
                        lastShakeTime = currentTime
                        onShake.invoke() // Calls the lambda passed from the Composable
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }
        }
    }

    fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(sensorEventListener)
    }
}