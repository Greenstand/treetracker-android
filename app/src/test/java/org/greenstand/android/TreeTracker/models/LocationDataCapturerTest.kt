package org.greenstand.android.TreeTracker.models

import android.content.SharedPreferences
import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.gson.GsonBuilder
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LocationDataCapturerTest {

    private lateinit var locationDataCapturer: LocationDataCapturer

    @MockK(relaxed = true)
    private lateinit var user: Planter

    @MockK(relaxed = true)
    private lateinit var locationUpdateManager: LocationUpdateManager

    @MockK(relaxed = true)
    private lateinit var configuration: Configuration
    @MockK(relaxed = true)
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var preferences: Preferences

    @MockK(relaxed = true)
    private lateinit var treeTrackerDAO: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var sessionTracker: SessionTracker

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        preferences = Preferences(sharedPreferences)
        every { configuration.locationDataConfig } returns LocationDataConfig()
        locationDataCapturer = LocationDataCapturer(
            user,
            locationUpdateManager,
            treeTrackerDAO,
            configuration,
            GsonBuilder().serializeNulls().create(),
            sessionTracker,
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

        assertNotNull("Tree UUID is generated", locationDataCapturer.generatedTreeUuid)
    }

    @Test
    fun turnOffTreeCaptureMode() {

        locationDataCapturer.turnOffTreeCaptureMode()

        assertNull("Tree UUID is reset to null", locationDataCapturer.generatedTreeUuid)
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
            locationsLiveData.postValue(location)
        }

        assertFalse(locationDataCapturer.isConvergenceWithinRange())
    }

    val locationValuesLowVariance = listOf(
        Pair(-122.08400001, 37.42149486),
        Pair(-122.08400111, 37.42149487),
        Pair(-122.08400021519, 37.42149483),
        Pair(-122.08400021999, 37.42149489),
        Pair(-122.0840086753, 37.42149481)
    )

    @Test
    fun convergenceWithinRange() {

        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData

        locationDataCapturer.start()
        locationDataCapturer.turnOnTreeCaptureMode()
        for (i in 0..4) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns locationValuesLowVariance[i].first
            every { location.latitude } returns locationValuesLowVariance[i].second
            locationsLiveData.postValue(location)
        }
        assertTrue(locationDataCapturer.isConvergenceWithinRange())
    }

    // Hard coded longitude and latitude pair values
    val locationValuesHighVariance = listOf(
        Pair(-122.08400001, 37.42149486),
        Pair(-122.08500111, 37.42149487),
        Pair(-122.093821519, 37.42149483),
        Pair(-122.0913121999, 37.42149489),
        Pair(-122.0773486753, 37.42149481)
    )

    @Test
    fun convergenceNotWithinRange() {
        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData

        locationDataCapturer.start()
        locationDataCapturer.turnOnTreeCaptureMode()
        for (i in 0..4) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns locationValuesHighVariance[i].first
            every { location.latitude } returns locationValuesHighVariance[i].second
            locationsLiveData.postValue(location)
        }

        assertFalse(locationDataCapturer.isConvergenceWithinRange())
    }

    val locationsWithLowAndHighVariance = listOf(
        Pair(-122.08400001, 37.42149486),
        Pair(-122.08400111, 37.42149487),
        Pair(-122.08400021519, 37.42149483),
        Pair(-122.08400021999, 37.42149489),
        Pair(-122.0840086753, 37.42149481),
        Pair(-122.0913121999, 37.42149489),
        Pair(-122.0773486753, 37.42149481)
    )
    @Test
    fun unconvergeWhenGPSWanders() {
        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData
        locationDataCapturer.start()
        locationDataCapturer.turnOnTreeCaptureMode()

        for (i in 0..4) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns locationValuesLowVariance[i].first
            every { location.latitude } returns locationValuesLowVariance[i].second
            locationsLiveData.postValue(location)
        }

        assertTrue(
            "Converges with low variance location values",
            locationDataCapturer.isConvergenceWithinRange()
        )

        for (i in 5..6) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns locationsWithLowAndHighVariance[i].first
            every { location.latitude } returns locationsWithLowAndHighVariance[i].second
            locationsLiveData.postValue(location)
        }
        assertFalse(
            "location unconverges when gps values wander",
            locationDataCapturer.isConvergenceWithinRange()
        )
    }
}
