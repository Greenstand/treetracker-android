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
package org.greenstand.android.TreeTracker.integration

import com.amazonaws.AmazonClientException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.database.DbFixtures
import org.greenstand.android.TreeTracker.database.InMemoryDatabaseTest
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.ConvergenceStatus
import org.greenstand.android.TreeTracker.models.DeviceConfigUploader
import org.greenstand.android.TreeTracker.models.LocationData
import org.greenstand.android.TreeTracker.models.SessionUploader
import org.greenstand.android.TreeTracker.usecases.UploadLocationDataUseCase
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.md5
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs the session, device config and GPS track uploaders against a real in-memory Room database,
 * with only the S3 client faked. Checks what each one sends and what it leaves in the database,
 * so the DAO queries and the uploaders are tested together rather than against mocks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UploadPipelineIntegrationTest : InMemoryDatabaseTest() {
    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    /** Every bundle handed to S3, in order, as (bundleId, json). */
    private val uploadedBundles = mutableListOf<Pair<String, String>>()
    private var s3Offline = false
    private val storageClient: ObjectStorageClient = mockk()

    private val dao: TreeTrackerDAO get() = database.treeTrackerDao()

    @Before
    fun setUpFakes() {
        every { storageClient.uploadBundle(any(), any()) } answers {
            if (s3Offline) throw AmazonClientException("offline")
            uploadedBundles += secondArg<String>() to firstArg<String>()
        }
        mockkObject(ObjectStorageClient.Companion)
        every { ObjectStorageClient.instance() } returns storageClient
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "device-id"
    }

    @After
    fun tearDownFakes() {
        unmockkAll()
    }

    private fun bundleJson(index: Int): JsonObject = json.parseToJsonElement(uploadedBundles[index].second).jsonObject

    private fun locationJson(lat: Double) =
        json.encodeToString(
            LocationData(
                planterCheckInId = null,
                latitude = lat,
                longitude = -lat,
                accuracy = 5f,
                treeUuid = null,
                convergenceStatus = ConvergenceStatus.CONVERGED,
                capturedAt = "2024-03-01T10:15:30Z",
            ),
        )

    private suspend fun insertSessionWithConfig(
        uuid: String,
        configUuid: String = "config-$uuid",
        uploaded: Boolean = false,
        organization: String? = "org",
    ): Long {
        val configId = dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = configUuid))
        return dao.insertSession(
            DbFixtures.session(uuid = uuid, isUploaded = uploaded, deviceConfigId = configId, organization = organization),
        )
    }

    // region SessionUploader

    @Test
    fun `session upload sends only pending sessions with their device config uuid`() =
        runTest {
            val pendingId = insertSessionWithConfig("pending", configUuid = "config-a")
            insertSessionWithConfig("already-sent", uploaded = true)

            SessionUploader(dao, storageClient, json).upload()

            val sessions = bundleJson(0)["sessions"]!!.jsonArray.map { it.jsonObject }
            assertEquals(1, sessions.size)
            assertEquals("pending", sessions[0]["id"]!!.jsonPrimitive.content)
            assertEquals("config-a", sessions[0]["device_configuration_id"]!!.jsonPrimitive.content)
            assertEquals("user-uuid", sessions[0]["originating_wallet_registration_id"]!!.jsonPrimitive.content)
            assertEquals("destination-wallet", sessions[0]["target_wallet"]!!.jsonPrimitive.content)

            val stored = dao.getSessionById(pendingId)
            assertTrue(stored.isUploaded)
            assertEquals(uploadedBundles[0].first, stored.bundleId)
            assertEquals(uploadedBundles[0].second.md5() + "_sessions", stored.bundleId)
            assertTrue(dao.getSessionsToUpload().isEmpty())
        }

    @Test
    fun `session upload sends an empty organization when the session has none`() =
        runTest {
            insertSessionWithConfig("no-org", organization = null)

            SessionUploader(dao, storageClient, json).upload()

            val session = bundleJson(0)["sessions"]!!.jsonArray.single().jsonObject
            assertEquals("", session["organization"]!!.jsonPrimitive.content)
        }

    @Test
    fun `failed session upload keeps the session pending so the next sync retries it`() =
        runTest {
            val id = insertSessionWithConfig("pending")
            s3Offline = true

            runCatching { SessionUploader(dao, storageClient, json).upload() }

            assertFalse(dao.getSessionById(id).isUploaded)
            assertEquals(listOf(id), dao.getSessionsToUpload().map { it.id })

            s3Offline = false
            SessionUploader(dao, storageClient, json).upload()

            assertTrue(dao.getSessionById(id).isUploaded)
            assertEquals(1, uploadedBundles.size)
        }

    // endregion

    // region DeviceConfigUploader

    @Test
    fun `device config upload sends pending configs with the instance id and marks them uploaded`() =
        runTest {
            val id = dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "config-1"))
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "config-sent", isUploaded = true))

            DeviceConfigUploader(dao, storageClient, json).upload(instanceId = "instance-7")

            val configs = bundleJson(0)["device_configurations"]!!.jsonArray.map { it.jsonObject }
            assertEquals(1, configs.size)
            assertEquals("config-1", configs[0]["id"]!!.jsonPrimitive.content)
            assertEquals("instance-7", configs[0]["instance_id"]!!.jsonPrimitive.content)
            assertEquals("device-id", configs[0]["device_identifier"]!!.jsonPrimitive.content)
            assertEquals(DbFixtures.CREATED_AT.toString(), configs[0]["logged_at"]!!.jsonPrimitive.content)
            val stored = dao.getDeviceConfigById(id)!!
            assertTrue(stored.isUploaded)
            assertEquals(uploadedBundles[0].first, stored.bundleId)
            assertTrue(uploadedBundles[0].first.endsWith("_deviceConfigs"))
        }

    @Test
    fun `device config upload does nothing when every config is already uploaded`() =
        runTest {
            dao.insertDeviceConfig(DbFixtures.deviceConfig(isUploaded = true))

            DeviceConfigUploader(dao, storageClient, json).upload(instanceId = "instance")

            verify(exactly = 0) { storageClient.uploadBundle(any(), any()) }
        }

    @Test
    fun `failed device config upload records the bundle id but leaves the config pending`() =
        runTest {
            val id = dao.insertDeviceConfig(DbFixtures.deviceConfig())
            s3Offline = true

            runCatching { DeviceConfigUploader(dao, storageClient, json).upload(instanceId = "instance") }

            val stored = dao.getDeviceConfigById(id)!!
            assertFalse(stored.isUploaded)
            assertTrue(stored.bundleId!!.endsWith("_deviceConfigs"))
        }

    // endregion

    // region UploadLocationDataUseCase

    @Test
    fun `track upload groups pending locations by session and deletes them afterwards`() =
        runTest {
            val first = insertSessionWithConfig("session-1")
            val second = insertSessionWithConfig("session-2")
            dao.insertLocationData(DbFixtures.location(first, json = locationJson(1.0)))
            dao.insertLocationData(DbFixtures.location(second, json = locationJson(2.0)))
            dao.insertLocationData(DbFixtures.location(first, json = locationJson(3.0)))

            assertTrue(UploadLocationDataUseCase(dao, json).execute(Unit))

            val tracks = bundleJson(0)["tracks"]!!.jsonArray.map { it.jsonObject }
            val latsBySession =
                tracks.associate { track ->
                    track["sessionId"]!!.jsonPrimitive.content to
                        track["locations"]!!.jsonArray.map {
                            it.jsonObject["latitude"]!!
                                .jsonPrimitive.content
                                .toDouble()
                        }
                }
            assertEquals(mapOf("session-1" to listOf(1.0, 3.0), "session-2" to listOf(2.0)), latsBySession)
            assertTrue(uploadedBundles[0].first.endsWith("_tracks"))
            assertTrue(dao.getLocationData().isEmpty())
            assertEquals(0, countRows("location"))
        }

    @Test
    fun `failed track upload keeps every location for the next sync`() =
        runTest {
            val sessionId = insertSessionWithConfig("session-1")
            dao.insertLocationData(DbFixtures.location(sessionId, json = locationJson(1.0)))
            dao.insertLocationData(DbFixtures.location(sessionId, json = locationJson(2.0)))
            s3Offline = true

            assertFalse(UploadLocationDataUseCase(dao, json).execute(Unit))

            assertEquals(2, dao.getLocationData().size)

            s3Offline = false
            assertTrue(UploadLocationDataUseCase(dao, json).execute(Unit))
            assertEquals(0, countRows("location"))
        }

    @Test
    fun `a corrupt location row stops the track upload without losing data`() =
        runTest {
            val sessionId = insertSessionWithConfig("session-1")
            dao.insertLocationData(DbFixtures.location(sessionId, json = locationJson(1.0)))
            dao.insertLocationData(DbFixtures.location(sessionId, json = "{broken"))

            assertFalse(UploadLocationDataUseCase(dao, json).execute(Unit))

            assertTrue(uploadedBundles.isEmpty())
            assertEquals(2, countRows("location"))
        }

    // endregion

    @Test
    fun `sync order of device configs then sessions then tracks leaves nothing pending`() =
        runTest {
            val sessionId = insertSessionWithConfig("session-1", configUuid = "config-1")
            dao.insertLocationData(DbFixtures.location(sessionId, json = locationJson(1.0)))

            DeviceConfigUploader(dao, storageClient, json).upload(instanceId = "instance")
            SessionUploader(dao, storageClient, json).upload()
            assertTrue(UploadLocationDataUseCase(dao, json).execute(Unit))

            assertEquals(listOf("_deviceConfigs", "_sessions", "_tracks"), uploadedBundles.map { "_" + it.first.substringAfter("_") })
            assertTrue(dao.getDeviceConfigsToUpload().isEmpty())
            assertTrue(dao.getSessionsToUpload().isEmpty())
            assertTrue(dao.getLocationData().isEmpty())
            val sessionBundle = bundleJson(1)["sessions"]!!.jsonArray.single().jsonObject
            val configBundle = bundleJson(0)["device_configurations"]!!.jsonArray.single().jsonObject
            assertEquals(configBundle["id"], sessionBundle["device_configuration_id"])
            assertEquals(
                "session-1",
                bundleJson(2)["tracks"]!!
                    .jsonArray
                    .single()
                    .jsonObject["sessionId"]!!
                    .jsonPrimitive.content,
            )
            assertEquals(JsonNull, bundleJson(2)["sessions"])
        }

    private fun countRows(table: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}