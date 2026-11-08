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
package org.greenstand.android.TreeTracker.permissions

import android.location.LocationManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PermissionViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val locationManager = mockk<LocationManager>(relaxed = true)
    private lateinit var testSubject: PermissionViewModel

    @Before
    fun setup() {
        testSubject = PermissionViewModel(locationManager)
    }

    @Test
    fun `verify isLocationEnabled sets the correct value in the permission Items state`() =
        runTest {
            val provider = LocationManager.GPS_PROVIDER
            coEvery { locationManager.isProviderEnabled(provider) } returns true
            testSubject.handleAction(PermissionAction.CheckLocationEnabled)
            val result = testSubject.state.value.isLocationEnabled!!
            assertTrue(result)
        }

    @Test
    fun `initial state has not checked the location provider yet`() {
        assertNull(testSubject.state.value.isLocationEnabled)
        assertTrue(testSubject.events.replayCache.isEmpty())
        verify(exactly = 0) { locationManager.isProviderEnabled(any()) }
    }

    @Test
    fun `disabled gps provider sets isLocationEnabled to false`() {
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns false

        testSubject.handleAction(PermissionAction.CheckLocationEnabled)

        assertEquals(false, testSubject.state.value.isLocationEnabled)
    }

    @Test
    fun `only the gps provider is checked`() {
        every { locationManager.isProviderEnabled(any()) } returns true

        testSubject.handleAction(PermissionAction.CheckLocationEnabled)

        verify(exactly = 1) { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }
        verify(exactly = 0) { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }
    }

    @Test
    fun `rechecking picks up a provider that was switched on`() {
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returnsMany listOf(false, true)

        testSubject.handleAction(PermissionAction.CheckLocationEnabled)
        assertFalse(testSubject.state.value.isLocationEnabled!!)

        testSubject.handleAction(PermissionAction.CheckLocationEnabled)
        assertTrue(testSubject.state.value.isLocationEnabled!!)
        assertTrue(testSubject.events.replayCache.isEmpty())
    }
}