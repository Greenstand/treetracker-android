/*
 * Copyright 2026 Treetracker
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

import android.Manifest
import android.app.Application
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.greenstand.android.TreeTracker.models.ConvergenceConfiguration
import org.greenstand.android.TreeTracker.models.LocationDataConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocationUpdateManagerTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val locationManager = mockk<LocationManager>(relaxed = true)
    private val convergenceConfiguration = mockk<ConvergenceConfiguration>()
    private val listener = slot<LocationListener>()
    private lateinit var locationUpdateManager: LocationUpdateManager

    @Before
    fun setUp() {
        every { convergenceConfiguration.locationDataConfig } returns
            LocationDataConfig(minTimeBetweenUpdates = 2000L, minDistanceBetweenUpdates = 5f)
        every {
            locationManager.requestLocationUpdates(any<String>(), any<Long>(), any<Float>(), capture(listener))
        } just Runs
        locationUpdateManager = LocationUpdateManager(locationManager, application, convergenceConfiguration)
    }

    private fun grantFineLocation() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun denyLocation() {
        shadowOf(application).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

    @Test
    fun `starts with no location and not updating`() {
        assertNull(locationUpdateManager.locationUpdateLiveData.value)
        assertNull(locationUpdateManager.currentLocation)
        assertFalse(locationUpdateManager.isUpdating)
    }

    @Test
    fun `WHEN location permission granted THEN GPS updates are requested with the configured intervals`() {
        grantFineLocation()

        val started = locationUpdateManager.startLocationUpdates()

        assertTrue(started)
        assertTrue(locationUpdateManager.isUpdating)
        verify {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 5f, any<LocationListener>())
        }
    }

    @Test
    fun `WHEN only coarse location is granted THEN GPS updates are still requested`() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        assertTrue(locationUpdateManager.startLocationUpdates())
    }

    @Test
    fun `WHEN already updating THEN startLocationUpdates does not request again`() {
        grantFineLocation()
        locationUpdateManager.startLocationUpdates()

        val started = locationUpdateManager.startLocationUpdates()

        assertTrue(started)
        verify(exactly = 1) {
            locationManager.requestLocationUpdates(any<String>(), any<Long>(), any<Float>(), any<LocationListener>())
        }
    }

    @Test
    fun `WHEN location permission denied THEN no updates are requested`() {
        denyLocation()

        val started = locationUpdateManager.startLocationUpdates()

        assertFalse(started)
        assertFalse(locationUpdateManager.isUpdating)
        verify(exactly = 0) {
            locationManager.requestLocationUpdates(any<String>(), any<Long>(), any<Float>(), any<LocationListener>())
        }
    }

    @Test
    fun `WHEN a location arrives THEN it becomes the current location and is published`() {
        grantFineLocation()
        locationUpdateManager.startLocationUpdates()
        val location =
            Location(LocationManager.GPS_PROVIDER).apply {
                latitude = 1.5
                longitude = 2.5
            }

        listener.captured.onLocationChanged(location)

        assertSame(location, locationUpdateManager.currentLocation)
        assertSame(location, locationUpdateManager.locationUpdateLiveData.value)
    }

    @Test
    fun `WHEN nobody observes THEN stopLocationUpdates removes the listener and clears the location`() {
        grantFineLocation()
        locationUpdateManager.startLocationUpdates()
        listener.captured.onLocationChanged(Location(LocationManager.GPS_PROVIDER))

        val stopped = locationUpdateManager.stopLocationUpdates()

        assertTrue(stopped)
        assertFalse(locationUpdateManager.isUpdating)
        assertNull(locationUpdateManager.locationUpdateLiveData.value)
        verify { locationManager.removeUpdates(listener.captured) }
    }

    @Test
    fun `WHEN the location is still observed THEN stopLocationUpdates keeps updating`() {
        grantFineLocation()
        locationUpdateManager.startLocationUpdates()
        locationUpdateManager.locationUpdateLiveData.observeForever(Observer<Location?> { })

        val stopped = locationUpdateManager.stopLocationUpdates()

        assertFalse(stopped)
        assertTrue(locationUpdateManager.isUpdating)
        verify(exactly = 0) { locationManager.removeUpdates(any<LocationListener>()) }
    }

    @Test
    fun `refreshLocationUpdateRequest re-requests updates with the latest config`() {
        grantFineLocation()
        locationUpdateManager.startLocationUpdates()
        every { convergenceConfiguration.locationDataConfig } returns
            LocationDataConfig(minTimeBetweenUpdates = 500L, minDistanceBetweenUpdates = 1f)

        locationUpdateManager.refreshLocationUpdateRequest()

        verify { locationManager.removeUpdates(listener.captured) }
        verify {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 1f, any<LocationListener>())
        }
    }

    @Test
    fun `WHEN permission denied THEN refreshLocationUpdateRequest only removes updates`() {
        denyLocation()

        locationUpdateManager.refreshLocationUpdateRequest()

        verify(exactly = 1) { locationManager.removeUpdates(any<LocationListener>()) }
        verify(exactly = 0) {
            locationManager.requestLocationUpdates(any<String>(), any<Long>(), any<Float>(), any<LocationListener>())
        }
    }

    @Test
    fun `refreshLocationUpdateRequest reuses the same listener`() {
        grantFineLocation()
        locationUpdateManager.startLocationUpdates()
        val first = listener.captured

        locationUpdateManager.refreshLocationUpdateRequest()

        assertSame(first, listener.captured)
        verify(exactly = 2) {
            locationManager.requestLocationUpdates(any<String>(), any<Long>(), any<Float>(), any<LocationListener>())
        }
    }
}