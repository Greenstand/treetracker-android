/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.models.location

import android.content.SharedPreferences
import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.LocationEntity
import org.greenstand.android.TreeTracker.models.ConvergenceConfiguration
import org.greenstand.android.TreeTracker.models.ConvergenceStatus
import org.greenstand.android.TreeTracker.models.LocationData
import org.greenstand.android.TreeTracker.models.LocationDataConfig
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LocationDataCapturerTest {
    private lateinit var locationDataCapturer: LocationDataCapturer

    @MockK(relaxed = true)
    private lateinit var locationUpdateManager: LocationUpdateManager

    @MockK(relaxed = true)
    private lateinit var convergenceConfiguration: ConvergenceConfiguration

    @MockK(relaxed = true)
    private lateinit var timeProvider: TimeProvider

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
        every { convergenceConfiguration.locationDataConfig } returns LocationDataConfig()
        locationDataCapturer =
            LocationDataCapturer(
                locationUpdateManager,
                treeTrackerDAO,
                convergenceConfiguration,
                Json {
                    explicitNulls = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                },
                sessionTracker,
                timeProvider,
            )
    }

    @Test
    fun start() {
        val liveData = mockk<LiveData<Location?>>(relaxed = true)
        every { locationUpdateManager.locationUpdateLiveData } returns liveData

        locationDataCapturer.startGpsUpdates()

        verify { liveData.observeForever(any<Observer<Location?>>()) }
    }

    @Test
    fun `WHEN GPS updates stop THEN pending location inserts are cancelled`() =
        runTest {
            val locationsLiveData = MutableLiveData<Location?>()
            val insertCompleted = CountDownLatch(1)
            every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData
            every { sessionTracker.currentSessionId } returns 1L
            every { timeProvider.currentTime() } returns Instant.fromEpochMilliseconds(1000)
            coEvery { treeTrackerDAO.insertLocationData(any()) } coAnswers {
                delay(500)
                insertCompleted.countDown()
                1L
            }

            locationDataCapturer.startGpsUpdates()
            val location = mockk<Location>(relaxed = true)
            every { location.latitude } returns 1.0
            every { location.longitude } returns 2.0
            every { location.accuracy } returns 3f

            locationsLiveData.value = location
            locationDataCapturer.stopGpsUpdates()

            assertFalse(insertCompleted.await(1, TimeUnit.SECONDS))
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

        locationDataCapturer.startGpsUpdates()
        locationDataCapturer.turnOnTreeCaptureMode()

        // Assuming a minimum of 5 Location data required for evaulating
        // convergence
        for (i in 1..4) {
            val location = mockk<Location>(relaxed = true)
            locationsLiveData.postValue(location)
        }

        assertFalse(locationDataCapturer.isConvergenceWithinRange())
    }

    val locationValuesLowVariance =
        listOf(
            Pair(-122.08400001, 37.42149486),
            Pair(-122.08400111, 37.42149487),
            Pair(-122.08400021519, 37.42149483),
            Pair(-122.08400021999, 37.42149489),
            Pair(-122.0840086753, 37.42149481),
        )

    @Test
    fun convergenceWithinRange() {
        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData

        locationDataCapturer.startGpsUpdates()
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
    val locationValuesHighVariance =
        listOf(
            Pair(-122.08400001, 37.42149486),
            Pair(-122.08500111, 37.42149487),
            Pair(-122.093821519, 37.42149483),
            Pair(-122.0913121999, 37.42149489),
            Pair(-122.0773486753, 37.42149481),
        )

    @Test
    fun convergenceNotWithinRange() {
        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData

        locationDataCapturer.startGpsUpdates()
        locationDataCapturer.turnOnTreeCaptureMode()
        for (i in 0..4) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns locationValuesHighVariance[i].first
            every { location.latitude } returns locationValuesHighVariance[i].second
            locationsLiveData.postValue(location)
        }

        assertFalse(locationDataCapturer.isConvergenceWithinRange())
    }

    val locationsWithLowAndHighVariance =
        listOf(
            Pair(-122.08400001, 37.42149486),
            Pair(-122.08400111, 37.42149487),
            Pair(-122.08400021519, 37.42149483),
            Pair(-122.08400021999, 37.42149489),
            Pair(-122.0840086753, 37.42149481),
            Pair(-122.0913121999, 37.42149489),
            Pair(-122.0773486753, 37.42149481),
        )

    @Test
    fun unconvergeWhenGPSWanders() {
        val locationsLiveData = MutableLiveData<Location>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData
        locationDataCapturer.startGpsUpdates()
        locationDataCapturer.turnOnTreeCaptureMode()

        for (i in 0..4) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns locationValuesLowVariance[i].first
            every { location.latitude } returns locationValuesLowVariance[i].second
            locationsLiveData.postValue(location)
        }

        assertTrue(
            "Converges with low variance location values",
            locationDataCapturer.isConvergenceWithinRange(),
        )

        for (i in 5..6) {
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns locationsWithLowAndHighVariance[i].first
            every { location.latitude } returns locationsWithLowAndHighVariance[i].second
            locationsLiveData.postValue(location)
        }
        assertFalse(
            "location unconverges when gps values wander",
            locationDataCapturer.isConvergenceWithinRange(),
        )
    }

    private fun startCapturing(): MutableLiveData<Location?> {
        val locationsLiveData = MutableLiveData<Location?>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData
        locationDataCapturer.startGpsUpdates()
        locationDataCapturer.turnOnTreeCaptureMode()
        return locationsLiveData
    }

    private fun MutableLiveData<Location?>.postLocations(values: List<Pair<Double, Double>>) {
        values.forEach { (longitude, latitude) ->
            val location = mockk<Location>(relaxed = true)
            every { location.longitude } returns longitude
            every { location.latitude } returns latitude
            postValue(location)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `convergence outside of tree capture mode throws`() {
        locationDataCapturer.convergence()
    }

    @Test
    fun `no coordinates are available before any convergence`() {
        assertFalse(locationDataCapturer.isLocationCoordinateAvailable())
    }

    @Test
    fun `WHEN converged THEN coordinates are available from the converged window`() {
        startCapturing().postLocations(locationValuesLowVariance)

        assertTrue(locationDataCapturer.isLocationCoordinateAvailable())
        assertNotNull(locationDataCapturer.lastConvergenceWithinRange)
        assertSame(locationDataCapturer.lastConvergenceWithinRange, locationDataCapturer.convergence())
    }

    @Test
    fun `WHEN not converged THEN convergence falls back to the current window`() {
        startCapturing().postLocations(locationValuesHighVariance)

        assertTrue(locationDataCapturer.isLocationCoordinateAvailable())
        assertNull(locationDataCapturer.lastConvergenceWithinRange)
        assertSame(locationDataCapturer.currentConvergence, locationDataCapturer.convergence())
    }

    @Test
    fun `locations outside of tree capture mode do not build a convergence`() {
        val locationsLiveData = MutableLiveData<Location?>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData
        locationDataCapturer.startGpsUpdates()

        locationsLiveData.postLocations(locationValuesLowVariance)

        assertNull(locationDataCapturer.currentConvergence)
        assertFalse(locationDataCapturer.isConvergenceWithinRange())
    }

    @Test
    fun `null locations are ignored`() {
        val locationsLiveData = startCapturing()

        locationsLiveData.postValue(null)

        assertNull(locationDataCapturer.currentConvergence)
        verify(exactly = 0) { sessionTracker.currentSessionId }
    }

    @Test
    fun `turnOffTreeCaptureMode clears the convergence`() {
        startCapturing().postLocations(locationValuesLowVariance)

        locationDataCapturer.turnOffTreeCaptureMode()

        assertNull(locationDataCapturer.currentConvergence)
        assertNull(locationDataCapturer.lastConvergenceWithinRange)
        assertFalse(locationDataCapturer.isLocationCoordinateAvailable())
        assertFalse(locationDataCapturer.isConvergenceWithinRange())
    }

    @Test
    fun `turnOffTreeCaptureMode drops the collected locations`() {
        val locationsLiveData = startCapturing()
        locationsLiveData.postLocations(locationValuesLowVariance.take(4))
        locationDataCapturer.turnOffTreeCaptureMode()
        locationDataCapturer.turnOnTreeCaptureMode()

        locationsLiveData.postLocations(locationValuesLowVariance.take(4))

        assertNull(locationDataCapturer.currentConvergence)
    }

    @Test
    fun `turnOnTreeCaptureMode resets the status to not converged`() {
        startCapturing().postLocations(locationValuesLowVariance)
        assertTrue(locationDataCapturer.isConvergenceWithinRange())

        locationDataCapturer.turnOnTreeCaptureMode()

        assertFalse(locationDataCapturer.isConvergenceWithinRange())
    }

    @Test
    fun `turnOnTreeCaptureMode generates a new tree uuid each time`() {
        locationDataCapturer.turnOnTreeCaptureMode()
        val first = locationDataCapturer.generatedTreeUuid

        locationDataCapturer.turnOnTreeCaptureMode()

        assertTrue(first != locationDataCapturer.generatedTreeUuid)
    }

    @Test
    fun `startGpsUpdates only starts location updates once`() {
        every { locationUpdateManager.locationUpdateLiveData } returns MutableLiveData()

        locationDataCapturer.startGpsUpdates()
        locationDataCapturer.startGpsUpdates()

        verify(exactly = 1) { locationUpdateManager.startLocationUpdates() }
    }

    @Test
    fun `stopGpsUpdates without starting does nothing`() {
        locationDataCapturer.stopGpsUpdates()

        verify(exactly = 0) { locationUpdateManager.stopLocationUpdates() }
    }

    @Test
    fun `stopGpsUpdates removes the observer and stops location updates`() {
        val locationsLiveData = MutableLiveData<Location?>()
        every { locationUpdateManager.locationUpdateLiveData } returns locationsLiveData
        locationDataCapturer.startGpsUpdates()

        locationDataCapturer.stopGpsUpdates()

        assertFalse(locationsLiveData.hasObservers())
        verify(exactly = 1) { locationUpdateManager.stopLocationUpdates() }
    }

    @Test
    fun `GPS updates can be restarted after stopping`() {
        every { locationUpdateManager.locationUpdateLiveData } returns MutableLiveData()
        locationDataCapturer.startGpsUpdates()
        locationDataCapturer.stopGpsUpdates()

        locationDataCapturer.startGpsUpdates()

        verify(exactly = 2) { locationUpdateManager.startLocationUpdates() }
    }

    @Test
    fun `percentage observers are only notified when the percentage increases`() {
        val received = mutableListOf<Float>()
        locationDataCapturer.percentageConvergenceObservers.add { received.add(it) }

        locationDataCapturer.newestPercentageConvergence = 0.5f
        locationDataCapturer.newestPercentageConvergence = 0.3f
        locationDataCapturer.newestPercentageConvergence = 0.8f

        assertEquals(listOf(0.5f, 0.8f), received)
        assertEquals(0.8f, locationDataCapturer.newestPercentageConvergence, 0f)
    }

    @Test
    fun `converged locations report full convergence percentage`() {
        val received = mutableListOf<Float>()
        locationDataCapturer.percentageConvergenceObservers.add { received.add(it) }

        startCapturing().postLocations(locationValuesLowVariance)

        assertEquals(listOf(1f), received)
    }

    @Test
    fun `unconverged locations report a partial convergence percentage`() {
        startCapturing().postLocations(locationValuesHighVariance)

        val percentage = locationDataCapturer.newestPercentageConvergence
        assertTrue(percentage > 0f)
        assertTrue(percentage < 1f)
    }

    @Test
    fun `WHEN a session is active THEN each location is saved for that session`() {
        every { sessionTracker.currentSessionId } returns 5L
        every { timeProvider.currentTime() } returns Instant.fromEpochMilliseconds(1000)
        val savedLocation = slot<LocationEntity>()
        coEvery { treeTrackerDAO.insertLocationData(capture(savedLocation)) } returns 1L
        val locationsLiveData = startCapturing()
        val location = mockk<Location>(relaxed = true)
        every { location.latitude } returns 1.0
        every { location.longitude } returns 2.0
        every { location.accuracy } returns 3f

        locationsLiveData.postValue(location)

        coVerify(timeout = 5000) { treeTrackerDAO.insertLocationData(any()) }
        assertEquals(5L, savedLocation.captured.sessionId)
        assertEquals(
            LocationData(
                planterCheckInId = 5L,
                latitude = 1.0,
                longitude = 2.0,
                accuracy = 3f,
                treeUuid = locationDataCapturer.generatedTreeUuid.toString(),
                convergenceStatus = ConvergenceStatus.NOT_CONVERGED,
                capturedAt = Instant.fromEpochMilliseconds(1000).toString(),
            ),
            Json.decodeFromString(LocationData.serializer(), savedLocation.captured.locationDataJson),
        )
    }

    @Test
    fun `WHEN no session is active THEN locations are not saved`() {
        every { sessionTracker.currentSessionId } returns null
        val locationsLiveData = startCapturing()

        locationsLiveData.postValue(mockk<Location>(relaxed = true))

        coVerify(exactly = 0) { treeTrackerDAO.insertLocationData(any()) }
    }

    @Test
    fun `WHEN already converged THEN converge keeps the converged status`() =
        runTest {
            startCapturing().postLocations(locationValuesLowVariance)

            locationDataCapturer.converge()

            assertTrue(locationDataCapturer.isConvergenceWithinRange())
        }

    @Test
    fun `WHEN convergence times out THEN the status is timed out`() =
        runTest {
            every { sessionTracker.currentSessionId } returns 5L
            every { timeProvider.currentTime() } returns Instant.fromEpochMilliseconds(1000)
            val savedLocation = slot<LocationEntity>()
            coEvery { treeTrackerDAO.insertLocationData(capture(savedLocation)) } returns 1L
            val locationsLiveData = startCapturing()

            locationDataCapturer.converge()

            assertFalse(locationDataCapturer.isConvergenceWithinRange())
            locationsLiveData.postValue(mockk<Location>(relaxed = true))
            coVerify(timeout = 5000) { treeTrackerDAO.insertLocationData(any()) }
            val saved = Json.decodeFromString(LocationData.serializer(), savedLocation.captured.locationDataJson)
            assertEquals(ConvergenceStatus.TIMED_OUT, saved.convergenceStatus)
        }
}