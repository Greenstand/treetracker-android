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
package org.greenstand.android.TreeTracker.usecases

import com.amazonaws.AmazonClientException
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.LocationEntity
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.models.ConvergenceStatus
import org.greenstand.android.TreeTracker.models.LocationData
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.md5
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class UploadLocationDataUseCaseTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var storageClient: ObjectStorageClient

    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private lateinit var uploadLocationDataUseCase: UploadLocationDataUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "test-device-id"
        mockkObject(ObjectStorageClient.Companion)
        every { ObjectStorageClient.instance() } returns storageClient
        uploadLocationDataUseCase = UploadLocationDataUseCase(dao = dao, json = json)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `WHEN locations exist THEN uploads tracks grouped by session and returns true`() =
        runTest {
            stubTwoSessionsWithLocations()
            val bundleSlot = slot<String>()
            every { storageClient.uploadBundle(capture(bundleSlot), any()) } returns Unit

            val result = uploadLocationDataUseCase.execute(Unit)

            assertTrue(result)
            val bundle = json.parseToJsonElement(bundleSlot.captured).jsonObject
            assertEquals("2", bundle["pack_format_version"]!!.jsonPrimitive.content)
            assertEquals("test-device-id", bundle["device_id"]!!.jsonPrimitive.content)
            val tracks = bundle["tracks"]!!.jsonArray
            assertEquals(2, tracks.size)
            assertEquals("session-uuid-1", tracks[0].jsonObject["sessionId"]!!.jsonPrimitive.content)
            assertEquals(2, tracks[0].jsonObject["locations"]!!.jsonArray.size)
            assertEquals("session-uuid-2", tracks[1].jsonObject["sessionId"]!!.jsonPrimitive.content)
            assertEquals(1, tracks[1].jsonObject["locations"]!!.jsonArray.size)
        }

    @Test
    fun `WHEN locations exist THEN maps location data fields into location requests`() =
        runTest {
            stubTwoSessionsWithLocations()
            val bundleSlot = slot<String>()
            every { storageClient.uploadBundle(capture(bundleSlot), any()) } returns Unit

            uploadLocationDataUseCase.execute(Unit)

            val location =
                json
                    .parseToJsonElement(bundleSlot.captured)
                    .jsonObject["tracks"]!!
                    .jsonArray[0]
                    .jsonObject["locations"]!!
                    .jsonArray[0]
                    .jsonObject
            assertEquals(3.5f, location["accuracy"]!!.jsonPrimitive.float)
            assertEquals("2023-01-01T00:00:01Z", location["capturedAt"]!!.jsonPrimitive.content)
            assertEquals(-1.29, location["latitude"]!!.jsonPrimitive.double)
            assertEquals(36.82, location["longitude"]!!.jsonPrimitive.double)
        }

    @Test
    fun `WHEN bundle uploaded THEN bundle id is the md5 of the bundle with tracks suffix`() =
        runTest {
            stubTwoSessionsWithLocations()
            val bundleSlot = slot<String>()
            val bundleIdSlot = slot<String>()
            every { storageClient.uploadBundle(capture(bundleSlot), capture(bundleIdSlot)) } returns Unit

            uploadLocationDataUseCase.execute(Unit)

            assertEquals("${bundleSlot.captured.md5()}_tracks", bundleIdSlot.captured)
        }

    @Test
    fun `WHEN upload succeeds THEN marks locations uploaded and purges them after uploading`() =
        runTest {
            stubTwoSessionsWithLocations()

            uploadLocationDataUseCase.execute(Unit)

            coVerifyOrder {
                storageClient.uploadBundle(any(), any())
                dao.updateLocationDataUploadStatus(listOf(1L, 2L, 3L), true)
                dao.purgeUploadedLocations()
            }
            coVerify(exactly = 1) { dao.getSessionById(1L) }
            coVerify(exactly = 1) { dao.getSessionById(2L) }
        }

    @Test
    fun `WHEN storage client throws AmazonClientException THEN returns false and keeps locations`() =
        runTest {
            stubTwoSessionsWithLocations()
            every { storageClient.uploadBundle(any(), any()) } throws AmazonClientException("offline")

            val result = uploadLocationDataUseCase.execute(Unit)

            assertFalse(result)
            coVerify(exactly = 0) { dao.updateLocationDataUploadStatus(any(), any()) }
            coVerify(exactly = 0) { dao.purgeUploadedLocations() }
        }

    @Test
    fun `WHEN stored location json is malformed THEN returns false without uploading`() =
        runTest {
            coEvery { dao.getLocationData() } returns
                listOf(LocationEntity(locationDataJson = "not json", sessionId = 1L).apply { id = 1L })

            val result = uploadLocationDataUseCase.execute(Unit)

            assertFalse(result)
            verify(exactly = 0) { storageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 0) { dao.updateLocationDataUploadStatus(any(), any()) }
        }

    @Test
    fun `WHEN session of a location cannot be resolved THEN returns false without uploading`() =
        runTest {
            coEvery { dao.getLocationData() } returns
                listOf(LocationEntity(locationDataJson = locationJson(1.0, 2.0), sessionId = 1L).apply { id = 1L })
            coEvery { dao.getSessionById(1L) } returns session(id = 99L, uuid = "other-session")

            val result = uploadLocationDataUseCase.execute(Unit)

            assertFalse(result)
            verify(exactly = 0) { storageClient.uploadBundle(any(), any()) }
        }

    @Test
    fun `WHEN reading locations fails THEN returns false`() =
        runTest {
            coEvery { dao.getLocationData() } throws IllegalStateException("db closed")

            val result = uploadLocationDataUseCase.execute(Unit)

            assertFalse(result)
            verify(exactly = 0) { storageClient.uploadBundle(any(), any()) }
        }

    @Test
    fun `WHEN marking locations uploaded fails THEN returns false and does not purge`() =
        runTest {
            stubTwoSessionsWithLocations()
            coEvery { dao.updateLocationDataUploadStatus(any(), any()) } throws IllegalStateException("db closed")

            val result = uploadLocationDataUseCase.execute(Unit)

            assertFalse(result)
            coVerify(exactly = 0) { dao.purgeUploadedLocations() }
        }

    @Test
    fun `WHEN there are no locations THEN still uploads a bundle with empty tracks`() =
        runTest {
            coEvery { dao.getLocationData() } returns emptyList()
            val bundleSlot = slot<String>()
            every { storageClient.uploadBundle(capture(bundleSlot), any()) } returns Unit

            val result = uploadLocationDataUseCase.execute(Unit)

            assertTrue(result)
            assertTrue(
                json
                    .parseToJsonElement(bundleSlot.captured)
                    .jsonObject["tracks"]!!
                    .jsonArray
                    .isEmpty(),
            )
            coVerify { dao.updateLocationDataUploadStatus(emptyList(), true) }
        }

    private fun stubTwoSessionsWithLocations() {
        coEvery { dao.getLocationData() } returns
            listOf(
                LocationEntity(locationDataJson = locationJson(-1.29, 36.82, accuracy = 3.5f), sessionId = 1L).apply { id = 1L },
                LocationEntity(locationDataJson = locationJson(-1.30, 36.83), sessionId = 2L).apply { id = 2L },
                LocationEntity(locationDataJson = locationJson(-1.31, 36.84), sessionId = 1L).apply { id = 3L },
            )
        coEvery { dao.getSessionById(1L) } returns session(id = 1L, uuid = "session-uuid-1")
        coEvery { dao.getSessionById(2L) } returns session(id = 2L, uuid = "session-uuid-2")
    }

    private fun locationJson(
        latitude: Double,
        longitude: Double,
        accuracy: Float = 5f,
    ): String =
        json.encodeToString(
            LocationData(
                planterCheckInId = null,
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                treeUuid = null,
                convergenceStatus = ConvergenceStatus.CONVERGED,
                capturedAt = "2023-01-01T00:00:01Z",
            ),
        )

    private fun session(
        id: Long,
        uuid: String,
    ): SessionEntity =
        SessionEntity(
            uuid = uuid,
            originUserId = "user-uuid",
            originWallet = "wallet",
            destinationWallet = "dest-wallet",
            startTime = Instant.parse("2023-01-01T00:00:00Z"),
            organization = "org",
            isUploaded = false,
        ).apply { this.id = id }
}