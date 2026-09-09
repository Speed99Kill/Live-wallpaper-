package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

data class MotionData(
    val roll: Float = 0f,   // -1.0 to 1.0 (X tilt)
    val pitch: Float = 0f,  // -1.0 to 1.0 (Y tilt)
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val isHardwareAvailable: Boolean = true,
    val isSimulated: Boolean = false
)

class MotionSensorTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private val _motionFlow = MutableStateFlow(MotionData(isHardwareAvailable = accelerometer != null))
    val motionFlow: StateFlow<MotionData> = _motionFlow.asStateFlow()

    private var smoothedRoll = 0f
    private var smoothedPitch = 0f
    private var smoothedAccelX = 0f
    private var smoothedAccelY = 0f

    private var sensitivity: Float = 1.2f
    private var invert: Boolean = false
    private var isListening = false

    var simulatedRoll: Float = 0f
        set(value) {
            field = value.coerceIn(-1.5f, 1.5f)
            updateSimulatedData()
        }

    var simulatedPitch: Float = 0f
        set(value) {
            field = value.coerceIn(-1.5f, 1.5f)
            updateSimulatedData()
        }

    var isSimulationActive: Boolean = false
        set(value) {
            field = value
            if (value) {
                updateSimulatedData()
            }
        }

    fun configure(sensitivityMultiplier: Float, invertMotion: Boolean) {
        this.sensitivity = sensitivityMultiplier
        this.invert = invertMotion
    }

    fun startListening() {
        if (isListening || sensorManager == null) return
        val sensorToUse = gravitySensor ?: accelerometer
        if (sensorToUse != null) {
            sensorManager.registerListener(this, sensorToUse, SensorManager.SENSOR_DELAY_GAME)
            isListening = true
        }
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isSimulationActive || event == null) return

        val rawX = event.values[0]
        val rawY = event.values[1]
        val rawZ = event.values[2]

        // Normal phone orientation: X goes right (-10 to 10), Y goes up (-10 to 10), Z goes out of screen
        val targetRoll = (-rawX / 9.81f) * (if (invert) -1f else 1f) * sensitivity
        val targetPitch = (rawY / 9.81f) * (if (invert) -1f else 1f) * sensitivity

        // Low-pass exponential smoothing filter for liquid-smooth physics (alpha = 0.15)
        val alpha = 0.18f
        smoothedRoll += (targetRoll - smoothedRoll) * alpha
        smoothedPitch += (targetPitch - smoothedPitch) * alpha

        smoothedAccelX = -rawX
        smoothedAccelY = rawY

        _motionFlow.value = MotionData(
            roll = smoothedRoll.coerceIn(-1.5f, 1.5f),
            pitch = smoothedPitch.coerceIn(-1.5f, 1.5f),
            accelX = smoothedAccelX,
            accelY = smoothedAccelY,
            isHardwareAvailable = true,
            isSimulated = false
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun updateSimulatedData() {
        _motionFlow.value = MotionData(
            roll = simulatedRoll * (if (invert) -1f else 1f) * sensitivity,
            pitch = simulatedPitch * (if (invert) -1f else 1f) * sensitivity,
            accelX = simulatedRoll * 5f,
            accelY = simulatedPitch * 5f,
            isHardwareAvailable = accelerometer != null,
            isSimulated = true
        )
    }

    // Auto-drift helper for ambient testing
    fun pulseSimulation(timeSec: Float) {
        if (isSimulationActive) {
            simulatedRoll = (sin(timeSec * 0.8) * 0.6).toFloat()
            simulatedPitch = (sin(timeSec * 0.6) * 0.5).toFloat()
        }
    }
}
