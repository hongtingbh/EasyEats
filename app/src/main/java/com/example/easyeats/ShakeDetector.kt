package com.example.easyeats

import kotlin.math.sqrt
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.easyeats.R

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
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {  }

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
                        playShakeSound() // <-- PLAY SOUND HERE
                        onShake.invoke() // Calls the lambda passed from the Composable
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }
        }
    }

    private val soundPool: SoundPool // The SoundPool instance
    private var soundId: Int = 0      // The ID of the loaded sound

    init {
        // Configure AudioAttributes for UI/Game sounds
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Initialize SoundPool
        soundPool = SoundPool.Builder()
            .setMaxStreams(1) // We only need to play one sound at a time
            .setAudioAttributes(audioAttributes)
            .build()

        // Load the sound file and store its ID
        soundId = soundPool.load(context, R.raw.shake, 1)
    }

    // New function to play the sound effect
    private fun playShakeSound() {
        if (soundId != 0) {
            // Play the sound (soundId, leftVolume, rightVolume, priority, loop, rate)
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    // Release resources when done
    fun release() {
        soundPool.release()
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