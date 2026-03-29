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
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.DeviceConfigEntity
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@ExperimentalCoroutinesApi
class DeviceConfigUploaderTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var objectStorageClient: ObjectStorageClient

    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private lateinit var deviceConfigUploader: DeviceConfigUploader

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "test-device-id"
        deviceConfigUploader =
            DeviceConfigUploader(
                dao = dao,
                objectStorageClient = objectStorageClient,
                json = json,
            )
    }

    @After
    fun tearDown() {
        unmockkObject(DeviceUtils)
    }

    @Test
    fun `WHEN upload called with configs THEN creates requests uploads bundle and marks uploaded`() =
        runTest {
            val deviceConfig =
                DeviceConfigEntity(
                    uuid = "config-uuid-1",
                    appVersion = "1.0",
                    appBuild = 1,
                    osVersion = "13",
                    sdkVersion = 33,
                    loggedAt = Instant.parse("2023-01-01T00:00:00Z"),
                ).apply { id = 1L }

            coEvery { dao.getDeviceConfigsToUpload() } returns listOf(deviceConfig)

            deviceConfigUploader.upload("instance-123")

            coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 1) { dao.updateDeviceConfigUploadStatus(listOf(1L), true) }
            coVerify(exactly = 1) { dao.updateDeviceConfigBundleIds(listOf(1L), any()) }
        }

    @Test
    fun `WHEN no configs to upload THEN no-ops`() =
        runTest {
            coEvery { dao.getDeviceConfigsToUpload() } returns emptyList()

            deviceConfigUploader.upload("instance-123")

            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 0) { dao.updateDeviceConfigUploadStatus(any(), any()) }
            coVerify(exactly = 0) { dao.updateDeviceConfigBundleIds(any(), any()) }
        }
}