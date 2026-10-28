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
package org.greenstand.android.TreeTracker.models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.DeviceConfigEntity
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@ExperimentalCoroutinesApi
class DeviceConfigUpdaterTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var timeProvider: TimeProvider

    private lateinit var deviceConfigUpdater: DeviceConfigUpdater

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { timeProvider.currentTime() } returns Instant.parse("2023-01-01T00:00:00Z")
        deviceConfigUpdater =
            DeviceConfigUpdater(
                dao = dao,
                timeProvider = timeProvider,
            )
    }

    @Test
    fun `WHEN getLatestDeviceConfig returns null THEN saves new config`() =
        runTest {
            coEvery { dao.getLatestDeviceConfig() } returns null

            deviceConfigUpdater.saveLatestConfig()

            coVerify(atLeast = 1) { dao.insertDeviceConfig(any()) }
        }

    @Test
    fun `WHEN app version differs from stored THEN saves new config`() =
        runTest {
            val existingConfig =
                DeviceConfigEntity(
                    uuid = "test-uuid",
                    appVersion = "different-version",
                    appBuild = BuildConfig.VERSION_CODE,
                    osVersion = android.os.Build.VERSION.RELEASE,
                    sdkVersion = android.os.Build.VERSION.SDK_INT,
                    loggedAt = Instant.parse("2023-01-01T00:00:00Z"),
                )
            coEvery { dao.getLatestDeviceConfig() } returns existingConfig

            deviceConfigUpdater.saveLatestConfig()

            coVerify(atLeast = 1) { dao.insertDeviceConfig(any()) }
        }

    @Test
    fun `WHEN config matches current values THEN does not save again`() =
        runTest {
            val matchingConfig =
                DeviceConfigEntity(
                    uuid = "test-uuid",
                    appVersion = BuildConfig.VERSION_NAME,
                    appBuild = BuildConfig.VERSION_CODE,
                    osVersion = android.os.Build.VERSION.RELEASE,
                    sdkVersion = android.os.Build.VERSION.SDK_INT,
                    loggedAt = Instant.parse("2023-01-01T00:00:00Z"),
                )
            coEvery { dao.getLatestDeviceConfig() } returns matchingConfig

            deviceConfigUpdater.saveLatestConfig()

            coVerify(exactly = 0) { dao.insertDeviceConfig(any()) }
        }

    @Test
    fun `WHEN no config stored THEN saves exactly one config with the current build values`() =
        runTest {
            coEvery { dao.getLatestDeviceConfig() } returns null
            val configSlot = slot<DeviceConfigEntity>()
            coEvery { dao.insertDeviceConfig(capture(configSlot)) } returns 1L

            deviceConfigUpdater.saveLatestConfig()

            coVerify(exactly = 1) { dao.insertDeviceConfig(any()) }
            val config = configSlot.captured
            assertEquals(BuildConfig.VERSION_NAME, config.appVersion)
            assertEquals(BuildConfig.VERSION_CODE, config.appBuild)
            assertEquals(android.os.Build.VERSION.RELEASE, config.osVersion)
            assertEquals(android.os.Build.VERSION.SDK_INT, config.sdkVersion)
            assertEquals(Instant.parse("2023-01-01T00:00:00Z"), config.loggedAt)
            assertFalse(config.isUploaded)
            assertTrue(config.uuid.isNotEmpty())
        }

    @Test
    fun `WHEN app build differs from stored THEN saves new config`() =
        runTest {
            coEvery { dao.getLatestDeviceConfig() } returns currentConfig().copy(appBuild = BuildConfig.VERSION_CODE + 1)

            deviceConfigUpdater.saveLatestConfig()

            coVerify(exactly = 1) { dao.insertDeviceConfig(any()) }
        }

    @Test
    fun `WHEN os version differs from stored THEN saves new config`() =
        runTest {
            coEvery { dao.getLatestDeviceConfig() } returns currentConfig().copy(osVersion = "older-os")

            deviceConfigUpdater.saveLatestConfig()

            coVerify(exactly = 1) { dao.insertDeviceConfig(any()) }
        }

    @Test
    fun `WHEN sdk version differs from stored THEN saves new config`() =
        runTest {
            coEvery { dao.getLatestDeviceConfig() } returns currentConfig().copy(sdkVersion = android.os.Build.VERSION.SDK_INT - 1)

            deviceConfigUpdater.saveLatestConfig()

            coVerify(exactly = 1) { dao.insertDeviceConfig(any()) }
        }

    @Test
    fun `WHEN config changed THEN new config gets a fresh uuid`() =
        runTest {
            coEvery { dao.getLatestDeviceConfig() } returns currentConfig().copy(appVersion = "0.0.1")
            val configSlot = slot<DeviceConfigEntity>()
            coEvery { dao.insertDeviceConfig(capture(configSlot)) } returns 1L

            deviceConfigUpdater.saveLatestConfig()

            assertTrue(configSlot.captured.uuid != "test-uuid")
            assertEquals(BuildConfig.VERSION_NAME, configSlot.captured.appVersion)
        }

    private fun currentConfig(): DeviceConfigEntity =
        DeviceConfigEntity(
            uuid = "test-uuid",
            appVersion = BuildConfig.VERSION_NAME,
            appBuild = BuildConfig.VERSION_CODE,
            osVersion = android.os.Build.VERSION.RELEASE,
            sdkVersion = android.os.Build.VERSION.SDK_INT,
            loggedAt = Instant.parse("2023-01-01T00:00:00Z"),
        )
}