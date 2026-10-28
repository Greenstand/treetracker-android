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
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SessionUploaderTest {
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

    private lateinit var sessionUploader: SessionUploader

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "test-device-id"
        sessionUploader =
            SessionUploader(
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
    fun `WHEN upload called THEN creates session requests uploads bundle and marks uploaded`() =
        runTest {
            val sessionEntity =
                SessionEntity(
                    uuid = "session-uuid-1",
                    originUserId = "user-uuid-1",
                    originWallet = "wallet-1",
                    destinationWallet = "dest-wallet-1",
                    startTime = Instant.parse("2023-01-01T00:00:00Z"),
                    organization = "TestOrg",
                    isUploaded = false,
                    deviceConfigId = 10L,
                ).apply { id = 1L }

            val deviceConfigEntity =
                DeviceConfigEntity(
                    uuid = "device-config-uuid-1",
                    appVersion = "1.0",
                    appBuild = 1,
                    osVersion = "13",
                    sdkVersion = 33,
                    loggedAt = Instant.parse("2023-01-01T00:00:00Z"),
                ).apply { id = 10L }

            coEvery { dao.getSessionsToUpload() } returns listOf(sessionEntity)
            coEvery { dao.getDeviceConfigById(10L) } returns deviceConfigEntity

            sessionUploader.upload()

            coVerify(exactly = 1) { dao.getDeviceConfigById(10L) }
            coVerify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 1) { dao.updateSessionUploadStatus(listOf(1L), true) }
            coVerify(exactly = 1) { dao.updateSessionBundleIds(listOf(1L), any()) }
        }

    @Test
    fun `WHEN upload called THEN resolves deviceConfigId to UUID via DAO`() =
        runTest {
            val sessionEntity =
                SessionEntity(
                    uuid = "session-uuid-2",
                    originUserId = "user-uuid-2",
                    originWallet = "wallet-2",
                    destinationWallet = "dest-wallet-2",
                    startTime = Instant.parse("2023-06-15T10:00:00Z"),
                    organization = "AnotherOrg",
                    isUploaded = false,
                    deviceConfigId = 20L,
                ).apply { id = 2L }

            val deviceConfigEntity =
                DeviceConfigEntity(
                    uuid = "resolved-device-uuid",
                    appVersion = "2.0",
                    appBuild = 2,
                    osVersion = "14",
                    sdkVersion = 34,
                    loggedAt = Instant.parse("2023-06-15T10:00:00Z"),
                ).apply { id = 20L }

            coEvery { dao.getSessionsToUpload() } returns listOf(sessionEntity)
            coEvery { dao.getDeviceConfigById(20L) } returns deviceConfigEntity

            sessionUploader.upload()

            coVerify(exactly = 1) { dao.getDeviceConfigById(20L) }
        }
}