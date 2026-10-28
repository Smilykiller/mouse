package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class AirMotionSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val hasGyroscope = gyroscope != null

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled = _isEnabled.asStateFlow()

    private val _isCalibrated = MutableStateFlow(false)
    val isCalibrated = _isCalibrated.asStateFlow()

    private val _tiltX = MutableStateFlow(0f)
    val tiltX = _tiltX.asStateFlow()

    private val _tiltY = MutableStateFlow(0f)
    val tiltY = _tiltY.asStateFlow()

    var onMotionDelta: ((dx: Float, dy: Float) -> Unit)? = null

    // Deadzone to prevent resting hand tremor
    var deadzone: Float = if (hasGyroscope) 0.08f else 0.45f
    var motionSensitivity: Float = 2.8f

    // Resting baseline for accelerometer fallback
    private var baselineAccelX: Float = 0f
    private var baselineAccelY: Float = 0f

    fun start() {
        if (_isEnabled.value) return

        val registered = if (hasGyroscope) {
            sensorManager?.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME) ?: false
        } else {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME) ?: false
        }

        if (registered) {
            _isEnabled.value = true
        }
    }

    fun stop() {
        if (!_isEnabled.value) return
        sensorManager?.unregisterListener(this)
        _isEnabled.value = false
    }

    fun toggle(): Boolean {
        if (_isEnabled.value) {
            stop()
        } else {
            start()
        }
        return _isEnabled.value
    }

    fun calibrateCenter() {
        _isCalibrated.value = true
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_isEnabled.value) return

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // Gyroscope outputs angular velocity in rad/s (True Air-Mouse behavior)
            // event.values[0]: rotation around X (pitch: tilting phone up/down)
            // event.values[1]: rotation around Y (roll: tilting phone left/right)
            // event.values[2]: rotation around Z (yaw: pointing phone left/right)
            val gyroYawZ = event.values[2]   // left/right aiming
            val gyroPitchX = event.values[0]  // up/down aiming

            val deltaX = if (abs(gyroYawZ) > deadzone) {
                // Waving left/right -> cursor moves left/right
                -gyroYawZ * motionSensitivity * 18f
            } else 0f

            val deltaY = if (abs(gyroPitchX) > deadzone) {
                // Tilting up/down -> cursor moves up/down
                -gyroPitchX * motionSensitivity * 18f
            } else 0f

            _tiltX.value = deltaX
            _tiltY.value = deltaY

            if (deltaX != 0f || deltaY != 0f) {
                onMotionDelta?.invoke(deltaX, deltaY)
            }
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && !hasGyroscope) {
            // Fallback: Calibrated accelerometer with resting bias removal
            val rawX = event.values[0]
            val rawY = event.values[1]

            if (!_isCalibrated.value) {
                baselineAccelX = rawX
                baselineAccelY = rawY
                _isCalibrated.value = true
            }

            val diffX = rawX - baselineAccelX
            val diffY = rawY - baselineAccelY

            val deltaX = if (abs(diffX) > deadzone) {
                -(diffX - (if (diffX > 0) deadzone else -deadzone)) * motionSensitivity * 1.5f
            } else 0f

            val deltaY = if (abs(diffY) > deadzone) {
                -(diffY - (if (diffY > 0) deadzone else -deadzone)) * motionSensitivity * 1.5f
            } else 0f

            _tiltX.value = deltaX
            _tiltY.value = deltaY

            if (deltaX != 0f || deltaY != 0f) {
                onMotionDelta?.invoke(deltaX, deltaY)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
