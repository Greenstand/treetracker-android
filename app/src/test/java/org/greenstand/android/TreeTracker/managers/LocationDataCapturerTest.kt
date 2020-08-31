package org.greenstand.android.TreeTracker.managers

import android.content.SharedPreferences
import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class LocationDataCapturerTest {

    private lateinit var locationDataCapturer: LocationDataCapturer

    @MockK(relaxed = true)
    private lateinit var userManager: UserManager
    @MockK(relaxed = true)
    private lateinit var locationUpdateManager: LocationUpdateManager
    @MockK(relaxed = true)
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var preferences: Preferences
    @MockK(relaxed = true)
    private lateinit var treeTrackerDAO: TreeTrackerDAO
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        preferences = Preferences(sharedPreferences, userManager)
        locationDataCapturer = LocationDataCapturer(
            userManager,
            locationUpdateManager,
            preferences,
            treeTrackerDAO
        )
    }

    @Test
    fun start() {
        val liveData = mockk<LiveData<Location?>>(relaxed = true)
        every { locationUpdateManager.locationUpdateLiveData } returns liveData

        locationDataCapturer.start()

        verify { liveData.observeForever(any<Observer<Location?>>()) }
    }

    @Test
    fun turnOnTreeCaptureMode() {

        locationDataCapturer.turnOnTreeCaptureMode()

        assertNotNull(locationDataCapturer.generatedTreeUuid)
    }

    @Test
    fun turnOffTreeCaptureMode() {

        locationDataCapturer.turnOffTreeCaptureMode()

        assertNull(locationDataCapturer.generatedTreeUuid)
    }

    @Test
    fun noConvergenceUntilMinLocationSize() {

        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData

        locationDataCapturer.start()
        locationDataCapturer.turnOnTreeCaptureMode()

        // Assuming a minimum of 5 Location data required for evaulating
        // convergence
        for (i in 1..4) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns -122.08400000000002 + i / 100000000
            locationsLiveData.postValue(location)
        }

        assertFalse(locationDataCapturer.convergenceWithinRange)
    }

    @Test
    fun convergenceOnRequiredLocationSize() {

        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData

        locationDataCapturer.start()
        locationDataCapturer.turnOnTreeCaptureMode()
        for (i in 1..5) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns -122.08400000000002 + i / 100000000
            locationsLiveData.postValue(location)
        }
        assertTrue(locationDataCapturer.convergenceWithinRange)
    }

    // Hard coded values used for the following tests
    val longitudeValues = listOf(
        -122.08400001,
        -122.08500111,
        -122.093121519,
        -122.0915121999,
        -122.0773486753,
        -121.0184743902
    )

    @Test
    fun verifyLongitudeMeanComputation() {

        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData

        locationDataCapturer.start()
        locationDataCapturer.turnOnTreeCaptureMode()

        for (i in 0..4) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns longitudeValues[i]
            locationsLiveData.postValue(location)
        }

        assertEquals(-122.08619670284, locationDataCapturer.longitudeMean)
    }

    @Test
    fun verifyLongitudeVarianceComputation() {

        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData

        locationDataCapturer.start()
        locationDataCapturer.turnOnTreeCaptureMode()

        for (i in 0..4) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns longitudeValues[i]
            locationsLiveData.postValue(location)
        }

        assertEquals(0.00016075008086548827, locationDataCapturer.longitudeVariance)
    }

    @Test
    fun verifyLongitudeStandardDeviationComputation() {

        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData

        locationDataCapturer.start()
        locationDataCapturer.turnOnTreeCaptureMode()

        for (i in 0..4) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns longitudeValues[i]
            locationsLiveData.postValue(location)
        }

        assertEquals(0.005670098427108444, locationDataCapturer.longitudeStdDev)
    }

    @Test
    fun runningStatsAfterInitialConvergence() {
        // When an initial computation for standard deviation is computed,
        // newer location values triggers the use of running computation
        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData

        locationDataCapturer.start()
        locationDataCapturer.turnOnTreeCaptureMode()

        // The value of 5th will use the running computation instead of computing the
        // stats using all values
        for (i in 0..5) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns longitudeValues[i]
            locationsLiveData.postValue(location)
        }
        assertEquals(-121.87309157888, locationDataCapturer.longitudeMean)
        assertEquals(0.14623389282643856, locationDataCapturer.longitudeVariance)
        assertEquals(0.3824054037620789, locationDataCapturer.longitudeStdDev)
    }
}
