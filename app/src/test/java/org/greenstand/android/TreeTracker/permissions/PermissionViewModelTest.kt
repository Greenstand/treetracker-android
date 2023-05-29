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
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
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
    fun `verify isLocationEnabled sets the correct value in the permission Items state`() = runBlocking {
        val provider = LocationManager.GPS_PROVIDER
        coEvery { locationManager.isProviderEnabled(provider) } returns true
        testSubject.isLocationEnabled()
        val result = testSubject.state.getOrAwaitValueTest().isLocationEnabled!!
        assertTrue(result)
    }
}