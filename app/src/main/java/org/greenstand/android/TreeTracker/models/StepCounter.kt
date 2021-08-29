package org.greenstand.android.TreeTracker.models

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import timber.log.Timber

class StepCounter(
    private val sensorManager: SensorManager,
    private val preferences: Preferences
) : LifecycleObserver {

    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepCountEventListener = StepCountEventListener()

    var absoluteStepCount: Int?
        get() = preferences.getInt(ABS_STEP_COUNT)
        private set(value) = preferences.edit().putInt(ABS_STEP_COUNT, value ?: 0).apply()

    var absoluteStepCountOnTreeCapture: Int?
        get() = preferences.getInt(ABS_STEP_COUNT_ON_TREE_CAPTURE)
        private set(value) = preferences
            .edit().putInt(ABS_STEP_COUNT_ON_TREE_CAPTURE, value ?: 0).apply()

    // Delta step count is the difference between the absolute count at the time of capturing
    // a tree minus the last absolute step count recorded when capturing a previous tree. This
    // is the indicator for the number of steps taken between two trees.
    val deltaSteps: Int
        get() = (absoluteStepCount ?: 0) - (absoluteStepCountOnTreeCapture ?: 0)

    fun enable() {
        Timber.d("StepCounter: enable - register listener")
        sensorManager.registerListener(
            stepCountEventListener, stepCounter, SensorManager.SENSOR_DELAY_FASTEST)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun disable() {
        Timber.d("StepCounter: disable - unregister listener")
        sensorManager.unregisterListener(stepCountEventListener)
    }

    fun snapshotAbsoluteStepCountOnTreeCapture() {
        absoluteStepCountOnTreeCapture = absoluteStepCount
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

    companion object {
        private val BASE_KEY = PrefKeys.SESSION + PrefKey("steps")
        private val ABS_STEP_COUNT = BASE_KEY + PrefKey("abs-step-count")
        private val ABS_STEP_COUNT_ON_TREE_CAPTURE = BASE_KEY +
                PrefKey("abs-step-count-on-tree-capture")
    }
}
