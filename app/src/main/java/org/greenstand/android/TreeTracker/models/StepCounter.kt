package org.greenstand.android.TreeTracker.models

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import org.greenstand.android.TreeTracker.preferences.Preferences
import timber.log.Timber

class StepCounter(
    private val sensorManager: SensorManager,
    private val preferences: Preferences
) {

    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepCountEventListener = StepCountEventListener()
    private var enabled = false

    var absoluteStepCount: Int?
        get() = preferences.getInt(User.ABS_STEP_COUNT)
        private set(value) = preferences.edit().putInt(User.ABS_STEP_COUNT, value ?: 0).apply()

    var absoluteStepCountOnTreeCapture: Int?
        get() = preferences.getInt(User.ABS_STEP_COUNT_ON_TREE_CAPTURE)
        set(value) = preferences
            .edit().putInt(User.ABS_STEP_COUNT_ON_TREE_CAPTURE, value ?: 0).apply()

    val viewLifecycleObserver: LifecycleObserver = object : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun unregisterListener() {
            Timber.d("StepCounter: unregister listener")
            sensorManager.unregisterListener(stepCountEventListener)
        }
    }

    fun enable() {
        Timber.d("StepCounter: enable - register listener")
        sensorManager.registerListener(
            stepCountEventListener, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun disable() {
        Timber.d("StepCounter: disable - unregister listener")
        sensorManager.unregisterListener(stepCountEventListener)
    }

    inner class StepCountEventListener : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Ignore
        }

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                absoluteStepCount = it.values[0].toInt()
                Timber.d("StepCounter: Step count [${it.values[0].toInt()}]")
            }
        }
    }
}
