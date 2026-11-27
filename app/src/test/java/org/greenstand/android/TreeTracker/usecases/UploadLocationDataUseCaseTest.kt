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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.Ordering
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.LocationEntity
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.models.ConvergenceStatus
import org.greenstand.android.TreeTracker.models.LocationData
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.md5
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UploadLocationDataUseCaseTest {
    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    private val dao: TreeTrackerDAO = mockk(relaxed = true)
    private val storageClient: ObjectStorageClient = mockk(relaxed = true)
    private lateinit var useCase: UploadLocationDataUseCase

    @Before
    fun setUp() {
        mockkObject(ObjectStorageClient.Companion)
        every { ObjectStorageClient.instance() } returns storageClient
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "device-id"
        useCase = UploadLocationDataUseCase(dao, json)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun locationJson(
        lat: Double,
        capturedAt: String = "2024-03-01T10:15:30Z",
    ) = json.encodeToString(
        LocationData(
            planterCheckInId = null,
            latitude = lat,
            longitude = lat + 1,
            accuracy = 3.5f,
            treeUuid = null,
            convergenceStatus = ConvergenceStatus.CONVERGED,
            capturedAt = capturedAt,
        ),
    )

    private fun location(
        id: Long,
        sessionId: Long,
        lat: Double,
    ) = LocationEntity(locationDataJson = locationJson(lat), sessionId = sessionId).also { it.id = id }

    private fun session(
        id: Long,
        uuid: String,
    ) = SessionEntity(
        uuid = uuid,
        originUserId = "user",
        originWallet = "wallet",
        destinationWallet = "wallet",
        startTime = Instant.parse("2024-03-01T10:00:00Z"),
        organization = null,
        isUploaded = false,
    ).also { it.id = id }

    @Test
    fun `groups locations into one track per session using the session uuid`() =
        runTest {
            coEvery { dao.getLocationData() } returns
                listOf(location(1, 10, 1.0), location(2, 20, 2.0), location(3, 10, 3.0))
            coEvery { dao.getSessionById(10) } returns session(10, "uuid-10")
            coEvery { dao.getSessionById(20) } returns session(20, "uuid-20")
            val bundle = slot<String>()
            every { storageClient.uploadBundle(capture(bundle), any()) } returns Unit

            assertTrue(useCase.execute(Unit))

            val root = json.parseToJsonElement(bundle.captured).jsonObject
            assertEquals("2", root["pack_format_version"]!!.jsonPrimitive.content)
            assertEquals("device-id", root["device_id"]!!.jsonPrimitive.content)
            val tracks = root["tracks"]!!.jsonArray.map { it.jsonObject }
            assertEquals(listOf("uuid-10", "uuid-20"), tracks.map { it["sessionId"]!!.jsonPrimitive.content })
            val firstTrackLats =
                tracks[0]["locations"]!!.jsonArray.map {
                    it.jsonObject["latitude"]!!
                        .jsonPrimitive.content
                        .toDouble()
                }
            assertEquals(listOf(1.0, 3.0), firstTrackLats)
            val loc = tracks[1]["locations"]!!.jsonArray.single().jsonObject
            assertEquals(3.0, loc["longitude"]!!.jsonPrimitive.content.toDouble(), 0.0)
            assertEquals(3.5, loc["accuracy"]!!.jsonPrimitive.content.toDouble(), 0.0)
            assertEquals("2024-03-01T10:15:30Z", loc["capturedAt"]!!.jsonPrimitive.content)
        }

    @Test
    fun `names the bundle after the md5 of its contents with a tracks suffix`() =
        runTest {
            coEvery { dao.getLocationData() } returns listOf(location(1, 10, 1.0))
            coEvery { dao.getSessionById(10) } returns session(10, "uuid-10")
            val bundle = slot<String>()
            val bundleId = slot<String>()
            every { storageClient.uploadBundle(capture(bundle), capture(bundleId)) } returns Unit

            useCase.execute(Unit)

            assertEquals("${bundle.captured.md5()}_tracks", bundleId.captured)
        }

    @Test
    fun `marks the uploaded locations and purges them after a successful upload`() =
        runTest {
            coEvery { dao.getLocationData() } returns listOf(location(1, 10, 1.0), location(2, 10, 2.0))
            coEvery { dao.getSessionById(10) } returns session(10, "uuid-10")

            assertTrue(useCase.execute(Unit))

            coVerify(ordering = Ordering.ORDERED) {
                storageClient.uploadBundle(any(), any())
                dao.updateLocationDataUploadStatus(listOf(1L, 2L), true)
                dao.purgeUploadedLocations()
            }
        }

    @Test
    fun `returns false and keeps locations pending when S3 is unreachable`() =
        runTest {
            coEvery { dao.getLocationData() } returns listOf(location(1, 10, 1.0))
            coEvery { dao.getSessionById(10) } returns session(10, "uuid-10")
            every { storageClient.uploadBundle(any(), any()) } throws AmazonClientException("offline")

            assertFalse(useCase.execute(Unit))

            coVerify(exactly = 0) { dao.updateLocationDataUploadStatus(any(), any()) }
            coVerify(exactly = 0) { dao.purgeUploadedLocations() }
        }

    @Test
    fun `returns false when a location row holds unreadable json`() =
        runTest {
            coEvery { dao.getLocationData() } returns
                listOf(LocationEntity(locationDataJson = "not json", sessionId = 10).also { it.id = 1 })

            assertFalse(useCase.execute(Unit))

            verify(exactly = 0) { storageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 0) { dao.updateLocationDataUploadStatus(any(), any()) }
        }

    @Test
    fun `returns false when a database call fails`() =
        runTest {
            coEvery { dao.getLocationData() } throws IllegalStateException("db closed")

            assertFalse(useCase.execute(Unit))

            verify(exactly = 0) { storageClient.uploadBundle(any(), any()) }
        }

    @Test
    fun `uploads an empty tracks bundle when there are no pending locations`() =
        runTest {
            coEvery { dao.getLocationData() } returns emptyList()
            val bundle = slot<String>()
            every { storageClient.uploadBundle(capture(bundle), any()) } returns Unit

            assertTrue(useCase.execute(Unit))

            assertTrue(
                json
                    .parseToJsonElement(bundle.captured)
                    .jsonObject["tracks"]!!
                    .jsonArray
                    .isEmpty(),
            )
            coVerify { dao.updateLocationDataUploadStatus(emptyList(), true) }
        }
}