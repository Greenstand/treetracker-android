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
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.usecases.UploadImageParams
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.md5
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@ExperimentalCoroutinesApi
class PlanterUploaderTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var uploadImageUseCase: UploadImageUseCase

    @MockK(relaxed = true)
    private lateinit var objectStorageClient: ObjectStorageClient

    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private lateinit var planterUploader: PlanterUploader

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "test-device-id"
        planterUploader =
            PlanterUploader(
                dao = dao,
                uploadImageUseCase = uploadImageUseCase,
                json = json,
                objectStorageClient = objectStorageClient,
            )
    }

    @After
    fun tearDown() {
        unmockkObject(DeviceUtils)
    }

    @Test
    fun `WHEN upload called THEN uploads user images planter info bundles and user bundles`() =
        runTest {
            val userEntity =
                UserEntity(
                    uuid = "user-uuid-1",
                    wallet = "wallet-1",
                    firstName = "John",
                    lastName = "Doe",
                    phone = "555-1234",
                    email = "john@test.com",
                    latitude = 37.0,
                    longitude = -122.0,
                    createdAt = Instant.parse("2023-01-01T00:00:00Z"),
                    photoPath = "/test/user-photo.jpg",
                    photoUrl = null,
                    powerUser = false,
                ).apply { id = 1L }

            val planterInfo =
                PlanterInfoEntity(
                    identifier = "planter-id",
                    firstName = "Jane",
                    lastName = "Doe",
                    organization = "TestOrg",
                    phone = "555-5678",
                    email = "jane@test.com",
                    latitude = 37.0,
                    longitude = -122.0,
                    createdAt = System.currentTimeMillis(),
                    recordUuid = "record-uuid-1",
                ).apply { id = 1L }

            val planterCheckIn =
                PlanterCheckInEntity(
                    planterInfoId = 1L,
                    localPhotoPath = "/test/checkin-photo.jpg",
                    photoUrl = "https://uploaded.url/checkin.jpg",
                    latitude = 37.0,
                    longitude = -122.0,
                    createdAt = System.currentTimeMillis(),
                ).apply { id = 1L }

            coEvery { dao.getAllUsersToUpload() } returns listOf(userEntity)
            coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/user.jpg"
            coEvery { dao.getAllPlanterInfoToUpload() } returns listOf(planterInfo)
            coEvery { dao.getAllPlanterCheckInsForPlanterInfoId(1L) } returns listOf(planterCheckIn)
            coEvery { dao.getPlanterCheckInsToUpload() } returns listOf(planterCheckIn)

            planterUploader.upload("instance-123")

            coVerify(atLeast = 1) { uploadImageUseCase.execute(any()) }
            coVerify(atLeast = 1) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 1) { dao.updateUserUploadStatus(listOf(1L), true) }
        }

    @Test
    fun `WHEN users have existing photoUrl THEN skips image upload for those users`() =
        runTest {
            val userWithPhoto =
                UserEntity(
                    uuid = "user-uuid-2",
                    wallet = "wallet-2",
                    firstName = "Existing",
                    lastName = "Photo",
                    phone = null,
                    email = null,
                    latitude = 37.0,
                    longitude = -122.0,
                    createdAt = Instant.parse("2023-01-01T00:00:00Z"),
                    photoPath = "/test/user-photo2.jpg",
                    photoUrl = "https://already-uploaded.url/photo.jpg",
                    powerUser = false,
                ).apply { id = 2L }

            coEvery { dao.getAllUsersToUpload() } returns listOf(userWithPhoto)
            coEvery { dao.getAllPlanterInfoToUpload() } returns emptyList()
            coEvery { dao.getPlanterCheckInsToUpload() } returns emptyList()

            planterUploader.upload("instance-123")

            coVerify(exactly = 0) { uploadImageUseCase.execute(any()) }
        }

    @Test
    fun `WHEN legacy check in image uploads THEN saves the image url on the check in`() =
        runTest {
            stubNothingToUpload()
            val checkIn = checkIn(id = 1L, photoUrl = null, localPhotoPath = "/test/checkin.jpg", createdAt = 100L)
            coEvery { dao.getPlanterCheckInsToUpload() } returns listOf(checkIn)
            coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/checkin.jpg"

            planterUploader.upload("instance-123")

            coVerify { uploadImageUseCase.execute(UploadImageParams(imagePath = "/test/checkin.jpg", lat = 37.0, long = -122.0)) }
            coVerify { dao.updatePlanterCheckIn(match { it.id == 1L && it.photoUrl == "https://uploaded.url/checkin.jpg" }) }
        }

    @Test
    fun `WHEN legacy check in image upload returns no url THEN check in is not updated`() =
        runTest {
            stubNothingToUpload()
            coEvery { dao.getPlanterCheckInsToUpload() } returns
                listOf(checkIn(id = 1L, photoUrl = null, localPhotoPath = "/test/checkin.jpg", createdAt = 100L))
            coEvery { uploadImageUseCase.execute(any()) } returns null

            planterUploader.upload("instance-123")

            coVerify(exactly = 0) { dao.updatePlanterCheckIn(any()) }
        }

    @Test
    fun `WHEN check in has no local photo THEN its image is not uploaded`() =
        runTest {
            stubNothingToUpload()
            coEvery { dao.getPlanterCheckInsToUpload() } returns
                listOf(checkIn(id = 1L, photoUrl = null, localPhotoPath = null, createdAt = 100L))

            planterUploader.upload("instance-123")

            coVerify(exactly = 0) { uploadImageUseCase.execute(any()) }
        }

    @Test
    fun `WHEN planter info uploaded THEN registration uses the oldest check in photo`() =
        runTest {
            stubNothingToUpload()
            val bundles = mutableListOf<String>()
            every { objectStorageClient.uploadBundle(capture(bundles), any()) } returns Unit
            coEvery { dao.getAllPlanterInfoToUpload() } returns listOf(planterInfo(id = 5L))
            coEvery { dao.getAllPlanterCheckInsForPlanterInfoId(5L) } returns
                listOf(
                    checkIn(id = 1L, photoUrl = "https://uploaded.url/newer.jpg", localPhotoPath = null, createdAt = 200L),
                    checkIn(id = 2L, photoUrl = "https://uploaded.url/oldest.jpg", localPhotoPath = null, createdAt = 100L),
                )

            planterUploader.upload("instance-123")

            val bundle = parse(bundles.single())
            assertEquals("1", bundle["pack_format_version"]!!.jsonPrimitive.content)
            assertEquals(
                "instance-123",
                bundle["devices"]!!
                    .jsonArray
                    .single()
                    .jsonObject["instanceId"]!!
                    .jsonPrimitive.content,
            )
            val registration = bundle["registrations"]!!.jsonArray.single().jsonObject
            assertEquals("https://uploaded.url/oldest.jpg", registration["image_url"]!!.jsonPrimitive.content)
            assertEquals("planter-id", registration["planter_identifier"]!!.jsonPrimitive.content)
            assertEquals("Jane", registration["first_name"]!!.jsonPrimitive.content)
            assertEquals("Doe", registration["last_name"]!!.jsonPrimitive.content)
            assertEquals("TestOrg", registration["organization"]!!.jsonPrimitive.content)
            assertEquals("555-5678", registration["phone"]!!.jsonPrimitive.content)
            assertEquals("jane@test.com", registration["email"]!!.jsonPrimitive.content)
            assertEquals(37.0, registration["lat"]!!.jsonPrimitive.double)
            assertEquals(-122.0, registration["lon"]!!.jsonPrimitive.double)
            assertEquals("record-uuid-5", registration["record_uuid"]!!.jsonPrimitive.content)
            assertEquals("test-device-id", registration["device_identifier"]!!.jsonPrimitive.content)
        }

    @Test
    fun `WHEN planter has no check ins THEN registration image url is empty`() =
        runTest {
            stubNothingToUpload()
            val bundles = mutableListOf<String>()
            every { objectStorageClient.uploadBundle(capture(bundles), any()) } returns Unit
            coEvery { dao.getAllPlanterInfoToUpload() } returns listOf(planterInfo(id = 5L))
            coEvery { dao.getAllPlanterCheckInsForPlanterInfoId(5L) } returns emptyList()

            planterUploader.upload("instance-123")

            val registration = parse(bundles.single())["registrations"]!!.jsonArray.single().jsonObject
            assertEquals("", registration["image_url"]!!.jsonPrimitive.content)
        }

    @Test
    fun `WHEN planter info uploaded THEN records bundle id uploads bundle then marks planters uploaded`() =
        runTest {
            stubNothingToUpload()
            val bundles = mutableListOf<String>()
            val bundleIds = mutableListOf<String>()
            every { objectStorageClient.uploadBundle(capture(bundles), capture(bundleIds)) } returns Unit
            coEvery { dao.getAllPlanterInfoToUpload() } returns listOf(planterInfo(id = 5L), planterInfo(id = 6L))

            planterUploader.upload("instance-123")

            val expectedBundleId = bundles.single().md5() + "_registrations"
            assertEquals(listOf(expectedBundleId), bundleIds)
            coVerifyOrder {
                dao.updatePlanterInfoBundleIds(listOf(5L, 6L), expectedBundleId)
                objectStorageClient.uploadBundle(any(), expectedBundleId)
                dao.updatePlanterInfoUploadStatus(listOf(5L, 6L), true)
            }
        }

    @Test
    fun `WHEN users uploaded THEN bundle contains wallet registrations`() =
        runTest {
            stubNothingToUpload()
            val bundles = mutableListOf<String>()
            every { objectStorageClient.uploadBundle(capture(bundles), any()) } returns Unit
            coEvery { dao.getAllUsersToUpload() } returns listOf(user(id = 1L, photoUrl = "https://uploaded.url/user.jpg"))

            planterUploader.upload("instance-123")

            val bundle = parse(bundles.single())
            assertEquals("2", bundle["pack_format_version"]!!.jsonPrimitive.content)
            val registration = bundle["wallet_registrations"]!!.jsonArray.single().jsonObject
            assertEquals("user-uuid-1", registration["id"]!!.jsonPrimitive.content)
            assertEquals("wallet-1", registration["wallet"]!!.jsonPrimitive.content)
            assertEquals("https://uploaded.url/user.jpg", registration["user_photo_url"]!!.jsonPrimitive.content)
            assertEquals("John", registration["first_name"]!!.jsonPrimitive.content)
            assertEquals("Doe", registration["last_name"]!!.jsonPrimitive.content)
            assertEquals("555-1234", registration["phone"]!!.jsonPrimitive.content)
            assertEquals("john@test.com", registration["email"]!!.jsonPrimitive.content)
            assertEquals(37.0, registration["lat"]!!.jsonPrimitive.double)
            assertEquals(-122.0, registration["lon"]!!.jsonPrimitive.double)
            assertEquals("2023-01-01T00:00:00Z", registration["registered_at"]!!.jsonPrimitive.content)
        }

    @Test
    fun `WHEN users uploaded THEN records bundle id uploads bundle then marks users uploaded`() =
        runTest {
            stubNothingToUpload()
            val bundles = mutableListOf<String>()
            val bundleIds = mutableListOf<String>()
            every { objectStorageClient.uploadBundle(capture(bundles), capture(bundleIds)) } returns Unit
            coEvery { dao.getAllUsersToUpload() } returns
                listOf(
                    user(id = 1L, photoUrl = "https://uploaded.url/user1.jpg"),
                    user(id = 2L, photoUrl = "https://uploaded.url/user2.jpg"),
                )

            planterUploader.upload("instance-123")

            val expectedBundleId = bundles.single().md5() + "_registrations"
            assertEquals(listOf(expectedBundleId), bundleIds)
            coVerifyOrder {
                dao.updateUserBundleIds(listOf(1L, 2L), expectedBundleId)
                objectStorageClient.uploadBundle(any(), expectedBundleId)
                dao.updateUserUploadStatus(listOf(1L, 2L), true)
            }
        }

    @Test
    fun `WHEN user image uploads THEN saves the image url on the user`() =
        runTest {
            stubNothingToUpload()
            val user = user(id = 1L, photoUrl = null)
            coEvery { dao.getAllUsersToUpload() } returns listOf(user)
            coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/user.jpg"

            planterUploader.upload("instance-123")

            coVerify { uploadImageUseCase.execute(UploadImageParams(imagePath = "/test/user-1.jpg", lat = 37.0, long = -122.0)) }
            coVerify { dao.updateUser(match { it.id == 1L && it.photoUrl == "https://uploaded.url/user.jpg" }) }
        }

    @Test
    fun `WHEN user image upload returns no url THEN upload fails before sending the users bundle`() =
        runTest {
            stubNothingToUpload()
            coEvery { dao.getAllUsersToUpload() } returns listOf(user(id = 1L, photoUrl = null))
            coEvery { uploadImageUseCase.execute(any()) } returns null

            assertFailsWith<NullPointerException> {
                planterUploader.upload("instance-123")
            }

            coVerify(exactly = 0) { dao.updateUser(any()) }
            coVerify(exactly = 0) { dao.updateUserUploadStatus(any(), any()) }
            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
        }

    @Test
    fun `WHEN nothing to upload THEN no bundle is uploaded`() =
        runTest {
            stubNothingToUpload()

            planterUploader.upload("instance-123")

            coVerify(exactly = 0) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 0) { dao.updatePlanterInfoUploadStatus(any(), any()) }
            coVerify(exactly = 0) { dao.updateUserUploadStatus(any(), any()) }
            coVerify { dao.removePlanterCheckInLocalImagePaths(emptyList()) }
        }

    @Test
    fun `WHEN check in photos were uploaded THEN deletes their local files oldest first`() =
        runTest {
            stubNothingToUpload()
            val newerPhoto = tempFolder.newFile("newer.jpg")
            val olderPhoto = tempFolder.newFile("older.jpg")
            val pendingPhoto = tempFolder.newFile("pending.jpg")
            coEvery { dao.getPlanterCheckInsToUpload() } returns
                listOf(
                    checkIn(id = 1L, photoUrl = "https://uploaded.url/1.jpg", localPhotoPath = newerPhoto.absolutePath, createdAt = 200L),
                    checkIn(id = 2L, photoUrl = "https://uploaded.url/2.jpg", localPhotoPath = olderPhoto.absolutePath, createdAt = 100L),
                    checkIn(id = 3L, photoUrl = null, localPhotoPath = pendingPhoto.absolutePath, createdAt = 50L),
                )
            coEvery { uploadImageUseCase.execute(any()) } returns null

            planterUploader.upload("instance-123")

            assertFalse(newerPhoto.exists())
            assertFalse(olderPhoto.exists())
            assertTrue(pendingPhoto.exists())
            coVerify { dao.removePlanterCheckInLocalImagePaths(listOf(2L, 1L)) }
        }

    @Test
    fun `WHEN uploading THEN images are uploaded before bundles and local files are cleaned up last`() =
        runTest {
            stubNothingToUpload()
            coEvery { dao.getPlanterCheckInsToUpload() } returns
                listOf(checkIn(id = 1L, photoUrl = null, localPhotoPath = "/test/checkin.jpg", createdAt = 100L))
            coEvery { dao.getAllUsersToUpload() } returns listOf(user(id = 1L, photoUrl = null))
            coEvery { dao.getAllPlanterInfoToUpload() } returns listOf(planterInfo(id = 5L))
            coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/image.jpg"

            planterUploader.upload("instance-123")

            coVerifyOrder {
                dao.updatePlanterCheckIn(any())
                dao.updateUser(any())
                dao.updatePlanterInfoUploadStatus(listOf(5L), true)
                dao.updateUserUploadStatus(listOf(1L), true)
                dao.removePlanterCheckInLocalImagePaths(any())
            }
        }

    @Test
    fun `WHEN planter bundle upload fails THEN planters are not marked uploaded and users are not sent`() =
        runTest {
            stubNothingToUpload()
            coEvery { dao.getAllPlanterInfoToUpload() } returns listOf(planterInfo(id = 5L))
            coEvery { dao.getAllUsersToUpload() } returns listOf(user(id = 1L, photoUrl = "https://uploaded.url/user.jpg"))
            every { objectStorageClient.uploadBundle(any(), any()) } throws RuntimeException("offline")

            assertFailsWith<RuntimeException> {
                planterUploader.upload("instance-123")
            }

            coVerify(exactly = 0) { dao.updatePlanterInfoUploadStatus(any(), any()) }
            coVerify(exactly = 0) { dao.updateUserBundleIds(any(), any()) }
        }

    private fun stubNothingToUpload() {
        coEvery { dao.getPlanterCheckInsToUpload() } returns emptyList()
        coEvery { dao.getAllUsersToUpload() } returns emptyList()
        coEvery { dao.getAllPlanterInfoToUpload() } returns emptyList()
        coEvery { dao.getAllPlanterCheckInsForPlanterInfoId(any()) } returns emptyList()
    }

    private fun parse(bundle: String): JsonObject = json.parseToJsonElement(bundle).jsonObject

    private fun checkIn(
        id: Long,
        photoUrl: String?,
        localPhotoPath: String?,
        createdAt: Long,
    ): PlanterCheckInEntity =
        PlanterCheckInEntity(
            planterInfoId = 5L,
            localPhotoPath = localPhotoPath,
            photoUrl = photoUrl,
            latitude = 37.0,
            longitude = -122.0,
            createdAt = createdAt,
        ).apply { this.id = id }

    private fun planterInfo(id: Long): PlanterInfoEntity =
        PlanterInfoEntity(
            identifier = "planter-id",
            firstName = "Jane",
            lastName = "Doe",
            organization = "TestOrg",
            phone = "555-5678",
            email = "jane@test.com",
            latitude = 37.0,
            longitude = -122.0,
            createdAt = 1_672_531_200_000L,
            recordUuid = "record-uuid-$id",
        ).apply { this.id = id }

    private fun user(
        id: Long,
        photoUrl: String?,
    ): UserEntity =
        UserEntity(
            uuid = "user-uuid-$id",
            wallet = "wallet-$id",
            firstName = "John",
            lastName = "Doe",
            phone = "555-1234",
            email = "john@test.com",
            latitude = 37.0,
            longitude = -122.0,
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
            photoPath = "/test/user-$id.jpg",
            photoUrl = photoUrl,
            powerUser = false,
        ).apply { this.id = id }
}